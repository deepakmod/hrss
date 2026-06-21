package com.mod.hrss.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveReviewRequest {

    @NotBlank(message = "Status is required")
    private String status; // APPROVED, REJECTED
}
