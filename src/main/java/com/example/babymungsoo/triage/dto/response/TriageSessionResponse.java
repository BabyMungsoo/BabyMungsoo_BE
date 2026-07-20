package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.triage.entity.TriageSession;

import java.time.LocalDateTime;
import java.util.List;

public record TriageSessionResponse(
        Long sessionId,
        Long petId,
        String initialSymptom,
        String symptomCategory,
        String status,
        List<AnswerResponse> answers,
        LocalDateTime createdAt
) {

    public static TriageSessionResponse from(TriageSession session) {
        return new TriageSessionResponse(
                session.getId(),
                session.getPetId(),
                session.getInitialSymptom(),
                session.getSymptomCategory(),
                session.getStatus().name(),
                session.getAnswers().stream()
                        .map(AnswerResponse::from)
                        .toList(),
                session.getCreatedAt()
        );
    }
}
