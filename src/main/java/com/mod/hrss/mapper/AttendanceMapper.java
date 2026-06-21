package com.mod.hrss.mapper;

import com.mod.hrss.dto.response.AttendanceDto;
import com.mod.hrss.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceDto toDto(Attendance attendance) {
        if (attendance == null) {
            return null;
        }

        AttendanceDto dto = new AttendanceDto();
        dto.setId(attendance.getId());
        dto.setCheckIn(attendance.getCheckIn());
        dto.setCheckOut(attendance.getCheckOut());
        dto.setWorkHours(attendance.getWorkHours());

        if (attendance.getEmployee() != null) {
            dto.setEmployeeId(attendance.getEmployee().getId());
            dto.setEmployeeName(attendance.getEmployee().getFirstName() + " " + attendance.getEmployee().getLastName());
        }

        return dto;
    }
}
