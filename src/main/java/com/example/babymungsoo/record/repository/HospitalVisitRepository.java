package com.example.babymungsoo.record.repository;

import com.example.babymungsoo.record.entity.HospitalVisit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HospitalVisitRepository extends JpaRepository<HospitalVisit, Long> {

    /**
     * 한 기록의 방문 답변을 최근 방문일 순으로.
     *
     * <p>가지 않았다는 답({@code NOT_VISITED})은 {@code visitedAt}이 null이다. PostgreSQL은
     * {@code DESC}에서 null을 맨 앞에 놓으므로, 파생 쿼리를 그대로 쓰면 안 간 답이 실제 방문보다
     * 위에 온다. {@code nulls last}를 명시해야 의도대로 뒤로 밀린다.
     *
     * <p>처치 태그를 함께 읽는다. 이 엔티티의 유일한 컬렉션이라 fetch join 이 안전하다.
     */
    @EntityGraph(attributePaths = {"treatments"})
    @Query("""
            select v from HospitalVisit v
            where v.recordId = :recordId
            order by v.visitedAt desc nulls last, v.visitId desc
            """)
    List<HospitalVisit> findByRecordId(@Param("recordId") Long recordId);

    @EntityGraph(attributePaths = {"treatments"})
    Optional<HospitalVisit> findWithTreatmentsByVisitId(Long visitId);

    void deleteByRecordId(Long recordId);

    /**
     * 기록별 답변 수와 실제 방문 수를 한 번에 센다.
     *
     * <p>목록 화면이 기록마다 조회하면 N+1이 된다. 기록 id 를 모아 한 번만 집계한다.
     */
    @Query("""
            select v.recordId as recordId,
                   count(v) as answerCount,
                   sum(case when v.visitStatus = com.example.babymungsoo.record.entity.VisitStatus.VISITED
                            then 1L else 0L end) as visitedCount
            from HospitalVisit v
            where v.recordId in :recordIds
            group by v.recordId
            """)
    List<VisitCount> countByRecordIds(@Param("recordIds") List<Long> recordIds);

    /** {@link #countByRecordIds(List)} 의 결과 한 줄. */
    interface VisitCount {
        Long getRecordId();

        Long getAnswerCount();

        Long getVisitedCount();
    }
}
