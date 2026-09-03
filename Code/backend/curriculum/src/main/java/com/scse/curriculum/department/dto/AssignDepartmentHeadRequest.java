package com.scse.curriculum.department.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignDepartmentHeadRequest {

    @NotNull
    private Integer userAccountId;
}
