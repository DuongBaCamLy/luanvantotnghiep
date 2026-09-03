import { api } from "./axios";
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

  getById: async (id: number) => {
    const res = await api.get(`/api/cohorts/${id}`);
    return res.data;
  },

  getByName: async (name: string) => {
    const res = await api.get(`/api/cohorts/name/${name}`);
    return res.data;
  },
  getByProgram: async (
  programId: number
): Promise<Cohort[]> => {
  const res = await api.get<Cohort[]>(
    `/api/cohorts/program/${programId}`
  )

  return res.data
},
  archive: async (id: number): Promise<Cohort> => {
    const res = await api.post<Cohort>(`/api/cohorts/${id}/archive`)
    return res.data
  },
  reactivate: async (id: number): Promise<Cohort> => {
    const res = await api.post<Cohort>(`/api/cohorts/${id}/reactivate`)
    return res.data
  },
};
