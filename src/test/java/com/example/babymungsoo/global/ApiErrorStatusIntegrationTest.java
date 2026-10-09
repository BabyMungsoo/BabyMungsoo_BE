package com.example.babymungsoo.global;

import com.example.babymungsoo.global.security.JwtTokenProvider;
import com.example.babymungsoo.user.entity.*;
import com.example.babymungsoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 없는 API·지원하지 않는 메서드가 500 으로 뭉개지지 않고 404/405 로 응답하는지 확인한다. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:apierror;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=api-error-status-test-only-secret-key-32-bytes",
        "admin.email=", "admin.password=", "claude.api.mock=true"
})
@AutoConfigureMockMvc
class ApiErrorStatusIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider jwt;

    private String token;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        User user = users.save(User.builder().email("member@example.com").name("홍길동")
                .password("encoded").loginType(LoginType.EMAIL).role(UserRole.USER).build());
        token = "Bearer " + jwt.createAccessToken(user);
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(put("/api/v1/users/me").header("Authorization", token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
    }

    @Test
    void unknownApiReturns404() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 API입니다."));
    }
}
