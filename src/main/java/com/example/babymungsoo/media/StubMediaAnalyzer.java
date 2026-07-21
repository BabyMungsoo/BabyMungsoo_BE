package com.example.babymungsoo.media;

import org.springframework.stereotype.Component;

@Component
public class StubMediaAnalyzer implements MediaAnalyzer {

    @Override
    public String analyze(String fileUrl) {
        // TODO: 실제 이미지 기반 증상 분석(예: Claude Vision) 연동 예정
        return "분석 대기 중인 스텁 결과입니다.";
    }
}
