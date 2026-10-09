package com.example.babymungsoo.AI.Entity;

public enum TriageLevel {
    IMMEDIATE("지금 바로 동물병원에 가세요"),      // 즉시 내원
    WATCH("빠른 시일 내 병원 진료가 필요해요"),     // 주의 관찰
    NORMAL("지금 당장 병원에 갈 필요는 없어요");    // 일반 관리

    /**
     * 보호자에게 보여 줄 결론 한 줄.
     *
     * <p>모델이 아니라 코드가 만든다. "지금 병원에 가야 하는가"는 등급의 함수라서,
     * 모델이 쓰게 두면 등급과 결론이 어긋날 수 있고 그 자리에 병명이 섞여 들어올 통로도 된다.
     * 문구는 시스템 프롬프트의 등급 정의(IMMEDIATE 즉시, WATCH 빠른 시일 내, NORMAL 지금 불필요)와
     * 같은 강도여야 한다.
     */
    private final String headline;

    TriageLevel(String headline) {
        this.headline = headline;
    }

    public String headline() {
        return headline;
    }
}
