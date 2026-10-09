package com.example.babymungsoo.hospital.dto;

import com.example.babymungsoo.hospital.entity.Hospital;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    // 큐레이션이 확인한 시설 태그 이름(HospitalTag). 없으면 빈 목록. 화면에서는 뱃지로 쓴다.
    private List<String> tags;
    private Float rating;
    private Integer reviewCount;
    private String imageUrl;
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
                .tags(hospital.getTags().stream().map(Enum::name).sorted().toList())
                .rating(hospital.getRating())
                .reviewCount(hospital.getReviewCount())
                .imageUrl(hospital.getImageUrl())
                .lastUpdated(hospital.getLastUpdated())
                .build();
    }
}