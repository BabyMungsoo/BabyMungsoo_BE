package com.example.babymungsoo.hospital.repository;

import com.example.babymungsoo.hospital.entity.Hospital;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findNearest 의 HQL 이 파싱되고 거리순으로 나오는지 H2 에서 확인한다.
 * acos/cos/sin/least 같은 함수는 컴파일로는 못 잡고 쿼리를 실제로 돌려야 드러난다.
 */
@DataJpaTest
class HospitalRepositoryTest {

    @Autowired
    HospitalRepository hospitalRepository;

    // 서울시청. 아래 병원들은 여기서 약 0.5km / 5km / 30km 떨어져 있다.
    static final double LAT = 37.5665;
    static final double LNG = 126.9780;

    @BeforeEach
    void seed() {
        hospitalRepository.saveAll(List.of(
                hospital("30km 24시", 37.83, 126.978, true),
                hospital("5km 일반", 37.611, 126.978, false),
                hospital("0.5km 일반", 37.571, 126.978, false),
                hospital("8km 24시", 37.638, 126.978, true)
        ));
    }

    @Test
    @DisplayName("반경 제한 없이 가까운 순으로 정렬하고 limit 만큼 자른다")
    void ordersByDistanceWithoutRadius() {
        List<Hospital> result = hospitalRepository.findNearest(LAT, LNG, false, PageRequest.of(0, 3));

        assertThat(result).extracting(Hospital::getHospitalName)
                .containsExactly("0.5km 일반", "5km 일반", "8km 24시");
    }

    @Test
    @DisplayName("only24h 면 24시간 병원만, 멀어도 가까운 순으로 준다")
    void filtersTo24hWhenRequested() {
        List<Hospital> result = hospitalRepository.findNearest(LAT, LNG, true, PageRequest.of(0, 3));

        assertThat(result).extracting(Hospital::getHospitalName)
                .containsExactly("8km 24시", "30km 24시");
    }

    private static Hospital hospital(String name, double lat, double lng, boolean open24h) {
        return Hospital.builder()
                .hospitalName(name)
                .address("서울")
                .phone("02-000-0000")
                .latitude(lat)
                .longitude(lng)
                .is24hour(open24h)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
