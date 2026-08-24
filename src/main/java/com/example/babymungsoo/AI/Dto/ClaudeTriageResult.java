package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Claude가 반환하는 응급도 분석 결과.
 * SDK의 구조화 출력(Structured Output)이 이 레코드로부터 JSON 스키마를 도출하므로,
 * 각 필드 설명은 모델에게 전달되는 스펙 역할을 한다.
 */
@JsonClassDescription("반려견 증상에 대한 응급도 분석 결과")
public record ClaudeTriageResult(

        @JsonPropertyDescription("응급도. IMMEDIATE(즉시 내원), WATCH(주의 관찰), NORMAL(일반 관리) 중 하나")
        TriageLevel level,

        @JsonPropertyDescription("보호자가 한눈에 상황을 파악할 수 있는 30자 이내의 한글 요약 제목")
        String title,

        @JsonPropertyDescription("해당 응급도로 판단한 근거. 항목당 한 문장씩 2~4개의 한글 문장. "
                + "병명을 단정하지 말고 관찰된 증상에 근거해 서술한다")
        List<String> reason,

        @JsonPropertyDescription("보호자가 지금 취해야 할 조치를 안내하는 한글 문단. "
                + "내원 시급성, 상태를 악화시키지 않기 위한 일반적인 조치, 관찰 항목만 담는다. "
                + "투약·처치 지시, 병명 확정, 검사 항목 특정은 진료행위이므로 포함하지 않는다")
        String guide
) {
}
