package com.example.babymungsoo.triage.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 증상에 따라 제공되는 추가 질문(마스터 데이터).
 * 사전에 시드(seed)로 저장해 두고, 증상 카테고리별로 조회해 사용자에게 노출한다.
 */
@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(length = 50)
    private String symptomCategory;

    @Column(nullable = false)
    private Integer orderNo;

    /**
     * 이 질문이 속한 문진 세션. 마스터 질문이면 null이다.
     *
     * <p>null이면 증상 분류별로 미리 저장해 둔 마스터 질문이고, 값이 있으면 그 세션의
     * 초기 증상을 보고 AI가 만든 질문이다. 두 종류를 한 테이블에 두는 대신, 조회는
     * {@code QuestionRepository}에서 {@code SessionIdIsNull} 조건으로 갈라 둔다.
     */
    @Column
    private Long sessionId;

    /**
     * 보호자가 고를 답변 선택지. 비어 있으면 자유 입력 질문이다.
     *
     * <p>마스터 질문은 항상 비어 있고, AI 생성 질문은 보통 2~4개 + "잘 모르겠어요"다.
     * 순서가 곧 "가벼운 것 → 심한 것"이라 {@code @OrderColumn}으로 보존한다.
     * 순서 컬럼이 있으면 Hibernate가 bag이 아니라 리스트로 다루므로, 나중에 다른
     * 컬렉션과 함께 fetch join 해도 {@code MultipleBagFetchException}이 나지 않는다.
     */
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = OPTION_MAX_LENGTH)
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /**
     * AI가 만든 세션 전용 질문을 만든다.
     *
     * <p>{@code code}가 unique 라 세션 id와 순번을 조합해 충돌을 피한다.
     * {@code symptomCategory}는 비워 둔다. 분류를 고르는 흐름이 아니기 때문이다.
     *
     * <p>{@code content}는 컬럼 상한이 255자라 넘치면 잘라 낸다. 질문 한 문장이
     * 255자를 넘을 일은 없지만, 모델 응답 때문에 저장이 실패하는 일은 없어야 한다.
     * 선택지도 같은 이유로 상한(100자)에서 자른다. 프롬프트 규칙은 20자다.
     */
    public static Question forSession(Long sessionId, int orderNo, String content, List<String> options) {
        List<String> trimmedOptions = options == null
                ? List.of()
                : options.stream().map(option -> truncate(option, OPTION_MAX_LENGTH)).toList();

        return Question.builder()
                .code("S" + sessionId + "_" + orderNo)
                .content(truncate(content, CONTENT_MAX_LENGTH))
                .orderNo(orderNo)
                .sessionId(sessionId)
                .options(new ArrayList<>(trimmedOptions))
                .build();
    }

    /** 선택지가 있는 질문인지. 응답의 {@code answerType}을 가른다. */
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private static final int CONTENT_MAX_LENGTH = 255;
    private static final int OPTION_MAX_LENGTH = 100;
}
