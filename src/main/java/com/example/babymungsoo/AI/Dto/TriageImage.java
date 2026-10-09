package com.example.babymungsoo.AI.Dto;

/**
 * 응급도 분석에 함께 넘기는 증상 사진 한 장.
 *
 * <p>URL 소스가 아니라 base64인 이유는 Anthropic 서버가 URL에 직접 접근할 수 있어야
 * 하는데, 업로드 파일은 로컬 디스크에 저장되고 우리 API를 통해서만 서빙되기 때문이다.
 *
 * @param contentType 이미지 MIME 타입. Claude가 받는 것은 jpeg/png/gif/webp 뿐이다
 * @param base64Data  개행이 없는 base64 문자열
 */
public record TriageImage(
        String contentType,
        String base64Data
) {
}
