package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Entity.TriageLevel;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Claude API 호출 전담 어댑터.
 *
 * <p>담당 범위는 프롬프트 생성 / 외부 호출 / 결과 반환 / 예외 변환까지다.
 * TriageResult 생성·저장, 세션 조회 등 비즈니스 로직은 {@code TriageService}가 담당한다.
 *
 * <p><b>현재는 실제 API를 호출하지 않고 Mock 결과를 반환한다.</b>
 * 교체 지점은 {@link #request(String)} 하나이며, 나머지 구조는 최종 형태를 유지한다.
 */
@Component
public class ClaudeApiClient {

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

    public ClaudeApiClient(
            @Value("${claude.api.key:}") String apiKey,
            @Value("${claude.api.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 증상 정보를 Claude에 전달해 응급도 분석 결과를 받는다.
     *
     * @param rawSymptoms 초기 증상과 문진 답변을 합친 원본 텍스트
     * @return 응급도 분석 결과
     * @throws CustomException API 키 미설정, 호출 실패, 타임아웃, 응답 파싱 실패
     */
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
     * <p>TODO: 아래 Mock 반환을 실제 SDK 호출로 교체한다. 교체 시 형태는 다음과 같다.
     * <pre>
     *   AnthropicClient client = AnthropicOkHttpClient.builder()
     *           .apiKey(apiKey)
     *           .timeout(Duration.ofSeconds(...))   // 클라이언트는 필드로 승격해 재사용
     *           .build();
     *
     *   StructuredMessageCreateParams&lt;ClaudeTriageResult&gt; params = MessageCreateParams.builder()
     *           .model(model)
     *           .maxTokens(...)
     *           .system(SYSTEM_PROMPT)
     *           .addUserMessage(userPrompt)
     *           .outputConfig(ClaudeTriageResult.class)   // 구조화 출력: 스키마 자동 도출
     *           .build();
     *
     *   client.messages().create(params) 의 text 블록에서 ClaudeTriageResult 를 꺼낸다.
     * </pre>
     *
     * <p>교체와 함께 아래 catch 절을 추가한다.
     * <ul>
     *   <li>{@code AnthropicIoException}      → {@link ErrorCode#AI_API_TIMEOUT}</li>
     *   <li>{@code AnthropicServiceException} → {@link ErrorCode#AI_ANALYSIS_FAILED}</li>
     *   <li>응답에 text 블록이 없거나 역직렬화 실패 → {@link ErrorCode#AI_RESPONSE_PARSE_ERROR}</li>
     * </ul>
     */
    private ClaudeTriageResult request(String userPrompt) {
        return mockResult();
    }

    /**
     * 실제 API 연동 전까지 사용하는 임시 응답.
     * 서비스 흐름(분석 → 저장 → 반환) 검증용이며, 연동 시 제거한다.
     */
    private ClaudeTriageResult mockResult() {
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
