package com.example.babymungsoo.record.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.hospital.entity.Hospital;
import com.example.babymungsoo.hospital.repository.HospitalRepository;
import com.example.babymungsoo.record.dto.HospitalVisitCreateRequestDto;
import com.example.babymungsoo.record.dto.HospitalVisitResponseDto;
import com.example.babymungsoo.record.entity.AnalysisRecord;
import com.example.babymungsoo.record.entity.HospitalVisit;
import com.example.babymungsoo.record.entity.NotVisitedReason;
import com.example.babymungsoo.record.entity.TreatmentTag;
import com.example.babymungsoo.record.entity.VisitStatus;
import com.example.babymungsoo.record.repository.AnalysisRecordRepository;
import com.example.babymungsoo.record.repository.HospitalVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 팔로우업 답변 저장 규칙. DB 없이 검증만 본다.
 *
 * 가지 않았다는 답도 저장하는 것이 이 기능의 핵심이라, "안 갔어요"에 진료 내용이 섞이면
 * 집계가 흐려진다. 상태별 필수값과 소유자 검증을 여기서 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HospitalVisitServiceTest {

    private static final Long OWNER_ID = 7L;
    private static final Long RECORD_ID = 41L;

    @Mock
    private HospitalVisitRepository hospitalVisitRepository;
    @Mock
    private AnalysisRecordRepository analysisRecordRepository;
    @Mock
    private HospitalRepository hospitalRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private HospitalVisitService service;

    @BeforeEach
    void setUp() {
        service = new HospitalVisitService(
                hospitalVisitRepository, analysisRecordRepository, hospitalRepository, currentUserProvider);

        when(currentUserProvider.getCurrentUserId()).thenReturn(OWNER_ID);
        when(analysisRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(OWNER_ID)));
        when(hospitalVisitRepository.save(any(HospitalVisit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("안 갔어요 — 이유만 있으면 저장된다")
    void savesNotVisited() {
        HospitalVisitCreateRequestDto request = request(VisitStatus.NOT_VISITED);
        set(request, "notVisitedReason", NotVisitedReason.SYMPTOM_IMPROVED);

        HospitalVisitResponseDto response = service.create(RECORD_ID, request);

        assertThat(response.getVisitStatus()).isEqualTo(VisitStatus.NOT_VISITED);
        assertThat(response.getNotVisitedReason()).isEqualTo(NotVisitedReason.SYMPTOM_IMPROVED);
        assertThat(response.getVisitedAt()).isNull();
    }

    @Test
    @DisplayName("안 갔어요에 진료 내용이 섞이면 거부한다 — 집계가 흐려진다")
    void rejectsClinicalDataOnNotVisited() {
        HospitalVisitCreateRequestDto request = request(VisitStatus.NOT_VISITED);
        set(request, "notVisitedReason", NotVisitedReason.NO_TIME);
        set(request, "diagnosis", "급성 위장염");

        assertThatThrownBy(() -> service.create(RECORD_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        verify(hospitalVisitRepository, never()).save(any());
    }

    @Test
    @DisplayName("안 갔어요에 이유가 없으면 거부한다")
    void requiresReason() {
        assertThatThrownBy(() -> service.create(RECORD_ID, request(VisitStatus.NOT_VISITED)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("다녀왔어요 — 방문일과 병원 중 하나가 없으면 거부한다")
    void requiresDateAndHospital() {
        HospitalVisitCreateRequestDto noDate = request(VisitStatus.VISITED);
        set(noDate, "hospitalName", "우리동물병원");
        assertThatThrownBy(() -> service.create(RECORD_ID, noDate)).isInstanceOf(CustomException.class);

        HospitalVisitCreateRequestDto noHospital = request(VisitStatus.VISITED);
        set(noHospital, "visitedAt", LocalDate.of(2026, 9, 22));
        assertThatThrownBy(() -> service.create(RECORD_ID, noHospital)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("다녀왔어요 — 진단·처치는 비워도 저장된다. 다녀왔다는 사실 자체가 지표다")
    void allowsEmptyClinicalDetail() {
        HospitalVisitCreateRequestDto request = request(VisitStatus.VISITED);
        set(request, "visitedAt", LocalDate.of(2026, 9, 22));
        set(request, "hospitalName", "우리동물병원");

        HospitalVisitResponseDto response = service.create(RECORD_ID, request);

        assertThat(response.getDiagnosis()).isNull();
        assertThat(response.getTreatments()).isEmpty();
    }

    @Test
    @DisplayName("hospitalId 를 주면 서버가 병원 이름을 채우고, 처치 태그 중복은 걷어 낸다")
    void fillsHospitalNameAndDedupesTreatments() {
        Hospital hospital = Hospital.builder().hospitalName("OO동물의료센터").build();
        when(hospitalRepository.findById(17L)).thenReturn(Optional.of(hospital));

        HospitalVisitCreateRequestDto request = request(VisitStatus.VISITED);
        set(request, "visitedAt", LocalDate.of(2026, 9, 22));
        set(request, "hospitalId", 17L);
        set(request, "treatments", List.of(TreatmentTag.FLUID, TreatmentTag.FLUID, TreatmentTag.PRESCRIPTION));

        HospitalVisitResponseDto response = service.create(RECORD_ID, request);

        assertThat(response.getHospitalName()).isEqualTo("OO동물의료센터");
        assertThat(response.getTreatments()).containsExactly(TreatmentTag.FLUID, TreatmentTag.PRESCRIPTION);
    }

    @Test
    @DisplayName("남의 기록에는 답을 붙일 수 없다 — 존재를 노출하지 않도록 NOT_FOUND")
    void rejectsOtherUsersRecord() {
        when(analysisRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(999L)));

        HospitalVisitCreateRequestDto request = request(VisitStatus.NOT_VISITED);
        set(request, "notVisitedReason", NotVisitedReason.OTHER);

        assertThatThrownBy(() -> service.create(RECORD_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECORD_NOT_FOUND);
    }

    // ----- 헬퍼 -----

    private static AnalysisRecord record(Long userId) {
        AnalysisRecord record = AnalysisRecord.builder()
                .userId(userId)
                .dogId(1L)
                .symptomText("구토")
                .aiResult("요약")
                .emergencyLevel("WATCH")
                .build();
        ReflectionTestUtils.setField(record, "recordId", RECORD_ID);
        return record;
    }

    private static HospitalVisitCreateRequestDto request(VisitStatus status) {
        HospitalVisitCreateRequestDto request = new HospitalVisitCreateRequestDto();
        set(request, "visitStatus", status);
        return request;
    }

    /** 요청 DTO 는 @Getter 만 있어 세터가 없다. 테스트에서만 필드를 직접 넣는다. */
    private static void set(Object target, String field, Object value) {
        try {
            Field declared = target.getClass().getDeclaredField(field);
            declared.setAccessible(true);
            declared.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
