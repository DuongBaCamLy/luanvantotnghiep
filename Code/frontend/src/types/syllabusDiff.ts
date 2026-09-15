export type FieldDiff = {
  oldValue?: string | null
  newValue?: string | null
}

export type ListDiff<T> = {
  added?: T[]
  removed?: T[]
  modified?: T[]
}

export type CloDiff = {
  code?: string
  description?: string
  descriptionVn?: string
  competencyLevel?: string
  bloomLevel?: string
  orderIndex?: number
  plos?: string[]
  changes?: Record<string, FieldDiff>
}

export type CloPloMappingDiff = {
  cloCode?: string
  ploCode?: string
  level?: string
  contributionWeight?: number
  notes?: string
  changes?: Record<string, FieldDiff>
}

export type TopicDiff = {
  name?: string
  nameVn?: string
  weekNumber?: number
  orderInWeek?: number

  teachingHours?: number
  labHours?: number
  selfStudyHours?: number

  topicType?: string
  teachingMethod?: string
  learningActivity?: string

  assessments?: string
  resources?: string
  contentWeight?: string
  contentLevel?: string

  changes?: Record<string, FieldDiff>
}

export type TopicCloMappingDiff = {
  topicName?: string
  weekNumber?: number
  orderInWeek?: number
  cloCode?: string
  teachingLevel?: string
  changes?: Record<string, FieldDiff>
}

export type PlannedActivityDiff = {
  week?: number
  topic?: string
  clo?: string
  assessments?: string
  learningActivities?: string
  resources?: string
  changes?: Record<string, FieldDiff>
}

export type AssessmentDiff = {
  name?: string
  nameVn?: string
  assessmentType?: string
  weightPercent?: number
  minScore?: number
  maxScore?: number
  orderIndex?: number
  changes?: Record<string, FieldDiff>
}

export type AssessmentCloMappingDiff = {
  assessmentName?: string
  orderIndex?: number
  cloCode?: string
  contributionPercent?: number
  changes?: Record<string, FieldDiff>
}

/**
 * Kept with the existing backend contract name during the comparison refactor.
 * Rename only in a later coordinated backend + frontend cleanup.
 */
export type readingsDiff = {
  bookId?: number
  title?: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  bookType?: string
  usageType?: string
  orderIndex?: number
  changes?: Record<string, FieldDiff>
}

export type SyllabusDiffResponse = {
  oldSyllabusId: number
  newSyllabusId: number
  oldVersionLabel: string
  newVersionLabel: string
  hasChanges: boolean

  /**
   * Canonical scalar form sections.
   * Raw Syllabus.notes must never appear as a field.
   */
  generalInfoDiff?: Record<string, FieldDiff>
  workloadCreditDiff?: Record<string, FieldDiff>
  requirementsDiff?: Record<string, FieldDiff>
  contentDiff?: Record<string, FieldDiff>
  assessmentInfoDiff?: Record<string, FieldDiff>
  examinationDiff?: Record<string, FieldDiff>
  revisionInfoDiff?: Record<string, FieldDiff>

  /**
   * Canonical repeating / matrix sections.
   */
  cloDiff?: ListDiff<CloDiff>
  cloPloDiff?: ListDiff<CloPloMappingDiff>
  topicDiff?: ListDiff<TopicDiff>
  topicCloDiff?: ListDiff<TopicCloMappingDiff>
  plannedActivityDiff?: ListDiff<PlannedActivityDiff>
  assessmentDiff?: ListDiff<AssessmentDiff>
  assessmentCloDiff?: ListDiff<AssessmentCloMappingDiff>
  readingsDiff?: ListDiff<readingsDiff>
}

export type SemanticAnalysisStatus =
  | "SUCCESS"
  | "PARTIAL"
  | "DISABLED"
  | "UNAVAILABLE"

export type SemanticChangeType =
  | "NO_MEANINGFUL_CHANGE"
  | "MINOR_REWORDING"
  | "MEANINGFUL_CHANGE"

export type SemanticChangeNature =
  | "SAME_MEANING"
  | "ADDED"
  | "REMOVED"
  | "EXPANDED"
  | "REDUCED"
  | "MODIFIED"
  | "COGNITIVE_LEVEL_INCREASE"
  | "COGNITIVE_LEVEL_DECREASE"
  | "OTHER"

export type SemanticSignificance =
  | "LOW"
  | "MEDIUM"
  | "HIGH"

export type SemanticSectionType =
  | "COURSE_OBJECTIVE"
  | "CLO"
  | "TOPIC"
  | "TEACHING_METHOD"
  | "LEARNING_ACTIVITY"
  | "EXAM_REQUIREMENT"

export type SemanticDiffResult = {
  itemId: string
  sectionType: SemanticSectionType
  fieldName: string

  oldText?: string | null
  newText?: string | null

  classification?: SemanticChangeType | null
  changeNature?: SemanticChangeNature | null
  significance?: SemanticSignificance | null

  requiresAi: boolean

  oldMeaning?: string | null
  newMeaning?: string | null

  summary?: string | null
}

export type SemanticSyllabusDiffResponse = {
  oldSyllabusId: number
  newSyllabusId: number

  oldVersionLabel?: string | null
  newVersionLabel?: string | null

  courseId: number
  courseCode?: string | null
  courseName?: string | null

  status: SemanticAnalysisStatus

  hasMeaningfulChanges: boolean
  hasUnresolvedItems: boolean

  overallSignificance?: SemanticSignificance | null

  summary?: string | null

  items: SemanticDiffResult[]
}
