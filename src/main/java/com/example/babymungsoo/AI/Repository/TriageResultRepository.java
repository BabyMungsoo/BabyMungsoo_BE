package com.example.babymungsoo.AI.Repository;

import com.example.babymungsoo.AI.Entity.TriageResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriageResultRepository extends JpaRepository<TriageResult, Long> {
    List<TriageResult> findAllByOrderByCreatedAtDesc();

    /**
     * 세션에 저장된 분석 결과를 근거(reason)까지 함께 로딩해 조회한다.
     *
     * <p>{@code reason}은 {@code @ElementCollection}(기본 LAZY)이다.
     * {@code TriageAnalyzeResponse.from()}은 컬렉션 참조만 복사할 뿐 순회하지 않으므로,
     * 초기화하지 않으면 미초기화 컬렉션이 그대로 DTO에 담긴다. 트랜잭션이 닫힌 뒤
     * Jackson이 직렬화할 때 지연 로딩에 실패한다(open-in-view=false 환경).
     *
     * <p>신규 저장 경로는 빌더로 넣은 순수 List라 이 문제가 드러나지 않고,
     * DB에서 읽어오는 이 조회에서만 발생한다.
     */
    @EntityGraph(attributePaths = {"reason"})
    Optional<TriageResult> findBySessionId(Long sessionId);
}
