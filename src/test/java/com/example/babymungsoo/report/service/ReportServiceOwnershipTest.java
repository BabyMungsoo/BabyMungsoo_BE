package com.example.babymungsoo.report.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.service.HospitalService;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import com.example.babymungsoo.report.entity.Report;
import com.example.babymungsoo.report.repository.ReportRepository;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리포트에는 증상·판정·보낸 병원이 들어간다. 남의 것을 읽거나 지울 수 없어야 하고,
 * 병원별 목록은 병원 계정이 생기기 전까지 ADMIN 만 볼 수 있어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceOwnershipTest {

    static final long ME = 5L;
    static final long SOMEONE_ELSE = 1L;

    @Mock ReportRepository reportRepository;
    @Mock AnalysisRecordService analysisRecordService;
    @Mock HospitalService hospitalService;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock UserRepository userRepository;

    @InjectMocks ReportService service;

    @Test
    @DisplayName("남의 리포트 상세는 404 — 있는지 없는지도 알려주지 않는다")
    void detailOfOthersReportIsNotFound() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report(1L, SOMEONE_ELSE)));

        assertThatThrownBy(() -> service.getReportById(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REPORT_NOT_FOUND);
    }

    @Test
    @DisplayName("기록 번호로 찾을 때도 남의 리포트는 404")
    void byRecordIdOfOthersReportIsNotFound() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(reportRepository.findByRecordId(33L)).thenReturn(Optional.of(report(1L, SOMEONE_ELSE)));

        assertThatThrownBy(() -> service.getReportByRecordId(33L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("내 리포트는 그대로 돌려준다")
    void myReportIsReturned() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        Report mine = report(2L, ME);
        when(reportRepository.findById(2L)).thenReturn(Optional.of(mine));

        assertThat(service.getReportById(2L)).isSameAs(mine);
    }

    @Test
    @DisplayName("생성 시 주인은 토큰의 사용자로 채운다")
    void createAssignsOwnerFromToken() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(analysisRecordService.getRecordById(33L)).thenReturn(AnalysisRecord.builder().recordId(33L).build());
        when(hospitalService.getHospitalById(1L)).thenReturn(Hospital.builder().hospitalId(1L).build());
        when(reportRepository.findByRecordId(33L)).thenReturn(Optional.empty());
        when(reportRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.createReport(Report.builder().recordId(33L).hospitalId(1L)
                .reportContent("내용").emergencyLevel("WATCH").build());

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(ME);
    }

    @Test
    @DisplayName("남의 리포트는 지워지지 않는다")
    void deleteOfOthersReportIsRejected() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report(1L, SOMEONE_ELSE)));

        assertThatThrownBy(() -> service.deleteReport(1L)).isInstanceOf(CustomException.class);
        verify(reportRepository, never()).delete(any());
    }

    @Test
    @DisplayName("내 리포트는 회수할 수 있다")
    void myReportCanBeDeleted() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        Report mine = report(2L, ME);
        when(reportRepository.findById(2L)).thenReturn(Optional.of(mine));

        service.deleteReport(2L);

        verify(reportRepository).delete(mine);
    }

    @Test
    @DisplayName("병원별 리포트 목록은 일반 사용자에게 막는다")
    void hospitalListIsAdminOnly() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(userRepository.findById(ME)).thenReturn(Optional.of(user(UserRole.USER)));

        assertThatThrownBy(() -> service.getReportsByHospitalId(1L, PageRequest.of(0, 10)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        verify(reportRepository, never()).findByHospitalIdOrderByCreatedAtDesc(anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN 은 병원별 리포트 목록을 볼 수 있다")
    void hospitalListIsAllowedForAdmin() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(ME);
        when(userRepository.findById(ME)).thenReturn(Optional.of(user(UserRole.ADMIN)));
        when(reportRepository.findByHospitalIdOrderByCreatedAtDesc(anyLong(), any()))
                .thenReturn(Page.empty());

        assertThat(service.getReportsByHospitalId(1L, PageRequest.of(0, 10))).isEmpty();
    }

    private static Report report(Long reportId, long userId) {
        return Report.builder()
                .reportId(reportId)
                .recordId(33L)
                .userId(userId)
                .hospitalId(1L)
                .reportContent("민감한 진료 의뢰 내용")
                .emergencyLevel("WATCH")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static User user(UserRole role) {
        User user = User.builder().email("a@b.c").password("x").name("이름").build();
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }
}
