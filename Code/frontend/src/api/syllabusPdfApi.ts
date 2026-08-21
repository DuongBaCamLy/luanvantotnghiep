import type { AxiosResponse } from "axios"
import { api } from "@/api/axios"

const decodeFilename = (response: AxiosResponse<Blob>, fallback: string) => {
  const disposition = response.headers["content-disposition"] as string | undefined
  if (!disposition) return fallback

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }

  const simpleMatch = disposition.match(/filename="?([^";]+)"?/i)
  return simpleMatch?.[1] || fallback
}

const downloadResponse = (response: AxiosResponse<Blob>, fallback: string) => {
  const filename = decodeFilename(response, fallback)
  const url = window.URL.createObjectURL(response.data)
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => window.URL.revokeObjectURL(url), 1_000)
}

export const syllabusPdfApi = {
  fetchPreview: async (syllabusId: number): Promise<Blob> => {
    const response = await api.get<Blob>(
      `/api/syllabuses/${syllabusId}/pdf/preview`,
      { responseType: "blob" }
    )
    return response.data
  },

  download: async (syllabusId: number, fallbackName?: string): Promise<void> => {
    const response = await api.get<Blob>(
      `/api/syllabuses/${syllabusId}/pdf`,
      { responseType: "blob" }
    )
    downloadResponse(
      response,
      fallbackName || `Syllabus_${syllabusId}.pdf`
    )
  },

  downloadFromReports: async (
    syllabusId: number,
    fallbackName?: string
  ): Promise<void> => {
    const response = await api.get<Blob>(
      `/api/reports/syllabuses/${syllabusId}/pdf`,
      { responseType: "blob" }
    )
    downloadResponse(
      response,
      fallbackName || `Syllabus_${syllabusId}.pdf`
    )
  },
}
