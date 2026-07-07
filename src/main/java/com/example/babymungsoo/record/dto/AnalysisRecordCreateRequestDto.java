package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import lombok.Getter;

@Getter
public class AnalysisRecordCreateRequestDto {

    private Long userId;
    private Long dogId;
    private String symptomText;
    private String aiResult;
    private String emergencyLevel;
    private String suspectedDisease;
    private String aiGuide;

    public AnalysisRecord toEntity() {
        return AnalysisRecord.builder()
                .userId(userId)
                .dogId(dogId)
                .symptomText(symptomText)
                .aiResult(aiResult)
                .emergencyLevel(emergencyLevel)
                .suspectedDisease(suspectedDisease)
                .aiGuide(aiGuide)
                .build();
    }
}