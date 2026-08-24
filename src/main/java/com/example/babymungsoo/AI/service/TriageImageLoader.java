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
import java.util.ArrayList;
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
     * Claude가 받는 이미지 포맷.
     *
     * <p>업로드 허용 목록({@code MediaService.ALLOWED_CONTENT_TYPES})보다 넓다. Claude는 WebP도
     * 받지만 JDK ImageIO에 WebP 리더가 없어 업로드 단계에서 막히기 때문이다. 디코더 의존성을
     * 추가하면 업로드 쪽만 열면 되도록, 여기서는 Claude 기준 그대로 둔다.
     */
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 장당 원본 크기 상한. {@code MediaService.MAX_FILE_BYTES}와 같은 값이어야 한다.
     *
     * <p>Claude 제한은 base64 기준 10MB인데 base64는 원본보다 약 33% 커진다.
     * 원본 7.5MB가 그 경계라 여유를 두고 7MB로 잡는다.
     */
    private static final long MAX_IMAGE_BYTES = 7L * 1024 * 1024;

    /**
     * 요청 1건에 실을 base64 총량 상한.
     *
     * <p>장당 제한만으로는 부족하다. 7MB짜리 5장이면 원본 35MB, base64로는 약 46MB가 되어
     * Anthropic의 <b>요청 전체 32MB 제한</b>을 넘겨 호출 자체가 실패한다. 시스템 프롬프트와
     * JSON 오버헤드가 함께 실리므로 여유를 두고 20MB에서 끊는다.
     */
    private static final long MAX_TOTAL_BASE64_BYTES = 20L * 1024 * 1024;

    /**
     * 요청 1건에 넣을 사진 수 상한. {@code TriageService.MAX_MEDIA_PER_SESSION}과 같은 값이어야 한다.
     *
     * <p>첨부 시점에 개수를 막으므로 새로 만든 세션은 이 상한에 걸리지 않는다.
     * 제한이 생기기 전에 6장 이상 붙은 세션이 남아 있을 수 있어 방어용으로 유지한다.
     */
    private static final int MAX_IMAGES = 5;

    private final MediaFileRepository mediaFileRepository;
    private final StorageService storageService;

    /**
     * 세션 사진을 업로드 순서대로 담되, 장수·합산 크기 상한에 걸리면 거기서 멈춘다.
     *
     * <p>조회가 id 오름차순으로 고정되어 있으므로 어떤 사진이 빠지는지가 호출마다 달라지지 않는다.
     */
    public List<TriageImage> load(Long sessionId) {
        List<MediaFile> mediaFiles = mediaFileRepository.findAllBySessionIdOrderByIdAsc(sessionId);
        if (mediaFiles.isEmpty()) {
            return List.of();
        }

        List<TriageImage> images = new ArrayList<>();
        long totalBase64Bytes = 0;

        for (MediaFile mediaFile : mediaFiles) {
            if (images.size() >= MAX_IMAGES) {
                log.warn("분석 입력에서 제외 - 사진 수 상한({}) 초과. sessionId={}", MAX_IMAGES, sessionId);
                break;
            }

            TriageImage image = toTriageImage(mediaFile);
            if (image == null) {
                continue;
            }

            // base64 출력은 ASCII라 문자 수가 곧 바이트 수다.
            long encodedBytes = image.base64Data().length();
            if (totalBase64Bytes + encodedBytes > MAX_TOTAL_BASE64_BYTES) {
                log.warn("분석 입력에서 제외 - 합산 크기 상한 초과. sessionId={}, mediaId={}, 누적={}B, 추가={}B",
                        sessionId, mediaFile.getId(), totalBase64Bytes, encodedBytes);
                break;
            }

            totalBase64Bytes += encodedBytes;
            images.add(image);
        }

        return List.copyOf(images);
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
