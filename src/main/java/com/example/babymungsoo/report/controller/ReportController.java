package com.example.babymungsoo.report.controller;

import com.example.babymungsoo.report.dto.ReportCreateRequestDto;
import com.example.babymungsoo.report.dto.ReportResponseDto;
import com.example.babymungsoo.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 리포트 생성
    // POST /api/v1/reports
    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(
            @Valid @RequestBody ReportCreateRequestDto requestDto) {
        ReportResponseDto response = ReportResponseDto
                .from(reportService.createReport(requestDto.toEntity()));
        return ResponseEntity.ok(response);
    }

    // 분석 기록 ID로 리포트 조회
    // GET /api/v1/reports/record/{recordId}
    @GetMapping("/record/{recordId}")
    public ResponseEntity<ReportResponseDto> getReportByRecordId(
            @PathVariable Long recordId) {
        ReportResponseDto response = ReportResponseDto
                .from(reportService.getReportByRecordId(recordId));
        return ResponseEntity.ok(response);
    }

    // 리포트 상세 조회
    // GET /api/v1/reports/{reportId}
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponseDto> getReportById(
            @PathVariable Long reportId) {
        ReportResponseDto response = ReportResponseDto
                .from(reportService.getReportById(reportId));
        return ResponseEntity.ok(response);
    }

    // 병원 ID로 리포트 목록 조회 (페이지 단위)
    // GET /api/v1/reports/hospital/{hospitalId}?page=0&size=10
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<Page<ReportResponseDto>> getReportsByHospitalId(
            @PathVariable Long hospitalId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportResponseDto> reports = reportService
                .getReportsByHospitalId(hospitalId, pageable)
                .map(ReportResponseDto::from);
        return ResponseEntity.ok(reports);
    }
}