package com.example.babymungsoo.record.dto;

import lombok.Getter;

/**
 * PATCH /api/v1/records/{recordId} 요청 본문.
 *
 * 보낸 필드만 반영합니다. aiResult / aiGuide 는 AI 가 만든 값이라 수정 대상에서 제외했고,
 * 사용자가 직접 적었거나 정정할 수 있는 값만 열어 둡니다.
 *
 * suspectedDisease 는 null 로 보내면 '값 없음'으로 지웁니다.
 */
@Getter
public class AnalysisRecordUpdateRequestDto {

    private String symptomText;

    private String emergencyLevel;

    private String suspectedDisease;
}
