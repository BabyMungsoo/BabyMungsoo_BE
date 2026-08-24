package com.example.babymungsoo.AI.Entity;

import com.example.babymungsoo.pet.entity.PetGender;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(columnDefinition = "TEXT")
    private String title;

    @ElementCollection
    @CollectionTable(name = "triage_reasons", joinColumns = @JoinColumn(name = "triage_id"))
    @Column(name = "reason", columnDefinition = "TEXT")
    private List<String> reason;

    @Column(columnDefinition = "TEXT")
    private String guide;

    @Column(columnDefinition = "TEXT")
    private String rawSymptoms;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}