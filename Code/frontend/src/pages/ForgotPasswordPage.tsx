import { useState } from "react"
import { useMutation } from "@tanstack/react-query"
import axios from "axios"
import { ArrowLeft, CheckCircle2, Mail } from "lucide-react"
import { useNavigate } from "react-router-dom"

import { forgotPasswordRequest } from "@/api/authApi"
import logoImg from "@/assets/logo.jpg"
import loginIllustration from "@/assets/login-illustration.png"
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"

function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    if (typeof message === "string" && message.trim()) {
      return message
    }
  }
  return "Unable to process the request. Please try again."
}

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState("")

  const mutation = useMutation({
    mutationFn: forgotPasswordRequest,
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    mutation.mutate({ email: email.trim() })
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(115deg,#e7b54f_0%,#c7ba5e_35%,#7eae79_67%,#2e9ca0_100%)] px-4 py-6 font-sans md:px-6">
      <div className="mx-auto flex min-h-[calc(100vh-48px)] w-full max-w-[960px] flex-col justify-center">
        <div className="overflow-hidden rounded-[2px] bg-white shadow-[0_18px_55px_rgba(45,72,65,0.24)] md:flex md:min-h-[650px]">
          <section className="hidden w-[53.5%] items-center justify-center bg-[#eef5f7] px-[22px] md:flex">
            <img
              src={loginIllustration}
              alt="Curriculum management illustration"
              className="w-full max-w-[468px] object-contain"
            />
          </section>

          <section className="flex w-full flex-col bg-white md:w-[46.5%]">
            <div className="flex flex-1 flex-col justify-center px-8 py-10 sm:px-10 md:px-[42px]">
              <div className="text-center">
                <img
                  src={logoImg}
                  alt="International University - VNU-HCM"
                  className="mx-auto h-[90px] w-[90px] object-cover"
                />
                <h1 className="mt-5 text-[17px] font-extrabold uppercase tracking-[0.25px] text-[#006b72]">
                  Curriculum Management System
                </h1>
              </div>

              <div className="mt-9 text-center">
                <h2 className="text-[20px] font-bold text-[#111827]">
                  Forgot password?
                </h2>
                <p className="mx-auto mt-2 max-w-[310px] text-[12px] leading-5 text-[#64748b]">
                  Enter your registered email address. We will send you a secure link to reset your password.
                </p>
              </div>

              {mutation.isSuccess ? (
                <div className="mt-7 space-y-5">
                  <Alert className="border-emerald-200 bg-emerald-50 text-emerald-900">
                    <CheckCircle2 className="h-4 w-4 text-emerald-700" />
                    <AlertTitle className="text-sm font-semibold">
                      Check your email
                    </AlertTitle>
                    <AlertDescription className="text-xs leading-5 text-emerald-800">
                      {mutation.data.message}
                    </AlertDescription>
                  </Alert>

                  <button
                    type="button"
                    onClick={() => navigate("/login")}
                    className="flex h-[42px] w-full items-center justify-center gap-2 bg-[#006b72] text-[13px] font-semibold text-white transition hover:bg-[#005960]"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back to Sign in
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="mt-7 space-y-4">
                  <div className="relative">
                    <Mail className="pointer-events-none absolute left-4 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-[#768791]" />
                    <input
                      id="forgot-email"
                      type="email"
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      placeholder="Your registered email"
                      autoComplete="email"
                      autoFocus
                      required
                      className="h-[42px] w-full rounded-full border-0 bg-[#edf2f4] pl-11 pr-4 text-[13px] text-slate-700 outline-none transition placeholder:text-[#64748b] focus:bg-[#e9f0f2] focus:ring-2 focus:ring-[#0b7b82]/25"
                    />
                  </div>

                  {mutation.isError && (
                    <Alert variant="destructive" className="border-red-200 bg-red-50 py-2">
                      <AlertDescription className="text-xs text-red-700">
                        {getApiErrorMessage(mutation.error)}
                      </AlertDescription>
                    </Alert>
                  )}

                  <button
                    type="submit"
                    disabled={mutation.isPending}
                    className="h-[42px] w-full bg-[#f0aa39] text-[13px] font-semibold text-white transition hover:bg-[#df9726] focus:outline-none focus:ring-2 focus:ring-[#f0aa39]/40 disabled:cursor-not-allowed disabled:opacity-65"
                  >
                    {mutation.isPending ? "Sending..." : "Send Reset Link"}
                  </button>

                  <button
                    type="button"
                    onClick={() => navigate("/login")}
                    className="flex h-10 w-full items-center justify-center gap-2 text-[12px] font-semibold text-[#006b72] hover:underline"
                  >
                    <ArrowLeft className="h-4 w-4" />
                    Back to Sign in
                  </button>
                </form>
              )}
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
