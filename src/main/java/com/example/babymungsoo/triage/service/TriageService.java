package com.example.babymungsoo.triage.service;

import com.example.babymungsoo.AI.Client.TriageAnalyzer;
import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Entity.TriageResult;
import com.example.babymungsoo.AI.Dto.PetProfile;
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
import com.example.babymungsoo.triage.dto.response.TriageSessionResponse;
import com.example.babymungsoo.triage.entity.Answer;
import com.example.babymungsoo.triage.entity.Question;
import com.example.babymungsoo.triage.entity.SessionStatus;
import com.example.babymungsoo.triage.entity.TriageSession;
import com.example.babymungsoo.triage.repository.AnswerRepository;
import com.example.babymungsoo.triage.repository.QuestionRepository;
import com.example.babymungsoo.triage.repository.TriageSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TriageService {

    // Pet.age는 단위 없는 정수로 저장되므로 분석 입력에서도 '세'로 해석한다.
    private static final String AGE_UNIT = "세";

    private final TriageSessionRepository triageSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TriageResultService triageResultService;
    private final PetRepository petRepository;
    private final MediaFileRepository mediaFileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final TriageAnalyzer triageAnalyzer;

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


    public QuestionResponse getNextQuestion(Long sessionId) {
        TriageSession session = findSession(sessionId);

        String category = session.getSymptomCategory();
        if (category == null || category.isBlank()) {
            // 세션에 증상 카테고리가 없으면 맞춤 질문을 제공하지 않는다(전체 카테고리 혼합 방지).
            return null;
        }

        Set<Long> answeredQuestionIds = session.getAnswers().stream()
                .map(Answer::getQuestion)
                .filter(Objects::nonNull)
                .map(Question::getId)
                .collect(Collectors.toSet());

        return questionRepository.findBySymptomCategoryOrderByOrderNoAsc(category).stream()
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

            String sessionCategory = session.getSymptomCategory();
            if (sessionCategory != null && !sessionCategory.isBlank()) {
                if (!Objects.equals(sessionCategory, question.getSymptomCategory())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }
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
        return TriageSessionResponse.from(session, mediaFileRepository.findAllBySessionId(sessionId));
    }


    public TriageSessionResponse getSession(Long sessionId) {
        TriageSession session = findSession(sessionId);
        return TriageSessionResponse.from(session, mediaFileRepository.findAllBySessionId(sessionId));
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

        ClaudeTriageResult analyzed = triageAnalyzer.analyze(rawSymptoms, petProfile);

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

    private List<Question> findQuestionsByCategory(String symptomCategory) {
        return (symptomCategory == null || symptomCategory.isBlank())
                ? questionRepository.findAllByOrderByOrderNoAsc()
                : questionRepository.findBySymptomCategoryOrderByOrderNoAsc(symptomCategory);
    }
}
