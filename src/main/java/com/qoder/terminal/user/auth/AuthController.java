package com.qoder.terminal.user.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.qoder.terminal.user.auth.AuthDtos.LoginRequest;
import com.qoder.terminal.user.auth.AuthDtos.RegisterRequest;
import com.qoder.terminal.user.auth.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService auth;
    private final JWKSet publicJwkSet;

    public AuthController(AuthService auth, JWKSet publicJwkSet) {
        this.auth = auth;
        this.publicJwkSet = publicJwkSet;
    }

    @PostMapping("/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/v1/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return publicJwkSet.toJSONObject(true);
    }
}
