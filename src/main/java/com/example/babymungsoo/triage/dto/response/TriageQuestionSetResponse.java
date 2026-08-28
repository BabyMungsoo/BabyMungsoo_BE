package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.triage.entity.Question;

import java.util.List;

/**
 * 추가 문진 질문 생성 결과.
 *
 * <p>질문 항목은 {@link QuestionResponse}를 그대로 쓴다. 다음 질문 조회
 * ({@code GET /sessions/{id}/next-question})가 같은 타입을 돌려주므로,
 * 프론트가 질문을 한 가지 모양으로만 다루면 된다.
 *
 * <p>{@code sessionId}는 담지 않는다. 호출자가 이미 아는 값이다.
 */
public record TriageQuestionSetResponse(
        boolean needsAdditionalQuestions,
        List<QuestionResponse> questions
) {

    public static TriageQuestionSetResponse from(List<Question> questions) {
        if (questions.isEmpty()) {
            return none();
        }

        return new TriageQuestionSetResponse(
                true,
                questions.stream().map(QuestionResponse::from).toList()
        );
    }

    /** 질문이 필요 없거나 생성에 실패했을 때. */
    public static TriageQuestionSetResponse none() {
        return new TriageQuestionSetResponse(false, List.of());
    }
}
