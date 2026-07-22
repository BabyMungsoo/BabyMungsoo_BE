package com.example.babymungsoo.triage.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.triage.dto.request.AnswerCreateRequest;
import com.example.babymungsoo.triage.dto.request.TriageSessionCreateRequest;
import com.example.babymungsoo.triage.dto.response.AnswerResponse;
import com.example.babymungsoo.triage.dto.response.QuestionResponse;
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
    private final CurrentUserProvider currentUserProvider;

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

    private List<Question> findQuestionsByCategory(String symptomCategory) {
        return (symptomCategory == null || symptomCategory.isBlank())
                ? questionRepository.findAllByOrderByOrderNoAsc()
                : questionRepository.findBySymptomCategoryOrderByOrderNoAsc(symptomCategory);
    }
}
