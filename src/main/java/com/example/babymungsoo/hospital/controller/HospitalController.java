package com.example.babymungsoo.hospital.controller;

import com.example.babymungsoo.hospital.dto.HospitalResponseDto;
import com.example.babymungsoo.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    public ResponseEntity<List<HospitalResponseDto>> getAllHospitals() {
        List<HospitalResponseDto> hospitals = hospitalService.getAllHospitals()
                .stream()
                .map(HospitalResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/{hospitalId}")
    public ResponseEntity<HospitalResponseDto> getHospitalById(
            @PathVariable Long hospitalId) {
        HospitalResponseDto hospital = HospitalResponseDto
                .from(hospitalService.getHospitalById(hospitalId));
        return ResponseEntity.ok(hospital);
    }

    /**
     * 거리 제한 없이 가까운 순 N곳. 결과 화면의 '가까운 동물병원' 목록이 쓴다.
     * recommend 는 5km 박스라 교외에서 비는데, 여기는 늘 무언가 돌려준다.
     */
    @GetMapping("/nearest")
    public ResponseEntity<List<HospitalResponseDto>> nearestHospitals(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam String level,
            @RequestParam(defaultValue = "3") int limit) {
        List<HospitalResponseDto> hospitals = hospitalService
                .nearestHospitals(lat, lng, level, limit)
                .stream()
                .map(HospitalResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<HospitalResponseDto>> recommendHospitals(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam String level) {
        List<HospitalResponseDto> hospitals = hospitalService
                .recommendHospitals(lat, lng, level)
                .stream()
                .map(HospitalResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(hospitals);
    }
}