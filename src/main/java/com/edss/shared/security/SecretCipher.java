package com.edss.shared.security;

import com.edss.shared.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-GCM at-rest encryption for small secrets (TOTP seeds, trusted-device
 * keys). Ciphertext format: base64url(iv || ciphertext || tag). Rotating the
 * encryption key invalidates all stored ciphertext.
 */
@Component
public class SecretCipher {

    private static final String ALG = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final SecretKeySpec key;

    public SecretCipher(SecurityProperties properties) {
        String raw = properties.secrets() == null ? null : properties.secrets().encryptionKey();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "edss.security.secrets.encryption-key must be set (base64-encoded 32 bytes).");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "edss.security.secrets.encryption-key must be valid base64.", ex);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "edss.security.secrets.encryption-key must decode to 32 bytes.");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        RNG.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return ENC.encodeToString(out);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES encrypt failed", ex);
        }
    }

    public String decrypt(String ciphertext) {
        byte[] raw = DEC.decode(ciphertext);
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

}
