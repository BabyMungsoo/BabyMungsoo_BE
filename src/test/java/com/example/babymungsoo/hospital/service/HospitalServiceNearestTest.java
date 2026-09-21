package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceNearestTest {

    @Mock
    HospitalRepository hospitalRepository;

    @InjectMocks
    HospitalService service;

    @Test
    @DisplayName("IMMEDIATE 는 24시간 병원만 가까운 순으로 준다")
    void immediateReturnsOpen24hOnly() {
        Hospital open24h = hospital("24시");
        when(hospitalRepository.findNearest(anyDouble(), anyDouble(), eq(true), eq(PageRequest.of(0, 3))))
                .thenReturn(List.of(open24h));

        List<Hospital> result = service.nearestHospitals(37.5665, 126.978, "IMMEDIATE", 3);

        assertThat(result).containsExactly(open24h);
        verify(hospitalRepository, never()).findNearest(anyDouble(), anyDouble(), eq(false), eq(PageRequest.of(0, 3)));
    }

    @Test
    @DisplayName("IMMEDIATE 인데 24시간 병원이 하나도 없으면 전체에서 가까운 순으로 폴백한다")
    void immediateFallsBackWhenNo24h() {
        Hospital any = hospital("일반");
        when(hospitalRepository.findNearest(anyDouble(), anyDouble(), eq(true), eq(PageRequest.of(0, 3))))
                .thenReturn(List.of());
        when(hospitalRepository.findNearest(anyDouble(), anyDouble(), eq(false), eq(PageRequest.of(0, 3))))
                .thenReturn(List.of(any));

        List<Hospital> result = service.nearestHospitals(37.5665, 126.978, "IMMEDIATE", 3);

        assertThat(result).containsExactly(any);
    }

    @Test
    @DisplayName("WATCH·NORMAL 은 처음부터 전체에서 가까운 순이고, limit 은 1~20 으로 잘린다")
    void otherLevelsUseAllAndClampLimit() {
        when(hospitalRepository.findNearest(anyDouble(), anyDouble(), eq(false), eq(PageRequest.of(0, 20))))
                .thenReturn(List.of());

        service.nearestHospitals(37.5665, 126.978, "WATCH", 999);

        verify(hospitalRepository).findNearest(anyDouble(), anyDouble(), eq(false), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("좌표가 이상하거나 등급을 모르면 거부한다")
    void rejectsBadInput() {
        assertThatThrownBy(() -> service.nearestHospitals(999.0, 126.978, "WATCH", 3))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.nearestHospitals(37.5665, 126.978, "URGENT", 3))
                .isInstanceOf(CustomException.class);
    }

    private static Hospital hospital(String name) {
        return Hospital.builder().hospitalName(name).address("서울").phone("02-000-0000")
                .latitude(37.5).longitude(127.0).is24hour(true).lastUpdated(LocalDateTime.now()).build();
    }
}
