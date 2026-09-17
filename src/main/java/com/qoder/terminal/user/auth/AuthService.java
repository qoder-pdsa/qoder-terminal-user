package com.qoder.terminal.user.auth;

import com.qoder.terminal.user.auth.AuthDtos.LoginRequest;
import com.qoder.terminal.user.auth.AuthDtos.RegisterRequest;
import com.qoder.terminal.user.auth.AuthDtos.TokenResponse;
import com.qoder.terminal.user.common.ApiException;
import com.qoder.terminal.user.user.Role;
import com.qoder.terminal.user.user.User;
import com.qoder.terminal.user.user.UserRepository;
import com.qoder.terminal.user.user.UserView;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final Clock clock;
    private final String inviteCode;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            TokenService tokens,
            Clock clock,
            @Value("${qoder.invite-code:}") String inviteCode) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.clock = clock;
        this.inviteCode = inviteCode;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (inviteCode.isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "registration_closed", "registration is disabled");
        }
        if (!constantTimeEquals(inviteCode, req.inviteCode())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "invalid_invite_code", "invalid invite code");
        }
        if (users.existsByUsername(req.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "username_taken", "username already exists");
        }
        User user = users.save(new User(
                req.username(), passwords.encode(req.password()), req.displayName(), Role.USER, clock.instant()));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        return users.findByUsername(req.username())
                .filter(user -> passwords.matches(req.password(), user.getPasswordHash()))
                .map(this::tokenFor)
                // 用户不存在与密码错误返回相同错误，避免枚举账号
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "invalid_credentials", "invalid username or password"));
    }

    private TokenResponse tokenFor(User user) {
        return new TokenResponse(tokens.issue(user), "Bearer", tokens.ttlSeconds(), UserView.of(user));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
