package com.mod.hrss.controller;

import com.mod.hrss.common.ApiResponse;
import com.mod.hrss.dto.request.TaskCommentRequest;
import com.mod.hrss.dto.request.TaskRequest;
import com.mod.hrss.dto.response.TaskCommentResponse;
import com.mod.hrss.dto.response.TaskResponse;
import com.mod.hrss.security.UserPrincipal;
import com.mod.hrss.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.createTask(request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Task created and assigned successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.getTaskById(id);
        
        // Security check: Only assignee, assigner, or ADMIN/MANAGER can view
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        boolean isRelated = response.getAssignedToId().equals(principal.getId()) || response.getAssignedById().equals(principal.getId());

        if (!isAdminOrManager && !isRelated) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasks(
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long assignedById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponse> tasks;
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        if (assignedToId != null) {
            // Non-admin/manager can only view their own tasks
            if (!isAdminOrManager && !assignedToId.equals(principal.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            tasks = taskService.getTasksAssignedTo(assignedToId, pageable);
        } else if (assignedById != null) {
            if (!isAdminOrManager && !assignedById.equals(principal.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            tasks = taskService.getTasksAssignedBy(assignedById, pageable);
        } else {
            if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                tasks = taskService.getAllTasks(pageable);
            } else {
                tasks = taskService.getTasksAssignedTo(principal.getId(), pageable);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", tasks));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.updateTaskStatus(id, status, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully"));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody TaskCommentRequest commentRequest,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskCommentResponse response = taskService.addComment(id, commentRequest, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Comment added successfully", response));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        // Security check: Verify user is related to task or is admin/manager
        TaskResponse task = taskService.getTaskById(id);
        boolean isAdminOrManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
        boolean isRelated = task.getAssignedToId().equals(principal.getId()) || task.getAssignedById().equals(principal.getId());

        if (!isAdminOrManager && !isRelated) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        List<TaskCommentResponse> comments = taskService.getComments(id);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
    }
}
