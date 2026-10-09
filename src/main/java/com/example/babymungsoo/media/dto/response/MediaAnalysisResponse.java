package com.example.babymungsoo.media.dto.response;

import com.example.babymungsoo.media.entity.MediaAnalysis;

public record MediaAnalysisResponse(
        Long mediaId,
        String status,
        String resultText
) {

    public static MediaAnalysisResponse from(MediaAnalysis mediaAnalysis) {
        return new MediaAnalysisResponse(
                mediaAnalysis.getMediaFileId(),
                mediaAnalysis.getStatus().name(),
                mediaAnalysis.getResultText()
        );
    }
}
