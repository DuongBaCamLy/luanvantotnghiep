import { formatVersionLabel } from "@/lib/syllabusVersion"
import {
  buildImportedComparisonTemplateProfile,
} from "@/lib/syllabusComparisonTemplate"
import type {
  SyllabusComparisonTemplateProfile,
} from "@/lib/syllabusComparisonTemplate"
import type { CreateSyllabusRequest, TopicDTO } from "@/types/syllabus"
import type {
  SyllabusImportData,
  TopicImportData,
  ValidSyllabusImportPreviewResponse,
  WeeklyActivityItem,
} from "@/types/syllabusImport"

const text = (value: unknown) => String(value ?? "").trim()
const normalizedText = (value: unknown) => text(value).toUpperCase().replace(/\s+/g, " ")

const finiteNumber = (value: unknown, fallback: number) => {
  if (value == null || (typeof value === "string" && !value.trim())) return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

const importedContentNotes = (source: TopicImportData) => {
  if (text(source.notes)) return source.notes
  const weight = text(source.contentWeight ?? source.teachingHours)
  const level = text(source.teachingLevel ?? source.contentLevel)
  return weight || level
    ? JSON.stringify({ schemaVersion: 1, contentWeight: weight, contentLevel: level })
    : undefined
}

export const normalizeImportedCompetencyLevel = (value?: string) => {
  const normalized = normalizedText(value)
  if (normalized.includes("SKILL")) return "SKILL"
  if (normalized.includes("ATTITUDE")) return "ATTITUDE"
  return "KNOWLEDGE"
}

export const normalizeImportedBloomLevel = (value?: string) => {
  const normalized = normalizedText(value)
  const match = ["REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE"]
    .find((level) => normalized.includes(level))
  return match ?? "UNDERSTAND"
}

export const normalizeImportedAssessmentType = (value?: string) => {
  const normalized = normalizedText(value).replace(/[\s/-]+/g, "_")
  const aliases: Record<string, string> = {
    LAB: "LAB_REPORT",
    LABS: "LAB_REPORT",
    LABORATORY: "LAB_REPORT",
    LAB_REPORTS: "LAB_REPORT",
    MIDTERM: "MIDTERM_EXAM",
    MID_TERM: "MIDTERM_EXAM",
    FINAL: "FINAL_EXAM",
    FINAL_EXAMINATION: "FINAL_EXAM",
  }
  const resolved = aliases[normalized] ?? normalized
  return [
    "QUIZ",
    "ASSIGNMENT",
    "LAB_REPORT",
    "MIDTERM_EXAM",
    "FINAL_EXAM",
    "PROJECT",
    "PRESENTATION",
    "PARTICIPATION",
  ].includes(resolved) ? resolved : "ASSIGNMENT"
}

export const normalizeImportedTopicType = (value?: string) => {
  const normalized = normalizedText(value).replace(/[\s/-]+/g, "_")
  if (!normalized) return undefined
  const aliases: Record<string, string> = {
    THEORY: "LECTURE",
    LABORATORY: "LAB",
    PRACTICE: "LAB",
    SELFSTUDY: "SELF_STUDY",
  }
  const resolved = aliases[normalized] ?? normalized
  return ["LECTURE", "LAB", "SEMINAR", "PROJECT", "SELF_STUDY"].includes(resolved)
    ? resolved
    : undefined
}

export const normalizeReadingUsage = (value?: string): "REQUIRED" | "RECOMMENDED" | "SUPPLEMENTARY" => {
  const normalized = normalizedText(value)
  if (normalized.includes("RECOMMEND")) return "RECOMMENDED"
  if (normalized.includes("SUPPLEMENT") || normalized.includes("OPTIONAL") || normalized.includes("REFERENCE")) {
    return "SUPPLEMENTARY"
  }
  return "REQUIRED"
}

const normalizeCourseTypes = (value?: string) => {
  const normalized = normalizedText(value)
  const values: string[] = []
  if (normalized.includes("COMPULSORY") || normalized.includes("REQUIRED")) values.push("Compulsory")
  if (normalized.includes("ELECTIVE") || normalized.includes("OPTIONAL")) values.push("Elective")
  if (normalized.includes("GENERAL")) values.push("General")
  if (values.length > 0) return JSON.stringify(values)
  return value ?? ""
}

const normalizeMatrixValue = (value?: string) => {
  const normalized = normalizedText(value)
  if (["3", "A", "U", "XXX"].includes(normalized)) return "xxx"
  if (["2", "D", "T", "XX"].includes(normalized)) return "xx"
  if (["1", "I", "X"].includes(normalized)) return "x"
  return normalized.toLowerCase()
}

const normalizeDateForInput = (value?: string) => {
  const raw = text(value)
  if (!raw) return ""
  const iso = raw.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
  if (iso) return `${iso[1]}-${iso[2].padStart(2, "0")}-${iso[3].padStart(2, "0")}`
  const numeric = raw.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/)
  if (numeric) return `${numeric[3]}-${numeric[2].padStart(2, "0")}-${numeric[1].padStart(2, "0")}`
  const parsed = new Date(raw)
  return Number.isNaN(parsed.getTime()) ? "" : parsed.toISOString().slice(0, 10)
}

const canonicalCode = (value: string, prefix: "CLO" | "PLO") => {
  const compact = normalizedText(value).replace(/\s+/g, "")
  if (!compact) return ""
  if (prefix === "PLO" && /^SLO\d+$/i.test(compact)) return compact
  if (compact.startsWith(prefix)) return compact
  return /^\d+$/.test(compact) ? `${prefix}${compact}` : compact
}

const resolveCloCode = (data: SyllabusImportData, value: string) => {
  const canonical = canonicalCode(value, "CLO")
  return data.clos?.find((clo) => canonicalCode(clo.code, "CLO") === canonical)?.code || canonical
}

const canonicalizeCloList = (data: SyllabusImportData, value?: string) => Array.from(new Set(
  text(value)
    .split(/[,;\n]+/)
    .flatMap((part) => part.match(/(?:CLO\s*)?\d+/gi) ?? [part])
    .map((part) => resolveCloCode(data, part))
    .filter(Boolean),
)).join(", ")

const resolveWeeklyAssessmentList = (data: SyllabusImportData, value?: string) => {
  const assessmentNames = (data.assessments ?? []).map((item) => text(item.name)).filter(Boolean)
  return Array.from(new Set(text(value).split(/[,;\n]+/).map((item) => item.trim()).filter(Boolean).map((item) => {
    const normalized = normalizedText(item)
    const match = assessmentNames.find((name) => {
      const candidate = normalizedText(name)
      if (normalized.includes("MIDTERM")) return candidate.includes("MIDTERM")
      if (normalized.includes("FINAL")) return candidate.includes("FINAL")
      if (normalized.includes("QUIZ") || normalized.includes("LAB") || normalized.includes("EXERCISE")) {
        return candidate.includes("QUIZ") || candidate.includes("ASSIGNMENT")
          || candidate.includes("LAB") || candidate.includes("EXERCISE")
      }
      return candidate === normalized
    })
    return match || item
  }))).join(", ")
}

type ImportedTopicRow = {
  topic: TopicDTO
  source?: TopicImportData
  sourceIndex?: number
  weekly?: WeeklyActivityItem
}

const examinationLabel = (value?: string) => {
  const normalized = text(value).replace(/[:;.,-]+$/g, "").trim()
  if (/^mid[ -]?term(?:\s+exam(?:ination)?)?$/i.test(normalized)) return "Midterm"
  if (/^final(?:\s+exam(?:ination)?)?$/i.test(normalized)) return "Final exam"
  return ""
}

/**
 * Repairs legacy previews where an exam-only row consumed the next Content
 * topic and the exam label was incorrectly placed in Assessments. The repair
 * is semantic (exam label + empty CLO), never tied to a particular week.
 */
const normalizeWeeklyActivities = (data: SyllabusImportData): WeeklyActivityItem[] => {
  const activities = Array.isArray(data.weeklyActivities) ? data.weeklyActivities : []
  const contentTopics = Array.isArray(data.topics) ? data.topics : []
  let contentIndex = 0
  let legacyMisalignmentFound = false

  const classified = activities.map((activity) => {
    const explicitExam = examinationLabel(activity.topic)
    const assessmentExam = /\bmid[ -]?term\b/i.test(text(activity.assessments))
      ? "Midterm"
      : /\bfinal\b/i.test(text(activity.assessments)) ? "Final exam" : ""
    const nextContent = contentTopics[contentIndex]
    const consumedContentOnExamRow = Boolean(
      !explicitExam
      && assessmentExam
      && !text(activity.clo)
      && nextContent
      && normalizedText(activity.topic) === normalizedText(nextContent.name),
    )
    const exam = explicitExam || (consumedContentOnExamRow ? assessmentExam : "")
    if (consumedContentOnExamRow) legacyMisalignmentFound = true
    if (!exam) contentIndex += 1
    return { activity, exam }
  })

  if (!legacyMisalignmentFound) {
    return classified.map(({ activity, exam }) => exam ? {
      ...activity,
      topic: exam,
      clo: "",
      assessments: "",
      learningActivities: "",
      resources: "",
    } : activity)
  }

  contentIndex = 0
  return classified.map(({ activity, exam }) => {
    if (exam) return {
      ...activity,
      topic: exam,
      clo: "",
      assessments: "",
      learningActivities: "",
      resources: "",
    }
    const content = contentTopics[contentIndex++]
    return { ...activity, topic: text(content?.name) || activity.topic }
  })
}

const pickWeeklyActivity = (
  topic: TopicImportData,
  activities: WeeklyActivityItem[],
  used: Set<number>,
) => {
  const topicName = normalizedText(topic.name)
  const exactIndex = activities.findIndex((activity, index) =>
    !used.has(index)
    && Number(activity.week) === Number(topic.weekNumber)
    && normalizedText(activity.topic) === topicName,
  )
  const sameWeekIndex = exactIndex >= 0 ? exactIndex : activities.findIndex((activity, index) =>
    !used.has(index) && Number(activity.week) === Number(topic.weekNumber),
  )
  if (sameWeekIndex < 0) return undefined
  used.add(sameWeekIndex)
  return activities[sameWeekIndex]
}

const buildTopicRows = (data: SyllabusImportData): ImportedTopicRow[] => {
  const sourceTopics = Array.isArray(data.topics) ? data.topics : []
  const activities = normalizeWeeklyActivities(data)
  const usedActivities = new Set<number>()

  const rows: ImportedTopicRow[] = sourceTopics.map((source, sourceIndex) => {
    const weekly = pickWeeklyActivity(source, activities, usedActivities)
    return {
      source,
      sourceIndex,
      weekly,
      topic: {
        weekNumber: finiteNumber(source.weekNumber ?? weekly?.week, sourceIndex + 1),
        orderInWeek: finiteNumber(source.orderInWeek, sourceIndex + 1),
        name: text(source.name) || text(weekly?.topic),
        nameVn: source.nameVn,
        teachingHours: finiteNumber(source.teachingHours, 3),
        labHours: finiteNumber(source.labHours, 0),
        selfStudyHours: finiteNumber(source.selfStudyHours, 6),
        topicType: normalizeImportedTopicType(source.topicType) ?? "LECTURE",
        teachingMethod: source.teachingMethod,
        learningActivity: source.learningActivity ?? weekly?.learningActivities,
        assessments: resolveWeeklyAssessmentList(data, weekly?.assessments),
        resources: source.resources ?? weekly?.resources,
        notes: importedContentNotes(source),
        contentWeight: source.contentWeight ?? source.teachingHours,
        contentLevel: source.contentLevel ?? source.teachingLevel,
        teachingLevel: source.teachingLevel ?? source.contentLevel,
      },
    }
  })

  if (sourceTopics.length === 0) activities.forEach((weekly, activityIndex) => {
    if (usedActivities.has(activityIndex) || !text(weekly.topic)) return
    if (/^(midterm|final(?:\s+exam(?:ination)?)?)$/i.test(text(weekly.topic))) return
    const week = finiteNumber(weekly.week, rows.length + 1)
    rows.push({
      weekly,
      topic: {
        weekNumber: week,
        orderInWeek: rows.filter((row) => row.topic.weekNumber === week).length + 1,
        name: text(weekly.topic),
        teachingHours: 3,
        labHours: 0,
        selfStudyHours: 6,
        topicType: "LECTURE",
        learningActivity: weekly.learningActivities,
        assessments: resolveWeeklyAssessmentList(data, weekly.assessments),
        resources: weekly.resources,
      },
    })
  })

  return rows
}

export const buildImportedStandardNotes = (
  data?: SyllabusImportData,
  comparisonTemplate?: SyllabusComparisonTemplateProfile,
) => {
  if (!data) return ""
  const topicRows = buildTopicRows(data)
  const weeklyActivities = normalizeWeeklyActivities(data)
  const ploCodes = Array.from(new Set(
    (data.cloPloMappings ?? []).map((mapping) => canonicalCode(mapping.ploCode, "PLO")).filter(Boolean),
  ))
  const matrixPrefix = ploCodes.some((code) => code.startsWith("SLO")) ? "SLO" : "PLO"
  const effectivePloCodes = Array.from(new Set([
    ...Array.from({ length: 6 }, (_, index) => `${matrixPrefix}${index + 1}`),
    ...ploCodes,
  ]))

  const cloPloMatrix: Record<string, Record<string, string>> = {}
  data.cloPloMappings?.forEach((mapping) => {
    const cloCode = resolveCloCode(data, mapping.cloCode)
    const ploCode = canonicalCode(mapping.ploCode, "PLO")
    if (!cloCode || !ploCode) return
    cloPloMatrix[cloCode] = {
      ...cloPloMatrix[cloCode],
      [ploCode]: normalizeMatrixValue(mapping.value),
    }
  })

  const topicDetails = Object.fromEntries(topicRows.map((row, index) => {
    let mappings = row.sourceIndex === undefined ? [] : (data.topicCloMappings ?? []).filter(
      (mapping) => mapping.topicIndex === row.sourceIndex! + 1,
    )
    if (mappings.length === 0 && row.sourceIndex !== undefined) {
      mappings = (data.topicCloMappings ?? []).filter(
        (mapping) => mapping.topicIndex === row.sourceIndex,
      )
    }
    const mappedClo = mappings.map((mapping) => resolveCloCode(data, mapping.cloCode)).filter(Boolean)
    const contentWeight = row.source?.contentWeight ?? row.source?.teachingHours
    return [String(index), {
      clo: mappedClo.length > 0
        ? Array.from(new Set(mappedClo)).join(", ")
        : canonicalizeCloList(data, row.weekly?.clo),
      assessments: text(row.weekly?.assessments),
      resources: text(row.source?.resources ?? row.weekly?.resources),
      level: text(row.source?.teachingLevel ?? row.source?.contentLevel),
      weight: text(contentWeight),
    }]
  }))

  const assessmentCloMatrix: Record<string, Record<string, string>> = {}
  data.assessments?.forEach((assessment, index) => {
    const orderIndex = finiteNumber(assessment.orderIndex, index + 1)
    let mappings = (data.assessmentCloMappings ?? []).filter((mapping) => mapping.assessmentIndex === orderIndex)
    if (mappings.length === 0 && orderIndex !== index + 1) {
      mappings = (data.assessmentCloMappings ?? []).filter(
        (mapping) => mapping.assessmentIndex === index + 1,
      )
    }
    if (mappings.length === 0) {
      mappings = (data.assessmentCloMappings ?? []).filter(
        (mapping) => mapping.assessmentIndex === index,
      )
    }
    assessmentCloMatrix[String(index)] = Object.fromEntries(mappings.map((mapping) => [
      resolveCloCode(data, mapping.cloCode),
      text(mapping.percentage),
    ]).filter(([code]) => Boolean(code)))
  })

  return JSON.stringify({
    schemaVersion: 1,
    comparisonTemplate,
    internalNotes: "",
    personResponsible: text(data.personResponsible),
    dateRevised: normalizeDateForInput(data.dateRevised),
    creditPoints: text(data.creditPoints),
    lectureCredits: text(data.lectureCredits),
    laboratoryCredits: text(data.laboratoryCredits),
    workloadStudentResponsibility: text(data.workloadStudentResponsibility),
    sourceCourseCode: text(data.sourceCourseCode),
    sourceCourseName: text(data.sourceCourseName),
    ploCodes: effectivePloCodes,
    cloPloMatrix,
    topicDetails,
    plannedActivities: weeklyActivities.map((activity, index) => ({
      week: finiteNumber(activity.week, index + 1),
      topic: text(activity.topic),
      clo: canonicalizeCloList(data, activity.clo),
      assessments: resolveWeeklyAssessmentList(data, activity.assessments),
      learningActivities: text(activity.learningActivities),
      resources: text(activity.resources),
    })),
    assessmentCloMatrix,
    readings: (data.readings ?? []).map((reading) => ({
      title: text(reading.title),
      author: text(reading.author),
      publisher: text(reading.publisher),
      year: text(reading.year),
      usageType: normalizeReadingUsage(reading.type),
    })),
    assessmentPassNote: text(data.assessmentPassNote)
      || "Target percentage of students having scores greater than 50 out of 100.",
    contentNote: text(data.contentNote)
      || "Weight: lecture session (3 hours). Teaching levels: I (Introduce); T (Teach); U (Utilize).",
  })
}

export type ImportedSyllabusInitialDataOptions = {
  courseId: number
  courseProgramId?: number
  nextVersionNumber: number
}

export const buildImportedSyllabusInitialData = (
  preview: ValidSyllabusImportPreviewResponse,
  options: ImportedSyllabusInitialDataOptions,
): CreateSyllabusRequest => {
  const data = preview.data
  const topicRows = buildTopicRows(data)

  return {
    courseId: options.courseId,
    courseProgramId: options.courseProgramId,
    versionNumber: options.nextVersionNumber,
    versionLabel: formatVersionLabel(options.nextVersionNumber),
    academicYear: "",
    semester: data.semester ?? "",
    courseDesignation: data.courseDesignation ?? "",
    courseTypes: normalizeCourseTypes(data.courseTypes || data.relation),
    language: data.language ?? "",
    relation: data.relation ?? "",
    teachingMethods: data.teachingMethods ?? "",
    workloadTotal: data.workloadTotal ?? "",
    workloadContact: data.workloadContact ?? "",
    workloadPrivate: data.workloadPrivate ?? "",
    prerequisites: data.prerequisites ?? "",
    objectives: data.objectives ?? "",
    examForms: data.examForms ?? "",
    examRequirements: data.examRequirements ?? "",
    major: data.major ?? "",
    changeSummary: `Imported from ${preview.fileName}`,
    sourceType: /\.xlsx$/i.test(preview.fileName) || preview.fileType.includes("spreadsheetml")
      ? "IMPORT_XLSX"
      : /\.docx$/i.test(preview.fileName) || preview.fileType.includes("wordprocessingml")
        ? "IMPORT_DOCX"
        : "IMPORT_PDF",
    originalFileName: preview.fileName,
    originalFileType: /\.xlsx$/i.test(preview.fileName)
      ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      : preview.fileType || "application/pdf",
    notes: buildImportedStandardNotes(
      data,
      buildImportedComparisonTemplateProfile(preview),
    ),
    clos: (data.clos ?? []).map((item, index) => ({
      code: text(item.code) || `CLO${index + 1}`,
      description: text(item.description),
      descriptionVn: item.descriptionVn,
      competencyLevel: normalizeImportedCompetencyLevel(item.competencyLevel),
      bloomLevel: normalizeImportedBloomLevel(item.bloomLevel),
      orderIndex: finiteNumber(item.orderIndex, index + 1),
    })),
    topics: topicRows.map((row) => row.topic),
    assessments: (data.assessments ?? []).map((item, index) => ({
      name: text(item.name) || `Assessment ${index + 1}`,
      nameVn: item.nameVn,
      assessmentType: text(item.assessmentType) || "Assessment",
      weightPercent: finiteNumber(item.weightPercent, 0),
      minScore: finiteNumber(item.minScore, 0),
      maxScore: finiteNumber(item.maxScore, 100),
      orderIndex: finiteNumber(item.orderIndex, index + 1),
    })),
  }
}