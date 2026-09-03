import { assessmentApi } from "@/api/assessmentApi"
import { bookApi, ploApi, syllabusBookApi } from "@/api/bookApi"
import { cloApi, cloPloMappingApi } from "@/api/cloApi"
import { courseProgramApi, type CourseProgramItem } from "@/api/courseProgramApi"
import { standardSyllabusApi } from "@/api/standardSyllabusApi"
import { syllabusApi } from "@/api/syllabusApi"
import { topicApi } from "@/api/topicApi"
import type { AssessmentComponent } from "@/types/assessment"
import type { Book, Plo, SyllabusBook, UsageType } from "@/types/book"
import type { ContributionLevel } from "@/types/clo"
import type {
  AssessmentDTO,
  CreateSyllabusRequest,
  SyllabusCreateContextResponse,
  TopicDTO,
} from "@/types/syllabus"
import type { Topic } from "@/types/topic"

export type SyllabusRelationCategory =
  | "topicClo"
  | "assessmentClo"
  | "cloPlo"
  | "readings"

export type SyllabusRelationOperation =
  | "resolve"
  | "fetch"
  | "create"
  | "delete"

export type SyllabusRelationSyncFailure = {
  category: SyllabusRelationCategory
  operation: SyllabusRelationOperation
  key?: string
  message: string
  cause?: unknown
}

export type SyllabusRelationCategoryResult = {
  status: "skipped" | "synchronized" | "failed"
  explicit: boolean
  desired: number
  created: number
  deleted: number
  unchanged: number
}

export type SyllabusRelationSyncResult = {
  syllabusId: number
  success: boolean
  categories: Record<SyllabusRelationCategory, SyllabusRelationCategoryResult>
  failures: SyllabusRelationSyncFailure[]
}

export type SyllabusRelationSyncOptions = {
  /** Throw SyllabusRelationSyncError when any category has a failure. Defaults to true. */
  throwOnFailure?: boolean
}

export class SyllabusRelationSyncError extends Error {
  readonly result: SyllabusRelationSyncResult

  constructor(result: SyllabusRelationSyncResult) {
    const summary = result.failures
      .slice(0, 4)
      .map((failure) => `${failure.category}/${failure.operation}${failure.key ? ` (${failure.key})` : ""}: ${failure.message}`)
      .join("; ")
    const remainder = result.failures.length > 4
      ? `; and ${result.failures.length - 4} more failure(s)`
      : ""
    super(`Syllabus ${result.syllabusId} relation synchronization failed: ${summary}${remainder}`)
    this.name = "SyllabusRelationSyncError"
    this.result = result
  }
}

type TopicDetail = {
  clo: string
  level: string
}

type ReadingInput = {
  title: string
  author: string
  publisher: string
  year: string
  usageType: string
}

type ParsedRelationNotes = {
  explicit: Record<SyllabusRelationCategory, boolean>
  topicDetails: Record<string, TopicDetail>
  assessmentCloMatrix: Record<string, Record<string, string>>
  cloPloMatrix: Record<string, Record<string, string>>
  readings: ReadingInput[]
}

type Contribution = {
  level: ContributionLevel
  weight: number
}

const own = (source: Record<string, unknown>, key: string) =>
  Object.prototype.hasOwnProperty.call(source, key)

const asRecord = (value: unknown): Record<string, unknown> =>
  value !== null && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {}

const asText = (value: unknown) => String(value ?? "").trim()

const parseMatrix = (value: unknown): Record<string, Record<string, string>> =>
  Object.fromEntries(
    Object.entries(asRecord(value)).map(([rowKey, row]) => [
      rowKey,
      Object.fromEntries(
        Object.entries(asRecord(row)).map(([columnKey, cell]) => [columnKey, asText(cell)]),
      ),
    ]),
  )

const parseRelationNotes = (raw: string | null | undefined): ParsedRelationNotes => {
  let source: Record<string, unknown> = {}
  try {
    source = asRecord(JSON.parse(raw || "{}"))
  } catch {
    // Plain-text legacy notes carry no explicit standard-form relation fields.
  }

  const topicDetails = Object.fromEntries(
    Object.entries(asRecord(source.topicDetails)).map(([key, rawDetail]) => {
      const detail = asRecord(rawDetail)
      return [key, { clo: asText(detail.clo), level: asText(detail.level) }]
    }),
  )

  const readings = Array.isArray(source.readings)
    ? source.readings.map((rawReading) => {
        if (typeof rawReading === "string") {
          return { title: rawReading.trim(), author: "", publisher: "", year: "", usageType: "REQUIRED" }
        }
        const reading = asRecord(rawReading)
        return {
          title: asText(reading.title ?? reading.text ?? reading.value),
          author: asText(reading.author),
          publisher: asText(reading.publisher),
          year: asText(reading.year),
          usageType: asText(reading.usageType) || "REQUIRED",
        }
      })
    : []

  return {
    explicit: {
      topicClo: own(source, "topicDetails"),
      assessmentClo: own(source, "assessmentCloMatrix"),
      cloPlo: own(source, "cloPloMatrix"),
      readings: own(source, "readings"),
    },
    topicDetails,
    assessmentCloMatrix: parseMatrix(source.assessmentCloMatrix),
    cloPloMatrix: parseMatrix(source.cloPloMatrix),
    readings,
  }
}

const normalizeCode = (value: unknown) => String(value ?? "")
  .trim()
  .toUpperCase()
  .replace(/\s+/g, "")
  .replace(/^(?:CLO|PLO|SLO)/, "")

const normalizeText = (value: unknown) => String(value ?? "")
  .trim()
  .toLocaleLowerCase()
  .replace(/\s+/g, " ")

const splitCodes = (value: string) => [...new Set(
  value
    .split(/[,;\n]+/)
    .map((code) => code.trim())
    .filter(Boolean),
)]

const contributionFrom = (raw: string): Contribution | null => {
  const value = raw.trim().toUpperCase()
  if (!value || value === "." || value === "-" || value === "0") return null

  if (value === "XXX" || value === "A" || value === "U" || value === "ACHIEVE") {
    return { level: "A", weight: 1 }
  }
  if (value === "XX" || value === "D" || value === "T" || value === "DEVELOP") {
    return { level: "D", weight: 2 / 3 }
  }
  if (value === "X" || value === "I" || value === "INTRODUCE") {
    return { level: "I", weight: 1 / 3 }
  }
  return null
}

const normalizeTeachingLevel = (raw: string): ContributionLevel => {
  const value = raw.trim().toUpperCase()
  const tokens = value.split(/[^A-Z]+/).filter(Boolean)
  if (value === "XXX" || tokens.some((token) => token === "A" || token === "U")) return "A"
  if (value === "XX" || tokens.some((token) => token === "D" || token === "T")) return "D"
  return "I"
}

const normalizeUsage = (raw: string): UsageType => {
  const value = raw.trim().toUpperCase()
  if (value === "RECOMMENDED") return "RECOMMENDED"
  if (value === "SUPPLEMENTARY" || value === "REFERENCE") return "SUPPLEMENTARY"
  return "REQUIRED"
}

const initialCategory = (explicit: boolean): SyllabusRelationCategoryResult => ({
  status: explicit ? "synchronized" : "skipped",
  explicit,
  desired: 0,
  created: 0,
  deleted: 0,
  unchanged: 0,
})

const messageFrom = (error: unknown) => {
  if (error instanceof Error && error.message.trim()) return error.message
  return String(error || "Unknown error")
}

const addFailure = (
  result: SyllabusRelationSyncResult,
  category: SyllabusRelationCategory,
  operation: SyllabusRelationOperation,
  error: unknown,
  key?: string,
) => {
  result.categories[category].status = "failed"
  result.failures.push({
    category,
    operation,
    key,
    message: messageFrom(error),
    cause: error,
  })
}

const perform = async (
  result: SyllabusRelationSyncResult,
  category: SyllabusRelationCategory,
  operation: "create" | "delete",
  key: string,
  action: () => Promise<unknown>,
) => {
  try {
    await action()
    result.categories[category][operation === "create" ? "created" : "deleted"] += 1
    return true
  } catch (error) {
    addFailure(result, category, operation, error, key)
    return false
  }
}

const sortedTopics = (topics: Topic[]) => [...topics].sort((left, right) =>
  left.weekNumber - right.weekNumber
  || Number(left.orderInWeek ?? 0) - Number(right.orderInWeek ?? 0)
  || left.id - right.id,
)

const sortedAssessments = (assessments: AssessmentComponent[]) => [...assessments].sort((left, right) =>
  Number(left.orderIndex ?? 0) - Number(right.orderIndex ?? 0)
  || left.id - right.id,
)

const matchTopics = (serverRows: Topic[], submittedRows?: TopicDTO[]) => {
  const orderedServerRows = sortedTopics(serverRows)
  if (!submittedRows) return orderedServerRows.map((row, index) => ({ row, index }))

  const unused = new Set(orderedServerRows.map((row) => row.id))
  return submittedRows.flatMap((source, index) => {
    const exactId = source.id === undefined
      ? undefined
      : orderedServerRows.find((row) => unused.has(row.id) && row.id === source.id)
    const exact = exactId ?? orderedServerRows.find((row) =>
      unused.has(row.id)
      && row.weekNumber === source.weekNumber
      && Number(row.orderInWeek ?? 0) === Number(source.orderInWeek ?? 0)
      && normalizeText(row.name) === normalizeText(source.name),
    )
    const fallback = exact ?? orderedServerRows.find((row) =>
      unused.has(row.id)
      && row.weekNumber === source.weekNumber
      && normalizeText(row.name) === normalizeText(source.name),
    )
    if (!fallback) return []
    unused.delete(fallback.id)
    return [{ row: fallback, index }]
  })
}

const matchAssessments = (serverRows: AssessmentComponent[], submittedRows?: AssessmentDTO[]) => {
  const orderedServerRows = sortedAssessments(serverRows)
  if (!submittedRows) return orderedServerRows.map((row, index) => ({ row, index }))

  const unused = new Set(orderedServerRows.map((row) => row.id))
  return submittedRows.flatMap((source, index) => {
    const exactId = source.id === undefined
      ? undefined
      : orderedServerRows.find((row) => unused.has(row.id) && row.id === source.id)
    const exact = exactId ?? orderedServerRows.find((row) =>
      unused.has(row.id)
      && Number(row.orderIndex ?? 0) === Number(source.orderIndex ?? 0)
      && normalizeText(row.name) === normalizeText(source.name),
    )
    const fallback = exact ?? orderedServerRows.find((row) =>
      unused.has(row.id) && normalizeText(row.name) === normalizeText(source.name),
    )
    if (!fallback) return []
    unused.delete(fallback.id)
    return [{ row: fallback, index }]
  })
}

const reconcileTopicClos = async (
  syllabusId: number,
  payload: CreateSyllabusRequest,
  notes: ParsedRelationNotes,
  result: SyllabusRelationSyncResult,
) => {
  const category = "topicClo" as const
  if (!notes.explicit[category]) return

  try {
    const [clos, topics] = await Promise.all([
      cloApi.getBySyllabus(syllabusId),
      topicApi.getBySyllabus(syllabusId),
    ])
    const cloByCode = new Map(clos.map((clo) => [normalizeCode(clo.code), clo]))
    const matchedTopics = matchTopics(topics, payload.topics)

    if (payload.topics && matchedTopics.length !== payload.topics.length) {
      addFailure(
        result,
        category,
        "resolve",
        new Error(`Matched ${matchedTopics.length} of ${payload.topics.length} submitted topic row(s)`),
      )
    }

    for (const { row: topic, index } of matchedTopics) {
      const detail = notes.topicDetails[String(index)] ?? { clo: "", level: "" }
      const desired = new Map<number, ContributionLevel>()
      const unresolved: string[] = []
      for (const rawCode of splitCodes(detail.clo)) {
        const clo = cloByCode.get(normalizeCode(rawCode))
        if (!clo) unresolved.push(rawCode)
        else desired.set(clo.id, normalizeTeachingLevel(detail.level))
      }
      result.categories[category].desired += desired.size
      if (unresolved.length > 0) {
        addFailure(result, category, "resolve", new Error(`Unknown CLO code(s): ${unresolved.join(", ")}`), `topic:${topic.id}`)
        continue
      }

      let existing
      try {
        existing = await standardSyllabusApi.getTopicClosByTopic(topic.id)
      } catch (error) {
        addFailure(result, category, "fetch", error, `topic:${topic.id}`)
        continue
      }
      const existingByClo = new Map(existing.map((link) => [link.cloId, link]))

      for (const link of existing) {
        const desiredLevel = desired.get(link.cloId)
        if (!desiredLevel) {
          await perform(result, category, "delete", `topic:${topic.id}/clo:${link.cloId}`, () =>
            standardSyllabusApi.deleteTopicClo(topic.id, link.cloId))
          continue
        }
        if (link.teachingLevel === desiredLevel) {
          result.categories[category].unchanged += 1
          continue
        }
        const deleted = await perform(result, category, "delete", `topic:${topic.id}/clo:${link.cloId}`, () =>
          standardSyllabusApi.deleteTopicClo(topic.id, link.cloId))
        if (deleted) {
          await perform(result, category, "create", `topic:${topic.id}/clo:${link.cloId}`, () =>
            standardSyllabusApi.createTopicClo({ topicId: topic.id, cloId: link.cloId, teachingLevel: desiredLevel }))
        }
      }

      for (const [cloId, teachingLevel] of desired) {
        if (existingByClo.has(cloId)) continue
        await perform(result, category, "create", `topic:${topic.id}/clo:${cloId}`, () =>
          standardSyllabusApi.createTopicClo({ topicId: topic.id, cloId, teachingLevel }))
      }
    }
  } catch (error) {
    addFailure(result, category, "fetch", error)
  }
}

const reconcileAssessmentClos = async (
  syllabusId: number,
  payload: CreateSyllabusRequest,
  notes: ParsedRelationNotes,
  result: SyllabusRelationSyncResult,
) => {
  const category = "assessmentClo" as const
  if (!notes.explicit[category]) return

  try {
    const [clos, assessments] = await Promise.all([
      cloApi.getBySyllabus(syllabusId),
      assessmentApi.getBySyllabus(syllabusId),
    ])
    const cloByCode = new Map(clos.map((clo) => [normalizeCode(clo.code), clo]))
    const matchedAssessments = matchAssessments(assessments, payload.assessments)

    if (payload.assessments && matchedAssessments.length !== payload.assessments.length) {
      addFailure(
        result,
        category,
        "resolve",
        new Error(`Matched ${matchedAssessments.length} of ${payload.assessments.length} submitted assessment row(s)`),
      )
    }

    for (const { row: assessment, index } of matchedAssessments) {
      const matrix = notes.assessmentCloMatrix[String(index)] ?? {}
      const desired = new Map<number, number>()
      const invalid: string[] = []
      for (const [rawCode, rawPercent] of Object.entries(matrix)) {
        if (!rawPercent.trim() || rawPercent.trim() === ".") continue
        const clo = cloByCode.get(normalizeCode(rawCode))
        const contributionPercent = Number(rawPercent)
        if (!clo || !Number.isFinite(contributionPercent) || contributionPercent <= 0 || contributionPercent > 100) {
          invalid.push(`${rawCode}=${rawPercent}`)
        } else {
          desired.set(clo.id, contributionPercent)
        }
      }
      result.categories[category].desired += desired.size
      if (invalid.length > 0) {
        addFailure(result, category, "resolve", new Error(`Invalid assessment-CLO cell(s): ${invalid.join(", ")}`), `assessment:${assessment.id}`)
        continue
      }

      let existing
      try {
        existing = await standardSyllabusApi.getAssessmentClosByAssessment(assessment.id)
      } catch (error) {
        addFailure(result, category, "fetch", error, `assessment:${assessment.id}`)
        continue
      }
      const existingByClo = new Map(existing.map((link) => [link.cloId, link]))

      for (const link of existing) {
        const desiredPercent = desired.get(link.cloId)
        if (desiredPercent === undefined) {
          await perform(result, category, "delete", `assessment:${assessment.id}/clo:${link.cloId}`, () =>
            standardSyllabusApi.deleteAssessmentClo(assessment.id, link.cloId))
          continue
        }
        if (Math.abs(Number(link.contributionPercent) - desiredPercent) < 0.001) {
          result.categories[category].unchanged += 1
          continue
        }
        const deleted = await perform(result, category, "delete", `assessment:${assessment.id}/clo:${link.cloId}`, () =>
          standardSyllabusApi.deleteAssessmentClo(assessment.id, link.cloId))
        if (deleted) {
          await perform(result, category, "create", `assessment:${assessment.id}/clo:${link.cloId}`, () =>
            standardSyllabusApi.createAssessmentClo({
              assessmentComponentId: assessment.id,
              cloId: link.cloId,
              contributionPercent: desiredPercent,
            }))
        }
      }

      for (const [cloId, contributionPercent] of desired) {
        if (existingByClo.has(cloId)) continue
        await perform(result, category, "create", `assessment:${assessment.id}/clo:${cloId}`, () =>
          standardSyllabusApi.createAssessmentClo({
            assessmentComponentId: assessment.id,
            cloId,
            contributionPercent,
          }))
      }
    }
  } catch (error) {
    addFailure(result, category, "fetch", error)
  }
}

const uniqueProgramId = (coursePrograms: CourseProgramItem[]) => {
  const ids = [...new Set(coursePrograms.map((item) => item.programId).filter((id) => id > 0))]
  return ids.length === 1 ? ids[0] : undefined
}

const resolveProgram = async (
  syllabusId: number,
  payload: CreateSyllabusRequest,
) : Promise<{ programId: number; context?: SyllabusCreateContextResponse }> => {
  let context: SyllabusCreateContextResponse | undefined
  const loadContext = async () => {
    context ??= await syllabusApi.getCreateContext(payload.courseId)
    return context
  }

  if (payload.courseProgramId) {
    const courseProgram = await courseProgramApi.getById(payload.courseProgramId)
    if (courseProgram.courseId !== payload.courseId) {
      throw new Error(`CourseProgram ${payload.courseProgramId} does not belong to course ${payload.courseId}`)
    }
    try {
      await loadContext()
    } catch {
      // Direct course-program resolution is sufficient; context is only a PLO fallback.
    }
    return { programId: courseProgram.programId, context }
  }

  let coursePrograms: CourseProgramItem[] = []
  try {
    coursePrograms = (await courseProgramApi.getAll())
      .filter((item) => item.courseId === payload.courseId)
  } catch {
    // Context-based resolution below remains valid when catalog access is unavailable.
  }
  const linked = coursePrograms.filter((item) => item.syllabusId === syllabusId)
  const linkedProgramId = uniqueProgramId(linked)
  if (linkedProgramId) {
    try {
      await loadContext()
    } catch {
      // The linked CourseProgram is authoritative even if create-context is unavailable.
    }
    return { programId: linkedProgramId, context }
  }

  if (payload.cohortId) {
    const cohortProgramId = uniqueProgramId(
      coursePrograms.filter((item) => item.cohortId === payload.cohortId),
    )
    if (cohortProgramId) return { programId: cohortProgramId, context }
  }

  // Legacy Drafts may predate the direct CourseProgram link. Reuse a unique
  // program already linked by another version of the same course.
  const historicalProgramId = uniqueProgramId(
    coursePrograms.filter((item) => item.syllabusId != null),
  )
  if (historicalProgramId) return { programId: historicalProgramId, context }

  if (payload.programId) {
    try {
      await loadContext()
    } catch {
      // Explicit program context still permits direct PLO lookup.
    }
    const matchingContext = !context
      || context.coursePrograms?.some((item) => item.programId === payload.programId)
    if (matchingContext) return { programId: payload.programId, context }
  }

  await loadContext()
  const contextProgramIds = [...new Set(
    (context?.coursePrograms ?? [])
      .map((item) => item.programId)
      .filter((id): id is number => Boolean(id && id > 0)),
  )]
  if (contextProgramIds.length === 1) return { programId: contextProgramIds[0], context }

  throw new Error(
    linked.length > 1
      ? `Syllabus ${syllabusId} is linked to multiple programs; provide courseProgramId`
      : `Cannot determine a unique program for course ${payload.courseId}; provide courseProgramId`,
  )
}

const reconcileCloPlos = async (
  syllabusId: number,
  payload: CreateSyllabusRequest,
  notes: ParsedRelationNotes,
  result: SyllabusRelationSyncResult,
) => {
  const category = "cloPlo" as const
  if (!notes.explicit[category]) return

  try {
    const clos = await cloApi.getBySyllabus(syllabusId)
    const hasDesiredMapping = Object.values(notes.cloPloMatrix).some((matrix) =>
      Object.values(matrix).some((rawContribution) => contributionFrom(rawContribution) !== null),
    )

    // Clearing the matrix is unambiguous and must also work for legacy Drafts
    // that were never linked to a CourseProgram.
    if (!hasDesiredMapping) {
      for (const clo of clos) {
        let existing
        try {
          existing = await cloPloMappingApi.getByClo(clo.id)
        } catch (error) {
          addFailure(result, category, "fetch", error, `clo:${clo.id}`)
          continue
        }
        for (const link of existing) {
          await perform(result, category, "delete", `mapping:${link.id}`, () =>
            cloPloMappingApi.delete(link.id))
        }
      }
      return
    }

    const resolved = await resolveProgram(syllabusId, payload)
    let plos: Plo[] = []
    let directPloError: unknown
    try {
      plos = await ploApi.getByProgram(resolved.programId)
    } catch (error) {
      directPloError = error
      plos = (resolved.context?.plos ?? [])
        .filter((plo) => plo.programId === resolved.programId)
        .map((plo) => ({
          id: plo.id,
          programId: resolved.programId,
          programCode: plo.programCode ?? undefined,
          code: plo.code,
          description: plo.description ?? undefined,
        }))
    }
    if (plos.length === 0) {
      throw directPloError ?? new Error(`Program ${resolved.programId} has no available PLO records`)
    }

    const cloByCode = new Map(clos.map((clo) => [normalizeCode(clo.code), clo]))
    const ploByCode = new Map(plos.map((plo) => [normalizeCode(plo.code), plo]))
    const selectedPloIds = new Set(plos.map((plo) => plo.id))
    const desiredByClo = new Map<number, Map<number, Contribution>>()
    const unresolved: string[] = []

    for (const [rawCloCode, matrix] of Object.entries(notes.cloPloMatrix)) {
      const clo = cloByCode.get(normalizeCode(rawCloCode))
      const populated = Object.entries(matrix).filter(([, raw]) => {
        const value = raw.trim()
        return Boolean(value && value !== "." && value !== "-" && value !== "0")
      })
      if (!clo) {
        if (populated.length > 0) unresolved.push(`CLO ${rawCloCode}`)
        continue
      }
      const desired = new Map<number, Contribution>()
      for (const [rawPloCode, rawContribution] of populated) {
        const plo = ploByCode.get(normalizeCode(rawPloCode))
        const contribution = contributionFrom(rawContribution)
        if (!plo || !contribution) unresolved.push(`${rawCloCode}/${rawPloCode}=${rawContribution}`)
        else desired.set(plo.id, contribution)
      }
      desiredByClo.set(clo.id, desired)
      result.categories[category].desired += desired.size
    }

    if (unresolved.length > 0) {
      addFailure(result, category, "resolve", new Error(`Unknown CLO/PLO matrix coordinate(s): ${unresolved.join(", ")}`))
      return
    }

    // An explicitly empty matrix means no mappings for the selected program.
    for (const clo of clos) {
      const desired = desiredByClo.get(clo.id) ?? new Map<number, Contribution>()
      let existing
      try {
        existing = await cloPloMappingApi.getByClo(clo.id)
      } catch (error) {
        addFailure(result, category, "fetch", error, `clo:${clo.id}`)
        continue
      }
      const scopedExisting = existing.filter((link) => selectedPloIds.has(link.ploId))
      const existingByPlo = new Map(scopedExisting.map((link) => [link.ploId, link]))

      for (const link of scopedExisting) {
        const wanted = desired.get(link.ploId)
        if (!wanted) {
          await perform(result, category, "delete", `mapping:${link.id}`, () => cloPloMappingApi.delete(link.id))
          continue
        }
        const sameWeight = Math.abs(Number(link.contributionWeight ?? 0) - wanted.weight) < 0.01
        if (link.level === wanted.level && sameWeight) {
          result.categories[category].unchanged += 1
          continue
        }
        const deleted = await perform(result, category, "delete", `mapping:${link.id}`, () => cloPloMappingApi.delete(link.id))
        if (deleted) {
          await perform(result, category, "create", `clo:${clo.id}/plo:${link.ploId}`, () =>
            cloPloMappingApi.create({
              cloId: clo.id,
              ploId: link.ploId,
              level: wanted.level,
              contributionWeight: wanted.weight,
            }))
        }
      }

      for (const [ploId, wanted] of desired) {
        if (existingByPlo.has(ploId)) continue
        await perform(result, category, "create", `clo:${clo.id}/plo:${ploId}`, () =>
          cloPloMappingApi.create({
            cloId: clo.id,
            ploId,
            level: wanted.level,
            contributionWeight: wanted.weight,
          }))
      }
    }
  } catch (error) {
    addFailure(result, category, "resolve", error)
  }
}

const readingKey = (reading: Pick<ReadingInput, "title" | "author" | "year">) => [
  normalizeText(reading.title),
  normalizeText(reading.author),
  String(reading.year ?? "").trim(),
].join("|")

const bookKey = (book: Pick<Book, "title" | "author" | "year">) => readingKey({
  title: book.title,
  author: book.author ?? "",
  year: String(book.year ?? ""),
})

const linkKey = (link: SyllabusBook) => readingKey({
  title: link.bookTitle,
  author: link.author ?? "",
  year: String(link.year ?? ""),
})

const reconcileReadings = async (
  syllabusId: number,
  notes: ParsedRelationNotes,
  result: SyllabusRelationSyncResult,
) => {
  const category = "readings" as const
  if (!notes.explicit[category]) return

  try {
    const [existingLinks, library] = await Promise.all([
      syllabusBookApi.getBySyllabus(syllabusId),
      bookApi.getAll(),
    ])
    const desiredReadings = notes.readings.filter((reading) => reading.title.trim())
    const duplicateKeys = new Set<string>()
    const desiredByKey = new Map<string, { reading: ReadingInput; orderIndex: number }>()
    desiredReadings.forEach((reading, index) => {
      const key = readingKey(reading)
      if (desiredByKey.has(key)) duplicateKeys.add(key)
      else desiredByKey.set(key, { reading, orderIndex: index + 1 })
    })
    if (duplicateKeys.size > 0) {
      addFailure(
        result,
        category,
        "resolve",
        new Error("The same book cannot be linked to one syllabus more than once"),
        [...duplicateKeys].join(", "),
      )
    }
    result.categories[category].desired = desiredByKey.size

    const linkedByKey = new Map(existingLinks.map((link) => [linkKey(link), link]))
    const libraryByKey = new Map(library.map((book) => [bookKey(book), book]))
    const desiredLinks = new Map<number, { usageType: UsageType; orderIndex: number }>()

    for (const [key, { reading, orderIndex }] of desiredByKey) {
      const linkedBook = linkedByKey.get(key)
      let book = linkedBook
        ? library.find((candidate) => candidate.id === linkedBook.bookId)
        : libraryByKey.get(key)
      if (!book && linkedBook) {
        book = {
          id: linkedBook.bookId,
          title: linkedBook.bookTitle,
          author: linkedBook.author,
          publisher: linkedBook.publisher,
          year: linkedBook.year,
        }
      }
      if (!book) {
        try {
          book = await bookApi.create({
            title: reading.title.trim(),
            author: reading.author.trim() || undefined,
            publisher: reading.publisher.trim() || undefined,
            year: reading.year && Number.isFinite(Number(reading.year))
              ? Number(reading.year)
              : undefined,
            bookType: "REFERENCE",
          })
          library.push(book)
          libraryByKey.set(key, book)
        } catch (error) {
          addFailure(result, category, "create", error, `book:${reading.title}`)
          continue
        }
      }
      desiredLinks.set(book.id, { usageType: normalizeUsage(reading.usageType), orderIndex })
    }

    for (const link of existingLinks) {
      const wanted = desiredLinks.get(link.bookId)
      if (!wanted) {
        await perform(result, category, "delete", `book:${link.bookId}`, () =>
          syllabusBookApi.delete(syllabusId, link.bookId))
        continue
      }
      if (link.usageType === wanted.usageType && Number(link.orderIndex ?? 0) === wanted.orderIndex) {
        result.categories[category].unchanged += 1
        continue
      }
      const deleted = await perform(result, category, "delete", `book:${link.bookId}`, () =>
        syllabusBookApi.delete(syllabusId, link.bookId))
      if (deleted) {
        await perform(result, category, "create", `book:${link.bookId}`, () =>
          syllabusBookApi.create({ syllabusId, bookId: link.bookId, ...wanted }))
      }
    }

    const existingBookIds = new Set(existingLinks.map((link) => link.bookId))
    for (const [bookId, wanted] of desiredLinks) {
      if (existingBookIds.has(bookId)) continue
      await perform(result, category, "create", `book:${bookId}`, () =>
        syllabusBookApi.create({ syllabusId, bookId, ...wanted }))
    }
  } catch (error) {
    addFailure(result, category, "fetch", error)
  }
}

/**
 * Reconciles the relation-backed parts of the editable standard syllabus form
 * after the base Syllabus and its CLO/topic/assessment children have been saved.
 *
 * Relation categories absent from the submitted standard notes JSON are skipped
 * so partial/legacy updates cannot erase existing relation data.
 */
export const syncStandardSyllabusRelations = async (
  syllabusId: number,
  payload: CreateSyllabusRequest,
  options: SyllabusRelationSyncOptions = {},
): Promise<SyllabusRelationSyncResult> => {
  const notes = parseRelationNotes(payload.notes)
  const result: SyllabusRelationSyncResult = {
    syllabusId,
    success: true,
    categories: {
      topicClo: initialCategory(notes.explicit.topicClo),
      assessmentClo: initialCategory(notes.explicit.assessmentClo),
      cloPlo: initialCategory(notes.explicit.cloPlo),
      readings: initialCategory(notes.explicit.readings),
    },
    failures: [],
  }

  // Categories are deliberately isolated: a failed PLO/context lookup must not
  // prevent topic, assessment, or reading reconciliation.
  await reconcileTopicClos(syllabusId, payload, notes, result)
  await reconcileAssessmentClos(syllabusId, payload, notes, result)
  await reconcileCloPlos(syllabusId, payload, notes, result)
  await reconcileReadings(syllabusId, notes, result)

  result.success = result.failures.length === 0
  if (!result.success && options.throwOnFailure !== false) {
    throw new SyllabusRelationSyncError(result)
  }
  return result
}
