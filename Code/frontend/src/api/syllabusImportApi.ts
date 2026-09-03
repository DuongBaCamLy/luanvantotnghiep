import { api } from "./axios"

import type {
  BulkSyllabusImportPreviewResponse,
  BulkConfirmSyllabusImportRequest,
  ConfirmSyllabusImportRequest,
  SyllabusImportPreviewResponse
} from "@/types/syllabusImport"
import type { Syllabus } from "@/types/syllabus"



export const syllabusImportApi = {


  preview: async (
    file: File
  ): Promise<SyllabusImportPreviewResponse> => {


    const formData = new FormData()


    formData.append(
      "file",
      file
    )


    const response =
      await api.post<SyllabusImportPreviewResponse>(
        "/api/syllabus-import/preview",
        formData,
        {
          headers:{
            "Content-Type":
              "multipart/form-data"
          }
        }
      )


    return response.data

  },

  previewBulk: async (file: File, programId: number, cohortId: number): Promise<BulkSyllabusImportPreviewResponse> => {
    const formData = new FormData()
    formData.append("file", file)
    const response = await api.post<BulkSyllabusImportPreviewResponse>(
      `/api/syllabus-import/preview-bulk?programId=${programId}&cohortId=${cohortId}`,
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
        // A programme specification can contain hundreds of PDF pages.
        // Keep the request alive while the backend separates every syllabus.
        timeout: 180_000,
      },
    )
    return response.data
  },
  confirm: async (request: ConfirmSyllabusImportRequest): Promise<Syllabus> => {
    const response = await api.post<Syllabus>("/api/syllabus-import/confirm", request)
    return response.data
  },
  confirmBulkItem: async (request: BulkConfirmSyllabusImportRequest): Promise<Syllabus> => {
    const response = await api.post<Syllabus>("/api/syllabus-import/confirm-bulk-item", request)
    return response.data
  },

}
