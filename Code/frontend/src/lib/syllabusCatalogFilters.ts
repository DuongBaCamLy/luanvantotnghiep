export const SYLLABUS_FILTER_ALL = "all"

export interface SyllabusFilter {
  programId?: number
  cohortId?: number
  semester?: string
  status?: string
  search?: string
}

export const readSyllabusFilter = (params: URLSearchParams): SyllabusFilter => {
  const numberParam = (key: string) => {
    const value = Number(params.get(key))
    return Number.isInteger(value) && value > 0 ? value : undefined
  }
  const optional = (key: string) => {
    const value = params.get(key)?.trim()
    return value && value !== SYLLABUS_FILTER_ALL ? value : undefined
  }
  return {
    programId: numberParam("programId"),
    cohortId: numberParam("cohortId"),
    semester: optional("semester"),
    status: optional("status"),
    search: optional("q"),
  }
}

export const SYLLABUS_SEMESTER_OPTIONS = Array.from(
  { length: 8 },
  (_, index) => String(index + 1),
)

export const SYLLABUS_CANONICAL_STATUSES = [
  "DRAFT",
  "SUBMITTED",
  "UNDER_REVIEW",
  "REVISION_REQUESTED",
  "REJECTED",
  "APPROVED",
  "ARCHIVED",
]

export const formatSyllabusFilterStatus = (status: string) => {
  switch (status.trim().toUpperCase()) {
    case "DRAFT": return "Draft"
    case "SUBMITTED": return "Pending Review"
    case "UNDER_REVIEW": return "Forwarded to Dean"
    case "REVISION_REQUESTED": return "Rejected / Revision"
    case "REJECTED": return "Rejected"
    case "APPROVED": return "Approved"
    case "ARCHIVED": return "Archived"
    default: return status
  }
}
