package com.example.babymungsoo.hospital.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 카카오 로컬 - 키워드로 장소 검색 API 응답 매핑.
 * 문서: https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-keyword
 *
 * <p>필요한 필드만 매핑하고 나머지는 {@code @JsonIgnoreProperties}로 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoKeywordResponse(
        List<Document> documents,
        Meta meta
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            // 카카오 장소 고유 ID (멱등 시드용)
            String id,
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_name") String categoryName,
            String phone,
            @JsonProperty("address_name") String addressName,
            @JsonProperty("road_address_name") String roadAddressName,
            // x=경도(longitude), y=위도(latitude) — 순서 주의
            String x,
            String y,
            @JsonProperty("place_url") String placeUrl
    ) {
        /** 도로명 주소를 우선 쓰고 없으면 지번 주소로 대체한다. */
        public String bestAddress() {
            return (roadAddressName != null && !roadAddressName.isBlank())
                    ? roadAddressName
                    : addressName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @JsonProperty("total_count") Integer totalCount,
            @JsonProperty("pageable_count") Integer pageableCount,
            @JsonProperty("is_end") Boolean isEnd
    ) {
    }
}
