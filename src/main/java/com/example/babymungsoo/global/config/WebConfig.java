package com.example.babymungsoo.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 허용할 프론트 출처 패턴. 쉼표로 구분한다.
     *
     * 기본값은 로컬 개발용이고, 배포 환경은 CORS_ALLOWED_ORIGIN_PATTERNS 로
     * 실제 프론트 도메인(예: https://*.vercel.app)을 주입한다.
     */
    @Value("${cors.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    /**
     * 웹(브라우저)에서 프론트를 띄울 때 필요한 CORS 설정입니다.
     *
     * Expo 웹 개발 서버는 http://localhost:8081 에서 도는데, 브라우저는 다른 포트를
     * 다른 출처로 보기 때문에 이 허용이 없으면 응답을 받아도 JS 로 전달하지 않습니다.
     * (iOS/Android 앱은 CORS 규칙이 없어서 이 설정과 무관하게 동작합니다.)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
