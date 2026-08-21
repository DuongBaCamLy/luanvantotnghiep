package com.scse.curriculum.dashboard.dto;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DashboardAdminResponse {
    private LocalDateTime generatedAt;
    private String dataSource;
    private String academicYear;
    private Integer semester;
    private List<TermOption> terms = new ArrayList<>();

    private long totalUsers;
    private long totalPrograms;
    private long totalCourses;
    private long totalDepartments;
    private long totalSyllabuses;
    private long totalInstructors;
    private long totalAssignedSections;
    private long totalAssignedCourses;
    private long submittedAssignments;
    private long notSubmittedAssignments;
    private long overdueAssignments;
private long approvedAssignments;
private double approvalRate;
private Map<String, Long> syllabusStatusOverview;
    private Long deadlineId;
    private LocalDateTime deadlineAt;
    private Boolean deadlineConfigured;
    private Boolean deadlinePassed;

    private List<FacultyNotSubmitted> facultiesNotSubmitted = new ArrayList<>();
    private List<CourseGroupStatistic> courseGroupStatistics = new ArrayList<>();

    @Data
    public static class TermOption {
        private String academicYear;
        private Integer semester;
        private String key;
        private String label;
        private long sectionCount;
    }

    @Data
    public static class FacultyNotSubmitted {
        private Integer instructorId;
        private String staffCode;
        private String instructorName;
        private String email;
        private String departmentCode;
        private String departmentName;
        private long assignedCourseCount;
        private long missingCount;
        private long overdueCount;
        private List<MissingCourse> missingCourses = new ArrayList<>();
    }

    @Data
    public static class MissingCourse {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private int sectionCount;
        private String latestStatus;
        private Integer syllabusId;
        private String versionLabel;
        private Boolean overdue;
    }

    @Data
    public static class CourseGroupStatistic {
        private Integer courseTypeId;
        private String courseTypeCode;
        private String courseTypeName;
        private String courseTypeNameVn;
        private long assignedCourses;
        private long assignedSections;
        private long submittedCourses;
        private long notSubmittedCourses;
        private long overdueCourses;
        private double submissionRate;
    }
}
