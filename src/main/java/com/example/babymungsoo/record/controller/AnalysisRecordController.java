package com.example.babymungsoo.record.controller;

import com.example.babymungsoo.record.dto.AnalysisRecordCreateRequestDto;
import com.example.babymungsoo.record.dto.AnalysisRecordResponseDto;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class AnalysisRecordController {

    private final AnalysisRecordService analysisRecordService;


    @PostMapping
    public ResponseEntity<AnalysisRecordResponseDto> createRecord(
            @RequestBody AnalysisRecordCreateRequestDto requestDto) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto
                .from(analysisRecordService.createRecord(requestDto.toEntity()));
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<AnalysisRecordResponseDto>> getAllRecords(
            @RequestParam Long userId) {
        List<AnalysisRecordResponseDto> records = analysisRecordService
                .getAllRecords(userId)
                .stream()
                .map(AnalysisRecordResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<AnalysisRecordResponseDto> getRecordById(
            @PathVariable Long recordId) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto
                .from(analysisRecordService.getRecordById(recordId));
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long recordId) {
        analysisRecordService.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }
}