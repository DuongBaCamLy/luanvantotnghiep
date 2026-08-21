package com.scse.curriculum.program.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CloneProgramRequest {

    @NotNull
    private Integer sourceCohortId;

    @NotNull
    private Integer targetCohortId;

    /**
     * false: giữ các môn đã có ở cohort mới và chỉ copy môn còn thiếu.
     * true: xóa toàn bộ course_program của cohort mới rồi copy lại từ cohort nguồn.
     */
    private Boolean overwriteExisting = false;
}
