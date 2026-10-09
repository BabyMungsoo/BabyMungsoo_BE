package com.example.babymungsoo.media.repository;

import com.example.babymungsoo.media.entity.MediaAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaAnalysisRepository extends JpaRepository<MediaAnalysis, Long> {
    Optional<MediaAnalysis> findByMediaFileId(Long mediaFileId);
}
