export type TopicType = "LECTURE" | "LAB" | "SEMINAR" | "PROJECT" | "SELF_STUDY"

export interface Topic {
  id: number
  syllabusId: number
  weekNumber: number
  orderInWeek?: number
  name: string
  nameVn?: string
  teachingHours?: number
  labHours?: number
  selfStudyHours?: number
  topicType?: TopicType | string
  teachingMethod?: string
  learningActivity?: string
  notes?: string
}

export interface CreateTopicRequest {
  syllabusId: number
  weekNumber: number
  orderInWeek?: number
  name: string
  nameVn?: string
  teachingHours?: number
  labHours?: number
  selfStudyHours?: number
  topicType?: string
  teachingMethod?: string
  learningActivity?: string
  notes?: string
}

export const TOPIC_TYPES: { value: TopicType; label: string }[] = [
  { value: "LECTURE", label: "Lý thuyết (Lecture)" },
  { value: "LAB", label: "Thực hành (Lab)" },
  { value: "SEMINAR", label: "Seminar" },
  { value: "PROJECT", label: "Dự án (Project)" },
  { value: "SELF_STUDY", label: "Tự học (Self Study)" },
]
