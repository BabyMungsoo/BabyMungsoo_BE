package com.example.babymungsoo.record.repository;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {

    List<AnalysisRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AnalysisRecord> findByUserIdAndDogIdOrderByCreatedAtDesc(Long userId, Long dogId);
}