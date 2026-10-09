package com.example.babymungsoo.pet.controller;

import com.example.babymungsoo.pet.service.BreedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/breeds")
public class BreedController {

    private final BreedService breedService;

    @GetMapping
    public List<String> getBreeds(
            @RequestParam(name = "keyword", defaultValue = "") String keyword
    ) {
        return breedService.search(keyword);
    }
}