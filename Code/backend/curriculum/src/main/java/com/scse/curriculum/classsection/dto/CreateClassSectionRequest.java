package com.scse.curriculum.classsection.dto;

import com.scse.curriculum.classsection.entity.SectionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClassSectionRequest {

    @NotNull(message = "Vui lòng chọn môn học")
    private Integer courseId;

    @NotNull(message = "Please select a program")
    private Integer programId;

    @NotNull(message = "Please select a cohort")
    private Integer cohortId;

    /**
     * FR-03.1: có thể phân công trước khi đề cương tồn tại.
     * Khi Faculty tạo Draft, backend sẽ tự gắn Draft vào phân công.
     */
    private Integer syllabusId;

    @NotNull(message = "Vui lòng chọn giảng viên")
    private Integer instructorId;

    private Integer semester;

    private String academicYear;

    private Integer groupNumber;

    private Integer labGroup;
    private Integer maxStudents;
    private String room;
    private String schedule;

    private SectionType sectionType;

    private Boolean isActive;
}
