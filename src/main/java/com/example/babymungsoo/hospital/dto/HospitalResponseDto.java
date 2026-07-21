package com.example.babymungsoo.hospital.dto;

import com.example.babymungsoo.hospital.entity.Hospital;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HospitalResponseDto {

    private Long hospitalId;
    private String hospitalName;
    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;
    private Boolean is24hour;
    private String openHours;
    private Float rating;
    private LocalDateTime lastUpdated;

    public static HospitalResponseDto from(Hospital hospital) {
        return HospitalResponseDto.builder()
                .hospitalId(hospital.getHospitalId())
                .hospitalName(hospital.getHospitalName())
                .address(hospital.getAddress())
                .phone(hospital.getPhone())
                .latitude(hospital.getLatitude())
                .longitude(hospital.getLongitude())
                .is24hour(hospital.getIs24hour())
                .openHours(hospital.getOpenHours())
                .rating(hospital.getRating())
                .lastUpdated(hospital.getLastUpdated())
                .build();
    }
}