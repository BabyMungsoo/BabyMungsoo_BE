package com.example.babymungsoo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 웹(브라우저)에서 프론트를 띄울 때 필요한 CORS 설정입니다.
     *
     * Expo 웹 개발 서버는 http://localhost:8081 에서 도는데, 브라우저는 다른 포트를
     * 다른 출처로 보기 때문에 이 허용이 없으면 응답을 받아도 JS 로 전달하지 않습니다.
     * (iOS/Android 앱은 CORS 규칙이 없어서 이 설정과 무관하게 동작합니다.)
     *
     * 배포 시에는 allowedOriginPatterns 를 실제 프론트 도메인으로 좁혀야 합니다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
