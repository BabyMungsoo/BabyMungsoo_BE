package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.global.exception.CustomException;

/**
 * 증상 정보를 받아 응급도 분석 결과를 반환하는 분석기.
 *
 * <p>담당 범위는 프롬프트 생성 / 외부 호출 / 결과 반환 / 예외 변환까지다.
 * TriageResult 생성·저장, 세션 조회 등 비즈니스 로직은 {@code TriageService}가 담당한다.
 *
 * <p>구현체는 {@code claude.api.mock} 설정값으로 전환한다.
 * <ul>
 *   <li>{@code true} → {@link StubTriageAnalyzer} (실호출·과금 없음, 기본값)</li>
 *   <li>{@code false} → {@link ClaudeTriageAnalyzer} (Claude 실제 호출)</li>
 * </ul>
 */
public interface TriageAnalyzer {

    /**
     * 증상 정보를 분석해 응급도 결과를 반환한다.
     *
     * @param rawSymptoms 초기 증상과 문진 답변을 합친 원본 텍스트
     * @param pet         분석 시점의 반려견 정보
     * @return 응급도 분석 결과
     * @throws CustomException API 키 미설정, 호출 실패, 타임아웃, 응답 파싱 실패
     */
    ClaudeTriageResult analyze(String rawSymptoms, PetProfile pet);
}
