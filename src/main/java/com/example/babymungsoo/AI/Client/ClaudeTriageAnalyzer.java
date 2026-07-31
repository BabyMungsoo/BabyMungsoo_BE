package com.example.babymungsoo.AI.Client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Claude API 호출 전담 어댑터.
 *
 * <p>{@code claude.api.mock=false}일 때 등록된다. 기본값은 {@code true}(Mock)이므로
 * 이 구현체를 쓰려면 {@code CLAUDE_API_MOCK=false}를 명시해야 한다.
 *
 * <p>담당 범위는 프롬프트 생성 / 외부 호출 / 결과 반환 / 예외 변환까지다.
 * TriageResult 생성·저장, 세션 조회 등 비즈니스 로직은 {@code TriageService}가 담당한다.
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

    // 응답은 제목 + 근거 2~4문장 + 안내 문단으로 길이가 제한적이다. 초과 과금을 막는 상한.
    private static final long MAX_TOKENS = 2000L;

    // SDK 기본 타임아웃은 10분이라 응급 상황에서 보호자를 지나치게 기다리게 한다.
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final String apiKey;
    private final String model;

    /**
     * API 키가 없으면 null이다.
     *
     * <p>생성자에서 무조건 빌드하면 키 없는 환경에서 빈 생성이 실패해 기동 자체가 막힌다.
     * 키 미설정은 {@link #validateApiKey()}가 {@code analyze()} 진입 직후 같은 조건으로
     * 먼저 던지므로, 이 필드가 null인 채로 사용되는 경로는 없다.
     */
    private final AnthropicClient client;

    public ClaudeTriageAnalyzer(
            @Value("${claude.api.key:}") String apiKey,
            @Value("${claude.api.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = StringUtils.hasText(apiKey)
                ? AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(TIMEOUT)
                        .build()
                : null;
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
     * <p>구조화 출력을 쓰므로 응답 JSON을 직접 파싱하지 않는다.
     * {@code outputConfig(Class)}가 {@link ClaudeTriageResult}의 Jackson 애노테이션에서
     * JSON 스키마를 도출하고, 응답도 해당 타입으로 역직렬화되어 돌아온다.
     *
     * <p>{@code effort}는 쓰지 않는다. {@code outputConfig(Class)}(스키마 자동 도출)와
     * {@code outputConfig(OutputConfig)}(effort)가 같은 빌더 슬롯이라 병행할 수 없고,
     * 스키마를 손으로 쓰면 {@code ClaudeTriageResult}와 이중 관리가 되기 때문이다.
     * 비용은 thinking 비활성화로 잡는다(호출당 약 1/3).
     *
     * <p>{@code claude-sonnet-5}는 {@code temperature}/{@code top_p}/{@code top_k}/
     * {@code budget_tokens}를 포함하면 400을 반환하므로 사용하지 않는다.
     */
    private ClaudeTriageResult request(String userPrompt) {
        StructuredMessageCreateParams<ClaudeTriageResult> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                // 정해진 스키마에 맞춰 분류하는 작업이라 깊은 추론이 필요 없다.
                .thinking(ThinkingConfigDisabled.builder().build())
                .outputConfig(ClaudeTriageResult.class)
                .build();

        try {
            StructuredMessage<ClaudeTriageResult> message = client.messages().create(params);

            // 안전 필터에 걸리는 등으로 text 블록이 없을 수 있으므로 빈 응답을 별도로 처리한다.
            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .findFirst()
                    .map(StructuredTextBlock::text)
                    .orElseThrow(() -> new CustomException(ErrorCode.AI_RESPONSE_PARSE_ERROR));
        } catch (AnthropicIoException e) {
            throw new CustomException(ErrorCode.AI_API_TIMEOUT);
        } catch (AnthropicInvalidDataException e) {
            throw new CustomException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        } catch (AnthropicServiceException e) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }
}
