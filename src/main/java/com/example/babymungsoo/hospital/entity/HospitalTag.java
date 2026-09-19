package com.example.babymungsoo.hospital.entity;

/**
 * 큐레이션 목록이 병원에 붙이는 시설 태그. 자유 문장 대신 두는 이유는, 문장은 검증할 수 없지만
 * "MRI 가 있다" 같은 사실은 병원 공식 사이트·기사에서 확인하고 붙일 수 있기 때문이다.
 *
 * <p>JSON 의 tags 값과 이름이 같아야 한다. 모르는 값이 들어오면 기동 시 실패시켜 오타를 바로 잡는다.
 */
public enum HospitalTag {
    /** MRI 장비 보유 */
    MRI,
    /** 응급실·응급의료센터·ICU 같은 전담 응급 시설 운영 (단순 야간 진료와 구분) */
    EMERGENCY_CENTER
}
