package com.scse.curriculum.program.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProgramDiffResponse {
    private Integer programId;
    private String programCode;
    private Integer oldCohortId;
    private Integer newCohortId;
    private String oldCohortYear;
    private String newCohortYear;

    private ListDiff<CourseProgramDiff> courseDiff;

    @Data
    @Builder
    public static class ListDiff<T> {
        private List<T> added;
        private List<T> removed;
        private List<T> modified;
    }

    @Data
    @Builder
    public static class CourseProgramDiff {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private Map<String, FieldDiff> changes;
    }

    @Data
    @Builder
    public static class FieldDiff {
        private String oldValue;
        private String newValue;
    }
}
