package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.LeaveApplyRequest;
import com.mod.hrss.dto.response.LeaveRequestDto;
import com.mod.hrss.entity.LeaveRequest;
import com.mod.hrss.entity.LeaveStatus;
import com.mod.hrss.entity.LeaveType;
import com.mod.hrss.entity.User;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.LeaveRequestMapper;
import com.mod.hrss.repository.LeaveRequestRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.LeaveService;
import com.mod.hrss.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public LeaveRequestDto applyLeave(LeaveApplyRequest request, String employeeEmail) {
        User employee = userRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee user not found"));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException("Start date cannot be after end date");
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(LeaveType.valueOf(request.getLeaveType()));
        leaveRequest.setStartDate(request.getStartDate());
        leaveRequest.setEndDate(request.getEndDate());
        leaveRequest.setReason(request.getReason());
        leaveRequest.setApprovalStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request applied by {} from {} to {}", employeeEmail, request.getStartDate(), request.getEndDate());

        // Notify manager
        if (employee.getManager() != null) {
            notificationService.sendNotification(
                    employee.getManager().getId(),
                    "New Leave Request",
                    employee.getFirstName() + " has applied for leave from " + request.getStartDate() + " to " + request.getEndDate()
            );
        }

        return leaveRequestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public LeaveRequestDto reviewLeave(Long leaveId, String approvalStatusStr, String managerEmail) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found with id: " + leaveId));

        if (leaveRequest.getApprovalStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Leave request has already been reviewed");
        }

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager user not found"));

        // Enforce hierarchy: must be employee's manager or an ADMIN
        boolean isManager = manager.getId().equals(leaveRequest.getEmployee().getManager() != null ? leaveRequest.getEmployee().getManager().getId() : null);
        boolean isAdmin = manager.getRoles().stream().anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));

        if (!isManager && !isAdmin) {
            throw new AccessDeniedException("Not authorized to review this leave request");
        }

        LeaveStatus newStatus = LeaveStatus.valueOf(approvalStatusStr);
        leaveRequest.setApprovalStatus(newStatus);
        leaveRequest.setApprovedBy(manager);

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request id {} reviewed by {} -> Status: {}", leaveId, managerEmail, newStatus);

        // Notify employee
        notificationService.sendNotification(
                leaveRequest.getEmployee().getId(),
                "Leave Request Reviewed",
                "Your leave request from " + leaveRequest.getStartDate() + " has been " + newStatus.name() + " by " + manager.getFirstName()
        );

        return leaveRequestMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getEmployeeLeaves(Long employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeId(employeeId, pageable).map(leaveRequestMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getTeamLeaves(String managerEmail, Pageable pageable) {
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
        return leaveRequestRepository.findByEmployeeManagerId(manager.getId(), pageable).map(leaveRequestMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestDto> getAllLeaves(Pageable pageable) {
        return leaveRequestRepository.findAll(pageable).map(leaveRequestMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestDto getLeaveById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        return leaveRequestMapper.toDto(leaveRequest);
    }
}
