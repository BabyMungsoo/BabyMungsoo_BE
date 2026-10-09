package com.example.babymungsoo.global;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 무중단 배포 스크립트(deploy.sh)는 토큰 없이 /actuator/health 가 UP 인지 보고 트래픽을 넘긴다.
 * 세부 정보(DB 종류 등)와 다른 actuator 엔드포인트는 외부에 열리면 안 된다.
 */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:actuatorhealth;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=actuator-health-test-only-secret-key-32-bytes",
        "admin.email=", "admin.password=", "claude.api.mock=true"
})
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndHidesDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(content().string(not(containsString("components"))));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());
    }
}
