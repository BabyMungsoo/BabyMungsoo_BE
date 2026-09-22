package com.example.babymungsoo.record.controller;

import com.example.babymungsoo.record.dto.HospitalVisitCreateRequestDto;
import com.example.babymungsoo.record.dto.HospitalVisitResponseDto;
import com.example.babymungsoo.record.dto.HospitalVisitUpdateRequestDto;
import com.example.babymungsoo.record.service.HospitalVisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 병원 방문 팔로우업.
 *
 * <p>생성·조회는 분석 기록 아래에 두고, 개별 수정·삭제는 방문 id 로 한다.
 * 소유자 검증은 서비스가 로그인 사용자로 한다. 요청에 담긴 userId 는 믿지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HospitalVisitController {

    private final HospitalVisitService hospitalVisitService;

    @PostMapping("/records/{recordId}/visits")
    public ResponseEntity<HospitalVisitResponseDto> create(
            @PathVariable Long recordId,
            @Valid @RequestBody HospitalVisitCreateRequestDto request) {
        HospitalVisitResponseDto response = hospitalVisitService.create(recordId, request);
        return ResponseEntity.created(URI.create("/api/v1/visits/" + response.getVisitId()))
                .body(response);
    }

    @GetMapping("/records/{recordId}/visits")
    public ResponseEntity<List<HospitalVisitResponseDto>> findByRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(hospitalVisitService.findByRecord(recordId));
    }

    @PatchMapping("/visits/{visitId}")
    public ResponseEntity<HospitalVisitResponseDto> update(
            @PathVariable Long visitId,
            @RequestBody HospitalVisitUpdateRequestDto request) {
        return ResponseEntity.ok(hospitalVisitService.update(visitId, request));
    }

    @DeleteMapping("/visits/{visitId}")
    public ResponseEntity<Void> delete(@PathVariable Long visitId) {
        hospitalVisitService.delete(visitId);
        return ResponseEntity.noContent().build();
    }
}
