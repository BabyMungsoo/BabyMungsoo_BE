package com.example.babymungsoo.auth.dto.response;

import com.example.babymungsoo.user.entity.User;

public record LoginResponse(
        Long userId,
        String email,
        String name,
        String accessToken,
        String tokenType
) {

    public static LoginResponse of(
            User user,
            String accessToken
    ) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                accessToken,
                "Bearer"
        );
    }
}