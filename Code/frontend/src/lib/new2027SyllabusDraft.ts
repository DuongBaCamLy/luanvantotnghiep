import { toSyllabusFormData } from "./syllabusFormData"
import type { CreateSyllabusRequest, Syllabus } from "@/types/syllabus"

export const isNew2027Syllabus = (syllabus?: Pick<Syllabus, "targetTemplateProfile"> | null) =>
  String(syllabus?.targetTemplateProfile ?? "").trim().toUpperCase() === "NEW_2027"

export function draftNotes(raw?: string | null): Record<string, unknown> {
  if (!raw) return {}
  try {
    const value: unknown = JSON.parse(raw)
    if (value && typeof value === "object" && !Array.isArray(value)) return value as Record<string, unknown>
  } catch { /* Preserve free-text notes when the first supplemental field is edited. */ }
  return { internalNotes: raw }
}

/** Preserve the imported notes verbatim until the user actually edits a field. */
export function patchDraftNotes(syllabus: Syllabus, patch: Record<string, unknown>): Syllabus {
  return { ...syllabus, notes: JSON.stringify({ ...draftNotes(syllabus.notes), ...patch }) }
}

export function removeDraftClo(syllabus: Syllabus, index: number): Syllabus {
  const code = syllabus.clos?.[index]?.code
  if (!code) return syllabus
  const notes = draftNotes(syllabus.notes)
  const cleanCodes = (value: unknown) => String(value ?? "").split(/[,;\n]/).map(item => item.trim()).filter(item => item && item !== code).join(", ")
  const patch: Record<string, unknown> = {}
  for (const key of ["cloPloMatrix", "loMatrix"]) {
    if (notes[key] && typeof notes[key] === "object" && !Array.isArray(notes[key])) {
      patch[key] = Object.fromEntries(Object.entries(notes[key]).filter(([entry]) => entry !== code))
    }
  }
  if (notes.assessmentCloMatrix && typeof notes.assessmentCloMatrix === "object") {
    patch.assessmentCloMatrix = Object.fromEntries(Object.entries(notes.assessmentCloMatrix).map(([key, row]) =>
      [key, Object.fromEntries(Object.entries(row as Record<string, unknown>).filter(([entry]) => entry !== code))]))
  }
  if (Array.isArray(notes.sourceContentRows)) patch.sourceContentRows = notes.sourceContentRows.map(row => ({ ...row,
    relatedCloCodes: Array.isArray(row.relatedCloCodes) ? row.relatedCloCodes.filter((entry: unknown) => entry !== code) : row.relatedCloCodes,
  }))
  if (Array.isArray(notes.plannedActivities)) patch.plannedActivities = notes.plannedActivities.map(row => ({ ...row, clo: cleanCodes(row.clo) }))
  if (notes.topicDetails && typeof notes.topicDetails === "object") patch.topicDetails = Object.fromEntries(Object.entries(notes.topicDetails)
    .map(([key, row]) => [key, { ...(row as Record<string, unknown>), clo: cleanCodes((row as Record<string, unknown>).clo) }]))
  return { ...patchDraftNotes(syllabus, patch), clos: syllabus.clos?.filter((_, position) => position !== index) }
}

export function removeDraftAssessment(syllabus: Syllabus, index: number): Syllabus {
  const notes = draftNotes(syllabus.notes)
  const patch: Record<string, unknown> = {}
  if (notes.assessmentCloMatrix && typeof notes.assessmentCloMatrix === "object") patch.assessmentCloMatrix =
    Object.fromEntries(Object.entries(notes.assessmentCloMatrix).flatMap(([key, row]) => {
      const position = Number(key)
      return position === index ? [] : [[Number.isInteger(position) && position > index ? String(position - 1) : key, row]]
    }))
  return { ...patchDraftNotes(syllabus, patch), assessments: syllabus.assessments?.filter((_, position) => position !== index) }
}

export function toNew2027DraftPayload(draft: Syllabus, initialData: CreateSyllabusRequest): CreateSyllabusRequest {
  return {
    ...initialData,
    ...toSyllabusFormData(draft, initialData),
    // Semester is the existing Draft context; the source semester list lives in notes.
    semester: draft.semester ?? "",
    notes: draft.notes ?? "",
    ...(draft.rubrics !== undefined ? { rubrics: draft.rubrics } : {}),
  }
}

/** Only reconcile relations explicitly edited; saving prose must not erase source mappings. */
export function new2027RelationPayload(original: Syllabus, payload: CreateSyllabusRequest): CreateSyllabusRequest {
  const before = draftNotes(original.notes)
  const after = draftNotes(payload.notes)
  const changed: Record<string, unknown> = {}
  for (const key of ["topicDetails", "assessmentCloMatrix", "cloPloMatrix", "readings"]) {
    if (!Object.hasOwn(after, key) || JSON.stringify(before[key]) === JSON.stringify(after[key])) continue
    // Source numeric/R/M marks and unspecified reading classifications are kept in notes.
    // They are not silently converted into normalized program/book relationships.
    if (key === "cloPloMatrix") {
      const rows = after.cloPloMatrix && typeof after.cloPloMatrix === "object" ? Object.values(after.cloPloMatrix) : []
      const sourceMarks = rows.some(row => row && typeof row === "object" && Object.values(row).some(value => {
        const mark = String(value ?? "").trim()
        return mark && !/^(x{1,3}|a|d|i|t|u|achieve|develop|introduce|\.|-|0)$/i.test(mark)
      }))
      if (after.sourceAlignmentPreserved === true || sourceMarks) continue
    }
    if (key === "readings" && (after.readingUsagePolicy === "SOURCE_UNSPECIFIED"
      || (Array.isArray(after.readings) && after.readings.some((row) =>
        !row || typeof row !== "object" || !(row as Record<string, unknown>).usageType)))) continue
    changed[key] = after[key]
  }
  return { ...payload, notes: JSON.stringify(changed) }
}
