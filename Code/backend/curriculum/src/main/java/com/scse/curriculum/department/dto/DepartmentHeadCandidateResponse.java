package com.scse.curriculum.department.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartmentHeadCandidateResponse {

    private Integer userAccountId;
    private String username;
    private Integer instructorId;
    private String staffCode;
    private String fullName;
    private Integer departmentId;
    private String departmentName;
}
