package com.qoder.terminal.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT issuing configuration; see qoder.jwt.* in application.properties. */
@ConfigurationProperties("qoder.jwt")
public record JwtProperties(
        String issuer, String audience, Duration ttl, String privateKey, boolean allowEphemeralKey) {}
