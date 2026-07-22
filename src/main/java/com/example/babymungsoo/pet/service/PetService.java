package com.example.babymungsoo.pet.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.pet.dto.request.PetCreateRequest;
import com.example.babymungsoo.pet.dto.request.PetUpdateRequest;
import com.example.babymungsoo.pet.dto.response.PetProfileResponse;
import com.example.babymungsoo.pet.dto.response.PetResponse;
import com.example.babymungsoo.pet.entity.Pet;
import com.example.babymungsoo.pet.repository.PetRepository;
import com.example.babymungsoo.user.entity.User;
import com.example.babymungsoo.user.entity.UserRole;
import com.example.babymungsoo.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public PetResponse createPet(PetCreateRequest request) {
        User currentUser = findCurrentUser();

        Pet pet = Pet.builder()
                .user(currentUser)
                .name(request.name().trim())
                .breed(request.breed().trim())
                .age(request.age())
                .gender(request.gender())
                .weight(request.weight())
                .neutered(request.isNeutered())
                .underlyingDisease(normalizeNullableValue(
                        request.underlyingDisease()
                ))
                .profileImage(normalizeNullableValue(
                        request.profileImage()
                ))
                .build();

        Pet savedPet = petRepository.save(pet);

        return PetResponse.from(savedPet);
    }

    public List<PetResponse> getPets() {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        return petRepository
                .findAllByUser_IdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .map(PetResponse::from)
                .toList();
    }

    public PetResponse getPet(Long petId) {
        Pet pet = findOwnedPet(petId);

        return PetResponse.from(pet);
    }

    @Transactional
    public PetResponse updatePet(
            Long petId,
            PetUpdateRequest request
    ) {
        Pet pet = findOwnedPet(petId);

        pet.update(
                request.name(),
                request.breed(),
                request.age(),
                request.gender(),
                request.weight(),
                request.isNeutered(),
                request.underlyingDisease(),
                request.profileImage()
        );

        return PetResponse.from(pet);
    }

    @Transactional
    public void deletePet(Long petId) {
        Pet pet = findOwnedPet(petId);

        petRepository.delete(pet);
    }

    public PetProfileResponse getPetProfile(Long petId) {
        Pet pet = findOwnedPet(petId);

        return PetProfileResponse.from(pet);
    }

    private User findCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        return userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private Pet findOwnedPet(Long petId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        return petRepository
                .findByIdAndUser_Id(petId, currentUserId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PET_NOT_FOUND)
                );
    }

    private String normalizeNullableValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return trimmedValue.isEmpty()
                ? null
                : trimmedValue;
    }

    // 관리자 전용 조회
    public List<PetResponse> getAllPetsForAdmin() {
        User currentUser = findCurrentUser();
        validateAdmin(currentUser);

        return petRepository.findAll()
                .stream()
                .map(PetResponse::from)
                .toList();
    }

    public PetResponse getPetForAdmin(Long petId) {
        validateAdmin(findCurrentUser());

        Pet pet = findPet(petId);

        return PetResponse.from(pet);
    }

    public PetProfileResponse getPetProfileForAdmin(Long petId) {
        validateAdmin(findCurrentUser());

        Pet pet = findPet(petId);

        return PetProfileResponse.from(pet);
    }

    private Pet findPet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PET_NOT_FOUND)
                );
    }

    private void validateAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }


}