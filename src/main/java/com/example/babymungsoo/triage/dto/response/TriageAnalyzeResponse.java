package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.example.babymungsoo.AI.Entity.TriageResult;

import java.time.LocalDateTime;
import java.util.List;

public record TriageAnalyzeResponse(
        Long triageResultId,
        Long petId,
        String level,
        String title,
        List<String> findings,
        String urgencyReason,
        List<String> escalationSigns,
        List<String> precautions,
        LocalDateTime createdAt
) {

    /**
     * 저장된 결과를 응답으로 옮긴다.
     *
     * <p>새 구조({@code findings} 이하)가 생기기 전에 저장된 행은 근거 목록({@code reason})과
     * 안내 문단({@code guide})만 갖고 있다. 분석이 세션당 멱등이라 그 행은 재분석해도
     * 그대로이므로, 여기서 옛 컬럼을 새 자리에 대체해 클라이언트가 두 구조를 다루지 않게 한다.
     * 제목도 옛 행은 모델이 쓴 문자열(병명이 섞여 있을 수 있음)이라 등급에서 다시 만든다.
     */
    public static TriageAnalyzeResponse from(TriageResult triageResult) {
        TriageLevel level = triageResult.getLevel();
        boolean legacy = triageResult.getFindings() == null && triageResult.getUrgencyReason() == null;

        List<String> findings = legacy
                ? nullToEmpty(triageResult.getReason())
                : TriageResult.splitLines(triageResult.getFindings());
        String urgencyReason = legacy
                ? triageResult.getGuide()
                : triageResult.getUrgencyReason();
        String title = legacy || triageResult.getTitle() == null
                ? level.headline()
                : triageResult.getTitle();

        return new TriageAnalyzeResponse(
                triageResult.getId(),
                triageResult.getPetId(),
                level.name(),
                title,
                findings,
                urgencyReason,
                TriageResult.splitLines(triageResult.getEscalationSigns()),
                TriageResult.splitLines(triageResult.getPrecautions()),
                triageResult.getCreatedAt()
        );
    }

    private static List<String> nullToEmpty(List<String> list) {
        return list == null ? List.of() : list;
    }
}
