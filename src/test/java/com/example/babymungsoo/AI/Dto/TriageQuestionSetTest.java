package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.AI.Dto.TriageQuestionSet.UsableQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모델 응답 정제 규칙. 모델이 규칙을 어겨도 저장·화면이 깨지지 않아야 한다.
 */
class TriageQuestionSetTest {

    @Test
    @DisplayName("선택지 끝에 '잘 모르겠어요'를 서버가 붙인다")
    void appendsUnknownOption() {
        TriageQuestionSet set = new TriageQuestionSet(true, List.of(
                new GeneratedQuestion("토한 횟수가 몇 번인가요?", List.of("1번", "2~3번", "4번 이상"))));

        UsableQuestion question = set.usableQuestions().get(0);

        assertThat(question.options()).containsExactly("1번", "2~3번", "4번 이상", "잘 모르겠어요");
    }

    @Test
    @DisplayName("모델이 넣은 '모르겠어요' 변형은 지우고 하나만 남긴다")
    void collapsesUnknownVariants() {
        TriageQuestionSet set = new TriageQuestionSet(true, List.of(
                new GeneratedQuestion("물을 마시나요?", List.of("잘 모르겠어요", "네", "모르겠음", "아니요", "잘 모름"))));

        assertThat(set.usableQuestions().get(0).options())
                .containsExactly("네", "아니요", "잘 모르겠어요");
    }

    @Test
    @DisplayName("빈 항목·공백·중복을 거르고 4개에서 자른다")
    void trimsDedupesAndCaps() {
        TriageQuestionSet set = new TriageQuestionSet(true, List.of(
                new GeneratedQuestion("얼마나 됐나요?",
                        Arrays.asList(" 1시간 안 ", "", null, "1시간 안", "오늘", "어제", "며칠 전", "일주일 넘게"))));

        assertThat(set.usableQuestions().get(0).options())
                .containsExactly("1시간 안", "오늘", "어제", "며칠 전", "잘 모르겠어요");
    }

    @Test
    @DisplayName("쓸 만한 선택지가 2개 미만이면 자유 입력 질문으로 낸다")
    void fallsBackToFreeText() {
        TriageQuestionSet set = new TriageQuestionSet(true, List.of(
                new GeneratedQuestion("어디를 다쳤나요?", List.of("잘 모르겠어요")),
                new GeneratedQuestion("언제부터인가요?", null),
                new GeneratedQuestion("무엇을 먹었나요?", List.of("초콜릿"))));

        List<UsableQuestion> questions = set.usableQuestions();

        assertThat(questions).hasSize(3);
        assertThat(questions).allSatisfy(question -> assertThat(question.options()).isEmpty());
    }

    @Test
    @DisplayName("질문 자체의 정제는 그대로다 — 빈 질문 제외, 5개 상한, 필요 없으면 빈 목록")
    void keepsQuestionRules() {
        List<GeneratedQuestion> seven = List.of(
                new GeneratedQuestion(" q1 ", List.of("a", "b")),
                new GeneratedQuestion("", List.of("a", "b")),
                new GeneratedQuestion("q2", List.of("a", "b")),
                new GeneratedQuestion("q3", List.of("a", "b")),
                new GeneratedQuestion("q4", List.of("a", "b")),
                new GeneratedQuestion("q5", List.of("a", "b")),
                new GeneratedQuestion("q6", List.of("a", "b")));

        assertThat(new TriageQuestionSet(true, seven).usableQuestions())
                .extracting(UsableQuestion::content)
                .containsExactly("q1", "q2", "q3", "q4", "q5");
        assertThat(new TriageQuestionSet(false, seven).usableQuestions()).isEmpty();
    }
}
