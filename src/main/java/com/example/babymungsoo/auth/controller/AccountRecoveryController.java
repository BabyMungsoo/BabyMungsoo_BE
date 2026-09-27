package com.example.babymungsoo.auth.controller;

import com.example.babymungsoo.auth.dto.request.*;
import com.example.babymungsoo.auth.dto.response.FindIdResponse;
import com.example.babymungsoo.auth.dto.response.PasswordResetTokenResponse;
import com.example.babymungsoo.auth.service.AccountRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계정 복구", description = "아이디 찾기 및 이메일·이름 확인을 통한 비밀번호 재설정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AccountRecoveryController {
    private final AccountRecoveryService recovery;

    @Operation(summary = "이름과 전화번호로 마스킹된 이메일 아이디 찾기")
    @PostMapping("/find-id")
    public FindIdResponse findId(@Valid @RequestBody FindIdRequest request) {
        return recovery.findId(request);
    }

    @Operation(summary = "이메일·이름 확인 후 재설정 토큰 발급",
            description = "메일 인증 없이 이메일과 이름이 일치하면 토큰을 바로 반환합니다. 불일치 시 404. 토큰은 15분간 한 번만 유효합니다.")
    @PostMapping("/password-reset/request")
    public PasswordResetTokenResponse requestReset(@Valid @RequestBody PasswordResetRequest request) {
        return recovery.requestReset(request);
    }

    @Operation(summary = "일회용 토큰으로 비밀번호 재설정")
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        recovery.confirmReset(request);
    }
}
