package com.example.babymungsoo.triage.controller;

import com.example.babymungsoo.triage.dto.request.AnswerCreateRequest;
import com.example.babymungsoo.triage.dto.request.TriageSessionCreateRequest;
import com.example.babymungsoo.triage.dto.response.AnswerResponse;
import com.example.babymungsoo.triage.dto.response.QuestionResponse;
import com.example.babymungsoo.triage.dto.response.TriageSessionResponse;
import com.example.babymungsoo.triage.service.TriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/sessions/{sessionId}/complete")
    public TriageSessionResponse completeSession(@PathVariable Long sessionId) {
        return triageService.completeSession(sessionId);
    }

    @GetMapping("/sessions/{sessionId}")
    public TriageSessionResponse getSession(@PathVariable Long sessionId) {
        return triageService.getSession(sessionId);
    }
}
