import { api } from "./axios"

export interface CourseTypeOption {
  id: number
  code: string
  name: string
  nameVn?: string | null
}

export const courseTypeApi = {
  getAll: () => api.get<CourseTypeOption[]>("/api/course-types").then((res) => res.data),
}
