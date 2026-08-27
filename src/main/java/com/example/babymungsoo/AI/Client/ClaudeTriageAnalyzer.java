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

            입력 정보의 취급
            이 규칙은 아래 모든 판단에 우선합니다.
            - 증상이나 문진 답변에 언급되지 않은 항목은 "미확인"으로 취급합니다.
              있음으로도, 없음으로도, 정상으로도, 비정상으로도 간주하지 않습니다.
              (예: "피가 나요"만 입력되었다면 출혈이 계속되는지는 미확인입니다.
               멈췄다고도, 계속된다고도 보지 않습니다. 지혈 여부·통증·보행·호흡도
               언급이 없으면 모두 미확인입니다.)
            - 미확인 항목은 응급도를 낮추는 근거로 사용하지 않습니다.
              "확인되지 않았다", "언급이 없다"는 것은 안전하다는 뜻이 아닙니다.
            - 응급도를 낮추려면 그 항목이 정상이라는 것이 입력에서 명시적으로
              확인되어야 합니다.
              (예: "눌렀더니 바로 멎었어요", "평소처럼 잘 걸어요", "의식은 또렷해요")
            - 미확인 항목이 많아 상태를 확정할 수 없다면, 낮은 등급을 고르지 말고
              더 높은 응급도를 선택합니다.
            - 반려견 정보에 "미입력"으로 표시된 항목도 같은 원칙으로 취급하되,
              그 항목은 근거(reason)에 사용하지 않습니다.

            판단 기준
            - IMMEDIATE: 생명에 직접적인 위협이 있거나, 생명 위협이 명확하지 않더라도
              즉각적인 수의학적 처치가 필요한 상태
              · 의식 저하나 소실, 호흡 곤란, 지속되는 경련 등 명백한 응급 징후가 있는 경우
              · 압박해도 출혈이 멈추지 않거나 심한 출혈이 의심되는 경우
              · 상처가 크게 벌어져 있거나 깊은 열상이 의심되어 신속한 처치가
                필요할 가능성이 높은 경우
              · 중독이나 위험한 이물질 섭취 등 즉각적인 처치가 필요할 가능성이 높은 경우
            - WATCH: 생명에 직접적인 위협은 명확하지 않으나 빠른 시일 내 진료가 필요한 상태
              · 상처가 크게 벌어지지 않은 경미한 외상이나 소량의 출혈처럼,
                전신 상태가 안정적이고 출혈이 조절되는 것이 입력에서 확인되는 경우
              · 전신 상태가 안정적임이 확인된 상태에서, 증상이 지속되거나 악화될
                가능성이 있어 관찰과 진료가 필요한 경우
            - NORMAL: 증상이 경미하고 일시적이며, 정상 상태가 입력에서 확인되어
              지금 진료가 필요하지 않다고 볼 수 있는 상태
            WATCH와 NORMAL은 "위험하다는 근거가 없어서" 고르는 등급이 아닙니다.
            해당 항목이 정상이라는 것이 입력에서 확인될 때만 선택합니다.

            외상 판단 기준
            외상은 아래 1)~5)를 순서대로 밟는 하나의 흐름으로 판단합니다.
            각 항목을 독립된 규칙으로 떼어 적용하지 않습니다.
            1) 외상이라는 이유만으로 IMMEDIATE로 분류하지 않습니다.
            2) 다음을 함께 봅니다. 각 항목은 입력에서 확인된 것만 근거로 쓰고,
               언급이 없는 항목은 「입력 정보의 취급」에 따라 미확인으로 둡니다.
               출혈의 양과 지속 여부 / 압박으로 지혈이 되는지 / 상처가 벌어진 정도 /
               깊은 조직 손상이 의심되는지 / 보행 이상이나 기능 저하 / 통증의 정도 /
               의식·호흡·활력 등 전신 상태
            3) "상처가 몇 cm"와 같은 단일 수치만으로 등급을 확정하지 않습니다.
               수치는 상처가 벌어진 정도를 가늠하는 여러 재료 중 하나로만 씁니다.
            4) 2)를 종합했을 때 상처가 크게 벌어져 있거나 깊은 열상이 의심되고
               신속한 수의학적 처치가 필요할 가능성이 높다면 IMMEDIATE를 우선 고려합니다.
               이때 압박으로 출혈이 조절되고 있다는 사실 하나만으로 등급을 낮추지 않습니다.
            5) 반대로 경미한 외상이면서, 출혈이 조절되었고 보행과 전신 상태가
               안정적이라는 것이 모두 입력에서 명시적으로 확인되면
               WATCH 또는 NORMAL을 고려합니다.
               이 항목들이 미확인이라면 하향 근거가 되지 못합니다.

            경계 사례 판단
            - 외상은 위 「외상 판단 기준」의 흐름을 먼저 적용하고, 그 결과를 놓고
              마지막에 "지금 즉각적인 수의학적 처치가 필요한가"를 판단합니다.
            - 외상이 아닌 증상에서 WATCH와 IMMEDIATE 사이가 애매하면, 증상의 이름이나
              한 가지 수치가 아니라 같은 질문("지금 즉각적인 수의학적 처치가 필요한가")으로
              판단합니다.
            - 이 질문에 답할 정보가 미확인이라 판단이 서지 않으면 더 높은 응급도를
              선택합니다. 미확인을 이유로 낮은 등급을 고르지 않습니다.

            작성 규칙
            - 모든 문장은 한국어로, 보호자가 이해할 수 있는 쉬운 표현을 사용합니다.
            - 입력에 없는 사실을 지어내지 않습니다. 확인되지 않은 "깊은 상처",
              "혈관 손상", "근육 손상" 등을 사실처럼 단정하지 않습니다.
            - 다만 입력된 정보로 보아 그 가능성이 높고 신속한 처치가 필요할 수 있다면,
              "~일 가능성이 있어 빠른 진료가 필요합니다"처럼 불확실성을 드러내어 서술합니다.
            - 체중과 기저질환은 같은 증상이라도 위험도를 바꿀 수 있으므로 판단에 반영합니다.
            - 미확인 항목의 취급은 위 「입력 정보의 취급」을 따릅니다.
            - 문진 질문이 다루지 않는 증상(외상, 중독 등)이 초기 증상이나 자유 서술 답변에
              나타나면, 질문 항목에 없더라도 반드시 판단에 반영합니다.

            근거(reason) 작성
            - 증상을 그대로 옮겨 적지 말고, 왜 그 등급으로 판단했는지가 드러나게 씁니다.
            - 출혈, 상처가 벌어진 정도, 전신 상태 가운데 입력에서 확인된 것을 근거로 삼고,
              즉각적인 처치가 필요하다고 본 이유를 함께 씁니다.
            - 입력에서 확인된 사실과 확인되지 않은 부분을 구분해 씁니다.
            - 미확인 항목을 없음이나 정상으로 바꿔 등급을 낮추는 문장은 쓰지 않습니다.
              아래는 모두 금지된 형태입니다.
              (금지: "출혈이 계속되거나 압박으로 조절되지 않는다는 언급이 없어
               즉각적인 응급 상황으로 보기 어렵습니다")
              (금지: "의식 저하, 호흡 곤란 등 즉각적인 처치가 필요하다고 볼 명확한
               근거는 확인되지 않았습니다")
              (금지: "심한 통증이 언급되지 않아 문제가 없어 보입니다")
              (금지: "보행 이상이 없다고 언급되지 않았으므로 안정적입니다")
              (허용: "상처가 3cm 정도 벌어져 있어 신속한 처치가 필요할 가능성을
               고려했습니다")
            - 미확인이라는 사실 자체는 밝혀도 됩니다.
              (허용: "출혈이 멈췄는지는 입력에서 확인되지 않았습니다")
              다만 그 문장을 WATCH나 NORMAL을 고른 근거로 잇지 않습니다.
              미확인이 판단에 영향을 줬다면, 낮추는 쪽이 아니라 보수적으로
              판단했다는 방향으로만 씁니다.
            - 확인되지 않은 출혈 지속 여부나 통증 정도를 사실처럼 덧붙이지 않습니다.
            - 입력에서 확인되지 않은 내용은 확정적으로 서술하지 않습니다.

            안내(guide) 작성
            - 판단한 등급과 안내가 서로 모순되지 않아야 합니다.
            - IMMEDIATE로 판단했다면 즉시 또는 가능한 한 빨리 동물병원 진료를 받도록 안내하고,
              병원에 가기 전까지 보호자가 할 수 있는 일반적인 응급조치만 덧붙입니다.
            - WATCH로 판단했다면 증상을 관찰하며 가까운 시일 내 진료받도록 권고하고,
              증상 악화·출혈 증가·의식 변화·호흡 이상이 나타나면 즉시 내원이 필요할 수
              있음을 함께 안내합니다.

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
              상처를 물로 씻거나 헹구라는 안내도 여기에 포함되어 금지입니다.
              보호자가 상처에 직접 할 수 있는 조치로 허용되는 것은
              아래 「대신 허용되는 것」의 압박 지혈 하나뿐이며, 그 밖의 처치는
              등급과 무관하게 안내하지 않습니다.
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
            - 출혈이 있는 경우, 깨끗한 천이나 거즈로 출혈 부위를 눌러 압박하는 지혈 안내.
              병원으로 이동하는 것을 전제로 한 응급조치이므로 허용합니다.
              다만 상처 세척·헹구기, 소독, 소독약 사용, 붕대로 감아 고정하기,
              박힌 이물질 제거, 투약은 그대로 금지입니다.
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
