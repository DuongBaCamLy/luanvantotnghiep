import { api } from "./axios"

export interface ClassSectionResponse {
  id: number
  courseId: number
  courseCode: string
  courseName: string
  syllabusId: number | null
  syllabusVersionNumber: number | null
  syllabusStatus: string | null
  instructorId: number
  instructorName: string
  semester: number
  academicYear: string
  groupNumber: number
  labGroup: number | null
  maxStudents: number | null
  room: string | null
  schedule: string | null
  sectionType:
    | "THEORY"
    | "LAB"
    | "COMBINED"
  isActive: boolean
  readyForSyllabusCreation: boolean
}

export interface CreateClassSectionRequest {
  courseId: number

  /**
   * New Admin assignments should normally use null.
   * The backend later links the Draft created by the assigned Faculty.
   * Existing linked assignments preserve their syllabusId on edit.
   */
  syllabusId?: number | null

  instructorId: number
  semester: number
  academicYear: string
  groupNumber: number
  labGroup?: number
  maxStudents?: number
  room?: string
  schedule?: string
  sectionType:
    | "THEORY"
    | "LAB"
    | "COMBINED"
  isActive?: boolean
}

export const getClassSections =
  async (): Promise<
    ClassSectionResponse[]
  > => {
    const { data } =
      await api.get<
        ClassSectionResponse[]
      >(
        "/api/class-sections",
      )

    return data
  }

export const getMyActiveAssignments =
  async (): Promise<
    ClassSectionResponse[]
  > => {
    const { data } =
      await api.get<
        ClassSectionResponse[]
      >(
        "/api/class-sections/my-assignments",
      )

    return data
  }

export const getClassSectionById =
  async (
    id: number,
  ): Promise<
    ClassSectionResponse
  > => {
    const { data } =
      await api.get<
        ClassSectionResponse
      >(
        `/api/class-sections/${id}`,
      )

    return data
  }

export const getSectionsByCourse =
  async (
    courseId: number,
  ): Promise<
    ClassSectionResponse[]
  > => {
    const { data } =
      await api.get<
        ClassSectionResponse[]
      >(
        `/api/class-sections/course/${courseId}`,
      )

    return data
  }

export const createClassSection =
  async (
    request:
      CreateClassSectionRequest,
  ): Promise<
    ClassSectionResponse
  > => {
    const { data } =
      await api.post<
        ClassSectionResponse
      >(
        "/api/class-sections",
        request,
      )

    return data
  }

export const updateClassSection =
  async ({
    id,
    req,
  }: {
    id: number
    req:
      CreateClassSectionRequest
  }): Promise<
    ClassSectionResponse
  > => {
    const { data } =
      await api.put<
        ClassSectionResponse
      >(
        `/api/class-sections/${id}`,
        req,
      )

    return data
  }

/**
 * Legacy hard-delete endpoint.
 *
 * Keep this client method only for compatibility with older code.
 * The Admin Teaching Assignments UI intentionally does NOT expose it,
 * because assignment history should be preserved and normal removal is
 * represented by isActive = false.
 */
export const deleteClassSection =
  async (
    id: number,
  ): Promise<void> => {
    await api.delete(
      `/api/class-sections/${id}`,
    )
  }