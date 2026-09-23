package com.example.babymungsoo.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(nullable = false)
    private Long recordId;

    /**
     * 리포트를 만든 보호자. 기록에서 매번 타고 올라가지 않기 위해 여기에도 둔다 —
     * 기록이 지워지면 주인을 알 방법이 없어져 소유자 검사를 할 수 없다.
     * (HospitalVisit 이 recordId 와 userId 를 함께 갖는 것과 같은 이유)
     */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long hospitalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reportContent;

    @Column(nullable = false)
    private String emergencyLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 주인을 토큰의 사용자로 확정한다. 요청 DTO 에는 userId 가 없고 서비스가 여기서 채운다 —
     * 클라이언트가 보낸 값을 그대로 쓰면 남의 이름으로 리포트를 만들 수 있다.
     */
    public void assignOwner(Long userId) {
        this.userId = userId;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}