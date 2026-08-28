package com.example.babymungsoo.triage.service;

import com.example.babymungsoo.AI.Client.TriageAnalyzer;
import com.example.babymungsoo.AI.Client.TriageQuestionGenerator;
import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Entity.TriageResult;
import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.AI.service.TriageImageLoader;
import com.example.babymungsoo.AI.service.TriageResultService;
import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.media.entity.MediaFile;
import com.example.babymungsoo.media.repository.MediaFileRepository;
import com.example.babymungsoo.pet.entity.Pet;
import com.example.babymungsoo.pet.repository.PetRepository;
import com.example.babymungsoo.triage.dto.request.AnswerCreateRequest;
import com.example.babymungsoo.triage.dto.request.TriageAnalyzeRequest;
import com.example.babymungsoo.triage.dto.request.TriageSessionCreateRequest;
import com.example.babymungsoo.triage.dto.response.AnswerResponse;
import com.example.babymungsoo.triage.dto.response.QuestionResponse;
import com.example.babymungsoo.triage.dto.response.TriageAnalyzeResponse;
import com.example.babymungsoo.triage.dto.response.TriageQuestionSetResponse;
import com.example.babymungsoo.triage.dto.response.TriageSessionResponse;
import com.example.babymungsoo.triage.entity.Answer;
import com.example.babymungsoo.triage.entity.Question;
import com.example.babymungsoo.triage.entity.SessionStatus;
import com.example.babymungsoo.triage.entity.TriageSession;
import com.example.babymungsoo.triage.repository.AnswerRepository;
import com.example.babymungsoo.triage.repository.QuestionRepository;
import com.example.babymungsoo.triage.repository.TriageSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TriageService {

    // Pet.age는 단위 없는 정수로 저장되므로 분석 입력에서도 '세'로 해석한다.
    private static final String AGE_UNIT = "세";

    // 문진 한 건에 첨부할 수 있는 사진 수. 분석 입력에 넣는 장수와 같은 값이어야 한다.
    private static final int MAX_MEDIA_PER_SESSION = 5;

    private final TriageSessionRepository triageSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TriageResultService triageResultService;
    private final PetRepository petRepository;
    private final MediaFileRepository mediaFileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TriageAnalyzer triageAnalyzer;
    private final TriageQuestionGenerator triageQuestionGenerator;
    private final TriageImageLoader triageImageLoader;

    @Transactional
    public TriageSessionResponse createSession(TriageSessionCreateRequest request) {
        if (request.petId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long userId = currentUserProvider.getCurrentUserId();

        TriageSession session = TriageSession.builder()
                .userId(userId)
                .petId(request.petId())
                .initialSymptom(request.initialSymptom())
                .symptomCategory(request.symptomCategory())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        TriageSession saved = triageSessionRepository.save(session);
        List<MediaFile> media = attachMedia(saved.getId(), userId, request.mediaIds());
        return TriageSessionResponse.from(saved, media);
    }


    public List<QuestionResponse> getQuestions(String symptomCategory) {
        return findQuestionsByCategory(symptomCategory).stream()
                .map(QuestionResponse::from)
                .toList();
    }


    /**
     * 아직 답하지 않은 질문 중 첫 번째를 돌려준다. 다 답했거나 생성된 질문이 없으면 null이다.
     *
     * <p>질문은 그 세션을 위해 생성된 것만 본다. 증상 분류로 마스터 질문을 꺼내던 방식은
     * 보호자가 분류를 직접 고르는 흐름이었는데, 지금은 초기 증상을 보고 질문을 만들기
     * 때문에 세션에 붙은 질문이 곧 물어볼 전부다.
     */
    public QuestionResponse getNextQuestion(Long sessionId) {
        TriageSession session = findSession(sessionId);

        List<Question> sessionQuestions = questionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        if (sessionQuestions.isEmpty()) {
            return null;
        }

        Set<Long> answeredQuestionIds = session.getAnswers().stream()
                .map(Answer::getQuestion)
                .filter(Objects::nonNull)
                .map(Question::getId)
                .collect(Collectors.toSet());

        return sessionQuestions.stream()
                .filter(question -> !answeredQuestionIds.contains(question.getId()))
                .findFirst()
                .map(QuestionResponse::from)
                .orElse(null);
    }

    @Transactional
    public AnswerResponse saveAnswer(Long sessionId, AnswerCreateRequest request) {
        TriageSession session = findSession(sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TRIAGE_SESSION_ALREADY_COMPLETED);
        }

        if (request.content() == null || request.content().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Question question = null;
        if (request.questionId() != null) {
            question = questionRepository.findById(request.questionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));

            // 남의 세션 질문에 답하는 것을 막는다. 마스터 질문(sessionId = null)은
            // 특정 세션 소유가 아니므로 기존처럼 그대로 허용한다.
            Long questionSessionId = question.getSessionId();
            if (questionSessionId != null && !Objects.equals(questionSessionId, sessionId)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Answer answer = Answer.builder()
                .question(question)
                .content(request.content())
                .build();

        session.addAnswer(answer);
        answerRepository.save(answer);

        return AnswerResponse.from(answer);
    }


    @Transactional
    public TriageSessionResponse completeSession(Long sessionId) {
        TriageSession session = findSession(sessionId);
        session.complete();
        return TriageSessionResponse.from(session, mediaFileRepository.findAllBySessionIdOrderByIdAsc(sessionId));
    }


    public TriageSessionResponse getSession(Long sessionId) {
        TriageSession session = findSession(sessionId);
        return TriageSessionResponse.from(session, mediaFileRepository.findAllBySessionIdOrderByIdAsc(sessionId));
    }


    /**
     * 초기 증상을 보고 추가로 물어볼 질문을 만들어 세션에 저장한다.
     *
     * <p>외부 AI 호출이 있으므로 {@code analyze()}와 같은 이유로 트랜잭션 밖에서 실행한다.
     * 여기서 쓰는 조회·저장은 모두 리포지토리 단위 트랜잭션으로 처리되고, 지연 로딩이
     * 필요한 연관은 건드리지 않는다.
     *
     * <p><b>질문 생성 실패는 분석을 막지 않는다.</b> 호출 실패·타임아웃·파싱 실패는
     * 로그만 남기고 "질문 없음"으로 응답한다. 추가 문진은 판단을 돕는 보조 수단이지
     * 필수 단계가 아니며, 여기서 막으면 정작 급한 보호자가 응급도 판단조차 못 받는다.
     *
     * <p>같은 세션을 다시 호출하면 이미 만들어 둔 질문을 그대로 돌려준다. 화면 재진입이나
     * 재시도로 질문이 중복 생성되면 보호자가 같은 질문을 두 번 보게 된다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TriageQuestionSetResponse generateQuestions(Long sessionId) {
        TriageSession session = findSession(sessionId);

        List<Question> existing = questionRepository.findBySessionIdOrderByOrderNoAsc(sessionId);
        if (!existing.isEmpty()) {
            return TriageQuestionSetResponse.from(existing);
        }

        String initialSymptom = session.getInitialSymptom();
        if (initialSymptom == null || initialSymptom.isBlank()) {
            // 물어볼 근거가 없으면 질문도 만들 수 없다.
            return TriageQuestionSetResponse.none();
        }

        Pet pet = petRepository.findByIdAndUser_Id(session.getPetId(), session.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.PET_NOT_FOUND));

        List<TriageImage> images = triageImageLoader.load(sessionId);

        List<String> contents;
        try {
            contents = triageQuestionGenerator
                    .generate(initialSymptom, toPetProfile(pet), images)
                    .usableContents();
        } catch (RuntimeException e) {
            log.warn("추가 질문 생성 실패 - 질문 없이 진행합니다. sessionId={}, reason={}",
                    sessionId, e.getMessage());
            return TriageQuestionSetResponse.none();
        }

        if (contents.isEmpty()) {
            return TriageQuestionSetResponse.none();
        }

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            questions.add(Question.forSession(sessionId, i + 1, contents.get(i)));
        }

        return TriageQuestionSetResponse.from(questionRepository.saveAll(questions));
    }

    /**
     * 완료된 문진 세션을 분석하고 결과를 저장한다.
     *
     * <p>외부 AI 호출이 수 초 걸리므로 그동안 DB 커넥션을 점유하지 않도록
     * 트랜잭션 밖에서 실행한다. 클래스 레벨 {@code @Transactional(readOnly = true)}가
     * 모든 public 메서드에 적용되므로, 단순히 애노테이션을 빼는 것으로는 부족하고
     * {@link Propagation#NOT_SUPPORTED}로 명시적으로 중단시켜야 한다.
     *
     * <p>세션 조회와 결과 저장은 각각 짧은 트랜잭션으로 나뉜다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TriageAnalyzeResponse analyze(TriageAnalyzeRequest request) {
        if (request.sessionId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 트랜잭션 밖에서 답변을 순회하므로 연관 데이터를 함께 로딩한다.
        TriageSession session = findSessionWithAnswers(request.sessionId());

        // 문진이 끝나야 판단 근거가 모두 모이므로, 완료된 세션만 분석한다.
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TRIAGE_SESSION_NOT_COMPLETED);
        }

        // 완료된 세션은 답변을 더 받지 않아 분석 입력이 고정된다.
        // 이미 결과가 있으면 같은 답이 나오므로 AI를 다시 호출하지 않는다.
        Optional<TriageAnalyzeResponse> existing =
                triageResultService.findBySessionId(session.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String rawSymptoms = buildRawSymptoms(session);

        // 완료 상태여도 초기 증상과 답변이 모두 비어 있으면 판단 근거가 없다.
        if (rawSymptoms.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 반려견 정보는 클라이언트 입력을 신뢰하지 않고 세션이 가리키는 반려견에서 조회한다.
        Pet pet = petRepository.findByIdAndUser_Id(session.getPetId(), session.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.PET_NOT_FOUND));

        PetProfile petProfile = toPetProfile(pet);

        // 사진은 보조 근거라, 읽지 못한 장이 있어도 로더가 그 장만 빼고 진행한다.
        List<TriageImage> images = triageImageLoader.load(session.getId());

        ClaudeTriageResult analyzed = triageAnalyzer.analyze(rawSymptoms, petProfile, images);

        TriageResult triageResult = TriageResult.builder()
                .sessionId(session.getId())
                .petId(session.getPetId())
                .breed(petProfile.breed())
                .age(petProfile.age())
                .ageUnit(petProfile.ageUnit())
                .gender(petProfile.gender())
                .weight(petProfile.weight())
                .neutered(petProfile.neutered())
                .underlyingDisease(petProfile.underlyingDisease())
                .level(analyzed.level())
                .title(analyzed.title())
                .reason(analyzed.reason())
                .guide(analyzed.guide())
                .rawSymptoms(rawSymptoms)
                .build();

        // 저장과 DTO 변환은 별도 트랜잭션에서 수행한다.
        try {
            return triageResultService.save(triageResult);
        } catch (DataIntegrityViolationException e) {
            // 위 중복 검사를 두 요청이 동시에 통과한 경우다. UNIQUE 제약이 늦게 온 쪽을
            // 막았으므로, 먼저 저장된 결과를 돌려준다.
            // 재조회는 반드시 새 트랜잭션이어야 한다. 제약 위반이 발생한 트랜잭션은
            // rollback-only로 마킹되어 그 안에서는 조회할 수 없다.
            return triageResultService.findBySessionId(session.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.AI_ANALYSIS_FAILED));
        }
    }

    // ----- 내부 헬퍼 -----

    /**
     * 영속 엔티티를 분석 입력용 값 객체로 옮긴다.
     *
     * <p>엔티티를 그대로 분석기에 넘기지 않는 이유는 두 가지다. 분석기가 영속 객체를 쥐면
     * 트랜잭션 밖에서 지연 로딩을 건드릴 위험이 생기고, 분석 시점의 값을 그대로 결과에
     * 스냅샷으로 남겨야 하는데 엔티티는 이후 수정될 수 있다.
     */
    private PetProfile toPetProfile(Pet pet) {
        return new PetProfile(
                pet.getBreed(),
                pet.getAge(),
                AGE_UNIT,
                pet.getGender(),
                pet.getWeight(),
                pet.isNeutered(),
                pet.getUnderlyingDisease()
        );
    }

    /**
     * 미리 업로드해 둔 사진들을 방금 만든 세션에 연결한다.
     *
     * <p>같은 사진이 동시에 다른 세션에도 붙는 걸 막기 위해 잠금 조회하고,
     * 존재하지 않거나 다른 사용자 소유인 mediaId 는 소유 사실을 노출하지 않도록
     * 둘 다 MEDIA_NOT_FOUND 로 처리한다(TriageSession 조회와 동일한 정책).
     */
    private List<MediaFile> attachMedia(Long sessionId, Long userId, List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }

        // 같은 id가 중복으로 와도(예: [4, 4]) 조회 결과는 한 건이라, 중복 제거한 개수와 비교해야
        // 정상 소유의 미디어를 MEDIA_NOT_FOUND로 잘못 거부하지 않는다.
        List<Long> distinctMediaIds = mediaIds.stream().distinct().toList();

        // 상한을 여기서 막지 않으면 6장 이상이 그대로 저장된 뒤 분석 단계에서 조용히 잘려나가,
        // 보호자는 올린 사진이 모두 반영된 줄 알게 된다. 저장 시점에 거부해 그 어긋남을 없앤다.
        if (distinctMediaIds.size() > MAX_MEDIA_PER_SESSION) {
            throw new CustomException(ErrorCode.TOO_MANY_MEDIA);
        }

        List<MediaFile> mediaFiles = mediaFileRepository.findWithLockByIdInAndUserId(distinctMediaIds, userId);
        if (mediaFiles.size() != distinctMediaIds.size()) {
            throw new CustomException(ErrorCode.MEDIA_NOT_FOUND);
        }

        for (MediaFile mediaFile : mediaFiles) {
            if (mediaFile.getSessionId() != null) {
                throw new CustomException(ErrorCode.MEDIA_ALREADY_ATTACHED);
            }
            mediaFile.assignSession(sessionId);
        }

        return mediaFiles;
    }

    private TriageSession findSession(Long sessionId) {
        TriageSession session = triageSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIAGE_SESSION_NOT_FOUND));

        return verifyOwner(session);
    }

    /**
     * 답변과 질문까지 함께 로딩해 세션을 조회한다.
     *
     * <p>{@code analyze()}는 트랜잭션 밖에서 실행되므로 답변을 지연 로딩할 수 없다.
     * 나머지 조회는 트랜잭션 안이라 {@link #findSession(Long)}으로 충분하다.
     */
    private TriageSession findSessionWithAnswers(Long sessionId) {
        TriageSession session = triageSessionRepository.findWithAnswersById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIAGE_SESSION_NOT_FOUND));

        return verifyOwner(session);
    }

    /**
     * 세션 소유자를 검증한다.
     *
     * <p>Prevent IDOR: do not allow access to sessions owned by other users.
     * 존재 여부를 노출하지 않도록 권한 없음도 NOT_FOUND로 응답한다.
     */
    private TriageSession verifyOwner(TriageSession session) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        if (!Objects.equals(session.getUserId(), currentUserId)) {
            throw new CustomException(ErrorCode.TRIAGE_SESSION_NOT_FOUND);
        }

        return session;
    }

    private String buildRawSymptoms(TriageSession session) {
        StringBuilder rawSymptoms = new StringBuilder();

        String initialSymptom = session.getInitialSymptom();
        if (initialSymptom != null && !initialSymptom.isBlank()) {
            rawSymptoms.append("초기 증상: ").append(initialSymptom).append("\n");
        }

        String symptomCategory = session.getSymptomCategory();
        if (symptomCategory != null && !symptomCategory.isBlank()) {
            rawSymptoms.append("증상 분류: ").append(symptomCategory).append("\n");
        }

        for (Answer answer : session.getAnswers()) {
            Question question = answer.getQuestion();
            // 질문 없이 저장된 자유 서술 답변도 분석 근거에서 누락하지 않는다.
            String questionContent = (question != null) ? question.getContent() : "추가 설명";

            rawSymptoms.append("- ")
                    .append(questionContent)
                    .append(": ")
                    .append(answer.getContent())
                    .append("\n");
        }

        return rawSymptoms.toString();
    }

    /**
     * 마스터 질문만 조회한다.
     *
     * <p>세션별 AI 생성 질문이 같은 테이블에 있으므로 조건을 걸지 않으면
     * 남의 세션 질문이 목록에 섞인다.
     */
    private List<Question> findQuestionsByCategory(String symptomCategory) {
        return (symptomCategory == null || symptomCategory.isBlank())
                ? questionRepository.findBySessionIdIsNullOrderByOrderNoAsc()
                : questionRepository.findBySymptomCategoryAndSessionIdIsNullOrderByOrderNoAsc(symptomCategory);
    }
}
