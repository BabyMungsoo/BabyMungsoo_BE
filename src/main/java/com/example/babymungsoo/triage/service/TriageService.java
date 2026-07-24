package com.example.babymungsoo.triage.service;

import com.example.babymungsoo.AI.Client.ClaudeApiClient;
import com.example.babymungsoo.AI.Dto.ClaudeTriageResult;
import com.example.babymungsoo.AI.Entity.TriageResult;
import com.example.babymungsoo.AI.Repository.TriageResultRepository;
import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TriageService {

    private final TriageSessionRepository triageSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TriageResultRepository triageResultRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ClaudeApiClient claudeApiClient;

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
        return TriageSessionResponse.from(saved);
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
        return TriageSessionResponse.from(session);
    }


    public TriageSessionResponse getSession(Long sessionId) {
        return TriageSessionResponse.from(findSession(sessionId));
    }


    @Transactional
    public TriageAnalyzeResponse analyze(TriageAnalyzeRequest request) {
        if (request.sessionId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        TriageSession session = findSession(request.sessionId());

        // 문진이 끝나야 판단 근거가 모두 모이므로, 완료된 세션만 분석한다.
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.TRIAGE_SESSION_NOT_COMPLETED);
        }

        String rawSymptoms = buildRawSymptoms(session);

        ClaudeTriageResult analyzed = claudeApiClient.analyze(
                rawSymptoms,
                request.breed(),
                request.age(),
                request.ageUnit()
        );

        TriageResult triageResult = TriageResult.builder()
                .petId(session.getPetId())
                .breed(request.breed())
                .age(request.age())
                .ageUnit(request.ageUnit())
                .level(analyzed.level())
                .title(analyzed.title())
                .reason(analyzed.reason())
                .guide(analyzed.guide())
                .rawSymptoms(rawSymptoms)
                .build();

        TriageResult saved = triageResultRepository.save(triageResult);
        return TriageAnalyzeResponse.from(saved);
    }

    // ----- 내부 헬퍼 -----

    private TriageSession findSession(Long sessionId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        TriageSession session = triageSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIAGE_SESSION_NOT_FOUND));

        // Prevent IDOR: do not allow access to sessions owned by other users.
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
