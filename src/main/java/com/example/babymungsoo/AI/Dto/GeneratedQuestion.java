package com.example.babymungsoo.AI.Dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Claude가 생성한 추가 문진 질문 한 건.
 *
 * <p>{@link TriageQuestionSet}의 구성 요소로, 구조화 출력 스키마에 그대로 반영된다.
 * 질문에 식별자를 두지 않는 이유는, id는 DB에 저장하면서 서버가 부여하기 때문이다.
 *
 * <p>선택지는 모델이 질문과 함께 만든다. 응급 상황에서 보호자가 문장을 치는 대신
 * 누르기만 하면 되도록 하기 위해서다. "잘 모르겠어요"는 모델이 빠뜨려도 서버가
 * 마지막에 보강하므로({@link TriageQuestionSet#usableQuestions()}) 여기서는 강제하지 않는다.
 */
public record GeneratedQuestion(

        @JsonPropertyDescription("보호자에게 물어볼 질문 한 문장. 한국어로 쓰고, 한 번에 한 가지만 묻는다")
        String content,

        @JsonPropertyDescription("보호자가 고를 답변 선택지 2~4개. 각각 20자 이내의 한국어. "
                + "서로 겹치지 않고 응급도를 가르는 범위를 덮어야 한다. "
                + "보호자가 눈으로 확인할 수 있는 사실로 쓰고, 병명·진단·응급도 표현은 담지 않는다. "
                + "가벼운 것에서 심한 것 순서로 놓는다. \"잘 모르겠어요\"는 넣지 않는다(서버가 마지막에 붙인다)")
        List<String> options
) {
}
