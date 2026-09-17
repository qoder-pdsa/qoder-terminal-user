package com.qoder.terminal.user.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** RSA signing key: read from a PEM environment variable in production; local development may generate an ephemeral key. */
@Configuration
public class JwtKeyConfig {

    static final String KEY_ID = "qoder-terminal-user-1";
    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    RSAKey rsaKey(JwtProperties props) {
        KeyPair pair = hasText(props.privateKey()) ? fromPem(props.privateKey()) : ephemeral(props);
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey(pair.getPrivate())
                .keyID(KEY_ID)
                .build();
    }

    @Bean
    JWKSet publicJwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new IllegalStateException("invalid RSA public key", e);
        }
    }

    private static KeyPair ephemeral(JwtProperties props) {
        if (!props.allowEphemeralKey()) {
            throw new IllegalStateException(
                    "USER_JWT_PRIVATE_KEY is required (set USER_JWT_ALLOW_EPHEMERAL=true only for local dev)");
        }
        log.warn("Using an ephemeral JWT signing key; tokens become invalid after restart");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }

    static KeyPair fromPem(String pem) {
        String body = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey)
                    factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("USER_JWT_PRIVATE_KEY must be a PKCS#8 RSA private key in PEM format", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
