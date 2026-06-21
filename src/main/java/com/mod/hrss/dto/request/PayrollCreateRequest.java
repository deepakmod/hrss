package com.mod.hrss.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayrollCreateRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Basic salary must be greater than zero")
    private BigDecimal basicSalary;

    private BigDecimal allowances = BigDecimal.ZERO;

    private BigDecimal deductions = BigDecimal.ZERO;

    @NotBlank(message = "Pay month is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Pay month must match YYYY-MM format")
    private String payMonth;
}
