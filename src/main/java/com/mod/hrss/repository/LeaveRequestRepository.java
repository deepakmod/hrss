package com.mod.hrss.repository;

import com.mod.hrss.entity.LeaveRequest;
import com.mod.hrss.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<LeaveRequest> findByApprovalStatus(LeaveStatus approvalStatus, Pageable pageable);
    Page<LeaveRequest> findByEmployeeManagerId(Long managerId, Pageable pageable);
}
