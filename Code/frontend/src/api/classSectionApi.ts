import { api } from "./axios"

export interface ClassSectionResponse {
  id: number
  courseId: number
  courseCode: string
  courseName: string

  programId: number | null
  programCode: string | null
  programName: string | null

  cohortId: number | null
  cohortName: string | null

  syllabusId: number | null
  syllabusVersionNumber: number | null
  syllabusStatus: string | null

  instructorUserId: number | null
  instructorFullName: string | null
  instructorUsername: string | null

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
  programId: number
  cohortId: number

  /**
   * New Admin assignments should normally use null.
   * The backend later links the Draft created by the assigned Instructor.
   * Existing linked assignments preserve their syllabusId on edit.
   */
  syllabusId?: number | null

  instructorUserId: number

  semester?: number
  academicYear?: string
  groupNumber?: number
  labGroup?: number
  maxStudents?: number
  room?: string
  schedule?: string

  sectionType?:
    | "THEORY"
    | "LAB"
    | "COMBINED"

  isActive?: boolean
}

export const getClassSections =
  async (): Promise<ClassSectionResponse[]> => {
    const { data } =
      await api.get<ClassSectionResponse[]>(
        "/api/class-sections",
      )

    return data
  }

export const getMyActiveAssignments =
  async (): Promise<ClassSectionResponse[]> => {
    const { data } =
      await api.get<ClassSectionResponse[]>(
        "/api/class-sections/my-assignments",
      )

    return data
  }

export const getClassSectionById =
  async (
    id: number,
  ): Promise<ClassSectionResponse> => {
    const { data } =
      await api.get<ClassSectionResponse>(
        `/api/class-sections/${id}`,
      )

    return data
  }

export const getSectionsByCourse =
  async (
    courseId: number,
  ): Promise<ClassSectionResponse[]> => {
    const { data } =
      await api.get<ClassSectionResponse[]>(
        `/api/class-sections/course/${courseId}`,
      )

    return data
  }

export const createClassSection =
  async (
    request: CreateClassSectionRequest,
  ): Promise<ClassSectionResponse> => {
    const { data } =
      await api.post<ClassSectionResponse>(
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
    req: CreateClassSectionRequest
  }): Promise<ClassSectionResponse> => {
    const { data } =
      await api.put<ClassSectionResponse>(
        `/api/class-sections/${id}`,
        req,
      )

    return data
  }

/**
 * Legacy hard-delete endpoint.
 *
 * Keep this client method only while the Admin UI still supports
 * explicit deletion of an unreferenced assignment.
 */
export const deleteClassSection =
  async (
    id: number,
  ): Promise<void> => {
    await api.delete(
      `/api/class-sections/${id}`,
    )
  }