package com.example.babymungsoo.AI.Dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Claude가 생성한 추가 문진 질문 한 건.
 *
 * <p>{@link TriageQuestionSet}의 구성 요소로, 구조화 출력 스키마에 그대로 반영된다.
 * 질문에 식별자를 두지 않는 이유는, id는 DB에 저장하면서 서버가 부여하기 때문이다.
 */
public record GeneratedQuestion(

        @JsonPropertyDescription("보호자에게 물어볼 질문 한 문장. 한국어로 쓰고, 한 번에 한 가지만 묻는다")
        String content
) {
}
