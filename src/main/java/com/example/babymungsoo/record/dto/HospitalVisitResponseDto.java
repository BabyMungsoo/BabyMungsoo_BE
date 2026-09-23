package com.example.babymungsoo.record.dto;

import com.example.babymungsoo.record.entity.HospitalVisit;
import com.example.babymungsoo.record.entity.NotVisitedReason;
import com.example.babymungsoo.record.entity.TreatmentTag;
import com.example.babymungsoo.record.entity.VisitStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class HospitalVisitResponseDto {

    private Long visitId;
    private Long recordId;
    private VisitStatus visitStatus;
    private NotVisitedReason notVisitedReason;
    private LocalDate visitedAt;
    private Long hospitalId;
    private String hospitalName;
    private String diagnosis;
    private List<TreatmentTag> treatments;
    private String memo;
    private LocalDate nextVisitAt;
    private LocalDateTime createdAt;

    public static HospitalVisitResponseDto from(HospitalVisit visit) {
        return HospitalVisitResponseDto.builder()
                .visitId(visit.getVisitId())
                .recordId(visit.getRecordId())
                .visitStatus(visit.getVisitStatus())
                .notVisitedReason(visit.getNotVisitedReason())
                .visitedAt(visit.getVisitedAt())
                .hospitalId(visit.getHospitalId())
                .hospitalName(visit.getHospitalName())
                .diagnosis(visit.getDiagnosis())
                .treatments(visit.getTreatments() == null ? List.of() : List.copyOf(visit.getTreatments()))
                .memo(visit.getMemo())
                .nextVisitAt(visit.getNextVisitAt())
                .createdAt(visit.getCreatedAt())
                .build();
    }
}
