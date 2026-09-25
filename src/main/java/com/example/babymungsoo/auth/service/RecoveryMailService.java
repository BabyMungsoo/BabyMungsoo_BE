package com.example.babymungsoo.auth.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class RecoveryMailService {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;

    public RecoveryMailService(ObjectProvider<JavaMailSender> sender,
                               @Value("${recovery.mail-from:}") String from,
                               @Value("${spring.mail.host:}") String host) {
        this.sender = sender;
        this.from = from;
        this.host = host;
    }

    public void checkAvailable() {
        if (from.isBlank() || host.isBlank() || sender.getIfAvailable() == null) {
            throw new CustomException(ErrorCode.RECOVERY_MAIL_UNAVAILABLE);
        }
    }

    public void sendResetToken(String email, String token) {
        checkAvailable();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[아기뭉수] 비밀번호 재설정");
        message.setText("비밀번호 재설정 화면에 아래 토큰을 입력해 주세요.\n\n" + token
                + "\n\n15분 동안 한 번만 사용할 수 있습니다. 요청하지 않았다면 이 메일을 무시하세요.");
        try {
            sender.getObject().send(message);
        } catch (MailException e) {
            throw new CustomException(ErrorCode.RECOVERY_MAIL_UNAVAILABLE);
        }
    }
}
