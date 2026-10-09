package com.example.babymungsoo.report.repository;

import com.example.babymungsoo.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 분석 기록 ID로 리포트 조회
    Optional<Report> findByRecordId(Long recordId);

    // 기록이 지워질 때 함께 정리한다. 남겨 두면 주인 없는 리포트가 계속 조회된다.
    void deleteByRecordId(Long recordId);

    // 병원 ID로 리포트 목록 조회 (페이지 단위)
    Page<Report> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);
}