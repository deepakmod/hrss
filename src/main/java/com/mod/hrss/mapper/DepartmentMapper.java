package com.mod.hrss.mapper;

import com.mod.hrss.dto.response.DepartmentResponse;
import com.mod.hrss.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        if (department == null) {
            return null;
        }

        DepartmentResponse response = new DepartmentResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        response.setDescription(department.getDescription());

        if (department.getManager() != null) {
            response.setManagerId(department.getManager().getId());
            response.setManagerName(department.getManager().getFirstName() + " " + department.getManager().getLastName());
        }

        return response;
    }
}
