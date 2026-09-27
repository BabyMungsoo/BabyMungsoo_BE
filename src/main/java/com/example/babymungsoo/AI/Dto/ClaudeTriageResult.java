package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** 소견·악화 신호 상한. 프롬프트 규칙과 같은 값이다. */
    public static final int MAX_FINDINGS = 4;
    public static final int MAX_ESCALATION_SIGNS = 4;
    public static final int MAX_PRECAUTIONS = 2;

    /**
     * 저장하기 전에 모델 출력을 계약에 맞게 다듬는다.
     *
     * <p>개수·형식 규칙은 프롬프트와 스키마 설명에 적혀 있을 뿐 모델이 반드시 지키지는 않는다.
     * 실호출 검증에서 20건 중 1건이 소견을 5개 냈고, IMMEDIATE에 악화 신호를 붙인 경우도 있었다.
     * 여기서 잡지 않으면 그대로 저장돼 응답이 계약 밖으로 나간다.
     *
     * <ul>
     *   <li>공백 정리, 빈 항목·중복 제거</li>
     *   <li>항목 안의 줄바꿈은 공백으로 — 목록을 줄바꿈으로 결합해 저장하므로,
     *       항목에 줄바꿈이 있으면 읽을 때 두 항목으로 쪼개진다</li>
     *   <li>상한에서 절단. 뒤쪽 항목을 버린다</li>
     *   <li>IMMEDIATE면 악화 신호를 비운다. "지금은 아니지만 이게 보이면 즉시"라는 조건이라
     *       즉시 내원에는 성립하지 않는다</li>
     * </ul>
     *
     * <p>글자 수 상한(40자)은 자르지 않는다. 문장 중간이 잘리면 뜻이 깨지고, 실측에서 넘긴 적이 없다.
     * 최소 개수(소견 2개)도 강제하지 않는다. 없는 소견을 만들어 낼 수는 없다.
     */
    public ClaudeTriageResult sanitized() {
        return new ClaudeTriageResult(
                level,
                cleanList(findings, MAX_FINDINGS),
                cleanText(urgencyReason),
                level == TriageLevel.IMMEDIATE ? List.of() : cleanList(escalationSigns, MAX_ESCALATION_SIGNS),
                cleanList(precautions, MAX_PRECAUTIONS)
        );
    }

    private static List<String> cleanList(List<String> raw, int max) {
        if (raw == null) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String item : raw) {
            String cleaned = cleanText(item);
            if (cleaned == null) {
                continue;
            }
            distinct.add(cleaned);
            if (distinct.size() == max) {
                break;
            }
        }
        return List.copyOf(distinct);
    }

    /** 앞뒤 공백을 걷고 안쪽 줄바꿈·연속 공백을 한 칸으로 만든다. 비면 null이다. */
    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("\\s+", " ").strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
