package com.example.babymungsoo.hospital.controller;

import com.example.babymungsoo.global.response.ApiResponse;
import com.example.babymungsoo.hospital.service.HospitalSeedService;
import com.example.babymungsoo.hospital.service.HospitalSeedService.SeedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "관리자 병원",
        description = "카카오 장소검색으로 동물병원 DB를 적재하는 관리자 전용 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/hospitals")
public class HospitalAdminController {

    private final HospitalSeedService hospitalSeedService;

    /**
     * 좌표 반경의 동물병원을 카카오에서 검색해 DB에 적재한다.
     * 카카오는 한 번에 최대 45건만 주므로, 넓게 채우려면 중심 좌표를 바꿔 여러 번 호출한다.
     * 기본값은 서울시청(37.5665, 126.9780) 중심, 반경 20km.
     */
    @Operation(summary = "동물병원 DB 시드(카카오 장소검색)")
    @PostMapping("/seed")
    public ApiResponse<SeedResult> seed(
            @RequestParam(defaultValue = "37.5665") double lat,
            @RequestParam(defaultValue = "126.9780") double lng,
            @RequestParam(defaultValue = "20000") int radius
    ) {
        SeedResult result = hospitalSeedService.seedAround(lat, lng, radius);
        return ApiResponse.success(result, "동물병원 시드가 완료되었습니다.");
    }
}
