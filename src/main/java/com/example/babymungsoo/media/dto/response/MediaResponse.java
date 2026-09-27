package com.example.babymungsoo.media.dto.response;

import com.example.babymungsoo.media.entity.MediaFile;

import java.time.LocalDateTime;

public record MediaResponse(
        Long mediaId,
        String fileUrl,
        String mediaType,
        LocalDateTime createdAt
) {

    public static MediaResponse from(MediaFile mediaFile) {
        return new MediaResponse(
                mediaFile.getId(),
//                "/api/v1/media/" + mediaFile.getId() + "/file",
                "/api/v1/media/public/" + mediaFile.getPublicId(),
                mediaFile.getMediaType().name(),
                mediaFile.getCreatedAt()
        );
    }
}
