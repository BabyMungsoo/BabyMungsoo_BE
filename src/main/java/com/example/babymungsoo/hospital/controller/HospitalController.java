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