package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.hospital.client.KakaoLocalClient;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 카카오 장소검색 결과를 우리 {@code hospital} 테이블로 적재(시드)하는 서비스.
 *
 * <p>카카오 장소 ID를 멱등 키로 써서, 같은 지역을 여러 번 시드해도
 * 이미 있는 병원은 갱신(이름·주소·전화·좌표만)하고 새 병원만 추가한다.
 * rating·openHours는 카카오가 주지 않으므로 신규 저장 시 null로 두고,
 * is24hour는 상호명으로 추정해 채운다({@link #OPEN_24H_NAME}).
 * 이후 운영자가 보정하면 그 값을 시드가 덮어쓰지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalSeedService {

    /**
     * 상호명으로 24시간 운영 여부를 추정하는 패턴.
     *
     * <p>카카오 키워드검색은 영업시간을 주지 않고, 영업시간을 주는 API(Google Places 등)는
     * 유료 구간이라 쓸 수 없다. 대신 국내 동물병원이 24시간 응급진료를 하면 상호에 '24시'를
     * 붙이는 관행을 이용한다. '24아프리카동물메디컬센터'처럼 '시' 없이 붙는 곳도 있어
     * '24' + 한글도 함께 잡는다.
     *
     * <p>추정값이라 100% 정확하지 않다. 상호만 24시고 야간은 예약제인 곳이 섞일 수 있으므로,
     * 화면에서는 반드시 '방문 전 전화 확인' 안내와 함께 보여준다.
     */
    private static final Pattern OPEN_24H_NAME = Pattern.compile("24\\s?시|24[가-힣]");

    // 전국 일괄 시드의 지점당 검색 반경(m). 카카오는 검색 1회당 최대 45건이라
    // 도심은 이 반경으로 촘촘히, 지방은 도시별 1지점으로 커버한다.
    private static final int NATIONWIDE_RADIUS = 7000;

    /**
     * 전국 주요 인구 밀집 지역의 중심 좌표 목록.
     * 카카오 검색이 45건/회로 제한되므로 광역시·대도시는 구 단위로 나눠 지점을 둔다.
     * 특정 지역이 비면 이 목록에 좌표를 추가하면 된다.
     */
    private static final List<SeedSpot> NATIONWIDE_SPOTS = List.of(
            // 서울
            new SeedSpot(37.5665, 126.9780, "서울 시청·종로"),
            new SeedSpot(37.4979, 127.0276, "서울 강남"),
            new SeedSpot(37.5172, 127.0473, "서울 삼성·잠실"),
            new SeedSpot(37.5559, 126.9236, "서울 마포·신촌"),
            new SeedSpot(37.5384, 127.0822, "서울 성수·광진"),
            new SeedSpot(37.6066, 127.0927, "서울 노원·도봉"),
            new SeedSpot(37.4837, 126.9020, "서울 관악·동작"),
            new SeedSpot(37.5509, 126.8495, "서울 강서·양천"),
            new SeedSpot(37.5636, 127.0369, "서울 성북·동대문"),
            new SeedSpot(37.5145, 126.8955, "서울 영등포·구로"),
            new SeedSpot(37.6584, 127.0300, "서울 강북·미아"),
            // 인천·경기
            new SeedSpot(37.4563, 126.7052, "인천 중구"),
            new SeedSpot(37.5074, 126.7218, "인천 부평"),
            new SeedSpot(37.3894, 126.6389, "인천 송도"),
            new SeedSpot(37.2636, 127.0286, "수원"),
            new SeedSpot(37.3830, 127.1189, "성남 분당"),
            new SeedSpot(37.4449, 127.1389, "성남 수정"),
            new SeedSpot(37.6584, 126.8320, "고양 덕양"),
            new SeedSpot(37.6749, 126.7700, "고양 일산"),
            new SeedSpot(37.3220, 127.0955, "용인 수지"),
            new SeedSpot(37.2802, 127.1148, "용인 기흥"),
            new SeedSpot(37.5035, 126.7660, "부천"),
            new SeedSpot(37.3219, 126.8309, "안산"),
            new SeedSpot(37.6360, 127.2165, "남양주"),
            new SeedSpot(37.2003, 127.0727, "화성 동탄"),
            new SeedSpot(37.3943, 126.9568, "안양"),
            new SeedSpot(36.9921, 127.1129, "평택"),
            new SeedSpot(37.7380, 127.0338, "의정부"),
            new SeedSpot(37.7226, 126.7626, "파주 운정"),
            new SeedSpot(37.6152, 126.7157, "김포"),
            new SeedSpot(37.4786, 126.8644, "광명"),
            // 부산
            new SeedSpot(35.1631, 129.0534, "부산 부산진"),
            new SeedSpot(35.1631, 129.1635, "부산 해운대"),
            new SeedSpot(35.2049, 129.0836, "부산 동래"),
            new SeedSpot(35.1046, 128.9746, "부산 사하"),
            new SeedSpot(35.1976, 128.9903, "부산 북구"),
            // 대구
            new SeedSpot(35.8693, 128.6062, "대구 중구"),
            new SeedSpot(35.8583, 128.6306, "대구 수성"),
            new SeedSpot(35.8299, 128.5327, "대구 달서"),
            // 광주·대전·울산·세종
            new SeedSpot(35.1520, 126.8905, "광주 서구"),
            new SeedSpot(35.1740, 126.9120, "광주 북구"),
            new SeedSpot(36.3515, 127.3845, "대전 서구"),
            new SeedSpot(36.3620, 127.3560, "대전 유성"),
            new SeedSpot(35.5384, 129.3114, "울산 남구"),
            new SeedSpot(36.4801, 127.2890, "세종"),
            // 강원
            new SeedSpot(37.8813, 127.7298, "춘천"),
            new SeedSpot(37.3422, 127.9202, "원주"),
            new SeedSpot(37.7519, 128.8761, "강릉"),
            // 충청
            new SeedSpot(36.6424, 127.4890, "청주"),
            new SeedSpot(36.8151, 127.1139, "천안"),
            new SeedSpot(36.7898, 127.0018, "아산"),
            // 전라
            new SeedSpot(35.8242, 127.1480, "전주"),
            new SeedSpot(35.9483, 126.9576, "익산"),
            new SeedSpot(34.7604, 127.6622, "여수"),
            new SeedSpot(34.9506, 127.4872, "순천"),
            new SeedSpot(34.8118, 126.3922, "목포"),
            // 경상
            new SeedSpot(35.2280, 128.6811, "창원"),
            new SeedSpot(35.2285, 128.8894, "김해"),
            new SeedSpot(35.1800, 128.1076, "진주"),
            new SeedSpot(36.0190, 129.3435, "포항"),
            new SeedSpot(35.8562, 129.2247, "경주"),
            new SeedSpot(36.1195, 128.3446, "구미"),
            new SeedSpot(35.3350, 129.0370, "양산"),
            // 제주
            new SeedSpot(33.4996, 126.5312, "제주시"),
            new SeedSpot(33.2541, 126.5600, "서귀포")
    );

    private final KakaoLocalClient kakaoLocalClient;
    private final HospitalRepository hospitalRepository;
    // 전국 시드에서 지점별로 seedAround()의 트랜잭션을 개별 적용하기 위한 자기 자신 프록시.
    // (같은 빈 내부 직접 호출은 @Transactional 프록시를 우회하므로 프록시를 거쳐 호출한다.)
    private final ObjectProvider<HospitalSeedService> selfProvider;

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
                    .is24hour(looksOpen24Hours(doc.placeName()))
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
     * 전국 주요 도시 목록({@link #NATIONWIDE_SPOTS})을 한 번에 시드한다.
     * 새 DB에서도 클릭 한 번으로 전국 데이터를 채우기 위한 진입점이다.
     *
     * <p>지점별로 개별 트랜잭션을 적용하고 예외를 격리하므로,
     * 한 지역의 카카오 호출 실패가 나머지 지역 적재를 막지 않는다.
     *
     * @return 전국 시드 집계 결과
     */
    public NationwideSeedResult seedNationwide() {
        HospitalSeedService self = selfProvider.getObject();

        int succeeded = 0;
        int failed = 0;
        int created = 0;
        int updated = 0;

        for (SeedSpot spot : NATIONWIDE_SPOTS) {
            try {
                SeedResult result = self.seedAround(spot.lat(), spot.lng(), NATIONWIDE_RADIUS);
                created += result.created();
                updated += result.updated();
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("전국 시드 지점 실패 - {} ({}, {}): {}",
                        spot.region(), spot.lat(), spot.lng(), e.getMessage());
            }
        }

        NationwideSeedResult summary =
                new NationwideSeedResult(NATIONWIDE_SPOTS.size(), succeeded, failed, created, updated);
        log.info("전국 동물병원 시드 완료 - {}", summary);
        return summary;
    }

    /**
     * 상호명에 24시간 표기가 있으면 24시간 운영으로 본다.
     *
     * <p>기존 데이터를 채우는 백필 러너도 같은 판정을 써야 해서 공개해 둔다.
     * 규칙이 두 군데로 갈라지면 시드와 백필 결과가 달라진다.
     */
    public static boolean looksOpen24Hours(String hospitalName) {
        return StringUtils.hasText(hospitalName) && OPEN_24H_NAME.matcher(hospitalName).find();
    }

    /** 전국 시드 대상 지점(중심 좌표 + 지역명). */
    public record SeedSpot(double lat, double lng, String region) {
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

    /**
     * 전국 시드 집계 결과.
     *
     * @param spots     대상 지점 수
     * @param succeeded 성공한 지점 수
     * @param failed    실패한 지점 수
     * @param created   신규 저장 건수 합계
     * @param updated   기존 갱신 건수 합계
     */
    public record NationwideSeedResult(int spots, int succeeded, int failed, int created, int updated) {
    }
}
