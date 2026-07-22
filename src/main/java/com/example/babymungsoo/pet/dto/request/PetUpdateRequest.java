package com.example.babymungsoo.pet.dto.request;

import com.example.babymungsoo.pet.entity.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PetUpdateRequest(

        @Schema(description = "반려견 이름", example = "몽수")
        @Size(min = 1, max = 50, message = "반려견 이름은 1자 이상 50자 이하여야 합니다.")
        String name,

        @Schema(description = "품종", example = "말티즈")
        @Size(min = 1, max = 50, message = "품종은 1자 이상 50자 이하여야 합니다.")
        String breed,

        @Schema(description = "나이", example = "6")
        @PositiveOrZero(message = "나이는 0 이상이어야 합니다.")
        Integer age,

        @Schema(description = "성별", example = "MALE")
        PetGender gender,

        @Schema(description = "체중(kg)", example = "4.1")
        @Positive(message = "체중은 0보다 커야 합니다.")
        Double weight,

        @Schema(description = "중성화 여부", example = "true")
        Boolean isNeutered,

        @Schema(description = "기저질환", example = "슬개골 탈구")
        @Size(max = 255, message = "기저질환은 255자 이하여야 합니다.")
        String underlyingDisease,

        @Schema(
                description = "프로필 이미지 URL",
                example = "https://example.com/pets/mongsu-new.jpg"
        )
        @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다.")
        String profileImage
) {
}