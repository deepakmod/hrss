package com.mod.hrss.service.impl;

import com.mod.hrss.dto.request.TaskCommentRequest;
import com.mod.hrss.dto.request.TaskRequest;
import com.mod.hrss.dto.response.TaskCommentResponse;
import com.mod.hrss.dto.response.TaskResponse;
import com.mod.hrss.entity.*;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.TaskMapper;
import com.mod.hrss.repository.TaskCommentRepository;
import com.mod.hrss.repository.TaskRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.NotificationService;
import com.mod.hrss.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TaskResponse createTask(TaskRequest request, String assignerEmail) {
        User assigner = userRepository.findByEmail(assignerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner user not found"));

        User assignee = userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found"));

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(TaskPriority.valueOf(request.getPriority()));
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(request.getDueDate());
        task.setAssignedBy(assigner);
        task.setAssignedTo(assignee);

        Task saved = taskRepository.save(task);
        log.info("Task '{}' assigned by {} to {}", task.getTitle(), assigner.getEmail(), assignee.getEmail());

        // Notify assignee
        notificationService.sendNotification(
                assignee.getId(),
                "New Task Assigned",
                "You have been assigned a new task: " + task.getTitle() + " by " + assigner.getFirstName()
        );

        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksAssignedTo(Long userId, Pageable pageable) {
        return taskRepository.findByAssignedToId(userId, pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksAssignedBy(Long userId, Pageable pageable) {
        return taskRepository.findByAssignedById(userId, pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(TaskPriority.valueOf(request.getPriority()));
        task.setDueDate(request.getDueDate());

        if (request.getAssignedToId() != null && !request.getAssignedToId().equals(task.getAssignedTo().getId())) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee user not found"));
            task.setAssignedTo(assignee);
            
            notificationService.sendNotification(
                    assignee.getId(),
                    "Task Assignment Update",
                    "A task has been reassigned to you: " + task.getTitle()
            );
        }

        Task updated = taskRepository.save(task);
        return taskMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, String statusStr, String email) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only assignee, assigner, or ADMIN can update task status
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        boolean isAssigner = task.getAssignedBy().getId().equals(currentUser.getId());
        boolean isAssignee = task.getAssignedTo().getId().equals(currentUser.getId());

        if (!isAdmin && !isAssigner && !isAssignee) {
            throw new AccessDeniedException("Not authorized to update this task status");
        }

        TaskStatus newStatus = TaskStatus.valueOf(statusStr);
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }

        Task updated = taskRepository.save(task);
        log.info("Task '{}' status updated to {}", task.getTitle(), newStatus);

        // Notify Assigner if Assignee updates status
        if (isAssignee && !isAssigner) {
            notificationService.sendNotification(
                    task.getAssignedBy().getId(),
                    "Task Status Updated",
                    currentUser.getFirstName() + " marked task '" + task.getTitle() + "' as " + newStatus.name()
            );
        }

        return taskMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        taskRepository.delete(task);
        log.info("Task with id {} soft-deleted", id);
    }

    @Override
    @Transactional
    public TaskCommentResponse addComment(Long taskId, TaskCommentRequest commentRequest, String email) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setComment(commentRequest.getComment());
        TaskComment saved = taskCommentRepository.save(comment);

        // Notify other party
        Long notifyUserId = user.getId().equals(task.getAssignedTo().getId()) 
                ? task.getAssignedBy().getId() 
                : task.getAssignedTo().getId();

        notificationService.sendNotification(
                notifyUserId,
                "New Task Comment",
                user.getFirstName() + " commented on task '" + task.getTitle() + "'"
        );

        return taskMapper.toCommentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResponse> getComments(Long taskId) {
        // Verify task exists
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(taskMapper::toCommentResponse)
                .collect(Collectors.toList());
    }
}
