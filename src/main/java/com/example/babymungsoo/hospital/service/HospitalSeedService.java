package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.hospital.client.KakaoLocalClient;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카카오 장소검색 결과를 우리 {@code hospital} 테이블로 적재(시드)하는 서비스.
 *
 * <p>카카오 장소 ID를 멱등 키로 써서, 같은 지역을 여러 번 시드해도
 * 이미 있는 병원은 갱신(이름·주소·전화·좌표만)하고 새 병원만 추가한다.
 * is24hour·rating·openHours는 카카오가 주지 않으므로 신규 저장 시 기본값(24시간=false)으로 두고,
 * 이후 운영자가 보정하면 그 값을 시드가 덮어쓰지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalSeedService {

    private final KakaoLocalClient kakaoLocalClient;
    private final HospitalRepository hospitalRepository;

    /**
     * 주어진 좌표 반경의 동물병원을 검색해 DB에 적재한다.
     *
     * @param latitude  중심 위도
     * @param longitude 중심 경도
     * @param radius    반경(m, 0~20000)
     * @return 이번 호출로 새로 추가/갱신된 결과 요약
     */
    @Transactional
    public SeedResult seedAround(double latitude, double longitude, int radius) {
        List<KakaoKeywordResponse.Document> documents =
                kakaoLocalClient.searchAnimalHospitals(latitude, longitude, radius);

        int created = 0;
        int updated = 0;
        int skipped = 0;
        LocalDateTime now = LocalDateTime.now();

        for (KakaoKeywordResponse.Document doc : documents) {
            // 좌표/ID가 비면 저장 의미가 없어 건너뛴다.
            if (!StringUtils.hasText(doc.id())
                    || !StringUtils.hasText(doc.x())
                    || !StringUtils.hasText(doc.y())) {
                skipped++;
                continue;
            }

            double lng = Double.parseDouble(doc.x());
            double lat = Double.parseDouble(doc.y());
            String phone = StringUtils.hasText(doc.phone()) ? doc.phone() : "정보 없음";

            var existing = hospitalRepository.findByKakaoPlaceId(doc.id());
            if (existing.isPresent()) {
                existing.get().updateFromKakao(
                        doc.placeName(), doc.bestAddress(), phone, lat, lng, now);
                updated++;
                continue;
            }

            Hospital hospital = Hospital.builder()
                    .kakaoPlaceId(doc.id())
                    .hospitalName(doc.placeName())
                    .address(doc.bestAddress())
                    .phone(phone)
                    .latitude(lat)
                    .longitude(lng)
                    .is24hour(false)   // 카카오 키워드검색은 24시간 여부를 주지 않음 → 기본 false
                    .openHours(null)
                    .rating(null)
                    .lastUpdated(now)
                    .build();
            hospitalRepository.save(hospital);
            created++;
        }

        SeedResult result = new SeedResult(documents.size(), created, updated, skipped);
        log.info("동물병원 시드 완료 - lat={}, lng={}, radius={}m, {}", latitude, longitude, radius, result);
        return result;
    }

    /**
     * 시드 결과 요약.
     *
     * @param fetched 카카오에서 받은 건수
     * @param created 신규 저장 건수
     * @param updated 기존 갱신 건수
     * @param skipped 좌표/ID 누락으로 건너뛴 건수
     */
    public record SeedResult(int fetched, int created, int updated, int skipped) {
    }
}
