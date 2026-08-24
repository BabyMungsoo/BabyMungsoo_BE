package com.example.babymungsoo.auth.dto.response;

import com.example.babymungsoo.user.entity.User;

public record SignupResponse(
        Long userId,
        String email,
        String name
) {

    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}