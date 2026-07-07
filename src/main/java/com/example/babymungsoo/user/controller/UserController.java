package com.example.babymungsoo.user.controller;

import com.example.babymungsoo.user.dto.response.UserMeResponse;
import com.example.babymungsoo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserMeResponse getCurrentUser() {
        return userService.getCurrentUser();
    }
}
