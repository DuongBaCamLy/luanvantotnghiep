import { api } from "./axios"
import type { Cohort } from "@/types/admin"

export const cohortApi = {
  create: async (data: {
    programId: number
    entryYear: number
    description?: string
  }): Promise<Cohort> => {
    const res = await api.post<Cohort>("/api/cohorts", data)
    return res.data
  },

  getAll: async (): Promise<Cohort[]> => {
    const res = await api.get<Cohort[]>("/api/cohorts")
    return res.data
  },

  getArchived: async (): Promise<Cohort[]> => {
    const res = await api.get<Cohort[]>("/api/cohorts/archived")
    return res.data
  },

  getById: async (id: number): Promise<Cohort> => {
    const res = await api.get<Cohort>(`/api/cohorts/${id}`)
    return res.data
  },

  getByName: async (name: string): Promise<Cohort> => {
    const res = await api.get<Cohort>(
      `/api/cohorts/name/${encodeURIComponent(name)}`,
    )
    return res.data
  },

  getByProgram: async (
    programId: number,
  ): Promise<Cohort[]> => {
    const res = await api.get<Cohort[]>(
      `/api/cohorts/program/${programId}`,
    )
    return res.data
  },

  archive: async (id: number): Promise<Cohort> => {
    const res = await api.post<Cohort>(
      `/api/cohorts/${id}/archive`,
    )
    return res.data
  },

  reactivate: async (id: number): Promise<Cohort> => {
    const res = await api.post<Cohort>(
      `/api/cohorts/${id}/reactivate`,
    )
    return res.data
  },
}