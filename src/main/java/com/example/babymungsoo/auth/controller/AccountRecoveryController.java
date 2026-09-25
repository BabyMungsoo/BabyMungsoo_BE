package com.example.babymungsoo.auth.controller;

import com.example.babymungsoo.auth.dto.request.*;
import com.example.babymungsoo.auth.dto.response.FindIdResponse;
import com.example.babymungsoo.auth.service.AccountRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "계정 복구", description = "아이디 찾기 및 이메일 인증을 통한 비밀번호 재설정")
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

    @Operation(summary = "비밀번호 재설정 메일 요청", description = "가입 여부와 관계없이 202 응답. 토큰은 15분간 유효하며 재발송 간격은 60초입니다.")
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestReset(@Valid @RequestBody PasswordResetRequest request) {
        recovery.requestReset(request);
    }

    @Operation(summary = "일회용 토큰으로 비밀번호 재설정")
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        recovery.confirmReset(request);
    }
}
