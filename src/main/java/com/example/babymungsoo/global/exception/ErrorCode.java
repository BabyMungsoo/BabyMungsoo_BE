package com.example.babymungsoo.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
    TRIAGE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "트리아지 세션을 찾을 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    TRIAGE_SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 세션에는 답변을 추가할 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    MEDIA_NOT_FOUND(HttpStatus.NOT_FOUND, "미디어를 찾을 수 없습니다."),
    MEDIA_ALREADY_ATTACHED(HttpStatus.CONFLICT, "이미 다른 문진 세션에 연결된 사진입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.BAD_REQUEST, "JPG, PNG, GIF 형식의 사진만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "사진 한 장의 크기는 7MB를 넘을 수 없습니다."),
    FILE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,"접근 권한이 없습니다."),
    HOSPITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "병원을 찾을 수 없습니다."),
    INVALID_EMERGENCY_LEVEL(HttpStatus.BAD_REQUEST, "지원하지 않는 응급 등급입니다."),
    KAKAO_API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 API 설정이 완료되지 않았습니다."),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "카카오 장소 검색 요청에 실패했습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 기록을 찾을 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다."),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 기록에 대한 리포트가 존재합니다."),

    TRIAGE_SESSION_NOT_COMPLETED(HttpStatus.CONFLICT, "완료되지 않은 세션은 분석할 수 없습니다."),
    AI_API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 분석 설정이 완료되지 않았습니다."),
    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "AI 분석 요청에 실패했습니다."),
    AI_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI 분석 응답 시간이 초과되었습니다."),
    AI_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 해석할 수 없습니다.");


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
