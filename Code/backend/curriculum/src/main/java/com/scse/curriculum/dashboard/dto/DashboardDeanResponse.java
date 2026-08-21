package com.scse.curriculum.dashboard.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read model cho FR-06.1.
 *
 * Mỗi môn trong CTĐT hiệu lực chỉ xuất hiện đúng một lần, vì vậy các phiên
 * bản Syllabus lịch sử không làm sai tỷ lệ phê duyệt.
 */
public class DashboardDeanResponse {
    private OffsetDateTime generatedAt;
    private String timeZone;
    private String dataSource;
    private SelectedScope scope;
    private List<MajorOption> majors = new ArrayList<>();
    private List<CohortOption> cohorts = new ArrayList<>();
    private List<SemesterOption> semesters = new ArrayList<>();
    private Summary summary;
    private List<StatusSlice> statusDistribution = new ArrayList<>();
    private List<SemesterProgress> semesterProgress = new ArrayList<>();
    private List<CourseProgress> courses = new ArrayList<>();
    private List<DataWarning> warnings = new ArrayList<>();

    public OffsetDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public SelectedScope getScope() { return scope; }
    public void setScope(SelectedScope scope) { this.scope = scope; }
    public List<MajorOption> getMajors() { return majors; }
    public void setMajors(List<MajorOption> majors) { this.majors = majors; }
    public List<CohortOption> getCohorts() { return cohorts; }
    public void setCohorts(List<CohortOption> cohorts) { this.cohorts = cohorts; }
    public List<SemesterOption> getSemesters() { return semesters; }
    public void setSemesters(List<SemesterOption> semesters) { this.semesters = semesters; }
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<StatusSlice> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(List<StatusSlice> statusDistribution) { this.statusDistribution = statusDistribution; }
    public List<SemesterProgress> getSemesterProgress() { return semesterProgress; }
    public void setSemesterProgress(List<SemesterProgress> semesterProgress) { this.semesterProgress = semesterProgress; }
    public List<CourseProgress> getCourses() { return courses; }
    public void setCourses(List<CourseProgress> courses) { this.courses = courses; }
    public List<DataWarning> getWarnings() { return warnings; }
    public void setWarnings(List<DataWarning> warnings) { this.warnings = warnings; }

    public static class SelectedScope {
        private Integer majorId;
        private String majorCode;
        private String majorName;
        private String majorNameVn;
        private Integer programId;
        private String programCode;
        private String programName;
        private Integer cohortId;
        private String cohortName;
        private Integer cohortEntryYear;
        private Integer semester;
        private String semesterLabel;

        public Integer getMajorId() { return majorId; }
        public void setMajorId(Integer majorId) { this.majorId = majorId; }
        public String getMajorCode() { return majorCode; }
        public void setMajorCode(String majorCode) { this.majorCode = majorCode; }
        public String getMajorName() { return majorName; }
        public void setMajorName(String majorName) { this.majorName = majorName; }
        public String getMajorNameVn() { return majorNameVn; }
        public void setMajorNameVn(String majorNameVn) { this.majorNameVn = majorNameVn; }
        public Integer getProgramId() { return programId; }
        public void setProgramId(Integer programId) { this.programId = programId; }
        public String getProgramCode() { return programCode; }
        public void setProgramCode(String programCode) { this.programCode = programCode; }
        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }
        public Integer getCohortId() { return cohortId; }
        public void setCohortId(Integer cohortId) { this.cohortId = cohortId; }
        public String getCohortName() { return cohortName; }
        public void setCohortName(String cohortName) { this.cohortName = cohortName; }
        public Integer getCohortEntryYear() { return cohortEntryYear; }
        public void setCohortEntryYear(Integer cohortEntryYear) { this.cohortEntryYear = cohortEntryYear; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public String getSemesterLabel() { return semesterLabel; }
        public void setSemesterLabel(String semesterLabel) { this.semesterLabel = semesterLabel; }
    }

    public static class MajorOption {
        private Integer id;
        private String code;
        private String name;
        private String nameVn;
        private long activeCohortCount;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getNameVn() { return nameVn; }
        public void setNameVn(String nameVn) { this.nameVn = nameVn; }
        public long getActiveCohortCount() { return activeCohortCount; }
        public void setActiveCohortCount(long activeCohortCount) { this.activeCohortCount = activeCohortCount; }
    }

    public static class CohortOption {
        private Integer id;
        private Integer majorId;
        private String majorCode;
        private Integer programId;
        private String programCode;
        private String programName;
        private Integer entryYear;
        private String name;
        private Boolean active;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer getMajorId() { return majorId; }
        public void setMajorId(Integer majorId) { this.majorId = majorId; }
        public String getMajorCode() { return majorCode; }
        public void setMajorCode(String majorCode) { this.majorCode = majorCode; }
        public Integer getProgramId() { return programId; }
        public void setProgramId(Integer programId) { this.programId = programId; }
        public String getProgramCode() { return programCode; }
        public void setProgramCode(String programCode) { this.programCode = programCode; }
        public String getProgramName() { return programName; }
        public void setProgramName(String programName) { this.programName = programName; }
        public Integer getEntryYear() { return entryYear; }
        public void setEntryYear(Integer entryYear) { this.entryYear = entryYear; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class SemesterOption {
        private Integer value;
        private String label;
        private long courseCount;

        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getCourseCount() { return courseCount; }
        public void setCourseCount(long courseCount) { this.courseCount = courseCount; }
    }

    public static class Summary {
        private long expectedCourses;
        private long createdSyllabuses;
        private long approvedSyllabuses;
        private long notApprovedSyllabuses;
        private long missingSyllabuses;
        private long draftSyllabuses;
        private long submittedSyllabuses;
        private long underReviewSyllabuses;
        private long revisionRequestedSyllabuses;
        private long rejectedSyllabuses;
        private long archivedSyllabuses;
        private long pendingReviewSyllabuses;
        private long actionRequiredCourses;
        private long expectedCredits;
        private long approvedCredits;
        private double approvalRate;
        private double creationRate;

        public long getExpectedCourses() { return expectedCourses; }
        public void setExpectedCourses(long expectedCourses) { this.expectedCourses = expectedCourses; }
        public long getCreatedSyllabuses() { return createdSyllabuses; }
        public void setCreatedSyllabuses(long createdSyllabuses) { this.createdSyllabuses = createdSyllabuses; }
        public long getApprovedSyllabuses() { return approvedSyllabuses; }
        public void setApprovedSyllabuses(long approvedSyllabuses) { this.approvedSyllabuses = approvedSyllabuses; }
        public long getNotApprovedSyllabuses() { return notApprovedSyllabuses; }
        public void setNotApprovedSyllabuses(long notApprovedSyllabuses) { this.notApprovedSyllabuses = notApprovedSyllabuses; }
        public long getMissingSyllabuses() { return missingSyllabuses; }
        public void setMissingSyllabuses(long missingSyllabuses) { this.missingSyllabuses = missingSyllabuses; }
        public long getDraftSyllabuses() { return draftSyllabuses; }
        public void setDraftSyllabuses(long draftSyllabuses) { this.draftSyllabuses = draftSyllabuses; }
        public long getSubmittedSyllabuses() { return submittedSyllabuses; }
        public void setSubmittedSyllabuses(long submittedSyllabuses) { this.submittedSyllabuses = submittedSyllabuses; }
        public long getUnderReviewSyllabuses() { return underReviewSyllabuses; }
        public void setUnderReviewSyllabuses(long underReviewSyllabuses) { this.underReviewSyllabuses = underReviewSyllabuses; }
        public long getRevisionRequestedSyllabuses() { return revisionRequestedSyllabuses; }
        public void setRevisionRequestedSyllabuses(long revisionRequestedSyllabuses) { this.revisionRequestedSyllabuses = revisionRequestedSyllabuses; }
        public long getRejectedSyllabuses() { return rejectedSyllabuses; }
        public void setRejectedSyllabuses(long rejectedSyllabuses) { this.rejectedSyllabuses = rejectedSyllabuses; }
        public long getArchivedSyllabuses() { return archivedSyllabuses; }
        public void setArchivedSyllabuses(long archivedSyllabuses) { this.archivedSyllabuses = archivedSyllabuses; }
        public long getPendingReviewSyllabuses() { return pendingReviewSyllabuses; }
        public void setPendingReviewSyllabuses(long pendingReviewSyllabuses) { this.pendingReviewSyllabuses = pendingReviewSyllabuses; }
        public long getActionRequiredCourses() { return actionRequiredCourses; }
        public void setActionRequiredCourses(long actionRequiredCourses) { this.actionRequiredCourses = actionRequiredCourses; }
        public long getExpectedCredits() { return expectedCredits; }
        public void setExpectedCredits(long expectedCredits) { this.expectedCredits = expectedCredits; }
        public long getApprovedCredits() { return approvedCredits; }
        public void setApprovedCredits(long approvedCredits) { this.approvedCredits = approvedCredits; }
        public double getApprovalRate() { return approvalRate; }
        public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }
        public double getCreationRate() { return creationRate; }
        public void setCreationRate(double creationRate) { this.creationRate = creationRate; }
    }

    public static class StatusSlice {
        private String key;
        private String label;
        private long count;
        private double percentage;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
    }

    public static class SemesterProgress {
        private Integer semester;
        private String label;
        private long expected;
        private long approved;
        private long notApproved;
        private long missing;
        private double approvalRate;

        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getExpected() { return expected; }
        public void setExpected(long expected) { this.expected = expected; }
        public long getApproved() { return approved; }
        public void setApproved(long approved) { this.approved = approved; }
        public long getNotApproved() { return notApproved; }
        public void setNotApproved(long notApproved) { this.notApproved = notApproved; }
        public long getMissing() { return missing; }
        public void setMissing(long missing) { this.missing = missing; }
        public double getApprovalRate() { return approvalRate; }
        public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }
    }

    public static class CourseProgress {
        private Integer courseProgramId;
        private Integer courseId;
        private String courseCode;
        private String courseName;
        private String courseNameVn;
        private Integer semester;
        private String semesterLabel;
        private Integer yearSuggest;
        private String courseType;
        private Boolean required;
        private Integer credits;
        private Integer syllabusId;
        private Integer versionNumber;
        private String versionLabel;
        private String status;
        private String statusLabel;
        private String preparedBy;
        private OffsetDateTime submittedAt;
        private OffsetDateTime approvedAt;
        private OffsetDateTime lastUpdatedAt;
        private Boolean explicitCurriculumLink;
        private String dataQualityState;

        public Integer getCourseProgramId() { return courseProgramId; }
        public void setCourseProgramId(Integer courseProgramId) { this.courseProgramId = courseProgramId; }
        public Integer getCourseId() { return courseId; }
        public void setCourseId(Integer courseId) { this.courseId = courseId; }
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        public String getCourseNameVn() { return courseNameVn; }
        public void setCourseNameVn(String courseNameVn) { this.courseNameVn = courseNameVn; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public String getSemesterLabel() { return semesterLabel; }
        public void setSemesterLabel(String semesterLabel) { this.semesterLabel = semesterLabel; }
        public Integer getYearSuggest() { return yearSuggest; }
        public void setYearSuggest(Integer yearSuggest) { this.yearSuggest = yearSuggest; }
        public String getCourseType() { return courseType; }
        public void setCourseType(String courseType) { this.courseType = courseType; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
        public Integer getCredits() { return credits; }
        public void setCredits(Integer credits) { this.credits = credits; }
        public Integer getSyllabusId() { return syllabusId; }
        public void setSyllabusId(Integer syllabusId) { this.syllabusId = syllabusId; }
        public Integer getVersionNumber() { return versionNumber; }
        public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
        public String getVersionLabel() { return versionLabel; }
        public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public String getPreparedBy() { return preparedBy; }
        public void setPreparedBy(String preparedBy) { this.preparedBy = preparedBy; }
        public OffsetDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
        public OffsetDateTime getApprovedAt() { return approvedAt; }
        public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
        public OffsetDateTime getLastUpdatedAt() { return lastUpdatedAt; }
        public void setLastUpdatedAt(OffsetDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
        public Boolean getExplicitCurriculumLink() { return explicitCurriculumLink; }
        public void setExplicitCurriculumLink(Boolean explicitCurriculumLink) { this.explicitCurriculumLink = explicitCurriculumLink; }
        public String getDataQualityState() { return dataQualityState; }
        public void setDataQualityState(String dataQualityState) { this.dataQualityState = dataQualityState; }
    }

    public static class DataWarning {
        private String severity;
        private String code;
        private String message;
        private List<String> references = new ArrayList<>();

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<String> getReferences() { return references; }
        public void setReferences(List<String> references) { this.references = references; }
    }
}
