package com.example.babymungsoo.report.controller;

import com.example.babymungsoo.report.dto.ReportCreateRequestDto;
import com.example.babymungsoo.report.dto.ReportResponseDto;
import com.example.babymungsoo.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 리포트 생성
    // POST /api/v1/reports
    @PostMapping
    public ResponseEntity<ReportResponseDto> createReport(
            @RequestBody ReportCreateRequestDto requestDto) {
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

    // 병원 ID로 리포트 목록 조회
    // GET /api/v1/reports/hospital/{hospitalId}
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<ReportResponseDto>> getReportsByHospitalId(
            @PathVariable Long hospitalId) {
        List<ReportResponseDto> reports = reportService
                .getReportsByHospitalId(hospitalId)
                .stream()
                .map(ReportResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reports);
    }
}