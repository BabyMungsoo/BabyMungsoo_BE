package com.example.babymungsoo.triage.repository;

import com.example.babymungsoo.triage.entity.TriageSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TriageSessionRepository extends JpaRepository<TriageSession, Long> {

    /**
     * 답변과 질문까지 한 번에 로딩해 세션을 조회한다.
     *
     * <p>트랜잭션 밖에서 답변을 순회하는 분석 흐름 전용이다.
     * 다른 조회는 트랜잭션 안에서 지연 로딩으로 충분하므로 {@code findById}를 쓴다.
     *
     * <p>JPQL {@code join fetch} 대신 {@code @EntityGraph}를 쓰는 이유는 LEFT JOIN으로
     * 처리되기 때문이다. {@code join fetch}는 기본이 INNER JOIN이라 답변이 없는 세션과
     * {@code question}이 null인 자유 서술 답변이 결과에서 누락된다.
     */
    @EntityGraph(attributePaths = {"answers", "answers.question"})
    Optional<TriageSession> findWithAnswersById(Long id);
}
