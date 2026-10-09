package com.example.babymungsoo.auth;

import com.example.babymungsoo.user.entity.*;
import com.example.babymungsoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:emailcheck;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=email-check-test-only-secret-key-at-least-32-bytes",
        "admin.email=", "admin.password=", "claude.api.mock=true"
})
@AutoConfigureMockMvc
class EmailCheckIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        users.save(User.builder().email("member@example.com").name("홍길동")
                .password(encoder.encode("password1!"))
                .loginType(LoginType.EMAIL).role(UserRole.ADMIN).build());
    }

    @Test
    void emailCheckIsPublicAndNormalizesLikeSignup() throws Exception {
        check("{\"email\":\"Member@Example.com\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        check("{\"email\":\"new@example.com\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void emailCheckRejectsInvalidEmail() throws Exception {
        check("{\"email\":\"not-an-email\"}").andExpect(status().isBadRequest());
        check("{}").andExpect(status().isBadRequest());
    }

    @Test
    void loginResponseIncludesRole() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"password1!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
    }

    private org.springframework.test.web.servlet.ResultActions check(String body) throws Exception {
        return mvc.perform(post("/api/v1/auth/email/check")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
