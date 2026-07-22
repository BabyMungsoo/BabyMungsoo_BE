package com.example.babymungsoo.pet.entity;

import com.example.babymungsoo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "dog")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dog_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "dog_name", nullable = false, length = 50)
    private String name;

    @Column(name = "dog_breed", nullable = false, length = 50)
    private String breed;

    @Column(name = "dog_age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "dog_gender", nullable = false, length = 10)
    private PetGender gender;

    @Column(name = "dog_weight")
    private Double weight;

    @Column(name = "is_neutered", nullable = false)
    private boolean neutered;

    @Column(name = "underlying_disease", length = 255)
    private String underlyingDisease;

    @Column(name = "dog_profile_image", length = 255)
    private String profileImage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void update(
            String name,
            String breed,
            Integer age,
            PetGender gender,
            Double weight,
            Boolean neutered,
            String underlyingDisease,
            String profileImage
    ) {
        if (name != null) {
            this.name = name.trim();
        }

        if (breed != null) {
            this.breed = breed.trim();
        }

        if (age != null) {
            this.age = age;
        }

        if (gender != null) {
            this.gender = gender;
        }

        if (weight != null) {
            this.weight = weight;
        }

        if (neutered != null) {
            this.neutered = neutered;
        }

        if (underlyingDisease != null) {
            this.underlyingDisease = normalizeNullableValue(underlyingDisease);
        }

        if (profileImage != null) {
            this.profileImage = normalizeNullableValue(profileImage);
        }
    }

    private String normalizeNullableValue(String value) {
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}