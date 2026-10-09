package com.example.babymungsoo.auth;

import com.example.babymungsoo.user.entity.*;
import com.example.babymungsoo.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:recovery;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=recovery-test-only-secret-key-that-is-at-least-32-bytes",
        "admin.email=", "admin.password=", "claude.api.mock=true"
})
@AutoConfigureMockMvc
class AccountRecoveryIntegrationTest {
    private static final String NEW_PASSWORD = "newPassword1!";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(User.builder().email("member@example.com").name("홍길동")
                .phone("010-1234-5678").password(encoder.encode("oldPassword1!"))
                .loginType(LoginType.EMAIL).role(UserRole.USER).build());
    }

    @Test
    void findIdIsPublicAndOnlyReturnsMaskedEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/find-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maskedEmails[0]").value("me***@example.com"));
        mvc.perform(post("/api/v1/auth/find-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phone\":\"01099999999\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maskedEmails").isEmpty());
    }

    @Test
    void emailAndNameMatchIssuesTokenThatChangesLoginPassword() throws Exception {
        String token = requestToken();
        User issued = users.findByEmail("member@example.com").orElseThrow();
        assertThat(issued.getPasswordResetHash()).hasSize(64).isNotEqualTo(token);
        confirm(token, NEW_PASSWORD, 204);
        User updated = users.findByEmail("member@example.com").orElseThrow();
        assertThat(encoder.matches(NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(updated.getPasswordResetHash()).isNull();
        confirm(token, "anotherPassword1!", 400);
        login("oldPassword1!", 401);
        login(NEW_PASSWORD, 200);
    }

    @Test
    void mismatchedNameUnknownEmailAndSocialAccountsReturn404WithoutIssuingToken() throws Exception {
        users.save(User.builder().email("social@example.com").name("소셜사용자")
                .loginType(LoginType.GOOGLE).role(UserRole.USER).build());
        request("member@example.com", "김철수").andExpect(status().isNotFound());
        request("missing@example.com", "홍길동").andExpect(status().isNotFound());
        request("social@example.com", "소셜사용자").andExpect(status().isNotFound());
        assertThat(users.findByEmail("member@example.com").orElseThrow().getPasswordResetHash()).isNull();
    }

    @Test
    void emailIsCaseInsensitiveAndNameIsTrimmed() throws Exception {
        request("Member@Example.com", "  홍길동 ").andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").isString());
    }

    @Test
    void concurrentConfirmRequestsOnlySucceedOnce() throws Exception {
        String token = requestToken();
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> confirm = () -> {
                start.await();
                return mvc.perform(post("/api/v1/auth/password-reset/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                        .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(confirm);
            var second = executor.submit(confirm);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(204, 400);
        }
    }

    @Test
    void expiredAndInvalidTokensCannotChangePassword() throws Exception {
        String token = requestToken();
        User user = users.findByEmail("member@example.com").orElseThrow();
        user.issuePasswordReset(user.getPasswordResetHash(), Instant.now().minusSeconds(901));
        users.save(user);
        confirm(token, NEW_PASSWORD, 400);
        confirm("x".repeat(43), NEW_PASSWORD, 400);
        assertThat(encoder.matches("oldPassword1!", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void reissuingInvalidatesPreviousToken() throws Exception {
        String first = requestToken();
        String second = requestToken();
        confirm(first, NEW_PASSWORD, 400);
        confirm(second, NEW_PASSWORD, 204);
    }

    @Test
    void rejectsInvalidInputAndOversizedUtf8Passwords() throws Exception {
        request("invalid", "홍길동").andExpect(status().isBadRequest());
        request("member@example.com", " ").andExpect(status().isBadRequest());
        String token = requestToken();
        confirm(token, "short", 400);
        confirm(token, "noSpecial123", 400);
        confirm(token, "가".repeat(25) + "a1!", 400);
    }

    private org.springframework.test.web.servlet.ResultActions request(String email, String name) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"name\":\"" + name + "\"}"));
    }

    private String requestToken() throws Exception {
        String body = request("member@example.com", "홍길동").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.resetToken");
    }

    private void confirm(String token, String password, int statusCode) throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"))
                .andExpect(status().is(statusCode));
    }

    private void login(String password, int statusCode) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"" + password + "\"}"))
                .andExpect(status().is(statusCode));
    }
}
