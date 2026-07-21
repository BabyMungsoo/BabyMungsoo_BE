package com.example.babymungsoo.report.dto;

import com.example.babymungsoo.report.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponseDto {

    private Long reportId;
    private Long recordId;
    private Long hospitalId;
    private String reportContent;
    private String emergencyLevel;
    private LocalDateTime createdAt;

    public static ReportResponseDto from(Report report) {
        return ReportResponseDto.builder()
                .reportId(report.getReportId())
                .recordId(report.getRecordId())
                .hospitalId(report.getHospitalId())
                .reportContent(report.getReportContent())
                .emergencyLevel(report.getEmergencyLevel())
                .createdAt(report.getCreatedAt())
                .build();
    }
}