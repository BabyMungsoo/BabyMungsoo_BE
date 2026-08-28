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
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigDisabled;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.AI.Dto.TriageQuestionSet;
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
 * 추가 문진 질문 생성 전담 어댑터.
 *
 * <p>{@code claude.api.mock=false}일 때 등록된다. 호출 구조는 {@link ClaudeTriageAnalyzer}와 같고,
 * 다른 점은 셋이다.
 * <ul>
 *   <li>시스템 프롬프트가 분리돼 있다. 응급도 판정 규칙을 싣지 않아 훨씬 짧다.</li>
 *   <li>모델을 {@code claude.api.question-model}로 따로 지정할 수 있다(기본값은 분석 모델과 동일).</li>
 *   <li>출력이 질문 목록뿐이라 {@code maxTokens}가 작다.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "claude.api.mock", havingValue = "false")
public class ClaudeTriageQuestionGenerator implements TriageQuestionGenerator {

    private static final String SYSTEM_PROMPT = """
            당신은 반려견 응급 문진을 돕는 보조 시스템입니다.
            보호자가 입력한 증상을 보고, 응급도를 판단하려면 무엇을 더 알아야 하는지만 정하세요.

            이 단계에서 하지 않는 것
            - 응급도(IMMEDIATE / WATCH / NORMAL)를 판정하지 않습니다.
            - 병명을 추정하거나 진단하지 않습니다.
            - 보호자에게 조치를 안내하지 않습니다.
            - 질문 외의 설명을 덧붙이지 않습니다.

            질문 만드는 규칙
            - 최대 5개까지만 만듭니다. 5개를 채우려 하지 마세요.
            - 초기 증상만으로 응급도를 판단하기에 충분하면 질문을 만들지 않습니다.
              이때 needsAdditionalQuestions를 false로 두고 questions를 비웁니다.
            - 보호자가 이미 말한 내용을 다시 묻지 않습니다.
              (예: "어젯밤부터 토했어요"라고 했다면 시작 시점을 다시 묻지 않습니다)
            - 응급도 판단을 가르는 정보를 우선합니다.
              발생 시점 / 횟수와 빈도 / 지속 시간 / 악화 여부 / 출혈이나 지혈 여부 /
              의식·호흡·기력 등 전신 상태 / 먹고 마시는 상태 / 통증이나 보행 이상
            - 한 질문에 한 가지만 묻습니다. 두 가지를 "그리고"로 묶지 않습니다.
            - 보호자가 바로 답할 수 있는 쉬운 한국어로 씁니다. 전문 용어를 쓰지 않습니다.
            - 질문에 응급도 결론이나 진단을 담지 않습니다.
              (쓰지 않는 예: "위험한 상태인데 언제부터 그랬나요?")
            - 반려견 정보에 "미입력"으로 표시된 항목은 정보가 없다는 뜻입니다.
              보호자가 이미 답한 것으로 보지 말고, 필요하면 물어봐도 됩니다.

            사진이 함께 제공된 경우
            - 사진에서 이미 확인되는 것은 다시 묻지 않습니다.
            - 사진만으로 판단하기 어려운 부분을 질문으로 채웁니다.
            """;

    // 값이 비어 있을 때 프롬프트에 적는 표기. ClaudeTriageAnalyzer 와 같은 이유로 "없음"과 구분한다.
    private static final String UNKNOWN = "미입력";

    // 출력은 질문 5개가 전부다. 분석(2000)보다 훨씬 작게 잡아 초과 과금을 막는다.
    private static final long MAX_TOKENS = 500L;

    // 질문 생성이 늦어지면 보호자가 첫 화면을 그만큼 늦게 본다. 분석(60초)보다 짧게 끊는다.
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String model;

    /** API 키가 없으면 null이다. 이유는 {@link ClaudeTriageAnalyzer}와 같다. */
    private final AnthropicClient client;

    public ClaudeTriageQuestionGenerator(
            @Value("${claude.api.key:}") String apiKey,
            @Value("${claude.api.question-model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = StringUtils.hasText(apiKey)
                ? AnthropicOkHttpClient.builder()
                        .apiKey(apiKey)
                        .timeout(TIMEOUT)
                        .maxRetries(0)
                        .build()
                : null;
    }

    @Override
    public TriageQuestionSet generate(String initialSymptom, PetProfile pet, List<TriageImage> images) {
        if (!StringUtils.hasText(apiKey)) {
            throw new CustomException(ErrorCode.AI_API_KEY_NOT_CONFIGURED);
        }

        List<ContentBlockParam> blocks = buildUserBlocks(initialSymptom, pet, images);

        try {
            return request(blocks);
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_FAILED);
        }
    }

    // ----- 내부 헬퍼 -----

    /** 이미지를 텍스트 앞에 두고 "사진 N:" 라벨을 붙이는 배치는 분석 호출과 같다. */
    private List<ContentBlockParam> buildUserBlocks(String initialSymptom, PetProfile pet,
                                                    List<TriageImage> images) {
        List<ContentBlockParam> blocks = new ArrayList<>();

        if (images != null) {
            int label = 1;
            for (TriageImage image : images) {
                blocks.add(textBlock("사진 " + label++ + ":"));
                blocks.add(imageBlock(image));
            }
        }

        blocks.add(textBlock(buildUserPrompt(initialSymptom, pet)));
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

    private Base64ImageSource.MediaType toSourceMediaType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }

    /**
     * 이미 아는 정보를 명시해 같은 것을 다시 묻지 않게 한다.
     *
     * <p>중복 질문을 막는 가장 확실한 방법이 "보호자가 이미 말한 내용"을 그대로 보여주는 것이라,
     * 초기 증상을 별도 항목으로 떼어 준다.
     */
    private String buildUserPrompt(String initialSymptom, PetProfile pet) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("[반려견 정보]\n");
        prompt.append("품종: ").append(textOrUnknown(pet.breed())).append("\n");
        prompt.append("나이: ").append(formatAge(pet)).append("\n");
        prompt.append("성별: ").append(formatGender(pet.gender())).append("\n");
        prompt.append("체중: ").append(formatWeight(pet.weight())).append("\n");
        prompt.append("중성화: ").append(pet.neutered() ? "완료" : "미완료").append("\n");
        prompt.append("기저질환: ").append(textOrUnknown(pet.underlyingDisease())).append("\n\n");

        prompt.append("[보호자가 이미 말한 증상]\n");
        prompt.append(textOrUnknown(initialSymptom)).append("\n\n");

        prompt.append("위 내용으로 응급도를 판단하기에 부족한 정보가 있으면 질문을 만들어 주세요.");

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

    /** 호출 지점. 구조화 출력·thinking 비활성화 등 설정은 분석 호출과 같은 이유로 동일하다. */
    private TriageQuestionSet request(List<ContentBlockParam> userBlocks) {
        StructuredMessageCreateParams<TriageQuestionSet> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessageOfBlockParams(userBlocks)
                .thinking(ThinkingConfigDisabled.builder().build())
                .outputConfig(TriageQuestionSet.class)
                .build();

        try {
            StructuredMessage<TriageQuestionSet> message = client.messages().create(params);

            return message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .findFirst()
                    .map(StructuredTextBlock::text)
                    .orElseThrow(() -> new CustomException(ErrorCode.AI_RESPONSE_PARSE_ERROR));
        } catch (AnthropicIoException e) {
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
