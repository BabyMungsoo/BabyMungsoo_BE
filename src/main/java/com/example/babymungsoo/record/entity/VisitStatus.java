package com.example.babymungsoo.record.entity;

/**
 * 분석 결과를 받은 뒤 병원에 갔는지.
 *
 * <p>가지 않았다는 답도 저장한다. 진료 내용만 받으면 병원에 가지 않은 보호자는 영영
 * 미응답으로 남아, "IMMEDIATE 판정 중 실제 내원 비율" 같은 지표를 낼 수 없다.
 */
public enum VisitStatus {
    VISITED,
    NOT_VISITED
}
