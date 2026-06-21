package com.mod.hrss.repository;

import com.mod.hrss.entity.Task;
import com.mod.hrss.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByAssignedToId(Long assignedToId, Pageable pageable);
    Page<Task> findByAssignedById(Long assignedById, Pageable pageable);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
