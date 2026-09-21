package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Claude가 반환하는 응급도 분석 결과.
 * SDK의 구조화 출력(Structured Output)이 이 레코드로부터 JSON 스키마를 도출하므로,
 * 각 필드 설명은 모델에게 전달되는 스펙 역할을 한다.
 *
 * <p>결론 문구(제목)는 여기 없다. "지금 병원에 가야 하는가"는 등급의 함수라
 * {@code TriageLevel.headline()}에서 코드가 만든다. 모델이 쓰게 두면 등급과 결론이
 * 어긋날 수 있고, 그 자리에 병명이 섞여 들어올 통로도 된다.
 */
@JsonClassDescription("반려견 증상에 대한 응급도 분석 결과")
public record ClaudeTriageResult(

        @JsonPropertyDescription("응급도. IMMEDIATE(즉시 내원), WATCH(주의 관찰), NORMAL(일반 관리) 중 하나")
        TriageLevel level,

        @JsonPropertyDescription("입력에서 확인된 소견. 2~4개, 각각 40자 이내의 짧은 한글 구절. "
                + "증상을 그대로 옮기지 말고 판단에 쓴 사실로 정리한다. "
                + "언급되지 않은 항목을 없음이나 정상으로 바꿔 쓰지 않는다. "
                + "병명이나 진단명은 쓰지 않는다")
        List<String> findings,

        @JsonPropertyDescription("소견이 왜 그 등급의 시급성으로 이어지는지 설명하는 한글 1~2문장. "
                + "첫 문장에 병원에 가야 하는지 여부가 나와야 한다. "
                + "판단한 응급도와 모순되지 않아야 한다. "
                + "언급되지 않은 항목을 없음이나 정상으로 바꿔 등급을 낮추는 문장은 쓰지 않는다")
        String urgencyReason,

        @JsonPropertyDescription("이 변화가 나타나면 바로 병원에 가야 한다는 악화 신호. "
                + "보호자가 눈으로 확인할 수 있는 변화로 쓴다. "
                + "WATCH와 NORMAL은 1~4개, IMMEDIATE는 빈 배열")
        List<String> escalationSigns,

        @JsonPropertyDescription("병원에 도착하기 전까지 상태를 악화시키지 않기 위한 주의 사항. "
                + "0~2개, 각각 40자 이내. 해당하는 것이 없으면 빈 배열로 두고 억지로 채우지 않는다. "
                + "이동·보온·안정, 물과 사료 치우기, 관찰 항목, 수의사에게 전달할 정보만 허용한다. "
                + "출혈 시 깨끗한 천이나 거즈로 눌러 지혈하도록 하는 안내는 병원 이동을 전제로 허용한다. "
                + "단 눈이나 눈 주변 출혈에는 압박 지혈을 안내하지 않고 눈을 건드리지 말고 이동하도록 안내한다. "
                + "그 밖의 투약·처치 지시, 병명 확정, 검사 항목 특정은 진료행위이므로 포함하지 않는다")
        List<String> precautions
) {
}
