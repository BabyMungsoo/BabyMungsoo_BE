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

    private Long petId;       // ← 이게 있어야 해요!
    private String breed;
    private Integer age;
    private String ageUnit;

    @Enumerated(EnumType.STRING)
    private TriageLevel level;

    private String title;

    @ElementCollection
    @CollectionTable(name = "triage_reasons", joinColumns = @JoinColumn(name = "triage_id"))
    @Column(name = "reason")
    private List<String> reason;

    private String guide;

    @Column(columnDefinition = "TEXT")
    private String rawSymptoms;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}