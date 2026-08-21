package com.scse.curriculum.classsection.dto;

import com.scse.curriculum.classsection.entity.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateClassSectionRequest {

    @NotNull(message = "Vui lòng chọn môn học")
    private Integer courseId;

    /**
     * FR-03.1: có thể phân công trước khi đề cương tồn tại.
     * Khi Faculty tạo Draft, backend sẽ tự gắn Draft vào phân công.
     */
    private Integer syllabusId;

    @NotNull(message = "Vui lòng chọn giảng viên")
    private Integer instructorId;

    @NotNull(message = "Vui lòng chọn học kỳ")
    @Positive(message = "Học kỳ phải lớn hơn 0")
    private Integer semester;

    @NotBlank(message = "Vui lòng nhập năm học/khóa áp dụng")
    private String academicYear;

    @NotNull(message = "Vui lòng nhập mã nhóm")
    @Positive(message = "Mã nhóm phải lớn hơn 0")
    private Integer groupNumber;

    private Integer labGroup;
    private Integer maxStudents;
    private String room;
    private String schedule;

    @NotNull(message = "Vui lòng chọn loại lớp")
    private SectionType sectionType;

    private Boolean isActive;
}
