import type { UserRole } from "@/types/auth"

export interface UserAccountResponse {
  id: number
  fullName: string | null
  username: string
  email: string
  role: UserRole

  managedMajorId: number | null
  managedMajorCode: string | null
  managedMajorName: string | null

  isActive: boolean
  lastLogin: string | null
  createdAt: string | null
}

export interface CreateUserRequest {
  fullName: string
  username: string
  email: string
  password: string
  role: UserRole
  managedMajorId: number | null
}

export interface UpdateUserRequest {
  fullName: string
  username: string
  email: string
  password?: string
  role: UserRole
  managedMajorId: number | null
  isActive?: boolean
}

export const ALL_ROLES: UserRole[] = [
  "ADMIN",
  "DEAN",
  "DEPT_HEAD",
  "INSTRUCTOR",
  "DEAN_SECRETARY",
]