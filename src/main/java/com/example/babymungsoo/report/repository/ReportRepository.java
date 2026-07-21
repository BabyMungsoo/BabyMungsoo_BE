package com.example.babymungsoo.report.repository;

import com.example.babymungsoo.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 분석 기록 ID로 리포트 조회
    Optional<Report> findByRecordId(Long recordId);

    // 병원 ID로 리포트 목록 조회
    List<Report> findByHospitalIdOrderByCreatedAtDesc(Long hospitalId);
}