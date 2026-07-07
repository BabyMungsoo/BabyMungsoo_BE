package com.example.babymungsoo.record.service;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.repository.AnalysisRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisRecordService {

    private final AnalysisRecordRepository analysisRecordRepository;

    @Transactional
    public AnalysisRecord createRecord(AnalysisRecord record) {
        return analysisRecordRepository.save(record);
    }

    public List<AnalysisRecord> getAllRecords(Long userId) {
        return analysisRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public AnalysisRecord getRecordById(Long recordId) {
        return analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("분석 기록을 찾을 수 없습니다. id: " + recordId));
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        AnalysisRecord record = analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("분석 기록을 찾을 수 없습니다. id: " + recordId));
        analysisRecordRepository.delete(record);
    }
}