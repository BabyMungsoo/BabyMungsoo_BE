package com.example.babymungsoo.record.controller;

import com.example.babymungsoo.record.dto.AnalysisRecordCreateRequestDto;
import com.example.babymungsoo.record.dto.AnalysisRecordResponseDto;
import com.example.babymungsoo.record.dto.AnalysisRecordUpdateRequestDto;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class AnalysisRecordController {

    private final AnalysisRecordService analysisRecordService;


    @PostMapping
    public ResponseEntity<AnalysisRecordResponseDto> createRecord(
            @Valid @RequestBody AnalysisRecordCreateRequestDto requestDto) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto
                .from(analysisRecordService.createRecord(requestDto.toEntity()));

        URI location = URI.create("/api/v1/records/" + response.getRecordId());
        return ResponseEntity.created(location).body(response);
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


    @PatchMapping("/{recordId}")
    public ResponseEntity<AnalysisRecordResponseDto> updateRecord(
            @PathVariable Long recordId,
            @RequestBody AnalysisRecordUpdateRequestDto requestDto) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto
                .from(analysisRecordService.updateRecord(recordId, requestDto));
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long recordId) {
        analysisRecordService.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }
}