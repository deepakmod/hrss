package com.mod.hrss.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskCommentResponse {
    private Long id;
    private String comment;
    private String username;
    private LocalDateTime createdAt;
}
