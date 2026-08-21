import { api } from "./axios"
import type { Syllabus } from "@/types/syllabus"
import type { SyllabusImportData, SyllabusImportMode, SyllabusImportPreviewResponse } from "@/types/syllabusImport"

export const syllabusImportApi = {
  preview: async (syllabusId: number, file: File): Promise<SyllabusImportPreviewResponse> => {
    const form = new FormData()
    form.append("file", file)
    const response = await api.post(`/api/syllabuses/${syllabusId}/import/preview`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    return response.data
  },
  confirm: async (syllabusId: number, data: SyllabusImportData, importMode: SyllabusImportMode = "MERGE"): Promise<Syllabus> => {
    const response = await api.post(`/api/syllabuses/${syllabusId}/import/confirm`, { data, importMode })
    return response.data
  },
}
