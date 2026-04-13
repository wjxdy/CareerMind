package com.careermind.service;

import com.careermind.domain.User;
import com.careermind.dto.LoginRequest;
import com.careermind.dto.RegisterRequest;

public interface UserService {
    User register(RegisterRequest request);
    String login(LoginRequest request);
    User getCurrentUser(Long userId);
    User getUserByEmail(String email);
    User updateUser(Long userId, User user);
}
