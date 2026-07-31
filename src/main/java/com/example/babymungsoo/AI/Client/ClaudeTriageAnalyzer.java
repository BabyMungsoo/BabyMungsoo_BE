package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Claude API 호출 전담 어댑터.
 *
 * <p>{@code claude.api.mock=false}일 때 등록된다. 기본값은 {@code true}(Mock)이므로
 * 이 구현체를 쓰려면 {@code CLAUDE_API_MOCK=false}를 명시해야 한다.
 *
 * <p><b>아직 실제 호출은 구현되지 않았다.</b> 인터페이스 분리 단계까지만 반영된 상태이며,
 * 교체 지점은 {@link #request(String)} 하나다.
 */
@Component
@ConditionalOnProperty(name = "claude.api.mock", havingValue = "false")
public class ClaudeTriageAnalyzer implements TriageAnalyzer {

    private static final String SYSTEM_PROMPT = """
            당신은 반려견 응급 증상을 분류하는 수의 트리아지 보조 시스템입니다.
            보호자가 입력한 증상과 문진 답변을 근거로 응급도를 판단하세요.

            판단 기준
            - IMMEDIATE: 생명이 위험할 수 있어 즉시 동물병원 내원이 필요한 상태
            - WATCH: 당장 위급하지는 않으나 악화 가능성이 있어 주의 관찰이 필요한 상태
            - NORMAL: 가정에서 관리하며 경과를 지켜봐도 되는 상태

            작성 규칙
            - 모든 문장은 한국어로, 보호자가 이해할 수 있는 쉬운 표현을 사용합니다.
            - 확실하지 않은 경우 더 높은 응급도를 선택합니다.
            - 진단명을 단정하지 말고, 관찰된 증상에 근거해 서술합니다.
            - 근거(reason)는 입력된 증상과 답변에서 직접 확인되는 내용만 사용합니다.
            """;

    private final String apiKey;
    private final String model;

    public ClaudeTriageAnalyzer(
            @Value("${claude.api.key:}") String apiKey,
            @Value("${claude.api.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ClaudeTriageResult analyze(String rawSymptoms, String breed, Integer age, String ageUnit) {
        validateApiKey();

        String userPrompt = buildUserPrompt(rawSymptoms, breed, age, ageUnit);

        try {
            return request(userPrompt);
        } catch (CustomException e) {
            // request() 내부에서 이미 의미 있는 ErrorCode로 변환된 예외는 그대로 전달한다.
            throw e;
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    // ----- 내부 헬퍼 -----

    private void validateApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new CustomException(ErrorCode.AI_API_KEY_NOT_CONFIGURED);
        }
    }

    private String buildUserPrompt(String rawSymptoms, String breed, Integer age, String ageUnit) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[반려견 정보]\n");
        prompt.append("품종: ").append(StringUtils.hasText(breed) ? breed : "정보 없음").append("\n");
        prompt.append("나이: ")
                .append(age != null ? age + (StringUtils.hasText(ageUnit) ? ageUnit : "") : "정보 없음")
                .append("\n\n");

        prompt.append("[증상 및 문진 내용]\n");
        prompt.append(rawSymptoms);

        return prompt.toString();
    }

    /**
     * Claude 호출 지점.
     *
     * <p>TODO: 실제 SDK 호출로 교체한다. 교체 시 형태는 다음과 같다.
     * <pre>
     *   AnthropicClient client = AnthropicOkHttpClient.builder()
     *           .apiKey(apiKey)
     *           .timeout(Duration.ofSeconds(...))   // 클라이언트는 필드로 승격해 재사용
     *           .build();
     *
     *   MessageCreateParams.builder()
     *           .model(model)
     *           .maxTokens(2000L)
     *           .system(SYSTEM_PROMPT)
     *           .addUserMessage(userPrompt)
     *           .thinking(...)                       // 비활성화: 고정 스키마 분류라 불필요 + 비용 1/3
     *           .outputConfig(ClaudeTriageResult.class)   // 구조화 출력: 스키마 자동 도출
     *           .build();
     * </pre>
     *
     * <p>주의 — {@code .outputConfig(Class)}(구조화 출력)와
     * {@code .outputConfig(OutputConfig)}(effort)는 같은 빌더 슬롯이다.
     * 둘을 함께 쓰는 형태는 컴파일로 확정한다. 병행이 불가하면 구조화 출력을 택한다
     * (응답 파싱 안정성이 우선이고, thinking 비활성화만으로 비용의 큰 부분은 잡힌다).
     *
     * <p>{@code claude-sonnet-5}는 {@code temperature}/{@code top_p}/{@code top_k}/
     * {@code budget_tokens}를 포함하면 400을 반환하므로 사용하지 않는다.
     *
     * <p>교체와 함께 아래 catch 절을 추가한다.
     * <ul>
     *   <li>{@code AnthropicIoException}      → {@link ErrorCode#AI_API_TIMEOUT}</li>
     *   <li>{@code AnthropicServiceException} → {@link ErrorCode#AI_ANALYSIS_FAILED}</li>
     *   <li>응답에 text 블록이 없거나 역직렬화 실패 → {@link ErrorCode#AI_RESPONSE_PARSE_ERROR}</li>
     * </ul>
     */
    private ClaudeTriageResult request(String userPrompt) {
        throw new UnsupportedOperationException(
                "Claude 실연동은 아직 구현되지 않았다. 현재는 CLAUDE_API_MOCK=true(기본값)로 "
                        + "StubTriageAnalyzer를 사용한다."
        );
    }
}
