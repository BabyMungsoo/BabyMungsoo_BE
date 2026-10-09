package com.example.babymungsoo.hospital.repository;

import com.example.babymungsoo.hospital.entity.Hospital;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {


    List<Hospital> findByIs24hourTrue();

    // 24시간 백필 대상. 이미 표시된 병원은 다시 손대지 않는다.
    List<Hospital> findByIs24hourFalse();

    // 시드 재실행 시 멱등 처리를 위한 카카오 장소 ID 조회
    Optional<Hospital> findByKakaoPlaceId(String kakaoPlaceId);

    // 큐레이션 항목이 이전에 채웠던 행. 검색어가 바뀌어 다른 장소로 옮겨 가면 이 행을 되돌린다.
    Optional<Hospital> findByCuratedKey(String curatedKey);

    @Query("SELECT h FROM Hospital h WHERE " +
            "h.latitude BETWEEN :minLat AND :maxLat AND " +
            "h.longitude BETWEEN :minLng AND :maxLng")
    List<Hospital> findHospitalsByLocation(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );

    /**
     * 거리 제한 없이 가까운 순으로 병원을 돌려준다. 결과 화면의 '가까운 동물병원' 3곳용.
     *
     * <p>{@link #findHospitalsByLocation} 은 ±0.045도 박스 안만 주므로 교외에서는 비어 버린다.
     * 여기서는 하버사인 거리로 DB 에서 정렬하고 Pageable 로 자른다 — 전체 행을 메모리에 올리지 않는다.
     * 0.0174533 은 도→라디안 계수. 부동소수 오차로 cos 합이 1 을 살짝 넘으면 acos 가 NaN 이 되어
     * least(1.0, …) 으로 막는다. 정렬만 필요하므로 지구 반지름은 곱하지 않는다.
     *
     * @param only24h true 면 24시간 병원만
     */
    @Query("SELECT h FROM Hospital h WHERE (:only24h = false OR h.is24hour = true) " +
            "ORDER BY acos(least(1.0, " +
            "cos(:lat * 0.017453292519943295) * cos(h.latitude * 0.017453292519943295) * " +
            "cos((h.longitude - :lng) * 0.017453292519943295) + " +
            "sin(:lat * 0.017453292519943295) * sin(h.latitude * 0.017453292519943295)))")
    List<Hospital> findNearest(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("only24h") boolean only24h,
            Pageable pageable
    );

    @Query("SELECT h FROM Hospital h WHERE " +
            "h.is24hour = true AND " +
            "h.latitude BETWEEN :minLat AND :maxLat AND " +
            "h.longitude BETWEEN :minLng AND :maxLng")
    List<Hospital> findAvailableHospitalsByLocation(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}