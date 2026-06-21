package com.mod.hrss.service;

import com.mod.hrss.dto.request.TaskCommentRequest;
import com.mod.hrss.dto.request.TaskRequest;
import com.mod.hrss.dto.response.TaskCommentResponse;
import com.mod.hrss.dto.response.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(TaskRequest request, String assignerEmail);
    TaskResponse getTaskById(Long id);
    Page<TaskResponse> getAllTasks(Pageable pageable);
    Page<TaskResponse> getTasksAssignedTo(Long userId, Pageable pageable);
    Page<TaskResponse> getTasksAssignedBy(Long userId, Pageable pageable);
    TaskResponse updateTask(Long id, TaskRequest request);
    TaskResponse updateTaskStatus(Long id, String statusStr, String email);
    void deleteTask(Long id);
    TaskCommentResponse addComment(Long taskId, TaskCommentRequest commentRequest, String email);
    List<TaskCommentResponse> getComments(Long taskId);
}
