package com.edss.identity.application;

import com.edss.identity.domain.BackupCode;
import com.edss.identity.infrastructure.BackupCodeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the 10-code recovery set per user. Codes are 10-character
 * upper-case alphanumeric (2^{51} bits, no I/O/0/1 to avoid ambiguity)
 * stored SHA-256 hashed. Consumption is single-use.
 */
@Service
@Transactional
public class BackupCodeService {

    public static final int CODE_COUNT = 10;
    private static final int CODE_LENGTH = 10;
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    private final BackupCodeRepository codes;
    private final Clock clock;

    public BackupCodeService(BackupCodeRepository codes, Clock clock) {
        this.codes = codes;
        this.clock = clock;
    }

    /**
     * Wipes any existing codes for the user and issues a fresh 10-code set.
     * The plaintext values are returned once — after this call they can only
     * be redeemed, never re-read.
     */
    public List<String> regenerate(UUID userId) {
        codes.deleteAllForUser(userId);
        Instant now = clock.instant();
        List<String> plaintext = new ArrayList<>(CODE_COUNT);
        for (int i = 0; i < CODE_COUNT; i++) {
            String code = randomCode();
            plaintext.add(code);
            codes.save(new BackupCode(UUID.randomUUID(), userId, sha256(code), now));
        }
        return plaintext;
    }

    /** Returns {@code true} if the code matched an unused code and was consumed. */
    public boolean consume(UUID userId, String plaintext) {
        Optional<BackupCode> hit = codes.findByUserIdAndCodeHash(userId, sha256(plaintext));
        if (hit.isEmpty() || hit.get().isUsed()) {
            return false;
        }
        hit.get().markUsed(clock.instant());
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

    private static String sha256(String value) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
