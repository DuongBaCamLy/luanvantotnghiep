import { api } from "./axios"
import type { Topic, CreateTopicRequest } from "@/types/topic"

export const topicApi = {
  getBySyllabus: async (syllabusId: number): Promise<Topic[]> => {
    const res = await api.get<Topic[]>(`/api/topics/syllabus/${syllabusId}`)
    return res.data
  },

  create: async (data: CreateTopicRequest): Promise<Topic> => {
    const res = await api.post<Topic>("/api/topics", data)
    return res.data
  },

  update: async (id: number, data: CreateTopicRequest): Promise<Topic> => {
    const res = await api.put<Topic>(`/api/topics/${id}`, data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/topics/${id}`)
  },
}
