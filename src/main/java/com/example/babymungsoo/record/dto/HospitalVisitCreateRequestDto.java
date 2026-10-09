package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.NotVisitedReason;
import com.example.babymungsoo.record.entity.TreatmentTag;
import com.example.babymungsoo.record.entity.VisitStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 팔로우업 답변.
 *
 * <p>필수값은 {@code visitStatus} 하나뿐이다. 진단·처치·메모를 강제하면 보호자가 답을
 * 남기지 않게 되고, 다녀왔다는 사실 자체가 이미 지표다. 상태별 필수값 검증은
 * {@code HospitalVisitService}가 한다.
 */
@Getter
public class HospitalVisitCreateRequestDto {

    @NotNull(message = "visitStatus는 필수입니다.")
    private VisitStatus visitStatus;

    /** NOT_VISITED 일 때 필수. */
    private NotVisitedReason notVisitedReason;

    /** VISITED 일 때 필수. */
    private LocalDate visitedAt;

    private Long hospitalId;
    private String hospitalName;
    private String diagnosis;
    private List<TreatmentTag> treatments;
    private String memo;
    private LocalDate nextVisitAt;
}
