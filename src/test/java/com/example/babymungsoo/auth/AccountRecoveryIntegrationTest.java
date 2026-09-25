package com.example.babymungsoo.auth;

import com.example.babymungsoo.auth.service.RecoveryMailService;
import com.example.babymungsoo.user.entity.*;
import com.example.babymungsoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean RecoveryMailService mail;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(User.builder().email("member@example.com").name("홍길동")
                .phone("010-1234-5678").password(encoder.encode("oldPassword1"))
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
    void resetTokenIsHashedSingleUseAndChangesLoginPassword() throws Exception {
        String token = requestToken();
        User issued = users.findByEmail("member@example.com").orElseThrow();
        assertThat(issued.getPasswordResetHash()).hasSize(64).isNotEqualTo(token);
        confirm(token, "newPassword1!", 204);
        User updated = users.findByEmail("member@example.com").orElseThrow();
        assertThat(encoder.matches("newPassword1!", updated.getPassword())).isTrue();
        assertThat(updated.getPasswordResetHash()).isNull();
        confirm(token, "anotherPassword1!", 400);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"oldPassword1\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"newPassword1\"}"))
                .andExpect(status().isOk());
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
                                .content("{\"token\":\"" + token + "\",\"newPassword\":\"newPassword1\"}"))
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
    void unknownAccountAndCooldownReturnSameStatusWithoutSendingMail() throws Exception {
        requestToken();
        request("member@example.com");
        request("missing@example.com");
        verify(mail, times(1)).sendResetToken(anyString(), anyString());
    }

    @Test
    void expiredAndInvalidTokensCannotChangePassword() throws Exception {
        String token = requestToken();
        User user = users.findByEmail("member@example.com").orElseThrow();
        user.issuePasswordReset(user.getPasswordResetHash(), Instant.now().minusSeconds(901));
        users.save(user);
        confirm(token, "newPassword1", 400);
        confirm("x".repeat(43), "newPassword1", 400);
        assertThat(encoder.matches("oldPassword1", users.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void resendingInvalidatesPreviousToken() throws Exception {
        String first = requestToken();
        User user = users.findByEmail("member@example.com").orElseThrow();
        user.issuePasswordReset(user.getPasswordResetHash(), Instant.now().minusSeconds(61));
        users.save(user);
        clearInvocations(mail);
        String second = requestToken();
        confirm(first, "newPassword1", 400);
        confirm(second, "newPassword1", 204);
    }

    @Test
    void socialAccountsCannotUseEmailRecovery() throws Exception {
        users.save(User.builder().email("social@example.com").name("소셜사용자")
                .phone("01012345678").loginType(LoginType.GOOGLE).role(UserRole.USER).build());
        request("social@example.com");
        verify(mail, never()).sendResetToken(anyString(), anyString());
        mvc.perform(post("/api/v1/auth/find-id").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"소셜사용자\",\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maskedEmails").isEmpty());
    }

    @Test
    void mailFailureRollsBackTokenIssuance() throws Exception {
        doThrow(new com.example.babymungsoo.global.exception.CustomException(
                com.example.babymungsoo.global.exception.ErrorCode.RECOVERY_MAIL_UNAVAILABLE))
                .when(mail).sendResetToken(anyString(), anyString());
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(users.findByEmail("member@example.com").orElseThrow().getPasswordResetHash()).isNull();
    }

    @Test
    void rejectsInvalidInputAndOversizedUtf8Passwords() throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        String token = requestToken();
        confirm(token, "short", 400);
        confirm(token, "가".repeat(25), 400);
    }

    private void request(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
    }

    private String requestToken() throws Exception {
        request("member@example.com");
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mail).sendResetToken(eq("member@example.com"), token.capture());
        return token.getValue();
    }

    private void confirm(String token, String password, int statusCode) throws Exception {
        mvc.perform(post("/api/v1/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"))
                .andExpect(status().is(statusCode));
    }
}
