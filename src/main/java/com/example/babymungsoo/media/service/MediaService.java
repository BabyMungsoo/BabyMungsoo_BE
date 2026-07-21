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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

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
        String contentType = detectImageContentType(file);

        String fileUrl = storageService.upload(file);

        MediaFile mediaFile = MediaFile.builder()
                .userId(currentUserProvider.getCurrentUserId())
                .fileUrl(fileUrl)
                .contentType(contentType)
                .mediaType(MediaType.IMAGE)
                .build();

        try {
            return mediaFileRepository.save(mediaFile);
        } catch (RuntimeException e) {
            // DB 저장 실패 시 이미 디스크에 쓰인 파일이 고아로 남지 않도록 보상 삭제
            storageService.delete(fileUrl);
            throw e;
        }
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
        String fileUrl = mediaFile.getFileUrl();

        mediaAnalysisRepository.findByMediaFileId(mediaFile.getId())
                .ifPresent(mediaAnalysisRepository::delete);
        mediaFileRepository.delete(mediaFile);

        // 디스크 삭제는 되돌릴 수 없으므로, DB 트랜잭션이 커밋되어 메타데이터 삭제가 확정된 뒤에만 수행한다.
        // (커밋 전에 지우면 DB 롤백 시 파일은 사라졌는데 행은 남는 불일치가 생김)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageService.delete(fileUrl);
            }
        });
    }

    @Transactional
    public MediaAnalysis analyze(Long mediaId) {
        // MediaFile 행에 쓰기 잠금을 걸어 같은 미디어에 대한 동시 분석 요청을 직렬화한다.
        // (그렇지 않으면 두 요청이 동시에 MediaAnalysis가 없다고 판단해 각자 생성을 시도하다 유니크 제약 위반으로 실패할 수 있음)
        Long currentUserId = currentUserProvider.getCurrentUserId();
        MediaFile mediaFile = mediaFileRepository.findWithLockByIdAndUserId(mediaId, currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDIA_NOT_FOUND));

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

    // 클라이언트가 보낸 Content-Type 헤더는 위조 가능하므로, 실제 픽셀 데이터를 디코딩해 진짜 래스터 이미지인지 검증한다.
    // ImageIO는 SVG(벡터/XML)를 다루는 기본 리더가 없어 이 과정에서 자연스럽게 함께 거부된다.
    private String detectImageContentType(MultipartFile file) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageInputStream == null) {
                throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream);
                reader.read(0);

                String[] mimeTypes = reader.getOriginatingProvider().getMIMETypes();
                return mimeTypes.length > 0 ? mimeTypes[0] : "image/" + reader.getFormatName().toLowerCase();
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new CustomException(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        }
    }
}
