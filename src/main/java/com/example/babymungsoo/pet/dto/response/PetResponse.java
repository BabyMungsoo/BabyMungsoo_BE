package com.example.babymungsoo.pet.dto.response;

import com.example.babymungsoo.pet.entity.Pet;
import com.example.babymungsoo.pet.entity.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record PetResponse(

        @Schema(description = "반려동물 ID", example = "1")
        Long petId,

        @Schema(description = "반려견 이름", example = "몽수")
        String name,

        @Schema(description = "품종", example = "말티즈")
        String breed,

        @Schema(description = "나이", example = "5")
        Integer age,

        @Schema(description = "성별", example = "MALE")
        PetGender gender,

        @Schema(description = "체중(kg)", example = "3.8")
        Double weight,

        @Schema(description = "중성화 여부", example = "true")
        boolean isNeutered,

        @Schema(description = "기저질환", example = "슬개골 탈구")
        String underlyingDisease,

        @Schema(
                description = "프로필 이미지 URL",
                example = "https://example.com/pets/mongsu.jpg"
        )
        String profileImage,

        @Schema(description = "등록 일시")
        LocalDateTime createdAt
) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getBreed(),
                pet.getAge(),
                pet.getGender(),
                pet.getWeight(),
                pet.isNeutered(),
                pet.getUnderlyingDisease(),
                pet.getProfileImage(),
                pet.getCreatedAt()
        );
    }
}