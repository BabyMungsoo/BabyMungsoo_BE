package com.example.babymungsoo.media;

import java.util.Optional;

public interface MediaAnalyzer {

    // 실제 분석 결과가 아직 없으면(예: 스텁 구현) Optional.empty()를 반환해야 한다.
    Optional<String> analyze(String fileUrl);
}
