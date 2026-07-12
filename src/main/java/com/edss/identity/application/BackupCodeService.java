package com.edss.identity.application;

import com.edss.identity.domain.BackupCode;
import com.edss.identity.infrastructure.BackupCodeRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the 10-code recovery set per user. Codes are 10-character
 * upper-case alphanumeric (~2^{50} bits, no I/O/0/1 to avoid ambiguity)
 * stored BCrypt(10) hashed. Consumption is single-use.
 *
 * <p>S-12: BCrypt rather than SHA-256. Even though the codes are
 * high-entropy random strings, a DB dump plus GPU cracking would trivially
 * enumerate SHA-256 preimages. BCrypt with a per-code salt costs ~65 ms per
 * candidate which is fine at 10 codes/login attempt and painful for an
 * offline attacker. Cost factor is 10 (not the 12 we use for passwords) to
 * keep verify() budget bounded — the 10-per-user linear scan multiplies
 * cost, and a code has 50 bits of entropy anyway.</p>
 */
@Service
@Transactional
public class BackupCodeService {

    public static final int CODE_COUNT = 10;
    private static final int CODE_LENGTH = 10;
    private static final int BCRYPT_COST = 10;
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    private final BackupCodeRepository codes;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_COST);
    private final Clock clock;

    public BackupCodeService(BackupCodeRepository codes, Clock clock) {
        this.codes = codes;
        this.clock = clock;
    }

    /**
     * Wipes any existing codes for the user and issues a fresh 10-code set.
     * The plaintext values are returned once — after this call they can only
     * be redeemed, never re-read. Rows are inserted in one batched saveAll.
     */
    public List<String> regenerate(UUID userId) {
        codes.deleteAllForUser(userId);
        Instant now = clock.instant();
        List<String> plaintext = new ArrayList<>(CODE_COUNT);
        List<BackupCode> rows = new ArrayList<>(CODE_COUNT);
        for (int i = 0; i < CODE_COUNT; i++) {
            String code = randomCode();
            plaintext.add(code);
            rows.add(new BackupCode(UUID.randomUUID(), userId, encoder.encode(code), now));
        }
        codes.saveAll(rows);
        return plaintext;
    }

    /**
     * Returns {@code true} if the code matched an unused code and was consumed.
     *
     * <p>BCrypt means we must load each row and call {@code matches} — no
     * hash-key lookup. 10 rows × 65ms = ~650ms worst case; acceptable at
     * login frequency. Constant work regardless of position in the list so
     * timing does not leak which code was hit.</p>
     */
    public boolean consume(UUID userId, String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return false;
        }
        List<BackupCode> all = codes.findByUserId(userId);
        BackupCode matched = null;
        for (BackupCode row : all) {
            if (row.isUsed()) {
                continue;
            }
            if (encoder.matches(plaintext, row.getCodeHash())) {
                matched = row;
            }
        }
        if (matched == null) {
            return false;
        }
        matched.markUsed(clock.instant());
        return true;
    }

    @Transactional(readOnly = true)
    public long remaining(UUID userId) {
        return codes.countByUserIdAndUsedAtIsNull(userId);
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
