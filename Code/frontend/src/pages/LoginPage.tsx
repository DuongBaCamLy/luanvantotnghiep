import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import { useMutation } from "@tanstack/react-query"
import axios from "axios"
import {
  AlertCircle,
  Eye,
  EyeOff,
  Headphones,
  LockKeyhole,
  Mail,
} from "lucide-react"

import { googleLoginRequest, loginRequest } from "@/api/authApi"
import { useAuthStore } from "@/store/authStore"
import type { UserRole } from "@/types/auth"
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { t } from "@/i18n"
import logoImg from "@/assets/logo.jpg"
import loginIllustration from "@/assets/login-illustration.png"

interface GoogleIdentityService {
  initialize(config: {
    client_id: string
    callback: (response: { credential?: string }) => void
  }): void
  renderButton(
    target: HTMLElement,
    options: {
      theme: string
      size: string
      width: number
      text: string
      shape: string
    },
  ): void
}

declare global {
  interface Window {
    google?: {
      accounts?: {
        id?: GoogleIdentityService
      }
    }
  }
}

function getAuthErrorMessage(error: unknown): string | null {
  if (!error) return null

  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === "string" && message.trim()) {
      return message
    }
  }

  return t("auth.failed")
}

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as
  | string
  | undefined

// Optional. If your university has an SSO URL, add it to .env as:
// VITE_SSO_URL=https://your-sso-url
const SSO_URL = import.meta.env.VITE_SSO_URL as string | undefined

const ROLE_HOME: Record<UserRole, string> = {
  ADMIN: "/admin",
  DEAN: "/dean",
  DEPT_HEAD: "/dept-head",
  INSTRUCTOR: "/instructor",
}

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)

  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)

  const mutation = useMutation({
    mutationFn: loginRequest,
    onSuccess: (data) => {
      setAuth(data)
      navigate(ROLE_HOME[data.role] ?? "/")
    },
  })

  const googleMutation = useMutation({
    mutationFn: googleLoginRequest,
    onSuccess: (data) => {
      setAuth(data)
      navigate(ROLE_HOME[data.role] ?? "/")
    },
  })

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) return

    const renderGoogleButton = () => {
      const target = document.getElementById("google-signin-button")
      if (!target || !window.google?.accounts?.id) return

      target.innerHTML = ""
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (response: { credential?: string }) => {
          if (response.credential) {
            googleMutation.mutate({ idToken: response.credential })
          }
        },
      })

      window.google.accounts.id.renderButton(target, {
        theme: "outline",
        size: "large",
        width: 360,
        text: "signin_with",
        shape: "rectangular",
      })
    }

    if (window.google?.accounts?.id) {
      renderGoogleButton()
      return
    }

    const existingScript = document.querySelector<HTMLScriptElement>(
      'script[src="https://accounts.google.com/gsi/client"]',
    )

    if (existingScript) {
      existingScript.addEventListener("load", renderGoogleButton, { once: true })
      return
    }

    const script = document.createElement("script")
    script.src = "https://accounts.google.com/gsi/client"
    script.async = true
    script.defer = true
    script.onload = renderGoogleButton
    document.body.appendChild(script)

    return () => {
      script.onload = null
    }
  }, [googleMutation])

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({ username, password })
  }

  const errorMessage =
    getAuthErrorMessage(mutation.error) ??
    getAuthErrorMessage(googleMutation.error)

  return (
    <div className="min-h-screen bg-[linear-gradient(115deg,#e7b54f_0%,#c7ba5e_35%,#7eae79_67%,#2e9ca0_100%)] px-4 py-6 font-sans md:px-6">
      <div className="mx-auto flex min-h-[calc(100vh-48px)] w-full max-w-[960px] flex-col justify-center">
        <div className="overflow-hidden rounded-[2px] bg-white shadow-[0_18px_55px_rgba(45,72,65,0.24)] md:flex md:min-h-[738px]">
          {/* LEFT ILLUSTRATION */}
          <section className="hidden w-[53.5%] items-center justify-center bg-[#eef5f7] px-[22px] md:flex">
            <img
              src={loginIllustration}
              alt="Curriculum management illustration"
              className="w-full max-w-[468px] object-contain"
            />
          </section>

          {/* RIGHT LOGIN */}
          <section className="flex w-full flex-col bg-white md:w-[46.5%]">
            <div className="flex flex-1 flex-col px-8 pb-8 pt-9 sm:px-10 md:px-[42px]">
              <div className="text-center">
                <img
                  src={logoImg}
                  alt="International University - VNU-HCM"
                  className="mx-auto h-[90px] w-[90px] object-cover"
                />

                <h1 className="mt-5 text-[17px] font-extrabold uppercase tracking-[0.25px] text-[#006b72]">
                  {t("app.name")}
                </h1>
              </div>

              <div className="mt-8 text-center">
                <h2 className="text-[19px] font-bold text-[#111827]">Sign in</h2>
              </div>

              <form onSubmit={handleSubmit} className="mt-6 space-y-3">
                <div className="relative">
                  <Mail className="pointer-events-none absolute left-4 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-[#768791]" />
                  <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                    placeholder="Email or username"
                    autoComplete="username"
                    autoFocus
                    required
                    className="h-[42px] w-full rounded-full border-0 bg-[#edf2f4] pl-11 pr-4 text-[13px] text-slate-700 outline-none transition placeholder:text-[#64748b] focus:bg-[#e9f0f2] focus:ring-2 focus:ring-[#0b7b82]/25"
                  />
                </div>

                <div className="relative">
                  <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-[#768791]" />
                  <input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Password"
                    autoComplete="current-password"
                    required
                    className="h-[42px] w-full rounded-full border-0 bg-[#edf2f4] pl-11 pr-12 text-[13px] text-slate-700 outline-none transition placeholder:text-[#64748b] focus:bg-[#e9f0f2] focus:ring-2 focus:ring-[#0b7b82]/25"
                  />

                  <button
                    type="button"
                    onClick={() => setShowPassword((current) => !current)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-[#768791] transition hover:text-[#006b72]"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? (
                      <EyeOff className="h-[17px] w-[17px]" />
                    ) : (
                      <Eye className="h-[17px] w-[17px]" />
                    )}
                  </button>
                </div>

                <div className="flex items-center justify-between pt-1 text-[11px]">
                  <label className="flex cursor-pointer items-center gap-2 text-[#243746]">
                    <input
                      type="checkbox"
                      className="h-[13px] w-[13px] cursor-pointer accent-[#006b72]"
                    />
                    <span>Remember me</span>
                  </label>

                  <button
                    type="button"
                    onClick={() => navigate("/forgot-password")}
                    className="font-semibold text-[#006b72] hover:underline"
                  >
                    Forgot password?
                  </button>
                </div>

                {errorMessage && (
                  <Alert
                    variant="destructive"
                    className="border-red-200 bg-red-50 py-2 text-red-800"
                  >
                    <AlertCircle className="h-4 w-4" color="#991b1b" />
                    <AlertTitle className="text-xs font-semibold text-red-800">
                      {t("auth.failedTitle")}
                    </AlertTitle>
                    <AlertDescription className="text-xs text-red-700">
                      {errorMessage}
                    </AlertDescription>
                  </Alert>
                )}

                <button
                  type="submit"
                  disabled={mutation.isPending}
                  className="mt-1 h-[42px] w-full bg-[#f0aa39] text-[13px] font-semibold text-white transition hover:bg-[#df9726] focus:outline-none focus:ring-2 focus:ring-[#f0aa39]/40 disabled:cursor-not-allowed disabled:opacity-65"
                >
                  {mutation.isPending ? "Signing in..." : "Sign in"}
                </button>

                <div className="flex items-center gap-3 py-2">
                  <span className="h-px flex-1 bg-[#d7e0e5]" />
                  <span className="text-[10px] font-medium uppercase text-[#60727d]">
                    or
                  </span>
                  <span className="h-px flex-1 bg-[#d7e0e5]" />
                </div>

                {GOOGLE_CLIENT_ID ? (
                  <div
                    id="google-signin-button"
                    className="flex min-h-[42px] w-full items-center justify-center overflow-hidden"
                  />
                ) : (
                  <button
                    type="button"
                    disabled
                    title="Configure VITE_GOOGLE_CLIENT_ID to enable Google sign-in"
                    className="flex h-[42px] w-full items-center justify-center gap-2 border border-[#d7e0e5] bg-white text-[12px] font-semibold text-[#22313b]"
                  >
                    <svg viewBox="0 0 24 24" className="h-[17px] w-[17px]" aria-hidden="true">
                      <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.23-.2-1.77H12v3.4h5.52a4.7 4.7 0 0 1-2.05 3.08l-.03.11 2.98 2.3.21.02c1.94-1.79 3.06-4.43 3.06-7.14Z"/>
                      <path fill="#34A853" d="M12 22c2.7 0 4.97-.89 6.63-2.43l-3.16-2.43c-.85.58-1.96.98-3.47.98-2.59 0-4.79-1.75-5.58-4.17l-.1.01-3.1 2.4-.04.09A10 10 0 0 0 12 22Z"/>
                      <path fill="#FBBC05" d="M6.42 13.95A6 6 0 0 1 6.1 12c0-.68.12-1.33.31-1.95l-.01-.13-3.14-2.44-.1.05A10 10 0 0 0 2 12c0 1.61.38 3.13 1.05 4.47l3.37-2.52Z"/>
                      <path fill="#EA4335" d="M12 5.88c1.88 0 3.15.81 3.88 1.49l2.82-2.76C16.97 3 14.7 2 12 2a10 10 0 0 0-8.84 5.53l3.26 2.52C7.22 7.63 9.42 5.88 12 5.88Z"/>
                    </svg>
                    Sign in with Google
                  </button>
                )}

                {SSO_URL ? (
                  <a
                    href={SSO_URL}
                    className="flex h-[42px] w-full items-center justify-center bg-[#006b72] text-[13px] font-semibold text-white transition hover:bg-[#005960] focus:outline-none focus:ring-2 focus:ring-[#006b72]/30"
                  >
                    Sign in via SSO
                  </a>
                ) : (
                  <button
                    type="button"
                    disabled
                    title="Configure VITE_SSO_URL to enable SSO"
                    className="flex h-[42px] w-full items-center justify-center bg-[#006b72] text-[13px] font-semibold text-white"
                  >
                    Sign in via SSO
                  </button>
                )}

                <p className="pt-1 text-center text-[9px] text-[#71818a]">
                  Use your school account to continue.
                </p>
              </form>
            </div>

            <div className="flex h-[94px] items-center justify-center gap-4 bg-[#eef4f6] px-5">
              <Headphones className="h-8 w-8 stroke-[1.7] text-[#006b72]" />
              <div>
                <div className="text-[10px] font-extrabold uppercase tracking-[0.6px] text-[#006b72]">
                  Help &amp; Support
                </div>
                <div className="mt-[1px] text-[16px] font-semibold leading-none text-[#f0aa39]">
                  *************
                </div>
                <div className="mt-[5px] text-[10px] text-[#71818a]">
                  Extension 2 (toll-free)
                </div>
              </div>
            </div>
          </section>
        </div>

        <p className="mt-6 text-center text-[12px] font-semibold uppercase text-white/95">
          International University – VNU-HCM · School of Computer Science and Engineering
        </p>
      </div>
    </div>
  )
}
