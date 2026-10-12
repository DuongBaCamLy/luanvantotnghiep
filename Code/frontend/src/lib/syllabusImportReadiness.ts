import type { SyllabusImportPreviewResponse } from "@/types/syllabusImport"

export type SyllabusImportReadiness = "READY_COMPLETE" | "READY_WITH_WARNINGS" | "BLOCKED_DATA_ERROR" | "SOURCE_REVIEW_REQUIRED"

export function syllabusImportReadiness(preview: SyllabusImportPreviewResponse): SyllabusImportReadiness {
  const errors = (preview.issues ?? []).filter((issue) => issue.severity.toUpperCase() === "ERROR")
  if (!preview.data) return "SOURCE_REVIEW_REQUIRED"
  if (errors.some((issue) => !/SOURCE_NOT_VERIFIED|source review|source row boundaries|OCR/i.test(issue.message))) return "BLOCKED_DATA_ERROR"
  if (!preview.valid || errors.length > 0) return "SOURCE_REVIEW_REQUIRED"
  const data = preview.data
  const partialContent = (data.topics ?? []).some((topic) => topic.weekNumber == null || topic.teachingHours == null
    || topic.labHours == null || topic.selfStudyHours == null)
  const incompleteMetadata = !data.workloadContact || !data.workloadPrivate || !data.personResponsible
  return partialContent || incompleteMetadata || (preview.issues ?? []).some((issue) => issue.severity.toUpperCase() === "WARNING")
    ? "READY_WITH_WARNINGS" : "READY_COMPLETE"
}
