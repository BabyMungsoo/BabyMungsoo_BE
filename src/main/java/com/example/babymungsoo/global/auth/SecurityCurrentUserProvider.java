package com.example.babymungsoo.global.auth;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider
        implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof Long userId)) {
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        return userId;
    }
}