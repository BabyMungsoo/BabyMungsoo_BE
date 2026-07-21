package com.example.babymungsoo.global.storage;

import com.example.babymungsoo.global.exception.CustomException;
import com.example.babymungsoo.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final String URL_PREFIX = "/uploads/";

    private final Path uploadDir;

    public LocalStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");

        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }

        return URL_PREFIX + storedFileName;
    }

    @Override
    public void delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith(URL_PREFIX)) {
            return;
        }

        String storedFileName = fileUrl.substring(URL_PREFIX.length());

        try {
            Files.deleteIfExists(uploadDir.resolve(storedFileName));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }
}