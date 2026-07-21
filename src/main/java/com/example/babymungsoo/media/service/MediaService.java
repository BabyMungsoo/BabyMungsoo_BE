package com.example.babymungsoo.media.service;

import com.example.babymungsoo.global.auth.CurrentUserProvider;
import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import com.example.babymungsoo.global.storage.StorageService;
import com.example.babymungsoo.media.MediaAnalyzer;
import com.example.babymungsoo.media.MediaFileDownload;
import com.example.babymungsoo.media.entity.MediaAnalysis;
import com.example.babymungsoo.media.entity.MediaAnalysisStatus;
import com.example.babymungsoo.media.entity.MediaFile;
import com.example.babymungsoo.media.entity.MediaType;
import com.example.babymungsoo.media.repository.MediaAnalysisRepository;
import com.example.babymungsoo.media.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaAnalysisRepository mediaAnalysisRepository;
    private final StorageService storageService;
    private final MediaAnalyzer mediaAnalyzer;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public MediaFile upload(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }

        String fileUrl = storageService.upload(file);

        MediaFile mediaFile = MediaFile.builder()
                .userId(currentUserProvider.getCurrentUserId())
                .fileUrl(fileUrl)
                .contentType(contentType)
                .mediaType(MediaType.IMAGE)
                .build();

        return mediaFileRepository.save(mediaFile);
    }

    public MediaFile getMedia(Long mediaId) {
        return findOwnedMedia(mediaId);
    }

    public MediaFileDownload downloadFile(Long mediaId) {
        MediaFile mediaFile = findOwnedMedia(mediaId);
        Resource resource = storageService.load(mediaFile.getFileUrl());
        return new MediaFileDownload(resource, mediaFile.getContentType());
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        MediaFile mediaFile = findOwnedMedia(mediaId);

        mediaAnalysisRepository.findByMediaFileId(mediaFile.getId())
                .ifPresent(mediaAnalysisRepository::delete);

        storageService.delete(mediaFile.getFileUrl());
        mediaFileRepository.delete(mediaFile);
    }

    @Transactional
    public MediaAnalysis analyze(Long mediaId) {
        MediaFile mediaFile = findOwnedMedia(mediaId);

        MediaAnalysis mediaAnalysis = mediaAnalysisRepository.findByMediaFileId(mediaFile.getId())
                .orElseGet(() -> MediaAnalysis.builder()
                        .mediaFileId(mediaFile.getId())
                        .status(MediaAnalysisStatus.PENDING)
                        .build());

        try {
            String resultText = mediaAnalyzer.analyze(mediaFile.getFileUrl());
            mediaAnalysis.complete(resultText);
        } catch (RuntimeException e) {
            mediaAnalysis.fail();
        }

        return mediaAnalysisRepository.save(mediaAnalysis);
    }

    private MediaFile findOwnedMedia(Long mediaId) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        return mediaFileRepository.findByIdAndUserId(mediaId, currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDIA_NOT_FOUND));
    }
}
