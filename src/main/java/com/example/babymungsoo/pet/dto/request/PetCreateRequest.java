package com.example.babymungsoo.pet.dto.request;

import com.example.babymungsoo.pet.entity.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PetCreateRequest(

        @Schema(description = "반려견 이름", example = "몽수")
        @NotBlank(message = "반려견 이름은 필수입니다.")
        @Size(max = 50, message = "반려견 이름은 50자 이하여야 합니다.")
        String name,

        @Schema(description = "품종", example = "말티즈")
        @NotBlank(message = "품종은 필수입니다.")
        @Size(max = 50, message = "품종은 50자 이하여야 합니다.")
        String breed,

        @Schema(description = "나이", example = "5")
        @NotNull(message = "나이는 필수입니다.")
        @PositiveOrZero(message = "나이는 0 이상이어야 합니다.")
        Integer age,

        @Schema(description = "성별", example = "MALE")
        @NotNull(message = "성별은 필수입니다.")
        PetGender gender,

        @Schema(description = "체중(kg)", example = "3.8")
        @Positive(message = "체중은 0보다 커야 합니다.")
        Double weight,

        @Schema(description = "중성화 여부", example = "true")
        @NotNull(message = "중성화 여부는 필수입니다.")
        Boolean isNeutered,

        @Schema(description = "기저질환", example = "슬개골 탈구")
        @Size(max = 255, message = "기저질환은 255자 이하여야 합니다.")
        String underlyingDisease,

        @Schema(
                description = "프로필 이미지 URL",
                example = "https://example.com/pets/mongsu.jpg"
        )
        @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
        String profileImage
) {
}