package com.example.babymungsoo.record.dto;

import lombok.Getter;

/**
 * PATCH /api/v1/records/{recordId} 요청 본문.
 *
 * 보낸 필드만 반영합니다. 고칠 수 있는 값은 보호자가 직접 적은 증상뿐입니다.
 *
 * 응급도·의심질환·aiResult·aiGuide 는 모두 AI 가 판단한 값이라 받지 않습니다.
 * 보호자가 바꿀 수 있게 두면, 'IMMEDIATE' 로 판정된 기록을 'NORMAL' 로 바꿔 둘 수 있고
 * 그 기록을 근거로 다음 판단을 하게 되어 위험합니다.
 */
@Getter
public class AnalysisRecordUpdateRequestDto {

    private String symptomText;
}
