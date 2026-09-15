import { Fragment, type ReactNode } from "react"
import { Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  parseStandardSyllabusNotes,
} from "@/components/syllabus/StandardSyllabusFormSections"
import type { Syllabus } from "@/types/syllabus"
import {
  inferSyllabusComparisonTemplateProfile,
  mergeSyllabusComparisonTemplateProfiles,
  readSyllabusComparisonTemplateProfile,
} from "@/lib/syllabusComparisonTemplate"
import type {
  SyllabusComparisonSectionDefinition,
  SyllabusComparisonSectionKey,
} from "@/lib/syllabusComparisonTemplate"
import type {
  AssessmentCloMappingDiff,
  AssessmentDiff,
  CloDiff,
  CloPloMappingDiff,
  FieldDiff,
  ListDiff,
  PlannedActivityDiff,
  readingsDiff,
  SemanticDiffResult,
  SemanticSyllabusDiffResponse,
  SyllabusDiffResponse,
  TopicCloMappingDiff,
  TopicDiff,
} from "@/types/syllabusDiff"

import ComparisonRow, {
  type ComparisonState,
} from "./ComparisonRow"
import ComparisonSection from "./ComparisonSection"

type Props = {
  oldSyllabus: Syllabus
  newSyllabus: Syllabus
  structuralDiff: SyllabusDiffResponse

  semanticDiff?: SemanticSyllabusDiffResponse
  semanticLoading?: boolean
  semanticError?: string | null

  onAnalyzeSemantic: () => void
}

type ScalarDiffMap =
  | Record<string, FieldDiff>
  | undefined

type StructuralItem = {
  changes?: Record<string, FieldDiff>
}

type ListField<T> = {
  key: string
  label: string
  value: (item: T) => unknown
}

type ListRowsProps<T extends StructuralItem> = {
  title: string
  diff?: ListDiff<T>
  fields: ListField<T>[]
  identity: (item: T, index: number) => string
  semanticFor?: (
    item: T,
    fieldKey: string,
  ) => SemanticDiffResult | undefined
  semanticKeys?: Set<string>
}

const display = (value: unknown): string => {
  if (Array.isArray(value)) {
    const values = value
      .map((item) => String(item ?? "").trim())
      .filter(Boolean)

    return values.length > 0
      ? values.join(", ")
      : "—"
  }

  const text = String(value ?? "").trim()
  return text || "—"
}

const normalized = (value: unknown) =>
  String(value ?? "")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase()

const valuesEqual = (
  oldValue: unknown,
  newValue: unknown,
) =>
  normalized(oldValue)
  === normalized(newValue)

const hasValue = (value: unknown) =>
  String(value ?? "").trim().length > 0

const semanticState = (
  oldValue: unknown,
  newValue: unknown,
  result?: SemanticDiffResult,
): ComparisonState => {
  if (valuesEqual(oldValue, newValue)) {
    return "UNCHANGED"
  }

  if (!result || result.requiresAi) {
    return "PENDING_AI"
  }

  switch (result.classification) {
    case "MINOR_REWORDING":
      return "REWORDING"

    case "MEANINGFUL_CHANGE":
      return "MODIFIED"

    case "NO_MEANINGFUL_CHANGE":
      return "UNCHANGED"

    default:
      return "PENDING_AI"
  }
}

const scalarState = (
  diff: ScalarDiffMap,
  key: string,
  oldValue: unknown,
  newValue: unknown,
  options?: {
    required?: boolean
    semantic?: SemanticDiffResult
    semanticField?: boolean
  },
): ComparisonState => {
  /*
   * Backend Task 2 is the source of truth for structural equality.
   * If the canonical backend did not emit this key, the field is
   * structurally unchanged even if display formatting differs.
   */
  if (!diff?.[key]) {
    return "UNCHANGED"
  }

  if (
    options?.required
    && hasValue(oldValue) !== hasValue(newValue)
  ) {
    return "DATA_MISSING"
  }

  if (options?.semanticField) {
    return semanticState(
      oldValue,
      newValue,
      options.semantic,
    )
  }

  return "MODIFIED"
}

const formatCourseTypes = (
  value: string | null | undefined,
) => {
  const raw = String(value ?? "").trim()

  if (!raw) {
    return "—"
  }

  try {
    const parsed = JSON.parse(raw)

    if (Array.isArray(parsed)) {
      const values = parsed
        .map((item) => String(item ?? "").trim())
        .filter(Boolean)

      return values.length > 0
        ? values.join(", ")
        : "—"
    }
  } catch {
    /*
     * Legacy comma-separated/plain text is valid display input.
     * Do not alter the persisted content.
     */
  }

  return raw
}

const effectiveCreditPoints = (
  syllabus: Syllabus,
  supplemental: ReturnType<
    typeof parseStandardSyllabusNotes
  >,
) => {
  if (supplemental.creditPoints.trim()) {
    return supplemental.creditPoints
  }

  if (
    syllabus.creditTheory == null
    && syllabus.creditLab == null
  ) {
    return ""
  }

  return String(
    (syllabus.creditTheory ?? 0)
    + (syllabus.creditLab ?? 0),
  )
}

const effectiveLectureCredits = (
  syllabus: Syllabus,
  supplemental: ReturnType<
    typeof parseStandardSyllabusNotes
  >,
) => {
  if (supplemental.lectureCredits.trim()) {
    return supplemental.lectureCredits
  }

  return syllabus.creditTheory == null
    ? ""
    : String(syllabus.creditTheory)
}

const effectiveLaboratoryCredits = (
  syllabus: Syllabus,
  supplemental: ReturnType<
    typeof parseStandardSyllabusNotes
  >,
) => {
  if (supplemental.laboratoryCredits.trim()) {
    return supplemental.laboratoryCredits
  }

  return syllabus.creditLab == null
    ? ""
    : String(syllabus.creditLab)
}

const semanticByExactId = (
  semanticDiff:
    | SemanticSyllabusDiffResponse
    | undefined,
  itemId: string,
) =>
  semanticDiff?.items?.find(
    (item) =>
      item.itemId.trim().toLowerCase()
      === itemId.trim().toLowerCase(),
  )

function SemanticDetail({
  result,
}: {
  result?: SemanticDiffResult
}) {
  if (!result) {
    return (
      <div className="flex flex-wrap items-center gap-2 text-xs text-amber-700">
        <span className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 font-bold">
          Needs AI analysis
        </span>

        <span>
          Text differs, but academic meaning has not been evaluated yet.
        </span>
      </div>
    )
  }

  if (result.requiresAi) {
    return (
      <div className="flex flex-wrap items-center gap-2 text-xs text-amber-700">
        <span className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 font-bold">
          Needs AI analysis
        </span>

        <span>
          Semantic interpretation is still unresolved.
        </span>
      </div>
    )
  }

  const classification =
    String(result.classification ?? "")
      .trim()
      .toUpperCase()

  const classificationLabel =
    classification === "MEANINGFUL_CHANGE"
      ? "Meaningful Change"
      : classification === "MINOR_REWORDING"
        ? "Minor Rewording"
        : classification === "NO_MEANINGFUL_CHANGE"
          ? "No Meaningful Change"
          : "Unknown Classification"

  const badgeClass =
    classification === "MEANINGFUL_CHANGE"
      ? "border-red-200 bg-red-50 text-red-700"
      : classification === "MINOR_REWORDING"
        ? "border-blue-200 bg-blue-50 text-blue-700"
        : "border-emerald-200 bg-emerald-50 text-emerald-700"

  return (
    <details className="group">
      <summary className="flex cursor-pointer list-none flex-wrap items-center gap-2 text-xs">
        <span
          className={`rounded-full border px-2.5 py-1 font-bold ${badgeClass}`}
        >
          {classificationLabel}
        </span>

        {result.changeNature && (
          <span className="rounded-full border border-slate-200 bg-white px-2.5 py-1 font-semibold text-slate-600">
            {result.changeNature.replaceAll("_", " ")}
          </span>
        )}

        {result.significance && (
          <span className="rounded-full border border-slate-200 bg-white px-2.5 py-1 font-semibold text-slate-600">
            {result.significance} significance
          </span>
        )}

        <span className="font-semibold text-[#007d84]">
          View AI explanation
        </span>
      </summary>

      <div className="mt-3 grid gap-3 lg:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-3">
          <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
            Old academic meaning
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-700">
            {display(result.oldMeaning)}
          </p>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-3">
          <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
            New academic meaning
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-700">
            {display(result.newMeaning)}
          </p>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-3 lg:col-span-2">
          <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
            AI analysis
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-700">
            {display(result.summary)}
          </p>
        </div>
      </div>
    </details>
  )
}

function ScalarRow({
  field,
  diff,
  fieldKey,
  oldValue,
  newValue,
  required = false,
  semanticResult,
  semanticField = false,
}: {
  field: ReactNode
  diff: ScalarDiffMap
  fieldKey: string
  oldValue: unknown
  newValue: unknown
  required?: boolean
  semanticResult?: SemanticDiffResult
  semanticField?: boolean
}) {
  const changed =
    Boolean(diff?.[fieldKey])

  return (
    <ComparisonRow
      field={field}
      oldValue={display(oldValue)}
      newValue={display(newValue)}
      state={scalarState(
        diff,
        fieldKey,
        oldValue,
        newValue,
        {
          required,
          semantic: semanticResult,
          semanticField,
        },
      )}
      detail={
        semanticField
        && changed
        && !valuesEqual(oldValue, newValue)
          ? (
              <SemanticDetail
                result={semanticResult}
              />
            )
          : undefined
      }
    />
  )
}

function itemFieldLabel(
  identity: string,
  fieldLabel: string,
  stateLabel?: string,
) {
  return (
    <div className="space-y-1">
      <p className="font-bold text-slate-700">
        {identity}
      </p>

      <p className="text-[11px] text-slate-500">
        {fieldLabel}
        {stateLabel
          ? ` · ${stateLabel}`
          : ""}
      </p>
    </div>
  )
}

function ListRows<T extends StructuralItem>({
  title,
  diff,
  fields,
  identity,
  semanticFor,
  semanticKeys = new Set<string>(),
}: ListRowsProps<T>) {
  const added = diff?.added ?? []
  const removed = diff?.removed ?? []
  const modified = diff?.modified ?? []

  if (
    added.length === 0
    && removed.length === 0
    && modified.length === 0
  ) {
    return (
      <ComparisonRow
        field={`${title} changes`}
        oldValue="No structural changes"
        newValue="No structural changes"
        state="UNCHANGED"
      />
    )
  }

  return (
    <>
      {removed.map((item, itemIndex) => {
        const label =
          identity(item, itemIndex)

        return (
          <Fragment
            key={`${title}-removed-${itemIndex}-${label}`}
          >
            {fields.map((field) => (
              <ComparisonRow
                key={`${title}-removed-${itemIndex}-${field.key}`}
                field={itemFieldLabel(
                  label,
                  field.label,
                  "Removed",
                )}
                oldValue={display(
                  field.value(item),
                )}
                newValue="—"
                state="REMOVED"
              />
            ))}
          </Fragment>
        )
      })}

      {modified.map((item, itemIndex) => {
        const label =
          identity(item, itemIndex)

        const changes =
          item.changes ?? {}

        const knownKeys =
          new Set(
            fields.map((field) => field.key),
          )

        const changedFields =
          fields.filter(
            (field) =>
              Boolean(changes[field.key]),
          )

        const unknownChangeKeys =
          Object.keys(changes)
            .filter(
              (key) =>
                !knownKeys.has(key),
            )

        return (
          <Fragment
            key={`${title}-modified-${itemIndex}-${label}`}
          >
            {changedFields.map((field) => {
              const change =
                changes[field.key]

              const semanticResult =
                semanticFor?.(
                  item,
                  field.key,
                )

              const isSemantic =
                semanticKeys.has(field.key)

              return (
                <ComparisonRow
                  key={`${title}-modified-${itemIndex}-${field.key}`}
                  field={itemFieldLabel(
                    label,
                    field.label,
                    "Modified",
                  )}
                  oldValue={display(
                    change?.oldValue,
                  )}
                  newValue={display(
                    change?.newValue,
                  )}
                  state={
                    isSemantic
                      ? semanticState(
                          change?.oldValue,
                          change?.newValue,
                          semanticResult,
                        )
                      : "MODIFIED"
                  }
                  detail={
                    isSemantic
                      ? (
                          <SemanticDetail
                            result={
                              semanticResult
                            }
                          />
                        )
                      : undefined
                  }
                />
              )
            })}

            {unknownChangeKeys.map(
              (fieldKey) => {
                const change =
                  changes[fieldKey]

                return (
                  <ComparisonRow
                    key={`${title}-modified-${itemIndex}-unknown-${fieldKey}`}
                    field={itemFieldLabel(
                      label,
                      fieldKey,
                      "Modified",
                    )}
                    oldValue={display(
                      change?.oldValue,
                    )}
                    newValue={display(
                      change?.newValue,
                    )}
                    state="MODIFIED"
                  />
                )
              },
            )}
          </Fragment>
        )
      })}

      {added.map((item, itemIndex) => {
        const label =
          identity(item, itemIndex)

        return (
          <Fragment
            key={`${title}-added-${itemIndex}-${label}`}
          >
            {fields.map((field) => (
              <ComparisonRow
                key={`${title}-added-${itemIndex}-${field.key}`}
                field={itemFieldLabel(
                  label,
                  field.label,
                  "Added",
                )}
                oldValue="—"
                newValue={display(
                  field.value(item),
                )}
                state="ADDED"
              />
            ))}
          </Fragment>
        )
      })}
    </>
  )
}

const cloFields: ListField<CloDiff>[] = [
  {
    key: "code",
    label: "Code",
    value: (item) => item.code,
  },
  {
    key: "description",
    label: "Description",
    value: (item) => item.description,
  },
  {
    key: "descriptionVn",
    label: "Vietnamese Description",
    value: (item) => item.descriptionVn,
  },
  {
    key: "competencyLevel",
    label: "Competency Level",
    value: (item) => item.competencyLevel,
  },
  {
    key: "bloomLevel",
    label: "Bloom Level",
    value: (item) => item.bloomLevel,
  },
  {
    key: "orderIndex",
    label: "Display Order",
    value: (item) => item.orderIndex,
  },
  {
    key: "plos",
    label: "Mapped PLOs",
    value: (item) => item.plos,
  },
]

const topicFields: ListField<TopicDiff>[] = [
  {
    key: "name",
    label: "Topic",
    value: (item) => item.name,
  },
  {
    key: "nameVn",
    label: "Vietnamese Topic",
    value: (item) => item.nameVn,
  },
  {
    key: "weekNumber",
    label: "Week",
    value: (item) => item.weekNumber,
  },
  {
    key: "orderInWeek",
    label: "Order in Week",
    value: (item) => item.orderInWeek,
  },
  {
    key: "teachingHours",
    label: "Teaching Hours",
    value: (item) => item.teachingHours,
  },
  {
    key: "labHours",
    label: "Laboratory Hours",
    value: (item) => item.labHours,
  },
  {
    key: "selfStudyHours",
    label: "Self-study Hours",
    value: (item) => item.selfStudyHours,
  },
  {
    key: "topicType",
    label: "Topic Type",
    value: (item) => item.topicType,
  },
  {
    key: "teachingMethod",
    label: "Teaching Method",
    value: (item) => item.teachingMethod,
  },
  {
    key: "learningActivity",
    label: "Learning Activity",
    value: (item) => item.learningActivity,
  },
  {
    key: "assessments",
    label: "Mapped Assessments",
    value: (item) => item.assessments,
  },
  {
    key: "resources",
    label: "Resources",
    value: (item) => item.resources,
  },
  {
    key: "contentWeight",
    label: "Content Weight",
    value: (item) => item.contentWeight,
  },
  {
    key: "contentLevel",
    label: "Content Level (I/T/U)",
    value: (item) => item.contentLevel,
  },
]

const cloPloFields:
  ListField<CloPloMappingDiff>[] = [
    {
      key: "cloCode",
      label: "CLO",
      value: (item) => item.cloCode,
    },
    {
      key: "ploCode",
      label: "PLO",
      value: (item) => item.ploCode,
    },
    {
      key: "level",
      label: "Contribution Level",
      value: (item) => item.level,
    },
    {
      key: "contributionWeight",
      label: "Contribution Weight",
      value: (item) =>
        item.contributionWeight,
    },
    {
      key: "notes",
      label: "Mapping Notes",
      value: (item) => item.notes,
    },
  ]

const topicCloFields:
  ListField<TopicCloMappingDiff>[] = [
    {
      key: "topicName",
      label: "Topic",
      value: (item) => item.topicName,
    },
    {
      key: "weekNumber",
      label: "Week",
      value: (item) => item.weekNumber,
    },
    {
      key: "orderInWeek",
      label: "Order in Week",
      value: (item) => item.orderInWeek,
    },
    {
      key: "cloCode",
      label: "CLO",
      value: (item) => item.cloCode,
    },
    {
      key: "teachingLevel",
      label: "Teaching Level",
      value: (item) =>
        item.teachingLevel,
    },
  ]

const plannedActivityFields:
  ListField<PlannedActivityDiff>[] = [
    {
      key: "week",
      label: "Week",
      value: (item) => item.week,
    },
    {
      key: "topic",
      label: "Topic",
      value: (item) => item.topic,
    },
    {
      key: "clo",
      label: "CLOs",
      value: (item) => item.clo,
    },
    {
      key: "assessments",
      label: "Assessments",
      value: (item) => item.assessments,
    },
    {
      key: "learningActivities",
      label: "Learning Activities",
      value: (item) =>
        item.learningActivities,
    },
    {
      key: "resources",
      label: "Resources",
      value: (item) => item.resources,
    },
  ]

const assessmentFields:
  ListField<AssessmentDiff>[] = [
    {
      key: "name",
      label: "Assessment",
      value: (item) => item.name,
    },
    {
      key: "nameVn",
      label: "Vietnamese Name",
      value: (item) => item.nameVn,
    },
    {
      key: "assessmentType",
      label: "Assessment Type",
      value: (item) =>
        item.assessmentType,
    },
    {
      key: "weightPercent",
      label: "Weight (%)",
      value: (item) =>
        item.weightPercent,
    },
    {
      key: "minScore",
      label: "Minimum Score",
      value: (item) => item.minScore,
    },
    {
      key: "maxScore",
      label: "Maximum Score",
      value: (item) => item.maxScore,
    },
    {
      key: "orderIndex",
      label: "Display Order",
      value: (item) => item.orderIndex,
    },
  ]

const assessmentCloFields:
  ListField<AssessmentCloMappingDiff>[] = [
    {
      key: "assessmentName",
      label: "Assessment",
      value: (item) =>
        item.assessmentName,
    },
    {
      key: "orderIndex",
      label: "Assessment Order",
      value: (item) => item.orderIndex,
    },
    {
      key: "cloCode",
      label: "CLO",
      value: (item) => item.cloCode,
    },
    {
      key: "contributionPercent",
      label: "Contribution (%)",
      value: (item) =>
        item.contributionPercent,
    },
  ]

const readingFields:
  ListField<readingsDiff>[] = [
    {
      key: "title",
      label: "Title",
      value: (item) => item.title,
    },
    {
      key: "author",
      label: "Author",
      value: (item) => item.author,
    },
    {
      key: "publisher",
      label: "Publisher",
      value: (item) => item.publisher,
    },
    {
      key: "year",
      label: "Publication Year",
      value: (item) => item.year,
    },
    {
      key: "edition",
      label: "Edition",
      value: (item) => item.edition,
    },
    {
      key: "isbn",
      label: "ISBN",
      value: (item) => item.isbn,
    },
    {
      key: "url",
      label: "URL",
      value: (item) => item.url,
    },
    {
      key: "bookType",
      label: "Resource Type",
      value: (item) => item.bookType,
    },
    {
      key: "usageType",
      label: "Usage Type",
      value: (item) => item.usageType,
    },
    {
      key: "orderIndex",
      label: "Display Order",
      value: (item) => item.orderIndex,
    },
  ]

export default function SyllabusComparisonView({
  oldSyllabus,
  newSyllabus,
  structuralDiff,
  semanticDiff,
  semanticLoading = false,
  semanticError,
  onAnalyzeSemantic,
}: Props) {
  const oldSupplemental =
    parseStandardSyllabusNotes(
      oldSyllabus.notes,
    )

  const newSupplemental =
    parseStandardSyllabusNotes(
      newSyllabus.notes,
    )

  const oldTemplate =
    readSyllabusComparisonTemplateProfile(
      oldSyllabus.notes,
    )
    ?? inferSyllabusComparisonTemplateProfile(
      oldSyllabus,
      structuralDiff,
    )

  const newTemplate =
    readSyllabusComparisonTemplateProfile(
      newSyllabus.notes,
    )
    ?? inferSyllabusComparisonTemplateProfile(
      newSyllabus,
      structuralDiff,
    )

  const comparisonTemplate =
    mergeSyllabusComparisonTemplateProfiles(
      oldTemplate,
      newTemplate,
    )

  const sectionMap =
    new Map<
      SyllabusComparisonSectionKey,
      SyllabusComparisonSectionDefinition
    >(
      comparisonTemplate.sections.map(
        (section) => [
          section.key,
          section,
        ],
      ),
    )

  const sectionDefinition = (
    key: SyllabusComparisonSectionKey,
  ) =>
    sectionMap.get(key)

  const hasField = (
    sectionKey: SyllabusComparisonSectionKey,
    fieldKey: string,
  ) =>
    Boolean(
      sectionDefinition(sectionKey)
        ?.fields
        .some(
          (field) =>
            field.key === fieldKey,
        ),
    )

  const fieldLabel = (
    sectionKey: SyllabusComparisonSectionKey,
    fieldKey: string,
    fallback: string,
  ) =>
    sectionDefinition(sectionKey)
      ?.fields
      .find(
        (field) =>
          field.key === fieldKey,
      )
      ?.label
    || fallback

  const listFieldsFor = <T,>(
    sectionKey: SyllabusComparisonSectionKey,
    available: ListField<T>[],
  ): ListField<T>[] => {
    const definition =
      sectionDefinition(sectionKey)

    if (!definition) {
      return []
    }

    const availableByKey =
      new Map(
        available.map(
          (field) => [
            field.key,
            field,
          ],
        ),
      )

    return definition.fields.flatMap(
      (templateField) => {
        const availableField =
          availableByKey.get(
            templateField.key,
          )

        if (!availableField) {
          return []
        }

        return [{
          ...availableField,
          label:
            templateField.label
            || availableField.label,
        }]
      },
    )
  }

  const visibleScalarCount = (
    sectionKey: SyllabusComparisonSectionKey,
    diff: ScalarDiffMap,
  ) => {
    const definition =
      sectionDefinition(sectionKey)

    if (!definition) {
      return 0
    }

    const visible =
      new Set(
        definition.fields.map(
          (field) => field.key,
        ),
      )

    return Object.keys(diff ?? {})
      .filter(
        (key) =>
          visible.has(key)
          && key !== "academicYear"
          && key !== "program",
      )
      .length
  }

  const visibleListCount = <
    T extends StructuralItem,
  >(
    sectionKey: SyllabusComparisonSectionKey,
    diff?: ListDiff<T>,
  ) => {
    const definition =
      sectionDefinition(sectionKey)

    if (!definition) {
      return 0
    }

    const visible =
      new Set(
        definition.fields.map(
          (field) => field.key,
        ),
      )

    const added =
      diff?.added?.length ?? 0

    const removed =
      diff?.removed?.length ?? 0

    const modified =
      (diff?.modified ?? [])
        .filter(
          (item) =>
            Object.keys(
              item.changes ?? {},
            ).some(
              (key) =>
                visible.has(key),
            ),
        )
        .length

    return added + removed + modified
  }

  const objectiveSemantic =
    semanticByExactId(
      semanticDiff,
      "general.objectives",
    )

  const teachingSemantic =
    semanticByExactId(
      semanticDiff,
      "general.teachingMethods",
    )

  const examRequirementSemantic =
    semanticByExactId(
      semanticDiff,
      "general.examRequirements",
    )

  const meaningfulCount =
    (semanticDiff?.items ?? [])
      .filter(
        (item) =>
          item.classification
          === "MEANINGFUL_CHANGE",
      )
      .length

  const rewordingCount =
    (semanticDiff?.items ?? [])
      .filter(
        (item) =>
          item.classification
          === "MINOR_REWORDING",
      )
      .length

  const unresolvedCount =
    (semanticDiff?.items ?? [])
      .filter(
        (item) => item.requiresAi,
      )
      .length

  const structuralChangeCount =
    visibleScalarCount(
      "general",
      structuralDiff.generalInfoDiff,
    )
    + visibleScalarCount(
      "workloadCredit",
      structuralDiff.workloadCreditDiff,
    )
    + visibleScalarCount(
      "requirements",
      structuralDiff.requirementsDiff,
    )
    + visibleScalarCount(
      "content",
      structuralDiff.contentDiff,
    )
    + visibleScalarCount(
      "assessment",
      structuralDiff.assessmentInfoDiff,
    )
    + visibleScalarCount(
      "examination",
      structuralDiff.examinationDiff,
    )
    + visibleScalarCount(
      "revision",
      structuralDiff.revisionInfoDiff,
    )
    + visibleListCount(
      "clo",
      structuralDiff.cloDiff,
    )
    + visibleListCount(
      "cloPlo",
      structuralDiff.cloPloDiff,
    )
    + visibleListCount(
      "content",
      structuralDiff.topicDiff,
    )
    + visibleListCount(
      "topicClo",
      structuralDiff.topicCloDiff,
    )
    + visibleListCount(
      "plannedActivities",
      structuralDiff.plannedActivityDiff,
    )
    + visibleListCount(
      "assessment",
      structuralDiff.assessmentDiff,
    )
    + visibleListCount(
      "assessmentClo",
      structuralDiff.assessmentCloDiff,
    )
    + visibleListCount(
      "readings",
      structuralDiff.readingsDiff,
    )

  const oldCohort =
    oldSyllabus.cohortName
    || oldSyllabus.academicYear
    || "Unknown cohort"

  const newCohort =
    newSyllabus.cohortName
    || newSyllabus.academicYear
    || "Unknown cohort"

  const oldProgram =
    oldSyllabus.programCode
    || oldSyllabus.programName
    || "—"

  const newProgram =
    newSyllabus.programCode
    || newSyllabus.programName
    || "—"

  const oldColumnLabel =
    `${oldCohort} · ${oldSyllabus.versionLabel}`

  const newColumnLabel =
    `${newCohort} · ${newSyllabus.versionLabel}`

  const oldTemplateLabel =
    oldTemplate.sourceFileName
    || (
      oldTemplate.source === "STANDARD_FORM"
        ? "Standard syllabus form"
        : "Legacy imported syllabus (inferred)"
    )

  const newTemplateLabel =
    newTemplate.sourceFileName
    || (
      newTemplate.source === "STANDARD_FORM"
        ? "Standard syllabus form"
        : "Legacy imported syllabus (inferred)"
    )

  const getCloSemantic = (
    item: CloDiff,
    fieldKey: string,
  ) => {
    if (
      fieldKey !== "description"
      || !item.code
    ) {
      return undefined
    }

    const code =
      item.code.trim().toLowerCase()

    return semanticByExactId(
      semanticDiff,
      `clo.${code}.description`,
    )
  }

  const renderGeneralField = (
    key: string,
  ): ReactNode => {
    const label = (
      fallback: string,
    ) =>
      fieldLabel(
        "general",
        key,
        fallback,
      )

    switch (key) {
      case "courseCode":
        return (
          <ScalarRow
            key={key}
            field={label("Course Code")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.courseCode}
            newValue={newSyllabus.courseCode}
            required
          />
        )

      case "courseName":
        return (
          <ScalarRow
            key={key}
            field={label("Course Name")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.courseName}
            newValue={newSyllabus.courseName}
            required
          />
        )

      case "courseNameVn":
        return (
          <ScalarRow
            key={key}
            field={label("Course Name (Vietnamese)")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.courseNameVn}
            newValue={newSyllabus.courseNameVn}
          />
        )

      case "courseDesignation":
        return (
          <ScalarRow
            key={key}
            field={label("Course Designation")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.courseDesignation}
            newValue={newSyllabus.courseDesignation}
            required
          />
        )

      case "courseTypes":
        return (
          <ScalarRow
            key={key}
            field={label("Course Type")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={formatCourseTypes(oldSyllabus.courseTypes)}
            newValue={formatCourseTypes(newSyllabus.courseTypes)}
            required
          />
        )

      case "semester":
        return (
          <ScalarRow
            key={key}
            field={label("Semester")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.semester}
            newValue={newSyllabus.semester}
            required
          />
        )

      case "personResponsible":
        return (
          <ScalarRow
            key={key}
            field={label("Person Responsible for the Course")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSupplemental.personResponsible}
            newValue={newSupplemental.personResponsible}
            required
          />
        )

      case "language":
        return (
          <ScalarRow
            key={key}
            field={label("Language of Instruction")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.language}
            newValue={newSyllabus.language}
            required
          />
        )

      case "relation":
        return (
          <ScalarRow
            key={key}
            field={label("Relation to Curriculum")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.relation}
            newValue={newSyllabus.relation}
          />
        )

      case "teachingMethods":
        return (
          <ScalarRow
            key={key}
            field={label("Teaching Methods")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.teachingMethods}
            newValue={newSyllabus.teachingMethods}
            semanticField
            semanticResult={teachingSemantic}
          />
        )

      case "major":
        return (
          <ScalarRow
            key={key}
            field={label("Major / Academic Area")}
            diff={structuralDiff.generalInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.major}
            newValue={newSyllabus.major}
          />
        )

      default:
        return null
    }
  }

  const renderWorkloadField = (
    key: string,
  ): ReactNode => {
    const label = (
      fallback: string,
    ) =>
      fieldLabel(
        "workloadCredit",
        key,
        fallback,
      )

    switch (key) {
      case "workloadTotal":
        return (
          <ScalarRow
            key={key}
            field={label("Total Workload")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={oldSyllabus.workloadTotal}
            newValue={newSyllabus.workloadTotal}
            required
          />
        )

      case "workloadContact":
        return (
          <ScalarRow
            key={key}
            field={label("Contact Hours")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={oldSyllabus.workloadContact}
            newValue={newSyllabus.workloadContact}
            required
          />
        )

      case "workloadPrivate":
        return (
          <ScalarRow
            key={key}
            field={label("Self-study / Private Study Hours")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={oldSyllabus.workloadPrivate}
            newValue={newSyllabus.workloadPrivate}
            required
          />
        )

      case "workloadStudentResponsibility":
        return (
          <ScalarRow
            key={key}
            field={label("Student Responsibility")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={oldSupplemental.workloadStudentResponsibility}
            newValue={newSupplemental.workloadStudentResponsibility}
          />
        )

      case "creditPoints":
        return (
          <ScalarRow
            key={key}
            field={label("Credit Points — Total")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={effectiveCreditPoints(oldSyllabus, oldSupplemental)}
            newValue={effectiveCreditPoints(newSyllabus, newSupplemental)}
          />
        )

      case "lectureCredits":
        return (
          <ScalarRow
            key={key}
            field={label("Credits — Lecture")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={effectiveLectureCredits(oldSyllabus, oldSupplemental)}
            newValue={effectiveLectureCredits(newSyllabus, newSupplemental)}
          />
        )

      case "laboratoryCredits":
        return (
          <ScalarRow
            key={key}
            field={label("Credits — Laboratory")}
            diff={structuralDiff.workloadCreditDiff}
            fieldKey={key}
            oldValue={effectiveLaboratoryCredits(oldSyllabus, oldSupplemental)}
            newValue={effectiveLaboratoryCredits(newSyllabus, newSupplemental)}
          />
        )

      default:
        return null
    }
  }

  const renderRequirementField = (
    key: string,
  ): ReactNode => {
    switch (key) {
      case "prerequisites":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("requirements", key, "Required / Recommended Prerequisites")}
            diff={structuralDiff.requirementsDiff}
            fieldKey={key}
            oldValue={oldSyllabus.prerequisites}
            newValue={newSyllabus.prerequisites}
            required
          />
        )

      case "objectives":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("requirements", key, "Course Objectives")}
            diff={structuralDiff.requirementsDiff}
            fieldKey={key}
            oldValue={oldSyllabus.objectives}
            newValue={newSyllabus.objectives}
            required
            semanticField
            semanticResult={objectiveSemantic}
          />
        )

      default:
        return null
    }
  }

  const renderExaminationField = (
    key: string,
  ): ReactNode => {
    switch (key) {
      case "examForms":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("examination", key, "Examination Forms")}
            diff={structuralDiff.examinationDiff}
            fieldKey={key}
            oldValue={oldSyllabus.examForms}
            newValue={newSyllabus.examForms}
            required
          />
        )

      case "examRequirements":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("examination", key, "Study / Examination Requirements")}
            diff={structuralDiff.examinationDiff}
            fieldKey={key}
            oldValue={oldSyllabus.examRequirements}
            newValue={newSyllabus.examRequirements}
            required
            semanticField
            semanticResult={examRequirementSemantic}
          />
        )

      default:
        return null
    }
  }

  const renderRevisionField = (
    key: string,
  ): ReactNode => {
    switch (key) {
      case "dateRevised":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("revision", key, "Date Revised")}
            diff={structuralDiff.revisionInfoDiff}
            fieldKey={key}
            oldValue={oldSupplemental.dateRevised}
            newValue={newSupplemental.dateRevised}
          />
        )

      case "internalNotes":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("revision", key, "Internal Notes")}
            diff={structuralDiff.revisionInfoDiff}
            fieldKey={key}
            oldValue={oldSupplemental.internalNotes}
            newValue={newSupplemental.internalNotes}
          />
        )

      case "changeSummary":
        return (
          <ScalarRow
            key={key}
            field={fieldLabel("revision", key, "Change Summary")}
            diff={structuralDiff.revisionInfoDiff}
            fieldKey={key}
            oldValue={oldSyllabus.changeSummary}
            newValue={newSyllabus.changeSummary}
          />
        )

      default:
        return null
    }
  }

  const sectionDescription = (
    key: SyllabusComparisonSectionKey,
  ) => {
    switch (key) {
      case "general":
        return "Fields recognized from the compared syllabus templates."
      case "workloadCredit":
        return "Workload and credit fields present in either compared template."
      case "requirements":
        return "Requirement/objective fields present in either compared template."
      case "clo":
        return "CLO columns follow the recognized source-template shape."
      case "content":
        return "Content fields follow the recognized source-template shape."
      case "topicClo":
        return "Canonical Topic–CLO relations for templates that contain this mapping."
      case "cloPlo":
        return "Canonical CLO–PLO relations for templates that contain this matrix."
      case "plannedActivities":
        return "Planned-activity fields follow the recognized source-template shape."
      case "assessment":
        return "Assessment fields follow the recognized source-template shape."
      case "assessmentClo":
        return "Canonical Assessment–CLO relations for templates that contain this matrix."
      case "examination":
        return "Examination fields present in either compared template."
      case "readings":
        return "Reading-list columns follow the recognized source-template shape."
      case "revision":
        return "Revision fields present in either compared template."
    }
  }

  const renderTemplateSection = (
    definition: SyllabusComparisonSectionDefinition,
    index: number,
  ): ReactNode => {
    const title =
      `${index + 1}. ${definition.label}`

    switch (definition.key) {
      case "general":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {definition.fields.map(
              (field) =>
                renderGeneralField(field.key),
            )}
          </ComparisonSection>
        )

      case "workloadCredit":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {definition.fields.map(
              (field) =>
                renderWorkloadField(field.key),
            )}
          </ComparisonSection>
        )

      case "requirements":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {definition.fields.map(
              (field) =>
                renderRequirementField(field.key),
            )}
          </ComparisonSection>
        )

      case "clo":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="CLO"
              diff={structuralDiff.cloDiff}
              fields={listFieldsFor("clo", cloFields)}
              identity={(item, itemIndex) =>
                item.code
                || `CLO ${itemIndex + 1}`}
              semanticFor={getCloSemantic}
              semanticKeys={new Set(["description"])}
            />
          </ComparisonSection>
        )

      case "content": {
        const visibleTopicFields =
          listFieldsFor(
            "content",
            topicFields,
          )

        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {hasField("content", "contentNote") && (
              <ScalarRow
                field={fieldLabel("content", "contentNote", "Content Note")}
                diff={structuralDiff.contentDiff}
                fieldKey="contentNote"
                oldValue={oldSupplemental.contentNote}
                newValue={newSupplemental.contentNote}
              />
            )}

            {visibleTopicFields.length > 0 && (
              <ListRows
                title="Topic"
                diff={structuralDiff.topicDiff}
                fields={visibleTopicFields}
                identity={(item, itemIndex) =>
                  item.name
                  || (
                    item.weekNumber != null
                      ? `Week ${item.weekNumber}`
                      : `Topic ${itemIndex + 1}`
                  )}
              />
            )}
          </ComparisonSection>
        )
      }

      case "topicClo":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="Topic-CLO mapping"
              diff={structuralDiff.topicCloDiff}
              fields={listFieldsFor("topicClo", topicCloFields)}
              identity={(item, itemIndex) =>
                [
                  item.topicName,
                  item.cloCode,
                ]
                  .filter(Boolean)
                  .join(" → ")
                || `Mapping ${itemIndex + 1}`}
            />
          </ComparisonSection>
        )

      case "cloPlo":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="CLO-PLO mapping"
              diff={structuralDiff.cloPloDiff}
              fields={listFieldsFor("cloPlo", cloPloFields)}
              identity={(item, itemIndex) =>
                [
                  item.cloCode,
                  item.ploCode,
                ]
                  .filter(Boolean)
                  .join(" → ")
                || `Mapping ${itemIndex + 1}`}
            />
          </ComparisonSection>
        )

      case "plannedActivities":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="Planned activity"
              diff={structuralDiff.plannedActivityDiff}
              fields={listFieldsFor("plannedActivities", plannedActivityFields)}
              identity={(item, itemIndex) =>
                item.topic
                || (
                  item.week != null
                    ? `Week ${item.week}`
                    : `Activity ${itemIndex + 1}`
                )}
            />
          </ComparisonSection>
        )

      case "assessment": {
        const visibleAssessmentFields =
          listFieldsFor(
            "assessment",
            assessmentFields,
          )

        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {hasField("assessment", "assessmentPassNote") && (
              <ScalarRow
                field={fieldLabel("assessment", "assessmentPassNote", "Assessment Pass Note")}
                diff={structuralDiff.assessmentInfoDiff}
                fieldKey="assessmentPassNote"
                oldValue={oldSupplemental.assessmentPassNote}
                newValue={newSupplemental.assessmentPassNote}
              />
            )}

            {visibleAssessmentFields.length > 0 && (
              <ListRows
                title="Assessment"
                diff={structuralDiff.assessmentDiff}
                fields={visibleAssessmentFields}
                identity={(item, itemIndex) =>
                  item.name
                  || `Assessment ${itemIndex + 1}`}
              />
            )}
          </ComparisonSection>
        )
      }

      case "assessmentClo":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="Assessment-CLO mapping"
              diff={structuralDiff.assessmentCloDiff}
              fields={listFieldsFor("assessmentClo", assessmentCloFields)}
              identity={(item, itemIndex) =>
                [
                  item.assessmentName,
                  item.cloCode,
                ]
                  .filter(Boolean)
                  .join(" → ")
                || `Mapping ${itemIndex + 1}`}
            />
          </ComparisonSection>
        )

      case "examination":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {definition.fields.map(
              (field) =>
                renderExaminationField(field.key),
            )}
          </ComparisonSection>
        )

      case "readings":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            <ListRows
              title="Reading"
              diff={structuralDiff.readingsDiff}
              fields={listFieldsFor("readings", readingFields)}
              identity={(item, itemIndex) =>
                item.title
                || `Reading ${itemIndex + 1}`}
            />
          </ComparisonSection>
        )

      case "revision":
        return (
          <ComparisonSection
            key={definition.key}
            title={title}
            description={sectionDescription(definition.key)}
            oldLabel={oldColumnLabel}
            newLabel={newColumnLabel}
          >
            {definition.fields.map(
              (field) =>
                renderRevisionField(field.key),
            )}
          </ComparisonSection>
        )
    }
  }

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-2xl border border-[#d8e4e7] bg-white shadow-sm">
        <div className="border-b border-slate-200 bg-gradient-to-r from-[#f4fbfb] to-white px-6 py-5">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#007d84]">
                Template-aware Syllabus Cohort Comparison
              </p>

              <h1 className="mt-1 text-2xl font-black text-[#17343d]">
                {newSyllabus.courseName}
              </h1>

              <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-500">
                <span className="rounded-md border bg-white px-2 py-1 font-mono font-bold text-[#007d84]">
                  {newSyllabus.courseCode}
                </span>

                <span className="rounded-md border bg-white px-2 py-1">
                  Comparison follows the uploaded syllabus template shape
                </span>
              </div>
            </div>

            <Button
              type="button"
              onClick={onAnalyzeSemantic}
              disabled={semanticLoading}
              className="bg-[#d99521] text-white hover:bg-[#bf811c]"
            >
              <Sparkles className="size-4" />

              {semanticLoading
                ? "Analyzing..."
                : semanticDiff
                  ? "Analyze Again"
                  : "Analyze Meaningful Changes"}
            </Button>
          </div>
        </div>

        <div className="grid md:grid-cols-[1fr_auto_1fr]">
          <div className="border-b border-slate-200 p-5 md:border-b-0 md:border-r">
            <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-red-500">
              Old Cohort
            </p>

            <p className="mt-1 text-xl font-black text-slate-900">
              {oldCohort}
            </p>

            <div className="mt-2 space-y-1 text-xs text-slate-500">
              <p>Program: <span className="font-semibold text-slate-700">{oldProgram}</span></p>
              <p>Semester: <span className="font-semibold text-slate-700">{display(oldSyllabus.semester)}</span></p>
              <p>Version: <span className="font-semibold text-slate-700">{oldSyllabus.versionLabel}</span></p>
              <p>Template: <span className="font-semibold text-slate-700">{oldTemplateLabel}</span></p>
            </div>
          </div>

          <div className="hidden items-center justify-center px-5 text-2xl font-black text-slate-300 md:flex">
            →
          </div>

          <div className="p-5 md:border-l">
            <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-emerald-600">
              New Cohort
            </p>

            <p className="mt-1 text-xl font-black text-slate-900">
              {newCohort}
            </p>

            <div className="mt-2 space-y-1 text-xs text-slate-500">
              <p>Program: <span className="font-semibold text-slate-700">{newProgram}</span></p>
              <p>Semester: <span className="font-semibold text-slate-700">{display(newSyllabus.semester)}</span></p>
              <p>Version: <span className="font-semibold text-slate-700">{newSyllabus.versionLabel}</span></p>
              <p>Template: <span className="font-semibold text-slate-700">{newTemplateLabel}</span></p>
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryMetric
            label="Structural Changes"
            value={structuralChangeCount}
            tone="slate"
          />

          <SummaryMetric
            label="Meaningful Changes"
            value={meaningfulCount}
            tone="red"
          />

          <SummaryMetric
            label="Minor Rewordings"
            value={rewordingCount}
            tone="blue"
          />

          <SummaryMetric
            label="Unresolved"
            value={unresolvedCount}
            tone="amber"
          />
        </div>

        {!semanticDiff && (
          <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2">
            <p className="text-xs text-amber-800">
              Structural comparison is ready. Narrative differences remain pending until semantic analysis is run.
            </p>

            <span className="rounded-full border border-amber-200 bg-white px-2.5 py-1 text-[11px] font-bold text-amber-700">
              AI not analyzed yet
            </span>
          </div>
        )}

        {semanticError && (
          <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
            {semanticError}
          </p>
        )}
      </section>

      {comparisonTemplate.sections.map(
        renderTemplateSection,
      )}
    </div>
  )
}

function SummaryMetric({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone:
    | "slate"
    | "red"
    | "blue"
    | "amber"
}) {
  const classes = {
    slate:
      "border-slate-200 bg-slate-50 text-slate-700",
    red:
      "border-red-200 bg-red-50 text-red-700",
    blue:
      "border-blue-200 bg-blue-50 text-blue-700",
    amber:
      "border-amber-200 bg-amber-50 text-amber-700",
  }

  return (
    <div
      className={`rounded-xl border px-4 py-3 ${classes[tone]}`}
    >
      <p className="text-2xl font-black">
        {value}
      </p>

      <p className="text-xs font-bold">
        {label}
      </p>
    </div>
  )
}
