package com.example.babymungsoo.AI.service;

import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.global.storage.StorageService;
import com.example.babymungsoo.media.entity.MediaFile;
import com.example.babymungsoo.media.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * 문진 세션에 연결된 사진을 읽어 분석 입력용 base64 이미지로 옮긴다.
 *
 * <p>사진은 응급도 판단의 보조 근거이지 필수 입력이 아니다. 그래서 이 클래스는
 * <b>어떤 실패도 위로 던지지 않는다.</b> 파일이 사라졌거나 포맷이 맞지 않거나 너무 크면
 * 그 장만 조용히 빼고 나머지로 진행한다. 한 장이 깨졌다고 분석 자체가 실패하면,
 * 정작 급한 보호자가 텍스트 기반 판단조차 못 받게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TriageImageLoader {

    /**
     * Claude가 받는 이미지 포맷. 업로드 단계에서도 같은 목록으로 거르지만,
     * 이 목록이 좁혀지기 전에 저장된 파일이 남아 있을 수 있어 여기서 한 번 더 확인한다.
     */
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 장당 원본 크기 상한.
     *
     * <p>Claude 제한은 base64 기준 10MB인데 base64는 원본보다 약 33% 커진다.
     * 원본 7.5MB가 그 경계라 여유를 두고 7MB로 잡는다.
     */
    private static final long MAX_IMAGE_BYTES = 7L * 1024 * 1024;

    /** 요청 1건에 넣을 사진 수 상한. 세션당 첨부는 5장까지를 전제로 한다. */
    private static final int MAX_IMAGES = 5;

    private final MediaFileRepository mediaFileRepository;
    private final StorageService storageService;

    public List<TriageImage> load(Long sessionId) {
        List<MediaFile> mediaFiles = mediaFileRepository.findAllBySessionIdOrderByIdAsc(sessionId);
        if (mediaFiles.isEmpty()) {
            return List.of();
        }

        return mediaFiles.stream()
                .limit(MAX_IMAGES)
                .map(this::toTriageImage)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private TriageImage toTriageImage(MediaFile mediaFile) {
        String contentType = mediaFile.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("분석 입력에서 제외 - 지원하지 않는 이미지 포맷. mediaId={}, contentType={}",
                    mediaFile.getId(), contentType);
            return null;
        }

        try {
            Resource resource = storageService.load(mediaFile.getFileUrl());

            long size = resource.contentLength();
            if (size <= 0 || size > MAX_IMAGE_BYTES) {
                log.warn("분석 입력에서 제외 - 크기 초과. mediaId={}, bytes={}", mediaFile.getId(), size);
                return null;
            }

            try (InputStream in = resource.getInputStream()) {
                // Base64 기본 인코더는 개행을 넣지 않는다. MIME 인코더를 쓰면 76자마다
                // 개행이 들어가 API가 거부하므로 바꾸지 말 것.
                return new TriageImage(contentType, Base64.getEncoder().encodeToString(in.readAllBytes()));
            }
        } catch (IOException | RuntimeException e) {
            log.warn("분석 입력에서 제외 - 파일을 읽지 못함. mediaId={}, reason={}",
                    mediaFile.getId(), e.getMessage());
            return null;
        }
    }
}
