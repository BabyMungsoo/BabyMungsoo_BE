package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.service.HospitalVisitService.VisitSummary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<Long> mediaIds;
    private LocalDateTime createdAt;

    /**
     * 병원에 다녀왔는지 물어본 질문에 답했는지. 가지 않았다는 답도 답이다.
     * 프론트가 질문 카드를 숨기는 기준이라, 방문 수가 0이어도 true 일 수 있다.
     */
    private boolean followUpAnswered;

    /** 실제로 다녀온 횟수. 목록의 "진료 완료" 표시에 쓴다. */
    private int visitCount;

    /** 팔로우업 답변이 아직 없는 경우(새로 만든 기록 등). */
    public static AnalysisRecordResponseDto from(AnalysisRecord record) {
        return from(record, VisitSummary.empty());
    }

    public static AnalysisRecordResponseDto from(AnalysisRecord record, VisitSummary summary) {
        return AnalysisRecordResponseDto.builder()
                .followUpAnswered(summary.answered())
                .visitCount(summary.visitCount())
                .recordId(record.getRecordId())
                .userId(record.getUserId())
                .dogId(record.getDogId())
                .symptomText(record.getSymptomText())
                .aiResult(record.getAiResult())
                .emergencyLevel(record.getEmergencyLevel())
                .suspectedDisease(record.getSuspectedDisease())
                .aiGuide(record.getAiGuide())
                .mediaIds(record.getMediaIds())
                .createdAt(record.getCreatedAt())
                .build();
    }
}