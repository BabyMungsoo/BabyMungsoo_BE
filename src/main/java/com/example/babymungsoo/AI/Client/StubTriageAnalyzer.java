package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.AI.Entity.TriageLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실호출 없이 고정 응답을 반환하는 Mock 분석기.
 *
 * <p>분석 흐름(분석 → 저장 → 반환) 검증과 개발 중 과금 차단이 목적이다.
 * API 키가 없어도 동작하므로 키 검증을 하지 않는다.
 *
 * <p>{@code claude.api.mock=true}(기본값)일 때 등록된다.
 * {@code matchIfMissing}을 쓰지 않으므로 {@code application.yml}에 해당 속성이
 * 반드시 존재해야 한다. 없으면 {@link ClaudeTriageAnalyzer}와 함께 빈이 0개가 되어 기동에 실패한다.
 */
@Component
@ConditionalOnProperty(name = "claude.api.mock", havingValue = "true")
public class StubTriageAnalyzer implements TriageAnalyzer {

    @Override
    public ClaudeTriageResult analyze(String rawSymptoms, PetProfile pet, List<TriageImage> images) {
        return new ClaudeTriageResult(
                TriageLevel.WATCH,
                List.of(
                        "[MOCK] 증상이 반복적으로 나타남",
                        "[MOCK] 물과 사료는 평소처럼 먹음",
                        "[MOCK] 기력은 평소와 비슷함"
                ),
                "[MOCK] 전신 상태는 안정적으로 확인되지만 증상이 반복되고 있어 가까운 시일 내 진료가 필요합니다.",
                List.of(
                        "[MOCK] 증상 횟수가 늘어남",
                        "[MOCK] 물을 마시지 않음",
                        "[MOCK] 축 늘어져 일어나지 못함"
                ),
                List.of("[MOCK] 이동 전까지 사료는 치워 두세요")
        );
    }
}
