package com.mod.hrss.service;

import com.mod.hrss.dto.request.UserRequest;
import com.mod.hrss.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserRequest userRequest);
    UserResponse getUserById(Long id);
    UserResponse getUserByEmail(String email);
    Page<UserResponse> getAllUsers(Pageable pageable);
    Page<UserResponse> getUsersByDepartment(Long departmentId, Pageable pageable);
    Page<UserResponse> getUsersByManager(Long managerId, Pageable pageable);
    UserResponse updateUser(Long id, UserRequest userRequest);
    void deleteUser(Long id);
    UserResponse assignManager(Long employeeId, Long managerId);
    UserResponse updateProfile(String email, String firstName, String lastName, String phone);
}
