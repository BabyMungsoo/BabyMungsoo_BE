package com.example.babymungsoo.hospital.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
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

        EmergencyLevel emergencyLevel = parseLevel(level);

        double range = 0.045;

        double minLat = lat - range;
        double maxLat = lat + range;
        double minLng = lng - range;
        double maxLng = lng + range;

        if (emergencyLevel == EmergencyLevel.IMMEDIATE) {
            return hospitalRepository.findAvailableHospitalsByLocation(
                    minLat, maxLat, minLng, maxLng
            );
        }

        return hospitalRepository.findHospitalsByLocation(
                minLat, maxLat, minLng, maxLng
        );
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

    private EmergencyLevel parseLevel(String level) {
        try {
            return EmergencyLevel.valueOf(level);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_EMERGENCY_LEVEL);
        }
    }

    public enum EmergencyLevel {
        IMMEDIATE,
        URGENT,
        NORMAL
    }
}