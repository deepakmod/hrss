package com.mod.hrss.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Long assignedById;
    private String assignedByName;
    private Long assignedToId;
    private String assignedToName;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private Integer version;
}
