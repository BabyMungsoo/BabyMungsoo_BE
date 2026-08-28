package com.example.babymungsoo.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
     * AI가 만든 세션 전용 질문을 만든다.
     *
     * <p>{@code code}가 unique 라 세션 id와 순번을 조합해 충돌을 피한다.
     * {@code symptomCategory}는 비워 둔다. 분류를 고르는 흐름이 아니기 때문이다.
     *
     * <p>{@code content}는 컬럼 상한이 255자라 넘치면 잘라 낸다. 질문 한 문장이
     * 255자를 넘을 일은 없지만, 모델 응답 때문에 저장이 실패하는 일은 없어야 한다.
     */
    public static Question forSession(Long sessionId, int orderNo, String content) {
        String trimmed = content.length() > CONTENT_MAX_LENGTH
                ? content.substring(0, CONTENT_MAX_LENGTH)
                : content;

        return Question.builder()
                .code("S" + sessionId + "_" + orderNo)
                .content(trimmed)
                .orderNo(orderNo)
                .sessionId(sessionId)
                .build();
    }

    private static final int CONTENT_MAX_LENGTH = 255;
}
