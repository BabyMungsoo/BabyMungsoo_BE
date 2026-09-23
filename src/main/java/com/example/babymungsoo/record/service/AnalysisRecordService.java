package com.example.babymungsoo.record.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisRecordService {

    private final AnalysisRecordRepository analysisRecordRepository;
    private final HospitalVisitRepository hospitalVisitRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 기록을 저장한다. 주인은 토큰의 사용자로 고정한다 —
     * 호출부가 보낸 userId 를 믿으면 남의 이름으로 기록을 만들 수 있다.
     */
    @Transactional
    public AnalysisRecord createRecord(AnalysisRecord record) {
        record.assignOwner(currentUserProvider.getCurrentUserId());
        return analysisRecordRepository.save(record);
    }

    /** 로그인한 사용자의 기록만. 조회 대상 사용자를 밖에서 받지 않는다. */
    public List<AnalysisRecord> getAllRecords() {
        return analysisRecordRepository.findByUserIdOrderByCreatedAtDesc(
                currentUserProvider.getCurrentUserId());
    }

    public AnalysisRecord getRecordById(Long recordId) {
        return requireOwned(analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND)));
    }

    /** 부분 수정. 더티 체킹으로 반영되므로 별도 save 는 필요 없습니다. */
    @Transactional
    public AnalysisRecord updateRecord(Long recordId, AnalysisRecordUpdateRequestDto requestDto) {
        AnalysisRecord record = requireOwned(analysisRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND)));

        record.update(
                requestDto.getSymptomText(),
                requestDto.getEmergencyLevel(),
                requestDto.getSuspectedDisease()
        );
        return record;
    }

    @Transactional
    public void deleteRecord(Long recordId) {
        // 방문 답변 저장과 같은 행을 잠근다. 잠그지 않으면 저장 쪽이 기록을 읽은 뒤 삭제가 끝나고
        // 그 다음에 답변이 들어가, 아래 정리가 이미 지나간 주인 없는 행이 남는다.
        AnalysisRecord record = requireOwned(analysisRecordRepository.findWithLockByRecordId(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND)));

        // 기록이 사라지면 거기 달린 팔로우업 답변도 함께 지운다.
        hospitalVisitRepository.deleteByRecordId(recordId);
        analysisRecordRepository.delete(record);
    }

    /**
     * 로그인한 사용자의 기록인지 확인한다.
     *
     * <p>권한 없음도 404 로 돌려준다 — 403 이면 "그 번호의 기록은 있다"가 새어 나가,
     * 번호를 훑어 남의 기록 존재 여부를 알아낼 수 있다.
     * ({@code HospitalVisitService.findOwnedRecord} 와 같은 규칙)
     */
    private AnalysisRecord requireOwned(AnalysisRecord record) {
        if (!Objects.equals(record.getUserId(), currentUserProvider.getCurrentUserId())) {
            throw new CustomException(ErrorCode.RECORD_NOT_FOUND);
        }
        return record;
    }
}