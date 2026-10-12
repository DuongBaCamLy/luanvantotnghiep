type CurriculumMapping = {
  id?: number
  courseId?: number
  courseCode?: string
  courseName?: string
  programId?: number
  cohortId?: number | null
  syllabusId?: number | null
}

type ImportItem = { preview: { valid: boolean; data?: { sourceCourseCode?: string; sourceCourseName?: string } | null } }
type Context = { programId: number; cohortId: number; cohortName?: string }

/** A UI choice only; the existing backend still checks every selected import. */
export function ds2021ImportChoices<T extends CurriculumMapping>(items: ImportItem[], mappings: T[], context: Context) {
  return items.map((item, index) => {
    const sourceCode = item.preview.data?.sourceCourseCode
    const sourceName = item.preview.data?.sourceCourseName
    const candidates = String(sourceCode ?? "").split(/\s*[/;,|]\s*/).map(code)
    const direct = mappings.filter(mapping => Number(mapping.programId) === context.programId
      && (mapping.cohortId == null || Number(mapping.cohortId) === context.cohortId)
      && candidates.includes(code(mapping.courseCode)))
      .sort((left, right) => Number(Number(right.cohortId) === context.cohortId)
        - Number(Number(left.cohortId) === context.cohortId))
    let mapping: T | undefined = direct[0]
    let ambiguous = new Set(direct.map(row => row.courseId)).size > 1
    if (!mapping) {
      try { mapping = resolveDs2021ChemistryMapping(mappings, { ...context, sourceCode, sourceName }) }
      catch { ambiguous = true }
    }
    return { index, code: sourceCode ?? "Unknown", name: sourceName ?? "Unnamed syllabus",
      state: !item.preview.valid ? "invalid" as const : ambiguous ? "ambiguous" as const : !mapping ? "unmapped" as const
        : Number(mapping.syllabusId ?? 0) > 0 ? "existing" as const : "missing" as const }
  })
}

const code = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, "")
const chemistryName = (value: unknown) => /^chemistry for engineers?$/i.test(String(value ?? "").replace(/\s+/g, " ").trim())

/** Routing hint only: the server verifies the stored PDF and import permission. */
export function canConfirmDs2021CloudAppendix(context: {
  cohortName?: string; sourceType?: string; sourceDocumentId?: number
  sourceCode?: string; sourceName?: string; isInstructor: boolean
}) {
  return !context.isInstructor
    && String(context.cohortName ?? "").trim().toUpperCase() === "DS2021"
    && context.sourceType === "PDF"
    && Number.isSafeInteger(context.sourceDocumentId) && Number(context.sourceDocumentId) > 0
    && code(context.sourceCode) === "IT164"
    && String(context.sourceName ?? "").trim().toLowerCase() === "cloud computing"
}

/** DS2021 PDF: curriculum CH011IU (p13), appendix CHE011IU (p93). Backend already recognizes this pair. */
export function resolveDs2021ChemistryMapping<T extends CurriculumMapping>(
  mappings: T[],
  context: { programId: number; cohortId: number; cohortName?: string; sourceCode?: string; sourceName?: string },
): T | undefined {
  if (String(context.cohortName ?? "").trim().toUpperCase() !== "DS2021"
    || code(context.sourceCode) !== "CHE011" || !chemistryName(context.sourceName)) return undefined

  return resolveChemistryMapping(mappings, context)
}

/** The shared NE/CE source has the same verified CHE011/CH011 spelling difference. */
export function resolveEngineeringChemistryMapping<T extends CurriculumMapping>(
  mappings: T[],
  context: { programId: number; cohortId: number; cohortName?: string; sourceCode?: string; sourceName?: string; targetTemplateProfile?: string },
): T | undefined {
  if (!/^(NE|CE)20\d{2}$/i.test(String(context.cohortName ?? "").trim())
    || String(context.targetTemplateProfile ?? "").trim().toUpperCase() !== "SOURCE_TEMPLATE"
    || code(context.sourceCode) !== "CHE011" || !chemistryName(context.sourceName)) return undefined
  return resolveChemistryMapping(mappings, context)
}

function resolveChemistryMapping<T extends CurriculumMapping>(
  mappings: T[], context: { programId: number; cohortId: number; cohortName?: string },
): T | undefined {

  const matches = mappings.filter(mapping => Number(mapping.programId) === context.programId
    && (mapping.cohortId == null || Number(mapping.cohortId) === context.cohortId)
    && code(mapping.courseCode) === "CH011" && chemistryName(mapping.courseName))
    .sort((left, right) => Number(Number(right.cohortId) === context.cohortId)
      - Number(Number(left.cohortId) === context.cohortId))
  if (new Set(matches.map(mapping => mapping.courseId)).size > 1) {
    throw new Error(`Import stopped: ambiguous CH011IU course identity in ${context.cohortName}.`)
  }
  return matches[0]
}
