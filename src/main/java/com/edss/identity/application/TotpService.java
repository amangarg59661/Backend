package com.edss.identity.application;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * TOTP (RFC 6238) helpers: fresh secret + otpauth URI + QR PNG (base64). Both
 * Google Authenticator and Microsoft Authenticator scan the same otpauth URI
 * format — no separate flow needed.
 */
@Service
public class TotpService {

    private static final Logger log = LoggerFactory.getLogger(TotpService.class);
    private static final String ISSUER = "EDSS";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier verifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    private final QrGenerator qr = new ZxingPngQrGenerator();

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String otpauthUri(String secret, String userEmail) {
        return "otpauth://totp/"
                + urlEncode(ISSUER)
                + ":"
                + urlEncode(userEmail)
                + "?secret="
                + secret
                + "&issuer="
                + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public String qrCodePngBase64(String secret, String userEmail) {
        QrData data =
                new QrData.Builder()
                        .label(userEmail)
                        .secret(secret)
                        .issuer(ISSUER)
                        .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                        .digits(6)
                        .period(30)
                        .build();
        try {
            byte[] png = qr.generate(data);
            return Base64.getEncoder().encodeToString(png);
        } catch (Exception ex) {
            log.warn("QR generation failed for {}", userEmail, ex);
            throw new IllegalStateException("Could not render QR code.", ex);
        }
    }

    public boolean verify(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
