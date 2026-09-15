import { formatVersionLabel } from "@/lib/syllabusVersion"
import { api } from "./axios"
import type {
  CloneSyllabusRequest,
  CreateSyllabusRequest,
  Syllabus,
  SubmissionValidationResponse,
  SyllabusCreateContextResponse,
} from "@/types/syllabus"
import type {
  SemanticSyllabusDiffResponse,
  SyllabusDiffResponse,
} from "@/types/syllabusDiff"

const normalizeSemesterLabel = (value: unknown) => {
  const text = String(value ?? "").trim()
  if (!text) return ""
  if (/^summer(?:\s+semester)?$/i.test(text)) return "Summer Semester"
  const match = text.match(/^(?:(?:semester|hk)\s*)?([1-8])$/i)
  return match ? `Semester ${match[1]}` : text
}

const withoutCreatedBy = (
  data: CreateSyllabusRequest
): CreateSyllabusRequest => {
  const safeData = { ...data }
  delete safeData.createdBy
  return safeData
}


const normalizeSyllabusCreatePayload = (
  data: CreateSyllabusRequest
): CreateSyllabusRequest => {
  const safeData = withoutCreatedBy(data)

  return {
    ...safeData,
    courseId: Number(data.courseId),
    versionNumber: Number(data.versionNumber || 1),
    versionLabel: formatVersionLabel(Number(data.versionNumber || 1), data.versionLabel),
    academicYear: data.academicYear?.trim() || "",
    semester: normalizeSemesterLabel(data.semester),
    changeSummary: data.changeSummary ?? "",
    notes: data.notes ?? "",
    clos: data.clos ?? [],
    topics: data.topics ?? [],
    assessments: data.assessments ?? [],
  }
}

const normalizeSyllabusUpdatePayload = (
  data: CreateSyllabusRequest
): CreateSyllabusRequest => {
  const safeData = withoutCreatedBy(data)
  const payload: CreateSyllabusRequest = {
    ...safeData,
    courseId: Number(data.courseId),
  }

  if (data.versionNumber !== undefined && data.versionNumber !== null) {
    payload.versionNumber = Number(data.versionNumber)
  }

  if (data.semester !== undefined && data.semester !== null) {
    payload.semester = normalizeSemesterLabel(data.semester)
  }

  // Quan trọng: không tự thêm clos/topics/assessments = [] khi update.
  // Nếu thêm mảng rỗng, backend sẽ hiểu là xóa toàn bộ collection.
  if (data.clos === undefined) delete payload.clos
  if (data.topics === undefined) delete payload.topics
  if (data.assessments === undefined) delete payload.assessments

  return payload
}

export const syllabusApi = {
  getAll: async (): Promise<Syllabus[]> => {
    const response = await api.get("/api/syllabuses")
    return response.data
  },

  getCreateContext: async (courseId: number): Promise<SyllabusCreateContextResponse> => {
    const response = await api.get(`/api/syllabuses/create-context`, { params: { courseId } })
    return response.data
  },

  getById: async (id: number): Promise<Syllabus> => {
    const response = await api.get(`/api/syllabuses/${id}`)
    return response.data
  },

  getForEdit: async (id: number): Promise<Syllabus> => {
    const response = await api.get(`/api/syllabuses/${id}`, {
      params: { forEdit: true },
    })
    return response.data
  },
getPreviousComparable:
  async (
    id: number,
  ): Promise<Syllabus | null> => {
    const response =
      await api.get(
        `/api/syllabuses/${id}/previous-comparable`,
      )

    if (response.status === 204) {
      return null
    }

    return response.data
  },
  getByCourse: async (courseId: number): Promise<Syllabus[]> => {
    const response = await api.get(`/api/syllabuses/course/${courseId}`)
    return response.data
  },

  getByStatus: async (status: string): Promise<Syllabus[]> => {
    const response = await api.get(`/api/syllabuses/status/${status}`)
    return response.data
  },

  create: async (
    data: CreateSyllabusRequest
  ): Promise<Syllabus> => {
    const payload = normalizeSyllabusCreatePayload(data)
    const response = await api.post("/api/syllabuses", payload)
    return response.data
  },

  update: async (
    id: number,
    data: CreateSyllabusRequest
  ): Promise<Syllabus> => {
    const payload = normalizeSyllabusUpdatePayload(data)
    const response = await api.put(`/api/syllabuses/${id}`, payload)
    return response.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/syllabuses/${id}`)
  },
  deleteCohortForReimport: async (
  programId: number,
  cohortId: number,
  confirmCohort: string,
): Promise<number> => {
  const response = await api.delete<{
    deletedCount: number
  }>("/api/syllabuses/maintenance/cohort", {
    params: {
      programId,
      cohortId,
      confirmCohort,
    },
  })

  return response.data.deletedCount
},

  validateForSubmit: async (id: number): Promise<SubmissionValidationResponse> => {
    const response = await api.get(`/api/syllabuses/${id}/submission-validation`)
    return response.data
  },

  submit: async (id: number): Promise<Syllabus> => {
    const response = await api.post(`/api/syllabuses/${id}/submit`)
    return response.data
  },

  getDiff: async (
    id: number,
    compareWith: number
  ): Promise<SyllabusDiffResponse> => {
    const response = await api.get(`/api/syllabuses/${id}/diff`, {
      params: { compareWith },
    })
    return response.data
  },

  getSemanticDiff: async (
  id: number,
  compareWith: number
): Promise<SemanticSyllabusDiffResponse> => {
  const response =
    await api.post<SemanticSyllabusDiffResponse>(
      `/api/syllabuses/${id}/diff/semantic`,
      null,
      {
        params: {
          compareWith,
        },
      }
    )

  return response.data
},
  clone: async (
    id: number,
    request: CloneSyllabusRequest
  ): Promise<Syllabus> => {
    const response = await api.post(
      `/api/syllabuses/${id}/clone`,
      request
    )
    return response.data
  },
}
