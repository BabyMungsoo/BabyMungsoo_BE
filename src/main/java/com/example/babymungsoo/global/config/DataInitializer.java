package com.example.babymungsoo.global.config;

import com.example.babymungsoo.user.entity.LoginType;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 관리자 계정을 기동 시 한 번 만든다.
 *
 * <p>dev 프로필로 제한하지 않는다. 관리자 API 가 ADMIN 전용이 되면서, 운영에 관리자가
 * 없으면 병원 시드를 아무도 돌릴 수 없어 병원 목록이 빈 채로 남는다.
 *
 * <p>ADMIN_EMAIL / ADMIN_PASSWORD 가 없으면 아무것도 하지 않고, 같은 이메일이 이미 있으면
 * 건너뛴다. 그래서 어느 환경에서 몇 번 기동해도 안전하다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer
        implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.name:관리자}")
    private String adminName;

    @Override
    public void run(String... args) {

        if (
                adminEmail == null
                        || adminEmail.isBlank()
                        || adminPassword == null
                        || adminPassword.isBlank()
        ) {
            return;
        }

        String normalizedEmail =
                adminEmail
                        .trim()
                        .toLowerCase();

        if (
                userRepository
                        .existsByEmail(
                                normalizedEmail
                        )
        ) {
            return;
        }

        User admin =
                User.builder()
                        .email(normalizedEmail)
                        .password(
                                passwordEncoder.encode(
                                        adminPassword
                                )
                        )
                        .name(adminName)
                        .loginType(
                                LoginType.EMAIL
                        )
                        .role(
                                UserRole.ADMIN
                        )
                        .build();

        userRepository.save(admin);
    }
}