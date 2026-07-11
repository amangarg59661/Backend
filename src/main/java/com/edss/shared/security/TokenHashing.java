package com.edss.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shared helpers for random-token generation + SHA-256 hashing in url-safe
 * base64. Used by refresh tokens, password reset tokens, trusted device
 * tokens, invite tokens, backup codes — anywhere the "random bytes → store
 * hash, keep plaintext ephemeral" pattern shows up.
 */
public final class TokenHashing {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();

    private TokenHashing() {}

    /** Random url-safe base64 token backed by {@code bytes} of entropy. */
    public static String randomUrlBase64(int bytes) {
        byte[] out = new byte[bytes];
        RNG.nextBytes(out);
        return ENC.encodeToString(out);
    }

    /** SHA-256 of {@code value} encoded as url-safe base64 (no padding). */
    public static String sha256UrlBase64(String value) {
        try {
            byte[] out =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return ENC.encodeToString(out);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
