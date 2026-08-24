package com.example.babymungsoo.AI.Dto;

import com.example.babymungsoo.pet.entity.PetGender;

/**
 * 응급도 분석에 넘기는 반려견 정보.
 *
 * <p>파라미터를 나열해 넘기면 항목이 늘 때마다 인터페이스와 구현체 두 곳의 시그니처가
 * 함께 바뀌므로 객체로 묶는다.
 *
 * <p>{@code pet.entity.Pet}을 직접 받지 않는 이유는 두 가지다. 분석기가 영속 엔티티를
 * 쥐면 트랜잭션 밖에서 지연 로딩을 건드릴 위험이 생기고, 분석 시점의 값을 그대로
 * 스냅샷으로 남겨야 하는데 엔티티는 이후 수정될 수 있다.
 *
 * <p>{@code weight}와 {@code underlyingDisease}는 미입력일 수 있다. 프롬프트로 옮길 때
 * "없음"과 "미입력"을 구분해야 한다. 자세한 이유는
 * {@code ClaudeTriageAnalyzer#buildUserPrompt} 참고.
 *
 * @param breed             품종 (없으면 null)
 * @param age               나이 (없으면 null)
 * @param ageUnit           나이 단위 (예: "세")
 * @param gender            성별 (없으면 null)
 * @param weight            체중 kg (미입력이면 null)
 * @param neutered          중성화 여부. {@code Pet}에서 non-null 컬럼이라 항상 값이 있다
 * @param underlyingDisease 기저질환 (미입력이면 null 또는 빈 문자열)
 */
public record PetProfile(
        String breed,
        Integer age,
        String ageUnit,
        PetGender gender,
        Double weight,
        boolean neutered,
        String underlyingDisease
) {
}
