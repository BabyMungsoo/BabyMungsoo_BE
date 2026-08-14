package com.example.babymungsoo.hospital.client;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 카카오 로컬 "키워드로 장소 검색" API 호출 전담 어댑터.
 *
 * <p>동물병원을 좌표 반경으로 검색해 이름·주소·전화·좌표까지 한 번에 받아온다.
 * 카카오는 한 검색당 최대 45건(15건 × 3페이지)까지만 돌려주므로,
 * 넓은 지역은 상위 {@code HospitalSeedService}에서 중심점을 나눠 여러 번 호출한다.
 */
@Component
public class KakaoLocalClient {

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String KEYWORD_PATH = "/v2/local/search/keyword.json";
    private static final String QUERY = "동물병원";

    // 카카오 응답 상한. size 15 × page 3 = 45건이 최대라 그 이상은 무의미하다.
    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGE = 3;

    private final RestClient restClient;
    private final String apiKey;

    public KakaoLocalClient(@Value("${kakao.rest-api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * 주어진 좌표를 중심으로 반경 안의 동물병원을 검색한다.
     *
     * @param latitude  위도
     * @param longitude 경도
     * @param radius    반경(m). 카카오 허용 범위 0~20000.
     * @return 검색된 동물병원 문서 목록(최대 45건)
     */
    public List<KakaoKeywordResponse.Document> searchAnimalHospitals(
            double latitude, double longitude, int radius) {

        if (!StringUtils.hasText(apiKey)) {
            throw new CustomException(ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED);
        }

        List<KakaoKeywordResponse.Document> collected = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGE; page++) {
            KakaoKeywordResponse response = requestPage(latitude, longitude, radius, page);
            if (response == null || response.documents() == null) {
                break;
            }

            // 키워드검색은 반경 내 연관 장소(세무사무소·일반의원 등)를 느슨하게 섞어 준다.
            // 카카오 카테고리가 "동물병원"인 결과만 남겨 오탐을 걸러낸다.
            for (KakaoKeywordResponse.Document doc : response.documents()) {
                if (doc.categoryName() != null && doc.categoryName().contains("동물병원")) {
                    collected.add(doc);
                }
            }

            // 마지막 페이지면 더 호출하지 않는다.
            if (response.meta() == null || Boolean.TRUE.equals(response.meta().isEnd())) {
                break;
            }
        }

        return collected;
    }

    private KakaoKeywordResponse requestPage(double latitude, double longitude,
                                             int radius, int page) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_PATH)
                            .queryParam("query", QUERY)
                            // x=경도, y=위도 순서 주의
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", radius)
                            .queryParam("page", page)
                            .queryParam("size", PAGE_SIZE)
                            .queryParam("sort", "distance")
                            .build())
                    .header("Authorization", "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoKeywordResponse.class);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_API_ERROR);
        }
    }
}
