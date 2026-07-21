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
@Table(name = "media_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // 업로드 시점에는 분석 요청 전이라 analysis가 없을 수 있음 (media-first 흐름), 분석 완료 후 연결
    @Column(name = "analysis_id")
    private Long analysisId;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType mediaType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void linkAnalysis(Long analysisId) {
        this.analysisId = analysisId;
    }
}
