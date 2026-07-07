package com.example.babymungsoo.hospital.service;

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
                .orElseThrow(() -> new IllegalArgumentException("병원을 찾을 수 없습니다. id: " + hospitalId));
    }

    public List<Hospital> recommendHospitals(Double lat, Double lng, String level) {

        double range = 0.045;

        double minLat = lat - range;
        double maxLat = lat + range;
        double minLng = lng - range;
        double maxLng = lng + range;


        if ("IMMEDIATE".equals(level)) {
            return hospitalRepository.findAvailableHospitalsByLocation(
                    minLat, maxLat, minLng, maxLng
            );
        }

        return hospitalRepository.findHospitalsByLocation(
                minLat, maxLat, minLng, maxLng
        );
    }
}