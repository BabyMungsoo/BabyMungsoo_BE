package com.example.babymungsoo.record.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "analysis_record",
        indexes = {
                @Index(name = "idx_analysis_record_user_created", columnList = "userId, createdAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recordId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long dogId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String symptomText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiResult;

    @Column(nullable = false)
    private String emergencyLevel;

    private String suspectedDisease;

    @Column(columnDefinition = "TEXT")
    private String aiGuide;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}