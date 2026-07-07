package com.example.babymungsoo.global.auth;

import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevCurrentUserProvider implements CurrentUserProvider {

    private static final String DEV_USER_EMAIL = "dev@example.com";

    private final UserRepository userRepository;

    @Override
    public Long getCurrentUserId() {
        User devUser = userRepository.findByEmail(DEV_USER_EMAIL)
                .orElseThrow(() -> new IllegalStateException("개발용 유저가 존재하지 않습니다."));

        return devUser.getId();
    }
}
