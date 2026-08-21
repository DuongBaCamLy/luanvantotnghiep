package com.scse.curriculum.syllabus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-03.2: thông tin học kỳ đích khi nhân bản đề cương.
 *
 * Faculty chỉ cần gửi classSectionId. Backend lấy course, academicYear,
 * semester và người tạo từ assignment/JWT. Admin có thể không chọn
 * assignment nhưng khi đó phải gửi academicYear và semester.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloneSyllabusRequest {

    private Integer classSectionId;

    private Integer cohortId;

    private String academicYear;

    private String semester;

    private String changeSummary;

    /** Backward-compatible constructor for assignment-based instructor cloning. */
    public CloneSyllabusRequest(
            Integer classSectionId,
            String academicYear,
            String semester,
            String changeSummary) {
        this.classSectionId = classSectionId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.changeSummary = changeSummary;
    }
}
