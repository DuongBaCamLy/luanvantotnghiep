import type { Syllabus } from "@/types/syllabus"
import type {
  SyllabusImportData,
  ValidSyllabusImportPreviewResponse,
} from "@/types/syllabusImport"
import type { SyllabusDiffResponse } from "@/types/syllabusDiff"

export type SyllabusComparisonSectionKey =
  | "general"
  | "workloadCredit"
  | "requirements"
  | "clo"
  | "content"
  | "topicClo"
  | "cloPlo"
  | "plannedActivities"
  | "assessment"
  | "assessmentClo"
  | "examination"
  | "readings"
  | "revision"

export type SyllabusComparisonFieldDefinition = {
  key: string
  label: string
}

export type SyllabusComparisonSectionDefinition = {
  key: SyllabusComparisonSectionKey
  label: string
  fields: SyllabusComparisonFieldDefinition[]
}

export type SyllabusComparisonTemplateProfile = {
  schemaVersion: 1
  source: "UPLOADED_TEMPLATE" | "STANDARD_FORM" | "INFERRED"
  sourceFileName?: string
  sourceFileType?: string
  sections: SyllabusComparisonSectionDefinition[]
}

type UnknownRecord = Record<string, unknown>

const asRecord = (value: unknown): UnknownRecord =>
  value && typeof value === "object" && !Array.isArray(value)
    ? value as UnknownRecord
    : {}

const isDefined = (value: unknown) =>
  value !== null && value !== undefined

const hasAnyDefined = (
  items: unknown[] | null | undefined,
  key: string,
) =>
  Array.isArray(items)
  && items.some((item) => {
    const source = asRecord(item)
    return isDefined(source[key])
  })

const hasAnyItems = (
  value: unknown[] | null | undefined,
) =>
  Array.isArray(value) && value.length > 0

const uniqueFields = (
  fields: SyllabusComparisonFieldDefinition[],
) => {
  const seen = new Set<string>()

  return fields.filter((field) => {
    if (!field.key || seen.has(field.key)) {
      return false
    }

    seen.add(field.key)
    return true
  })
}

const field = (
  key: string,
  label: string,
): SyllabusComparisonFieldDefinition => ({
  key,
  label,
})

const section = (
  key: SyllabusComparisonSectionKey,
  label: string,
  fields: SyllabusComparisonFieldDefinition[],
): SyllabusComparisonSectionDefinition => ({
  key,
  label,
  fields: uniqueFields(fields),
})

const generalFieldsFromImport = (
  data: SyllabusImportData,
) => [
  isDefined(data.sourceCourseCode)
    ? field("courseCode", "Course Code")
    : null,
  isDefined(data.sourceCourseName)
    ? field("courseName", "Course Name")
    : null,
  isDefined(data.courseDesignation)
    ? field("courseDesignation", "Course Designation")
    : null,
  isDefined(data.courseTypes)
    ? field("courseTypes", "Course Type")
    : null,
  isDefined(data.semester)
    ? field("semester", "Semester")
    : null,
  isDefined(data.personResponsible)
    ? field("personResponsible", "Person Responsible for the Course")
    : null,
  isDefined(data.language)
    ? field("language", "Language of Instruction")
    : null,
  isDefined(data.relation)
    ? field("relation", "Relation to Curriculum")
    : null,
  isDefined(data.teachingMethods)
    ? field("teachingMethods", "Teaching Methods")
    : null,
  isDefined(data.major)
    ? field("major", "Major / Academic Area")
    : null,
].filter(Boolean) as SyllabusComparisonFieldDefinition[]

const workloadFieldsFromImport = (
  data: SyllabusImportData,
) => [
  isDefined(data.workloadTotal)
    ? field("workloadTotal", "Total Workload")
    : null,
  isDefined(data.workloadContact)
    ? field("workloadContact", "Contact Hours")
    : null,
  isDefined(data.workloadPrivate)
    ? field("workloadPrivate", "Self-study / Private Study Hours")
    : null,
  isDefined(data.workloadStudentResponsibility)
    ? field("workloadStudentResponsibility", "Student Responsibility")
    : null,
  isDefined(data.creditPoints)
    ? field("creditPoints", "Credit Points — Total")
    : null,
  isDefined(data.lectureCredits)
    ? field("lectureCredits", "Credits — Lecture")
    : null,
  isDefined(data.laboratoryCredits)
    ? field("laboratoryCredits", "Credits — Laboratory")
    : null,
].filter(Boolean) as SyllabusComparisonFieldDefinition[]

const requirementFieldsFromImport = (
  data: SyllabusImportData,
) => [
  isDefined(data.prerequisites)
    ? field("prerequisites", "Required / Recommended Prerequisites")
    : null,
  isDefined(data.objectives)
    ? field("objectives", "Course Objectives")
    : null,
].filter(Boolean) as SyllabusComparisonFieldDefinition[]

const cloFieldsFromImport = (
  data: SyllabusImportData,
) => {
  if (!hasAnyItems(data.clos)) {
    return []
  }

  return [
    field("code", "Code"),
    hasAnyDefined(data.clos, "description")
      ? field("description", "Description")
      : null,
    hasAnyDefined(data.clos, "descriptionVn")
      ? field("descriptionVn", "Vietnamese Description")
      : null,
    hasAnyDefined(data.clos, "competencyLevel")
      ? field("competencyLevel", "Competency Level")
      : null,
    hasAnyDefined(data.clos, "bloomLevel")
      ? field("bloomLevel", "Bloom Level")
      : null,
    hasAnyDefined(data.clos, "orderIndex")
      ? field("orderIndex", "Display Order")
      : null,
  ].filter(Boolean) as SyllabusComparisonFieldDefinition[]
}

const contentFieldsFromImport = (
  data: SyllabusImportData,
) => {
  const fields: Array<SyllabusComparisonFieldDefinition | null> = []

  if (isDefined(data.contentNote)) {
    fields.push(field("contentNote", "Content Note"))
  }

  if (hasAnyItems(data.topics)) {
    fields.push(field("name", "Topic"))

    if (hasAnyDefined(data.topics, "nameVn")) {
      fields.push(field("nameVn", "Vietnamese Topic"))
    }

    if (hasAnyDefined(data.topics, "weekNumber")) {
      fields.push(field("weekNumber", "Week"))
    }

    if (hasAnyDefined(data.topics, "orderInWeek")) {
      fields.push(field("orderInWeek", "Order in Week"))
    }

    if (hasAnyDefined(data.topics, "teachingHours")) {
      fields.push(field("teachingHours", "Teaching Hours"))
    }

    if (hasAnyDefined(data.topics, "labHours")) {
      fields.push(field("labHours", "Laboratory Hours"))
    }

    if (hasAnyDefined(data.topics, "selfStudyHours")) {
      fields.push(field("selfStudyHours", "Self-study Hours"))
    }

    if (hasAnyDefined(data.topics, "topicType")) {
      fields.push(field("topicType", "Topic Type"))
    }

    if (hasAnyDefined(data.topics, "teachingMethod")) {
      fields.push(field("teachingMethod", "Teaching Method"))
    }

    if (hasAnyDefined(data.topics, "learningActivity")) {
      fields.push(field("learningActivity", "Learning Activity"))
    }

    if (hasAnyDefined(data.topics, "resources")) {
      fields.push(field("resources", "Resources"))
    }

    if (
      hasAnyDefined(data.topics, "contentWeight")
      || hasAnyDefined(data.topics, "teachingHours")
    ) {
      fields.push(field("contentWeight", "Content Weight"))
    }

    if (
      hasAnyDefined(data.topics, "contentLevel")
      || hasAnyDefined(data.topics, "teachingLevel")
    ) {
      fields.push(field("contentLevel", "Content Level (I/T/U)"))
    }
  }

  return uniqueFields(
    fields.filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )
}

const plannedActivityFieldsFromImport = (
  data: SyllabusImportData,
) => {
  if (!hasAnyItems(data.weeklyActivities)) {
    return []
  }

  return [
    field("week", "Week"),
    hasAnyDefined(data.weeklyActivities, "topic")
      ? field("topic", "Topic")
      : null,
    hasAnyDefined(data.weeklyActivities, "clo")
      ? field("clo", "CLOs")
      : null,
    hasAnyDefined(data.weeklyActivities, "assessments")
      ? field("assessments", "Assessments")
      : null,
    hasAnyDefined(data.weeklyActivities, "learningActivities")
      ? field("learningActivities", "Learning Activities")
      : null,
    hasAnyDefined(data.weeklyActivities, "resources")
      ? field("resources", "Resources")
      : null,
  ].filter(Boolean) as SyllabusComparisonFieldDefinition[]
}

const assessmentFieldsFromImport = (
  data: SyllabusImportData,
) => {
  const fields: Array<SyllabusComparisonFieldDefinition | null> = []

  if (isDefined(data.assessmentPassNote)) {
    fields.push(field("assessmentPassNote", "Assessment Pass Note"))
  }

  if (hasAnyItems(data.assessments)) {
    fields.push(field("name", "Assessment"))

    if (hasAnyDefined(data.assessments, "nameVn")) {
      fields.push(field("nameVn", "Vietnamese Name"))
    }

    if (hasAnyDefined(data.assessments, "assessmentType")) {
      fields.push(field("assessmentType", "Assessment Type"))
    }

    if (hasAnyDefined(data.assessments, "weightPercent")) {
      fields.push(field("weightPercent", "Weight (%)"))
    }

    if (hasAnyDefined(data.assessments, "minScore")) {
      fields.push(field("minScore", "Minimum Score"))
    }

    if (hasAnyDefined(data.assessments, "maxScore")) {
      fields.push(field("maxScore", "Maximum Score"))
    }

    if (hasAnyDefined(data.assessments, "orderIndex")) {
      fields.push(field("orderIndex", "Display Order"))
    }
  }

  return uniqueFields(
    fields.filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )
}

const examinationFieldsFromImport = (
  data: SyllabusImportData,
) => [
  isDefined(data.examForms)
    ? field("examForms", "Examination Forms")
    : null,
  isDefined(data.examRequirements)
    ? field("examRequirements", "Study / Examination Requirements")
    : null,
].filter(Boolean) as SyllabusComparisonFieldDefinition[]

const readingFieldsFromImport = (
  data: SyllabusImportData,
) => {
  if (!hasAnyItems(data.readings)) {
    return []
  }

  return [
    field("title", "Title"),
    hasAnyDefined(data.readings, "author")
      ? field("author", "Author")
      : null,
    hasAnyDefined(data.readings, "publisher")
      ? field("publisher", "Publisher")
      : null,
    hasAnyDefined(data.readings, "year")
      ? field("year", "Publication Year")
      : null,
    hasAnyDefined(data.readings, "type")
      ? field("usageType", "Usage Type")
      : null,
  ].filter(Boolean) as SyllabusComparisonFieldDefinition[]
}

const parserProvidedTemplateSections = (
  data: SyllabusImportData,
): SyllabusComparisonSectionDefinition[] => {
  const source =
    data as SyllabusImportData & {
      templateSections?: unknown
    }

  if (!Array.isArray(source.templateSections)) {
    return []
  }

  return source.templateSections
    .map((rawSection) => {
      const sectionSource =
        asRecord(rawSection)

      const key =
        String(sectionSource.key ?? "")
          .trim() as SyllabusComparisonSectionKey

      if (
        !key
        || ![
          "general",
          "workloadCredit",
          "requirements",
          "clo",
          "content",
          "topicClo",
          "cloPlo",
          "plannedActivities",
          "assessment",
          "assessmentClo",
          "examination",
          "readings",
          "revision",
        ].includes(key)
      ) {
        return null
      }

      const rawFields =
        Array.isArray(sectionSource.fields)
          ? sectionSource.fields
          : []

      const fields =
        rawFields
          .map((rawField) => {
            if (typeof rawField === "string") {
              const keyValue = rawField.trim()

              return keyValue
                ? field(keyValue, keyValue)
                : null
            }

            const fieldSource =
              asRecord(rawField)

            const keyValue =
              String(
                fieldSource.key ?? "",
              ).trim()

            if (!keyValue) {
              return null
            }

            return field(
              keyValue,
              String(
                fieldSource.label
                ?? keyValue,
              ).trim()
              || keyValue,
            )
          })
          .filter(
            (
              item,
            ): item is SyllabusComparisonFieldDefinition =>
              item !== null,
          )

      if (fields.length === 0) {
        return null
      }

      return section(
        key,
        String(
          sectionSource.label
          ?? key,
        ).trim()
        || key,
        fields,
      )
    })
    .filter(
      (
        item,
      ): item is SyllabusComparisonSectionDefinition =>
        item !== null,
    )
}

/**
 * Builds the comparison shape from the actual parsed upload.
 *
 * The upload is authoritative: a section/field is included only when the
 * parser recognized it in the uploaded template. The comparison page must
 * not inject unrelated fixed sections for imported syllabuses.
 *
 * When the backend parser provides templateSections, their source order and
 * recognized fields are used directly. Older parsers fall back to a safe
 * shape inferred from the extracted DTO.
 */
export const buildImportedComparisonTemplateProfile = (
  preview: ValidSyllabusImportPreviewResponse,
): SyllabusComparisonTemplateProfile => {
  const data = preview.data

  const parserSections =
    parserProvidedTemplateSections(data)

  if (parserSections.length > 0) {
    return {
      schemaVersion: 1,
      source: "UPLOADED_TEMPLATE",
      sourceFileName: preview.fileName,
      sourceFileType: preview.fileType,
      sections: parserSections,
    }
  }

  const sections: SyllabusComparisonSectionDefinition[] = []

  const push = (
    key: SyllabusComparisonSectionKey,
    label: string,
    fields: SyllabusComparisonFieldDefinition[],
  ) => {
    if (fields.length > 0) {
      sections.push(section(key, label, fields))
    }
  }

  push(
    "general",
    "General Information",
    generalFieldsFromImport(data),
  )

  push(
    "workloadCredit",
    "Workload & Credit Points",
    workloadFieldsFromImport(data),
  )

  push(
    "requirements",
    "Requirements & Course Objectives",
    requirementFieldsFromImport(data),
  )

  push(
    "clo",
    "Course Learning Outcomes (CLO)",
    cloFieldsFromImport(data),
  )

  push(
    "content",
    "Content / Topics",
    contentFieldsFromImport(data),
  )

  if (hasAnyItems(data.topicCloMappings)) {
    push(
      "topicClo",
      "Topic–CLO Mapping",
      [
        field("topicName", "Topic"),
        field("weekNumber", "Week"),
        field("orderInWeek", "Order in Week"),
        field("cloCode", "CLO"),
        field("teachingLevel", "Teaching Level"),
      ],
    )
  }

  if (hasAnyItems(data.cloPloMappings)) {
    push(
      "cloPlo",
      "Learning Outcomes Matrix (CLO × PLO)",
      [
        field("cloCode", "CLO"),
        field("ploCode", "PLO"),
        field("level", "Contribution Level"),
        field("contributionWeight", "Contribution Weight"),
      ],
    )
  }

  push(
    "plannedActivities",
    "Planned Learning Activities",
    plannedActivityFieldsFromImport(data),
  )

  push(
    "assessment",
    "Assessment Plan",
    assessmentFieldsFromImport(data),
  )

  if (hasAnyItems(data.assessmentCloMappings)) {
    push(
      "assessmentClo",
      "Assessment–CLO Matrix",
      [
        field("assessmentName", "Assessment"),
        field("orderIndex", "Assessment Order"),
        field("cloCode", "CLO"),
        field("contributionPercent", "Contribution (%)"),
      ],
    )
  }

  push(
    "examination",
    "Examination & Study Requirements",
    examinationFieldsFromImport(data),
  )

  push(
    "readings",
    "Reading List",
    readingFieldsFromImport(data),
  )

  if (isDefined(data.dateRevised)) {
    push(
      "revision",
      "Revision Information",
      [
        field("dateRevised", "Date Revised"),
      ],
    )
  }

  return {
    schemaVersion: 1,
    source: "UPLOADED_TEMPLATE",
    sourceFileName: preview.fileName,
    sourceFileType: preview.fileType,
    sections,
  }
}

const standardProfile = (): SyllabusComparisonTemplateProfile => ({
  schemaVersion: 1,
  source: "STANDARD_FORM",
  sections: [
    section("general", "General Information", [
      field("courseCode", "Course Code"),
      field("courseName", "Course Name"),
      field("courseNameVn", "Course Name (Vietnamese)"),
      field("courseDesignation", "Course Designation"),
      field("courseTypes", "Course Type"),
      field("semester", "Semester"),
      field("personResponsible", "Person Responsible for the Course"),
      field("language", "Language of Instruction"),
      field("relation", "Relation to Curriculum"),
      field("teachingMethods", "Teaching Methods"),
      field("major", "Major / Academic Area"),
    ]),
    section("workloadCredit", "Workload & Credit Points", [
      field("workloadTotal", "Total Workload"),
      field("workloadContact", "Contact Hours"),
      field("workloadPrivate", "Self-study / Private Study Hours"),
      field("workloadStudentResponsibility", "Student Responsibility"),
      field("creditPoints", "Credit Points — Total"),
      field("lectureCredits", "Credits — Lecture"),
      field("laboratoryCredits", "Credits — Laboratory"),
    ]),
    section("requirements", "Requirements & Course Objectives", [
      field("prerequisites", "Required / Recommended Prerequisites"),
      field("objectives", "Course Objectives"),
    ]),
    section("clo", "Course Learning Outcomes (CLO)", [
      field("code", "Code"),
      field("description", "Description"),
      field("descriptionVn", "Vietnamese Description"),
      field("competencyLevel", "Competency Level"),
      field("bloomLevel", "Bloom Level"),
      field("orderIndex", "Display Order"),
    ]),
    section("content", "Content / Topics", [
      field("contentNote", "Content Note"),
      field("name", "Topic"),
      field("nameVn", "Vietnamese Topic"),
      field("weekNumber", "Week"),
      field("orderInWeek", "Order in Week"),
      field("teachingHours", "Teaching Hours"),
      field("labHours", "Laboratory Hours"),
      field("selfStudyHours", "Self-study Hours"),
      field("topicType", "Topic Type"),
      field("teachingMethod", "Teaching Method"),
      field("learningActivity", "Learning Activity"),
      field("assessments", "Mapped Assessments"),
      field("resources", "Resources"),
      field("contentWeight", "Content Weight"),
      field("contentLevel", "Content Level (I/T/U)"),
    ]),
    section("topicClo", "Topic–CLO Mapping", [
      field("topicName", "Topic"),
      field("weekNumber", "Week"),
      field("orderInWeek", "Order in Week"),
      field("cloCode", "CLO"),
      field("teachingLevel", "Teaching Level"),
    ]),
    section("cloPlo", "Learning Outcomes Matrix (CLO × PLO)", [
      field("cloCode", "CLO"),
      field("ploCode", "PLO"),
      field("level", "Contribution Level"),
      field("contributionWeight", "Contribution Weight"),
      field("notes", "Mapping Notes"),
    ]),
    section("plannedActivities", "Planned Learning Activities", [
      field("week", "Week"),
      field("topic", "Topic"),
      field("clo", "CLOs"),
      field("assessments", "Assessments"),
      field("learningActivities", "Learning Activities"),
      field("resources", "Resources"),
    ]),
    section("assessment", "Assessment Plan", [
      field("assessmentPassNote", "Assessment Pass Note"),
      field("name", "Assessment"),
      field("nameVn", "Vietnamese Name"),
      field("assessmentType", "Assessment Type"),
      field("weightPercent", "Weight (%)"),
      field("minScore", "Minimum Score"),
      field("maxScore", "Maximum Score"),
      field("orderIndex", "Display Order"),
    ]),
    section("assessmentClo", "Assessment–CLO Matrix", [
      field("assessmentName", "Assessment"),
      field("orderIndex", "Assessment Order"),
      field("cloCode", "CLO"),
      field("contributionPercent", "Contribution (%)"),
    ]),
    section("examination", "Examination & Study Requirements", [
      field("examForms", "Examination Forms"),
      field("examRequirements", "Study / Examination Requirements"),
    ]),
    section("readings", "Reading List", [
      field("title", "Title"),
      field("author", "Author"),
      field("publisher", "Publisher"),
      field("year", "Publication Year"),
      field("edition", "Edition"),
      field("isbn", "ISBN"),
      field("url", "URL"),
      field("bookType", "Resource Type"),
      field("usageType", "Usage Type"),
      field("orderIndex", "Display Order"),
    ]),
    section("revision", "Revision Information", [
      field("dateRevised", "Date Revised"),
      field("internalNotes", "Internal Notes"),
      field("changeSummary", "Change Summary"),
    ]),
  ],
})

export const STANDARD_SYLLABUS_COMPARISON_TEMPLATE =
  standardProfile()

const validSectionKeys =
  new Set<SyllabusComparisonSectionKey>(
    STANDARD_SYLLABUS_COMPARISON_TEMPLATE
      .sections
      .map((item) => item.key),
  )

export const readSyllabusComparisonTemplateProfile = (
  rawNotes?: string | null,
): SyllabusComparisonTemplateProfile | undefined => {
  if (!rawNotes?.trim()) {
    return undefined
  }

  try {
    const root = asRecord(JSON.parse(rawNotes))
    const profile = asRecord(root.comparisonTemplate)

    if (
      Number(profile.schemaVersion) !== 1
      || !Array.isArray(profile.sections)
    ) {
      return undefined
    }

    const sections =
      profile.sections
        .map((rawSection) => {
          const source = asRecord(rawSection)
          const key =
            String(source.key ?? "")
              .trim() as SyllabusComparisonSectionKey

          if (!validSectionKeys.has(key)) {
            return null
          }

          const fields =
            Array.isArray(source.fields)
              ? source.fields
                  .map((rawField) => {
                    const sourceField =
                      asRecord(rawField)

                    const keyValue =
                      String(
                        sourceField.key
                        ?? "",
                      ).trim()

                    if (!keyValue) {
                      return null
                    }

                    return field(
                      keyValue,
                      String(
                        sourceField.label
                        ?? keyValue,
                      ).trim()
                      || keyValue,
                    )
                  })
                  .filter(
                    (
                      item,
                    ): item is SyllabusComparisonFieldDefinition =>
                      item !== null,
                  )
              : []

          if (fields.length === 0) {
            return null
          }

          return section(
            key,
            String(
              source.label
              ?? key,
            ).trim()
            || key,
            fields,
          )
        })
        .filter(
          (
            item,
          ): item is SyllabusComparisonSectionDefinition =>
            item !== null,
        )

    if (sections.length === 0) {
      return undefined
    }

    const source =
      String(profile.source ?? "")
        .trim()
        .toUpperCase()

    return {
      schemaVersion: 1,
      source:
        source === "UPLOADED_TEMPLATE"
          ? "UPLOADED_TEMPLATE"
          : source === "STANDARD_FORM"
            ? "STANDARD_FORM"
            : "INFERRED",
      sourceFileName:
        typeof profile.sourceFileName === "string"
          ? profile.sourceFileName
          : undefined,
      sourceFileType:
        typeof profile.sourceFileType === "string"
          ? profile.sourceFileType
          : undefined,
      sections,
    }
  } catch {
    return undefined
  }
}

const rawNotesObject = (
  rawNotes?: string | null,
): UnknownRecord => {
  if (!rawNotes?.trim()) {
    return {}
  }

  try {
    return asRecord(JSON.parse(rawNotes))
  } catch {
    return {}
  }
}

const rawHas = (
  source: UnknownRecord,
  ...keys: string[]
) =>
  keys.some((key) =>
    Object.prototype.hasOwnProperty.call(
      source,
      key,
    ),
  )

const diffHasItems = <T,>(
  diff:
    | {
        added?: T[]
        removed?: T[]
        modified?: T[]
      }
    | undefined,
) =>
  Boolean(
    diff
    && (
      (diff.added?.length ?? 0) > 0
      || (diff.removed?.length ?? 0) > 0
      || (diff.modified?.length ?? 0) > 0
    ),
  )

const listFieldsFromSnapshot = (
  items: unknown[] | undefined,
  definitions: Array<
    [string, string]
  >,
) =>
  definitions
    .filter(([key]) =>
      hasAnyDefined(items, key),
    )
    .map(([key, label]) =>
      field(key, label),
    )

/**
 * Compatibility fallback for syllabuses saved before comparisonTemplate
 * metadata existed.
 *
 * Imported legacy syllabuses are inferred from the data that was actually
 * persisted. Manual syllabuses use the standard form profile.
 */
export const inferSyllabusComparisonTemplateProfile = (
  syllabus: Syllabus,
  diff?: SyllabusDiffResponse,
): SyllabusComparisonTemplateProfile => {
  const sourceType =
    String(syllabus.sourceType ?? "")
      .toUpperCase()

  if (
    !sourceType.startsWith("IMPORT_")
    && sourceType !== "CLONE"
  ) {
    return STANDARD_SYLLABUS_COMPARISON_TEMPLATE
  }

  const notes = rawNotesObject(
    syllabus.notes,
  )

  const sections: SyllabusComparisonSectionDefinition[] = []

  const push = (
    key: SyllabusComparisonSectionKey,
    label: string,
    fields: SyllabusComparisonFieldDefinition[],
  ) => {
    if (fields.length > 0) {
      sections.push(
        section(key, label, fields),
      )
    }
  }

  push(
    "general",
    "General Information",
    [
      field("courseCode", "Course Code"),
      field("courseName", "Course Name"),
      isDefined(syllabus.courseNameVn)
        ? field("courseNameVn", "Course Name (Vietnamese)")
        : null,
      isDefined(syllabus.courseDesignation)
        ? field("courseDesignation", "Course Designation")
        : null,
      isDefined(syllabus.courseTypes)
        ? field("courseTypes", "Course Type")
        : null,
      isDefined(syllabus.semester)
        ? field("semester", "Semester")
        : null,
      rawHas(notes, "personResponsible", "instructor")
        ? field("personResponsible", "Person Responsible for the Course")
        : null,
      isDefined(syllabus.language)
        ? field("language", "Language of Instruction")
        : null,
      isDefined(syllabus.relation)
        ? field("relation", "Relation to Curriculum")
        : null,
      isDefined(syllabus.teachingMethods)
        ? field("teachingMethods", "Teaching Methods")
        : null,
      isDefined(syllabus.major)
        ? field("major", "Major / Academic Area")
        : null,
    ].filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )

  push(
    "workloadCredit",
    "Workload & Credit Points",
    [
      isDefined(syllabus.workloadTotal)
        ? field("workloadTotal", "Total Workload")
        : null,
      isDefined(syllabus.workloadContact)
        ? field("workloadContact", "Contact Hours")
        : null,
      isDefined(syllabus.workloadPrivate)
        ? field("workloadPrivate", "Self-study / Private Study Hours")
        : null,
      rawHas(notes, "workloadStudentResponsibility")
        ? field("workloadStudentResponsibility", "Student Responsibility")
        : null,
      rawHas(notes, "creditPoints", "ects")
        ? field("creditPoints", "Credit Points — Total")
        : null,
      rawHas(notes, "lectureCredits", "creditsTheory")
        ? field("lectureCredits", "Credits — Lecture")
        : null,
      rawHas(notes, "laboratoryCredits", "creditsPractice")
        ? field("laboratoryCredits", "Credits — Laboratory")
        : null,
    ].filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )

  push(
    "requirements",
    "Requirements & Course Objectives",
    [
      isDefined(syllabus.prerequisites)
        ? field("prerequisites", "Required / Recommended Prerequisites")
        : null,
      isDefined(syllabus.objectives)
        ? field("objectives", "Course Objectives")
        : null,
    ].filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )

  const clos =
    syllabus.clos as unknown[] | undefined

  if (
    (clos?.length ?? 0) > 0
    || diffHasItems(diff?.cloDiff)
  ) {
    const cloFields = [
      field("code", "Code"),
      ...listFieldsFromSnapshot(
        clos,
        [
          ["description", "Description"],
          ["descriptionVn", "Vietnamese Description"],
          ["competencyLevel", "Competency Level"],
          ["bloomLevel", "Bloom Level"],
          ["orderIndex", "Display Order"],
        ],
      ),
    ]

    push(
      "clo",
      "Course Learning Outcomes (CLO)",
      cloFields,
    )
  }

  const topics =
    syllabus.topics as unknown[] | undefined

  if (
    (topics?.length ?? 0) > 0
    || rawHas(notes, "contentNote")
    || diffHasItems(diff?.topicDiff)
  ) {
    const contentFields: SyllabusComparisonFieldDefinition[] = []

    if (rawHas(notes, "contentNote")) {
      contentFields.push(
        field("contentNote", "Content Note"),
      )
    }

    if (
      (topics?.length ?? 0) > 0
      || diffHasItems(diff?.topicDiff)
    ) {
      contentFields.push(
        field("name", "Topic"),
        ...listFieldsFromSnapshot(
          topics,
          [
            ["nameVn", "Vietnamese Topic"],
            ["weekNumber", "Week"],
            ["orderInWeek", "Order in Week"],
            ["teachingHours", "Teaching Hours"],
            ["labHours", "Laboratory Hours"],
            ["selfStudyHours", "Self-study Hours"],
            ["topicType", "Topic Type"],
            ["teachingMethod", "Teaching Method"],
            ["learningActivity", "Learning Activity"],
            ["assessments", "Mapped Assessments"],
            ["resources", "Resources"],
          ],
        ),
      )

      if (rawHas(notes, "topicDetails")) {
        contentFields.push(
          field("contentWeight", "Content Weight"),
          field("contentLevel", "Content Level (I/T/U)"),
        )
      }
    }

    push(
      "content",
      "Content / Topics",
      contentFields,
    )
  }

  if (
    rawHas(notes, "topicDetails")
    || diffHasItems(diff?.topicCloDiff)
  ) {
    push(
      "topicClo",
      "Topic–CLO Mapping",
      [
        field("topicName", "Topic"),
        field("weekNumber", "Week"),
        field("orderInWeek", "Order in Week"),
        field("cloCode", "CLO"),
        field("teachingLevel", "Teaching Level"),
      ],
    )
  }

  if (
    rawHas(notes, "cloPloMatrix", "loMatrix")
    || diffHasItems(diff?.cloPloDiff)
  ) {
    push(
      "cloPlo",
      "Learning Outcomes Matrix (CLO × PLO)",
      [
        field("cloCode", "CLO"),
        field("ploCode", "PLO"),
        field("level", "Contribution Level"),
        field("contributionWeight", "Contribution Weight"),
        field("notes", "Mapping Notes"),
      ],
    )
  }

  if (
    rawHas(notes, "plannedActivities", "activities")
    || diffHasItems(diff?.plannedActivityDiff)
  ) {
    push(
      "plannedActivities",
      "Planned Learning Activities",
      [
        field("week", "Week"),
        field("topic", "Topic"),
        field("clo", "CLOs"),
        field("assessments", "Assessments"),
        field("learningActivities", "Learning Activities"),
        field("resources", "Resources"),
      ],
    )
  }

  const assessments =
    syllabus.assessments as unknown[] | undefined

  if (
    (assessments?.length ?? 0) > 0
    || rawHas(notes, "assessmentPassNote")
    || diffHasItems(diff?.assessmentDiff)
  ) {
    const fields: SyllabusComparisonFieldDefinition[] = []

    if (rawHas(notes, "assessmentPassNote")) {
      fields.push(
        field("assessmentPassNote", "Assessment Pass Note"),
      )
    }

    if (
      (assessments?.length ?? 0) > 0
      || diffHasItems(diff?.assessmentDiff)
    ) {
      fields.push(
        field("name", "Assessment"),
        ...listFieldsFromSnapshot(
          assessments,
          [
            ["nameVn", "Vietnamese Name"],
            ["assessmentType", "Assessment Type"],
            ["weightPercent", "Weight (%)"],
            ["minScore", "Minimum Score"],
            ["maxScore", "Maximum Score"],
            ["orderIndex", "Display Order"],
          ],
        ),
      )
    }

    push(
      "assessment",
      "Assessment Plan",
      fields,
    )
  }

  if (
    rawHas(notes, "assessmentCloMatrix", "assessmentMatrix")
    || diffHasItems(diff?.assessmentCloDiff)
  ) {
    push(
      "assessmentClo",
      "Assessment–CLO Matrix",
      [
        field("assessmentName", "Assessment"),
        field("orderIndex", "Assessment Order"),
        field("cloCode", "CLO"),
        field("contributionPercent", "Contribution (%)"),
      ],
    )
  }

  push(
    "examination",
    "Examination & Study Requirements",
    [
      isDefined(syllabus.examForms)
        ? field("examForms", "Examination Forms")
        : null,
      isDefined(syllabus.examRequirements)
        ? field("examRequirements", "Study / Examination Requirements")
        : null,
    ].filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )

  if (
    rawHas(notes, "readings", "references")
    || diffHasItems(diff?.readingsDiff)
  ) {
    push(
      "readings",
      "Reading List",
      [
        field("title", "Title"),
        field("author", "Author"),
        field("publisher", "Publisher"),
        field("year", "Publication Year"),
        field("edition", "Edition"),
        field("isbn", "ISBN"),
        field("url", "URL"),
        field("bookType", "Resource Type"),
        field("usageType", "Usage Type"),
        field("orderIndex", "Display Order"),
      ],
    )
  }

  push(
    "revision",
    "Revision Information",
    [
      rawHas(notes, "dateRevised")
        ? field("dateRevised", "Date Revised")
        : null,
      rawHas(notes, "internalNotes")
        ? field("internalNotes", "Internal Notes")
        : null,
      isDefined(syllabus.changeSummary)
        ? field("changeSummary", "Change Summary")
        : null,
    ].filter(Boolean) as SyllabusComparisonFieldDefinition[],
  )

  return {
    schemaVersion: 1,
    source: "INFERRED",
    sourceFileName:
      syllabus.originalFileName ?? undefined,
    sourceFileType:
      syllabus.originalFileType ?? undefined,
    sections,
  }
}

/**
 * Comparison across two cohorts uses the union of the two uploaded shapes.
 *
 * New-cohort order is authoritative. Fields/sections that existed only in
 * the old cohort are appended so removed template content is never hidden.
 */
export const mergeSyllabusComparisonTemplateProfiles = (
  oldProfile: SyllabusComparisonTemplateProfile,
  newProfile: SyllabusComparisonTemplateProfile,
): SyllabusComparisonTemplateProfile => {
  const oldByKey =
    new Map(
      oldProfile.sections.map(
        (item) => [item.key, item],
      ),
    )

  const newByKey =
    new Map(
      newProfile.sections.map(
        (item) => [item.key, item],
      ),
    )

  const orderedKeys =
    Array.from(
      new Set([
        ...newProfile.sections.map(
          (item) => item.key,
        ),
        ...oldProfile.sections.map(
          (item) => item.key,
        ),
      ]),
    )

  const sections =
    orderedKeys.map((key) => {
      const oldSection =
        oldByKey.get(key)

      const newSection =
        newByKey.get(key)

      const orderedFields =
        Array.from(
          new Map(
            [
              ...(newSection?.fields ?? []),
              ...(oldSection?.fields ?? []),
            ].map(
              (item) => [item.key, item],
            ),
          ).values(),
        )

      return section(
        key,
        newSection?.label
        || oldSection?.label
        || key,
        orderedFields,
      )
    })

  return {
    schemaVersion: 1,
    source:
      oldProfile.source === "UPLOADED_TEMPLATE"
      || newProfile.source === "UPLOADED_TEMPLATE"
        ? "UPLOADED_TEMPLATE"
        : oldProfile.source === "STANDARD_FORM"
          && newProfile.source === "STANDARD_FORM"
          ? "STANDARD_FORM"
          : "INFERRED",
    sourceFileName:
      newProfile.sourceFileName
      || oldProfile.sourceFileName,
    sourceFileType:
      newProfile.sourceFileType
      || oldProfile.sourceFileType,
    sections,
  }
}
