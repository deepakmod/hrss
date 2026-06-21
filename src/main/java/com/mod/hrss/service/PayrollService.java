package com.mod.hrss.service;

import com.mod.hrss.dto.request.PayrollCreateRequest;
import com.mod.hrss.dto.response.PayrollDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PayrollService {
    PayrollDto createPayroll(PayrollCreateRequest request);
    Page<PayrollDto> getEmployeePayrolls(Long employeeId, Pageable pageable);
    PayrollDto getPayrollById(Long id);
}
