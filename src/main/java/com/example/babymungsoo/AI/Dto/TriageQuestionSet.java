package com.example.babymungsoo.AI.Dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

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

    /** 질문이 필요 없거나 생성에 실패했을 때의 결과. */
    public static TriageQuestionSet none() {
        return new TriageQuestionSet(false, List.of());
    }

    /**
     * 실제로 쓸 수 있는 질문만 골라 상한까지 자른다.
     *
     * <p>모델이 빈 문자열을 섞어 보내거나 5개를 넘겨 보낼 수 있어 저장 전에 한 번 거른다.
     */
    public List<String> usableContents() {
        if (!needsAdditionalQuestions || questions == null) {
            return List.of();
        }

        return questions.stream()
                .filter(question -> question != null && question.content() != null)
                .map(question -> question.content().trim())
                .filter(content -> !content.isEmpty())
                .limit(MAX_QUESTIONS)
                .toList();
    }
}
