import {
  BookOpen,
  CheckCircle2,
  FileText,
  GraduationCap,
  Grid3X3,
  ListChecks,
  Plus,
  Trash2,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { AssessmentDTO, CloDTO, TopicDTO } from "@/types/syllabus"
import type { TopicImportData } from "@/types/syllabusImport"

export type ReadingDraft = {
  title: string
  author: string
  publisher: string
  year: string
  usageType?: string
}

type TopicDetail = {
  clo: string
  assessments: string
  resources: string
  level: string
  weight: string
}

type PlannedActivityDraft = {
  week: number
  topic: string
  clo: string
  assessments: string
  learningActivities: string
  resources: string
}

export type StandardSyllabusSupplemental = {
  schemaVersion: 1
  internalNotes: string
  personResponsible: string
  dateRevised: string
  creditPoints: string
  lectureCredits: string
  laboratoryCredits: string
  workloadStudentResponsibility: string
  ploCodes: string[]
  cloPloMatrix: Record<string, Record<string, string>>
  topicDetails: Record<string, TopicDetail>
  plannedActivities: PlannedActivityDraft[]
  assessmentCloMatrix: Record<string, Record<string, string>>
  readings: ReadingDraft[]
  assessmentPassNote: string
  contentNote: string
  [key: string]: unknown
}

export type StandardSyllabusFormSectionsProps = {
  clos: CloDTO[]
  onClosChange: (value: CloDTO[]) => void
  topics: TopicDTO[]
  onTopicsChange: (value: TopicDTO[]) => void
  assessments: AssessmentDTO[]
  onAssessmentsChange: (value: AssessmentDTO[]) => void
  supplemental: StandardSyllabusSupplemental
  onSupplementalChange: (value: StandardSyllabusSupplemental) => void
  examForms: string
  onExamFormsChange: (value: string) => void
  examRequirements: string
  onExamRequirementsChange: (value: string) => void
  sectionNumbers?: Partial<StandardSyllabusSectionNumbers>
  importedContentTopics?: TopicImportData[]
  readOnly?: boolean
}

export type StandardSyllabusSectionNumbers = {
  clos: string
  content: string
  matrix: string
  examination: string
  plannedActivities: string
  assessment: string
  readings: string
}

export const WORD_V8_SECTION_NUMBERS: StandardSyllabusSectionNumbers = {
  clos: "3",
  content: "4",
  matrix: "5",
  examination: "7–8",
  plannedActivities: "9",
  assessment: "10",
  readings: "12",
}

const LEGACY_PDF_SECTION_NUMBERS: StandardSyllabusSectionNumbers = {
  clos: "",
  content: "",
  matrix: "2",
  examination: "",
  plannedActivities: "3",
  assessment: "4",
  readings: "",
}

const BLOOM_LEVELS = ["REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE"]
const COMPETENCY_LEVELS = ["KNOWLEDGE", "SKILL", "ATTITUDE"]
const CONTENT_LEVELS = ["I", "T", "U", "I, T", "I, U", "T, U", "I, T, U"]
const LEARNING_ACTIVITY_OPTIONS = ["LECTURE", "LAB", "EXERCISES"]
const MATRIX_VALUES = ["", "x", "xx", "xxx"]

const emptySupplemental = (): StandardSyllabusSupplemental => ({
  schemaVersion: 1,
  internalNotes: "",
  personResponsible: "",
  dateRevised: "",
  creditPoints: "",
  lectureCredits: "",
  laboratoryCredits: "",
  workloadStudentResponsibility: "",
  ploCodes: ["SLO1", "SLO2", "SLO3", "SLO4", "SLO5", "SLO6"],
  cloPloMatrix: {},
  topicDetails: {},
  plannedActivities: [],
  assessmentCloMatrix: {},
  readings: [],
  assessmentPassNote: "Target percentage of students having scores greater than 50 out of 100.",
  contentNote: "Weight: lecture session (3 hours). Teaching levels: I (Introduce); T (Teach); U (Utilize).",
})

const asRecord = (value: unknown): Record<string, unknown> =>
  value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {}

const asText = (value: unknown) => typeof value === "string" ? value : ""

const parseMatrix = (value: unknown) => Object.fromEntries(
  Object.entries(asRecord(value)).map(([rowKey, row]) => [
    rowKey,
    Object.fromEntries(Object.entries(asRecord(row)).map(([key, cell]) => [key, asText(cell)])),
  ]),
) as Record<string, Record<string, string>>

const parseReadings = (value: unknown): ReadingDraft[] => Array.isArray(value)
  ? value.map((item) => {
      if (typeof item === "string") return { title: item, author: "", publisher: "", year: "" }
      const source = asRecord(item)
      return {
        title: asText(source.title ?? source.text ?? source.value),
        author: asText(source.author),
        publisher: asText(source.publisher),
        year: String(source.year ?? ""),
        usageType: asText(source.usageType) || "REQUIRED",
      }
    })
  : []

const parseTopicContentNotes = (raw?: string): Pick<TopicDetail, "weight" | "level"> => {
  if (!raw?.trim()) return { weight: "", level: "" }
  try {
    const source = asRecord(JSON.parse(raw))
    return {
      weight: asText(source.contentWeight ?? source.weight),
      level: asText(source.contentLevel ?? source.teachingLevel ?? source.level),
    }
  } catch {
    return { weight: "", level: "" }
  }
}

// eslint-disable-next-line react-refresh/only-export-components
export const parseStandardSyllabusNotes = (raw?: string | null): StandardSyllabusSupplemental => {
  if (!raw?.trim()) return emptySupplemental()
  try {
    const source = asRecord(JSON.parse(raw))
    const defaults = emptySupplemental()
    const topicDetails = Object.fromEntries(
      Object.entries(asRecord(source.topicDetails)).map(([key, value]) => {
        const detail = asRecord(value)
        return [key, {
          clo: asText(detail.clo),
          assessments: asText(detail.assessments),
          resources: asText(detail.resources),
          level: asText(detail.level),
          weight: asText(detail.weight),
        }]
      }),
    )
    return {
      ...source,
      schemaVersion: 1,
      internalNotes: asText(source.internalNotes),
      personResponsible: asText(source.personResponsible) || asText(source.instructor),
      dateRevised: asText(source.dateRevised),
      creditPoints: asText(source.creditPoints ?? source.ects),
      lectureCredits: asText(source.lectureCredits ?? source.creditsTheory),
      laboratoryCredits: asText(source.laboratoryCredits ?? source.creditsPractice),
      workloadStudentResponsibility: asText(source.workloadStudentResponsibility),
      ploCodes: Array.isArray(source.ploCodes) ? source.ploCodes.map(String).filter(Boolean) : defaults.ploCodes,
      cloPloMatrix: parseMatrix(source.cloPloMatrix ?? source.loMatrix),
      topicDetails,
      plannedActivities: Array.isArray(source.plannedActivities)
        ? source.plannedActivities.map((item, index) => {
            const activity = asRecord(item)
            return {
              week: Number(activity.week) || index + 1,
              topic: asText(activity.topic),
              clo: asText(activity.clo),
              assessments: asText(activity.assessments),
              learningActivities: asText(activity.learningActivities ?? activity.activities),
              resources: asText(activity.resources),
            }
          })
        : [],
      assessmentCloMatrix: parseMatrix(source.assessmentCloMatrix),
      readings: parseReadings(source.readings ?? source.references),
      assessmentPassNote: asText(source.assessmentPassNote) || defaults.assessmentPassNote,
      contentNote: asText(source.contentNote) || defaults.contentNote,
    }
  } catch {
    return { ...emptySupplemental(), internalNotes: raw }
  }
}

// eslint-disable-next-line react-refresh/only-export-components
export const serializeStandardSyllabusNotes = (value: StandardSyllabusSupplemental) => JSON.stringify(value)

const inputClass = "h-9 min-w-0 border-slate-200 bg-white text-xs font-semibold text-slate-900 shadow-none focus-visible:border-[#15949a] focus-visible:ring-[#15949a]/15 disabled:bg-white disabled:text-slate-900 disabled:opacity-100"
const textareaClass = "min-h-20 w-full resize-y rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs leading-5 outline-none transition focus:border-[#15949a] focus:ring-2 focus:ring-[#15949a]/10"
const compactTextarea = `${textareaClass} min-h-16 px-2.5`

const optionList = (options: string[], current?: string) =>
  current && !options.includes(current) ? [...options, current] : options

const parseSelectedValues = (value?: string) => Array.from(new Set(
  (value ?? "").split(/[,;\n]+/).map((item) => item.trim()).filter(Boolean),
))

const serializeSelectedValues = (values: string[]) => values.join(", ")

const replaceSelectedValue = (value: string | undefined, oldValue: string, newValue?: string) =>
  serializeSelectedValues(parseSelectedValues(value).flatMap((item) => (
    item === oldValue ? (newValue ? [newValue] : []) : [item]
  )))

const effectiveCloCode = (clo: CloDTO, index: number) => clo.code.trim() || `CLO${index + 1}`

const renameColumn = (matrix: Record<string, Record<string, string>>, oldKey: string, newKey: string) =>
  Object.fromEntries(Object.entries(matrix).map(([rowKey, row]) => {
    if (oldKey === newKey || row[oldKey] === undefined) return [rowKey, row]
    const nextRow = { ...row, [newKey]: row[newKey] ?? row[oldKey] }
    delete nextRow[oldKey]
    return [rowKey, nextRow]
  }))

const removeColumn = (matrix: Record<string, Record<string, string>>, key: string) =>
  Object.fromEntries(Object.entries(matrix).map(([rowKey, row]) => {
    const nextRow = { ...row }
    delete nextRow[key]
    return [rowKey, nextRow]
  }))

const reindexAfterDelete = <T,>(record: Record<string, T>, deleted: number) =>
  Object.fromEntries(Object.entries(record).flatMap(([key, value]) => {
    const index = Number(key)
    if (!Number.isInteger(index)) return [[key, value]]
    if (index === deleted) return []
    return [[String(index > deleted ? index - 1 : index), value]]
  }))

export default function StandardSyllabusFormSections({
  clos, onClosChange,
  topics, onTopicsChange,
  assessments, onAssessmentsChange,
  supplemental, onSupplementalChange,
  examForms, onExamFormsChange,
  examRequirements, onExamRequirementsChange,
  sectionNumbers,
  importedContentTopics,
  readOnly = false,
}: StandardSyllabusFormSectionsProps) {
  const numbers = { ...LEGACY_PDF_SECTION_NUMBERS, ...sectionNumbers }
  const cloCodes = clos.map(effectiveCloCode)
  const patchSupplemental = (patch: Partial<StandardSyllabusSupplemental>) =>
    onSupplementalChange({ ...supplemental, ...patch })
  const updateAt = <T,>(items: T[], index: number, patch: Partial<T>) =>
    items.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item)

  const updateClo = (index: number, patch: Partial<CloDTO>) => {
    const oldCode = effectiveCloCode(clos[index], index)
    const next = updateAt(clos, index, patch)
    const newCode = effectiveCloCode(next[index], index)
    onClosChange(next)
    if (oldCode !== newCode) {
      const matrix = { ...supplemental.cloPloMatrix }
      matrix[newCode] = { ...matrix[oldCode], ...matrix[newCode] }
      delete matrix[oldCode]
      onSupplementalChange({
        ...supplemental,
        cloPloMatrix: matrix,
        assessmentCloMatrix: renameColumn(supplemental.assessmentCloMatrix, oldCode, newCode),
        topicDetails: Object.fromEntries(Object.entries(supplemental.topicDetails).map(([key, detail]) => [
          key,
          { ...detail, clo: replaceSelectedValue(detail.clo, oldCode, newCode) },
        ])),
        plannedActivities: supplemental.plannedActivities.map((activity) => ({
          ...activity,
          clo: replaceSelectedValue(activity.clo, oldCode, newCode),
        })),
      })
    }
  }

  const deleteClo = (index: number) => {
    const code = cloCodes[index]
    const matrix = { ...supplemental.cloPloMatrix }
    delete matrix[code]
    onClosChange(clos.filter((_, i) => i !== index).map((clo, i) => ({ ...clo, orderIndex: i + 1 })))
    onSupplementalChange({
      ...supplemental,
      cloPloMatrix: matrix,
      assessmentCloMatrix: removeColumn(supplemental.assessmentCloMatrix, code),
      topicDetails: Object.fromEntries(Object.entries(supplemental.topicDetails).map(([key, detail]) => [
        key,
        { ...detail, clo: replaceSelectedValue(detail.clo, code) },
      ])),
      plannedActivities: supplemental.plannedActivities.map((activity) => ({
        ...activity,
        clo: replaceSelectedValue(activity.clo, code),
      })),
    })
  }

  const updateAssessment = (index: number, patch: Partial<AssessmentDTO>) => {
    const oldName = assessments[index].name.trim()
    const next = updateAt(assessments, index, patch)
    const newName = next[index].name.trim()
    onAssessmentsChange(next)
    if (oldName && oldName !== newName) {
      onTopicsChange(topics.map((topic) => ({
        ...topic,
        assessments: replaceSelectedValue(topic.assessments, oldName, newName),
      })))
      patchSupplemental({
        topicDetails: Object.fromEntries(Object.entries(supplemental.topicDetails).map(([key, detail]) => [
          key,
          { ...detail, assessments: replaceSelectedValue(detail.assessments, oldName, newName) },
        ])),
        plannedActivities: supplemental.plannedActivities.map((activity) => ({
          ...activity,
          assessments: replaceSelectedValue(activity.assessments, oldName, newName),
        })),
      })
    }
  }

  const deleteAssessment = (index: number) => {
    const name = assessments[index].name.trim()
    onAssessmentsChange(assessments.filter((_, i) => i !== index).map((item, i) => ({ ...item, orderIndex: i + 1 })))
    onTopicsChange(topics.map((topic) => ({
      ...topic,
      assessments: replaceSelectedValue(topic.assessments, name),
    })))
    patchSupplemental({
      assessmentCloMatrix: reindexAfterDelete(supplemental.assessmentCloMatrix, index),
      topicDetails: Object.fromEntries(Object.entries(supplemental.topicDetails).map(([key, detail]) => [
        key,
        { ...detail, assessments: replaceSelectedValue(detail.assessments, name) },
      ])),
      plannedActivities: supplemental.plannedActivities.map((activity) => ({
        ...activity,
        assessments: replaceSelectedValue(activity.assessments, name),
      })),
    })
  }

  const addTopic = () => onTopicsChange([...topics, {
    weekNumber: Math.max(1, ...topics.map((topic) => Number(topic.weekNumber) + 1)),
    orderInWeek: 1,
    name: "",
    teachingHours: 3,
    labHours: 0,
    selfStudyHours: 6,
    topicType: "LECTURE",
    teachingMethod: "Lecture",
    learningActivity: "",
    assessments: "",
    resources: "",
  }])

  const deleteTopic = (index: number) => {
    onTopicsChange(topics.filter((_, i) => i !== index))
    patchSupplemental({ topicDetails: reindexAfterDelete(supplemental.topicDetails, index) })
  }

  const getTopicDetail = (topic: TopicDTO, index: number): TopicDetail => {
    const stored = supplemental.topicDetails[String(index)]
    const imported = parseTopicContentNotes(topic.notes)
    const raw = importedContentTopics?.[index]
    return {
      clo: stored?.clo ?? "",
      assessments: stored?.assessments ?? topic.assessments ?? "",
      resources: stored?.resources ?? topic.resources ?? "",
      level: stored?.level
        || asText(topic.teachingLevel ?? topic.contentLevel)
        || asText(raw?.teachingLevel ?? raw?.contentLevel)
        || imported.level,
      weight: stored?.weight
        || asText(topic.contentWeight)
        || asText(raw?.contentWeight ?? raw?.teachingHours)
        || imported.weight,
    }
  }
  const updateTopicDetail = (topic: TopicDTO, index: number, patch: Partial<TopicDetail>) =>
    patchSupplemental({
      topicDetails: {
        ...supplemental.topicDetails,
        [String(index)]: { ...getTopicDetail(topic, index), ...patch },
      },
    })

  const plannedActivities: PlannedActivityDraft[] = supplemental.plannedActivities.length > 0
    ? supplemental.plannedActivities
    : topics.map((topic, index) => ({
        week: topic.weekNumber || index + 1,
        topic: topic.name,
        clo: getTopicDetail(topic, index).clo,
        assessments: getTopicDetail(topic, index).assessments || topic.assessments || "",
        learningActivities: serializeSelectedValues(Array.from(new Set([
          ...parseSelectedValues(topic.teachingMethod),
          ...parseSelectedValues(topic.learningActivity),
        ]))),
        resources: topic.resources || "",
      }))
  const updatePlannedActivity = (index: number, patch: Partial<PlannedActivityDraft>) =>
    patchSupplemental({ plannedActivities: updateAt(plannedActivities, index, patch) })
  const deletePlannedActivity = (index: number) =>
    patchSupplemental({ plannedActivities: plannedActivities.filter((_, itemIndex) => itemIndex !== index) })
  const addPlannedActivity = () => patchSupplemental({
    plannedActivities: [...plannedActivities, {
      week: Math.max(0, ...plannedActivities.map((item) => Number(item.week) || 0)) + 1,
      topic: "",
      clo: "",
      assessments: "",
      learningActivities: "",
      resources: "",
    }],
  })

  const totalWeight = assessments.reduce((sum, item) => sum + Number(item.weightPercent || 0), 0)

  return (
    <div className="flex flex-col gap-6">
      <Section number={numbers.clos} order={1} icon={GraduationCap} title="Course Learning Outcomes (CLO)" description="Mỗi CLO gắn với một mức năng lực và Bloom level; tất cả nội dung đều có thể chỉnh sửa." accent="teal">
        <div className="grid gap-4 xl:grid-cols-2">
          {clos.map((clo, index) => (
            <article key={clo.id ?? `clo-${index}`} className="rounded-xl border border-slate-200 bg-white p-4 hover:shadow-sm">
              <div className="mb-4 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="flex size-9 items-center justify-center rounded-lg bg-[#e9f7f7] text-xs font-bold text-[#007d84]">{String(index + 1).padStart(2, "0")}</span>
                  <Input className="h-9 w-28 bg-slate-50 font-mono text-xs font-bold uppercase text-[#007d84]" value={clo.code} onChange={(event) => updateClo(index, { code: event.target.value })} aria-label={`CLO ${index + 1} code`} />
                </div>
                <DeleteButton label={`Delete ${effectiveCloCode(clo, index)}`} onClick={() => deleteClo(index)} />
              </div>
              <Field label="Learning outcome" required><textarea className={textareaClass} value={clo.description} onChange={(event) => updateClo(index, { description: event.target.value })} placeholder="Students will be able to..." /></Field>
              <Field label="Vietnamese description"><textarea className={compactTextarea} value={clo.descriptionVn ?? ""} onChange={(event) => updateClo(index, { descriptionVn: event.target.value })} /></Field>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <Field label="Competency"><ValueSelect value={clo.competencyLevel} options={optionList(COMPETENCY_LEVELS, clo.competencyLevel)} onChange={(value) => updateClo(index, { competencyLevel: value })} placeholder="Select competency" /></Field>
                <Field label="Bloom level"><ValueSelect value={clo.bloomLevel} options={optionList(BLOOM_LEVELS, clo.bloomLevel)} onChange={(value) => updateClo(index, { bloomLevel: value })} placeholder="Select Bloom level" /></Field>
              </div>
            </article>
          ))}
        </div>
        {clos.length === 0 && <Empty icon={GraduationCap} text="No CLO has been defined." />}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <AddButton label="Add CLO" onClick={() => onClosChange([...clos, { code: `CLO${clos.length + 1}`, description: "", competencyLevel: "KNOWLEDGE", bloomLevel: "UNDERSTAND", orderIndex: clos.length + 1 }])} />
          <div className="flex flex-wrap gap-2">{COMPETENCY_LEVELS.map((level) => <span key={level} className="rounded-full border bg-slate-50 px-3 py-1 text-[11px] text-slate-600">{level}: {clos.filter((clo) => clo.competencyLevel === level).length}</span>)}</div>
        </div>
      </Section>

      <Section number={numbers.content} order={2} icon={FileText} title="Content" description="Nội dung giảng dạy, trọng số và mức độ: I — Introduce, T — Teach, U — Utilize." accent="blue">
        <Field label="Ghi chú trọng số / mức độ"><Input className={inputClass} value={supplemental.contentNote} onChange={(event) => patchSupplemental({ contentNote: event.target.value })} /></Field>
        <Table minWidth="1180px"><thead><HeaderRow><Th className="w-16 text-center">No.</Th><Th>Topic</Th><Th className="w-24">Teaching</Th><Th className="w-24">Lab</Th><Th className="w-28">Self-study</Th><Th className="w-52">Mapped CLOs</Th><Th className="w-28">Weight</Th><Th className="w-40">Level</Th><Th className="w-14" /></HeaderRow></thead><tbody>
          {topics.map((topic, index) => {
            const detail = getTopicDetail(topic, index)
            return <tr key={topic.id ?? `content-${index}`} className="align-top hover:bg-slate-50/50"><Td className="text-center font-semibold text-slate-500">{index + 1}</Td><Td>{readOnly ? <p className="min-h-12 px-2 py-1 text-xs font-medium leading-5 text-slate-900">{topic.name || "—"}</p> : <textarea className={`${compactTextarea} font-medium text-slate-900`} value={topic.name} onChange={(event) => onTopicsChange(updateAt(topics, index, { name: event.target.value }))} />}</Td><Td><NumberBox label={`Topic ${index + 1} teaching hours`} value={topic.teachingHours} onChange={(value) => onTopicsChange(updateAt(topics, index, { teachingHours: value }))} /></Td><Td><NumberBox label={`Topic ${index + 1} lab hours`} value={topic.labHours} onChange={(value) => onTopicsChange(updateAt(topics, index, { labHours: value }))} /></Td><Td><NumberBox label={`Topic ${index + 1} self-study hours`} value={topic.selfStudyHours} onChange={(value) => onTopicsChange(updateAt(topics, index, { selfStudyHours: value }))} /></Td><Td><MultiValuePicker value={detail.clo} options={cloCodes} onChange={(value) => updateTopicDetail(topic, index, { clo: value })} placeholder="Select CLOs" readOnly={readOnly} /></Td><Td>{readOnly ? <span className="flex min-h-9 items-center justify-center text-sm font-bold text-[#006f76]">{detail.weight || "—"}</span> : <Input type="number" min="0" step="0.5" className={`${inputClass} text-center text-sm font-bold text-[#006f76]`} value={detail.weight} onChange={(event) => updateTopicDetail(topic, index, { weight: event.target.value })} placeholder="Weight" />}</Td><Td>{readOnly ? <span className="inline-flex min-h-8 items-center rounded-md bg-[#e9f7f7] px-3 text-xs font-bold text-[#006f76]">{detail.level || "—"}</span> : <ValueSelect value={detail.level} options={optionList(CONTENT_LEVELS, detail.level)} onChange={(value) => updateTopicDetail(topic, index, { level: value })} placeholder="I / T / U" />}</Td><Td>{!readOnly && <DeleteButton label={`Delete topic ${index + 1}`} onClick={() => deleteTopic(index)} />}</Td></tr>
          })}
        </tbody></Table>
        {topics.length === 0 && <Empty icon={FileText} text="No course content has been added." />}
        {!readOnly && <AddButton label="Add Content Row" onClick={addTopic} />}
      </Section>

      <Section number={numbers.matrix} order={4} icon={Grid3X3} title="Learning Outcomes Matrix (CLO × SLO)" description="Bấm vào từng ô để đổi mức đóng góp: trống → x → xx → xxx. Nhãn SLO cũng có thể chỉnh sửa." accent="violet">
        <div className="flex flex-wrap gap-2">{supplemental.ploCodes.map((code, index) => <div key={`slo-${index}`} className="flex items-center rounded-lg border bg-slate-50 p-1"><Input className="h-8 w-24 border-0 bg-transparent text-center text-xs font-bold uppercase shadow-none" value={code} onChange={(event) => {
          const nextCode = event.target.value
          patchSupplemental({ ploCodes: supplemental.ploCodes.map((item, i) => i === index ? nextCode : item), cloPloMatrix: renameColumn(supplemental.cloPloMatrix, code, nextCode) })
        }} /><DeleteButton label={`Delete ${code}`} onClick={() => patchSupplemental({ ploCodes: supplemental.ploCodes.filter((_, i) => i !== index), cloPloMatrix: removeColumn(supplemental.cloPloMatrix, code) })} /></div>)}<AddButton label="Add SLO" onClick={() => patchSupplemental({ ploCodes: [...supplemental.ploCodes, `SLO${supplemental.ploCodes.length + 1}`] })} /></div>
        <Table minWidth="720px"><thead><HeaderRow><Th>CLO / SLO</Th>{supplemental.ploCodes.map((code, index) => <Th key={`head-${index}`} className="text-center">{code || `SLO${index + 1}`}</Th>)}</HeaderRow></thead><tbody>{cloCodes.map((cloCode, cloIndex) => <tr key={`${cloCode}-${cloIndex}`}><Td className="bg-slate-50 font-mono text-xs font-bold text-[#007d84]">{cloCode}</Td>{supplemental.ploCodes.map((sloCode, sloIndex) => {
          const value = supplemental.cloPloMatrix[cloCode]?.[sloCode] ?? ""
          return <Td key={`${cloIndex}-${sloIndex}`} className="text-center"><button type="button" className={`mx-auto flex size-10 items-center justify-center rounded-lg border text-xs font-bold ${value ? "border-[#8bd0d3] bg-[#eaf8f8] text-[#007d84]" : "border-dashed text-slate-300"}`} onClick={() => {
            const currentIndex = MATRIX_VALUES.indexOf(value.toLowerCase())
            const next = MATRIX_VALUES[(currentIndex + 1) % MATRIX_VALUES.length]
            patchSupplemental({ cloPloMatrix: { ...supplemental.cloPloMatrix, [cloCode]: { ...supplemental.cloPloMatrix[cloCode], [sloCode]: next } } })
          }}>{value || "—"}</button></Td>
        })}</tr>)}</tbody></Table>
        <p className="rounded-lg border bg-slate-50 px-4 py-3 text-xs text-slate-500"><b className="text-[#007d84]">x</b> low · <b className="text-[#007d84]">xx</b> medium · <b className="text-[#007d84]">xxx</b> high contribution</p>
      </Section>

      <Section number={numbers.plannedActivities} order={5} icon={ListChecks} title="Planned Learning Activities and Teaching Methods" description="Kế hoạch giảng dạy theo tuần; CLO, đánh giá và hoạt động học tập dùng chung dữ liệu từ các phần tương ứng." accent="amber">
        <Table minWidth="980px"><thead><HeaderRow><Th className="w-20">Week</Th><Th>Topic</Th><Th className="w-48">CLO</Th><Th className="w-56">Assessments</Th><Th className="w-56">Learning activities / methods</Th><Th>Resources</Th><Th className="w-14" /></HeaderRow></thead><tbody>{plannedActivities.map((activity, index) => {
          return <tr key={`plan-${activity.week}-${index}`} className="align-top"><Td><Input type="number" min={1} className={inputClass} value={activity.week} onChange={(event) => updatePlannedActivity(index, { week: Number(event.target.value) })} /></Td><Td><textarea className={compactTextarea} value={activity.topic} onChange={(event) => updatePlannedActivity(index, { topic: event.target.value })} /></Td><Td><MultiValuePicker value={activity.clo} options={cloCodes} onChange={(value) => updatePlannedActivity(index, { clo: value })} placeholder="Select CLOs" readOnly={readOnly} /></Td><Td><MultiValuePicker value={activity.assessments} options={assessments.map((item) => item.name.trim()).filter(Boolean)} onChange={(value) => updatePlannedActivity(index, { assessments: value })} placeholder="Select assessments" readOnly={readOnly} /></Td><Td><MultiValuePicker value={activity.learningActivities} options={LEARNING_ACTIVITY_OPTIONS} onChange={(value) => updatePlannedActivity(index, { learningActivities: value })} placeholder="Select activities" readOnly={readOnly} /></Td><Td><textarea className={compactTextarea} value={activity.resources} onChange={(event) => updatePlannedActivity(index, { resources: event.target.value })} /></Td><Td>{!readOnly && <DeleteButton label={`Delete week ${activity.week}`} onClick={() => deletePlannedActivity(index)} />}</Td></tr>
        })}</tbody></Table>
        {!readOnly && <AddButton label="Add Week / Topic" onClick={addPlannedActivity} />}
      </Section>

      <Section number={numbers.assessment} order={6} icon={CheckCircle2} title="Assessment Plan" description="Tỷ trọng của từng hình thức đánh giá theo mỗi CLO; tổng trọng số nên bằng 100%." accent="green">
        <div className="grid gap-3 sm:grid-cols-3"><Metric label="Components" value={assessments.length} /><Metric label="Allocated" value={`${totalWeight}%`} state={totalWeight === 100 ? "ok" : "warn"} /><Metric label="Remaining" value={`${100 - totalWeight}%`} state={totalWeight === 100 ? "ok" : "warn"} /></div>
        <Table minWidth="1040px"><thead><HeaderRow><Th>Assessment</Th><Th className="w-28">Weight %</Th><Th className="w-36">Score range</Th>{cloCodes.map((code, index) => <Th key={`${code}-${index}`} className="text-center">{code}</Th>)}<Th className="w-14" /></HeaderRow></thead><tbody>{assessments.map((assessment, index) => <tr key={assessment.id ?? `assessment-${index}`} className="align-top"><Td><div className="space-y-2"><Input className={inputClass} value={assessment.name} onChange={(event) => updateAssessment(index, { name: event.target.value })} /><Input className={inputClass} value={assessment.assessmentType ?? ""} onChange={(event) => updateAssessment(index, { assessmentType: event.target.value })} placeholder="Assessment type" /></div></Td><Td><NumberBox label="Weight percent" value={assessment.weightPercent} onChange={(value) => updateAssessment(index, { weightPercent: value })} /></Td><Td><div className="grid grid-cols-2 gap-1"><NumberBox label="Minimum score" value={assessment.minScore} onChange={(value) => updateAssessment(index, { minScore: value })} /><NumberBox label="Maximum score" value={assessment.maxScore} onChange={(value) => updateAssessment(index, { maxScore: value })} /></div></Td>{cloCodes.map((code, cloIndex) => <Td key={`${index}-${cloIndex}`}><Input type="number" min={0} max={100} className={inputClass} value={supplemental.assessmentCloMatrix[String(index)]?.[code] ?? ""} onChange={(event) => patchSupplemental({ assessmentCloMatrix: { ...supplemental.assessmentCloMatrix, [String(index)]: { ...supplemental.assessmentCloMatrix[String(index)], [code]: event.target.value } } })} /></Td>)}<Td><DeleteButton label={`Delete ${assessment.name || `assessment ${index + 1}`}`} onClick={() => deleteAssessment(index)} /></Td></tr>)}</tbody></Table>
        {assessments.length === 0 && <Empty icon={CheckCircle2} text="No assessment component has been added." />}
        <div className="flex flex-wrap items-start justify-between gap-4"><AddButton label="Add Assessment" onClick={() => onAssessmentsChange([...assessments, { name: "", assessmentType: "ASSIGNMENT", weightPercent: 0, minScore: 0, maxScore: 100, orderIndex: assessments.length + 1 }])} /><Field label="Assessment pass note" className="w-full max-w-2xl"><textarea className={compactTextarea} value={supplemental.assessmentPassNote} onChange={(event) => patchSupplemental({ assessmentPassNote: event.target.value })} /></Field></div>
      </Section>

      <Section number={numbers.examination} order={3} icon={BookOpen} title="Examination, Requirements & Reading List" description="Hình thức thi, yêu cầu học tập, tài liệu tham khảo và thông tin hiệu chỉnh." accent="rose">
        <Subsection title="Examination forms and study requirements" eyebrow="Examination">
          <div className="grid gap-4 lg:grid-cols-2"><Field label="Examination forms"><textarea className={`${textareaClass} min-h-28 text-sm`} value={examForms} onChange={(event) => onExamFormsChange(event.target.value)} placeholder="Short-answer questions, programming exercises..." /></Field><Field label="Study and examination requirements"><textarea className={`${textareaClass} min-h-28 text-sm`} value={examRequirements} onChange={(event) => onExamRequirementsChange(event.target.value)} placeholder="Attendance, participation, minimum score..." /></Field></div>
        </Subsection>
        <Subsection title="Reading list" eyebrow="References">
          <div className="space-y-3">{supplemental.readings.map((reading, index) => <article key={`reading-${index}`} className="grid gap-3 rounded-xl border bg-slate-50/50 p-4 lg:grid-cols-[2fr_1fr_100px_40px]"><Field label="Title" required><Input className={inputClass} value={reading.title} onChange={(event) => patchSupplemental({ readings: updateAt(supplemental.readings, index, { title: event.target.value }) })} /></Field><Field label="Author"><Input className={inputClass} value={reading.author} onChange={(event) => patchSupplemental({ readings: updateAt(supplemental.readings, index, { author: event.target.value }) })} /></Field><Field label="Year"><Input className={inputClass} value={reading.year} onChange={(event) => patchSupplemental({ readings: updateAt(supplemental.readings, index, { year: event.target.value }) })} /></Field><div className="flex items-end">{!readOnly && <DeleteButton label={`Delete ${reading.title || `reading ${index + 1}`}`} onClick={() => patchSupplemental({ readings: supplemental.readings.filter((_, i) => i !== index) })} />}</div></article>)}</div>
          {supplemental.readings.length === 0 && <Empty icon={BookOpen} text="No reading resource has been added." />}
          {!readOnly && <AddButton label="Add Reading" onClick={() => patchSupplemental({ readings: [...supplemental.readings, { title: "", author: "", publisher: "", year: "", usageType: "SUPPLEMENTARY" }] })} />}
        </Subsection>
        <Subsection title="Revision information" eyebrow="Revision"><div className="grid gap-4 lg:grid-cols-[220px_1fr]"><Field label="Date revised"><Input type="date" className={inputClass} value={supplemental.dateRevised} onChange={(event) => patchSupplemental({ dateRevised: event.target.value })} /></Field><Field label="Revision / reviewer notes"><textarea className={compactTextarea} value={supplemental.internalNotes} onChange={(event) => patchSupplemental({ internalNotes: event.target.value })} /></Field></div></Subsection>
      </Section>
    </div>
  )
}

type Accent = "teal" | "blue" | "violet" | "amber" | "green" | "rose"
const accents: Record<Accent, string> = { teal: "bg-[#007d84]", blue: "bg-[#3978b7]", violet: "bg-[#7668b5]", amber: "bg-[#c88928]", green: "bg-[#25866b]", rose: "bg-[#ad5268]" }

function Section({ number, order, icon: Icon, title, description, accent, children }: { number: string; order?: number; icon: typeof BookOpen; title: string; description: string; accent: Accent; children: React.ReactNode }) {
  return <section style={{ order }} className="overflow-hidden rounded-2xl border border-[#d8e4e7] bg-white shadow-[0_1px_2px_rgba(20,55,65,0.04)]"><header className="border-b bg-gradient-to-r from-[#f8fbfb] to-white px-5 py-5 sm:px-6"><div className="flex items-start gap-4">{number && <span className={`flex size-10 shrink-0 items-center justify-center rounded-xl text-sm font-bold text-white ${accents[accent]}`}>{number}</span>}<div><div className="flex items-center gap-2"><Icon className="size-4 text-[#58737d]" /><h2 className="font-bold text-[#17343d]">{title}</h2></div><p className="mt-1 text-xs leading-5 text-[#70858d]">{description}</p></div></div></header><div className="space-y-5 p-5 sm:p-6">{children}</div></section>
}

function Subsection({ title, eyebrow, children }: { title: string; eyebrow: string; children: React.ReactNode }) {
  return <section className="space-y-4 rounded-xl border border-slate-200 p-4 sm:p-5"><div><p className="text-[10px] font-bold uppercase tracking-[0.14em] text-[#007d84]">{eyebrow}</p><h3 className="mt-1 text-sm font-bold text-[#17343d]">{title}</h3></div>{children}</section>
}

function Field({ label, required = false, className = "", children }: { label: string; required?: boolean; className?: string; children: React.ReactNode }) {
  return <label className={`block space-y-1.5 text-[11px] font-semibold text-[#526d77] ${className}`}><span>{label}{required && <b className="ml-1 text-rose-500">*</b>}</span>{children}</label>
}

function Table({ minWidth, children }: { minWidth: string; children: React.ReactNode }) {
  return <div className="overflow-x-auto rounded-xl border border-slate-200"><table className="w-full border-collapse text-sm" style={{ minWidth }}>{children}</table></div>
}

function HeaderRow({ children }: { children: React.ReactNode }) { return <tr className="bg-[#f6f9fa] text-left text-[10px] font-bold uppercase tracking-[0.12em] text-[#69818a]">{children}</tr> }
function Th({ children, className = "" }: { children?: React.ReactNode; className?: string }) { return <th className={`border-b border-r border-slate-200 px-3 py-3 last:border-r-0 ${className}`}>{children}</th> }
function Td({ children, className = "" }: { children: React.ReactNode; className?: string }) { return <td className={`border-b border-r border-slate-200 p-2 align-top last:border-r-0 ${className}`}>{children}</td> }

function ValueSelect({ value, options, onChange, placeholder }: { value?: string; options: string[]; onChange: (value: string) => void; placeholder: string }) {
  return <Select value={value || undefined} onValueChange={onChange}><SelectTrigger className={inputClass}><SelectValue placeholder={placeholder} /></SelectTrigger><SelectContent>{options.map((option) => <SelectItem key={option} value={option}>{option.replaceAll("_", " ")}</SelectItem>)}</SelectContent></Select>
}

function MultiValuePicker({ value, options, onChange, placeholder, readOnly = false }: {
  value?: string
  options: string[]
  onChange: (value: string) => void
  placeholder: string
  readOnly?: boolean
}) {
  const selected = parseSelectedValues(value)
  const available = Array.from(new Set([...options, ...selected])).filter(Boolean)

  if (readOnly) {
    return <div className="flex min-h-9 flex-wrap gap-1.5 rounded-lg border border-slate-200 bg-white px-2 py-1.5">
      {selected.length > 0
        ? selected.map((item) => <span key={item} className="rounded-md bg-[#e9f7f7] px-2 py-1 text-[11px] font-bold text-[#006f76]">{item.replaceAll("_", " ")}</span>)
        : <span className="text-xs text-slate-400">—</span>}
    </div>
  }

  return <DropdownMenu>
    <DropdownMenuTrigger asChild>
      <Button type="button" variant="outline" className="h-auto min-h-9 w-full justify-start whitespace-normal border-slate-200 bg-white px-2 py-1.5 text-left text-xs font-semibold shadow-none">
        {selected.length > 0 ? selected.join(", ") : <span className="font-normal text-slate-400">{placeholder}</span>}
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent className="min-w-56">
      {available.length > 0 ? available.map((option) => (
        <DropdownMenuCheckboxItem
          key={option}
          checked={selected.includes(option)}
          onSelect={(event) => event.preventDefault()}
          onCheckedChange={(checked) => onChange(serializeSelectedValues(
            checked ? [...selected, option] : selected.filter((item) => item !== option),
          ))}
        >
          {option.replaceAll("_", " ")}
        </DropdownMenuCheckboxItem>
      )) : <p className="px-2 py-1.5 text-xs text-slate-400">No options available</p>}
    </DropdownMenuContent>
  </DropdownMenu>
}

function AddButton({ label, onClick }: { label: string; onClick: () => void }) { return <Button type="button" variant="outline" size="sm" onClick={onClick}><Plus className="size-4" />{label}</Button> }
function DeleteButton({ label, onClick }: { label: string; onClick: () => void }) { return <Button type="button" variant="ghost" size="sm" className="size-8 p-0 text-slate-400 hover:bg-rose-50 hover:text-rose-600" onClick={onClick} aria-label={label} title={label}><Trash2 className="size-4" /></Button> }
function NumberBox({ label, value, onChange }: { label: string; value?: number; onChange: (value: number) => void }) { return <Input type="number" min={0} step="0.01" className={`${inputClass} px-1 text-center`} value={value ?? 0} onChange={(event) => onChange(Number(event.target.value))} aria-label={label} title={label} /> }
function Metric({ label, value, state }: { label: string; value: string | number; state?: "ok" | "warn" }) { return <div className={`rounded-xl border px-4 py-3 ${state === "ok" ? "border-emerald-200 bg-emerald-50" : state === "warn" ? "border-amber-200 bg-amber-50" : "border-slate-200 bg-slate-50"}`}><p className="text-[10px] font-bold uppercase tracking-wider text-slate-500">{label}</p><p className="mt-1 text-xl font-bold text-[#17343d]">{value}</p></div> }
function Empty({ icon: Icon, text }: { icon: typeof BookOpen; text: string }) { return <div className="rounded-xl border border-dashed bg-slate-50/50 px-6 py-9 text-center"><Icon className="mx-auto size-8 text-slate-300" /><p className="mt-3 text-sm text-slate-500">{text}</p></div> }
