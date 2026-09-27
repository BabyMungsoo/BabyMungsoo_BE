package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모델 출력 정제 규칙. 프롬프트가 요구하는 개수·형식을 모델이 어겨도 저장·응답이 계약 안에 있어야 한다.
 */
class ClaudeTriageResultTest {

    @Test
    @DisplayName("소견 5개는 4개로, 악화 신호 5개는 4개로, 주의 3개는 2개로 자른다")
    void capsLists() {
        ClaudeTriageResult raw = new ClaudeTriageResult(
                TriageLevel.WATCH,
                List.of("f1", "f2", "f3", "f4", "f5"),
                "가까운 시일 내 진료가 필요합니다.",
                List.of("e1", "e2", "e3", "e4", "e5"),
                List.of("p1", "p2", "p3"));

        ClaudeTriageResult result = raw.sanitized();

        assertThat(result.findings()).containsExactly("f1", "f2", "f3", "f4");
        assertThat(result.escalationSigns()).containsExactly("e1", "e2", "e3", "e4");
        assertThat(result.precautions()).containsExactly("p1", "p2");
    }

    @Test
    @DisplayName("항목 안 줄바꿈은 공백이 된다 — 줄바꿈으로 결합해 저장하므로 두 항목으로 쪼개지면 안 된다")
    void flattensNewlines() {
        ClaudeTriageResult raw = new ClaudeTriageResult(
                TriageLevel.WATCH,
                List.of("어제부터\n두 차례 구토", "물은  마심"),
                "첫 문장.\n둘째 문장.",
                List.of(), List.of());

        ClaudeTriageResult result = raw.sanitized();

        assertThat(result.findings()).containsExactly("어제부터 두 차례 구토", "물은 마심");
        assertThat(result.urgencyReason()).isEqualTo("첫 문장. 둘째 문장.");
    }

    @Test
    @DisplayName("빈 항목·공백·중복은 걷어 내고 null 목록은 빈 목록이 된다")
    void dropsBlanksAndDuplicates() {
        ClaudeTriageResult raw = new ClaudeTriageResult(
                TriageLevel.NORMAL,
                Arrays.asList(" a ", "", null, "a", "b"),
                null,
                null,
                Arrays.asList("x", " x "));

        ClaudeTriageResult result = raw.sanitized();

        assertThat(result.findings()).containsExactly("a", "b");
        assertThat(result.urgencyReason()).isNull();
        assertThat(result.escalationSigns()).isEmpty();
        assertThat(result.precautions()).containsExactly("x");
    }

    @Test
    @DisplayName("IMMEDIATE면 모델이 악화 신호를 줘도 비운다")
    void clearsEscalationSignsForImmediate() {
        ClaudeTriageResult raw = new ClaudeTriageResult(
                TriageLevel.IMMEDIATE,
                List.of("f1"),
                "지금 즉시 진료가 필요합니다.",
                List.of("e1", "e2"),
                List.of("p1"));

        assertThat(raw.sanitized().escalationSigns()).isEmpty();
        assertThat(raw.sanitized().precautions()).containsExactly("p1");
    }

    @Test
    @DisplayName("글자 수 상한과 최소 개수는 강제하지 않는다")
    void doesNotTruncateTextOrPadLists() {
        String longFinding = "가".repeat(80);
        ClaudeTriageResult raw = new ClaudeTriageResult(
                TriageLevel.WATCH, List.of(longFinding), "이유", List.of("e1"), List.of());

        ClaudeTriageResult result = raw.sanitized();

        assertThat(result.findings()).containsExactly(longFinding);
        assertThat(result.escalationSigns()).containsExactly("e1");
    }
}
