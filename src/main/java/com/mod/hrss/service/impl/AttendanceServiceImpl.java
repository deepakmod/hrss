package com.mod.hrss.service.impl;

import com.mod.hrss.dto.response.AttendanceDto;
import com.mod.hrss.entity.Attendance;
import com.mod.hrss.entity.User;
import com.mod.hrss.exception.BusinessException;
import com.mod.hrss.exception.ResourceNotFoundException;
import com.mod.hrss.mapper.AttendanceMapper;
import com.mod.hrss.repository.AttendanceRepository;
import com.mod.hrss.repository.UserRepository;
import com.mod.hrss.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    @Transactional
    public AttendanceDto checkIn(String email) {
        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Optional<Attendance> activeAttendance = attendanceRepository
                .findFirstByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(employee.getId());

        if (activeAttendance.isPresent()) {
            throw new BusinessException("User is already checked in");
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setCheckIn(LocalDateTime.now());

        Attendance saved = attendanceRepository.save(attendance);
        log.info("User {} checked in at {}", email, saved.getCheckIn());
        return attendanceMapper.toDto(saved);
    }

    @Override
    @Transactional
    public AttendanceDto checkOut(String email) {
        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Attendance activeAttendance = attendanceRepository
                .findFirstByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(employee.getId())
                .orElseThrow(() -> new BusinessException("No active check-in found for user"));

        activeAttendance.setCheckOut(LocalDateTime.now());
        
        // Calculate work hours
        Duration duration = Duration.between(activeAttendance.getCheckIn(), activeAttendance.getCheckOut());
        double hours = duration.toSeconds() / 3600.0;
        // round to 2 decimal places
        hours = Math.round(hours * 100.0) / 100.0;
        activeAttendance.setWorkHours(hours);

        Attendance saved = attendanceRepository.save(activeAttendance);
        log.info("User {} checked out at {}. Work hours: {}", email, saved.getCheckOut(), saved.getWorkHours());
        return attendanceMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceDto> getEmployeeAttendance(Long employeeId, Pageable pageable) {
        return attendanceRepository.findByEmployeeId(employeeId, pageable).map(attendanceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getMonthlyAttendanceReport(Long employeeId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        return attendanceRepository.findByEmployeeIdAndCheckInBetween(employeeId, start, end).stream()
                .map(attendanceMapper::toDto)
                .collect(Collectors.toList());
    }
}
