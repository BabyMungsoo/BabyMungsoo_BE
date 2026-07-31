package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
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
    public ClaudeTriageResult analyze(String rawSymptoms, String breed, Integer age, String ageUnit) {
        return new ClaudeTriageResult(
                TriageLevel.WATCH,
                "[MOCK] 경과 관찰이 필요한 상태입니다",
                List.of(
                        "[MOCK] 증상이 반복적으로 나타나고 있어 악화 가능성이 있습니다.",
                        "[MOCK] 의식 저하나 출혈 등 즉시 내원이 필요한 징후는 확인되지 않았습니다."
                ),
                "[MOCK] 물과 사료를 소량씩 나누어 주고 12시간 동안 상태를 지켜보세요. "
                        + "증상이 심해지거나 새로운 증상이 나타나면 즉시 동물병원에 방문하세요."
        );
    }
}
