package com.example.babymungsoo.media;

import org.springframework.core.io.Resource;

public record MediaFileDownload(
        Resource resource,
        String contentType
) {
}
