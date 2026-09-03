export type FieldDiff = {
  oldValue?: string | null
  newValue?: string | null
}

export type ListDiff<T> = {
  added?: T[]
  removed?: T[]
  modified?: T[]
}

export type CloDiff = {
  code?: string
  description?: string
  descriptionVn?: string
  competencyLevel?: string
  bloomLevel?: string
  orderIndex?: number
  plos?: string[]
  changes?: Record<string, FieldDiff>
}

export type CloPloMappingDiff = {
  cloCode?: string
  ploCode?: string
  level?: string
  contributionWeight?: number
  notes?: string
  changes?: Record<string, FieldDiff>
}

export type TopicDiff = {
  name?: string
  nameVn?: string
  weekNumber?: number
  orderInWeek?: number
  changes?: Record<string, FieldDiff>
}

export type TopicCloMappingDiff = {
  topicName?: string
  weekNumber?: number
  orderInWeek?: number
  cloCode?: string
  teachingLevel?: string
  changes?: Record<string, FieldDiff>
}

export type AssessmentDiff = {
  name?: string
  nameVn?: string
  weightPercent?: number
  orderIndex?: number
  changes?: Record<string, FieldDiff>
}

export type AssessmentCloMappingDiff = {
  assessmentName?: string
  orderIndex?: number
  cloCode?: string
  contributionPercent?: number
  changes?: Record<string, FieldDiff>
}

export type readingsDiff = {
  bookId?: number
  title?: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  bookType?: string
  usageType?: string
  orderIndex?: number
  changes?: Record<string, FieldDiff>
}

export type SyllabusDiffResponse = {
  oldSyllabusId: number
  newSyllabusId: number
  oldVersionLabel: string
  newVersionLabel: string
  hasChanges: boolean
  generalInfoDiff?: Record<string, FieldDiff>
  cloDiff?: ListDiff<CloDiff>
  cloPloDiff?: ListDiff<CloPloMappingDiff>
  topicDiff?: ListDiff<TopicDiff>
  topicCloDiff?: ListDiff<TopicCloMappingDiff>
  assessmentDiff?: ListDiff<AssessmentDiff>
  assessmentCloDiff?: ListDiff<AssessmentCloMappingDiff>
  readingsDiff?: ListDiff<readingsDiff>
}
