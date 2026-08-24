package com.example.babymungsoo.global.security;

import com.example.babymungsoo.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}")
            long accessTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.accessTokenExpirationMs =
                accessTokenExpirationMs;
    }

    public String createAccessToken(User user) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + accessTokenExpirationMs
        );

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);

        return Long.parseLong(
                claims.getSubject()
        );
    }

    public String getRole(String token) {
        return getClaims(token)
                .get("role", String.class);
    }

    public boolean validateToken(String token) {

        try {

            getClaims(token);

            return true;

        } catch (Exception exception) {

            return false;
        }
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}