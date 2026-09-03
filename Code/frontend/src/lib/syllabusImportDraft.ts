import type {
  SyllabusImportPreviewResponse,
  ValidSyllabusImportPreviewResponse,
} from "@/types/syllabusImport"

const STORAGE_PREFIX = "scse:syllabus-import-preview:"

type StoredSyllabusImportDraft = {
  schemaVersion: 2
  targetCourseId: number
  targetCourseProgramId?: number
  createdAt: number
  preview: SyllabusImportPreviewResponse
}

const storageKey = (draftId: string) => `${STORAGE_PREFIX}${draftId}`

const isImportPreview = (value: unknown): value is SyllabusImportPreviewResponse => {
  if (!value || typeof value !== "object") return false
  const source = value as Partial<SyllabusImportPreviewResponse>
  return typeof source.fileName === "string"
    && typeof source.fileType === "string"
    && typeof source.valid === "boolean"
    && Boolean(source.data && typeof source.data === "object")
}

export const isUsableSyllabusImportPreview = (
  value: SyllabusImportPreviewResponse | null | undefined,
): value is ValidSyllabusImportPreviewResponse => Boolean(
  value?.valid
  && value.data
  && value.data.sourceCourseCode?.trim()
  && (value.data.clos?.length ?? 0) > 0
  && Math.max(value.data.topics?.length ?? 0, value.data.weeklyActivities?.length ?? 0) > 0
  && (value.data.assessments?.length ?? 0) > 0,
)

/**
 * Keeps extracted data available while the user reviews the form. React Router
 * location state is still passed for fast navigation; this session copy is the
 * reload/direct-navigation fallback and never contains the original file bytes.
 */
export const saveSyllabusImportDraft = (
  preview: SyllabusImportPreviewResponse,
  targetCourseId: number,
  targetCourseProgramId?: number,
) => {
  const draftId = typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`

  try {
    const stored: StoredSyllabusImportDraft = {
      schemaVersion: 2,
      targetCourseId,
      targetCourseProgramId,
      createdAt: Date.now(),
      preview,
    }
    window.sessionStorage.setItem(storageKey(draftId), JSON.stringify(stored))
  } catch {
    // Router state still carries the preview when storage is unavailable.
  }
  return draftId
}

export const loadSyllabusImportDraft = (
  draftId?: string | null,
  expectedCourseId?: number,
  expectedCourseProgramId?: number,
) => {
  if (
    !draftId
    || typeof window === "undefined"
    || !expectedCourseId
    || !Number.isFinite(expectedCourseId)
  ) return undefined

  try {
    const raw = window.sessionStorage.getItem(storageKey(draftId))
    if (!raw) return undefined
    const parsed = JSON.parse(raw) as Partial<StoredSyllabusImportDraft>
    const expired = typeof parsed.createdAt !== "number"
      || Date.now() - parsed.createdAt > 4 * 60 * 60 * 1_000
    const programMismatch = parsed.targetCourseProgramId !== undefined
      && parsed.targetCourseProgramId !== expectedCourseProgramId
    return parsed.schemaVersion === 2
      && parsed.targetCourseId === expectedCourseId
      && !expired
      && !programMismatch
      && isImportPreview(parsed.preview)
      ? parsed.preview
      : undefined
  } catch {
    return undefined
  }
}

export const removeSyllabusImportDraft = (draftId?: string | null) => {
  if (!draftId || typeof window === "undefined") return
  try {
    window.sessionStorage.removeItem(storageKey(draftId))
  } catch {
    // Nothing else to clean up when storage is unavailable.
  }
}
