package com.example.babymungsoo.pet.controller;

import com.example.babymungsoo.pet.dto.response.PetProfileResponse;
import com.example.babymungsoo.pet.dto.response.PetResponse;
import com.example.babymungsoo.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "관리자 반려동물",
        description = "관리자 전용 반려동물 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/pets")
public class AdminPetController {

    private final PetService petService;

    @Operation(summary = "전체 반려동물 목록 조회")
    @GetMapping
    public List<PetResponse> getAllPets() {
        return petService.getAllPetsForAdmin();
    }

    @Operation(summary = "반려동물 상세 조회")
    @GetMapping("/{petId}")
    public PetResponse getPet(
            @PathVariable Long petId
    ) {
        return petService.getPetForAdmin(petId);
    }

    @Operation(summary = "AI 분석용 반려동물 프로필 조회")
    @GetMapping("/{petId}/profile")
    public PetProfileResponse getPetProfile(
            @PathVariable Long petId
    ) {
        return petService.getPetProfileForAdmin(petId);
    }
}