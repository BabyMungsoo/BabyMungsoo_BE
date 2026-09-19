package com.example.babymungsoo.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

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

    // 이 행을 채운 큐레이션 항목의 이름(hospitals-24h-*.json 의 name). 나중에 검색어를 고쳐
    // 다른 장소로 매칭되면, 같은 키로 채워졌던 이전 행을 찾아 되돌리기 위한 연결 고리다.
    @Column(unique = true)
    private String curatedKey;

    // 큐레이션이 확인해 붙인 시설 태그(MRI, 응급센터 등). 목록에 없는 병원은 비어 있다.
    // 목록 조회가 병원 수백 건을 돌려주므로 EAGER 대신 BatchSize 로 묶어 읽는다.
    @ElementCollection
    @CollectionTable(name = "hospital_tag", joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "tag", nullable = false)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 200)
    @Builder.Default
    private Set<HospitalTag> tags = EnumSet.noneOf(HospitalTag.class);

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
     * 큐레이션 목록(수기로 확인한 24시간 병원)에 있는 병원으로 표시한다.
     *
     * <p>{@link #markOpen24Hours} 와 같이 24시간으로 표시하고, 어느 항목이 채웠는지 curatedKey 로 남긴다.
     * 전화는 카카오 값이 비어 있을 때만 목록 값을 쓴다 — 카카오 쪽이 더 자주 갱신되기 때문이다.
     * 운영시간·진료분야 같은 문구는 검증할 수 없어 목록에 두지 않고, 예전 목록이 넣어 둔 문구도 지운다.
     * 시설 태그는 목록 값으로 통째로 바꾼다 — 목록에서 태그를 뺐으면 DB 에서도 빠져야 한다.
     */
    public void applyCurated(String curatedKey, String fallbackPhone, Set<HospitalTag> tags,
                             LocalDateTime lastUpdated) {
        this.curatedKey = curatedKey;
        this.is24hour = true;
        this.openHours = null;
        this.tags.clear();
        this.tags.addAll(tags);
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
        this.tags.clear();
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