import { api } from "./axios"

export type TopicCloLink = {
  topicId: number
  topicName?: string
  cloId: number
  cloCode?: string
  teachingLevel: "I" | "D" | "A"
}

export type AssessmentCloLink = {
  assessmentComponentId: number
  assessmentName?: string
  cloId: number
  cloCode?: string
  contributionPercent: number
}

export const standardSyllabusApi = {
  getTopicClosByTopic: async (topicId: number): Promise<TopicCloLink[]> => {
    const response = await api.get<TopicCloLink[]>(`/api/topic-clos/topic/${topicId}`)
    return response.data
  },
  createTopicClo: async (
    data: { topicId: number; cloId: number; teachingLevel: "I" | "D" | "A" },
  ): Promise<TopicCloLink> => {
    const response = await api.post<TopicCloLink>("/api/topic-clos", data)
    return response.data
  },
  deleteTopicClo: async (topicId: number, cloId: number): Promise<void> => {
    await api.delete(`/api/topic-clos/${topicId}/${cloId}`)
  },
  getAssessmentClosByAssessment: async (assessmentId: number): Promise<AssessmentCloLink[]> => {
    const response = await api.get<AssessmentCloLink[]>(`/api/assessment-clos/assessment/${assessmentId}`)
    return response.data
  },
  createAssessmentClo: async (
    data: { assessmentComponentId: number; cloId: number; contributionPercent: number },
  ): Promise<AssessmentCloLink> => {
    const response = await api.post<AssessmentCloLink>("/api/assessment-clos", data)
    return response.data
  },
  deleteAssessmentClo: async (assessmentId: number, cloId: number): Promise<void> => {
    await api.delete(`/api/assessment-clos/${assessmentId}/${cloId}`)
  },
}
