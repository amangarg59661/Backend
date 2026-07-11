package com.edss.identity.api;

import com.edss.identity.api.dto.ForgotPasswordRequest;
import com.edss.identity.api.dto.LoginRequest;
import com.edss.identity.api.dto.LoginResponse;
import com.edss.identity.api.dto.OkResponse;
import com.edss.identity.api.dto.RefreshRequest;
import com.edss.identity.api.dto.RefreshResponse;
import com.edss.identity.api.dto.ResetPasswordRequest;
import com.edss.identity.api.dto.TwoFaVerifyRequest;
import com.edss.identity.application.AuthService;
import com.edss.identity.application.PasswordResetService;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.api.HttpRequests;
import com.edss.shared.config.FeaturesProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "auth", description = "Authentication, 2FA, session and password reset endpoints.")
public class AuthController {

    private final AuthService auth;
    private final PasswordResetService passwordReset;
    private final FeaturesProperties features;

    public AuthController(
            AuthService auth, PasswordResetService passwordReset, FeaturesProperties features) {
        this.auth = auth;
        this.passwordReset = passwordReset;
        this.features = features;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return auth.login(
                req.email(),
                req.password(),
                req.trustedDeviceToken(),
                clientIp(http),
                userAgent(http));
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest req) {
        auth.logout(req == null ? null : req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/verify")
    public LoginResponse verify2fa(
            @Valid @RequestBody TwoFaVerifyRequest req, HttpServletRequest http) {
        return auth.verifyTwoFactor(
                req.challengeId(),
                req.method(),
                req.code(),
                req.rememberDevice(),
                clientIp(http),
                userAgent(http));
    }

    @PostMapping("/2fa/whatsapp/send")
    public OkResponse sendWhatsappOtp(
            @Valid @RequestBody com.edss.identity.api.dto.WhatsappOtpChallengeRequest req) {
        auth.requestWhatsappOtp(req.challengeId());
        return OkResponse.instance();
    }

    @PostMapping("/forgot-password")
    public OkResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        requirePasswordReset();
        passwordReset.requestReset(req.email());
        return OkResponse.instance();
    }

    @PostMapping("/reset-password")
    public OkResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        requirePasswordReset();
        passwordReset.resetPassword(req.token(), req.newPassword());
        return OkResponse.instance();
    }

    private void requirePasswordReset() {
        if (!features.auth().passwordReset()) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND, "Password reset is disabled on this deployment.");
        }
    }

    private static String clientIp(HttpServletRequest request) {
        return HttpRequests.clientIp(request);
    }

    private static String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
