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
     *
     * <p>단건 조회(<code>findBySessionId</code>)가 아니라 최신 1건을 집는 이유는
     * 기존 데이터에 같은 세션의 결과가 여러 건 있을 수 있기 때문이다.
     * 멱등 처리 이전에는 분석할 때마다 새 행을 만들었고, {@code sessionId}의 UNIQUE 제약은
     * 이미 존재하는 테이블에는 {@code ddl-auto: update}로 추가되지 않는다.
     * 단건 조회였다면 그런 DB에서 재분석이 조회 단계에서 예외로 죽는다.
     */
    @EntityGraph(attributePaths = {"reason"})
    Optional<TriageResult> findTopBySessionIdOrderByIdDesc(Long sessionId);
}
