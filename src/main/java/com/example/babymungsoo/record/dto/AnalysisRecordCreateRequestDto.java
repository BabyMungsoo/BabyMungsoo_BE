package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class AnalysisRecordCreateRequestDto {

    @NotNull(message = "userId는 필수입니다.")
    @Positive(message = "userId는 양수여야 합니다.")
    private Long userId;

    @NotNull(message = "dogId는 필수입니다.")
    @Positive(message = "dogId는 양수여야 합니다.")
    private Long dogId;

    @NotBlank(message = "symptomText는 필수입니다.")
    private String symptomText;

    @NotBlank(message = "aiResult는 필수입니다.")
    private String aiResult;

    @NotBlank(message = "emergencyLevel은 필수입니다.")
    private String emergencyLevel;

    private String suspectedDisease;

    private String aiGuide;

    /** 분석에 쓴 사진의 media ID. 사진 없이 문진만으로 분석했다면 비워 둡니다. */
    private Long mediaId;

    public AnalysisRecord toEntity() {
        return AnalysisRecord.builder()
                .userId(userId)
                .dogId(dogId)
                .symptomText(symptomText)
                .aiResult(aiResult)
                .emergencyLevel(emergencyLevel)
                .suspectedDisease(suspectedDisease)
                .aiGuide(aiGuide)
                .mediaId(mediaId)
                .build();
    }
}