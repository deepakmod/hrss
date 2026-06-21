package com.mod.hrss.repository;

import com.mod.hrss.entity.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    Page<Payroll> findByEmployeeId(Long employeeId, Pageable pageable);
    Optional<Payroll> findByEmployeeIdAndPayMonth(Long employeeId, String payMonth);
}
