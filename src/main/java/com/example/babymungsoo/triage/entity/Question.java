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
}
