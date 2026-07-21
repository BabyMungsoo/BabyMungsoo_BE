package com.example.babymungsoo.report.service;

import com.example.babymungsoo.report.entity.Report;
import com.example.babymungsoo.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    // 리포트 생성
    @Transactional
    public Report createReport(Report report) {
        return reportRepository.save(report);
    }

    // 분석 기록 ID로 리포트 조회
    public Report getReportByRecordId(Long recordId) {
        return reportRepository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "리포트를 찾을 수 없습니다. recordId: " + recordId));
    }

    // 리포트 상세 조회
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "리포트를 찾을 수 없습니다. id: " + reportId));
    }

    // 병원 ID로 리포트 목록 조회
    public List<Report> getReportsByHospitalId(Long hospitalId) {
        return reportRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId);
    }
}