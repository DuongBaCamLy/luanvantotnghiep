export interface CloDTO {
  code: string
  description: string
  descriptionVn?: string
  competencyLevel?: string
  bloomLevel?: string
  orderIndex?: number
}

export interface TopicDTO {
  weekNumber: number
  orderInWeek: number
  name: string
  nameVn?: string
  teachingHours?: number
  labHours?: number
  selfStudyHours?: number
  topicType?: string
  teachingMethod?: string
  learningActivity?: string
  notes?: string
}

export interface AssessmentDTO {
  name: string
  nameVn?: string
  assessmentType?: string
  weightPercent: number
  minScore?: number
  maxScore?: number
  orderIndex?: number
}

export interface SyllabusNoteReference {
  value?: string
  text?: string
  title?: string
}

export interface SyllabusNoteActivity {
  week?: string | number
  topic?: string
  clo?: string
  assessments?: string
  activities?: string
  resources?: string
}

export interface SyllabusNoteAssessmentMatrix {
  type?: string
  clo1?: string
  clo2?: string
  clo3?: string
  weight?: string | number
}

export interface SyllabusNotesData {
  instructor?: string
  creditsTheory?: string | number
  creditsPractice?: string | number
  ects?: string | number
  periodsTheory?: string | number
  periodsPractice?: string | number

  references?: string[] | SyllabusNoteReference[]
  loMatrix?: Record<string, number[]>
  activities?: SyllabusNoteActivity[]
  assessmentMatrix?: SyllabusNoteAssessmentMatrix[]

  [key: string]: unknown
}

export interface Syllabus {
  id: number

  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string

   versionNumber: number
  versionLabel: string
  cohortId?: number | null
  cohortName?: string | null
  academicYear: string

  courseDesignation?: string | null
  courseTypes?: string | null
  semester?: string | null
  language?: string | null
  relation?: string | null
  teachingMethods?: string | null
  workloadTotal?: string | null
  workloadContact?: string | null
  workloadPrivate?: string | null
  prerequisites?: string | null
  objectives?: string | null
  examForms?: string | null
  examRequirements?: string | null
  rubrics?: string | null
  major?: string | null

  status: string
  isCurrent: boolean

  createdById: number
  createdByUsername: string

  approvedById: number | null
  approvedByUsername: string | null

  submittedAt: string | null
  approvedAt: string | null

  changeSummary: string | null
  notes: string | null

  clos?: CloDTO[]
  topics?: TopicDTO[]
  assessments?: AssessmentDTO[]

  createdAt: string
  updatedAt: string
}

export interface CreateSyllabusRequest {
  classSectionId?: number
  programId?: number
cohortId?: number | null
courseTypeId?: number
required?: boolean
  courseId: number
courseProgramId?: number
  versionNumber?: number
  versionLabel?: string
  academicYear?: string

  courseDesignation?: string | null
courseTypes?: string | null
semester?: string | null
language?: string | null
relation?: string | null
teachingMethods?: string | null
workloadTotal?: string | null
workloadContact?: string | null
workloadPrivate?: string | null
prerequisites?: string | null
objectives?: string | null
examForms?: string | null
examRequirements?: string | null
rubrics?: string | null
major?: string | null

  createdBy?: number

  changeSummary?: string
  notes?: string

  sourceSyllabusId?: number

  clos?: CloDTO[]
  topics?: TopicDTO[]
  assessments?: AssessmentDTO[]
}

export interface CloneSyllabusRequest {
  cohortId?: number
  classSectionId?: number
  academicYear?: string
  semester?: string
  changeSummary?: string
}

export interface SubmissionValidationIssue {
  code: string
  section: string
  tabId: number
  field: string
  message: string
}

export interface SubmissionValidationResponse {
  syllabusId: number
  valid: boolean
  message: string
  errorCount: number
  assessmentTotalWeight: number | null
  workloadTotal: number | null
  workloadContact: number | null
  workloadPrivate: number | null
  topicContactHours: number | null
  topicPrivateHours: number | null
  issues: SubmissionValidationIssue[]
}


export interface SyllabusCreateContextResponse {
  course: {
    id: number
    courseCode: string
    name: string
    nameVn?: string
    creditTheory?: number
    creditLab?: number
    courseLevel?: string
    description?: string
    departmentName?: string
  }
  latestSyllabus?: Syllabus | null
  latestApprovedSyllabus?: Syllabus | null
}
