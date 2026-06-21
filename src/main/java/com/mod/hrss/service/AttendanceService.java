package com.mod.hrss.service;

import com.mod.hrss.dto.response.AttendanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AttendanceService {
    AttendanceDto checkIn(String email);
    AttendanceDto checkOut(String email);
    Page<AttendanceDto> getEmployeeAttendance(Long employeeId, Pageable pageable);
    List<AttendanceDto> getMonthlyAttendanceReport(Long employeeId, int year, int month);
}
