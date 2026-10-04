import { useState } from "react"
import { FileStack, Loader2 } from "lucide-react"
import { toast } from "sonner"

import { syllabusImportApi } from "@/api/syllabusImportApi"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  parseTargetTemplateSections,
  validateNew2027TargetTemplateResponse,
} from "@/lib/syllabusComparisonTemplate"
import type {
  BulkSyllabusImportPreviewResponse,
  SyllabusImportData,
} from "@/types/syllabusImport"

type Props = {
  open: boolean
  onClose: () => void
  programId: number
  cohortId: number
  cohortName?: string
  allowedCourseCodes?: string[]
  onStart: (response: BulkSyllabusImportPreviewResponse) => void
}

export default function BulkImportSyllabusDialog({ open, onClose, programId, cohortId, cohortName, allowedCourseCodes, onStart }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<BulkSyllabusImportPreviewResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [new2027Confirmed, setNew2027Confirmed] = useState(false)

  const isNew2027Target =
  String(result?.targetTemplateProfile ?? "").trim().toUpperCase() === "NEW_2027"
  const targetSections =
    result ? parseTargetTemplateSections(result) : []
  const targetMetadataError =
    result && isNew2027Target
      ? validateNew2027TargetTemplateResponse(result)
      : null

  const close = () => {
    setFile(null)
    setResult(null)
    setLoading(false)
    setNew2027Confirmed(false)
    onClose()
  }

  const extract = async () => {
    if (!file) return
    if (file.size > 200 * 1024 * 1024) {
      toast.error("The PDF exceeds the 200 MB upload limit.")
      return
    }
    try {
      setLoading(true)
      const response = await syllabusImportApi.previewBulk(file, programId, cohortId)
      const metadataError =
        isNew2027Target
          ? validateNew2027TargetTemplateResponse(response)
          : null

      setNew2027Confirmed(false)
      setResult(response)

      const normalize = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, "")
      const allowed = new Set((allowedCourseCodes ?? []).map(normalize))
      const blocked = allowedCourseCodes
        ? response.items.map((item) => item.preview.data?.sourceCourseCode || "Unknown")
          .filter((code) => !allowed.has(normalize(code)))
        : []

      if (metadataError) {
        toast.error(metadataError)
      } else if (blocked.length > 0) {
        toast.error(`Import blocked. You are not assigned to: ${blocked.join(", ")}.`)
      } else {
        toast.success(`Detected ${response.syllabusCount} syllabuses.`)
      }
    } catch (error) {
      console.error(error)
      const status = (error as { response?: { status?: number; data?: { message?: string } } })?.response?.status
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(status === 413
        ? "The PDF exceeds the server upload limit."
        : message || "Upload failed. Check that the backend was restarted, then try again.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(value) => !value && close()}>
      <DialogContent className={`max-h-[92vh] overflow-hidden border-[#cfe2e4] bg-white p-0 ${isNew2027Target ? "sm:max-w-5xl" : "sm:max-w-xl"}`}>
        <div className="h-1 bg-gradient-to-r from-[#007d84] via-[#20a0a5] to-[#f0a72f]" />
        <div className="space-y-5 px-6 pb-2 pt-5">
          <DialogHeader className="text-left">
            <DialogTitle className="text-xl text-[#006f76]">Import Program Document</DialogTitle>
            <DialogDescription>{cohortName} · PDF or DOCX containing multiple syllabuses</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label>PDF or Word document</Label>
            <Input type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" disabled={loading} onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setResult(null)
              setNew2027Confirmed(false)
            }} />
          </div>
          {result && (() => {
            const normalize = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, "")
            const allowed = new Set((allowedCourseCodes ?? []).map(normalize))
            const blocked = allowedCourseCodes
              ? result.items.map((item) => item.preview.data?.sourceCourseCode || "Unknown").filter((code) => !allowed.has(normalize(code)))
              : []
            return <>
            <div className="grid grid-cols-3 gap-3 rounded-xl border border-[#d9e8ea] bg-[#f5fafa] p-4 text-center">
              <Stat value={result.pageCount} label="Pages" />
              <Stat value={result.syllabusCount} label="Detected" />
              <Stat value={result.items.filter((item) => item.preview.valid).length} label="Ready" />
            </div>
            <p className="text-xs font-semibold text-[#006f76]">Source type: {result.sourceType === "DOCX" ? "WORD" : "PDF"}</p>
            {blocked.length > 0 && <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-800">You are not assigned to these courses and cannot import their syllabuses: <b>{blocked.join(", ")}</b>. No syllabus will be saved.</p>}
            </>
          })()}

          {result && isNew2027Target && (
            <div className="space-y-3 rounded-xl border border-[#cfe2e4] bg-white p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="text-sm font-bold text-[#006f76]">Review Template 2027</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Source semantic content remains unchanged; only the target review structure is NEW_2027.
                  </p>
                </div>
                <span className="rounded-full border border-[#b9d9dc] bg-[#eef8f8] px-2.5 py-1 text-[11px] font-semibold text-[#006f76]">
                  {result.targetTemplateProfile || "Missing target profile"}
                </span>
              </div>

              {targetMetadataError ? (
                <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-medium text-rose-800">
                  {targetMetadataError} Nothing can be saved until the backend returns the complete NEW_2027 contract.
                </p>
              ) : (
                <div className="max-h-[46vh] space-y-3 overflow-y-auto pr-1">
                  {targetSections.map((section) => (
                    <section key={section.key} className="overflow-hidden rounded-lg border border-slate-200">
                      <div className="border-b bg-slate-50 px-3 py-2">
                        <p className="text-xs font-bold text-slate-800">{section.label}</p>
                        {section.key === "cloLlo" && (
                          <p className="mt-1 text-[11px] font-medium text-amber-700">
                            No canonical LLO model exists in the source. LLO values will stay Missing / Absent in source; nothing is generated.
                          </p>
                        )}
                      </div>
                      <div className="divide-y divide-slate-100">
                        {section.fields.map((targetField) => {
                          const evidence = summarizeSourceEvidence(
                            result.items,
                            section.key,
                            targetField.key,
                          )

                          return (
                            <div
                              key={`${section.key}-${targetField.key}`}
                              className="grid gap-2 px-3 py-2 text-xs sm:grid-cols-[minmax(0,0.8fr)_minmax(0,2fr)_auto] sm:items-start"
                            >
                              <div>
                                <p className="font-semibold text-slate-700">TARGET</p>
                                <p className="mt-0.5 text-slate-900">{targetField.label}</p>
                              </div>
                              <div>
                                <p className="font-semibold text-slate-500">SOURCE</p>
                                <p className="mt-0.5 break-words text-slate-600">
                                  {evidence.summary}
                                </p>

                                {evidence.example && (
                                  <p className="mt-1 break-words text-slate-500">
                                    <span className="font-semibold">
                                      Example parsed value:
                                    </span>{" "}
                                    {evidence.example}
                                  </p>
                                )}

                                {evidence.affected && (
                                  <p className="mt-1 break-words text-amber-700">
                                    <span className="font-semibold">
                                      Affected courses:
                                    </span>{" "}
                                    {evidence.affected}
                                  </p>
                                )}

                                {evidence.note && (
                                  <p className="mt-1 break-words text-slate-500">
                                    {evidence.note}
                                  </p>
                                )}
                              </div>
                              <span className={`w-fit rounded-full px-2 py-1 text-[10px] font-semibold ${evidenceClassName(evidence.state)}`}>
                                {evidenceLabel(evidence.state)}
                              </span>
                            </div>
                          )
                        })}
                      </div>
                    </section>
                  ))}
                </div>
              )}

              <label className={`flex items-start gap-3 rounded-lg border px-3 py-2.5 text-xs ${targetMetadataError ? "cursor-not-allowed border-slate-200 bg-slate-50 text-slate-400" : "cursor-pointer border-amber-200 bg-amber-50 text-amber-900"}`}>
                <input
                  type="checkbox"
                  className="mt-0.5 size-4"
                  checked={new2027Confirmed}
                  disabled={Boolean(targetMetadataError)}
                  onChange={(event) => setNew2027Confirmed(event.target.checked)}
                />
                <span>
                  I reviewed the NEW_2027 target structure and source evidence. I understand that missing LLO data will not be generated, and no syllabus is saved until I confirm this import.
                </span>
              </label>
            </div>
          )}
        </div>
        <DialogFooter className="border-t border-[#e2ecee] bg-[#f7fafb] px-6 py-4">
          <Button variant="ghost" onClick={close}>Cancel</Button>
          {!result ? (
            <Button className="bg-[#007d84] hover:bg-[#006b71]" disabled={!file || loading} onClick={extract}>
              {loading ? <Loader2 className="size-4 animate-spin" /> : <FileStack className="size-4" />}
              {loading ? "Extracting..." : "Extract"}
            </Button>
          ) : (
            <Button
              className="bg-[#007d84] hover:bg-[#006b71]"
              disabled={
                result.syllabusCount === 0
                || Boolean(
                  allowedCourseCodes
                  && result.items.some((item) =>
                    !new Set(
                      allowedCourseCodes.map((code) =>
                        code.replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, ""),
                      ),
                    ).has(
                      String(item.preview.data?.sourceCourseCode ?? "")
                        .replace(/[^A-Z0-9]/gi, "")
                        .toUpperCase()
                        .replace(/IU$/, ""),
                    ),
                  ),
                )
                || (
                  isNew2027Target
                  && (
                    Boolean(targetMetadataError)
                    || !new2027Confirmed
                  )
                )
              }
              onClick={() => {
                if (isNew2027Target) {
                  const metadataError =
                    validateNew2027TargetTemplateResponse(result)

                  if (metadataError) {
                    toast.error(metadataError)
                    return
                  }

                  if (!new2027Confirmed) {
                    toast.error("Review Template 2027 and confirm before importing.")
                    return
                  }
                }

                onStart(result)
                close()
              }}
            >
              {isNew2027Target ? "Confirm & Import" : "Import"} {result.syllabusCount}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function Stat({ value, label }: { value: number; label: string }) {
  return <div><div className="text-xl font-bold text-[#006f76]">{value}</div><div className="text-[11px] uppercase tracking-wide text-slate-500">{label}</div></div>
}

type SourceEvidenceState =
  | "PARSED"
  | "ABSENT_IN_SOURCE"
  | "UNRESOLVED"
  | "NOT_MODELED"

type SourceEvidenceSummaryState =
  | SourceEvidenceState
  | "MIXED"

type SourceEvidence = {
  state: SourceEvidenceState
  sample?: string
  note?: string
}

type SourceEvidenceSummary = {
  state: SourceEvidenceSummaryState
  summary: string
  example?: string
  affected?: string
  note?: string
}

const recordOf = (
  value: unknown,
): Record<string, unknown> =>
  value !== null
  && typeof value === "object"
  && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {}

const rowsFrom = (
  value: unknown,
): unknown[] =>
  Array.isArray(value)
    ? value
    : []

const hasEvidenceValue = (
  value: unknown,
) => {
  if (
    value === null
    || value === undefined
  ) {
    return false
  }

  if (typeof value === "string") {
    return value.trim().length > 0
  }

  if (Array.isArray(value)) {
    return value.length > 0
  }

  return true
}

const previewValue = (
  value: unknown,
) =>
  String(value ?? "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 160)

const sampleValue = (
  value: unknown,
) =>
  hasEvidenceValue(value)
    ? previewValue(value)
    : undefined

const sampleRows = (
  value: unknown,
  keys: string[],
) => {
  const rows =
    rowsFrom(value)

  for (const row of rows) {
    const source =
      recordOf(row)

    for (const key of keys) {
      const candidate =
        source[key]

      if (hasEvidenceValue(candidate)) {
        return previewValue(candidate)
      }
    }
  }

  return undefined
}

const sourceTemplateFieldLabel = (
  data: SyllabusImportData,
  sectionKey: string,
  fieldKey: string,
) => {
  const section =
    data.templateSections
      ?.find(
        (candidate) =>
          candidate.key === sectionKey,
      )

  const field =
    section?.fields
      ?.find(
        (candidate) =>
          candidate.key === fieldKey,
      )

  return field?.label
}

const sourceProvenanceEntry = (
  data: SyllabusImportData,
  sectionKey: string,
  fieldKey: string,
) => {
  const raw =
    data.sourceProvenance
      ?.[sectionKey]
      ?.[fieldKey]

  if (!raw) {
    return undefined
  }

  const source =
    recordOf(raw)

  const state =
    source.state

  if (
    state !== "PARSED"
    && state !== "ABSENT_IN_SOURCE"
    && state !== "UNRESOLVED"
  ) {
    return undefined
  }

  return {
    state,
    sourceLabel:
      typeof source.sourceLabel === "string"
        && source.sourceLabel.trim()
        ? source.sourceLabel.trim()
        : undefined,
  } as const
}

const evidenceFromProvenance = (
  data: SyllabusImportData,
  provenanceSection: string,
  provenanceKey: string,
  sample?: string,
  description?: string,
): SourceEvidence => {
  const provenance =
    sourceProvenanceEntry(
      data,
      provenanceSection,
      provenanceKey,
    )

  /*
   * Patch 3.1B is authoritative when provenance is present.
   */
  if (provenance) {
    if (provenance.state === "PARSED") {
      return {
        state: "PARSED",
        sample,
      }
    }

    if (provenance.state === "ABSENT_IN_SOURCE") {
      return {
        state: "ABSENT_IN_SOURCE",
        note:
          provenance.sourceLabel
            ? `Source field "${provenance.sourceLabel}" was identified as absent from canonical parsed content.`
            : `The recognized source structure does not expose ${description || "this canonical field"}.`,
      }
    }

    return {
      state: "UNRESOLVED",
      note:
        provenance.sourceLabel
          ? `Source exposes "${provenance.sourceLabel}", but the parser could not resolve a canonical value reliably.`
          : `The source appears to contain ${description || "this field"}, but no reliable canonical value was resolved.`,
    }
  }

  /*
   * Backward-compatible fallback for responses produced by an older
   * backend instance before restart. Parsed semantic data remains valid
   * evidence, but missing-state inference uses source template structure.
   */
  if (sample) {
    return {
      state: "PARSED",
      sample,
    }
  }

  const sourceLabel =
    sourceTemplateFieldLabel(
      data,
      provenanceSection,
      provenanceKey,
    )

  if (sourceLabel) {
    return {
      state: "UNRESOLVED",
      note:
        `Source exposes "${sourceLabel}", but no reliable canonical value was resolved.`,
    }
  }

  return {
    state: "ABSENT_IN_SOURCE",
    note:
      `The recognized source structure does not expose ${description || "this canonical field"}.`,
  }
}

const notModeledEvidence = (
  description: string,
): SourceEvidence => ({
  state: "NOT_MODELED",
  note:
    `${description} is not represented by the current canonical source import model. `
    + "No value is inferred or generated.",
})

const missingLloEvidence =
  (): SourceEvidence => ({
    state: "ABSENT_IN_SOURCE",
    note:
      "No canonical LLO model exists in the CS2026 source import data. "
      + "LLO values stay Missing / Absent in source; nothing is generated.",
  })

const rubricCriteriaRows = (
  data: SyllabusImportData,
) =>
  rowsFrom(data.rubricItems)
    .flatMap(
      (rubric) => {
        const criteria =
          recordOf(rubric).criteria

        return rowsFrom(criteria)
      },
    )

const sourceEvidenceForTargetField = (
  data: SyllabusImportData | null | undefined,
  sectionKey: string,
  fieldKey: string,
): SourceEvidence => {
  if (!data) {
    return {
      state: "UNRESOLVED",
      note:
        "Syllabus preview data is unavailable.",
    }
  }

  /*
   * Template 2027 section 6 is target-only at this stage.
   * Never derive LLO values from CLO/topic data.
   */
  if (sectionKey === "cloLlo") {
    return missingLloEvidence()
  }

  if (sectionKey === "general") {
    const generalValues: Record<string, unknown> = {
      courseCode: data.sourceCourseCode,
      courseName: data.sourceCourseName,
      courseDesignation: data.courseDesignation,
      courseTypes: data.courseTypes,
      semester: data.semester,
      personResponsible: data.personResponsible,
      language: data.language,
      relation: data.relation,
      teachingMethods: data.teachingMethods,
    }

    if (
      Object.prototype.hasOwnProperty.call(
        generalValues,
        fieldKey,
      )
    ) {
      return evidenceFromProvenance(
        data,
        "general",
        fieldKey,
        sampleValue(
          generalValues[fieldKey],
        ),
        fieldKey,
      )
    }

    const workloadValues: Record<string, unknown> = {
      workloadTotal: data.workloadTotal,
      workloadContact: data.workloadContact,
      workloadPrivate: data.workloadPrivate,
      creditPoints: data.creditPoints,
      lectureCredits: data.lectureCredits,
      laboratoryCredits: data.laboratoryCredits,
    }

    if (
      Object.prototype.hasOwnProperty.call(
        workloadValues,
        fieldKey,
      )
    ) {
      return evidenceFromProvenance(
        data,
        "workloadCredit",
        fieldKey,
        sampleValue(
          workloadValues[fieldKey],
        ),
        fieldKey,
      )
    }

    if (fieldKey === "prerequisites") {
      return evidenceFromProvenance(
        data,
        "requirements",
        "prerequisites",
        sampleValue(
          data.prerequisites,
        ),
        "prerequisites",
      )
    }
  }

  if (
    sectionKey === "objectives"
    && fieldKey === "objectives"
  ) {
    return evidenceFromProvenance(
      data,
      "requirements",
      "objectives",
      sampleValue(
        data.objectives,
      ),
      "course objectives",
    )
  }

  if (sectionKey === "clo") {
    if (fieldKey === "mappedPloCodes") {
      return evidenceFromProvenance(
        data,
        "cloPlo",
        "ploCode",
        sampleRows(
          data.cloPloMappings,
          ["ploCode"],
        ),
        "CLO-PLO mapping",
      )
    }

    return evidenceFromProvenance(
      data,
      "clo",
      fieldKey,
      sampleRows(
        data.clos,
        [fieldKey],
      ),
      `CLO ${fieldKey}`,
    )
  }

  if (sectionKey === "content") {
    if (fieldKey === "cloCodes") {
      return evidenceFromProvenance(
        data,
        "topicClo",
        "cloCode",
        sampleRows(
          data.topicCloMappings,
          ["cloCode"],
        ),
        "Topic-CLO mapping",
      )
    }

    return evidenceFromProvenance(
      data,
      "content",
      fieldKey,
      sampleRows(
        data.topics,
        fieldKey === "contentLevel"
          ? ["contentLevel", "teachingLevel"]
          : [fieldKey],
      ),
      `course-content ${fieldKey}`,
    )
  }

  if (sectionKey === "cloPlo") {
    if (fieldKey === "ploCode") {
      return evidenceFromProvenance(
        data,
        "cloPlo",
        "ploCode",
        sampleRows(
          data.cloPloMappings,
          ["ploCode"],
        ),
        "PLO code",
      )
    }

    if (fieldKey === "cloCodes") {
      return evidenceFromProvenance(
        data,
        "cloPlo",
        "cloCode",
        sampleRows(
          data.cloPloMappings,
          ["cloCode"],
        ),
        "contributing CLO code",
      )
    }

    /*
     * The current canonical CLO-PLO import DTO stores mapping codes/value
     * but has no PLO group/description payload.
     */
    if (fieldKey === "ploGroup") {
      return notModeledEvidence(
        "PLO Group",
      )
    }

    if (fieldKey === "ploDescription") {
      return notModeledEvidence(
        "PLO Description",
      )
    }
  }

  if (
    sectionKey === "examination"
    && fieldKey === "examForms"
  ) {
    return evidenceFromProvenance(
      data,
      "examination",
      "examForms",
      sampleValue(
        data.examForms,
      ),
      "examination forms",
    )
  }

  if (sectionKey === "studyRequirements") {
    if (fieldKey === "examRequirements") {
      return evidenceFromProvenance(
        data,
        "examination",
        "examRequirements",
        sampleValue(
          data.examRequirements,
        ),
        "study/examination requirements",
      )
    }

    if (fieldKey === "assessmentPassNote") {
      return evidenceFromProvenance(
        data,
        "assessment",
        "assessmentPassNote",
        sampleValue(
          data.assessmentPassNote,
        ),
        "passing requirement",
      )
    }
  }

  if (sectionKey === "plannedActivities") {
    return evidenceFromProvenance(
      data,
      "plannedActivities",
      fieldKey,
      sampleRows(
        data.weeklyActivities,
        [fieldKey],
      ),
      `planned-activity ${fieldKey}`,
    )
  }

  if (sectionKey === "assessment") {
    if (fieldKey === "cloCode") {
      return evidenceFromProvenance(
        data,
        "assessmentClo",
        "cloCode",
        sampleRows(
          data.assessmentCloMappings,
          ["cloCode"],
        ),
        "Assessment-CLO mapping",
      )
    }

    if (fieldKey === "contributionPercent") {
      return evidenceFromProvenance(
        data,
        "assessmentClo",
        "contributionPercent",
        sampleRows(
          data.assessmentCloMappings,
          ["percentage"],
        ),
        "Assessment-CLO contribution",
      )
    }

    return evidenceFromProvenance(
      data,
      "assessment",
      fieldKey,
      sampleRows(
        data.assessments,
        [fieldKey],
      ),
      `assessment ${fieldKey}`,
    )
  }

  if (sectionKey === "rubrics") {
    if (
      fieldKey === "type"
      || fieldKey === "title"
    ) {
      return evidenceFromProvenance(
        data,
        "rubrics",
        fieldKey,
        sampleRows(
          data.rubricItems,
          [fieldKey],
        ),
        `rubric ${fieldKey}`,
      )
    }

    return evidenceFromProvenance(
      data,
      "rubrics",
      fieldKey,
      sampleRows(
        rubricCriteriaRows(data),
        [fieldKey],
      ),
      `rubric ${fieldKey}`,
    )
  }

  if (sectionKey === "readings") {
    return evidenceFromProvenance(
      data,
      "readings",
      fieldKey,
      sampleRows(
        data.readings,
        [fieldKey],
      ),
      `reading-list ${fieldKey}`,
    )
  }

  return {
    state: "UNRESOLVED",
    note:
      `No source-evidence mapping is defined for ${sectionKey}.${fieldKey}. `
      + "The value is not inferred or generated.",
  }
}

const summarizeSourceEvidence = (
  items: BulkSyllabusImportPreviewResponse["items"],
  sectionKey: string,
  fieldKey: string,
): SourceEvidenceSummary => {
  const rows =
    items.map(
      (item) => ({
        code:
          item.preview.data?.sourceCourseCode
          || `Pages ${item.startPage}-${item.endPage}`,
        evidence:
          sourceEvidenceForTargetField(
            item.preview.data,
            sectionKey,
            fieldKey,
          ),
      }),
    )

  const total =
    rows.length

  const parsed =
    rows.filter(
      (item) =>
        item.evidence.state === "PARSED",
    ).length

  const absent =
    rows.filter(
      (item) =>
        item.evidence.state === "ABSENT_IN_SOURCE",
    ).length

  const unresolved =
    rows.filter(
      (item) =>
        item.evidence.state === "UNRESOLVED",
    ).length

  const notModeled =
    rows.filter(
      (item) =>
        item.evidence.state === "NOT_MODELED",
    ).length

  if (
    total > 0
    && notModeled === total
  ) {
    return {
      state: "NOT_MODELED",
      summary:
        "Canonical source model unavailable · nothing generated",
      note:
        rows[0]?.evidence.note,
    }
  }

  const state: SourceEvidenceSummaryState =
    total > 0
    && parsed === total
      ? "PARSED"
      : total > 0
        && absent === total
        ? "ABSENT_IN_SOURCE"
        : total > 0
          && unresolved === total
          ? "UNRESOLVED"
          : "MIXED"

  const example =
    rows.find(
      (item) =>
        item.evidence.state === "PARSED"
        && item.evidence.sample,
    )?.evidence.sample

  const affectedCodes =
    Array.from(
      new Set(
        rows
          .filter(
            (item) =>
              item.evidence.state !== "PARSED"
              && item.evidence.state !== "NOT_MODELED",
          )
          .map(
            (item) =>
              item.code,
          ),
      ),
    )

  const affectedLimit =
    8

  const showAffected =
    state === "MIXED"
    || unresolved > 0

  const affected =
    showAffected
    && affectedCodes.length > 0
      ? (
          affectedCodes
            .slice(
              0,
              affectedLimit,
            )
            .join(", ")
          + (
            affectedCodes.length > affectedLimit
              ? ` +${affectedCodes.length - affectedLimit} more`
              : ""
          )
        )
      : undefined

  const note =
    rows.find(
      (item) =>
        item.evidence.state !== "PARSED"
        && item.evidence.note,
    )?.evidence.note

  return {
    state,
    summary:
      `${parsed}/${total} parsed · ${absent} absent · ${unresolved} unresolved`,
    example,
    affected,
    note,
  }
}

const evidenceLabel = (
  state: SourceEvidenceSummaryState,
) => {
  if (state === "PARSED") {
    return "Parsed"
  }

  if (state === "ABSENT_IN_SOURCE") {
    return "Missing"
  }

  if (state === "UNRESOLVED") {
    return "Unresolved"
  }

  if (state === "NOT_MODELED") {
    return "Not modeled"
  }

  return "Mixed"
}

const evidenceClassName = (
  state: SourceEvidenceSummaryState,
) => {
  if (state === "PARSED") {
    return "bg-emerald-100 text-emerald-700"
  }

  if (state === "ABSENT_IN_SOURCE") {
    return "bg-amber-100 text-amber-800"
  }

  if (state === "UNRESOLVED") {
    return "bg-rose-100 text-rose-700"
  }

  if (state === "NOT_MODELED") {
    return "bg-slate-100 text-slate-600"
  }

  return "bg-slate-100 text-slate-700"
}
