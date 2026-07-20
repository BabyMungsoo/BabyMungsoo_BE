package com.example.babymungsoo.triage.dto.request;

public record AnswerCreateRequest(
        Long questionId,
        String content
) {
}
