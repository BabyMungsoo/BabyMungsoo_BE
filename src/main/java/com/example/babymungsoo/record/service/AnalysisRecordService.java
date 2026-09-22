package com.example.babymungsoo.record.service;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.record.dto.AnalysisRecordUpdateRequestDto;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.repository.AnalysisRecordRepository;
import com.example.babymungsoo.record.repository.HospitalVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisRecordService {

    private final AnalysisRecordRepository analysisRecordRepository;
    private final HospitalVisitRepository hospitalVisitRepository;

    @Transactional
    public AnalysisRecord createRecord(AnalysisRecord record) {
        return analysisRecordRepository.save(record);
    }

    public List<AnalysisRecord> getAllRecords(Long userId) {
        return analysisRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public AnalysisRecord getRecordById(Long recordId) {
        return analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));
    }

    /** 부분 수정. 더티 체킹으로 반영되므로 별도 save 는 필요 없습니다. */
    @Transactional
    public AnalysisRecord updateRecord(Long recordId, AnalysisRecordUpdateRequestDto requestDto) {
        AnalysisRecord record = analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        record.update(
                requestDto.getSymptomText(),
                requestDto.getEmergencyLevel(),
                requestDto.getSuspectedDisease()
        );
        return record;
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        AnalysisRecord record = analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        // 기록이 사라지면 거기 달린 팔로우업 답변도 함께 지운다. 남으면 주인 없는 행이 된다.
        hospitalVisitRepository.deleteByRecordId(recordId);
        analysisRecordRepository.delete(record);
    }
}