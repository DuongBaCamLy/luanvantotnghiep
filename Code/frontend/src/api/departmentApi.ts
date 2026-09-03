import { api } from "./axios"
import type { Department, CreateDepartmentRequest, DepartmentHeadCandidate } from "@/types/admin"

export const departmentApi = {
  getAll: async (): Promise<Department[]> => {
    const response = await api.get<Department[]>("/api/departments")
    return response.data
  },

  create: async (data: CreateDepartmentRequest): Promise<Department> => {
    const response = await api.post<Department>("/api/departments", data)
    return response.data
  },

  getHeadCandidates: async (): Promise<DepartmentHeadCandidate[]> => {
    const response = await api.get<DepartmentHeadCandidate[]>("/api/departments/head-candidates")
    return response.data
  },

  assignHead: async (departmentId: number, userAccountId: number): Promise<Department> => {
    const response = await api.put<Department>(`/api/departments/${departmentId}/head`, { userAccountId })
    return response.data
  },
}
