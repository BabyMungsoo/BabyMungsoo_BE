package com.example.babymungsoo.triage.dto.request;

/**
 * 완료된 문진 세션을 대상으로 AI 응급도 분석을 요청한다.
 * 증상 정보는 세션(initialSymptom)과 답변에서 조립하므로 별도로 받지 않는다.
 *
 * <p>품종/나이는 Pet 도메인이 없어 클라이언트가 직접 전달한다.
 * TODO: Pet 도메인 구현 시 세션의 petId로 조회하도록 변경
 */
public record TriageAnalyzeRequest(
        Long sessionId,
        String breed,
        Integer age,
        String ageUnit
) {
}
