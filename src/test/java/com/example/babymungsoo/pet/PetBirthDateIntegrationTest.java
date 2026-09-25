package com.example.babymungsoo.pet;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.pet.repository.PetRepository;
import com.example.babymungsoo.user.entity.*;
import com.example.babymungsoo.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
 "spring.config.import=", "spring.profiles.active=test",
 "spring.datasource.url=jdbc:h2:mem:pet-birth;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver",
 "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop",
 "jwt.secret=birth-test-only-secret-key-that-is-at-least-32-bytes",
 "admin.email=", "admin.password=", "claude.api.mock=true"
})
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class PetBirthDateIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired UserRepository users;
 @Autowired PetRepository pets;
 @Autowired EntityManager em;
 @MockitoBean CurrentUserProvider currentUser;

 @BeforeEach void setup() {
  User user = users.save(User.builder().email("birth@example.com").name("보호자")
    .password("test-only").loginType(LoginType.EMAIL).role(UserRole.USER).build());
  when(currentUser.getCurrentUserId()).thenReturn(user.getId());
 }

 private Long create(LocalDate birthday) throws Exception {
  mvc.perform(post("/api/v1/pets").contentType(MediaType.APPLICATION_JSON).content(
    "{\"name\":\"몽수\",\"breed\":\"푸들\",\"age\":9,\"birthDate\":\"" + birthday + "\",\"gender\":\"MALE\",\"isNeutered\":false}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.birthDate").value(birthday.toString()));
  em.flush(); em.clear();
  return pets.findAll().getFirst().getId();
 }

 @Test void birthdaySurvivesSaveAndReadAndDrivesAge() throws Exception {
  LocalDate birthday = LocalDate.now().minusMonths(6);
  Long id = create(birthday);
  assertThat(pets.findById(id).orElseThrow().getBirthDate()).isEqualTo(birthday);
  mvc.perform(get("/api/v1/pets/" + id)).andExpect(status().isOk())
    .andExpect(jsonPath("$.age").value(0)).andExpect(jsonPath("$.birthDate").value(birthday.toString()));
  mvc.perform(get("/api/v1/pets")).andExpect(jsonPath("$[0].birthDate").value(birthday.toString()));
  mvc.perform(get("/api/v1/pets/" + id + "/profile"))
    .andExpect(jsonPath("$.basicRiskInfo").value(org.hamcrest.Matchers.containsString("6개월")));
 }

 @Test void patchPreservesBirthdayUnlessAgeModeChanges() throws Exception {
  Long id = create(LocalDate.now().minusMonths(6));
  mvc.perform(patch("/api/v1/pets/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"수정된 이름\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.birthDate").isNotEmpty());
  LocalDate birthday = LocalDate.now().minusYears(2);
  mvc.perform(patch("/api/v1/pets/" + id).contentType(MediaType.APPLICATION_JSON)
    .content("{\"age\":0,\"birthDate\":\"" + birthday + "\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.age").value(2));
  mvc.perform(patch("/api/v1/pets/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"age\":3,\"birthDate\":null}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.age").value(3)).andExpect(jsonPath("$.birthDate").isEmpty());
  em.flush(); em.clear();
  assertThat(pets.findById(id).orElseThrow().getBirthDate()).isNull();
 }

 @Test void futureBirthdayAndInvalidDateAreRejected() throws Exception {
  Long id = create(LocalDate.now());
  for (String date : new String[]{LocalDate.now().plusDays(1).toString(), "2026-02-30"}) {
   mvc.perform(patch("/api/v1/pets/" + id).contentType(MediaType.APPLICATION_JSON)
     .content("{\"birthDate\":\"" + date + "\"}"))
     .andExpect(status().isBadRequest());
  }
 }

 @Test void legacyAgeOnlyRequestStillWorks() throws Exception {
  mvc.perform(post("/api/v1/pets").contentType(MediaType.APPLICATION_JSON)
    .content("{\"name\":\"몽수\",\"breed\":\"푸들\",\"age\":4,\"gender\":\"MALE\",\"isNeutered\":false}"))
    .andExpect(status().isCreated()).andExpect(jsonPath("$.age").value(4)).andExpect(jsonPath("$.birthDate").isEmpty());
 }
}
