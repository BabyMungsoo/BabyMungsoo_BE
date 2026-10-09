package com.example.babymungsoo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        String token,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(
                min = 8,
                max = 64,
                message = "비밀번호는 8자 이상 64자 이하여야 합니다."
        )
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\\p{Punct}])\\S+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함하고 공백이 없어야 합니다."
        )
        String newPassword
) {}