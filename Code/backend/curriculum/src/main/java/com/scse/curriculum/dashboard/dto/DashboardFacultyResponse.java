package com.scse.curriculum.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Data;

/**
 * FR-06.3 - Dashboard giảng viên.
 *
 * Dữ liệu môn học luôn xuất phát từ ClassSection đang hoạt động. Deadline được
 * lấy từ cấu hình SyllabusDeadline đúng năm học + học kỳ, không dùng dữ liệu
 * giả hoặc chuỗi "TBD".
 */
@Data
public class DashboardFacultyResponse {

    private Integer facultyUserId;
    private Integer instructorId;
    private String instructorName;
    private String staffCode;
    private String departmentCode;
    private String departmentName;

    private String timeZone;
    private OffsetDateTime generatedAt;
    private String defaultTermKey;

    private long assignedCourses;
    private long assignedSections;
    private long completedSyllabuses;
    private long pendingSyllabuses;
    private long inProgressSyllabuses;
    private long actionRequiredCourses;
    private long overdueCourses;
    private long dueSoonCourses;
    private long unconfiguredDeadlineCourses;

    private List<TermSummary> terms;
    private List<CourseAssignment> upcomingDeadlines;

    @Data
    public static class TermSummary {
        private String key;
        private String academicYear;
        private Integer semester;
        private long courseCount;
        private long sectionCount;
        private Long deadlineId;
        private Integer deadlineRevision;
        private OffsetDateTime deadline;
        private boolean deadlineConfigured;
        private long actionRequiredCount;
        private long overdueCount;
        private long dueSoonCount;
    }

    @Data
    public static class CourseAssignment {
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private Integer totalCredits;
        private String departmentCode;
        private String departmentName;

        private String termKey;
        private String academicYear;
        private Integer semester;
        private Integer primaryClassSectionId;
        private List<Integer> classSectionIds;
        private Integer sectionCount;
        private List<Integer> groupNumbers;
        private List<String> sectionTypes;
        private List<String> rooms;
        private List<String> schedules;

        private Integer syllabusId;
        private Integer syllabusVersionNumber;
        private String syllabusVersionLabel;
        private String status;
        private Boolean currentVersion;

        private Long deadlineId;
        private Integer deadlineRevision;
        private OffsetDateTime deadline;
        private Long daysRemaining;
        private Long minutesRemaining;
        private String deadlineState;

        private boolean actionRequired;
        private String recommendedAction;
        private String dataQualityState;
    }
}
