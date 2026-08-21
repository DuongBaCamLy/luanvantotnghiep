package com.scse.curriculum.instructor.dto;

import com.scse.curriculum.user.entity.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InstructorResponse {

    private Integer id;

    private String staffCode;

    private String fullName;

    private String email;

    private String degree;

    private String academicRank;

    private Integer departmentId;

    private String departmentCode;

    private String departmentName;

    private String departmentNameVn;

    /**
     * Status of the instructor profile itself.
     * This is intentionally separate from the login account status.
     */
    private Boolean isActive;

    /**
     * Linked login-account metadata. Null means the instructor profile
     * has not been linked to a system user account yet.
     */
    private Integer userAccountId;

    private String username;

    private Boolean accountActive;

    private UserRole role;

    private Integer courseCount;

    private LocalDateTime createdAt;
}