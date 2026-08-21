import type { AssessmentDTO, CloDTO, TopicDTO } from "@/types/syllabus"

export interface SyllabusImportIssue {
  severity: "ERROR" | "WARNING"
  section: string
  row?: number | null
  field: string
  message: string
}

export interface ReadingImportItem {
  title: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  bookType?: string
  usageType?: string
  orderIndex?: number
}

export interface SyllabusImportData {
  sourceCourseCode?: string
  sourceCourseName?: string
  courseDesignation?: string
  courseTypes?: string
  semester?: string
  language?: string
  relation?: string
  teachingMethods?: string
  workloadTotal?: string
  workloadContact?: string
  workloadPrivate?: string
  prerequisites?: string
  objectives?: string
  examForms?: string
  examRequirements?: string
  rubrics?: string
  major?: string
  clos: CloDTO[]
  topics: TopicDTO[]
  assessments: AssessmentDTO[]
  readingList: ReadingImportItem[]
  cloPloMappings?: { cloCode: string; ploCode: string; level?: string; contributionWeight?: number; notes?: string }[]
  topicCloMappings?: { weekNumber?: number; topicName?: string; cloCode: string; teachingLevel?: string }[]
  assessmentCloMappings?: { assessmentName: string; cloCode: string; contributionPercent?: number }[]
}

export interface SyllabusImportPreviewResponse {
  fileName: string
  fileType: string
  valid: boolean
  errorCount: number
  warningCount: number
  data: SyllabusImportData
  issues: SyllabusImportIssue[]
}

export type SyllabusImportMode =
  | "MERGE"
  | "REPLACE_ALL"
  | "UPDATE_DETECTED_FIELDS_ONLY"
