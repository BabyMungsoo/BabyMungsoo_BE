package com.example.babymungsoo.report.dto;

import com.example.babymungsoo.report.entity.Report;
import lombok.Getter;

@Getter
public class ReportCreateRequestDto {

    private Long recordId;
    private Long hospitalId;
    private String reportContent;
    private String emergencyLevel;

    public Report toEntity() {
        return Report.builder()
                .recordId(recordId)
                .hospitalId(hospitalId)
                .reportContent(reportContent)
                .emergencyLevel(emergencyLevel)
                .build();
    }
}