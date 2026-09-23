package com.example.babymungsoo.record.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.example.babymungsoo.record.dto.HospitalVisitCreateRequestDto;
import com.example.babymungsoo.record.dto.HospitalVisitResponseDto;
import com.example.babymungsoo.record.dto.HospitalVisitUpdateRequestDto;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.entity.HospitalVisit;
import com.example.babymungsoo.record.entity.TreatmentTag;
import com.example.babymungsoo.record.entity.VisitStatus;
import com.example.babymungsoo.record.repository.AnalysisRecordRepository;
import com.example.babymungsoo.record.repository.HospitalVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 분석 기록에 달리는 병원 방문 팔로우업.
 *
 * <p>가지 않았다는 답도 저장한다. 그래야 "IMMEDIATE 판정 중 실제 내원 비율" 같은 지표가 나온다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalVisitService {

    private final HospitalVisitRepository hospitalVisitRepository;
    private final AnalysisRecordRepository analysisRecordRepository;
    private final HospitalRepository hospitalRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public HospitalVisitResponseDto create(Long recordId, HospitalVisitCreateRequestDto request) {
        // 저장 도중 기록이 지워져 주인 없는 답변이 남지 않도록 기록을 잠그고 읽는다.
        AnalysisRecord record = findOwnedRecord(recordId, true);
        validate(request);

        HospitalVisit visit = HospitalVisit.builder()
                .recordId(record.getRecordId())
                .userId(record.getUserId())
                .visitStatus(request.getVisitStatus())
                .notVisitedReason(request.getNotVisitedReason())
                .visitedAt(request.getVisitedAt())
                .hospitalId(request.getHospitalId())
                .hospitalName(request.getHospitalName())
                .diagnosis(request.getDiagnosis())
                // 저장 시에는 null 대신 빈 목록이어야 한다. null 이면 응답 변환에서 터진다.
                .treatments(cleanTreatments(request.getTreatments(), List.of()))
                .memo(request.getMemo())
                .nextVisitAt(request.getNextVisitAt())
                .build();

        fillHospitalName(visit, request.getHospitalId());

        return HospitalVisitResponseDto.from(hospitalVisitRepository.save(visit));
    }

    public List<HospitalVisitResponseDto> findByRecord(Long recordId) {
        findOwnedRecord(recordId, false);

        return hospitalVisitRepository.findByRecordId(recordId).stream()
                .map(HospitalVisitResponseDto::from)
                .toList();
    }

    /** 더티 체킹으로 반영되므로 별도 save 는 필요 없다. */
    @Transactional
    public HospitalVisitResponseDto update(Long visitId, HospitalVisitUpdateRequestDto request) {
        HospitalVisit visit = findOwnedVisit(visitId);

        if (visit.getVisitStatus() == VisitStatus.NOT_VISITED) {
            // 가지 않았다는 답에는 진료 내용을 붙일 수 없다. 다녀왔다면 지우고 다시 남긴다.
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        validateHospitalPatch(request);

        visit.update(
                request.getVisitedAt(),
                request.getHospitalId(),
                request.getHospitalName(),
                request.getDiagnosis(),
                // 수정에서 null 은 "건드리지 않음"이라 그대로 넘긴다.
                cleanTreatments(request.getTreatments(), null),
                request.getMemo(),
                request.getNextVisitAt()
        );
        fillHospitalName(visit, request.getHospitalId());

        return HospitalVisitResponseDto.from(visit);
    }

    @Transactional
    public void delete(Long visitId) {
        hospitalVisitRepository.delete(findOwnedVisit(visitId));
    }

    /** 분석 기록이 지워지면 거기 달린 답변도 함께 지운다. */
    @Transactional
    public void deleteByRecordId(Long recordId) {
        hospitalVisitRepository.deleteByRecordId(recordId);
    }

    /**
     * 기록별 답변 수·방문 수. 목록 화면이 기록마다 조회하면 N+1이라 한 번에 센다.
     *
     * <p>답변이 없는 기록은 결과에 없다. 호출자가 {@link VisitSummary#empty()}로 채운다.
     */
    public Map<Long, VisitSummary> summarize(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, VisitSummary> summaries = new HashMap<>();
        for (HospitalVisitRepository.VisitCount count : hospitalVisitRepository.countByRecordIds(recordIds)) {
            summaries.put(count.getRecordId(), new VisitSummary(
                    count.getAnswerCount() != null && count.getAnswerCount() > 0,
                    count.getVisitedCount() == null ? 0 : count.getVisitedCount().intValue()
            ));
        }
        return summaries;
    }

    public VisitSummary summarize(Long recordId) {
        return summarize(List.of(recordId)).getOrDefault(recordId, VisitSummary.empty());
    }

    /**
     * 팔로우업 답변 현황.
     *
     * @param answered   가지 않았다는 답도 답이다. 프론트가 질문 카드를 숨기는 기준이다.
     * @param visitCount 실제로 다녀온 횟수. 목록의 "진료 완료" 표시에 쓴다.
     */
    public record VisitSummary(boolean answered, int visitCount) {
        public static VisitSummary empty() {
            return new VisitSummary(false, 0);
        }
    }

    // ----- 내부 헬퍼 -----

    /**
     * 병원을 지우는 수정을 막는다.
     *
     * <p>{@code hospitalName}에 빈 문자열을 보내면 "값을 줬다"로 읽혀 기존 {@code hospitalId}가
     * 지워진다. 그대로 두면 다녀왔다는 기록에 병원이 하나도 없는 상태가 남는데, 생성 때는
     * 금지한 상태다. 병원을 건드리는 수정이면 결과가 비지 않는지 미리 본다.
     */
    private void validateHospitalPatch(HospitalVisitUpdateRequestDto request) {
        boolean touchesHospital = request.getHospitalId() != null || request.getHospitalName() != null;
        if (!touchesHospital) {
            return;
        }
        if (request.getHospitalId() == null && !StringUtils.hasText(request.getHospitalName())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 상태별 필수값을 검증한다.
     *
     * <p>가지 않았다는 답에 진료 내용이 섞이면 집계가 흐려지므로 막는다.
     */
    private void validate(HospitalVisitCreateRequestDto request) {
        if (request.getVisitStatus() == VisitStatus.VISITED) {
            if (request.getVisitedAt() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (request.getHospitalId() == null && !StringUtils.hasText(request.getHospitalName())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (request.getNotVisitedReason() != null) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return;
        }

        if (request.getNotVisitedReason() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean hasClinicalData = request.getVisitedAt() != null
                || request.getHospitalId() != null
                || StringUtils.hasText(request.getHospitalName())
                || StringUtils.hasText(request.getDiagnosis())
                || (request.getTreatments() != null && !request.getTreatments().isEmpty())
                || StringUtils.hasText(request.getMemo())
                || request.getNextVisitAt() != null;
        if (hasClinicalData) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 중복과 null 을 걷어 낸다. 사람이 고르는 값이라 개수 상한은 두지 않는다.
     *
     * @param whenNull 입력이 null 일 때 돌려줄 값. 저장은 빈 목록, 부분 수정은 null(건드리지 않음)
     */
    private List<TreatmentTag> cleanTreatments(List<TreatmentTag> treatments, List<TreatmentTag> whenNull) {
        if (treatments == null) {
            return whenNull;
        }
        List<TreatmentTag> distinct = new ArrayList<>();
        for (TreatmentTag tag : treatments) {
            if (tag != null && !distinct.contains(tag)) {
                distinct.add(tag);
            }
        }
        return distinct;
    }

    /** 지도에서 고른 병원이면 이름을 채워 둔다. 프론트가 병원을 따로 조회하지 않게. */
    private void fillHospitalName(HospitalVisit visit, Long hospitalId) {
        if (hospitalId == null) {
            return;
        }
        String name = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOSPITAL_NOT_FOUND))
                .getHospitalName();
        visit.applyHospitalName(name);
    }

    /**
     * @param lock 쓰기 경로에서만 true. 조회까지 잠그면 읽기가 삭제를 기다리게 된다.
     */
    private AnalysisRecord findOwnedRecord(Long recordId, boolean lock) {
        AnalysisRecord record = (lock
                ? analysisRecordRepository.findWithLockByRecordId(recordId)
                : analysisRecordRepository.findById(recordId))
                .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        // 남의 기록에 답을 붙이지 못하게 막는다. 존재 여부를 노출하지 않도록 권한 없음도 NOT_FOUND 다.
        if (!Objects.equals(record.getUserId(), currentUserProvider.getCurrentUserId())) {
            throw new CustomException(ErrorCode.RECORD_NOT_FOUND);
        }
        return record;
    }

    private HospitalVisit findOwnedVisit(Long visitId) {
        HospitalVisit visit = hospitalVisitRepository.findWithTreatmentsByVisitId(visitId)
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        if (!Objects.equals(visit.getUserId(), currentUserProvider.getCurrentUserId())) {
            throw new CustomException(ErrorCode.VISIT_NOT_FOUND);
        }
        return visit;
    }
}
