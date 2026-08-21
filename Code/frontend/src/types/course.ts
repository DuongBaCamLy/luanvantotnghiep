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
  courseLevel: string
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
  courseLevel: string // e.g. INTRODUCTORY, INTERMEDIATE, ADVANCED
  description?: string
}

