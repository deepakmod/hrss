package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.PayrollCreateRequest;
import com.mod.hrss.dto.response.PayrollDto;
import com.mod.hrss.entity.Payroll;
import com.mod.hrss.entity.User;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.PayrollMapper;
import com.mod.hrss.repository.PayrollRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.NotificationService;
import com.mod.hrss.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final UserRepository userRepository;
    private final PayrollMapper payrollMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PayrollDto createPayroll(PayrollCreateRequest request) {
        User employee = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Check if payroll already exists for the employee in the given month
        payrollRepository.findByEmployeeIdAndPayMonth(request.getEmployeeId(), request.getPayMonth())
                .ifPresent(p -> {
                    throw new BusinessException("Payroll already generated for employee in " + request.getPayMonth());
                });

        BigDecimal basic = request.getBasicSalary();
        BigDecimal allowances = request.getAllowances() != null ? request.getAllowances() : BigDecimal.ZERO;
        BigDecimal deductions = request.getDeductions() != null ? request.getDeductions() : BigDecimal.ZERO;

        BigDecimal netSalary = basic.add(allowances).subtract(deductions);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Net salary cannot be negative");
        }

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setBasicSalary(basic);
        payroll.setAllowances(allowances);
        payroll.setDeductions(deductions);
        payroll.setNetSalary(netSalary);
        payroll.setPayMonth(request.getPayMonth());

        Payroll saved = payrollRepository.save(payroll);
        log.info("Payroll generated for employee {} for month {}", employee.getEmail(), request.getPayMonth());

        // Notify employee
        notificationService.sendNotification(
                employee.getId(),
                "Payslip Generated",
                "Your payslip for month " + request.getPayMonth() + " has been generated. Net Salary: $" + netSalary
        );

        return payrollMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollDto> getEmployeePayrolls(Long employeeId, Pageable pageable) {
        return payrollRepository.findByEmployeeId(employeeId, pageable).map(payrollMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDto getPayrollById(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found with id: " + id));
        return payrollMapper.toDto(payroll);
    }
}
