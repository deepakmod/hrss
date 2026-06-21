package com.mod.hrss.mapper;

import com.mod.hrss.dto.response.LeaveRequestDto;
import com.mod.hrss.entity.LeaveRequest;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestMapper {

    public LeaveRequestDto toDto(LeaveRequest leaveRequest) {
        if (leaveRequest == null) {
            return null;
        }

        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(leaveRequest.getId());
        dto.setLeaveType(leaveRequest.getLeaveType().name());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setReason(leaveRequest.getReason());
        dto.setApprovalStatus(leaveRequest.getApprovalStatus().name());

        if (leaveRequest.getEmployee() != null) {
            dto.setEmployeeId(leaveRequest.getEmployee().getId());
            dto.setEmployeeName(leaveRequest.getEmployee().getFirstName() + " " + leaveRequest.getEmployee().getLastName());
        }

        if (leaveRequest.getApprovedBy() != null) {
            dto.setApprovedById(leaveRequest.getApprovedBy().getId());
            dto.setApprovedByName(leaveRequest.getApprovedBy().getFirstName() + " " + leaveRequest.getApprovedBy().getLastName());
        }

        return dto;
    }
}
