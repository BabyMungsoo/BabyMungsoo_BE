package com.example.babymungsoo.AI.Dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 추가 문진 질문 생성 결과.
 *
 * <p>SDK의 구조화 출력이 이 레코드에서 JSON 스키마를 도출하므로, 각 필드 설명은
 * 모델에게 전달되는 스펙 역할을 한다({@code ClaudeTriageResult}와 같은 방식).
 *
 * <p>이 단계는 응급도를 판정하지 않는다. 최종 판단에 필요한 정보를 더 모으기 위한
 * 질문만 만든다.
 */
@JsonClassDescription("초기 증상만으로 부족한 정보를 확인하기 위한 추가 문진 질문 목록")
public record TriageQuestionSet(

        @JsonPropertyDescription("추가 질문이 필요하면 true, 초기 증상만으로 충분하면 false")
        boolean needsAdditionalQuestions,

        @JsonPropertyDescription("보호자에게 물어볼 질문. 0개에서 5개까지. "
                + "needsAdditionalQuestions가 false면 빈 배열")
        List<GeneratedQuestion> questions
) {

    /** 한 세션에 만들 수 있는 질문 수 상한. 프롬프트와 저장 단계 양쪽에서 지킨다. */
    public static final int MAX_QUESTIONS = 5;

    /** 모델이 만드는 선택지 상한. 이 뒤에 {@link #UNKNOWN_OPTION}이 붙어 화면에는 최대 5개다. */
    public static final int MAX_OPTIONS = 4;

    /**
     * 선택지가 있는 모든 질문의 마지막 항목. 보호자가 확인하지 못한 것을 "미입력"이 아니라
     * "모름"으로 남기기 위한 것이다. 분석 프롬프트의 미확인 취급 규칙(#48)이 이 값을 받는다.
     */
    public static final String UNKNOWN_OPTION = "잘 모르겠어요";

    /** 정제를 거친 질문 한 건. 선택지가 비어 있으면 자유 입력 질문이다. */
    public record UsableQuestion(String content, List<String> options) {
    }

    /** 질문이 필요 없거나 생성에 실패했을 때의 결과. */
    public static TriageQuestionSet none() {
        return new TriageQuestionSet(false, List.of());
    }

    /**
     * 실제로 쓸 수 있는 질문만 골라 상한까지 자른다.
     *
     * <p>모델이 빈 문자열을 섞어 보내거나 5개를 넘겨 보낼 수 있어 저장 전에 한 번 거른다.
     * 선택지도 같은 이유로 정제한다 — 빈 항목·중복 제거, 상한 절단, 그리고 모델이
     * "잘 모르겠어요"를 넣었든 안 넣었든 서버가 마지막에 하나만 둔다.
     *
     * <p>정제 후 선택지가 2개 미만이면 고를 것이 없으므로 선택지를 비워 자유 입력 질문으로 낸다.
     * 그래야 모델이 선택지를 못 만든 질문도 보호자에게 도달한다.
     */
    public List<UsableQuestion> usableQuestions() {
        if (!needsAdditionalQuestions || questions == null) {
            return List.of();
        }

        return questions.stream()
                .filter(question -> question != null && question.content() != null)
                .filter(question -> !question.content().isBlank())
                .map(question -> new UsableQuestion(
                        question.content().strip(),
                        sanitizeOptions(question.options())))
                .limit(MAX_QUESTIONS)
                .toList();
    }

    private static List<String> sanitizeOptions(List<String> raw) {
        if (raw == null) {
            return List.of();
        }

        Set<String> distinct = new LinkedHashSet<>();
        for (String option : raw) {
            if (option == null) {
                continue;
            }
            String trimmed = option.strip();
            if (trimmed.isEmpty() || isUnknownVariant(trimmed)) {
                continue;
            }
            distinct.add(trimmed);
            if (distinct.size() == MAX_OPTIONS) {
                break;
            }
        }

        if (distinct.size() < 2) {
            return List.of();
        }

        List<String> options = new ArrayList<>(distinct);
        options.add(UNKNOWN_OPTION);
        return List.copyOf(options);
    }

    /**
     * "잘 모르겠어요", "모르겠음", "잘 모름", "몰라요"처럼 모델이 제각각 쓰는 미확인 선택지를
     * 한 가지로 모은다. "모름"은 받침이 붙어 "모르"와 다른 글자라 따로 본다.
     */
    private static boolean isUnknownVariant(String option) {
        return option.contains("모르") || option.contains("모름") || option.contains("몰라");
    }
}
