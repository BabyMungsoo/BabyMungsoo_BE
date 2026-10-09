package com.example.babymungsoo.pet.repository;

import com.example.babymungsoo.pet.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Pet> findByIdAndUser_Id(Long petId, Long userId);
}