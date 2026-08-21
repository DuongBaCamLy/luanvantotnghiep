import { api } from "./axios"
import type {
  CloneProgramRequest,
  CloneProgramResponse,
  CreateProgramRequest,
  CurriculumTimelineItem,
  Major,
  Program,
  ProgramCreditValidation,
  ProgramArchiveValidation,
  ProgramType,
  UpdateProgramRequest,
} from "@/types/admin"

export const programApi = {
  getAll: async (): Promise<Program[]> => {
    const response = await api.get<Program[]>("/api/programs")
    return response.data
  },
  getById: async (id: number): Promise<Program> => {
  const response = await api.get<Program>(
    `/api/programs/${id}`
  )

  return response.data
},

  create: async (data: CreateProgramRequest): Promise<Program> => {
    const response = await api.post<Program>("/api/programs", data)
    return response.data
  },

  update: async (id: number, data: UpdateProgramRequest): Promise<Program> => {
    const response = await api.put<Program>(`/api/programs/${id}`, data)
    return response.data
  },

  validateArchive: async (id: number): Promise<ProgramArchiveValidation> => {
    const response = await api.get<ProgramArchiveValidation>(`/api/programs/${id}/archive-validation`)
    return response.data
  },

  archive: async (id: number): Promise<Program> => {
    const response = await api.post<Program>(`/api/programs/${id}/archive`)
    return response.data
  },

  reactivate: async (id: number): Promise<Program> => {
    const response = await api.post<Program>(`/api/programs/${id}/reactivate`)
    return response.data
  },

  getMajors: async (): Promise<Major[]> => {
    const response = await api.get<Major[]>("/api/majors")
    return response.data
  },

  getProgramTypes: async (): Promise<ProgramType[]> => {
    const response = await api.get<ProgramType[]>("/api/program-types")
    return response.data
  },

  getDiff: async (id: number, oldCohortId: number, newCohortId: number): Promise<any> => {
    const response = await api.get(`/api/programs/${id}/diff`, {
      params: { oldCohortId, newCohortId }
    })
    return response.data
  },

  validateCredits: async (id: number, cohortId: number): Promise<ProgramCreditValidation> => {
    const response = await api.get<ProgramCreditValidation>(`/api/programs/${id}/credit-validation`, {
      params: { cohortId },
    })
    return response.data
  },



  cloneToCohort: async (
  id: number,
  data: CloneProgramRequest
): Promise<CloneProgramResponse> => {
  const response = await api.post<CloneProgramResponse>(
    `/api/programs/${id}/clone-cohort`,
    data
  )

  return response.data
},

getCurriculumTimeline: async (
  id: number
): Promise<CurriculumTimelineItem[]> => {
  const response = await api.get<
    CurriculumTimelineItem[]
  >(
    `/api/programs/${id}/curriculum-timeline`
  )

  return response.data
},
}
