import { create } from "zustand"
import { persist } from "zustand/middleware"
import type { AuthUser, LoginResponse } from "@/types/auth"

interface AuthState {
  accessToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  setAuth: (data: LoginResponse) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      isAuthenticated: false,

      setAuth: (data) =>
        set({
          accessToken: data.accessToken,
          isAuthenticated: true,
          user: {
            userId: data.userId,
            username: data.username,
            email: data.email,
            role: data.role,
            instructorId: data.instructorId,
          },
        }),

      logout: () =>
        set({
          accessToken: null,
          user: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: "auth-storage", // key trong localStorage
    }
  )
)