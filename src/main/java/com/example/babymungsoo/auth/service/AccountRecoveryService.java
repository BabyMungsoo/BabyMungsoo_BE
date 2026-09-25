package com.example.babymungsoo.auth.service;

import com.example.babymungsoo.auth.dto.request.*;
import com.example.babymungsoo.auth.dto.response.FindIdResponse;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.user.entity.LoginType;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRecoveryService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryMailService mail;
    private static final SecureRandom RANDOM = new SecureRandom();

    public FindIdResponse findId(FindIdRequest request) {
        String phone = normalizePhone(request.phone());
        if (phone.length() < 7) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new FindIdResponse(users.findByNameAndLoginType(request.name().trim(), LoginType.EMAIL)
                .stream().filter(user -> phone.equals(normalizePhone(user.getPhone())))
                .map(user -> maskEmail(user.getEmail())).distinct().sorted().toList());
    }

    @Transactional
    public void requestReset(PasswordResetRequest request) {
        // Check configuration independently of account existence.
        mail.checkAvailable();
        User user = users.findByEmailForUpdate(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || user.getLoginType() != LoginType.EMAIL) {
            return;
        }
        Instant now = Instant.now();
        if (user.getPasswordResetRequestedAt() != null
                && now.isBefore(user.getPasswordResetRequestedAt().plusSeconds(60))) {
            return;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        user.issuePasswordReset(hash(token), now);
        mail.sendResetToken(user.getEmail(), token);
    }

    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        // BCrypt accepts at most 72 bytes, including multibyte passwords.
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        User user = users.findByPasswordResetHashForUpdate(hash(request.token()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        if (user.getLoginType() != LoginType.EMAIL || user.getPasswordResetExpiresAt() == null
                || !Instant.now().isBefore(user.getPasswordResetExpiresAt())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        user.resetPassword(passwordEncoder.encode(request.newPassword()));
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        int visible = Math.min(2, Math.max(0, at - 1));
        return email.substring(0, visible) + "***" + email.substring(at);
    }
}
