package com.example.babymungsoo.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 API 는 로그인만으로 열리면 안 된다.
 * 시드는 카카오를 수백 번 호출하고 병원 DB 를 통째로 갱신한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminEndpointSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("일반 사용자는 병원 시드를 호출할 수 없다")
    @WithMockUser(roles = "USER")
    void userCannotCallHospitalSeed() throws Exception {
        mockMvc.perform(post("/api/v1/admin/hospitals/seed-curated").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비로그인도 막힌다")
    @WithAnonymousUser
    void anonymousCannotCallHospitalSeed() throws Exception {
        mockMvc.perform(post("/api/v1/admin/hospitals/seed-curated").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 펫 목록도 일반 사용자에게 막힌다")
    @WithMockUser(roles = "USER")
    void userCannotCallAdminPets() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/admin/pets"))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf();
    }
}
