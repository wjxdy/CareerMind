package com.careermind.controller;

import com.careermind.dto.ApiResponse;
import com.careermind.dto.LoginRequest;
import com.careermind.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "注册功能已关闭");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);

        // 根据邮箱查询用户信息
        var user = userService.getUserByEmail(request.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));

        return ApiResponse.success(result);
    }
}
