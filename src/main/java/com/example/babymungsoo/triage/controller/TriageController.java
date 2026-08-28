package com.example.babymungsoo.triage.controller;

import com.example.babymungsoo.triage.dto.request.AnswerCreateRequest;
import com.example.babymungsoo.triage.dto.request.TriageAnalyzeRequest;
import com.example.babymungsoo.triage.dto.request.TriageSessionCreateRequest;
import com.example.babymungsoo.triage.dto.response.AnswerResponse;
import com.example.babymungsoo.triage.dto.response.QuestionResponse;
import com.example.babymungsoo.triage.dto.response.TriageAnalyzeResponse;
import com.example.babymungsoo.triage.dto.response.TriageQuestionSetResponse;
import com.example.babymungsoo.triage.dto.response.TriageSessionResponse;
import com.example.babymungsoo.triage.service.TriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Triage", description = "반려견 문진 및 AI 응급도 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/triage")
public class TriageController {

    private final TriageService triageService;

    @PostMapping("/sessions")
    public TriageSessionResponse createSession(@RequestBody TriageSessionCreateRequest request) {
        return triageService.createSession(request);
    }

    @GetMapping("/questions")
    public List<QuestionResponse> getQuestions(@RequestParam(required = false) String symptomCategory) {
        return triageService.getQuestions(symptomCategory);
    }

    @GetMapping("/sessions/{sessionId}/next-question")
    public QuestionResponse getNextQuestion(@PathVariable Long sessionId) {
        return triageService.getNextQuestion(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public AnswerResponse saveAnswer(@PathVariable Long sessionId,
                                     @RequestBody AnswerCreateRequest request) {
        return triageService.saveAnswer(sessionId, request);
    }

    @Operation(
            summary = "추가 문진 질문 생성",
            description = "세션의 초기 증상과 첨부 사진을 근거로 추가로 물어볼 질문을 최대 5개 만든다. "
                    + "초기 증상만으로 충분하면 질문 없이 needsAdditionalQuestions=false 로 응답한다. "
                    + "생성에 실패해도 오류를 내지 않고 질문 없음으로 응답해, 최종 분석은 계속 진행할 수 있다. "
                    + "같은 세션을 다시 호출하면 이미 만든 질문을 그대로 돌려준다."
    )
    @PostMapping("/sessions/{sessionId}/questions")
    public TriageQuestionSetResponse generateQuestions(@PathVariable Long sessionId) {
        return triageService.generateQuestions(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public TriageSessionResponse completeSession(@PathVariable Long sessionId) {
        return triageService.completeSession(sessionId);
    }

    @GetMapping("/sessions/{sessionId}")
    public TriageSessionResponse getSession(@PathVariable Long sessionId) {
        return triageService.getSession(sessionId);
    }

    @Operation(
            summary = "AI 응급도 분석",
            description = "완료된 문진 세션의 초기 증상과 답변을 근거로 응급도를 분석하고, "
                    + "결과를 저장한 뒤 반환한다. 세션이 COMPLETED 상태가 아니면 분석할 수 없다."
    )
    @PostMapping("/analyze")
    public TriageAnalyzeResponse analyze(@Valid @RequestBody TriageAnalyzeRequest request) {
        return triageService.analyze(request);
    }
}
