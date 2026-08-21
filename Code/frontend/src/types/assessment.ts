export type AssessmentType =
  | "QUIZ"
  | "ASSIGNMENT"
  | "LAB_REPORT"
  | "MIDTERM_EXAM"
  | "FINAL_EXAM"
  | "PROJECT"
  | "PRESENTATION"
  | "PARTICIPATION"

export interface AssessmentComponent {
  id: number
  syllabusId: number
  courseCode?: string
  name: string
  nameVn?: string
  assessmentType?: AssessmentType
  weightPercent?: number
  minScore?: number
  maxScore?: number
  orderIndex?: number
}

export interface CreateAssessmentComponentRequest {
  syllabusId: number
  name: string
  nameVn?: string
  assessmentType?: AssessmentType
  weightPercent?: number
  minScore?: number
  maxScore?: number
  orderIndex?: number
}

export const ASSESSMENT_TYPES: { value: AssessmentType; label: string }[] = [
  { value: "PARTICIPATION", label: "Chuyên cần / Tham gia (Participation)" },
  { value: "QUIZ", label: "Kiểm tra ngắn (Quiz)" },
  { value: "ASSIGNMENT", label: "Bài tập (Assignment)" },
  { value: "LAB_REPORT", label: "Báo cáo thực hành (Lab Report)" },
  { value: "MIDTERM_EXAM", label: "Thi giữa kỳ (Midterm Exam)" },
  { value: "PROJECT", label: "Đồ án (Project)" },
  { value: "PRESENTATION", label: "Thuyết trình (Presentation)" },
  { value: "FINAL_EXAM", label: "Thi cuối kỳ (Final Exam)" },
]
