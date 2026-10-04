import type { ReactNode } from "react"

import type { Syllabus } from "@/types/syllabus"

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
  <span className="whitespace-pre-wrap text-sm leading-6 text-slate-800">
    {text(value)}
  </span>
)

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
}: {
  number: string
  title: string
  note?: string
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
      </div>
    </section>
  )
}

function Field({
  label,
  value,
}: {
  label: string
  value: unknown
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/50 p-3">
      <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
        {label}
      </p>
      <div className="mt-1">
        {cell(value)}
      </div>
    </div>
  )
}

function Table({
  headers,
  rows,
  emptyText,
}: {
  headers: string[]
  rows: unknown[][]
  emptyText: string
}) {
  if (rows.length === 0) {
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
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function RubricSummary({
  source,
}: {
  source: unknown
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
            ? criteria.map((criterion) => {
              const levels = list(criterion.levels).map(text)
              return scaleLabels.length > 0
                ? [criterion.criterion, ...scaleLabels.map((_, levelIndex) => levels[levelIndex] ?? "—")]
                : [criterion.criterion, levels.join(" | ")]
            })
            : [[rubric.title ?? `Rubric ${index + 1}`, "No criteria available."]]

        return (
          <div key={`${text(rubric.type)}-${index}`} className="space-y-3 rounded-xl border border-slate-200 bg-slate-50/40 p-4">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
                {text(rubric.type)}
              </p>
              <h3 className="mt-1 font-bold text-[#17343d]">
                {text(rubric.title ?? `Rubric ${index + 1}`)}
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
}: {
  syllabus: Syllabus
}) {
  const data = syllabus as unknown as Loose
  const notes = parseJsonObject(data.notes)
  const rawClos = list(data.clos).map(obj)
  const topics = list(data.topics).map(obj)
  const assessments = list(data.assessments).map(obj)
  const plannedActivities = list(notes.plannedActivities).map(obj)
  const readings = list(notes.readings ?? data.readings ?? data.books ?? data.syllabusBooks).map(obj)
  const topicDetails = obj(notes.topicDetails)
  const cloPloMatrix = obj(notes.cloPloMatrix ?? notes.loMatrix)
  const assessmentCloMatrix = obj(notes.assessmentCloMatrix)
  const ploCodes = list(notes.ploCodes).map(String).filter(Boolean)

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
  isCs2027Target
    ? cs2027TargetPloCodes
    : sourcePloCodes

const sourceOnlyPloCodes =
  isCs2027Target
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
              NEW_2027 TARGET VIEW
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

      <Section number="1" title="General Information">
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="Course Code" value={data.courseCode} />
          <Field label="Course Name" value={data.courseName} />
          <Field label="Course Designation" value={data.courseDesignation} />
          <Field label="Course Type" value={courseTypeText(data.courseTypes)} />
          <Field label="Semester" value={data.semester} />
          <Field label="Person Responsible" value={notes.personResponsible ?? notes.instructor} />
          <Field label="Language" value={data.language} />
          <Field label="Relation to Curriculum" value={data.relation} />
          <Field label="Teaching Methods" value={data.teachingMethods} />
          <Field label="Total Workload" value={data.workloadTotal} />
          <Field label="Contact Hours" value={data.workloadContact} />
          <Field label="Self-study Hours" value={data.workloadPrivate} />
          <Field label="Credit Points" value={notes.creditPoints ?? notes.ects} />
          <Field label="Theory / Lecture Credits" value={notes.lectureCredits ?? notes.creditsTheory} />
          <Field label="Practice / Laboratory Credits" value={notes.laboratoryCredits ?? notes.creditsPractice} />
          <Field label="Prerequisites" value={data.prerequisites} />
        </div>
      </Section>

      <Section number="2" title="Course Objectives">
        <Field label="Course Objectives" value={cleanObjectives(data.objectives)} />
      </Section>

      <Section number="3" title="Course Learning Outcomes (CLO)">
        {hasUnreliableCloBlock && (
          <Notice>
            Structured CLO text for this course contains cross-section spill from later syllabus sections.
            It is hidden here instead of displaying misleading generated-looking data. The original source evidence remains preserved in the imported record.
          </Notice>
        )}
        <Table
          headers={["CLO Code", "Competency Level", "CLO — Vietnamese", "CLO — English", "Mapped PLOs"]}
          emptyText="No reliable structured CLO data is available from the source."
          rows={clos.map((clo, index) => {
            const code = cloCode(clo, index)
            return [
              code,
              clo.competencyLevel ?? clo.competency,
              clo.descriptionVn ?? clo.descriptionVN,
              clo.description ?? clo.descriptionEn,
              mappedPlosForClo(code),
            ]
          })}
        />
      </Section>

      <Section number="4" title="Course Content">
        <Table
          headers={["Topic — Vietnamese", "Topic — English", "Weight", "Level (I/T/U)", "Related CLOs"]}
          emptyText="No reliable course-content topic data is available from the source."
          rows={topics.map((topic, index) => {
            const detail = obj(topicDetails[String(index)] ?? topicDetails[String(index + 1)])
            return [
              topic.nameVn ?? topic.nameVN,
              topic.name ?? topic.topic ?? topic.title,
              detail.weight,
              detail.level,
              relatedCloForTopic(topic, index),
            ]
          })}
        />
      </Section>

      <Section number="5" title="Course CLO–PLO Alignment">
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
          rows={cloMatrixRows}
        />
      </Section>

      <Section
        number="6"
        title="Detailed CLO–LLO Table"
        note="No canonical LLO model exists in the CS2026 source import data. LLO values stay Missing / Absent in source; nothing is generated."
      >
        <Table
          headers={["Unit", "LLO Code", "Related CLO", "LLO — Vietnamese", "LLO — English"]}
          rows={[]}
          emptyText="Missing / Absent in source. No LLO values are generated."
        />
      </Section>

      <Section number="7" title="Examination Forms">
        <Field label="Examination Forms" value={data.examForms} />
      </Section>

      <Section number="8" title="Study and Examination Requirements">
        <div className="grid gap-3 md:grid-cols-2">
          <Field label="Study / Examination Requirements" value={data.examRequirements} />
          <Field
            label="Passing Requirement"
            value={stripKnownPrefix(notes.assessmentPassNote, [/^%?Pass:\s*/i])}
          />
        </div>
      </Section>

      <Section number="9" title="Planned Learning Activities and Teaching Methods">
        <Table
          headers={["Week", "Topic", "CLO", "Activities", "Assessment", "Resources"]}
          emptyText="No weekly planned-activity data is available."
          rows={plannedActivities.map((activity) => [
            activity.week,
            activity.topic,
            activity.clo,
            stripKnownPrefix(activity.learningActivities ?? activity.activities, [/^activities\s+/i]),
            activity.assessments,
            activity.resources,
          ])}
        />
      </Section>

      <Section number="10" title="Assessment Plan">
        <Table
          headers={["Assessment", "Weight (%)", "CLO", "Contribution (%)"]}
          emptyText="No assessment-plan data is available."
          rows={assessments.flatMap((assessment, index) => {
            const row = obj(assessmentCloMatrix[String(index)])
            const entries = Object.entries(row).filter(([, value]) => isPresent(value))
            if (entries.length === 0) {
              return [[
                assessment.name ?? assessment.assessmentName,
                assessment.weightPercent ?? assessment.weight,
                "—",
                "—",
              ]]
            }
            return entries.map(([code, contribution]) => [
              assessment.name ?? assessment.assessmentName,
              assessment.weightPercent ?? assessment.weight,
              code,
              contribution,
            ])
          })}
        />
      </Section>

      <Section number="11" title="Assignment Description and Rubric Summary">
        <div className="space-y-4">
          <Field
            label="Assignment Description"
            value={notes.assignmentDescription ?? notes.assignmentSummary ?? data.assignmentDescription ?? "No separate assignment description is available in the persisted canonical model."}
          />
          <RubricSummary source={notes.rubrics ?? notes.rubricSummary ?? data.rubrics} />
        </div>
      </Section>

      <Section number="12" title="Reading List">
        <Table
          headers={["Title", "Author", "Publication Year", "Publisher", "Usage Type"]}
          emptyText="No reading-list data is available."
          rows={readings.map((reading) => [
            reading.title ?? reading.bookTitle ?? reading.name,
            reading.author ?? reading.authors,
            reading.year ?? reading.publicationYear,
            reading.publisher,
            reading.usageType ?? reading.bookType ?? reading.type,
          ])}
        />
      </Section>
    </section>
  )
}

