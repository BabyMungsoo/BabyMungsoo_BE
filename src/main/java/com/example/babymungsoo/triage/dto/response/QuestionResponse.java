package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.triage.entity.Question;

public record QuestionResponse(
        Long id,
        String code,
        String content,
        String symptomCategory,
        Integer orderNo
) {

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getCode(),
                question.getContent(),
                question.getSymptomCategory(),
                question.getOrderNo()
        );
    }
}
