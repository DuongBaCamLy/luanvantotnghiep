import { api } from "./axios"
import type { RelationType } from "./courseRelationshipApi"
import type { SyllabusFilter } from "@/lib/syllabusCatalogFilters"

export interface CurriculumMapCourse {
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null
  creditTheory?: number | null
  creditLab?: number | null
  semester: string
  academicYear?: string | null
  major?: string | null
  courseTypes?: string | null
  syllabusVersion?: string | null
}

export interface CurriculumMapSemester {
  semester: string
  courses: CurriculumMapCourse[]
}

export interface CurriculumMapRelation {
  fromCourseId: number
  fromCourseCode: string
  fromCourseName: string
  toCourseId: number
  toCourseCode: string
  toCourseName: string
  relationType: RelationType
}

export interface CurriculumMapResponse {
  programId: number
  programCode: string
  majorId?: number | null
  majorCode?: string | null
  cohortId?: number | null
  cohortName?: string | null
  academicYear?: string | null
  major?: string | null
  semesters: CurriculumMapSemester[]
  relations: CurriculumMapRelation[]
}

export const curriculumMapApi = {
  get: async (filter: SyllabusFilter): Promise<CurriculumMapResponse> => {
    const response = await api.get<CurriculumMapResponse>("/api/curriculum-map", {
      params: {
        programId: filter.programId,
        cohortId: filter.cohortId,
        semester: filter.semester,
        status: filter.status,
      },
    })
    return response.data
  },
}
