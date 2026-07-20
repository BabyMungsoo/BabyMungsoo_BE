package com.example.babymungsoo.triage.dto.request;

public record TriageSessionCreateRequest(
        Long petId,
        String initialSymptom,
        String symptomCategory
) {
}
