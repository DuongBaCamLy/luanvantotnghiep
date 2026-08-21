import { api } from "./axios"
import type {
  AssessmentComponent,
  CreateAssessmentComponentRequest,
} from "@/types/assessment"

export const assessmentApi = {
  getBySyllabus: async (
    syllabusId: number
  ): Promise<AssessmentComponent[]> => {
    const res = await api.get<AssessmentComponent[]>(
      `/api/assessment-components/syllabus/${syllabusId}`
    )
    return res.data
  },

  create: async (
    data: CreateAssessmentComponentRequest
  ): Promise<AssessmentComponent> => {
    const res = await api.post<AssessmentComponent>(
      "/api/assessment-components",
      data
    )
    return res.data
  },

  update: async (
    id: number,
    data: CreateAssessmentComponentRequest
  ): Promise<AssessmentComponent> => {
    const res = await api.put<AssessmentComponent>(
      `/api/assessment-components/${id}`,
      data
    )
    return res.data
  },

  delete: async (
    id: number
  ): Promise<void> => {
    await api.delete(
      `/api/assessment-components/${id}`
    )
  },
}