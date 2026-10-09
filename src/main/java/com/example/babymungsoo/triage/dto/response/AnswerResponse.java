package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.triage.entity.Answer;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long questionId,
        String content,
        LocalDateTime createdAt
) {

    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion() != null ? answer.getQuestion().getId() : null,
                answer.getContent(),
                answer.getCreatedAt()
        );
    }
}
