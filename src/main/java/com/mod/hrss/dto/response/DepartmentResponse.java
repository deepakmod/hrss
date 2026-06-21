package com.mod.hrss.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentResponse {
    private Long id;
    private String name;
    private String description;
    private Long managerId;
    private String managerName;
}
