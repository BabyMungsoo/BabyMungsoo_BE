package com.example.babymungsoo.record.controller;

import com.example.babymungsoo.record.dto.AnalysisRecordCreateRequestDto;
import com.example.babymungsoo.record.dto.AnalysisRecordResponseDto;
import com.example.babymungsoo.record.dto.AnalysisRecordUpdateRequestDto;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import com.example.babymungsoo.record.service.HospitalVisitService;
import com.example.babymungsoo.record.service.HospitalVisitService.VisitSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class AnalysisRecordController {

    private final AnalysisRecordService analysisRecordService;
    private final HospitalVisitService hospitalVisitService;


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
        List<AnalysisRecord> found = analysisRecordService.getAllRecords(userId);

        // 기록마다 방문을 조회하면 N+1이라 한 번에 세어 붙인다.
        Map<Long, VisitSummary> summaries = hospitalVisitService.summarize(
                found.stream().map(AnalysisRecord::getRecordId).toList());

        List<AnalysisRecordResponseDto> records = found.stream()
                .map(record -> AnalysisRecordResponseDto.from(
                        record,
                        summaries.getOrDefault(record.getRecordId(), VisitSummary.empty())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<AnalysisRecordResponseDto> getRecordById(
            @PathVariable Long recordId) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto.from(
                analysisRecordService.getRecordById(recordId),
                hospitalVisitService.summarize(recordId));
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{recordId}")
    public ResponseEntity<AnalysisRecordResponseDto> updateRecord(
            @PathVariable Long recordId,
            @RequestBody AnalysisRecordUpdateRequestDto requestDto) {
        AnalysisRecordResponseDto response = AnalysisRecordResponseDto.from(
                analysisRecordService.updateRecord(recordId, requestDto),
                hospitalVisitService.summarize(recordId));
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable Long recordId) {
        analysisRecordService.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }
}