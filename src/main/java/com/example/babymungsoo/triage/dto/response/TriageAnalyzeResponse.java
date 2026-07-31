package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.AI.Entity.TriageResult;

import java.time.LocalDateTime;
import java.util.List;

public record TriageAnalyzeResponse(
        Long triageResultId,
        Long petId,
        String level,
        String title,
        List<String> reason,
        String guide,
        LocalDateTime createdAt
) {

    public static TriageAnalyzeResponse from(TriageResult triageResult) {
        return new TriageAnalyzeResponse(
                triageResult.getId(),
                triageResult.getPetId(),
                triageResult.getLevel().name(),
                triageResult.getTitle(),
                triageResult.getReason(),
                triageResult.getGuide(),
                triageResult.getCreatedAt()
        );
    }
}
