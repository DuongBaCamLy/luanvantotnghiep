export type UserRole =
  | "ADMIN"
  | "DEAN"
  | "DEPT_HEAD"
  | "INSTRUCTOR"

export interface LoginRequest {
  username: string
  password: string
}

export interface GoogleLoginRequest {
  idToken: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  userId: number
  username: string
  email: string
  role: UserRole
  instructorId: number | null
}

export interface AuthUser {
  userId: number
  username: string
  email: string
  role: UserRole
  instructorId: number | null
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface AuthMessageResponse {
  message: string
}
