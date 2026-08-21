export type ApprovalStep =
  | "STEP1_DEPT_HEAD"
  | "STEP2_PROG_COORDINATOR"
  | "STEP3_DEAN"

export type ApprovalStatus =
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "REVISION_REQUESTED"

export interface ApprovalRequestItem {
  id: number
  syllabusId: number
  courseCode: string
  courseName: string
  versionNumber: number
  versionLabel: string
  syllabusStatus: string
  step: ApprovalStep
  status: ApprovalStatus
  requestedById: number
  requestedByUsername: string
  reviewedById: number | null
  reviewedByUsername: string | null
  comment: string | null
  submittedAt: string | null
  createdAt: string
  resolvedAt: string | null
  revisionDraftId: number | null
  revisionDraftVersionLabel: string | null
}

/**
 * FR-05.8:
 * Một bản ghi trong lịch sử review/comment của syllabus.
 */
export interface ApprovalHistoryItem {
  id: number
  step: ApprovalStep | null
  status: ApprovalStatus | null
  requestedByUsername: string | null
  reviewedByUsername: string | null
  comment: string | null
  createdAt: string | null
  resolvedAt: string | null
}

export interface ReviewApprovalPayload {
  status: "APPROVED" | "REJECTED"
  comment: string
}