package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.client.KakaoLocalClient;
import com.example.babymungsoo.hospital.client.dto.KakaoKeywordResponse.Document;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.example.babymungsoo.hospital.service.HospitalCuratedSeedService.CuratedSeedResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 큐레이션 파일(resources/data/hospitals-24h-seoul.json)을 실제로 읽되,
 * 카카오 호출과 DB 는 목으로 대체한다. 카카오 키 없이도 매칭 규칙이 검증되어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class HospitalCuratedSeedServiceTest {

    @Mock
    KakaoLocalClient kakaoLocalClient;

    @Mock
    HospitalRepository hospitalRepository;

    HospitalCuratedSeedService service;

    @BeforeEach
    void setUp() {
        service = new HospitalCuratedSeedService(kakaoLocalClient, hospitalRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("큐레이션 파일의 병원 15곳을 전부 읽는다")
    void loadsEveryCuratedEntry() {
        when(kakaoLocalClient.searchByName(anyString())).thenReturn(List.of());

        CuratedSeedResult result = service.seedCurated();

        assertThat(result.total()).isEqualTo(15);
        assertThat(result.skippedNames()).hasSize(15);
    }

    @Test
    @DisplayName("카카오 결과 중 해당 구 주소를 가진 첫 결과만 받아들이고, 다른 지역 동명 병원은 거른다")
    void picksFirstCandidateInDistrict() {
        when(kakaoLocalClient.searchByName(anyString())).thenReturn(List.of());
        // VIP동물의료센터 청담점 — 부산 동명 병원이 먼저 오고 강남구 병원이 뒤에 온다
        when(kakaoLocalClient.searchByName("VIP동물의료센터 청담점")).thenReturn(List.of(
                doc("busan-1", "VIP동물의료센터 부산점", "부산 해운대구 해운대로 1", "051-000-0000"),
                doc("seoul-1", "VIP동물의료센터 청담점", "서울 강남구 삼성로 614", "02-511-7522")
        ));
        when(hospitalRepository.findByKakaoPlaceId("seoul-1")).thenReturn(Optional.empty());

        CuratedSeedResult result = service.seedCurated();

        ArgumentCaptor<Hospital> saved = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(saved.capture());
        Hospital hospital = saved.getValue();

        assertThat(hospital.getKakaoPlaceId()).isEqualTo("seoul-1");
        assertThat(hospital.getAddress()).isEqualTo("서울 강남구 삼성로 614");
        assertThat(hospital.getIs24hour()).isTrue();
        assertThat(hospital.getOpenHours()).contains("24시간");
        assertThat(hospital.getSpecialties()).contains("CT/MRI");
        assertThat(hospital.getFeatures()).contains("MRI, CT 보유");
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skippedNames()).doesNotContain("VIP동물의료센터 청담점");
    }

    @Test
    @DisplayName("이미 시드된 병원이면 새 행을 만들지 않고 24시간 표시와 운영시간만 덧씌운다")
    void updatesExistingRowInsteadOfInserting() {
        when(kakaoLocalClient.searchByName(anyString())).thenReturn(List.of());
        when(kakaoLocalClient.searchByName("웨스턴동물의료센터")).thenReturn(List.of(
                doc("mapo-1", "웨스턴동물의료센터", "서울 마포구 신촌로 160", "02-335-7582")
        ));
        Hospital existing = seededHospital("mapo-1", "웨스턴동물의료센터", "02-335-7582");
        when(hospitalRepository.findByKakaoPlaceId("mapo-1")).thenReturn(Optional.of(existing));

        CuratedSeedResult result = service.seedCurated();

        verify(hospitalRepository, never()).save(any());
        assertThat(existing.getIs24hour()).isTrue();
        assertThat(existing.getOpenHours()).contains("24시간");
        assertThat(existing.getSpecialties()).contains("고난도 수술");
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.created()).isZero();
    }

    @Test
    @DisplayName("카카오에 전화가 없을 때만 목록의 전화를 쓴다")
    void usesCuratedPhoneOnlyWhenKakaoHasNone() {
        when(kakaoLocalClient.searchByName(anyString())).thenReturn(List.of());
        when(kakaoLocalClient.searchByName("우리동생동물병원")).thenReturn(List.of(
                doc("guro-1", "우리동생동물병원", "서울 구로구 경인로 662", "")
        ));
        Hospital existing = seededHospital("guro-1", "우리동생동물병원", "정보 없음");
        when(hospitalRepository.findByKakaoPlaceId("guro-1")).thenReturn(Optional.of(existing));

        service.seedCurated();

        assertThat(existing.getPhone()).isEqualTo("02-2068-7582");
    }

    @Test
    @DisplayName("카카오 키가 없으면 첫 호출에서 바로 중단한다")
    void abortsWhenKakaoKeyMissing() {
        when(kakaoLocalClient.searchByName(anyString()))
                .thenThrow(new CustomException(ErrorCode.KAKAO_API_KEY_NOT_CONFIGURED));

        assertThatThrownBy(() -> service.seedCurated())
                .isInstanceOf(CustomException.class);

        verify(hospitalRepository, never()).save(any());
    }

    @Test
    @DisplayName("개별 카카오 호출 실패는 그 병원만 건너뛰고 나머지는 계속한다")
    void skipsSingleEntryOnKakaoError() {
        when(kakaoLocalClient.searchByName(anyString())).thenReturn(List.of());
        when(kakaoLocalClient.searchByName("N동물의료센터 노원점"))
                .thenThrow(new CustomException(ErrorCode.KAKAO_API_ERROR));

        CuratedSeedResult result = service.seedCurated();

        assertThat(result.skippedNames()).contains("N동물의료센터 노원점");
        assertThat(result.total()).isEqualTo(15);
    }

    private static Document doc(String id, String name, String roadAddress, String phone) {
        return new Document(id, name, "의료,건강 > 동물병원", phone, roadAddress, roadAddress,
                "126.9780", "37.5665", "https://place.map.kakao.com/" + id);
    }

    private static Hospital seededHospital(String placeId, String name, String phone) {
        return Hospital.builder()
                .kakaoPlaceId(placeId)
                .hospitalName(name)
                .address("서울")
                .phone(phone)
                .latitude(37.5665)
                .longitude(126.9780)
                .is24hour(false)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
