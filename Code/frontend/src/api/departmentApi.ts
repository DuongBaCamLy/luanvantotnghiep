import { api } from "./axios"
import type { Department, CreateDepartmentRequest } from "@/types/admin"

export const departmentApi = {
  getAll: async (): Promise<Department[]> => {
    const response = await api.get<Department[]>("/api/departments")
    return response.data
  },

  create: async (data: CreateDepartmentRequest): Promise<Department> => {
    const response = await api.post<Department>("/api/departments", data)
    return response.data
  }
}
