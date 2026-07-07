package com.example.babymungsoo.user.dto.response;

import com.example.babymungsoo.user.entity.User;

public record UserMeResponse(
        Long id,
        String email,
        String name
) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}