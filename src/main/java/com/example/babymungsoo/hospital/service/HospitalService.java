package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    public Hospital getHospitalById(Long hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_NOT_FOUND));
    }

    public List<Hospital> recommendHospitals(Double lat, Double lng, String level) {

        validateCoordinate(lat, lng);

        TriageLevel emergencyLevel = parseLevel(level);

        double range = 0.045;

        double minLat = lat - range;
        double maxLat = lat + range;
        double minLng = lng - range;
        double maxLng = lng + range;

        if (emergencyLevel == TriageLevel.IMMEDIATE) {
            return hospitalRepository.findAvailableHospitalsByLocation(
                    minLat, maxLat, minLng, maxLng
            );
        }

        return hospitalRepository.findHospitalsByLocation(
                minLat, maxLat, minLng, maxLng
        );
    }

    /** 결과 화면에 한 번에 보여줄 상한. 그 이상은 지도(9번)에서 본다. */
    private static final int MAX_NEAREST = 20;

    /**
     * 거리 제한 없이 가까운 순으로 limit 곳. 결과 화면(4번)의 '가까운 동물병원' 목록용.
     *
     * <p>recommend 와 달리 반경 박스가 없어 교외에서도 늘 무언가 돌려준다.
     * IMMEDIATE 는 24시간 병원만 가까운 순으로 주되, 그런 병원이 하나도 없으면(데이터 미비)
     * 빈 목록 대신 전체에서 가까운 순으로 준다 — 응급 상황에 빈 화면은 곤란하다.
     */
    public List<Hospital> nearestHospitals(Double lat, Double lng, String level, int limit) {
        validateCoordinate(lat, lng);
        TriageLevel emergencyLevel = parseLevel(level);
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_NEAREST)));

        if (emergencyLevel == TriageLevel.IMMEDIATE) {
            List<Hospital> open24h = hospitalRepository.findNearest(lat, lng, true, page);
            if (!open24h.isEmpty()) {
                return open24h;
            }
        }
        return hospitalRepository.findNearest(lat, lng, false, page);
    }

    private void validateCoordinate(Double lat, Double lng) {
        if (lat == null || lng == null
                || lat.isNaN() || lng.isNaN()
                || lat.isInfinite() || lng.isInfinite()
                || lat < -90 || lat > 90
                || lng < -180 || lng > 180) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    // AI 분석이 반환하는 TriageLevel을 그대로 받는다. 병원 도메인이 EmergencyLevel을 따로 두면
    // AI가 준 WATCH가 400으로 튕겨, 분석 결과에서 병원 찾기로 넘어가는 경로가 끊긴다.
    private TriageLevel parseLevel(String level) {
        try {
            return TriageLevel.valueOf(level);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_LEVEL);
        }
    }
}