package com.mod.hrss.service;

import com.mod.hrss.dto.request.LeaveApplyRequest;
import com.mod.hrss.dto.response.LeaveRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveService {
    LeaveRequestDto applyLeave(LeaveApplyRequest request, String employeeEmail);
    LeaveRequestDto reviewLeave(Long leaveId, String approvalStatusStr, String managerEmail);
    Page<LeaveRequestDto> getEmployeeLeaves(Long employeeId, Pageable pageable);
    Page<LeaveRequestDto> getTeamLeaves(String managerEmail, Pageable pageable);
    Page<LeaveRequestDto> getAllLeaves(Pageable pageable);
    LeaveRequestDto getLeaveById(Long id);
}
