package com.example.babymungsoo.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(
                        Customizer.withDefaults()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/api/v1/auth/signup",
                                        "/api/v1/auth/login",
                                        "/api/v1/media/public/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/api-docs/**"
                                )
                                .permitAll()

                                // 관리자 전용. 시드는 카카오를 수백 번 호출하고 병원 DB 를 통째로
                                // 갱신하므로, 로그인했다는 것만으로 열어 두면 아무나 쿼터를 태우고
                                // 데이터를 흔들 수 있다. 서비스 쪽 validateAdmin 과 이중으로 막는다.
                                .requestMatchers("/api/v1/admin/**")
                                .hasRole("ADMIN")

                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}