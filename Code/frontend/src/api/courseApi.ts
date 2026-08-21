import { api } from "./axios"
import type { Course, CreateCourseRequest } from "@/types/course"

export const courseApi = {
  getAll: async (): Promise<Course[]> => {
    const response = await api.get<Course[]>("/api/courses")
    return response.data
  },
  
  getById: async (id: number): Promise<Course> => {
    const response = await api.get<Course>(`/api/courses/${id}`)
    return response.data
  },

  create: async (data: CreateCourseRequest): Promise<Course> => {
    const response = await api.post<Course>("/api/courses", data)
    return response.data
  }
}

