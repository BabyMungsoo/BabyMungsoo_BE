package com.example.babymungsoo.user.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.user.dto.response.UserMeResponse;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.repository.UserRepository;
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

        Long currentUserId =
                currentUserProvider
                        .getCurrentUserId();

        User user =
                userRepository
                        .findById(currentUserId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.USER_NOT_FOUND
                                )
                        );

        return UserMeResponse.from(user);
    }
}