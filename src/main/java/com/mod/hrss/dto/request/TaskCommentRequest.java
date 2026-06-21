package com.mod.hrss.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCommentRequest {

    @NotBlank(message = "Comment cannot be empty")
    private String comment;
}
