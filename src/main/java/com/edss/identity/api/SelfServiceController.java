package com.edss.identity.api;

import com.edss.identity.api.dto.BackupCodesResponse;
import com.edss.identity.api.dto.MfaDisableRequest;
import com.edss.identity.api.dto.MfaMethodDto;
import com.edss.identity.api.dto.OkResponse;
import com.edss.identity.api.dto.PasswordChangeRequest;
import com.edss.identity.api.dto.SessionDto;
import com.edss.identity.api.dto.TrustedDeviceDto;
import com.edss.identity.api.dto.TwoFactorCodeRequest;
import com.edss.identity.api.dto.TwoFactorEnrollStartResponse;
import com.edss.identity.api.dto.WhatsappEnrollStartRequest;
import com.edss.identity.application.BackupCodeService;
import com.edss.identity.application.MfaMethodsService;
import com.edss.identity.application.PasswordChangeService;
import com.edss.identity.application.SessionService;
import com.edss.identity.application.TrustedDeviceService;
import com.edss.identity.domain.MfaMethod;
import com.edss.identity.domain.MfaMethodType;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.config.FeaturesProperties;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service account management for the authenticated user: multi-method
 * 2FA enrollment + disablement, password change, session inspection +
 * revocation, trusted-device list + revocation.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@PreAuthorize("isAuthenticated()")
@Tag(name = "self-service", description = "Authenticated user account settings.")
public class SelfServiceController {

    private final UserRepository users;
    private final MfaMethodsService mfa;
    private final BackupCodeService backupCodes;
    private final PasswordChangeService passwordChange;
    private final SessionService sessionService;
    private final TrustedDeviceService trustedDevices;
    private final FeaturesProperties features;

    public SelfServiceController(
            UserRepository users,
            MfaMethodsService mfa,
            BackupCodeService backupCodes,
            PasswordChangeService passwordChange,
            SessionService sessionService,
            TrustedDeviceService trustedDevices,
            FeaturesProperties features) {
        this.users = users;
        this.mfa = mfa;
        this.backupCodes = backupCodes;
        this.passwordChange = passwordChange;
        this.sessionService = sessionService;
        this.trustedDevices = trustedDevices;
        this.features = features;
    }

    // ---------------------------------------------------------------------
    // 2FA methods listing + disable
    // ---------------------------------------------------------------------

    @GetMapping("/2fa/methods")
    public List<MfaMethodDto> listMethods(@AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = principal.userId();
        return mfa.listAll(userId).stream()
                .map(
                        m ->
                                new MfaMethodDto(
                                        m.getId(),
                                        m.getMethodType().wire(),
                                        m.isEnabled(),
                                        m.getPhoneE164(),
                                        m.getEnrolledAt(),
                                        m.getMethodType() == MfaMethodType.BACKUP_CODE
                                                ? backupCodes.remaining(userId)
                                                : null))
                .toList();
    }

    @DeleteMapping("/2fa/methods/{methodType}")
    public OkResponse disableMethod(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String methodType,
            @Valid @RequestBody MfaDisableRequest req) {
        mfa.disable(principal.userId(), MfaMethodType.ofWire(methodType), req.password());
        return OkResponse.instance();
    }

    // ---------------------------------------------------------------------
    // TOTP enrollment
    // ---------------------------------------------------------------------

    @PostMapping("/2fa/totp/enroll/start")
    public TwoFactorEnrollStartResponse startTotpEnrollment(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        requireEnrollmentEnabled();
        UUID userId = principal.userId();
        String email =
                users.findById(userId)
                        .map(u -> u.getEmail())
                        .orElseThrow(
                                () -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        MfaMethodsService.TotpEnrollmentStart start = mfa.startTotpEnrollment(userId, email);
        return new TwoFactorEnrollStartResponse(
                start.secret(), start.otpauthUri(), start.qrCodePngBase64());
    }

    @PostMapping("/2fa/totp/enroll/verify")
    public OkResponse verifyTotpEnrollment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TwoFactorCodeRequest req) {
        requireEnrollmentEnabled();
        mfa.verifyTotpEnrollment(principal.userId(), req.code());
        return OkResponse.instance();
    }

    // ---------------------------------------------------------------------
    // WhatsApp OTP enrollment
    // ---------------------------------------------------------------------

    @PostMapping("/2fa/whatsapp/enroll/start")
    public OkResponse startWhatsappEnrollment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody WhatsappEnrollStartRequest req) {
        requireEnrollmentEnabled();
        mfa.startWhatsappEnrollment(principal.userId(), req.phoneE164());
        return OkResponse.instance();
    }

    @PostMapping("/2fa/whatsapp/enroll/verify")
    public OkResponse verifyWhatsappEnrollment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody TwoFactorCodeRequest req) {
        requireEnrollmentEnabled();
        mfa.verifyWhatsappEnrollment(principal.userId(), req.code());
        return OkResponse.instance();
    }

    // ---------------------------------------------------------------------
    // Backup codes
    // ---------------------------------------------------------------------

    @PostMapping("/2fa/backup-codes/regenerate")
    public BackupCodesResponse regenerateBackupCodes(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        requireEnrollmentEnabled();
        return new BackupCodesResponse(mfa.regenerateBackupCodes(principal.userId()));
    }

    // ---------------------------------------------------------------------
    // Password change + sessions + trusted devices
    // ---------------------------------------------------------------------

    @PostMapping("/password/change")
    public OkResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PasswordChangeRequest req) {
        passwordChange.changePassword(principal.userId(), req.currentPassword(), req.newPassword());
        return OkResponse.instance();
    }

    @GetMapping("/sessions")
    public List<SessionDto> listSessions(@AuthenticationPrincipal AuthenticatedUser principal) {
        UUID current = principal.sessionId();
        return sessionService.listActive(principal.userId()).stream()
                .map(
                        s ->
                                new SessionDto(
                                        s.getId(),
                                        s.getUserAgent(),
                                        s.getIpAddress(),
                                        s.getCreatedAt(),
                                        s.getLastActiveAt(),
                                        s.getId().equals(current)))
                .toList();
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID sessionId) {
        sessionService.revoke(principal.userId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trusted-devices")
    public List<TrustedDeviceDto> listDevices(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return trustedDevices.list(principal.userId()).stream()
                .map(
                        d ->
                                new TrustedDeviceDto(
                                        d.getId(),
                                        d.getUserAgent(),
                                        d.getIpAddress(),
                                        d.getCreatedAt(),
                                        d.getExpiresAt()))
                .toList();
    }

    @DeleteMapping("/trusted-devices/{deviceId}")
    public ResponseEntity<Void> revokeDevice(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID deviceId) {
        trustedDevices.revoke(principal.userId(), deviceId);
        return ResponseEntity.noContent().build();
    }

    private void requireEnrollmentEnabled() {
        if (!features.auth().twoFactorEnrollment()) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND, "2FA enrollment is disabled on this deployment.");
        }
    }
}
