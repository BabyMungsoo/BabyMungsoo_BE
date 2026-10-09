package com.example.babymungsoo.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "media_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MediaAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long mediaFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaAnalysisStatus status;

    @Column(columnDefinition = "TEXT")
    private String resultText;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void complete(String resultText) {
        this.status = MediaAnalysisStatus.COMPLETED;
        this.resultText = resultText;
    }

    public void fail() {
        this.status = MediaAnalysisStatus.FAILED;
        this.resultText = null;
    }
}
