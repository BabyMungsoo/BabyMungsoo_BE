package com.example.babymungsoo.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospital")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hospitalId;

    // 카카오 장소 고유 ID. 시드 재실행 시 중복 저장을 막는 멱등 키로 쓴다.
    // 수기로 입력한 병원은 null일 수 있어 unique는 걸되 nullable로 둔다.
    @Column(unique = true)
    private String kakaoPlaceId;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Boolean is24hour;

    private String openHours;

    private Float rating;

    // 리뷰 개수. 시안의 "4.8 (256)"에서 (256)에 해당. 데이터 확보 전엔 null.
    private Integer reviewCount;

    // 병원 대표 썸네일 이미지 URL. 데이터 확보 전엔 null.
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * 카카오 검색 결과로 채운 필드(이름·주소·전화·좌표)만 갱신한다.
     * is24hour·rating·openHours는 카카오 키워드검색이 주지 않는 값이라,
     * 운영자가 수기로 보정한 값을 시드 재실행 때 덮어쓰지 않도록 그대로 둔다.
     */
    public void updateFromKakao(String hospitalName, String address,
                                String phone, Double latitude, Double longitude,
                                LocalDateTime lastUpdated) {
        this.hospitalName = hospitalName;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastUpdated = lastUpdated;
    }
}