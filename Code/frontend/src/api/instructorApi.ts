import { api } from "./axios"

import type {
  Instructor,
  InstructorRequest,
} from "@/types/instructor"

export interface InstructorExportFilters {
  instructorIds: number[]
  search?: string
  department?: string
  role?: string
  degree?: string
  status?: string
}

export const instructorApi = {
  getAll:
    async (): Promise<Instructor[]> => {
      const response =
        await api.get<Instructor[]>(
          "/api/instructors",
        )

      return response.data
    },

  getById:
    async (
      id: number,
    ): Promise<Instructor> => {
      const response =
        await api.get<Instructor>(
          `/api/instructors/${id}`,
        )

      return response.data
    },

  create:
    async (
      data: InstructorRequest,
    ): Promise<Instructor> => {
      const response =
        await api.post<Instructor>(
          "/api/instructors",
          data,
        )

      return response.data
    },

  update:
    async (
      id: number,
      data: InstructorRequest,
    ): Promise<Instructor> => {
      const response =
        await api.put<Instructor>(
          `/api/instructors/${id}`,
          data,
        )

      return response.data
    },

  delete:
    async (
      id: number,
    ): Promise<string> => {
      const response =
        await api.delete<string>(
          `/api/instructors/${id}`,
        )

      return response.data
    },

  exportExcel:
    async (
      filters: InstructorExportFilters,
    ): Promise<Blob> => {
      const response =
        await api.post(
          "/api/instructors/export",
          filters,
          {
            responseType: "blob",
          },
        )

      return response.data as Blob
    },
}
