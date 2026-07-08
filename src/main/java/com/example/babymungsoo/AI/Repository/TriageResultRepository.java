package com.example.babymungsoo.AI.Repository;

import com.example.babymungsoo.AI.Entity.TriageResult;  // ← 여기!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageResultRepository extends JpaRepository<TriageResult, Long> {
    List<TriageResult> findAllByOrderByCreatedAtDesc();
}
