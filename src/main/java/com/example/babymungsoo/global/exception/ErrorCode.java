package com.example.babymungsoo.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "미디어를 찾을 수 없습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "현재는 사진 파일만 업로드할 수 있습니다."),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}