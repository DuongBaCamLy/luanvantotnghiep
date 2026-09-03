import type { UserRole } from "@/types/auth"

export interface UserAccountResponse {
  id: number
  username: string
  email: string
  role: UserRole
  instructorId: number | null
  managedMajorId: number | null
  managedMajorCode: string | null
  managedMajorName: string | null
  isActive: boolean
  lastLogin: string | null
  createdAt: string | null
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  role: UserRole
  instructorId: number | null
  managedMajorId: number | null
}

export interface UpdateUserRequest {
  username: string
  email: string
  password?: string
  role: UserRole
  instructorId: number | null
  managedMajorId: number | null
  isActive?: boolean
}

export const ALL_ROLES: UserRole[] = [
  "ADMIN",
  "DEAN",
  "DEPT_HEAD",
  "INSTRUCTOR",
]
