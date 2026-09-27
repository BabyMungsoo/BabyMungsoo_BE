package com.example.babymungsoo.record.repository;

import com.example.babymungsoo.record.entity.AnalysisRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRecordRepository extends JpaRepository<AnalysisRecord, Long> {

    List<AnalysisRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AnalysisRecord> findByUserIdAndDogIdOrderByCreatedAtDesc(Long userId, Long dogId);

    /**
     * 기록을 잠그고 읽는다.
     *
     * <p>방문 답변을 저장하는 것과 기록을 지우는 것이 엇갈리면, 저장 쪽이 기록을 읽은 뒤
     * 삭제가 먼저 끝나고 그 다음에 답변이 들어가 주인 없는 행이 남는다.
     * {@code HospitalVisit.recordId}는 연관이 아니라 단순 컬럼이라 DB가 막아 주지 않는다.
     * 두 경로가 같은 행을 잠그게 해서 순서를 강제한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AnalysisRecord> findWithLockByRecordId(Long recordId);
}