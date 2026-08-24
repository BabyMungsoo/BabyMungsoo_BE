package com.example.babymungsoo.auth.service;

import com.example.babymungsoo.auth.dto.request.LoginRequest;
import com.example.babymungsoo.auth.dto.request.SignupRequest;
import com.example.babymungsoo.auth.dto.response.LoginResponse;
import com.example.babymungsoo.auth.dto.response.SignupResponse;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.global.security.JwtTokenProvider;
import com.example.babymungsoo.user.entity.LoginType;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(
            SignupRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        if (
                userRepository.existsByEmail(email)
        ) {
            throw new CustomException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        String encodedPassword =
                passwordEncoder.encode(
                        request.password()
                );

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .name(request.name().trim())
                .phone(normalizeNullable(
                        request.phone()
                ))
                .loginType(LoginType.EMAIL)
                .role(UserRole.USER)
                .build();

        User savedUser =
                userRepository.save(user);

        return SignupResponse.from(
                savedUser
        );
    }

    public LoginResponse login(
            LoginRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.INVALID_CREDENTIALS
                                )
                        );

        if (
                user.getLoginType()
                        != LoginType.EMAIL
        ) {
            throw new CustomException(
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        if (
                user.getPassword() == null
                        || !passwordEncoder.matches(
                        request.password(),
                        user.getPassword()
                )
        ) {
            throw new CustomException(
                    ErrorCode.INVALID_CREDENTIALS
            );
        }

        String accessToken =
                jwtTokenProvider
                        .createAccessToken(user);

        return LoginResponse.of(
                user,
                accessToken
        );
    }

    private String normalizeNullable(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    public void logout() {
        // JWT Stateless 인증 방식이므로
        // 서버에서 별도 세션을 제거하지 않는다.
        // 클라이언트에서 Access Token을 삭제하면 로그아웃된다.
    }
}