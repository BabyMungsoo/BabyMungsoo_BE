package com.example.babymungsoo.AI.service;

import com.example.babymungsoo.AI.Entity.TriageResult;
import com.example.babymungsoo.AI.Repository.TriageResultRepository;
import com.example.babymungsoo.triage.dto.response.TriageAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 분석 결과 저장 전담 서비스.
 *
 * <p>외부 AI 호출과 DB 트랜잭션을 분리하기 위해 {@code TriageService}에서 떼어냈다.
 * 별도 빈이어야 {@link Transactional}이 프록시를 타므로 같은 클래스에 두면 안 된다.
 *
 * <p>엔티티 생성은 호출자가 담당한다. 여기서는 저장과 응답 변환만 책임진다.
 */
@Service
@RequiredArgsConstructor
public class TriageResultService {

    private final TriageResultRepository triageResultRepository;

    /**
     * 세션에 이미 저장된 분석 결과를 조회한다.
     *
     * <p>DTO 변환이 트랜잭션 안이어야 한다. 신규 저장과 달리 여기서 꺼낸 엔티티는
     * DB에서 로딩되므로 {@code reason}({@code @ElementCollection}, 기본 LAZY)이
     * 실제 지연 컬렉션이다. 변환을 트랜잭션 밖으로 빼면 지연 로딩에 실패한다.
     */
    @Transactional(readOnly = true)
    public Optional<TriageAnalyzeResponse> findBySessionId(Long sessionId) {
        return triageResultRepository.findTopBySessionIdOrderByIdDesc(sessionId)
                .map(TriageAnalyzeResponse::from);
    }

    /**
     * 분석 결과를 저장하고 응답 DTO로 변환한다.
     *
     * <p>DTO 변환을 트랜잭션 안에서 수행해야 한다.
     * {@code TriageResult.reason}이 {@code @ElementCollection}(기본 LAZY)이라
     * 변환이 트랜잭션 밖으로 나가면 지연 로딩에 실패할 수 있다.
     */
    @Transactional
    public TriageAnalyzeResponse save(TriageResult triageResult) {
        TriageResult saved = triageResultRepository.save(triageResult);
        return TriageAnalyzeResponse.from(saved);
    }
}
