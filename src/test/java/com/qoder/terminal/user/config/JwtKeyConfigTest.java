package com.qoder.terminal.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtKeyConfigTest {

    private static JwtProperties props(String pem, boolean allowEphemeral) {
        return new JwtProperties("iss", "aud", Duration.ofHours(1), pem, allowEphemeral);
    }

    @Test
    void loadsPkcs8PemPrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        var key = new JwtKeyConfig().rsaKey(props(pem, false));

        assertThat(key.toRSAPublicKey().getModulus()).isEqualTo(((java.security.interfaces.RSAPublicKey) pair.getPublic()).getModulus());
    }

    @Test
    void failsFastWithoutKeyUnlessEphemeralAllowed() {
        assertThatThrownBy(() -> new JwtKeyConfig().rsaKey(props("", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("USER_JWT_PRIVATE_KEY");
        assertThat(new JwtKeyConfig().rsaKey(props("", true)).isPrivate()).isTrue();
    }

    @Test
    void rejectsMalformedPem() {
        assertThatThrownBy(() -> new JwtKeyConfig().rsaKey(props("-----BEGIN PRIVATE KEY-----\nnope\n-----END PRIVATE KEY-----", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PKCS#8");
    }
}
