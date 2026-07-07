package com.example.babymungsoo.global.config;

import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEV_USER_EMAIL = "dev@example.com";
    private static final String DEV_USER_NAME = "개발용 유저";

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(DEV_USER_EMAIL)) {
            return;
        }

        User devUser = User.builder()
                .email(DEV_USER_EMAIL)
                .name(DEV_USER_NAME)
                .build();

        userRepository.save(devUser);
    }
}
