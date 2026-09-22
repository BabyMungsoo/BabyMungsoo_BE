package com.example.babymungsoo.record.entity;

/**
 * 병원에서 받은 처치. 보호자가 고르는 고정 목록이다.
 *
 * <p>자유 텍스트로 받지 않는 이유는 나중에 "IMMEDIATE 판정 중 수액 처치를 받은 비율" 같은
 * 집계를 쿼리로 내기 위해서다. 목록에 없는 처치는 memo 에 적는다.
 */
public enum TreatmentTag {
    FLUID,
    INJECTION,
    TEST,
    PRESCRIPTION,
    HOSPITALIZED,
    OBSERVATION
}
