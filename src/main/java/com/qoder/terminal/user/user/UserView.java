package com.qoder.terminal.user.user;

import java.time.Instant;
import java.util.UUID;

/** Public user information, without the password hash. */
public record UserView(UUID id, String username, String displayName, Role role, Instant createdAt) {

    public static UserView of(User user) {
        return new UserView(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
