package com.qoder.terminal.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 签发配置，见 application.properties 中 qoder.jwt.*。 */
@ConfigurationProperties("qoder.jwt")
public record JwtProperties(
        String issuer, String audience, Duration ttl, String privateKey, boolean allowEphemeralKey) {}
