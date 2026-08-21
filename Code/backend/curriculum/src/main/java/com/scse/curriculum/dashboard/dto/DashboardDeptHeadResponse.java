package com.scse.curriculum.dashboard.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardDeptHeadResponse {
    private long totalCoursesInDept;
    private long syllabusesToReview;
    
    private List<CourseSyllabusStatus> coursesStatus;

    @Data
    public static class CourseSyllabusStatus {
        private String courseCode;
        private String courseName;
        private String instructorName;
        private String status;
    }
}
