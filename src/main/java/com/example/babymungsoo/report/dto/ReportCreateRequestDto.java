package com.example.babymungsoo.report.dto;

import com.example.babymungsoo.report.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class ReportCreateRequestDto {

    @NotNull(message = "recordId는 필수입니다.")
    @Positive(message = "recordId는 양수여야 합니다.")
    private Long recordId;

    @NotNull(message = "hospitalId는 필수입니다.")
    @Positive(message = "hospitalId는 양수여야 합니다.")
    private Long hospitalId;

    @NotBlank(message = "reportContent는 필수입니다.")
    private String reportContent;

    @NotBlank(message = "emergencyLevel은 필수입니다.")
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