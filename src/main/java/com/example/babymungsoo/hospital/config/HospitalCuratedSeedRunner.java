package com.example.babymungsoo.hospital.config;

import com.example.babymungsoo.hospital.service.HospitalCuratedSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용: 기동 시 24시간 병원 큐레이션 목록을 DB 에 반영한다.
 *
 * <p>{@link HospitalSeedRunner}(전국 카카오 시드) 뒤에 돈다. 순서를 바꿔도 결과는 같지만
 * (장소 ID 멱등), 시드가 먼저 넣어 둔 행에 24시간 표시만 얹는 쪽이 카카오 호출이 적다.
 *
 * <p>목록이 15곳 남짓이라 매 기동마다 카카오를 그만큼 호출한다. 키가 없거나 호출이
 * 실패해도 기동은 막지 않는다 — 병원 목록이 조금 덜 채워질 뿐이다.
 */
@Slf4j
@Component
@Profile("dev")
@Order(10)
@RequiredArgsConstructor
public class HospitalCuratedSeedRunner implements CommandLineRunner {

    private final HospitalCuratedSeedService hospitalCuratedSeedService;

    @Override
    public void run(String... args) {
        try {
            var result = hospitalCuratedSeedService.seedCurated();
            log.info("24시간 병원 큐레이션 자동 반영 - {}", result);
        } catch (Exception e) {
            log.warn("24시간 병원 큐레이션 반영 실패 - KAKAO_REST_API_KEY 설정을 확인하세요: {}", e.getMessage());
        }
    }
}
