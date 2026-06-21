package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.DepartmentRequest;
import com.mod.hrss.dto.response.DepartmentResponse;
import com.mod.hrss.entity.Department;
import com.mod.hrss.entity.User;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.DepartmentMapper;
import com.mod.hrss.repository.DepartmentRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.getName())) {
            throw new BusinessException("Department name already exists");
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            department.setManager(manager);
        }

        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return departmentMapper.toResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(departmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getName().equalsIgnoreCase(request.getName())) {
            if (departmentRepository.existsByName(request.getName())) {
                throw new BusinessException("Department name already exists");
            }
            department.setName(request.getName());
        }

        department.setDescription(request.getDescription());

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
            department.setManager(manager);
        } else {
            department.setManager(null);
        }

        Department updated = departmentRepository.save(department);
        return departmentMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        departmentRepository.delete(department);
        log.info("Department with id {} was soft-deleted", id);
    }
}
