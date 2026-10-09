package com.example.babymungsoo.global.config;

import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 관리자 계정 생성은 모든 환경에서 돌아야 한다 — 운영에 관리자가 없으면
 * 병원 시드를 아무도 돌릴 수 없다. 대신 설정이 없으면 아무 일도 하지 않아야 한다.
 */
@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("ADMIN_EMAIL / ADMIN_PASSWORD 가 없으면 아무것도 만들지 않는다")
    void doesNothingWithoutConfig() {
        configure("", "");

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정이 있으면 ADMIN 권한으로 만든다 — 비밀번호는 인코딩해서")
    void createsAdminWhenConfigured() {
        configure("Admin@Example.com ", "secret1234");
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret1234")).thenReturn("encoded");

        initializer.run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        // 이메일은 공백을 털고 소문자로 맞춘다 — 로그인 조회와 같은 형태여야 한다
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("이미 있으면 다시 만들지 않는다 — 몇 번 기동해도 같다")
    void skipsWhenAlreadyExists() {
        configure("admin@example.com", "secret1234");
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    private void configure(String email, String password) {
        ReflectionTestUtils.setField(initializer, "adminEmail", email);
        ReflectionTestUtils.setField(initializer, "adminPassword", password);
        ReflectionTestUtils.setField(initializer, "adminName", "관리자");
    }
}
