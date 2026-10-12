import { isValidElement, type ReactNode } from "react"

import type { Syllabus } from "@/types/syllabus"
import { SourceRubricGrid, type SourceRubric } from "./SourceRubricGrid"
import { patchDraftNotes, removeDraftClo, removeDraftAssessment } from "@/lib/new2027SyllabusDraft"

const isSourceRubricGrid = (value: unknown): value is SourceRubric =>
  !!value && typeof value === "object"
  && (value as SourceRubric).format === "DS2026_RUBRIC_GRID_V1"
  && Array.isArray((value as SourceRubric).blocks)

type Loose = Record<string, unknown>

const obj = (value: unknown): Loose =>
  value && typeof value === "object" && !Array.isArray(value)
    ? value as Loose
    : {}

const list = (value: unknown): unknown[] =>
  Array.isArray(value) ? value : []

const text = (value: unknown): string => {
  const out = String(value ?? "").trim()
  return out || "—"
}

const textOrEmpty = (value: unknown): string => {
  const out = String(value ?? "").trim()
  return out
}

const normalizeSpace = (value: unknown): string =>
  String(value ?? "")
    .replace(/\s+/g, " ")
    .trim()

const parseJsonObject = (value: unknown): Loose => {
  if (!value || typeof value !== "string") return obj(value)

  try {
    return obj(JSON.parse(value))
  } catch {
    return {}
  }
}

const parseMaybeJson = (value: unknown): unknown => {
  if (typeof value !== "string") return value
  const trimmed = value.trim()
  if (!trimmed) return ""
  if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return value

  try {
    return JSON.parse(trimmed)
  } catch {
    return value
  }
}

const isPresent = (value: unknown): boolean =>
  textOrEmpty(value).length > 0

const severeSpillMarkers = [
  /planned learning activities/i,
  /assessment plan/i,
  /assessment type/i,
  /rubrics?\s*\(optional\)/i,
  /grading checklist/i,
  /holistic rubric/i,
  /analytic rubric/i,
  /reading list/i,
  /final grade/i,
  /total score/i,
  /course learning outcome\s*\(clo\).*content the description/i,
]

const hasSevereSectionSpill = (value: unknown): boolean => {
  const source = normalizeSpace(value)
  return source.length > 280
    && severeSpillMarkers.some((pattern) => pattern.test(source))
}

const cleanObjectives = (value: unknown): string => {
  const source = normalizeSpace(value)
  if (!source) return "—"

  const markers = [
    /Course learning CLO\s*1/i,
    /Course learning outcomes?\b/i,
    /Competency level\b/i,
    /Content\s+The description/i,
    /Topic\s+Weight\b/i,
    /Examination forms\b/i,
    /Study and Attendance\b/i,
    /Reading list\b/i,
  ]

  const cutAt = markers
    .map((pattern) => {
      const match = source.match(pattern)
      return match?.index ?? -1
    })
    .filter((index) => index > 80)
    .sort((a, b) => a - b)[0]

  return text(cutAt ? source.slice(0, cutAt).trim() : source)
}

const stripKnownPrefix = (value: unknown, prefixes: RegExp[]): string => {
  let out = normalizeSpace(value)
  for (const prefix of prefixes) {
    out = out.replace(prefix, "").trim()
  }
  return text(out)
}

const cell = (value: unknown) => (
  isValidElement(value) ? value :
  <span className="whitespace-pre-wrap text-sm leading-6 text-slate-800">
    {text(value)}
  </span>
)

const editableCell = (value: unknown, label: string, onChange?: (value: string) => void) =>
  onChange ? <textarea aria-label={label} className="min-h-16 w-full min-w-28 resize-y rounded-md border border-slate-200 bg-white px-2 py-1 text-sm leading-6 text-slate-800 focus:border-[#007d84] focus:outline-none"
    value={String(value ?? "")} onChange={(event) => onChange(event.target.value)} /> : value

function Notice({
  children,
}: {
  children: ReactNode
}) {
  return (
    <div className="rounded-xl border border-dashed border-amber-300 bg-amber-50/70 p-4 text-sm leading-6 text-amber-900">
      {children}
    </div>
  )
}

function Section({
  number,
  title,
  children,
  note,
  sourceTables,
  onSourceTablesChange,
}: {
  number: string
  title: string
  note?: string
  sourceTables?: unknown
  onSourceTablesChange?: (value: unknown[]) => void
  children: ReactNode
}) {
  return (
    <section className="overflow-hidden rounded-2xl border border-[#cfe1e4] bg-white shadow-sm">
      <header className="border-b border-slate-100 bg-gradient-to-r from-[#f5fbfb] to-white px-5 py-4">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-[#007d84] text-sm font-bold text-white">
            {number}
          </span>
          <div>
            <h2 className="font-bold text-[#17343d]">
              {number}. {title}
            </h2>
            {note && (
              <p className="mt-1 text-xs leading-5 text-slate-500">
                {note}
              </p>
            )}
          </div>
        </div>
      </header>
      <div className="space-y-4 p-5">
        {children}
        <SourceTables source={sourceTables} onChange={onSourceTablesChange} />
      </div>
    </section>
  )
}

function SourceTables({ source, onChange }: { source: unknown; onChange?: (value: unknown[]) => void }) {
  return <>{list(source).map(obj).map((table, index) => {
    const update = (value: unknown) => onChange?.(list(source).map((item, position) => position === index ? value : item))
    if (isSourceRubricGrid(table)) return <SourceRubricGrid key={index} source={table} onChange={onChange ? update : undefined} />
    const headers = list(table.headers).map(textOrEmpty)
    if (!headers.length) return null
    const rows = list(table.rows).filter(Array.isArray)
      .map((row) => headers.map((_, column) => row[column] ?? ""))
    return <div key={index} className="space-y-2">
      <h3 className="text-sm font-semibold text-[#17343d]">{text(table.title)}</h3>
      {list(table.pages).length > 0 && <p className="text-xs text-slate-500">Source PDF pages: {list(table.pages).join(", ")}</p>}
      <Table headers={headers} rows={rows.map((row, rowIndex) => row.map((value, column) =>
        editableCell(value, `${String(table.title ?? "Source table")} row ${rowIndex + 1} ${headers[column]}`, onChange ? changed => update({ ...table,
          rows: list(table.rows).map((item, position) => position === rowIndex ? list(item).map((entry, col) => col === column ? changed : entry) : item),
        }) : undefined)))} emptyText="No source rows available."
        onAdd={onChange ? () => update({ ...table, rows: [...list(table.rows), headers.map(() => "")] }) : undefined}
        onDelete={onChange ? row => update({ ...table, rows: list(table.rows).filter((_, position) => position !== row) }) : undefined} />
    </div>
  })}</>
}

function Field({
  label,
  value,
  onChange,
  editValue,
}: {
  label: string
  value: unknown
  editValue?: unknown
  onChange?: (value: string) => void
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
      <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
        {label}
      </p>
      <div className="mt-1">
        {cell(editableCell(onChange && editValue !== undefined ? editValue : value, label, onChange))}
      </div>
    </div>
  )
}

function Table({
  headers,
  rows,
  emptyText,
  onAdd,
  onDelete,
}: {
  headers: string[]
  rows: unknown[][]
  emptyText: string
  onAdd?: () => void
  onDelete?: (row: number) => void
}) {
  if (rows.length === 0 && !onAdd) {
    return (
      <div className="rounded-xl border border-dashed border-slate-200 bg-slate-50/60 p-5 text-sm text-slate-500">
        {emptyText}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200">
      <table className="w-full min-w-[760px] border-collapse text-sm">
        <thead>
          <tr className="bg-slate-50 text-left text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
            {headers.map((header) => (
              <th key={header} className="border-b border-r border-slate-200 px-3 py-3 last:border-r-0">
                {header}
              </th>
            ))}
            {onDelete && <th className="px-3 py-3">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex} className="align-top hover:bg-slate-50/40">
              {row.map((value, cellIndex) => (
                <td key={cellIndex} className="border-b border-r border-slate-100 px-3 py-3 last:border-r-0">
                  {cell(value)}
                </td>
              ))}
              {onDelete && <td className="border-b border-slate-100 px-3 py-3"><button type="button" className="text-xs text-rose-700"
                aria-label={`Remove ${headers[0]} row ${rowIndex + 1}`} onClick={() => onDelete(rowIndex)}>Remove</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
      {onAdd && <button type="button" className="m-3 rounded-md border border-[#007d84] px-3 py-2 text-xs font-semibold text-[#007d84]"
        onClick={onAdd}>Add {headers[0]} row</button>}
    </div>
  )
}

function RubricSummary({
  source,
  onChange,
}: {
  source: unknown
  onChange?: (value: unknown) => void
}) {
  const parsed = parseMaybeJson(source)
  const parsedObject = obj(parsed)
  const rubricsSource =
    list(parsed).length > 0
      ? list(parsed)
      : list(parseMaybeJson(parsedObject.rubrics))

  const rubrics =
    rubricsSource
      .map(obj)
      .filter((rubric) => Object.keys(rubric).length > 0)
  const updateRubric = (rubric: Loose, patch: Loose) => {
    const updated = rubricsSource.map((item) => item === rubric ? { ...rubric, ...patch } : item)
    const result = Array.isArray(parsed) ? updated : { ...parsedObject, rubrics: updated }
    onChange?.(typeof source === "string" ? JSON.stringify(result) : result)
  }

  if (rubrics.length === 0) {
    return (
      <Field
        label="Rubric Summary"
        value="No structured rubric summary is available in the persisted canonical model."
      />
    )
  }

  return (
    <div className="space-y-4">
      {rubrics.map((rubric, index) => {
        const scaleLabels = list(rubric.scaleLabels)
          .map(textOrEmpty)
          .filter(Boolean)
        const criteria = list(rubric.criteria).map(obj)
        const headers =
          scaleLabels.length > 0
            ? ["Criterion", ...scaleLabels]
            : ["Criterion", "Levels"]

        const rows =
          criteria.length > 0
            ? criteria.map((criterion, criterionIndex) => {
              const levels = list(criterion.levels).map(text)
              const updateCriterion = (patch: Loose) => updateRubric(rubric, {
                criteria: criteria.map((item, position) => position === criterionIndex ? { ...item, ...patch } : item),
              })
              const criterionCell = editableCell(criterion.criterion, `Rubric ${index + 1} Criterion ${criterionIndex + 1}`,
                onChange ? changed => updateCriterion({ criterion: changed }) : undefined)
              return scaleLabels.length > 0
                ? [criterionCell, ...scaleLabels.map((_, levelIndex) => editableCell(onChange ? list(criterion.levels)[levelIndex] : levels[levelIndex] ?? "—",
                    `Rubric ${index + 1} Criterion ${criterionIndex + 1} Level ${levelIndex + 1}`, onChange ? changed => updateCriterion({
                      levels: scaleLabels.map((_, position) => position === levelIndex ? changed : list(criterion.levels)[position] ?? ""),
                    }) : undefined))]
                : [criterionCell, levels.join(" | ")]
            })
            : [[rubric.title ?? `Rubric ${index + 1}`, "No criteria available."]]

        return (
          <div key={`${text(rubric.type)}-${index}`} className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/40 p-4">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
                {text(rubric.type)}
              </p>
              <h3 className="mt-1 font-bold text-[#17343d]">
                {onChange ? cell(editableCell(rubric.title, `Rubric ${index + 1} Title`, changed => updateRubric(rubric, { title: changed }))) : text(rubric.title ?? `Rubric ${index + 1}`)}
              </h3>
            </div>
            <Table
              headers={headers}
              rows={rows}
              emptyText="No rubric criteria available."
            />
          </div>
        )
      })}
    </div>
  )
}

const courseTypeText = (value: unknown) => {
  const raw = String(value ?? "").trim()
  if (!raw) return "—"

  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) return parsed.map(String).join(", ") || "—"
  } catch {
    // Keep legacy raw string.
  }

  return raw
}

const cloCode = (item: Loose, index: number) =>
  text(item.code ?? item.cloCode ?? `CLO${index + 1}`)

const normalizeTopic = (value: unknown) =>
  normalizeSpace(value)
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()

export default function New2027SyllabusView({
  syllabus,
  onChange,
}: {
  syllabus: Syllabus
  onChange?: (value: Syllabus) => void
}) {
  const data = syllabus as unknown as Loose
  const notes = parseJsonObject(data.notes)
  const sourceSections = obj(notes.sourceSupplementalSections)
  const rawClos = list(data.clos).map(obj)
  const topics = list(data.topics).map(obj)
  const assessments = list(data.assessments).map(obj)
  const plannedActivities = list(notes.plannedActivities).map(obj)
  const readings = list(notes.readings ?? data.readings ?? data.books ?? data.syllabusBooks).map(obj)
  const topicDetails = obj(notes.topicDetails)
  const sourceContentRows = list(notes.sourceContentRows).map(obj)
  const cloPloMatrix = obj(notes.cloPloMatrix ?? notes.loMatrix)
  const assessmentCloMatrix = obj(notes.assessmentCloMatrix)
  const ploCodes = list(notes.ploCodes).map(String).filter(Boolean)
  const changeField = (key: keyof Syllabus) => onChange ? (value: string) => onChange({ ...syllabus, [key]: value }) : undefined
  const changeNotes = (patch: Loose) => onChange?.(patchDraftNotes(syllabus, patch))
  const noteField = (key: string) => onChange ? (value: string) => changeNotes({ [key]: value }) : undefined
  const noteRow = (key: string, index: number, patch: Loose) => changeNotes({
    [key]: (key === "readings" ? readings : list(notes[key])).map((row, position) => position === index ? { ...obj(row), ...patch } : row),
  })
  const sourceProps = (section: string) => ({ sourceTables: sourceSections[section],
    onSourceTablesChange: onChange ? (value: unknown[]) => changeNotes({ sourceSupplementalSections: { ...sourceSections, [section]: value } }) : undefined,
  })
  const nativeRow = (key: "clos" | "topics" | "assessments", index: number, patch: Loose) =>
    onChange?.({ ...syllabus, [key]: list(syllabus[key]).map((row, position) => position === index ? { ...obj(row), ...patch } : row) })
  const editNoteRow = (key: string, index: number, field: string, value: unknown, label: string) =>
    editableCell(value, label, onChange ? changed => noteRow(key, index, { [field]: changed }) : undefined)
  const editNativeRow = (key: "clos" | "topics" | "assessments", index: number, field: string, value: unknown, label: string) =>
    editableCell(value, label, onChange ? changed => nativeRow(key, index, { [field]: changed }) : undefined)
  const topicDetailField = (index: number, key: string, value: unknown) => editableCell(value, `Content ${index + 1} ${key}`, onChange ? changed => {
    const rowKey = Object.hasOwn(topicDetails, String(index)) ? String(index) : Object.hasOwn(topicDetails, String(index + 1)) ? String(index + 1) : String(index)
    changeNotes({ topicDetails: { ...topicDetails, [rowKey]: { ...obj(topicDetails[rowKey]), [key]: changed } } })
  } : undefined)
  const assessmentCells = (assessment: Loose, index: number) => [
    editNativeRow("assessments", index, "name", assessment.name ?? assessment.assessmentName, `Assessment ${index + 1} Name`),
    onChange ? <input type="number" min={0} max={100} step="any" aria-label={`Assessment ${index + 1} Weight (%)`} className="w-24 rounded-md border border-slate-200 p-2"
      value={assessment.weightPercent == null ? "" : String(assessment.weightPercent)} onChange={(event) => nativeRow("assessments", index, { weightPercent: event.target.value === "" ? null : Number(event.target.value) })} />
      : assessment.weightPercent ?? assessment.weight,
  ]
  const assessmentRowIndexes = assessments.flatMap((_, index) => {
    const size = Object.values(obj(assessmentCloMatrix[String(index)])).filter(isPresent).length
    return Array(Math.max(1, size)).fill(index) as number[]
  })
  const addAssessment = () => {
    const rows: unknown[] = [...assessments, { name: "", weightPercent: null, minScore: null, maxScore: null,
      orderIndex: Math.max(0, ...assessments.map(row => Number(row.orderIndex) || 0)) + 1 }]
    const key: "assessments" | "clos" = "assessments"
    onChange?.({ ...syllabus, [key]: rows } as Syllabus)
  }

  const clos =
  rawClos.filter(
    (clo) =>
      !hasSevereSectionSpill(
        clo.description
        ?? clo.descriptionEn
        ?? clo.englishDescription,
      ),
  )

const hasUnreliableCloBlock =
  clos.length !== rawClos.length

  const matrixPloCodes =
  Array.from(
    new Set(
      Object.values(cloPloMatrix)
        .flatMap((row) =>
          Object.keys(obj(row)),
        ),
    ),
  )

const sourcePloCodes =
  Array.from(
    new Set([
      ...ploCodes,
      ...matrixPloCodes,
    ]),
  )

const isCs2027Target =
  String(
    syllabus.cohortName
    ?? syllabus.academicYear
    ?? "",
  ).trim().toUpperCase() === "CS2027"
  && String(
    syllabus.programCode
    ?? "",
  ).trim().toUpperCase() === "CS"

const cs2027TargetPloCodes = [
  "PLO1",
  "PLO2",
  "PLO3",
  "PLO4",
  "PLO5",
  "PLO6",
]

const effectivePloCodes =
  isCs2027Target && notes.sourceAlignmentPreserved !== true
    ? cs2027TargetPloCodes
    : sourcePloCodes

const sourceOnlyPloCodes =
  isCs2027Target && notes.sourceAlignmentPreserved !== true
    ? sourcePloCodes.filter(
        (code) =>
          !cs2027TargetPloCodes.includes(code),
      )
    : []

  const cloMatrixRows = Object.keys(cloPloMatrix).length > 0
    ? Object.entries(cloPloMatrix)
      .map(([code, row]) => {
        const rowObj = obj(row)
        return [code, ...effectivePloCodes.map((plo) => rowObj[plo] ?? "")]
      })
    : clos.map((clo, index) => {
      const code = cloCode(clo, index)
      const row = obj(cloPloMatrix[code])
      return [code, ...effectivePloCodes.map((plo) => row[plo] ?? "")]
    })

  const mappedPlosForClo = (code: string) => {
    const row = obj(cloPloMatrix[code])
    return effectivePloCodes
      .filter((plo) => isPresent(row[plo]))
      .join(", ")
  }

  const relatedCloForTopic = (topic: Loose, index: number) => {
    const detail = obj(topicDetails[String(index)] ?? topicDetails[String(index + 1)])
    const direct = textOrEmpty(detail.clo ?? topic.clo ?? topic.clos ?? topic.cloCode)
    if (direct) return direct

    const topicName = normalizeTopic(topic.name ?? topic.topic ?? topic.title)
    const activity =
      plannedActivities[index]
      ?? plannedActivities.find((item) => normalizeTopic(item.topic) === topicName)

    return textOrEmpty(activity?.clo)
  }

  return (
    <section aria-label="NEW_2027 syllabus view" className="space-y-5">
      <div className="rounded-2xl border border-[#a9d7db] bg-[#f2fbfb] px-5 py-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#007d84]">
              {onChange ? "NEW_2027 DRAFT EDITOR" : "NEW_2027 TARGET VIEW"}
            </p>
            <h2 className="mt-1 text-xl font-black text-[#17343d]">
              {`${String(syllabus.cohortName ?? syllabus.academicYear ?? "Target").trim() || "Target"} Standard Syllabus Template`}
            </h2>
            <p className="mt-1 text-xs leading-5 text-slate-600">
              Source semantic content is preserved. Missing or unreliable structured fields are not generated.
            </p>
          </div>
          <span className="rounded-full border border-[#9fd4d8] bg-white px-3 py-1 text-xs font-bold text-[#007d84]">
            12 sections
          </span>
        </div>
      </div>

      <Section number="1" title="General Information" {...sourceProps("1")}>
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="Course Code" value={data.courseCode} />
          <Field label="Course Name" value={data.courseName} />
          <Field label="Course Designation" value={data.courseDesignation} onChange={changeField("courseDesignation")} />
          <Field label="Course Type" value={courseTypeText(data.courseTypes)} editValue={isPresent(data.courseTypes) ? courseTypeText(data.courseTypes) : ""}
            onChange={onChange ? value => onChange({ ...syllabus, courseTypes: JSON.stringify(value.split(/[,;\n]/).map(item => item.trim()).filter(Boolean)) }) : undefined} />
          <Field label="Semester" value={notes.sourceSemester || data.semester} onChange={noteField("sourceSemester")} />
          <Field label="Person Responsible" value={notes.personResponsible ?? notes.instructor} onChange={noteField("personResponsible")} />
          <Field label="Language" value={data.language} onChange={changeField("language")} />
          <Field label="Relation to Curriculum" value={data.relation} onChange={changeField("relation")} />
          <Field label="Teaching Methods" value={data.teachingMethods} onChange={changeField("teachingMethods")} />
          <Field label={notes.sourceWorkloadEstimated === true ? "Total Workload (estimated)" : "Total Workload"} value={data.workloadTotal} onChange={changeField("workloadTotal")} />
          <Field label={notes.sourceWorkloadUnit === "tiết" ? "Contact workload (source unit: tiết)" : "Contact Hours"} value={data.workloadContact} onChange={changeField("workloadContact")} />
          <Field label={notes.sourceWorkloadUnit === "tiết" ? "Self-study workload (source unit: tiết)" : "Self-study Hours"} value={data.workloadPrivate} onChange={changeField("workloadPrivate")} />
          <Field label="Credit Points" value={notes.creditPoints ?? notes.ects} onChange={noteField("creditPoints")} />
          <Field label="Theory / Lecture Credits" value={notes.lectureCredits ?? notes.creditsTheory} onChange={noteField("lectureCredits")} />
          <Field label="Practice / Laboratory Credits" value={notes.laboratoryCredits ?? notes.creditsPractice} onChange={noteField("laboratoryCredits")} />
          <Field label="Prerequisites" value={data.prerequisites} onChange={changeField("prerequisites")} />
        </div>
      </Section>

      <Section number="2" title="Course Objectives" {...sourceProps("2")}>
        <Field label="Course Objectives" value={notes.sourceImportEvidence ? data.objectives : cleanObjectives(data.objectives)} editValue={data.objectives ?? ""} onChange={changeField("objectives")} />
      </Section>

      <Section number="3" title="Course Learning Outcomes (CLO)" {...sourceProps("3")}>
        {hasUnreliableCloBlock && (
          <Notice>
            Structured CLO text for this course contains cross-section spill from later syllabus sections.
            It is hidden here instead of displaying misleading generated-looking data. The original source evidence remains preserved in the imported record.
          </Notice>
        )}
        <Table
          headers={["CLO Code", "Competency Level", "CLO — Vietnamese", "CLO — English", "Mapped PLOs"]}
          emptyText="No CLO data is available for this syllabus. A blank table does not establish that the source has no CLOs."
          onAdd={onChange ? () => {
            let next = 1
            while (rawClos.some(clo => clo.code === `CLO${next}`)) next++
            onChange({ ...syllabus, clos: [...(syllabus.clos ?? []), { code: `CLO${next}`, description: "", orderIndex: rawClos.length + 1 }] })
          } : undefined}
          onDelete={onChange ? row => onChange(removeDraftClo(syllabus, rawClos.indexOf(clos[row]))) : undefined}
          rows={clos.map((clo, index) => {
            const code = cloCode(clo, index)
            return [
              code,
              onChange ? <select aria-label={`${code} Competency Level`} className="rounded-md border border-slate-200 bg-white p-2" value={String(clo.competencyLevel ?? "")}
                onChange={(event) => nativeRow("clos", rawClos.indexOf(clo), { competencyLevel: event.target.value })}>
                {Array.from(new Set(["", "KNOWLEDGE", "SKILL", "ATTITUDE", String(clo.competencyLevel ?? "")])).map(value => <option key={value} value={value}>{value || "Select competency"}</option>)}
              </select> : clo.competencyLevel ?? clo.competency,
              editNativeRow("clos", rawClos.indexOf(clo), "descriptionVn", clo.descriptionVn ?? clo.descriptionVN, `${code} Vietnamese`),
              editNativeRow("clos", rawClos.indexOf(clo), "description", notes.sourceCloLanguage === "VI" && clo.description === clo.descriptionVn ? "" : clo.description ?? clo.descriptionEn, `${code} English`),
              mappedPlosForClo(code),
            ]
          })}
        />
      </Section>

      <Section number="4" title="Course Content" {...sourceProps("4")}>
        {(Boolean(notes.contentNote) || onChange) && <Field label="Source content description" value={notes.contentNote} onChange={noteField("contentNote")} />}
        {sourceContentRows.length > 0 && (
          <Notice>The source content table is shown separately from the weekly activity plan. Week, hours and related CLOs are not inferred for unverified rows.</Notice>
        )}
        <Table
          headers={sourceContentRows.length > 0 ? ["Topic — Vietnamese", "Topic — English", "Weight", "Level (I/T/U)", "Related CLOs", "Week", "Hours"] : ["Topic — Vietnamese", "Topic — English", "Weight", "Level (I/T/U)", "Related CLOs"]}
          emptyText="No course-content topic data is available for this syllabus. Review the source before marking Content as absent."
          onAdd={onChange && (Array.isArray(notes.sourceContentRows) || topics.length === 0) ? () => changeNotes({ sourceContentRows: [...sourceContentRows,
            { name: "", nameVn: "", weight: "", level: "", relatedCloCodes: [], weekNumber: null, teachingHours: null, labHours: null, selfStudyHours: null }],
          }) : undefined}
          onDelete={onChange && sourceContentRows.length > 0 ? index => changeNotes({ sourceContentRows: list(notes.sourceContentRows).filter((_, position) => position !== index) }) : undefined}
          rows={sourceContentRows.length > 0 ? sourceContentRows.map((row, index) => [
            editNoteRow("sourceContentRows", index, "nameVn", row.nameVn, `Content ${index + 1} Vietnamese`),
            editNoteRow("sourceContentRows", index, "name", notes.sourceContentLanguage === "VI" && row.name === row.nameVn ? "" : row.name, `Content ${index + 1} English`),
            editNoteRow("sourceContentRows", index, "weight", row.weight, `Content ${index + 1} Weight`),
            editNoteRow("sourceContentRows", index, "level", row.level, `Content ${index + 1} Level`),
            list(row.relatedCloCodes).join(", "),
            row.weekNumber,
            ["teachingHours", "labHours", "selfStudyHours"].filter((key) => row[key] != null)
              .map((key) => `${key === "teachingHours" ? "Teaching" : key === "labHours" ? "Lab" : "Self-study"}: ${row[key]}`).join("; "),
          ]) : topics.map((topic, index) => {
            const detail = obj(topicDetails[String(index)] ?? topicDetails[String(index + 1)])
            return [
              editNativeRow("topics", index, "nameVn", topic.nameVn ?? topic.nameVN, `Content ${index + 1} Vietnamese`),
              editNativeRow("topics", index, "name", topic.name ?? topic.topic ?? topic.title, `Content ${index + 1} English`),
              topicDetailField(index, "weight", detail.weight),
              topicDetailField(index, "level", detail.level),
              relatedCloForTopic(topic, index),
            ]
          })}
        />
      </Section>

      <Section number="5" title="Course CLO–PLO Alignment" {...sourceProps("5")}>
        {notes.sourceAlignmentPreserved === true && (
          <Notice>Source alignment is shown verbatim. Numeric marks are retained; source-only columns are not treated as verified program mappings.</Notice>
        )}
        {hasUnreliableCloBlock && cloMatrixRows.length > 0 && (
          <Notice>
            CLO descriptions were not reliable for display, but the persisted alignment matrix is still shown separately because it is stored as structured mapping data.
          </Notice>
        )}
        {sourceOnlyPloCodes.length > 0 && (
  <Notice>
    Source mappings outside the CS2027 target PLO scope
    {" "}
    ({sourceOnlyPloCodes.join(", ")})
    {" "}
    are preserved as source evidence and are not rendered
    as CS2027 target PLO columns.
  </Notice>
)}
        <Table
          headers={["CLO", ...effectivePloCodes]}
          emptyText="No CLO–PLO matrix is available for this syllabus."
          rows={cloMatrixRows.map((row) => row.map((value, column) => column === 0 ? value : editableCell(value,
            `${String(row[0])} ${effectivePloCodes[column - 1]} alignment`, onChange ? changed => changeNotes({
              cloPloMatrix: { ...cloPloMatrix, [String(row[0])]: { ...obj(cloPloMatrix[String(row[0])]), [effectivePloCodes[column - 1]]: changed } },
            }) : undefined)))}
        />
      </Section>

      <Section
        number="6"
        {...sourceProps("6")}
        title="Detailed CLO–LLO Table"
        note="Structured LLO data has not been verified against the source. A blank table does not establish that the source has no LLOs."
      >
        <Table
          headers={["Unit", "LLO Code", "Related CLO", "LLO — Vietnamese", "LLO — English"]}
          rows={[]}
          emptyText="Source not verified for this section. No LLO values are generated."
        />
      </Section>

      <Section number="7" title="Examination Forms" {...sourceProps("7")}>
        <Field label="Examination Forms" value={data.examForms} onChange={changeField("examForms")} />
      </Section>

      <Section number="8" title="Study and Examination Requirements" {...sourceProps("8")}>
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="Study / Examination Requirements" value={data.examRequirements} onChange={changeField("examRequirements")} />
          <Field
            label="Passing Requirement"
            value={stripKnownPrefix(notes.assessmentPassNote, [/^%?Pass:\s*/i])}
            editValue={notes.assessmentPassNote ?? ""}
            onChange={noteField("assessmentPassNote")}
          />
        </div>
      </Section>

      <Section number="9" title="Planned Learning Activities and Teaching Methods" {...sourceProps("9")}>
          {notes.sourceWeeklyReviewRequired === true && <Notice>Only verified weekly rows are shown. Additional source rows require review; Course Content is preserved independently in Section 4.</Notice>}
        {list(sourceSections["9"]).length === 0 && <Table
          headers={["Week", "Topic", "CLO", "Activities", "Assessment", "Resources"]}
          emptyText="No weekly planned-activity data is available."
          onAdd={onChange ? () => changeNotes({ plannedActivities: [...plannedActivities, { week: "", topic: "", clo: "", learningActivities: "", assessments: "", resources: "" }] }) : undefined}
          onDelete={onChange ? index => changeNotes({ plannedActivities: list(notes.plannedActivities).filter((_, position) => position !== index) }) : undefined}
          rows={plannedActivities.map((activity, index) => [
            editNoteRow("plannedActivities", index, "week", activity.week, `Week row ${index + 1} Week`),
            editNoteRow("plannedActivities", index, "topic", activity.topic, `Week row ${index + 1} Topic`),
            editNoteRow("plannedActivities", index, "clo", activity.clo, `Week row ${index + 1} CLO`),
            onChange ? editNoteRow("plannedActivities", index, "learningActivities", activity.learningActivities ?? activity.activities, `Week row ${index + 1} Activities`) : stripKnownPrefix(activity.learningActivities ?? activity.activities, [/^activities\s+/i]),
            editNoteRow("plannedActivities", index, "assessments", activity.assessments, `Week row ${index + 1} Assessment`),
            editNoteRow("plannedActivities", index, "resources", activity.resources, `Week row ${index + 1} Resources`),
          ])}
        />}
      </Section>

      <Section number="10" title="Assessment Plan" {...sourceProps("10")}>
        {list(sourceSections["10"]).length === 0 && <Table
          headers={["Assessment", "Weight (%)", "CLO", "Contribution (%)"]}
          emptyText="No assessment-plan data is available."
          onAdd={onChange ? addAssessment : undefined}
          onDelete={onChange ? row => onChange(removeDraftAssessment(syllabus, assessmentRowIndexes[row])) : undefined}
          rows={assessments.flatMap((assessment, index) => {
            const row = obj(assessmentCloMatrix[String(index)])
            const entries = Object.entries(row).filter(([, value]) => isPresent(value))
            if (entries.length === 0) {
              return [[
                ...assessmentCells(assessment, index),
                "—",
                "—",
              ]]
            }
            return entries.map(([code, contribution]) => [
              ...assessmentCells(assessment, index),
              code,
              editableCell(contribution, `Assessment ${index + 1} ${code} Contribution (%)`, onChange ? changed => changeNotes({
                assessmentCloMatrix: { ...assessmentCloMatrix, [String(index)]: { ...row, [code]: changed } },
              }) : undefined),
            ])
          })}
        />}
      </Section>

      <Section number="11" title="Assignment Description and Rubric Summary" {...sourceProps("11")}>
        <div className="space-y-4">
          {(onChange || !list(sourceSections["11"]).some(isSourceRubricGrid)
            || isPresent(notes.assignmentDescription ?? notes.assignmentSummary ?? data.assignmentDescription)) && <Field
            label="Assignment Description"
            value={notes.assignmentDescription ?? notes.assignmentSummary ?? data.assignmentDescription ?? "No separate assignment description is available in the persisted canonical model."}
            editValue={notes.assignmentDescription ?? notes.assignmentSummary ?? data.assignmentDescription ?? ""}
            onChange={noteField("assignmentDescription")}
          />}
          {(!list(sourceSections["11"]).some(isSourceRubricGrid)
            || isPresent(notes.rubrics ?? notes.rubricSummary ?? data.rubrics))
            && <RubricSummary source={notes.rubrics ?? notes.rubricSummary ?? data.rubrics} onChange={onChange ? value => {
              const parsed = parseMaybeJson(value)
              onChange({ ...patchDraftNotes(syllabus, { rubrics: value }), rubrics: JSON.stringify(Array.isArray(parsed) ? { rubrics: parsed } : parsed) })
            } : undefined} />}
        </div>
      </Section>

      <Section number="12" title="Reading List" {...sourceProps("12")}>
        {list(sourceSections["12"]).some((table) => obj(table).format === "DS2026_READING_ROWS_V1") ? null : notes.sourceReadingList ? <Field label="Source reading list" value={notes.sourceReadingList} onChange={noteField("sourceReadingList")} /> : <Table
          headers={["Title", "Author", "Publication Year", "Publisher", "Usage Type"]}
          emptyText="No reading-list data is available."
          onAdd={onChange ? () => changeNotes({ readings: [...readings, { title: "", author: "", year: "", publisher: "", usageType: "" }] }) : undefined}
          onDelete={onChange ? index => changeNotes({ readings: readings.filter((_, position) => position !== index) }) : undefined}
          rows={readings.map((reading, index) => [
            editNoteRow("readings", index, "title", reading.title ?? reading.bookTitle ?? reading.name, `Reading ${index + 1} Title`),
            editNoteRow("readings", index, "author", reading.author ?? reading.authors, `Reading ${index + 1} Author`),
            editNoteRow("readings", index, "year", reading.year ?? reading.publicationYear, `Reading ${index + 1} Publication Year`),
            editNoteRow("readings", index, "publisher", reading.publisher, `Reading ${index + 1} Publisher`),
            notes.readingUsagePolicy === "SOURCE_UNSPECIFIED"
              ? "Not specified in source"
              : onChange ? <select aria-label={`Reading ${index + 1} Usage Type`} className="rounded-md border border-slate-200 bg-white p-2" value={String(reading.usageType ?? reading.bookType ?? reading.type ?? "")}
                onChange={(event) => noteRow("readings", index, { usageType: event.target.value })}>
                {Array.from(new Set(["", "REQUIRED", "RECOMMENDED", "SUPPLEMENTARY", String(reading.usageType ?? reading.bookType ?? reading.type ?? "")])).map(value => <option key={value} value={value}>{value || "Select usage"}</option>)}
              </select> : reading.usageType ?? reading.bookType ?? reading.type,
          ])}
        />}
      </Section>
    </section>
  )
}

