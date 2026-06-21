package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.request.PayrollCreateRequest;
import com.mod.hrss.dto.response.PayrollDto;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.service.PayrollService;
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
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollDto>> createPayroll(@Valid @RequestBody PayrollCreateRequest request) {
        PayrollDto response = payrollService.createPayroll(request);
        return ResponseEntity.ok(ApiResponse.success("Payroll generated successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PayrollDto>> getPayrollById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        PayrollDto response = payrollService.getPayrollById(id);

        // Security check: Must be owner or admin
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = response.getEmployeeId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success("Payroll record retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PayrollDto>>> getPayrolls(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "payMonth") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Long targetEmployeeId = employeeId != null ? employeeId : principal.getId();

        // Security check: Must be owner or admin
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = targetEmployeeId.equals(principal.getId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        Page<PayrollDto> payrolls = payrollService.getEmployeePayrolls(targetEmployeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payroll records retrieved successfully", payrolls));
    }
}
