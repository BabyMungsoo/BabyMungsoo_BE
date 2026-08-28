package com.example.babymungsoo.triage.repository;

import com.example.babymungsoo.triage.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 질문 조회.
 *
 * <p>한 테이블에 마스터 질문(sessionId = null)과 세션별 AI 생성 질문(sessionId != null)이
 * 함께 있으므로, 마스터 조회에는 반드시 {@code SessionIdIsNull} 조건을 건다.
 * 조건이 빠지면 남의 세션 질문이 마스터 목록에 섞여 나간다.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /** 마스터 질문 전체. */
    List<Question> findBySessionIdIsNullOrderByOrderNoAsc();

    /** 증상 분류별 마스터 질문. */
    List<Question> findBySymptomCategoryAndSessionIdIsNullOrderByOrderNoAsc(String symptomCategory);

    /** 한 세션을 위해 생성된 질문. 생성된 순서(orderNo)대로 돌려준다. */
    List<Question> findBySessionIdOrderByOrderNoAsc(Long sessionId);

    /**
     * 마스터 질문 수.
     *
     * <p>{@code count()}를 쓰면 세션 질문이 쌓인 뒤로는 항상 0보다 커져서
     * 마스터 시드가 영영 돌지 않는다. 시드 여부는 마스터만 세어 판단해야 한다.
     */
    long countBySessionIdIsNull();
}
