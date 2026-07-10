package com.edss.identity.application;

import com.edss.identity.domain.User;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password change for an already-authenticated user. Requires the current
 * password so a hijacked session cannot lock the real user out. New password
 * enforces the same policy as {@code ResetPasswordRequest} (12+ chars, mixed
 * classes) — validation happens on the DTO.
 */
@Service
@Transactional
public class PasswordChangeService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordChangeService(
            UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(
                    ApiErrorCode.INVALID_CREDENTIALS, "Current password incorrect.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_FAILED,
                    "New password must be different from the current one.");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword), clock.instant());
    }
}
