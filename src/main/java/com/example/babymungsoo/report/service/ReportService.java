package com.example.babymungsoo.report.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.service.HospitalService;
import com.example.babymungsoo.record.service.AnalysisRecordService;
import com.example.babymungsoo.report.entity.Report;
import com.example.babymungsoo.report.repository.ReportRepository;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final AnalysisRecordService analysisRecordService;
    private final HospitalService hospitalService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    // 리포트 생성
    @Transactional
    public Report createReport(Report report) {
        // 내 기록인지까지 함께 확인된다 — getRecordById 가 남의 기록이면 404 를 낸다
        analysisRecordService.getRecordById(report.getRecordId());
        hospitalService.getHospitalById(report.getHospitalId());

        // 주인은 토큰의 사용자로 고정한다. 요청 값을 믿으면 남의 이름으로 만들 수 있다.
        report.assignOwner(currentUserProvider.getCurrentUserId());

        // 동일 recordId에 대한 리포트 중복 저장 방지
        reportRepository.findByRecordId(report.getRecordId())
                .ifPresent(existing -> {
                    throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
                });

        return reportRepository.save(report);
    }

    // 분석 기록 ID로 리포트 조회
    public Report getReportByRecordId(Long recordId) {
        return requireOwned(reportRepository.findByRecordId(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND)));
    }

    // 리포트 상세 조회
    public Report getReportById(Long reportId) {
        return requireOwned(reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND)));
    }

    /**
     * 병원이 받은 리포트 목록.
     *
     * <p>병원 계정 개념이 아직 없어 ADMIN 만 볼 수 있게 막는다. 열어 두면 아무나
     * 한 병원에 보낸 모든 사람의 리포트를 페이지로 긁을 수 있다.
     * 병원 로그인이 생기면 그 병원 소속인지 확인하는 조건으로 바꾼다.
     */
    public Page<Report> getReportsByHospitalId(Long hospitalId, Pageable pageable) {
        requireAdmin();
        return reportRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId, pageable);
    }

    /** 잘못 보낸 리포트를 회수한다. 본인 것만 지울 수 있다. */
    @Transactional
    public void deleteReport(Long reportId) {
        Report report = requireOwned(reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND)));
        reportRepository.delete(report);
    }

    /**
     * 기록이 지워질 때 딸린 리포트도 함께 지운다.
     *
     * <p>남겨 두면 주인을 확인할 기록이 없어져 조회를 막을 수도, 사용자가 지울 수도 없는
     * 행이 내용을 담은 채 남는다.
     */
    @Transactional
    public void deleteByRecordId(Long recordId) {
        reportRepository.deleteByRecordId(recordId);
    }

    /**
     * 내 리포트인지 확인한다. 권한 없음도 404 로 돌려준다 —
     * 403 이면 그 번호의 리포트가 있다는 사실이 새어 나간다. (분석 기록과 같은 규칙)
     */
    private Report requireOwned(Report report) {
        if (!Objects.equals(report.getUserId(), currentUserProvider.getCurrentUserId())) {
            throw new CustomException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }

    private void requireAdmin() {
        User user = userRepository.findById(currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}