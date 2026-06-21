package com.mod.hrss.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private Set<String> roles;
}
