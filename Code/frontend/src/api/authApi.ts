import { api } from "@/api/axios"

import type {
  AuthMessageResponse,
  ForgotPasswordRequest,
  GoogleLoginRequest,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
} from "@/types/auth"

export async function loginRequest(
  payload: LoginRequest
): Promise<LoginResponse> {

  const { data } =
    await api.post<LoginResponse>(
      "/auth/login",
      payload
    )

  return data
}

export async function googleLoginRequest(
  payload: GoogleLoginRequest
): Promise<LoginResponse> {

  const { data } =
    await api.post<LoginResponse>(
      "/auth/google",
      payload
    )

  return data
}

export async function logoutRequest(): Promise<void> {
  await api.post("/auth/logout")
}
export async function forgotPasswordRequest(
  payload: ForgotPasswordRequest,
): Promise<AuthMessageResponse> {
  const { data } = await api.post<AuthMessageResponse>(
    "/auth/forgot-password",
    payload,
  )

  return data
}

export async function resetPasswordRequest(
  payload: ResetPasswordRequest,
): Promise<AuthMessageResponse> {
  const { data } = await api.post<AuthMessageResponse>(
    "/auth/reset-password",
    payload,
  )

  return data
}
