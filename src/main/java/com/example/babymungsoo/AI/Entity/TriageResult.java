package com.example.babymungsoo.AI.Entity;

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
    // 세션당 결과 유일성(재분석 정책)은 이슈 #19에서 다룬다.
    private Long sessionId;

    private Long petId;
    private String breed;
    private Integer age;
    private String ageUnit;

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