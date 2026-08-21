import { api } from "./axios"

export interface DeanDashboardScope {
  majorId: number | null
  majorCode: string | null
  majorName: string | null
  majorNameVn: string | null
  programId: number | null
  programCode: string | null
  programName: string | null
  cohortId: number | null
  cohortName: string | null
  cohortEntryYear: number | null
  semester: number | null
  semesterLabel: string
}

export interface DeanMajorOption {
  id: number
  code: string
  name: string
  nameVn: string
  activeCohortCount: number
}

export interface DeanCohortOption {
  id: number
  majorId: number
  majorCode: string
  programId: number
  programCode: string
  programName: string
  entryYear: number
  name: string
  active: boolean
}

export interface DeanSemesterOption {
  value: number
  label: string
  courseCount: number
}

export interface DeanDashboardSummary {
  expectedCourses: number
  createdSyllabuses: number
  approvedSyllabuses: number
  notApprovedSyllabuses: number
  missingSyllabuses: number
  draftSyllabuses: number
  submittedSyllabuses: number
  underReviewSyllabuses: number
  revisionRequestedSyllabuses: number
  rejectedSyllabuses: number
  archivedSyllabuses: number
  pendingReviewSyllabuses: number
  actionRequiredCourses: number
  expectedCredits: number
  approvedCredits: number
  approvalRate: number
  syllabusStatusOverview: Record<string, number>
  creationRate: number
}

export interface DeanStatusSlice {
  key: string
  label: string
  count: number
  percentage: number
}

export interface DeanSemesterProgress {
  semester: number
  label: string
  expected: number
  approved: number
  notApproved: number
  missing: number
  approvalRate: number
}

export interface DeanCourseProgress {
  courseProgramId: number
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null
  semester: number | null
  semesterLabel: string
  yearSuggest?: number | null
  courseType?: string | null
  required: boolean
  credits: number
  syllabusId: number | null
  versionNumber: number | null
  versionLabel: string | null
  status: string
  statusLabel: string
  preparedBy?: string | null
  submittedAt?: string | null
  approvedAt?: string | null
  lastUpdatedAt?: string | null
  explicitCurriculumLink: boolean
  dataQualityState: string
}

export interface DeanDashboardWarning {
  severity: "ERROR" | "WARNING" | "INFO"
  code: string
  message: string
  references: string[]
}

export interface DashboardDeanResponse {
  generatedAt: string
  timeZone: string
  dataSource: string
  scope: DeanDashboardScope
  majors: DeanMajorOption[]
  cohorts: DeanCohortOption[]
  semesters: DeanSemesterOption[]
  summary: DeanDashboardSummary
  statusDistribution: DeanStatusSlice[]
  semesterProgress: DeanSemesterProgress[]
  courses: DeanCourseProgress[]
  warnings: DeanDashboardWarning[]
}

export interface DeanDashboardQuery {
  majorId?: number
  cohortId?: number
  semester?: number
}

export interface DashboardDeptHeadResponse {
  totalCoursesInDept: number
  syllabusesToReview: number
  coursesStatus: {
    courseCode: string
    courseName: string
    instructorName: string
    status: string
  }[]
}

export type FacultyDeadlineState =
  | "COMPLETED"
  | "SUBMITTED"
  | "IN_REVIEW"
  | "UPCOMING"
  | "DUE_SOON"
  | "DUE_TODAY"
  | "OVERDUE"
  | "NOT_CONFIGURED"

export type FacultyRecommendedAction =
  | "CREATE"
  | "EDIT"
  | "REVISE"
  | "VIEW_PROGRESS"
  | "VIEW_APPROVED"
  | "VIEW_HISTORY"

export type FacultyAssignmentDataQualityState =
  | "CONSISTENT"
  | "UNLINKED"
  | "PARTIAL_LINKAGE"
  | "MULTIPLE_LINKS"

export interface FacultyTermSummary {
  key: string
  academicYear: string
  semester: number
  courseCount: number
  sectionCount: number
  deadlineId: number | null
  deadlineRevision: number | null
  deadline: string | null
  deadlineConfigured: boolean
  actionRequiredCount: number
  overdueCount: number
  dueSoonCount: number
}

export interface FacultyCourseAssignment {
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null
  totalCredits: number
  departmentCode?: string | null
  departmentName?: string | null

  termKey: string
  academicYear: string
  semester: number
  primaryClassSectionId: number
  classSectionIds: number[]
  sectionCount: number
  groupNumbers: number[]
  sectionTypes: string[]
  rooms: string[]
  schedules: string[]

  syllabusId: number | null
  syllabusVersionNumber: number | null
  syllabusVersionLabel: string | null
  status: string
  currentVersion: boolean | null

  deadlineId: number | null
  deadlineRevision: number | null
  deadline: string | null
  daysRemaining: number | null
  minutesRemaining: number | null
  deadlineState: FacultyDeadlineState

  actionRequired: boolean
  recommendedAction: FacultyRecommendedAction
  dataQualityState: FacultyAssignmentDataQualityState
}

export interface DashboardFacultyResponse {
  facultyUserId: number
  instructorId: number
  instructorName: string
  staffCode: string
  departmentCode?: string | null
  departmentName?: string | null
  timeZone: string
  generatedAt: string
  defaultTermKey: string | null

  assignedCourses: number
  assignedSections: number
  completedSyllabuses: number
  pendingSyllabuses: number
  inProgressSyllabuses: number
  actionRequiredCourses: number
  overdueCourses: number
  dueSoonCourses: number
  unconfiguredDeadlineCourses: number

  terms: FacultyTermSummary[]
  upcomingDeadlines: FacultyCourseAssignment[]
}

export interface AdminDashboardQuery {
  academicYear?: string
  semester?: number
  programId?: number
  cohortId?: number
}
export interface AdminTermOption {
  academicYear: string
  semester: number
  key: string
  label: string
  sectionCount: number
}

export interface AdminMissingCourse {
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null
  sectionCount: number
  latestStatus: string
  syllabusId?: number | null
  versionLabel?: string | null
  overdue: boolean
}

export interface AdminFacultyNotSubmitted {
  instructorId: number
  staffCode: string
  instructorName: string
  email: string
  departmentCode?: string | null
  departmentName?: string | null
  assignedCourseCount: number
  missingCount: number
  overdueCount: number
  missingCourses: AdminMissingCourse[]
}

export interface AdminCourseGroupStatistic {
  courseTypeId?: number | null
  courseTypeCode: string
  courseTypeName: string
  courseTypeNameVn: string
  assignedCourses: number
  assignedSections: number
  submittedCourses: number
  notSubmittedCourses: number
  overdueCourses: number
  submissionRate: number
}

export interface DashboardAdminResponse {
  generatedAt: string
  dataSource: string
  academicYear?: string | null
  semester?: number | null
  terms: AdminTermOption[]
  totalUsers: number
  totalPrograms: number
  totalCourses: number
  totalDepartments: number
  totalSyllabuses: number
  totalInstructors: number
  totalAssignedSections: number
  totalAssignedCourses: number
  submittedAssignments: number
notSubmittedAssignments: number
overdueAssignments: number

approvedAssignments: number
approvalRate: number
syllabusStatusOverview: Record<string, number>
  deadlineId?: number | null
  deadlineAt?: string | null
  deadlineConfigured: boolean
  deadlinePassed: boolean
  facultiesNotSubmitted: AdminFacultyNotSubmitted[]
  courseGroupStatistics: AdminCourseGroupStatistic[]
}

export type ContributionLevel = "I" | "D" | "A"

export interface HeatmapPloColumn {
  id: number
  code: string
  description: string
  descriptionVn?: string | null
  category?: string | null
  versionNumber?: number | null
  covered: boolean
  courseCount: number
  cloCount: number
}

export interface HeatmapCellCoverage {
  ploId: number
  ploCode: string
  level: ContributionLevel | null
  mappingCount: number
  cloCodes: string[]
}

export interface HeatmapCourseCoverage {
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null
  syllabusId?: number | null
  syllabusVersion?: number | null
  syllabusVersionLabel?: string | null
  syllabusAcademicYear?: string | null
  syllabusSemester?: string | null
  explicitCurriculumLink?: boolean | null
  hasApprovedSyllabus: boolean
  totalClos: number
  mappedClos: number
  unmappedCloCodes: string[]
  coverageLevels: (ContributionLevel | null)[]
  cells: HeatmapCellCoverage[]
}

export interface HeatmapSummary {
  totalPlos: number
  coveredPlos: number
  uncoveredPlos: number
  ploCoveragePercentage: number
  totalCourses: number
  coursesWithApprovedSyllabus: number
  coursesWithoutApprovedSyllabus: number
  totalClos: number
  mappedClos: number
  unmappedClos: number
  approvedSyllabusPercentage: number
  cloMappingPercentage: number
  errorCount: number
  warningCount: number
  infoCount: number
}

export interface HeatmapWarning {
  severity: "ERROR" | "WARNING" | "INFO"
  code: string
  message: string
  references: string[]
}

export interface HeatmapQuery {
  cohortId: number
  academicYear: string
  semester: string
  courseTypeId?: number
}

export interface DashboardHeatmapResponse {
  programId: number
  programCode: string
  programName: string
  programNameVn?: string | null
  cohortId: number
  cohortName: string
  cohortEntryYear: number
  academicYear: string
  semester: string
  courseTypeId?: number | null
  courseTypeCode?: string | null
  courseTypeName?: string | null
  courseTypeNameVn?: string | null
  dataSource: string
  scopeKey: string
  plos: string[]
  ploDetails: HeatmapPloColumn[]
  courseCoverages: HeatmapCourseCoverage[]
  summary: HeatmapSummary
  warnings: HeatmapWarning[]
}
export interface CreditDistributionGroup {
  courseTypeId?: number | null
  code: string
  name: string
  nameVn?: string | null
  credits: number
  courseCount: number
  percentage: number
}

export interface CreditDistributionWarning {
  severity?: "ERROR" | "WARNING" | "INFO"
  code: string
  message: string
  references?: string[]
}

export interface CreditDistributionResponse {
  programId: number
  programCode: string
  programName?: string | null
  programNameVn?: string | null

  cohortId: number
  cohortName: string
  cohortEntryYear?: number | null

  declaredProgramCredits?: number | null
  calculatedTotalCredits: number
  creditDifference: number
  matchesDeclaredTotal: boolean

  uniqueCourseCount: number
  duplicateRowsRemoved: number
  dataSource?: string | null

  groups: CreditDistributionGroup[]
  warnings: CreditDistributionWarning[]
}

export const dashboardApi = {
  getAdminTerms: () =>
  api
    .get<AdminTermOption[]>(
      "/api/dashboard/admin/terms",
    )
    .then((res) => res.data),
  getDeanDashboard: (query: DeanDashboardQuery = {}) =>
    api
      .get<DashboardDeanResponse>("/api/dashboard/dean", {
        params: query,
      })
      .then((res) => res.data),
  getDeptHeadDashboard: (userId: number) =>
    api
      .get<DashboardDeptHeadResponse>(`/api/dashboard/dept-head/${userId}`)
      .then((res) => res.data),
  getMyFacultyDashboard: () =>
    api
      .get<DashboardFacultyResponse>("/api/dashboard/faculty/me")
      .then((res) => res.data),
  getFacultyDashboard: (userId: number) =>
    api
      .get<DashboardFacultyResponse>(`/api/dashboard/faculty/${userId}`)
      .then((res) => res.data),
  getAdminDashboard: (query: AdminDashboardQuery = {}) =>
    api.get<DashboardAdminResponse>("/api/dashboard/admin", { params: query }).then((res) => res.data),
  getHeatmapCoverage: (programId: number, query: HeatmapQuery) =>
    api
      .get<DashboardHeatmapResponse>(`/api/dashboard/heatmap/${programId}`, {
        params: query,
      })
      .then((res) => res.data),
  getCreditDistribution: (
    programId: number,
    cohortId: number,
  ) =>
    api
      .get<CreditDistributionResponse>(
        "/api/dashboard/credit-distribution",
        {
          params: {
            programId,
            cohortId,
          },
        },
      )
      .then((res) => res.data),
    }
