package com.example.babymungsoo.record.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.record.dto.AnalysisRecordUpdateRequestDto;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.repository.AnalysisRecordRepository;
import com.example.babymungsoo.record.repository.HospitalVisitRepository;
import com.example.babymungsoo.report.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 분석 기록은 남의 것을 보거나 고치거나 지울 수 없어야 한다.
 * 증상 원문·사진·AI 판정이 함께 나가므로 번호만 바꿔서 접근되면 개인정보 노출이다.
 */
@ExtendWith(MockitoExtension.class)
class AnalysisRecordServiceOwnershipTest {

    static final long ME = 5L;
    static final long SOMEONE_ELSE = 1L;

    @Mock
    AnalysisRecordRepository analysisRecordRepository;

    @Mock
    HospitalVisitRepository hospitalVisitRepository;

    @Mock
    ReportRepository reportRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    AnalysisRecordService service;

    @Test
    @DisplayName("남의 기록 상세는 404 — 있는지 없는지도 알려주지 않는다")
    void detailOfOthersRecordIsNotFound() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(analysisRecordRepository.findById(9L)).thenReturn(Optional.of(record(9L, SOMEONE_ELSE)));

        assertThatThrownBy(() -> service.getRecordById(9L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECORD_NOT_FOUND);
    }

    @Test
    @DisplayName("내 기록 상세는 그대로 돌려준다")
    void detailOfMyRecordIsReturned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        AnalysisRecord mine = record(3L, ME);
        when(analysisRecordRepository.findById(3L)).thenReturn(Optional.of(mine));

        assertThat(service.getRecordById(3L)).isSameAs(mine);
    }

    @Test
    @DisplayName("남의 기록은 수정되지 않는다")
    void updateOfOthersRecordIsRejected() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        AnalysisRecord theirs = record(9L, SOMEONE_ELSE);
        when(analysisRecordRepository.findById(9L)).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> service.updateRecord(9L, new AnalysisRecordUpdateRequestDto()))
                .isInstanceOf(CustomException.class);
        assertThat(theirs.getSymptomText()).isEqualTo("남의 증상");
    }

    @Test
    @DisplayName("남의 기록은 삭제되지 않고, 거기 달린 방문 답변도 건드리지 않는다")
    void deleteOfOthersRecordIsRejected() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(analysisRecordRepository.findWithLockByRecordId(9L))
                .thenReturn(Optional.of(record(9L, SOMEONE_ELSE)));

        assertThatThrownBy(() -> service.deleteRecord(9L)).isInstanceOf(CustomException.class);

        verify(analysisRecordRepository, never()).delete(any());
        verify(hospitalVisitRepository, never()).deleteByRecordId(anyLong());
        verify(reportRepository, never()).deleteByRecordId(anyLong());
    }

    @Test
    @DisplayName("내 기록을 지우면 팔로우업 답변과 리포트도 함께 지운다")
    void deleteAlsoRemovesVisitsAndReport() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        AnalysisRecord mine = record(3L, ME);
        when(analysisRecordRepository.findWithLockByRecordId(3L)).thenReturn(Optional.of(mine));

        service.deleteRecord(3L);

        verify(hospitalVisitRepository).deleteByRecordId(3L);
        // 리포트를 남기면 주인을 확인할 기록이 없어져, 조회도 삭제도 못 하는 행이 내용을 담은 채 남는다
        verify(reportRepository).deleteByRecordId(3L);
        verify(analysisRecordRepository).delete(mine);
    }

    @Test
    @DisplayName("목록은 토큰의 사용자로만 조회한다 — 조회 대상을 밖에서 못 정한다")
    void listUsesTokenUserOnly() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);

        service.getAllRecords();

        verify(analysisRecordRepository).findByUserIdOrderByCreatedAtDesc(ME);
    }

    @Test
    @DisplayName("저장할 때 주인은 토큰의 사용자로 덮어쓴다 — 남의 이름으로 못 만든다")
    void createOverwritesOwnerWithTokenUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(analysisRecordRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        // 클라이언트가 남의 userId 를 심어 보낸 상황
        service.createRecord(record(null, SOMEONE_ELSE));

        ArgumentCaptor<AnalysisRecord> saved = ArgumentCaptor.forClass(AnalysisRecord.class);
        verify(analysisRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(ME);
    }

    private static AnalysisRecord record(Long recordId, long userId) {
        return AnalysisRecord.builder()
                .recordId(recordId)
                .userId(userId)
                .dogId(1L)
                .symptomText(userId == ME ? "내 증상" : "남의 증상")
                .aiResult("요약")
                .emergencyLevel("WATCH")
                .build();
    }
}
