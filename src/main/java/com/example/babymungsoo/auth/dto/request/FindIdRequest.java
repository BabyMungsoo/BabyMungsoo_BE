package com.example.babymungsoo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FindIdRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 20) @Pattern(regexp = "[0-9+() -]{7,20}") String phone
) {}
