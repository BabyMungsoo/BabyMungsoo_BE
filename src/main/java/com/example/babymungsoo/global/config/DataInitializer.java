package com.example.babymungsoo.global.config;

import com.example.babymungsoo.user.entity.LoginType;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("dev")
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