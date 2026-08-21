import { api } from "./axios"
import type {
  Clo,
  CreateCloRequest,
  CloPloMapping,
  CreateCloPloMappingRequest,
} from "@/types/clo"

export const cloApi = {
  getBySyllabus: async (syllabusId: number): Promise<Clo[]> => {
    const res = await api.get<Clo[]>(
      `/api/clos/syllabus/${syllabusId}`
    )
    return res.data
  },

  create: async (
    data: CreateCloRequest
  ): Promise<Clo> => {
    const res = await api.post<Clo>(
      "/api/clos",
      data
    )
    return res.data
  },

  delete: async (
    id: number
  ): Promise<void> => {
    await api.delete(
      `/api/clos/${id}`
    )
  },
}

export const cloPloMappingApi = {
  getByClo: async (
    cloId: number
  ): Promise<CloPloMapping[]> => {
    const res = await api.get<CloPloMapping[]>(
      `/api/clo-plo-mappings/clo/${cloId}`
    )
    return res.data
  },

  create: async (
    data: CreateCloPloMappingRequest
  ): Promise<CloPloMapping> => {
    const res = await api.post<CloPloMapping>(
      "/api/clo-plo-mappings",
      data
    )
    return res.data
  },

  delete: async (
    id: number
  ): Promise<void> => {
    await api.delete(
      `/api/clo-plo-mappings/${id}`
    )
  },
}