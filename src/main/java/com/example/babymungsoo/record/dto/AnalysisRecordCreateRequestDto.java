package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

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

    /** 분석에 쓴 사진들의 media ID 목록(최대 5장). 사진 없이 문진만으로 분석했다면 비워 둡니다. */
    @Size(max = 5, message = "사진은 최대 5장까지 등록할 수 있습니다.")
    private List<Long> mediaIds;

    public AnalysisRecord toEntity() {
        AnalysisRecord.AnalysisRecordBuilder builder = AnalysisRecord.builder()
                .userId(userId)
                .dogId(dogId)
                .symptomText(symptomText)
                .aiResult(aiResult)
                .emergencyLevel(emergencyLevel)
                .suspectedDisease(suspectedDisease)
                .aiGuide(aiGuide);
        if (mediaIds != null) {
            builder.mediaIds(mediaIds);
        }
        return builder.build();
    }
}