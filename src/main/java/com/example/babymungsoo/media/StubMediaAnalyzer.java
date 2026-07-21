package com.example.babymungsoo.media;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StubMediaAnalyzer implements MediaAnalyzer {

    @Override
    public Optional<String> analyze(String fileUrl) {
        // TODO: 실제 이미지 기반 증상 분석(예: Claude Vision) 연동 예정 — 그 전까지는 완료로 보고하지 않는다.
        return Optional.empty();
    }
}
