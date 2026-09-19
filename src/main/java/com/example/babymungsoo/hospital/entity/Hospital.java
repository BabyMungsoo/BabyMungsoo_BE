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

    // 주요 진료 분야. "24시 응급, CT/MRI, 정형외과" 처럼 쉼표로 이어진 문자열.
    // 카카오가 주지 않는 값이라 큐레이션 목록으로만 채워지고, 없으면 null.
    private String specialties;

    // 병원 특징 한 줄. "대학병원급 MRI·CT 보유, 분과별 전문의 협진" 처럼 장비·강점을 문장으로 둔다.
    // MRI 보유 같은 걸 boolean 으로 쪼개지 않는 이유: 출처가 언급 안 한 병원을 '없음'으로
    // 보여주면 오정보가 된다. 큐레이션 목록으로만 채워지고, 없으면 null.
    @Column(length = 500)
    private String features;

    // 이 행을 채운 큐레이션 항목의 이름(hospitals-24h-*.json 의 name). 나중에 검색어를 고쳐
    // 다른 장소로 매칭되면, 같은 키로 채워졌던 이전 행을 찾아 되돌리기 위한 연결 고리다.
    @Column(unique = true)
    private String curatedKey;

    private Float rating;

    // 리뷰 개수. 시안의 "4.8 (256)"에서 (256)에 해당. 데이터 확보 전엔 null.
    private Integer reviewCount;

    // 병원 대표 썸네일 이미지 URL. 데이터 확보 전엔 null.
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    /**
     * 상호명으로 24시간 운영이 확인됐을 때 표시한다.
     *
     * <p>이 값을 true 로 바꾸는 경로는 시드(신규 저장)와 백필 두 곳뿐이고,
     * {@link #updateFromKakao} 는 일부러 건드리지 않는다 — 수기 보정값을 지키기 위해서다.
     */
    public void markOpen24Hours(LocalDateTime lastUpdated) {
        this.is24hour = true;
        this.lastUpdated = lastUpdated;
    }

    /**
     * 큐레이션 목록(수기로 확인한 24시간 병원)의 값을 덮어쓴다.
     *
     * <p>{@link #markOpen24Hours} 와 같이 24시간으로 표시하면서, 카카오가 주지 않는
     * 운영시간·진료 분야·특징까지 함께 채운다. 전화는 카카오 값이 비어 있을 때만 목록 값을 쓴다 —
     * 카카오 쪽이 더 자주 갱신되기 때문이다.
     */
    public void applyCurated(String curatedKey, String openHours, String specialties, String features,
                             String fallbackPhone, LocalDateTime lastUpdated) {
        this.curatedKey = curatedKey;
        this.is24hour = true;
        this.openHours = openHours;
        this.specialties = specialties;
        this.features = features;
        if (isMissingPhone() && fallbackPhone != null && !fallbackPhone.isBlank()) {
            this.phone = fallbackPhone;
        }
        this.lastUpdated = lastUpdated;
    }

    /**
     * 큐레이션으로 채웠던 값을 걷어낸다. 검색어를 고쳐 같은 항목이 다른 장소로 옮겨 갔을 때,
     * 잘못 채워졌던 행이 24시간 병원으로 남지 않게 한다.
     *
     * @param open24HoursByName 상호명만으로 판정한 24시간 여부 — 큐레이션 전 상태로 되돌리는 기준
     */
    public void clearCurated(boolean open24HoursByName, LocalDateTime lastUpdated) {
        this.curatedKey = null;
        this.is24hour = open24HoursByName;
        this.openHours = null;
        this.specialties = null;
        this.features = null;
        this.lastUpdated = lastUpdated;
    }

    /** 시드가 전화 없음을 "정보 없음" 문자열로 저장하므로 null 과 함께 비어 있음으로 본다. */
    private boolean isMissingPhone() {
        return phone == null || phone.isBlank() || "정보 없음".equals(phone.trim());
    }

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