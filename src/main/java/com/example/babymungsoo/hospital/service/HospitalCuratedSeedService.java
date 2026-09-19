package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.client.KakaoLocalClient;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 수기로 확인한 24시간 동물병원 목록({@code resources/data/hospitals-24h-*.json})을
 * {@code hospital} 테이블에 반영하는 서비스.
 *
 * <p>카카오 키워드검색은 영업시간을 주지 않아 {@link HospitalSeedService} 는 상호명에
 * '24시'가 있는 곳만 24시간으로 잡는다. 'OO동물의료센터'처럼 이름에 24가 없는 24시간 병원은
 * 그 방식으로는 영영 안 잡히므로, 이 목록으로 보완한다.
 *
 * <p>목록에는 상호명·구·전화·운영시간·진료 분야만 두고, 좌표·주소·장소 ID 는 기동 시
 * 카카오 검색으로 확정한다. 블로그 등에서 옮겨 적은 주소는 오타나 옛 주소일 수 있어
 * 그대로 저장하지 않는다. 카카오에서 못 찾는 병원은 저장하지 않고 로그만 남긴다 —
 * 좌표 없는 병원은 지도에 못 올리고, 틀린 좌표는 응급 상황에서 더 해롭다.
 *
 * <p>장소 ID 기준 멱등이라 여러 번 실행해도 행이 늘지 않는다. 이미 시드된 병원이면
 * 24시간 표시와 운영시간·진료 분야만 덧씌운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalCuratedSeedService {

    /** 큐레이션 파일 목록. 다른 도시를 추가하면 여기에 경로를 더한다. */
    private static final List<String> CURATED_FILES = List.of(
            "data/hospitals-24h-seoul.json"
    );

    private final KakaoLocalClient kakaoLocalClient;
    private final HospitalRepository hospitalRepository;
    private final ObjectMapper objectMapper;

    /**
     * 큐레이션 파일을 전부 읽어 DB 에 반영한다.
     *
     * @return 반영 결과 요약. 카카오에서 못 찾은 병원 이름은 {@code skippedNames} 로 돌려준다.
     */
    @Transactional
    public CuratedSeedResult seedCurated() {
        int created = 0;
        int updated = 0;
        List<String> skippedNames = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        List<CuratedHospital> entries = loadEntries();

        for (CuratedHospital entry : entries) {
            KakaoKeywordResponse.Document doc = resolve(entry);
            if (doc == null) {
                skippedNames.add(entry.name());
                continue;
            }

            var existing = hospitalRepository.findByKakaoPlaceId(doc.id());
            if (existing.isPresent()) {
                existing.get().applyCurated(entry.openHours(), entry.specialties(), entry.features(), entry.phone(), now);
                updated++;
                continue;
            }

            Hospital hospital = Hospital.builder()
                    .kakaoPlaceId(doc.id())
                    .hospitalName(doc.placeName())
                    .address(doc.bestAddress())
                    .phone(StringUtils.hasText(doc.phone()) ? doc.phone() : "정보 없음")
                    .latitude(Double.parseDouble(doc.y()))
                    .longitude(Double.parseDouble(doc.x()))
                    .is24hour(true)
                    .lastUpdated(now)
                    .build();
            hospital.applyCurated(entry.openHours(), entry.specialties(), entry.features(), entry.phone(), now);
            hospitalRepository.save(hospital);
            created++;
        }

        CuratedSeedResult result = new CuratedSeedResult(entries.size(), created, updated, skippedNames);
        log.info("24시간 병원 큐레이션 반영 완료 - {}", result);
        if (!skippedNames.isEmpty()) {
            log.warn("카카오에서 찾지 못해 건너뛴 병원 {}건: {}", skippedNames.size(), skippedNames);
        }
        return result;
    }

    /**
     * 카카오 키워드검색으로 목록 한 줄에 해당하는 장소를 찾는다.
     *
     * <p>키워드 검색은 좌표 없이 전국을 대상으로 하므로 이름이 비슷한 다른 지역 병원이
     * 앞에 올 수 있다. 주소에 해당 구가 들어 있는 첫 결과만 받아들이고, 없으면 null 을 준다.
     * 좌표가 비어 있는 결과도 지도에 못 올리므로 버린다.
     */
    private KakaoKeywordResponse.Document resolve(CuratedHospital entry) {
        List<KakaoKeywordResponse.Document> candidates;
        try {
            candidates = kakaoLocalClient.searchByName(entry.keyword());
        } catch (CustomException e) {
            // 키 미설정은 첫 호출에서 바로 드러나므로 전체를 중단한다. 그 외 호출 실패는 이 병원만 건너뛴다.
            if (e.getErrorCode() == ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED) {
                throw e;
            }
            log.warn("카카오 검색 실패 - {}: {}", entry.name(), e.getMessage());
            return null;
        }

        return candidates.stream()
                .filter(doc -> StringUtils.hasText(doc.id())
                        && StringUtils.hasText(doc.x())
                        && StringUtils.hasText(doc.y()))
                .filter(doc -> matchesDistrict(doc, entry.district()))
                .findFirst()
                .orElse(null);
    }

    /** 도로명·지번 주소 어느 쪽에든 구 이름이 있으면 같은 지역으로 본다. */
    private static boolean matchesDistrict(KakaoKeywordResponse.Document doc, String district) {
        if (!StringUtils.hasText(district)) {
            return true;
        }
        return containsText(doc.roadAddressName(), district) || containsText(doc.addressName(), district);
    }

    private static boolean containsText(String text, String needle) {
        return text != null && text.contains(needle);
    }

    private List<CuratedHospital> loadEntries() {
        List<CuratedHospital> all = new ArrayList<>();
        for (String path : CURATED_FILES) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                CuratedFile file = objectMapper.readValue(in, CuratedFile.class);
                if (file.hospitals() != null) {
                    all.addAll(file.hospitals());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("큐레이션 파일을 읽지 못했습니다: " + path, e);
            }
        }
        return all;
    }

    /** 큐레이션 파일 한 벌. source/note 는 사람이 읽는 메모라 여기서는 무시한다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuratedFile(List<CuratedHospital> hospitals) {
    }

    /**
     * 큐레이션 파일의 병원 한 줄.
     *
     * @param name        출처에 적힌 상호명 (로그·건너뜀 목록 표시용)
     * @param keyword     카카오 검색어. 지점명이나 구 이름을 붙여 다른 지역 동명 병원을 피한다
     * @param district    주소로 검증할 구 이름 (예: "강남구")
     * @param address     출처에 적힌 주소. 저장에는 쓰지 않고 사람이 대조할 때 참고한다
     * @param phone       카카오에 전화가 없을 때만 쓰는 대체 전화
     * @param openHours   운영시간 문구
     * @param specialties 주요 진료 분야 (쉼표 구분)
     * @param features    특징 한 줄 (장비·강점). 출처에 없으면 null
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuratedHospital(
            String name,
            String keyword,
            String district,
            String address,
            String phone,
            String openHours,
            String specialties,
            String features
    ) {
    }

    /**
     * 큐레이션 반영 결과.
     *
     * @param total        목록에 있던 병원 수
     * @param created      새로 저장한 수
     * @param updated      이미 있어 24시간 표시만 덧씌운 수
     * @param skippedNames 카카오에서 찾지 못해 건너뛴 병원 이름
     */
    public record CuratedSeedResult(int total, int created, int updated, List<String> skippedNames) {
    }
}
