package com.example.babymungsoo.hospital.controller;

import com.example.babymungsoo.global.response.ApiResponse;
import com.example.babymungsoo.hospital.service.HospitalCuratedSeedService;
import com.example.babymungsoo.hospital.service.HospitalCuratedSeedService.CuratedSeedResult;
import com.example.babymungsoo.hospital.service.HospitalSeedService;
import com.example.babymungsoo.hospital.service.HospitalSeedService.NationwideSeedResult;
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
    private final HospitalCuratedSeedService hospitalCuratedSeedService;

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

    /**
     * 전국 주요 도시를 한 번에 시드한다.
     * 새 DB(배포 환경 등)를 초기 적재할 때 이 엔드포인트 한 번이면 전국 데이터가 채워진다.
     * 좌표 목록은 서비스 상수로 관리하며, kakaoPlaceId 기준 멱등이라 여러 번 호출해도 안전하다.
     */
    @Operation(summary = "전국 동물병원 일괄 시드(카카오 장소검색)")
    @PostMapping("/seed-nationwide")
    public ApiResponse<NationwideSeedResult> seedNationwide() {
        NationwideSeedResult result = hospitalSeedService.seedNationwide();
        return ApiResponse.success(result, "전국 동물병원 시드가 완료되었습니다.");
    }

    /**
     * 수기 확인한 24시간 병원 목록(resources/data/hospitals-24h-*.json)을 반영한다.
     * 카카오 시드는 상호명에 '24시'가 있는 곳만 24시간으로 잡아서, 이름에 24가 없는
     * 의료센터류를 여기서 보완한다. 좌표는 카카오로 확정하므로 못 찾은 병원은 응답의
     * skippedNames 로 돌려준다 — 그 이름은 JSON 의 keyword 를 손봐서 다시 호출하면 된다.
     */
    @Operation(summary = "24시간 동물병원 큐레이션 반영")
    @PostMapping("/seed-curated")
    public ApiResponse<CuratedSeedResult> seedCurated() {
        CuratedSeedResult result = hospitalCuratedSeedService.seedCurated();
        return ApiResponse.success(result, "24시간 병원 큐레이션 반영이 완료되었습니다.");
    }
}
