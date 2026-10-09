package com.example.babymungsoo.auth.dto.response;

/** 이메일·이름 확인 후 발급하는 일회용 재설정 토큰. 15분 동안 한 번만 쓸 수 있다. */
public record PasswordResetTokenResponse(String resetToken) {}
