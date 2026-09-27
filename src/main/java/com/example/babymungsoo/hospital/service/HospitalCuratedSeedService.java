package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.client.KakaoLocalClient;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.entity.HospitalTag;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 수기로 확인한 24시간 동물병원 목록({@code resources/data/hospitals-24h-*.json})을
 * {@code hospital} 테이블에 반영하는 서비스.
 *
 * <p>카카오 키워드검색은 영업시간을 주지 않아 {@link HospitalSeedService} 는 상호명에
 * '24시'가 있는 곳만 24시간으로 잡는다. 'OO동물의료센터'처럼 이름에 24가 없는 24시간 병원은
 * 그 방식으로는 영영 안 잡히므로, 이 목록으로 보완한다.
 *
 * <p>목록에는 상호명·구·전화만 두고, 좌표·주소·장소 ID 는 기동 시
 * 카카오 검색으로 확정한다. 블로그 등에서 옮겨 적은 주소는 오타나 옛 주소일 수 있어
 * 그대로 저장하지 않는다. 카카오에서 못 찾는 병원은 저장하지 않고 로그만 남긴다 —
 * 좌표 없는 병원은 지도에 못 올리고, 틀린 좌표는 응급 상황에서 더 해롭다.
 *
 * <p>같은 장소인지는 <b>카카오 상호명에 {@code match} 가 들어 있는지</b>로 판단한다.
 * 출처의 구(district)는 같은 이름의 지점이 여럿일 때 고르는 데만 쓴다 — 출처가 구를 틀리게
 * 적어도 이름이 맞으면 저장하고, 구가 달랐다는 사실은 결과에 남겨 목록을 고칠 수 있게 한다.
 * (처음엔 구만 검사했더니 '서울동물메디컬센터'에 마포구의 다른 병원이 들어가고,
 * 구를 잘못 적은 '우리동생동물병원'은 영영 안 잡혔다.)
 *
 * <p>운영시간·진료분야 같은 문구는 검증할 수 없어 싣지 않는다 — 이 목록이 주는 정보는
 * "존재하며 24시간을 표방한다" 뿐이고, 화면은 늘 '방문 전 전화 확인' 과 함께 보여준다.
 *
 * <p>장소 ID 기준 멱등이라 여러 번 실행해도 행이 늘지 않는다. 항목이 이전에 채웠던 행은
 * {@code curatedKey} 로 기억해 두므로, 검색어를 고쳐 다른 장소로 옮겨 가면 이전 행은 되돌린다.
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
     * @return 반영 결과. 매칭된 병원은 카카오 상호·주소와 함께 {@code matched} 로,
     *         못 찾은 병원 이름은 {@code skippedNames} 로 돌려준다.
     */
    @Transactional
    public CuratedSeedResult seedCurated() {
        int created = 0;
        int updated = 0;
        List<MatchedHospital> matched = new ArrayList<>();
        List<SkippedHospital> skipped = new ArrayList<>();
        // 한 번의 실행에서 두 항목이 같은 장소를 가리키면 뒤의 것은 오매칭이다 (지점 다른 체인점 등)
        Set<String> claimedPlaceIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (CuratedFile file : loadFiles()) {
            for (CuratedHospital entry : file.hospitals()) {
                Resolution resolution = resolve(entry, file.region(), claimedPlaceIds);
                KakaoKeywordResponse.Document doc = resolution.document();
                if (doc == null) {
                    revertPreviousMatch(entry, null, now);
                    skipped.add(new SkippedHospital(entry.name(), entry.keyword(), resolution.candidates()));
                    continue;
                }
                claimedPlaceIds.add(doc.id());
                revertPreviousMatch(entry, doc.id(), now);

                boolean districtMatched = matchesDistrict(doc, entry.district());
                matched.add(new MatchedHospital(entry.name(), doc.placeName(), doc.bestAddress(), districtMatched));
                if (!districtMatched) {
                    log.warn("큐레이션 '{}' 는 {} 로 적혀 있지만 카카오 주소는 '{}' 입니다. JSON 의 district 를 확인하세요.",
                            entry.name(), entry.district(), doc.bestAddress());
                }

                var existing = hospitalRepository.findByKakaoPlaceId(doc.id());
                Set<HospitalTag> tags = entry.tagSet();
                if (existing.isPresent()) {
                    existing.get().applyCurated(entry.name(), entry.phone(), tags, now);
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
                hospital.applyCurated(entry.name(), entry.phone(), tags, now);
                hospitalRepository.save(hospital);
                created++;
            }
        }

        List<String> skippedNames = skipped.stream().map(SkippedHospital::curatedName).toList();
        CuratedSeedResult result = new CuratedSeedResult(
                matched.size() + skipped.size(), created, updated, matched, skippedNames, skipped);
        log.info("24시간 병원 큐레이션 반영 완료 - total={}, created={}, updated={}, skipped={}",
                result.total(), created, updated, skipped.size());
        if (!skipped.isEmpty()) {
            log.warn("카카오에서 찾지 못해 건너뛴 병원 {}건: {}", skipped.size(), skippedNames);
        }
        return result;
    }

    /**
     * 이 항목이 이전 실행에서 다른 장소에 채워 둔 값이 있으면 걷어낸다.
     *
     * <p>검색어를 고쳐 매칭이 바뀌었거나(구 필터로 엉뚱한 병원이 들어갔던 경우),
     * 더는 매칭이 안 되는 경우 모두 해당한다. 상호명 기준 24시간 여부로 되돌린다.
     */
    private void revertPreviousMatch(CuratedHospital entry, String resolvedPlaceId, LocalDateTime now) {
        hospitalRepository.findByCuratedKey(entry.name())
                .filter(previous -> !previous.getKakaoPlaceId().equals(resolvedPlaceId))
                .ifPresent(previous -> {
                    previous.clearCurated(HospitalSeedService.looksOpen24Hours(previous.getHospitalName()), now);
                    log.info("큐레이션 '{}' 가 다른 장소로 옮겨 가, 이전에 채웠던 '{}' 를 되돌립니다.",
                            entry.name(), previous.getHospitalName());
                });
    }

    /**
     * 카카오 키워드검색 결과에서 목록 한 줄에 해당하는 장소를 고른다.
     *
     * <p>받아들이는 조건: 좌표와 장소 ID 가 있고, 주소가 파일의 지역(서울 등)에 속하며,
     * 카카오 상호명(공백·'24시'·괄호 제거)에 {@code match} 가 들어 있어야 한다.
     * 그런 후보가 여럿이면 출처의 구와 주소가 맞는 쪽을, 없으면 첫 번째를 고른다.
     * 이번 실행에서 이미 다른 항목이 가져간 장소는 제외한다.
     */
    private Resolution resolve(CuratedHospital entry, String region, Set<String> claimed) {
        List<KakaoKeywordResponse.Document> candidates;
        try {
            candidates = kakaoLocalClient.searchByName(entry.keyword());
        } catch (CustomException e) {
            // 키 미설정은 첫 호출에서 바로 드러나므로 전체를 중단한다. 그 외 호출 실패는 이 병원만 건너뛴다.
            if (e.getErrorCode() == ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED) {
                throw e;
            }
            log.warn("카카오 검색 실패 - {}: {}", entry.name(), e.getMessage());
            return Resolution.none(List.of("(카카오 호출 실패: " + e.getMessage() + ")"));
        }

        // 건너뛰게 되면 이 목록을 결과에 실어 보낸다 — 검색어(keyword)나 match 를 어떻게 고칠지 여기서 판단한다
        List<String> candidateLabels = candidates.stream()
                .map(doc -> doc.placeName() + " | " + doc.bestAddress() + " | " + doc.categoryName())
                .toList();

        String needle = normalizeName(StringUtils.hasText(entry.match()) ? entry.match() : entry.name());

        List<KakaoKeywordResponse.Document> sameName = candidates.stream()
                .filter(doc -> StringUtils.hasText(doc.id())
                        && StringUtils.hasText(doc.x())
                        && StringUtils.hasText(doc.y()))
                .filter(doc -> !claimed.contains(doc.id()))
                .filter(doc -> inRegion(doc, region))
                .filter(doc -> normalizeName(doc.placeName()).contains(needle))
                .toList();

        if (sameName.isEmpty()) {
            log.info("큐레이션 '{}' 검색 결과 {}건 중 이름이 맞는 곳이 없습니다: {}",
                    entry.name(), candidates.size(), candidateLabels);
            return Resolution.none(candidateLabels);
        }

        KakaoKeywordResponse.Document chosen = sameName.stream()
                .filter(doc -> matchesDistrict(doc, entry.district()))
                .findFirst()
                .orElse(sameName.get(0));
        return new Resolution(chosen, candidateLabels);
    }

    /** 검색 한 번의 결과. 고른 장소가 없으면 document 가 null 이고 candidates 만 남는다. */
    private record Resolution(KakaoKeywordResponse.Document document, List<String> candidates) {
        static Resolution none(List<String> candidates) {
            return new Resolution(null, candidates);
        }
    }

    /**
     * 상호명 비교용 정규화. 공백·'24시'·괄호 안 내용을 빼고 소문자로 맞춘다.
     * 카카오는 '24시 라온동물병원' 을 '라온동물병원' 이나 '24시라온동물병원' 으로 적기도 한다.
     */
    static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name
                .replaceAll("\\([^)]*\\)", "")
                .replace("24시간", "")
                .replace("24시", "")
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    /** 파일의 지역(예: "서울")이 주소 앞머리에 있어야 한다. 지역이 비어 있으면 검사하지 않는다. */
    private static boolean inRegion(KakaoKeywordResponse.Document doc, String region) {
        if (!StringUtils.hasText(region)) {
            return true;
        }
        return containsText(doc.roadAddressName(), region) || containsText(doc.addressName(), region);
    }

    /** 도로명·지번 주소 어느 쪽에든 구 이름이 있으면 같은 구로 본다. 구가 비어 있으면 맞는 것으로 친다. */
    private static boolean matchesDistrict(KakaoKeywordResponse.Document doc, String district) {
        if (!StringUtils.hasText(district)) {
            return true;
        }
        return containsText(doc.roadAddressName(), district) || containsText(doc.addressName(), district);
    }

    private static boolean containsText(String text, String needle) {
        return text != null && text.contains(needle);
    }

    private List<CuratedFile> loadFiles() {
        List<CuratedFile> files = new ArrayList<>();
        for (String path : CURATED_FILES) {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                CuratedFile file = objectMapper.readValue(in, CuratedFile.class);
                files.add(new CuratedFile(file.region(), file.hospitals() == null ? List.of() : file.hospitals()));
            } catch (IOException e) {
                throw new UncheckedIOException("큐레이션 파일을 읽지 못했습니다: " + path, e);
            } catch (JacksonException e) {
                // Jackson 3의 파싱 예외는 IOException이 아닌 unchecked 예외라 별도로 감싼다.
                throw new IllegalStateException("큐레이션 파일 형식이 잘못되었습니다: " + path, e);
            }
        }
        return files;
    }

    /**
     * 큐레이션 파일 한 벌. source/note 는 사람이 읽는 메모라 여기서는 무시한다.
     *
     * @param region    이 파일의 병원이 속한 지역 (예: "서울"). 카카오 주소에 이 글자가 있어야 받아들인다
     * @param hospitals 병원 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuratedFile(String region, List<CuratedHospital> hospitals) {
    }

    /**
     * 큐레이션 파일의 병원 한 줄.
     *
     * @param name        출처에 적힌 상호명. 로그·건너뜀 목록에 쓰고, {@code curatedKey} 로 저장된다
     * @param keyword     카카오 검색어. 지점명이나 구 이름을 붙여 다른 지역 동명 병원을 피한다
     * @param match       카카오 상호명에 들어 있어야 하는 핵심 이름 (예: "우리동생"). 비면 name 을 쓴다
     * @param district    출처에 적힌 구. 같은 이름의 지점이 여럿일 때 고르는 데 쓴다
     * @param address     출처에 적힌 주소. 저장에는 쓰지 않고 사람이 대조할 때 참고한다
     * @param phone       카카오에 전화가 없을 때만 쓰는 대체 전화
     * @param tags        공식 사이트·기사로 확인한 시설 태그 ({@link HospitalTag} 이름). 없으면 생략
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CuratedHospital(
            String name,
            String keyword,
            String match,
            String district,
            String address,
            String phone,
            List<String> tags
    ) {
        /** 태그 이름을 enum 으로 바꾼다. 모르는 이름은 오타이므로 기동 시 바로 실패시킨다. */
        Set<HospitalTag> tagSet() {
            Set<HospitalTag> result = EnumSet.noneOf(HospitalTag.class);
            for (String tag : tags == null ? List.<String>of() : tags) {
                try {
                    result.add(HospitalTag.valueOf(tag));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "큐레이션 '" + name + "' 의 태그 '" + tag + "' 는 HospitalTag 에 없습니다", e);
                }
            }
            return result;
        }
    }

    /**
     * 매칭된 병원 하나.
     *
     * @param curatedName     목록의 상호명
     * @param kakaoName       카카오가 준 상호명
     * @param address         카카오가 준 주소
     * @param districtMatched 출처의 구와 카카오 주소가 맞는지. false 면 목록의 district 를 고칠 것
     */
    public record MatchedHospital(String curatedName, String kakaoName, String address, boolean districtMatched) {
    }

    /**
     * 건너뛴 병원 하나. 카카오가 무엇을 돌려줬는지 함께 담아, 검색어를 어떻게 고칠지 결과만 보고 정할 수 있게 한다.
     *
     * @param curatedName 목록의 상호명
     * @param keyword     썼던 검색어
     * @param candidates  카카오 응답 ("상호 | 주소 | 카테고리"). 비어 있으면 검색어 자체가 안 잡힌 것
     */
    public record SkippedHospital(String curatedName, String keyword, List<String> candidates) {
    }

    /**
     * 큐레이션 반영 결과.
     *
     * @param total        목록에 있던 병원 수
     * @param created      새로 저장한 수
     * @param updated      이미 있어 24시간 표시만 덧씌운 수
     * @param matched      매칭된 병원 (카카오 상호·주소 포함 — 오매칭 확인용)
     * @param skippedNames 카카오에서 찾지 못해 건너뛴 병원 이름 (로그·요약용)
     * @param skipped      건너뛴 병원과 그때 카카오가 돌려준 후보 (검색어 손볼 때 참고)
     */
    public record CuratedSeedResult(int total, int created, int updated,
                                    List<MatchedHospital> matched, List<String> skippedNames,
                                    List<SkippedHospital> skipped) {
    }
}
