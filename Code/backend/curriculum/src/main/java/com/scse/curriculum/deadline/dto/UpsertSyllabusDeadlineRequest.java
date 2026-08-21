package com.scse.curriculum.deadline.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertSyllabusDeadlineRequest {

    @NotBlank(message = "Vui lòng nhập năm học")
    private String academicYear;

    @NotNull(message = "Vui lòng chọn học kỳ")
    @Min(value = 1, message = "Học kỳ phải từ 1 đến 8")
    @Max(value = 8, message = "Học kỳ phải từ 1 đến 8")
    private Integer semester;

    @NotNull(message = "Vui lòng chọn thời điểm deadline")
    private LocalDateTime deadlineAt;

    @NotEmpty(message = "Vui lòng cấu hình ít nhất một mốc nhắc")
    private List<@Min(0) @Max(60) Integer> reminderDays;

    @NotEmpty(message = "Vui lòng cấu hình ít nhất một mốc escalation")
    private List<@Min(0) @Max(365) Integer> escalationDays;

    private Boolean active;
}
