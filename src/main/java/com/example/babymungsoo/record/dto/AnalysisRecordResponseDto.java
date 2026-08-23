package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalysisRecordResponseDto {

    private Long recordId;
    private Long userId;
    private Long dogId;
    private String symptomText;
    private String aiResult;
    private String emergencyLevel;
    private String suspectedDisease;
    private String aiGuide;
    private Long mediaId;
    private LocalDateTime createdAt;

    public static AnalysisRecordResponseDto from(AnalysisRecord record) {
        return AnalysisRecordResponseDto.builder()
                .recordId(record.getRecordId())
                .userId(record.getUserId())
                .dogId(record.getDogId())
                .symptomText(record.getSymptomText())
                .aiResult(record.getAiResult())
                .emergencyLevel(record.getEmergencyLevel())
                .suspectedDisease(record.getSuspectedDisease())
                .aiGuide(record.getAiGuide())
                .mediaId(record.getMediaId())
                .createdAt(record.getCreatedAt())
                .build();
    }
}