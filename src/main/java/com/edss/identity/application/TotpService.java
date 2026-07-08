package com.edss.identity.application;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

/**
 * TOTP verification (RFC 6238). Secrets live on {@code user_two_factor} in
 * encrypted form and are decrypted by the caller before verification. This
 * service does not persist or generate secrets in v1 — enrollment lands with
 * the settings feature.
 */
@Service
public class TotpService {

    private final CodeVerifier verifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public boolean verify(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
}
