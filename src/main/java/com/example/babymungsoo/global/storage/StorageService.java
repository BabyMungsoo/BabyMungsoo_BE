package com.example.babymungsoo.global.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String upload(MultipartFile file);

    void delete(String fileUrl);

    Resource load(String fileUrl);
}