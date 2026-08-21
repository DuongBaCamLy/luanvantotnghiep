import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from "axios"

import { useAuthStore } from "@/store/authStore"
import type { LoginResponse } from "@/types/auth"

const API_BASE_URL = "http://localhost:8080"

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean
}

export const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
})

let refreshPromise: Promise<LoginResponse> | null = null

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,

  async (error: AxiosError) => {
    const originalRequest =
      error.config as RetryableRequestConfig | undefined

    const status = error.response?.status
    const requestUrl = originalRequest?.url ?? ""
    const isAuthRequest =
      requestUrl.startsWith("/auth/")

    const authState =
      useAuthStore.getState()

    if (
      status !== 401
      || !originalRequest
      || originalRequest._retry
      || isAuthRequest
      || !authState.isAuthenticated
    ) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post<LoginResponse>(
            `${API_BASE_URL}/auth/refresh`,
            {},
            {
              withCredentials: true,
            }
          )
          .then((response) => response.data)
          .finally(() => {
            refreshPromise = null
          })
      }

      const refreshedAuth =
        await refreshPromise

      useAuthStore
        .getState()
        .setAuth(refreshedAuth)

      originalRequest.headers.Authorization =
        `Bearer ${refreshedAuth.accessToken}`

      return api(originalRequest)

    } catch (refreshError) {

      useAuthStore
        .getState()
        .logout()

      window.location.href = "/login"

      return Promise.reject(refreshError)
    }
  }
)