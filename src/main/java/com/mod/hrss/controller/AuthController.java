package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.request.LoginRequest;
import com.mod.hrss.dto.request.PasswordResetRequest;
import com.mod.hrss.dto.request.SignupRequest;
import com.mod.hrss.dto.request.TokenRefreshRequest;
import com.mod.hrss.dto.response.AuthResponse;
import com.mod.hrss.dto.response.UserResponse;
import com.mod.hrss.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserResponse response = authService.register(signupRequest);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        AuthResponse response = authService.refresh(refreshRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Resolve token: prefer Authorization header, fall back to accessToken cookie
        String tokenToBlacklist = authHeader;
        if (tokenToBlacklist == null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    tokenToBlacklist = "Bearer " + cookie.getValue();
                    break;
                }
            }
        }

        authService.logout(tokenToBlacklist);

        // Clear HttpOnly cookies from the browser
        clearCookie(response, "accessToken");
        clearCookie(response, "refreshToken");

        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        authService.resetPassword(resetRequest);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);   // match the flag used when setting the cookie
        cookie.setPath("/");
        cookie.setMaxAge(0);       // instructs the browser to delete the cookie immediately
        response.addCookie(cookie);
    }
}

