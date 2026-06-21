package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.LoginRequest;
import com.mod.hrss.dto.request.PasswordResetRequest;
import com.mod.hrss.dto.request.SignupRequest;
import com.mod.hrss.dto.request.TokenRefreshRequest;
import com.mod.hrss.dto.response.AuthResponse;
import com.mod.hrss.dto.response.UserResponse;
import com.mod.hrss.entity.Role;
import com.mod.hrss.entity.RoleName;
import com.mod.hrss.entity.User;
import com.mod.hrss.entity.UserStatus;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.UserMapper;
import com.mod.hrss.repository.RoleRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.security.jwt.JwtTokenProvider;
import com.mod.hrss.security.jwt.TokenBlacklistService;
import com.mod.hrss.service.AuthService;
import com.mod.hrss.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();
        
        // Update password changed timestamps if necessary
        if (user.getPasswordChangedAt() == null) {
            user.setPasswordChangedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        log.info("User {} successfully logged in", userPrincipal.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                userPrincipal.getUsername(),
                roles
        );
    }

    @Override
    @Transactional
    public UserResponse register(SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new BusinessException("Email already in use");
        }

        if (userRepository.existsByEmployeeCode(signupRequest.getEmployeeCode())) {
            throw new BusinessException("Employee code already in use");
        }

        User user = new User();
        user.setEmployeeCode(signupRequest.getEmployeeCode());
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPhone(signupRequest.getPhone());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChangedAt(LocalDateTime.now());
        // Password expires in 90 days
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(90));

        Role employeeRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                .orElseThrow(() -> new BusinessException("Default Role not found"));
        user.setRoles(Collections.singleton(employeeRole));

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // Send registration notification
        notificationService.sendNotification(
                savedUser.getId(),
                "Welcome to HRSS",
                "Hello " + savedUser.getFirstName() + ", your HRSS account is active!"
        );

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse refresh(TokenRefreshRequest refreshRequest) {
        String oldRefreshToken = refreshRequest.getRefreshToken();

        if (!tokenProvider.validateToken(oldRefreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }

        if (tokenBlacklistService.isTokenBlacklisted(oldRefreshToken)) {
            throw new BusinessException("Refresh token has been blacklisted (token reuse detected)");
        }

        String username = tokenProvider.getUsernameFromJwt(oldRefreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("User account is inactive");
        }

        // Generate new tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        // Blacklist old refresh token to implement rotation
        tokenBlacklistService.blacklistToken(oldRefreshToken);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        log.info("Token rotated for user {}", username);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                username,
                roles
        );
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            tokenBlacklistService.blacklistToken(jwt);
            log.info("Token successfully logged out and blacklisted");
        }
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest resetRequest) {
        User user = userRepository.findByEmail(resetRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(resetRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(90));
        userRepository.save(user);

        log.info("Password reset successfully for user {}", user.getEmail());

        notificationService.sendNotification(
                user.getId(),
                "Password Security Alert",
                "Your account password was updated successfully."
        );
    }
}
