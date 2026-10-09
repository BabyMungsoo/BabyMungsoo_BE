package com.example.babymungsoo.pet.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class BreedService {

    private static final List<String> BREEDS = List.of(
            "골든 리트리버",
            "닥스훈트",
            "래브라도 리트리버",
            "말티즈",
            "믹스견",
            "보더 콜리",
            "비글",
            "비숑 프리제",
            "시바 이누",
            "시베리안 허스키",
            "시츄",
            "웰시 코기",
            "진돗개",
            "치와와",
            "포메라니안",
            "푸들"
    );

    public List<String> search(String keyword) {
        String query = normalize(keyword);

        return BREEDS.stream()
                .filter(breed -> normalize(breed).contains(query))
                .sorted()
                .toList();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}