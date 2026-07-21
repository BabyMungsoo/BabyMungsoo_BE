package com.example.babymungsoo.report.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.service.HospitalService;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import com.example.babymungsoo.report.entity.Report;
import com.example.babymungsoo.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final AnalysisRecordService analysisRecordService;
    private final HospitalService hospitalService;

    // 리포트 생성
    @Transactional
    public Report createReport(Report report) {
        // recordId / hospitalId 존재 검증
        analysisRecordService.getRecordById(report.getRecordId());
        hospitalService.getHospitalById(report.getHospitalId());

        // 동일 recordId에 대한 리포트 중복 저장 방지
        reportRepository.findByRecordId(report.getRecordId())
                .ifPresent(existing -> {
                    throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
                });

        return reportRepository.save(report);
    }

    // 분석 기록 ID로 리포트 조회
    public Report getReportByRecordId(Long recordId) {
        return reportRepository.findByRecordId(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }

    // 리포트 상세 조회
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }

    // 병원 ID로 리포트 목록 조회 (페이지 단위)
    public Page<Report> getReportsByHospitalId(Long hospitalId, Pageable pageable) {
        return reportRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);
    }
}