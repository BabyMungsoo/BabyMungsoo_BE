package com.example.babymungsoo.triage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 완료된 문진 세션을 대상으로 AI 응급도 분석을 요청한다.
 * 증상 정보는 세션(initialSymptom)과 답변에서 조립하므로 별도로 받지 않는다.
 *
 * <p>품종/나이도 세션이 가리키는 반려견에서 조회한다.
 * 클라이언트가 전달한 값을 신뢰하면 실제와 다른 프로필로 분석·저장될 수 있다.
 */
public record TriageAnalyzeRequest(

        @Schema(description = "분석할 문진 세션 ID", example = "1")
        @NotNull(message = "sessionId는 필수입니다.")
        Long sessionId
) {
}
