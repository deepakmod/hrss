package com.mod.hrss.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String priority; // LOW, MEDIUM, HIGH

    private LocalDate dueDate;

    private Long assignedToId;
}
