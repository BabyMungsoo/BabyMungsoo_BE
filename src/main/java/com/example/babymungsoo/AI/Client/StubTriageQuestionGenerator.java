package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.GeneratedQuestion;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.AI.Dto.TriageQuestionSet;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 실호출 없이 추가 문진 흐름을 확인하기 위한 대역.
 *
 * <p>{@code claude.api.mock=true}(기본값)일 때 등록된다.
 *
 * <p>세 갈래를 모두 눌러볼 수 있어야 흐름 검증이 끝나므로, 초기 증상에 특정 표시가
 * 들어 있으면 그 경로로 응답한다. 실제 생성기는 이런 분기가 없다.
 * <ul>
 *   <li>"질문없음"이 들어 있으면 → 질문 0개</li>
 *   <li>"질문실패"가 들어 있으면 → 예외(호출자가 삼키는지 확인용)</li>
 *   <li>그 외 → 고정 질문 3개</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "claude.api.mock", havingValue = "true")
public class StubTriageQuestionGenerator implements TriageQuestionGenerator {

    @Override
    public TriageQuestionSet generate(String initialSymptom, PetProfile pet, List<TriageImage> images) {
        String symptom = initialSymptom == null ? "" : initialSymptom;

        if (symptom.contains("질문실패")) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        }

        if (symptom.contains("질문없음")) {
            return TriageQuestionSet.none();
        }

        // 세 번째는 선택지를 비워 둔다. 모델이 선택지를 못 만든 질문이 자유 입력으로
        // 내려가는 경로(answerType = TEXT)까지 Stub으로 눌러볼 수 있어야 한다.
        return new TriageQuestionSet(true, List.of(
                new GeneratedQuestion("[MOCK] 증상이 처음 나타난 것은 언제인가요?",
                        List.of("[MOCK] 1시간 안", "[MOCK] 오늘", "[MOCK] 어제", "[MOCK] 며칠 전부터")),
                new GeneratedQuestion("[MOCK] 같은 증상이 몇 번이나 반복됐나요?",
                        List.of("[MOCK] 1번", "[MOCK] 2~3번", "[MOCK] 4번 이상", "[MOCK] 계속 반복돼요")),
                new GeneratedQuestion("[MOCK] 평소와 비교해 기운이 떨어져 보이나요?", List.of())
        ));
    }
}
