package com.example.babymungsoo.global.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {

    @Override
    public String upload(MultipartFile file) {
        // TODO: 로컬 파일 저장 또는 S3 업로드 구현 예정
        return "temp-file-url";
    }

    @Override
    public void delete(String fileUrl) {
        // TODO: 파일 삭제 구현 예정
    }
}
