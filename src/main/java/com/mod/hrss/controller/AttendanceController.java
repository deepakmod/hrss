package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.response.AttendanceDto;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkIn(@AuthenticationPrincipal UserPrincipal principal) {
        AttendanceDto dto = attendanceService.checkIn(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Checked in successfully", dto));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceDto>> checkOut(@AuthenticationPrincipal UserPrincipal principal) {
        AttendanceDto dto = attendanceService.checkOut(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AttendanceDto>>> getAttendance(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "checkIn") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long targetEmployeeId = employeeId != null ? employeeId : principal.getId();

        // Security check: Must be owner or manager/admin
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        boolean isOwner = targetEmployeeId.equals(principal.getId());

        if (!isAdminOrManager && !isOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        Page<AttendanceDto> attendance = attendanceService.getEmployeeAttendance(targetEmployeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Attendance records retrieved successfully", attendance));
    }

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getMonthlyReport(
            @RequestParam(required = false) Long employeeId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long targetEmployeeId = employeeId != null ? employeeId : principal.getId();

        // Security check: Must be owner or manager/admin
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        boolean isOwner = targetEmployeeId.equals(principal.getId());

        if (!isAdminOrManager && !isOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        List<AttendanceDto> report = attendanceService.getMonthlyAttendanceReport(targetEmployeeId, year, month);
        return ResponseEntity.ok(ApiResponse.success("Monthly attendance report retrieved successfully", report));
    }
}
