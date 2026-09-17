package com.qoder.terminal.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "qoder.invite-code=demo-invite",
    "qoder.jwt.allow-ephemeral-key=true"
})
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String register(String username) throws Exception {
        String body = """
                {"inviteCode":"demo-invite","username":"%s","password":"s3cret-pass","displayName":"Demo"}
                """.formatted(username);
        String response = mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String uniqueName() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void registerThenLoginThenReadProfile() throws Exception {
        String username = uniqueName();
        register(username);

        String login = mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"s3cret-pass\"}".formatted(username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(login, "$.accessToken");

        mvc.perform(get("/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void rejectsWrongInviteCodeAndDuplicateUsername() throws Exception {
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"inviteCode":"wrong","username":"%s","password":"s3cret-pass","displayName":"X"}
                        """.formatted(uniqueName())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_invite_code"));

        String username = uniqueName();
        register(username);
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"inviteCode":"demo-invite","username":"%s","password":"s3cret-pass","displayName":"X"}
                        """.formatted(username)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordAndUnknownUserLookTheSame() throws Exception {
        String username = uniqueName();
        register(username);
        for (String user : new String[] {username, "nobody_here"}) {
            mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"%s\",\"password\":\"wrong-pass\"}".formatted(user)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("invalid_credentials"));
        }
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/activities")).andExpect(status().isUnauthorized());
        mvc.perform(get("/v1/me").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    void activitiesAreRecordedPerUserNewestFirst() throws Exception {
        String alice = register(uniqueName());
        String bob = register(uniqueName());

        for (String content : new String[] {"700 Q", "ASK 对比腾讯和阿里"}) {
            String type = content.startsWith("ASK") ? "ASK" : "COMMAND";
            mvc.perform(post("/v1/activities").header("Authorization", "Bearer " + alice)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"%s\",\"content\":\"%s\"}".formatted(type, content)))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/v1/activities").header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("ASK"))
                .andExpect(jsonPath("$[1].content").value("700 Q"));

        mvc.perform(get("/v1/activities").header("Authorization", "Bearer " + bob))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidActivity() throws Exception {
        String token = register(uniqueName());
        mvc.perform(post("/v1/activities").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"HACK\",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/activities?limit=0").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tokenVerifiesAgainstPublishedJwks() throws Exception {
        String token = register(uniqueName());
        String jwks = mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        RSAKey publicKey = JWKSet.parse(jwks).getKeys().getFirst().toRSAKey();
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("qoder-terminal-user");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("qoder-terminal");
    }
}
