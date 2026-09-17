package com.qoder.terminal.user.user;

import java.time.Instant;
import java.util.UUID;

/** 对外暴露的用户信息，不含密码哈希。 */
public record UserView(UUID id, String username, String displayName, Role role, Instant createdAt) {

    public static UserView of(User user) {
        return new UserView(
                user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
