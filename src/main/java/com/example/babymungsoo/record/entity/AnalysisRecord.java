package com.example.babymungsoo.record.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** 분석에 쓴 사진들 (media.id 목록, 최대 5장). 문진 분석과 사진 업로드가 별개 흐름이라 없을 수 있습니다. */
    @ElementCollection
    @CollectionTable(name = "analysis_record_media", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "media_id")
    @Builder.Default
    private List<Long> mediaIds = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 부분 수정. null 로 들어온 필드는 건드리지 않습니다.
     * 단 suspectedDisease 는 nullable 컬럼이라, null 로 보내면 값을 지우는 것으로 봅니다.
     */
    /**
     * 주인을 토큰의 사용자로 확정한다. 요청 DTO 에는 userId 가 없고 서비스가 여기서 채운다 —
     * 클라이언트가 보낸 값을 그대로 쓰면 남의 이름으로 기록을 만들 수 있다.
     */
    public void assignOwner(Long userId) {
        this.userId = userId;
    }

    public void update(String symptomText, String emergencyLevel, String suspectedDisease) {
        if (symptomText != null && !symptomText.isBlank()) {
            this.symptomText = symptomText;
        }
        if (emergencyLevel != null && !emergencyLevel.isBlank()) {
            this.emergencyLevel = emergencyLevel;
        }
        this.suspectedDisease = suspectedDisease;
    }
}