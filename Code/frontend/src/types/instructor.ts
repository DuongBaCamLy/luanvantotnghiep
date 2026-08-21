import type { UserRole } from "./auth"

export interface Instructor {
  id: number
  staffCode: string
  fullName: string
  email: string
  degree: string
  academicRank: string
  departmentId: number
  departmentCode: string
  departmentName: string
  departmentNameVn: string
  isActive: boolean
  createdAt: string
  role?: UserRole
  courseCount?: number
}

export interface InstructorRequest {
  staffCode: string
  fullName: string
  email: string
  degree: string
  academicRank: string
  departmentId: number
  isActive: boolean
  role?: UserRole
}
