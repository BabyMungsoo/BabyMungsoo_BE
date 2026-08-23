package com.example.babymungsoo.triage.dto.request;

import java.util.List;

public record TriageSessionCreateRequest(
        Long petId,
        String initialSymptom,
        String symptomCategory,
        /** 이 문진에 함께 첨부할, 미리 업로드해 둔 사진들의 mediaId. 없으면 null 또는 빈 리스트. */
        List<Long> mediaIds
) {
}
