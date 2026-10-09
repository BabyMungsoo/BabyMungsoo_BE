package com.example.babymungsoo.triage.dto.response;

import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.example.babymungsoo.AI.Entity.TriageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결과 행 → 응답 변환. DB 없이 엔티티를 빌더로 만들어 검증한다.
 *
 * 분석이 세션당 멱등이라 새 구조(findings 이하)가 생기기 전 저장된 행은 영영 옛 구조로 남는다.
 * 클라이언트가 두 구조를 다루지 않도록 변환에서 대체하는 규칙을 여기서 고정한다.
 */
class TriageAnalyzeResponseTest {

    @Test
    @DisplayName("새 구조 행은 줄바꿈 결합 컬럼을 목록으로 풀어 돌려준다")
    void newRow() {
        TriageResult row = TriageResult.builder()
                .level(TriageLevel.WATCH)
                .title(TriageLevel.WATCH.headline())
                .findings("어제부터 두 차례 구토\n물과 사료는 평소처럼 먹음")
                .urgencyReason("가까운 시일 내 진료가 필요합니다.")
                .escalationSigns("토한 횟수가 늘어남\n물을 마시지 않음")
                .precautions(null)
                .build();

        TriageAnalyzeResponse response = TriageAnalyzeResponse.from(row);

        assertThat(response.title()).isEqualTo("빠른 시일 내 병원 진료가 필요해요");
        assertThat(response.findings()).containsExactly("어제부터 두 차례 구토", "물과 사료는 평소처럼 먹음");
        assertThat(response.urgencyReason()).isEqualTo("가까운 시일 내 진료가 필요합니다.");
        assertThat(response.escalationSigns()).containsExactly("토한 횟수가 늘어남", "물을 마시지 않음");
        assertThat(response.precautions()).isEmpty();
    }

    @Test
    @DisplayName("옛 구조 행은 reason→findings, guide→urgencyReason 으로 대체하고 제목은 등급에서 다시 만든다")
    void legacyRow() {
        TriageResult row = TriageResult.builder()
                .level(TriageLevel.IMMEDIATE)
                .title("배뇨 곤란과 복부 통증, 요로 폐쇄 의심으로 즉시 진료 필요")
                .reason(List.of("근거 1", "근거 2"))
                .guide("옛 안내 문단")
                .build();

        TriageAnalyzeResponse response = TriageAnalyzeResponse.from(row);

        assertThat(response.title()).isEqualTo("지금 바로 동물병원에 가세요");
        assertThat(response.findings()).containsExactly("근거 1", "근거 2");
        assertThat(response.urgencyReason()).isEqualTo("옛 안내 문단");
        assertThat(response.escalationSigns()).isEmpty();
        assertThat(response.precautions()).isEmpty();
    }

    @Test
    @DisplayName("빈 목록은 null 로 저장되고 null 은 빈 목록으로 읽힌다")
    void joinAndSplit() {
        assertThat(TriageResult.joinLines(List.of())).isNull();
        assertThat(TriageResult.joinLines(List.of(" ", ""))).isNull();
        assertThat(TriageResult.joinLines(List.of("a ", " b"))).isEqualTo("a\nb");
        assertThat(TriageResult.splitLines(null)).isEmpty();
        assertThat(TriageResult.splitLines("a\n\nb\n")).containsExactly("a", "b");
    }
}
