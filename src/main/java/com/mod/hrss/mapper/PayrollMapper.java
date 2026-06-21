package com.mod.hrss.mapper;

import com.mod.hrss.dto.response.PayrollDto;
import com.mod.hrss.entity.Payroll;
import org.springframework.stereotype.Component;

@Component
public class PayrollMapper {

    public PayrollDto toDto(Payroll payroll) {
        if (payroll == null) {
            return null;
        }

        PayrollDto dto = new PayrollDto();
        dto.setId(payroll.getId());
        dto.setBasicSalary(payroll.getBasicSalary());
        dto.setAllowances(payroll.getAllowances());
        dto.setDeductions(payroll.getDeductions());
        dto.setNetSalary(payroll.getNetSalary());
        dto.setPayMonth(payroll.getPayMonth());

        if (payroll.getEmployee() != null) {
            dto.setEmployeeId(payroll.getEmployee().getId());
            dto.setEmployeeName(payroll.getEmployee().getFirstName() + " " + payroll.getEmployee().getLastName());
        }

        return dto;
    }
}
