package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.request.LeaveApplyRequest;
import com.mod.hrss.dto.request.LeaveReviewRequest;
import com.mod.hrss.dto.response.LeaveRequestDto;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> applyLeave(
            @Valid @RequestBody LeaveApplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeaveRequestDto response = leaveService.applyLeave(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Leave application submitted successfully", response));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> reviewLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveReviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeaveRequestDto response = leaveService.reviewLeave(id, request.getStatus(), principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Leave request reviewed successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> getLeaveById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        LeaveRequestDto response = leaveService.getLeaveById(id);
        
        // Security check: must be employee of the leave request or manager/admin
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        boolean isOwner = response.getEmployeeId().equals(principal.getId());

        if (!isAdminOrManager && !isOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success("Leave request retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<LeaveRequestDto>>> getLeaves(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeaveRequestDto> leaves;
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isManager = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));

        if (employeeId != null) {
            // Verify access: Owner or Manager/Admin
            boolean isOwner = employeeId.equals(principal.getId());
            if (!isAdmin && !isManager && !isOwner) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            leaves = leaveService.getEmployeeLeaves(employeeId, pageable);
        } else {
            if (isAdmin) {
                leaves = leaveService.getAllLeaves(pageable);
            } else if (isManager) {
                leaves = leaveService.getTeamLeaves(principal.getUsername(), pageable);
            } else {
                leaves = leaveService.getEmployeeLeaves(principal.getId(), pageable);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved successfully", leaves));
    }
}
