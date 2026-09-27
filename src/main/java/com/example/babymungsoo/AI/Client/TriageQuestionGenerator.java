package com.example.babymungsoo.AI.Client;

import com.example.babymungsoo.AI.Dto.PetProfile;
import com.example.babymungsoo.AI.Dto.TriageImage;
import com.example.babymungsoo.AI.Dto.TriageQuestionSet;
import com.example.babymungsoo.global.exception.CustomException;

import java.util.List;

/**
 * 초기 증상을 보고 추가로 물어볼 문진 질문을 만드는 생성기.
 *
 * <p>{@link TriageAnalyzer}와 같은 방식으로 구현체를 {@code claude.api.mock} 설정으로 전환한다.
 * <ul>
 *   <li>{@code true} → {@link StubTriageQuestionGenerator} (실호출·과금 없음, 기본값)</li>
 *   <li>{@code false} → {@link ClaudeTriageQuestionGenerator} (Claude 실제 호출)</li>
 * </ul>
 *
 * <p>이 단계는 응급도를 판정하지 않는다. 판정은 {@link TriageAnalyzer}가 전담한다.
 * 질문 생성이 실패해도 최종 분석은 진행되어야 하므로, 호출자가 예외를 삼키고
 * 질문 없이 넘어간다({@code TriageService.generateQuestions()} 참고).
 */
public interface TriageQuestionGenerator {

    /**
     * 초기 증상을 바탕으로 추가 질문을 만든다.
     *
     * @param initialSymptom 보호자가 입력한 자연어 증상
     * @param pet            분석 시점의 반려견 정보
     * @param images         세션에 첨부된 증상 사진. 없으면 빈 리스트
     * @return 질문 필요 여부와 질문 목록(0~5개)
     * @throws CustomException API 키 미설정, 호출 실패, 타임아웃, 응답 파싱 실패
     */
    TriageQuestionSet generate(String initialSymptom, PetProfile pet, List<TriageImage> images);
}
