export type CourseLevel =
  | "UNDERGRADUATE"
  | "GRADUATE"

export interface Course {
  id: number
  courseCode: string
  name: string
  nameVn: string
  departmentId: number
  departmentCode: string
  departmentName: string
  creditTheory: number
  creditLab: number
  totalCredits: number
  courseLevel: CourseLevel
  description: string | null
  isActive: boolean
}

export interface CreateCourseRequest {
  courseCode: string
  name: string
  nameVn: string
  departmentId: number
  creditTheory: number
  creditLab: number
  courseLevel: CourseLevel
  description?: string
}