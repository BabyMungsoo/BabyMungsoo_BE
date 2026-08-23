package com.example.babymungsoo.hospital.config;

import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.example.babymungsoo.hospital.service.HospitalSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용: hospital 테이블이 비어 있으면 서버 기동 시 한 번 전국 시드를 돌린다.
 *
 * <p>각자 로컬 DB를 새로 띄우는 구조라, 이게 없으면 새로 받은 사람마다
 * {@code /api/v1/admin/hospitals/seed-nationwide} 를 직접 호출해야 병원 목록이 뜬다.
 * dev 프로필에서만 동작하고, 카카오 호출이 실패해도(키 미설정 등) 기동을 막지 않는다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class HospitalSeedRunner implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;
    private final HospitalSeedService hospitalSeedService;

    @Override
    public void run(String... args) {
        if (hospitalRepository.count() > 0) {
            return;
        }

        log.info("hospital 테이블이 비어 있어 전국 시드를 자동 실행합니다.");
        try {
            var result = hospitalSeedService.seedNationwide();
            log.info("자동 시드 완료 - {}", result);
        } catch (Exception e) {
            log.warn("자동 시드 실패 - KAKAO_REST_API_KEY 설정을 확인하세요: {}", e.getMessage());
        }
    }
}
