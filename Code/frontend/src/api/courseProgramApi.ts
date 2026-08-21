import { api } from "./axios"

export type CurriculumTerm =
  | "HK1"
  | "HK2"
  | "HK3"
  | "HK4"
  | "HK5"
  | "HK6"
  | "SUMMER"
  | "HK7"
  | "HK8"
  | "ELECTIVE"

export interface CourseProgramItem {
  id: number
  termCode: CurriculumTerm | null

  courseId: number
  courseCode: string
  courseName: string

  programId: number
  programCode: string
  programName: string

  majorId: number
  majorCode: string
  majorName: string

  cohortId: number | null
  cohortName: string | null

  courseTypeId: number | null
  courseTypeName: string | null

  creditTheory: number | null
  creditLab: number | null
  totalCredits: number

  syllabusId: number | null
  syllabusVersionNumber: number | null
  syllabusVersionLabel: string | null
  syllabusStatus: string | null
  syllabusCurrent: boolean | null
  hasSyllabus: boolean

  semesterSuggest: number | null
  yearSuggest: number | null
  required: boolean
}

export interface CreateCourseProgramRequest {
  termCode?: CurriculumTerm | null
  courseId: number
  programId: number
  cohortId?: number | null
  courseTypeId: number
  semesterSuggest?: number | null
  yearSuggest?: number | null
  required: boolean
}

export interface UpdateCourseProgramRequest {
  termCode?: CurriculumTerm
  semesterSuggest?: number
  courseTypeId?: number
  isRequired?: boolean
}

export const courseProgramApi = {
  getAll: async (): Promise<CourseProgramItem[]> => {
    const res = await api.get<CourseProgramItem[]>(
      "/api/course-programs"
    )
    return res.data
  },

  getCurriculum: async (
    programId: number,
    cohortId: number
  ): Promise<CourseProgramItem[]> => {
    const res = await api.get<CourseProgramItem[]>(
      "/api/course-programs/curriculum",
      {
        params: {
          programId,
          cohortId,
        },
      }
    )

    return res.data
  },

  getByProgram: async (
    programId: number
  ): Promise<CourseProgramItem[]> => {
    const res = await api.get<CourseProgramItem[]>(
      `/api/course-programs/program/${programId}`
    )

    return res.data
  },

  getById: async (
    id: number
  ): Promise<CourseProgramItem> => {
    const res = await api.get<CourseProgramItem>(
      `/api/course-programs/${id}`
    )

    return res.data
  },

  create: async (
    data: CreateCourseProgramRequest
  ): Promise<CourseProgramItem> => {
    const res = await api.post<CourseProgramItem>(
      "/api/course-programs",
      data
    )

    return res.data
  },

  update: async (
    id: number,
    data: UpdateCourseProgramRequest
  ): Promise<CourseProgramItem> => {
    const res = await api.put<CourseProgramItem>(
      `/api/course-programs/${id}`,
      data
    )

    return res.data
  },

  assignSyllabus: async (
    courseProgramId: number,
    syllabusId: number
  ): Promise<CourseProgramItem> => {
    const res = await api.put<CourseProgramItem>(
      `/api/course-programs/${courseProgramId}/syllabus/${syllabusId}`
    )

    return res.data
  },

  delete: async (
    id: number
  ): Promise<void> => {
    await api.delete(
      `/api/course-programs/${id}`
    )
  },
}