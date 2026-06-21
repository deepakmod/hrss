package com.mod.hrss.repository;

import com.mod.hrss.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Page<Attendance> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<Attendance> findFirstByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(Long employeeId);
    List<Attendance> findByEmployeeIdAndCheckInBetween(Long employeeId, LocalDateTime start, LocalDateTime end);
}
