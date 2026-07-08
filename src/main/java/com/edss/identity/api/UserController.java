package com.edss.identity.api;

import com.edss.identity.api.dto.UserDto;
import com.edss.identity.infrastructure.UserRepository;
import com.edss.shared.api.ApiErrorCode;
import com.edss.shared.api.ApiException;
import com.edss.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "users", description = "Current user account info.")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.SESSION_EXPIRED, "Session expired.");
        }
        return users.findById(principal.userId())
                .map(
                        u ->
                                new UserDto(
                                        u.getId(),
                                        u.getEmail(),
                                        u.getName(),
                                        u.getAvatarUrl(),
                                        u.getPrimaryRole(),
                                        u.isHasBothRoles(),
                                        u.getCreatedAt()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "User not found."));
    }
}
