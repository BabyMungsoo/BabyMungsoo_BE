package com.example.babymungsoo.record.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 분석 기록에 대한 병원 방문 팔로우업.
 *
 * <p>분석 기록 하나에 여러 건이 달린다(1:N). 재방문과 경과 관찰이 있어 1:1이 아니다.
 *
 * <p>{@code visitStatus}가 {@code NOT_VISITED}면 {@code notVisitedReason}만 채워지고
 * 진료 관련 필드는 모두 비어 있다. 가지 않았다는 답도 지표라서 같은 테이블에 둔다.
 */
@Entity
@Table(
        name = "hospital_visit",
        indexes = {
                @Index(name = "idx_hospital_visit_record", columnList = "recordId"),
                @Index(name = "idx_hospital_visit_user", columnList = "userId")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HospitalVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long visitId;

    @Column(nullable = false)
    private Long recordId;

    /** 분석 기록의 소유자. 방문 조회·수정 때마다 기록을 다시 읽지 않으려고 함께 둔다. */
    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus visitStatus;

    /** NOT_VISITED 일 때만 채워진다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotVisitedReason notVisitedReason;

    /** VISITED 일 때만 채워진다. 시각은 의미가 없어 날짜만 받는다. */
    private LocalDate visitedAt;

    /** 지도에서 고른 병원. 목록에 없는 병원이면 null이고 hospitalName 만 남는다. */
    private Long hospitalId;

    /** hospitalId 가 있으면 서버가 병원 이름을 채워 둔다. 프론트가 병원을 따로 조회하지 않게. */
    private String hospitalName;

    /**
     * 수의사에게 들은 진단·소견을 보호자가 적은 것.
     *
     * <p>이 값은 AI 프롬프트나 {@code AnalysisRecord.suspectedDisease}로 흘러가지 않는다.
     * 되먹이면 앱이 진단명을 출력하는 경로가 생긴다.
     */
    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    /**
     * 받은 처치. 고정 목록이라 별도 테이블에 둔다 — 나중에 집계를 쿼리로 내기 위해서다.
     * 이 엔티티의 유일한 컬렉션이라 {@code MultipleBagFetchException} 위험이 없다.
     */
    @ElementCollection
    @CollectionTable(name = "hospital_visit_treatments", joinColumns = @JoinColumn(name = "visit_id"))
    @OrderColumn(name = "treatment_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "treatment", nullable = false, length = 30)
    @Builder.Default
    private List<TreatmentTag> treatments = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String memo;

    private LocalDate nextVisitAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** 부분 수정. null 로 들어온 필드는 건드리지 않는다. 방문 여부 자체는 바꾸지 못한다. */
    public void update(LocalDate visitedAt, Long hospitalId, String hospitalName,
                       String diagnosis, List<TreatmentTag> treatments,
                       String memo, LocalDate nextVisitAt) {
        if (visitedAt != null) {
            this.visitedAt = visitedAt;
        }
        if (hospitalId != null || hospitalName != null) {
            this.hospitalId = hospitalId;
            this.hospitalName = hospitalName;
        }
        if (diagnosis != null) {
            this.diagnosis = diagnosis;
        }
        if (treatments != null) {
            this.treatments = new ArrayList<>(treatments);
        }
        if (memo != null) {
            this.memo = memo;
        }
        if (nextVisitAt != null) {
            this.nextVisitAt = nextVisitAt;
        }
    }

    /** hospitalId 로 찾은 병원 이름을 채운다. */
    public void applyHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }
}
