package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.TreatmentTag;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 방문 기록 부분 수정. null 로 온 필드는 건드리지 않는다.
 *
 * <p>{@code visitStatus}는 바꾸지 못한다. "다녀왔다"를 "안 갔다"로 뒤집는 건 수정이 아니라
 * 다른 답이므로, 지우고 다시 남기는 편이 기록상 정확하다.
 */
@Getter
public class HospitalVisitUpdateRequestDto {

    private LocalDate visitedAt;
    private Long hospitalId;
    private String hospitalName;
    private String diagnosis;
    private List<TreatmentTag> treatments;
    private String memo;
    private LocalDate nextVisitAt;
}
