package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.UserRequest;
import com.mod.hrss.dto.response.UserResponse;
import com.mod.hrss.entity.Department;
import com.mod.hrss.entity.Role;
import com.mod.hrss.entity.RoleName;
import com.mod.hrss.entity.User;
import com.mod.hrss.entity.UserStatus;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.UserMapper;
import com.mod.hrss.repository.DepartmentRepository;
import com.mod.hrss.repository.RoleRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.NotificationService;
import com.mod.hrss.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new BusinessException("Email already in use");
        }

        if (userRepository.existsByEmployeeCode(userRequest.getEmployeeCode())) {
            throw new BusinessException("Employee code already in use");
        }

        User user = new User();
        user.setEmployeeCode(userRequest.getEmployeeCode());
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setEmail(userRequest.getEmail());
        user.setPhone(userRequest.getPhone());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword() != null ? userRequest.getPassword() : "password123"));
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordExpiresAt(LocalDateTime.now().plusDays(90));

        if (userRequest.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        }

        if (userRequest.getManagerId() != null) {
            User manager = userRepository.findById(userRequest.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(manager);
        }

        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleNameStr : userRequest.getRoles()) {
                Role role = roleRepository.findByName(RoleName.valueOf(roleNameStr))
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleNameStr));
                roles.add(role);
            }
            user.setRoles(roles);
        } else {
            Role employeeRole = roleRepository.findByName(RoleName.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new BusinessException("Default Role not found"));
            user.setRoles(Set.of(employeeRole));
        }

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByDepartment(Long departmentId, Pageable pageable) {
        return userRepository.findByDepartmentId(departmentId, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByManager(Long managerId, Pageable pageable) {
        return userRepository.findByManagerId(managerId, pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest userRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhone(userRequest.getPhone());

        // Update email if changed and not in use
        if (!user.getEmail().equalsIgnoreCase(userRequest.getEmail())) {
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                throw new BusinessException("Email already in use");
            }
            user.setEmail(userRequest.getEmail());
        }

        // Update department
        if (userRequest.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userRequest.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        // Update manager
        if (userRequest.getManagerId() != null) {
            if (userRequest.getManagerId().equals(id)) {
                throw new BusinessException("User cannot be their own manager");
            }
            User manager = userRepository.findById(userRequest.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        // Update roles
        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleNameStr : userRequest.getRoles()) {
                Role role = roleRepository.findByName(RoleName.valueOf(roleNameStr))
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleNameStr));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        User updated = userRepository.save(user);
        return userMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // Soft delete
        userRepository.delete(user);
        log.info("User with id {} was soft-deleted", id);
    }

    @Override
    @Transactional
    public UserResponse assignManager(Long employeeId, Long managerId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        
        if (employeeId.equals(managerId)) {
            throw new BusinessException("An employee cannot be their own manager");
        }

        User manager = null;
        if (managerId != null) {
            manager = userRepository.findById(managerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
        }

        employee.setManager(manager);
        User saved = userRepository.save(employee);

        if (manager != null) {
            notificationService.sendNotification(
                    employeeId,
                    "Manager Assignment Update",
                    "You have been assigned to manager: " + manager.getFirstName() + " " + manager.getLastName()
            );
            notificationService.sendNotification(
                    managerId,
                    "Team Assignment Update",
                    "Employee " + employee.getFirstName() + " " + employee.getLastName() + " has been assigned to your team."
            );
        }

        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, String firstName, String lastName, String phone) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }
}
