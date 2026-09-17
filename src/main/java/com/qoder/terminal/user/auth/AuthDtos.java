package com.qoder.terminal.user.auth;

import com.qoder.terminal.user.user.UserView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String inviteCode,
            @NotBlank @Pattern(regexp = "^[a-z0-9_]{3,32}$", message = "3-32 位小写字母、数字或下划线") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserView user) {}
}
