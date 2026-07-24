package com.example.babymungsoo.pet.controller;

import com.example.babymungsoo.pet.dto.request.PetCreateRequest;
import com.example.babymungsoo.pet.dto.request.PetUpdateRequest;
import com.example.babymungsoo.pet.dto.response.PetProfileResponse;
import com.example.babymungsoo.pet.dto.response.PetResponse;
import com.example.babymungsoo.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "반려동물",
        description = "반려동물 프로필 CRUD 및 AI 분석용 프로필 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pets")
public class PetController {

    private final PetService petService;

    @Operation(summary = "반려동물 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse createPet(
            @Valid @RequestBody PetCreateRequest request
    ) {
        return petService.createPet(request);
    }

    @Operation(summary = "현재 사용자의 반려동물 목록 조회")
    @GetMapping
    public List<PetResponse> getPets() {
        return petService.getPets();
    }

    @Operation(summary = "반려동물 상세 조회")
    @GetMapping("/{petId}")
    public PetResponse getPet(
            @PathVariable Long petId
    ) {
        return petService.getPet(petId);
    }

    @Operation(summary = "반려동물 정보 수정")
    @PatchMapping("/{petId}")
    public PetResponse updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request
    ) {
        return petService.updatePet(petId, request);
    }

    @Operation(summary = "반려동물 삭제")
    @DeleteMapping("/{petId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePet(
            @PathVariable Long petId
    ) {
        petService.deletePet(petId);
    }

    @Operation(summary = "AI 분석 요청용 반려동물 프로필 조회")
    @GetMapping("/{petId}/profile")
    public PetProfileResponse getPetProfile(
            @PathVariable Long petId
    ) {
        return petService.getPetProfile(petId);
    }
}