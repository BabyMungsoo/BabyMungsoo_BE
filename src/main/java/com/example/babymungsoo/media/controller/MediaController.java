package com.example.babymungsoo.media.controller;

import com.example.babymungsoo.global.response.ApiResponse;
import com.example.babymungsoo.media.dto.response.MediaAnalysisResponse;
import com.example.babymungsoo.media.dto.response.MediaResponse;
import com.example.babymungsoo.media.entity.MediaAnalysis;
import com.example.babymungsoo.media.entity.MediaFile;
import com.example.babymungsoo.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MediaResponse> upload(@RequestParam("file") MultipartFile file) {
        MediaFile mediaFile = mediaService.upload(file);
        return ApiResponse.success(MediaResponse.from(mediaFile));
    }

    @GetMapping("/{mediaId}")
    public ApiResponse<MediaResponse> getMedia(@PathVariable Long mediaId) {
        MediaFile mediaFile = mediaService.getMedia(mediaId);
        return ApiResponse.success(MediaResponse.from(mediaFile));
    }

    @DeleteMapping("/{mediaId}")
    public ApiResponse<Void> deleteMedia(@PathVariable Long mediaId) {
        mediaService.deleteMedia(mediaId);
        return ApiResponse.success("미디어가 삭제되었습니다.");
    }

    @PostMapping("/{mediaId}/analyze")
    public ApiResponse<MediaAnalysisResponse> analyze(@PathVariable Long mediaId) {
        MediaAnalysis mediaAnalysis = mediaService.analyze(mediaId);
        return ApiResponse.success(MediaAnalysisResponse.from(mediaAnalysis));
    }
}
