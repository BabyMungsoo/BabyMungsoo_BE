package com.example.babymungsoo.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "user_pwd", length = 255)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50)
    private String name;

    @Column(name = "user_phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    private LoginType loginType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void changePassword(
            String encodedPassword
    ) {
        this.password = encodedPassword;
    }
}