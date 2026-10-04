export type UserRole =
  | "ADMIN"
  | "DEAN"
  | "DEAN_SECRETARY"
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
}

export interface AuthUser {
  userId: number
  username: string
  email: string
  role: UserRole
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