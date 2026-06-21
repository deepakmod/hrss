package com.mod.hrss.mapper;

import com.mod.hrss.dto.response.TaskCommentResponse;
import com.mod.hrss.dto.response.TaskResponse;
import com.mod.hrss.entity.Task;
import com.mod.hrss.entity.TaskComment;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority().name());
        response.setStatus(task.getStatus().name());
        response.setDueDate(task.getDueDate());
        response.setCompletedAt(task.getCompletedAt());
        response.setVersion(task.getVersion());

        if (task.getAssignedBy() != null) {
            response.setAssignedById(task.getAssignedBy().getId());
            response.setAssignedByName(task.getAssignedBy().getFirstName() + " " + task.getAssignedBy().getLastName());
        }

        if (task.getAssignedTo() != null) {
            response.setAssignedToId(task.getAssignedTo().getId());
            response.setAssignedToName(task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName());
        }

        return response;
    }

    public TaskCommentResponse toCommentResponse(TaskComment comment) {
        if (comment == null) {
            return null;
        }

        TaskCommentResponse response = new TaskCommentResponse();
        response.setId(comment.getId());
        response.setComment(comment.getComment());
        response.setCreatedAt(comment.getCreatedAt());

        if (comment.getUser() != null) {
            response.setUsername(comment.getUser().getFirstName() + " " + comment.getUser().getLastName());
        }

        return response;
    }
}
