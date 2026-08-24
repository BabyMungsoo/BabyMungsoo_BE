package com.example.babymungsoo.AI.Client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.pet.entity.PetGender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
            - 근거(reason)는 입력된 증상과 답변에서 직접 확인되는 내용만 사용합니다.
            - 체중과 기저질환은 같은 증상이라도 위험도를 바꿀 수 있으므로 판단에 반영합니다.
            - 반려견 정보에 "미입력"으로 표시된 항목은 해당 사항이 없다는 뜻이 아니라
              정보가 제공되지 않았다는 뜻입니다. 없는 것으로 단정하지 말고,
              그 항목을 근거(reason)에 사용하지 않습니다.

            사진이 함께 제공된 경우
            - 사진은 보조 근거입니다. 사진에서 실제로 확인되는 것만 서술하고,
              보이지 않는 것을 추측하지 않습니다.
            - 사진이 흐리거나 판단에 도움이 되지 않으면 근거로 사용하지 않습니다.
            - 사진을 근거로 쓸 때는 몇 번째 사진에서 무엇이 보였는지 밝힙니다.
            - 사진만으로 병변을 진단하지 않습니다. 아래 금지 사항이 사진에도 그대로 적용됩니다.

            금지 사항
            당신은 수의사가 아니며, 아래는 수의사만 할 수 있는 진료행위입니다.
            어떤 경우에도 다음을 출력하지 마세요.

            - 투약 지시: 약품명을 말하거나 무엇을 먹이라고 안내하지 않습니다.
              (예: "지사제를 먹이세요", "진통제를 주세요", "인공눈물을 넣어주세요")
            - 처치 지시: 보호자가 직접 몸에 무언가를 하도록 안내하지 않습니다.
              (예: "소독하세요", "구토를 유도하세요", "붕대를 감으세요", "혀를 잡아당기세요")
            - 병명 확정: 진단명을 단정하지 않습니다. 가능성을 언급하더라도
              관찰된 증상에 근거한 서술에 그칩니다.
            - 검사 항목 특정: 어떤 검사를 받아야 하는지 지정하지 않습니다.
              (예: "혈액검사를 받으세요", "엑스레이를 찍어보세요")

            대신 허용되는 것
            - 언제 병원에 가야 하는지에 대한 판단과 그 시급성 안내
            - 이동·보온·안정 등 상태를 악화시키지 않기 위한 일반적인 조치
              (예: "물과 사료를 치우고 안정시켜 주세요", "이동 시 몸을 흔들지 않게 해주세요")
            - 병원에 갈 때까지 지켜봐야 할 관찰 항목
            - 수의사에게 전달하면 도움이 되는 정보 안내
              (예: "섭취한 제품의 포장을 챙겨 가시면 도움이 됩니다")
            """;

    // 값이 비어 있을 때 프롬프트에 적는 표기. "없음"과 구분해야 하는 이유는 buildUserPrompt 참고.
    private static final String UNKNOWN = "미입력";

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
                        // timeout은 HTTP 호출 1건당 제한이라 SDK 기본 재시도(2회)와 겹치면
                        // 전체 대기가 TIMEOUT의 3배까지 늘어난다. 또한 클라이언트가 끊겨도
                        // 서버는 응답을 생성해 과금했을 수 있어, 재시도는 같은 응답을 다시 사는 셈이다.
                        .maxRetries(0)
                        .build()
                : null;
    }

    @Override
    public ClaudeTriageResult analyze(String rawSymptoms, PetProfile pet, List<TriageImage> images) {
        validateApiKey();

        List<ContentBlockParam> blocks = buildUserBlocks(rawSymptoms, pet, images);

        try {
            return request(blocks);
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

    /**
     * 사용자 메시지를 구성할 블록 목록을 만든다.
     *
     * <p>이미지를 텍스트보다 <b>앞에</b> 두는 것이 Anthropic 권장 배치다. 또 각 이미지 앞에
     * "사진 N:" 라벨을 붙여야 프롬프트와 응답에서 특정 사진을 지칭할 수 있다.
     *
     * <p>사진이 없으면 텍스트 블록 하나만 남아 기존 호출과 동일해진다.
     */
    private List<ContentBlockParam> buildUserBlocks(String rawSymptoms, PetProfile pet,
                                                    List<TriageImage> images) {
        List<ContentBlockParam> blocks = new ArrayList<>();

        if (images != null) {
            int label = 1;
            for (TriageImage image : images) {
                blocks.add(textBlock("사진 " + label++ + ":"));
                blocks.add(imageBlock(image));
            }
        }

        blocks.add(textBlock(buildUserPrompt(rawSymptoms, pet)));
        return blocks;
    }

    private ContentBlockParam textBlock(String text) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(text).build());
    }

    private ContentBlockParam imageBlock(TriageImage image) {
        return ContentBlockParam.ofImage(
                ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .mediaType(toSourceMediaType(image.contentType()))
                                .data(image.base64Data())
                                .build())
                        .build()
        );
    }

    /**
     * MIME 문자열을 SDK 열거값으로 옮긴다.
     *
     * <p>여기 도달하기 전에 {@code TriageImageLoader}가 지원 포맷만 남기므로
     * default 분기는 방어용이다.
     */
    private Base64ImageSource.MediaType toSourceMediaType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }

    /**
     * 반려견 정보와 증상을 하나의 텍스트 프롬프트로 조립한다.
     *
     * <p>비어 있는 항목을 "없음"이 아니라 {@link #UNKNOWN}으로 적는 것이 중요하다.
     * {@code weight}와 {@code underlyingDisease}는 nullable이라 "질환이 없어서 비었는지"와
     * "입력을 안 해서 비었는지"를 데이터만으로는 구분할 수 없다. 이때 "없음"으로 적으면
     * 모델이 기저질환이 없다고 단정해 응급도를 낮출 수 있다. 시스템 프롬프트에
     * "미입력 항목을 없는 것으로 단정하지 말라"는 규칙을 함께 두어 방향을 맞춘다.
     */
    private String buildUserPrompt(String rawSymptoms, PetProfile pet) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[반려견 정보]\n");
        prompt.append("품종: ").append(textOrUnknown(pet.breed())).append("\n");
        prompt.append("나이: ").append(formatAge(pet)).append("\n");
        prompt.append("성별: ").append(formatGender(pet.gender())).append("\n");
        prompt.append("체중: ").append(formatWeight(pet.weight())).append("\n");
        prompt.append("중성화: ").append(pet.neutered() ? "완료" : "미완료").append("\n");
        prompt.append("기저질환: ").append(textOrUnknown(pet.underlyingDisease())).append("\n\n");

        prompt.append("[증상 및 문진 내용]\n");
        prompt.append(rawSymptoms);

        return prompt.toString();
    }

    private String textOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN;
    }

    private String formatAge(PetProfile pet) {
        if (pet.age() == null) {
            return UNKNOWN;
        }
        return pet.age() + (StringUtils.hasText(pet.ageUnit()) ? pet.ageUnit() : "");
    }

    private String formatGender(PetGender gender) {
        if (gender == null) {
            return UNKNOWN;
        }
        return gender == PetGender.MALE ? "수컷" : "암컷";
    }

    private String formatWeight(Double weight) {
        return weight != null ? weight + "kg" : UNKNOWN;
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
    private ClaudeTriageResult request(List<ContentBlockParam> userBlocks) {
        StructuredMessageCreateParams<ClaudeTriageResult> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessageOfBlockParams(userBlocks)
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
            // AnthropicIoException은 타임아웃뿐 아니라 연결 거부·DNS 실패 등 모든 IO 오류를 감싼다.
            // 전부 타임아웃으로 보고하면 원인과 메시지가 어긋나므로 원인 예외로 구분한다.
            // SocketTimeoutException의 부모인 InterruptedIOException으로 잡아
            // OkHttp의 callTimeout이 던지는 형태까지 함께 처리한다.
            if (e.getCause() instanceof InterruptedIOException) {
                throw new CustomException(ErrorCode.AI_API_TIMEOUT);
            }
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        } catch (AnthropicInvalidDataException e) {
            throw new CustomException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        } catch (AnthropicServiceException e) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }
}
