export interface Department {
  id: number
  code: string
  name: string
  nameVn: string
  isActive: boolean
  headUserId: number | null
  headUsername: string | null
  headInstructorId: number | null
  headFullName: string | null
  createdAt: string
}

export interface DepartmentHeadCandidate {
  userAccountId: number
  username: string
  instructorId: number | null
  staffCode: string | null
  fullName: string | null
  departmentId: number | null
  departmentName: string | null
}

export interface CreateDepartmentRequest {
  code: string
  name: string
  nameVn: string
}

export interface Program {
  id: number
  code: string
  name: string
  nameVn: string
  majorId: number
  majorCode: string
  programTypeId: number
  programTypeCode: string
  departmentId: number
  departmentCode: string
  accreditationBody: string | null
  totalCredits: number
  durationYears: number
  validFrom: string
  validTo: string | null
  isActive: boolean
  createdAt: string
}

export interface CreateProgramRequest {
  code: string
  name: string
  nameVn: string
  majorId: number
  programTypeId: number
  departmentId: number
  accreditationBody: string
  totalCredits: number
  durationYears: number
  validFrom: string // YYYY-MM-DD
}

export interface UpdateProgramRequest {
  name: string
  nameVn: string
  majorId: number
  programTypeId: number
  departmentId: number
  accreditationBody: string | null
  totalCredits: number
  durationYears: number
  validFrom: string
  validTo: string | null
}

export interface ProgramArchiveValidation {
  programId: number
  canArchive: boolean
  violations: string[]
}

export interface AuditLog {
  id: number
  tableName: string
  recordId: number
  action: string
  oldValue: string | null
  newValue: string | null
  changedById: number | null
  changedByUsername: string
  changedAt: string
  ipAddress: string | null
  userAgent: string | null
}

export interface Major {
  id: number
  code: string
  name: string
  nameVn: string
}

export interface ProgramType {
  id: number
  code: string
  name: string
}



export interface ProgramCreditValidationCourse {
  courseProgramId: number
  courseId: number
  courseCode: string
  courseName: string
  creditTheory: number | null
  creditLab: number | null
  totalCredits: number
  courseTypeName: string | null
  semesterSuggest: number | null
  yearSuggest: number | null
  required: boolean
}

export interface ProgramCreditValidation {
  programId: number
  programCode: string
  cohortId: number
  cohortName: string
  expectedTotalCredits: number | null
  actualTotalCredits: number
  requiredCredits: number
  electiveCredits: number
  difference: number
  valid: boolean
  message: string
  courses: ProgramCreditValidationCourse[]
}

export interface CloneProgramRequest {
  sourceCohortId: number
  targetCohortId: number
  overwriteExisting?: boolean
}

export interface CloneProgramResponse {
  programId: number
  programCode: string
  sourceCohortId: number
  sourceCohortName: string
  targetCohortId: number
  targetCohortName: string
  copiedCount: number
  skippedCount: number
  overwrittenCount: number
  createdCourseProgramIds: number[]
  message: string
}

export interface Cohort {
  id: number
  programId: number
  programCode: string
  programName: string
  entryYear: number
  name: string
  description: string | null
  isActive: boolean
}

export interface CreateCurriculumRequest {
  program: CreateProgramRequest
  cohort: { entryYear: number; name: string; description?: string }
  cloneSource?: { programId: number; cohortId: number }
}

export interface CreateCurriculumResponse {
  program: Program
  cohort: Cohort
  clone: CloneProgramResponse | null
  message: string
}

export interface CurriculumTimelineCourseSummary {
  courseId: number
  courseCode: string
  courseName: string
  courseType: string | null
  required: boolean | null
  yearSuggest: number | null
  semesterSuggest: number | null
  termCode: string | null
  syllabusId: number | null
  syllabusVersionLabel: string | null
}

export interface CurriculumTimelineFieldChange {
  field: string
  label: string
  oldValue: string
  newValue: string
}

export interface CurriculumTimelineCourseChange {
  courseId: number
  courseCode: string
  courseName: string
  changeType: "ADDED" | "REMOVED" | "MODIFIED"
  changes: CurriculumTimelineFieldChange[]
}

export interface CurriculumTimelineItem {
  programId: number
  programCode: string
  programName: string
  cohortId: number
  cohortName: string
  entryYear: number
  previousCohortId: number | null
  previousCohortName: string | null
  previousEntryYear: number | null
  baseline: boolean
  totalCourses: number
  changeCount: number
  currentCourses: CurriculumTimelineCourseSummary[]
  addedCourses: CurriculumTimelineCourseChange[]
  removedCourses: CurriculumTimelineCourseChange[]
  changedCourses: CurriculumTimelineCourseChange[]
}
export interface ProgramDiffField {
  oldValue: string | null
  newValue: string | null
}

export interface ProgramDiffCourse {
  courseId: number
  courseCode: string
  courseName: string
  changes: Record<string, ProgramDiffField> | null
}

export interface ProgramDiffResponse {
  programId: number
  programCode: string
  oldCohortId: number
  newCohortId: number
  oldCohortYear: string
  newCohortYear: string
  courseDiff: {
    added: ProgramDiffCourse[]
    removed: ProgramDiffCourse[]
    modified: ProgramDiffCourse[]
  }
}