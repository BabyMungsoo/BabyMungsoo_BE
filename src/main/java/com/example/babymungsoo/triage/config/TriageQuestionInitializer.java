package com.example.babymungsoo.triage.config;

import com.example.babymungsoo.triage.entity.Question;
import com.example.babymungsoo.triage.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@org.springframework.context.annotation.Profile({"local", "dev"})
@RequiredArgsConstructor
public class TriageQuestionInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        // count() 를 쓰면 세션별 AI 생성 질문이 쌓인 뒤로는 항상 0보다 커져,
        // 새 DB에서도 마스터 시드가 돌지 않는다. 마스터만 세어 판단한다.
        if (questionRepository.countBySessionIdIsNull() > 0) {
            return;
        }

        questionRepository.saveAll(List.of(
                Question.builder().code("VOMIT_01").content("구토를 몇 회 했나요?").symptomCategory("구토").orderNo(1).build(),
                Question.builder().code("VOMIT_02").content("구토물에 피나 이물질이 섞여 있나요?").symptomCategory("구토").orderNo(2).build(),
                Question.builder().code("VOMIT_03").content("마지막 식사는 언제였나요?").symptomCategory("구토").orderNo(3).build(),
                Question.builder().code("SEIZURE_01").content("경련이 몇 분간 지속되었나요?").symptomCategory("경련").orderNo(1).build(),
                Question.builder().code("SEIZURE_02").content("현재 의식이 없거나 반응이 없나요?").symptomCategory("경련").orderNo(2).build(),
                Question.builder().code("INGEST_01").content("무엇을 얼마나 섭취했나요?").symptomCategory("이물질섭취").orderNo(1).build(),
                Question.builder().code("INGEST_02").content("섭취한 지 얼마나 지났나요?").symptomCategory("이물질섭취").orderNo(2).build()
        ));
    }
}
