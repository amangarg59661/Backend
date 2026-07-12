package com.edss.shared.security;

import com.edss.shared.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-GCM at-rest encryption for small secrets (TOTP seeds, trusted-device
 * keys). Envelope format: {@code v1:<kid>:<b64url(iv||ct||tag)>}. The kid
 * prefix (S-10) means we can rotate the key without invalidating any stored
 * ciphertext — add a new kid to config, flip {@code activeKid}, and old rows
 * keep decrypting via their embedded kid until they are re-encrypted.
 *
 * <p>Backward compatibility: values without an envelope prefix (legacy
 * single-key ciphertext) still decrypt via the legacy key configured under
 * {@code edss.security.secrets.encryption-key}, which is registered as an
 * implicit kid of {@code v1}.</p>
 */
@Component
public class SecretCipher {

    private static final String ENVELOPE_VERSION = "v1";
    private static final String LEGACY_KID = "v1";
    private static final String ALG = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final Map<String, SecretKeySpec> keysByKid;
    private final String activeKid;

    public SecretCipher(SecurityProperties properties) {
        SecurityProperties.Secrets cfg = properties.secrets();
        if (cfg == null) {
            throw new IllegalStateException("edss.security.secrets must be configured.");
        }
        Map<String, SecretKeySpec> loaded = new LinkedHashMap<>();
        if (cfg.keys() != null) {
            cfg.keys().forEach((kid, raw) -> loaded.put(kid, parseKey(kid, raw)));
        }
        String legacyRaw = cfg.encryptionKey();
        if (legacyRaw != null && !legacyRaw.isBlank()) {
            loaded.putIfAbsent(LEGACY_KID, parseKey(LEGACY_KID, legacyRaw));
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException(
                    "Configure edss.security.secrets.keys.<kid> or the legacy encryption-key.");
        }
        String active = cfg.activeKid();
        if (active == null || active.isBlank()) {
            active = loaded.containsKey(LEGACY_KID) ? LEGACY_KID : loaded.keySet().iterator().next();
        }
        if (!loaded.containsKey(active)) {
            throw new IllegalStateException(
                    "edss.security.secrets.active-kid " + active + " not found in keys map.");
        }
        this.keysByKid = Map.copyOf(loaded);
        this.activeKid = active;
    }

    public String encrypt(String plaintext) {
        SecretKeySpec key = keysByKid.get(activeKid);
        byte[] iv = new byte[IV_BYTES];
        RNG.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return ENVELOPE_VERSION + ":" + activeKid + ":" + ENC.encodeToString(out);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES encrypt failed", ex);
        }
    }

    public String decrypt(String ciphertext) {
        String kid;
        String body;
        if (ciphertext.startsWith(ENVELOPE_VERSION + ":")) {
            String rest = ciphertext.substring((ENVELOPE_VERSION + ":").length());
            int sep = rest.indexOf(':');
            if (sep <= 0) {
                throw new IllegalStateException("Malformed ciphertext envelope.");
            }
            kid = rest.substring(0, sep);
            body = rest.substring(sep + 1);
        } else {
            // Legacy row written before S-10.
            kid = LEGACY_KID;
            body = ciphertext;
        }
        SecretKeySpec key = keysByKid.get(kid);
        if (key == null) {
            throw new IllegalStateException("Unknown key-id in ciphertext: " + kid);
        }
        byte[] raw = DEC.decode(body);
        if (raw.length <= IV_BYTES) {
            throw new IllegalStateException("Ciphertext too short.");
        }
        byte[] iv = new byte[IV_BYTES];
        System.arraycopy(raw, 0, iv, 0, IV_BYTES);
        byte[] ct = new byte[raw.length - IV_BYTES];
        System.arraycopy(raw, IV_BYTES, ct, 0, ct.length);
        try {
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES decrypt failed", ex);
        }
    }

    private static SecretKeySpec parseKey(String kid, String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Empty key material for kid " + kid);
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Key " + kid + " must be valid base64.", ex);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("Key " + kid + " must decode to 32 bytes.");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
