package com.example.babymungsoo.user.service;

import com.example.babymungsoo.user.dto.response.UserMeResponse;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.repository.UserRepository;
import com.example.babymungsoo.global.auth.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserMeResponse getCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserMeResponse.from(user);
    }
}
