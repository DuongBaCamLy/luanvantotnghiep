import { api } from "./axios"
import type { CreateCurriculumRequest, CreateCurriculumResponse } from "@/types/admin"

export const curriculumApi = {
  create: async (data: CreateCurriculumRequest): Promise<CreateCurriculumResponse> => {
    const response = await api.post<CreateCurriculumResponse>("/api/curricula", data)
    return response.data
  },
}
