package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.triage.entity.Question;

import java.util.List;

public record QuestionResponse(
        Long id,
        String code,
        String content,
        String symptomCategory,
        Integer orderNo,
        /** CHOICE면 options 중 하나를 고르는 질문, TEXT면 자유 입력. 마스터 질문은 항상 TEXT다. */
        AnswerType answerType,
        /** 화면에 보이는 순서 그대로. 마지막은 항상 "잘 모르겠어요". TEXT면 빈 배열 */
        List<String> options
) {

    public enum AnswerType { CHOICE, TEXT }

    public static QuestionResponse from(Question question) {
        boolean choice = question.hasOptions();
        return new QuestionResponse(
                question.getId(),
                question.getCode(),
                question.getContent(),
                question.getSymptomCategory(),
                question.getOrderNo(),
                choice ? AnswerType.CHOICE : AnswerType.TEXT,
                choice ? List.copyOf(question.getOptions()) : List.of()
        );
    }
}
