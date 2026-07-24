package com.example.babymungsoo.pet.dto.response;

import com.example.babymungsoo.pet.entity.Pet;
import com.example.babymungsoo.pet.entity.PetGender;
import io.swagger.v3.oas.annotations.media.Schema;

public record PetProfileResponse(

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
                description = "AI 분석 요청에 전달할 기초 프로필 정보",
                example = "품종: 말티즈, 나이: 5세, 성별: 수컷, 체중: 3.8kg, 중성화 여부: 완료, 기저질환: 슬개골 탈구"
        )
        String basicRiskInfo
) {

    public static PetProfileResponse from(Pet pet) {
        return new PetProfileResponse(
                pet.getId(),
                pet.getName(),
                pet.getBreed(),
                pet.getAge(),
                pet.getGender(),
                pet.getWeight(),
                pet.isNeutered(),
                pet.getUnderlyingDisease(),
                createBasicRiskInfo(pet)
        );
    }

    private static String createBasicRiskInfo(Pet pet) {
        String gender = pet.getGender() == PetGender.MALE
                ? "수컷"
                : "암컷";

        String weight = pet.getWeight() == null
                ? "미입력"
                : pet.getWeight() + "kg";

        String neutered = pet.isNeutered()
                ? "완료"
                : "미완료";

        String underlyingDisease =
                pet.getUnderlyingDisease() == null
                        || pet.getUnderlyingDisease().isBlank()
                        ? "없음"
                        : pet.getUnderlyingDisease();

        return String.format(
                "품종: %s, 나이: %d세, 성별: %s, 체중: %s, 중성화 여부: %s, 기저질환: %s",
                pet.getBreed(),
                pet.getAge(),
                gender,
                weight,
                neutered,
                underlyingDisease
        );
    }
}