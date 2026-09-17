package com.qoder.terminal.user.auth;

import com.qoder.terminal.user.config.JwtProperties;
import com.qoder.terminal.user.user.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues RS256 access tokens; data / analyst verify them via /.well-known/jwks.json. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    public String issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .audience(List.of(props.audience()))
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.ttl()))
                .claim("preferred_username", user.getUsername())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return props.ttl().toSeconds();
    }
}
