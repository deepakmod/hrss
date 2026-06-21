package com.mod.hrss.service;

import com.mod.hrss.dto.request.LoginRequest;
import com.mod.hrss.dto.request.PasswordResetRequest;
import com.mod.hrss.dto.request.SignupRequest;
import com.mod.hrss.dto.request.TokenRefreshRequest;
import com.mod.hrss.dto.response.AuthResponse;
import com.mod.hrss.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest);
    UserResponse register(SignupRequest signupRequest);
    AuthResponse refresh(TokenRefreshRequest refreshRequest);
    void logout(String token);
    void resetPassword(PasswordResetRequest resetRequest);
}
