package com.example.babymungsoo.AI.Entity;

import com.example.babymungsoo.pet.entity.PetGender;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "triage_results")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TriageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 문진 세션이 만든 결과인지 추적하기 위한 연결.
    // 세션당 결과는 하나만 존재한다. 완료된 세션은 답변 추가가 차단되어 분석 입력이
    // 불변이므로, 재분석 요청에는 기존 결과를 그대로 반환한다(멱등).
    // UNIQUE는 동시 요청이 검사를 동시에 통과했을 때의 최후 방어선이다.
    @Column(unique = true)
    private Long sessionId;

    // 아래는 분석 시점의 반려견 정보 스냅샷이다. Pet은 이후 수정될 수 있으므로,
    // 왜 그런 판단이 나왔는지 되짚으려면 당시 값이 결과와 함께 남아 있어야 한다.
    private Long petId;
    private String breed;
    private Integer age;
    private String ageUnit;

    @Enumerated(EnumType.STRING)
    private PetGender gender;

    private Double weight;

    // 원시형 boolean이 아니라 Boolean이다. 이 컬럼이 생기기 전에 저장된 행은 값이 없는데,
    // false로 읽히면 "중성화하지 않았다"는 기록이 되어 사실과 다를 수 있다.
    private Boolean neutered;

    @Column(columnDefinition = "TEXT")
    private String underlyingDisease;

    @Enumerated(EnumType.STRING)
    private TriageLevel level;

    // 결론 한 줄. TriageLevel.headline()에서 코드가 만든다.
    // 이 컬럼이 생긴 뒤 저장된 행에는 모델이 쓴 옛 제목이 남아 있을 수 있다(아래 findings 참고).
    @Column(columnDefinition = "TEXT")
    private String title;

    // ----- 옛 구조 (레거시 행 읽기용) -----
    // findings 이하가 생기기 전에는 근거 문장 목록과 안내 문단이 결과의 전부였다.
    // 분석이 세션당 멱등이라 기존 행은 재분석해도 새 구조로 바뀌지 않으므로,
    // 응답 변환에서 새 컬럼이 비어 있으면 이 두 컬럼으로 대체한다. 신규 저장 경로는 채우지 않는다.

    @ElementCollection
    @CollectionTable(name = "triage_reasons", joinColumns = @JoinColumn(name = "triage_id"))
    @Column(name = "reason", columnDefinition = "TEXT")
    private List<String> reason;

    @Column(columnDefinition = "TEXT")
    private String guide;

    // ----- 새 구조 -----
    // 목록은 @ElementCollection이 아니라 줄바꿈으로 결합한 TEXT다. 조회가 reason을
    // @EntityGraph로 fetch join 하고 있어, List 컬렉션(bag)을 더 얹으면 Hibernate가
    // MultipleBagFetchException으로 조회를 거부한다. 한 번 쓰고 읽기만 하는 데이터라
    // 테이블을 늘릴 이유도 없다. 결합·분리는 아래 joinLines / splitLines로 한다.
    // 전부 nullable이다. 기존 행이 있는 DB에 NOT NULL 컬럼을 붙이면 ddl-auto: update가
    // 조용히 실패해 쿼리만 죽는다(PR #44의 public_id 사고).

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String urgencyReason;

    @Column(columnDefinition = "TEXT")
    private String escalationSigns;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Column(columnDefinition = "TEXT")
    private String rawSymptoms;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ----- 목록 컬럼 결합·분리 -----

    private static final String LINE_SEPARATOR = "\n";

    /** 목록을 한 컬럼에 담는다. 비어 있으면 null이라 "값 없음"과 "빈 목록"이 같은 상태로 남는다. */
    public static String joinLines(List<String> lines) {
        if (lines == null) {
            return null;
        }
        String joined = lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .map(String::strip)
                .collect(Collectors.joining(LINE_SEPARATOR));
        return joined.isEmpty() ? null : joined;
    }

    /** {@link #joinLines(List)}의 역. null이면 빈 목록이다. */
    public static List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(LINE_SEPARATOR))
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}