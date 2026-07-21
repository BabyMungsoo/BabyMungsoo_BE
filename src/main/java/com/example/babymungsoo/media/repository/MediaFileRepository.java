package com.example.babymungsoo.media.repository;

import com.example.babymungsoo.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    Optional<MediaFile> findByIdAndUserId(Long id, Long userId);
}
