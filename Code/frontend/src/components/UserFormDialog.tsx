import { useEffect, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  Eye,
  EyeOff,
  Loader2,
  ShieldCheck,
  UserRound,
} from "lucide-react"

import {
  createUser,
  updateUser,
} from "@/api/userApi"
import { programApi } from "@/api/programApi"
import {
  Alert,
  AlertDescription,
} from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { UserRole } from "@/types/auth"
import type { UserAccountResponse } from "@/types/user"

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  editingUser: UserAccountResponse | null
}

const NONE = "__none__"

const SRS_ROLES: UserRole[] = [
  "ADMIN",
  "DEAN",
  "DEAN_SECRETARY",
  "DEPT_HEAD",
  "INSTRUCTOR",
]

const ASSIGNABLE_ROLES: UserRole[] = [
  "DEAN",
  "DEAN_SECRETARY",
  "DEPT_HEAD",
  "INSTRUCTOR",
]

const ROLE_LABELS: Partial<Record<UserRole, string>> = {
  ADMIN: "Administrator",
  DEAN: "Dean",
  DEAN_SECRETARY: "Dean Secretary",
  DEPT_HEAD: "Head of Department",
  INSTRUCTOR: "Instructor",
}

const getRoleLabel = (role: UserRole) =>
  ROLE_LABELS[role] ?? role

const isValidEmail = (value: string) =>
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)

function getErrorMessage(
  error: unknown,
): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: string
            error?: string
          }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }

    if (response?.data?.error) {
      return response.data.error
    }
  }

  if (error instanceof Error) {
    return error.message
  }

  return "Unable to save the user account."
}

export default function UserFormDialog({
  open,
  onOpenChange,
  editingUser,
}: Props) {
  const queryClient = useQueryClient()
  const isEditMode = Boolean(editingUser)

  const roleOptions: UserRole[] =
    editingUser?.role === "ADMIN"
      ? ["ADMIN"]
      : ASSIGNABLE_ROLES

  const [fullName, setFullName] = useState("")
  const [username, setUsername] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [role, setRole] =
    useState<UserRole>("INSTRUCTOR")
  const [managedMajorId, setManagedMajorId] =
    useState("")
  const [showPassword, setShowPassword] =
    useState(false)
  const [validationError, setValidationError] =
    useState("")

  const {
    data: majors = [],
    isLoading: majorsLoading,
  } = useQuery({
    queryKey: ["majors"],
    queryFn: programApi.getMajors,
    enabled: open,
  })

  useEffect(() => {
    if (!open) {
      return
    }

    if (editingUser) {
      setFullName(editingUser.fullName ?? "")
      setUsername(editingUser.username)
      setEmail(editingUser.email)
      setPassword("")
      setRole(
        SRS_ROLES.includes(editingUser.role)
          ? editingUser.role
          : "INSTRUCTOR",
      )
      setManagedMajorId(
        editingUser.managedMajorId
          ? String(editingUser.managedMajorId)
          : "",
      )
    } else {
      setFullName("")
      setUsername("")
      setEmail("")
      setPassword("")
      setRole("INSTRUCTOR")
      setManagedMajorId("")
    }

    setShowPassword(false)
    setValidationError("")
  }, [open, editingUser])

  const mutation = useMutation({
    mutationFn: async () => {
      const normalizedFullName =
        fullName.trim()
      const normalizedUsername =
        username.trim()
      const normalizedEmail =
        email.trim()

      const managedMajorIdValue =
        role === "DEPT_HEAD"
        && managedMajorId
          ? Number(managedMajorId)
          : null

      if (isEditMode) {
        return updateUser(
          editingUser!.id,
          {
            fullName: normalizedFullName,
            username: normalizedUsername,
            email: normalizedEmail,
            password:
              password
                ? password
                : undefined,
            role,


            managedMajorId:
              managedMajorIdValue,
          },
        )
      }

      return createUser({
        fullName: normalizedFullName,
        username: normalizedUsername,
        email: normalizedEmail,
        password,
        role,


        managedMajorId:
          managedMajorIdValue,
      })
    },

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["users"],
      })

      onOpenChange(false)
    },
  })

  const handleRoleChange = (
    value: string,
  ) => {
    const nextRole =
      value as UserRole

    setRole(nextRole)
    setValidationError("")

    if (nextRole !== "DEPT_HEAD") {
      setManagedMajorId("")
    }
  }

  const validate = () => {
    const normalizedFullName =
      fullName.trim()
    const normalizedUsername =
      username.trim()
    const normalizedEmail =
      email.trim()

    if (!normalizedFullName) {
      return "Full name is required."
    }

    if (normalizedFullName.length > 255) {
      return "Full name must not exceed 255 characters."
    }

    if (
      normalizedUsername.length < 3
    ) {
      return "Username must contain at least 3 characters."
    }

    if (
      !isValidEmail(normalizedEmail)
    ) {
      return "Please enter a valid email address."
    }

    if (
      !isEditMode
      && password.length < 8
    ) {
      return "Password must contain at least 8 characters."
    }

    if (
      isEditMode
      && password
      && password.length < 8
    ) {
      return "A new password must contain at least 8 characters."
    }

    if (
      role === "DEPT_HEAD"
      && !managedMajorId
    ) {
      return "Managed Major is required for a Head of Department account."
    }

    return ""
  }

  const handleSubmit = (
    event: React.FormEvent,
  ) => {
    event.preventDefault()

    const error = validate()

    if (error) {
      setValidationError(error)
      return
    }

    setValidationError("")
    mutation.mutate()
  }

  const errorMessage =
    validationError
    || (
      mutation.isError
        ? getErrorMessage(
            mutation.error,
          )
        : ""
    )

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!mutation.isPending) {
          onOpenChange(nextOpen)
        }
      }}
    >
      <DialogContent className="overflow-hidden p-0 sm:max-w-[620px]">
        <div className="border-b border-[#d7e5e8] bg-[#f7fbfb] px-6 py-5">
          <DialogHeader className="text-left">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-xl bg-[#e7f5f5] text-[#007d84]">
                <ShieldCheck className="size-5" />
              </span>

              <div>
                <DialogTitle className="text-xl text-[#17343d]">
                  {isEditMode
                    ? "Edit User Account"
                    : "Create New User"}
                </DialogTitle>

                <DialogDescription className="mt-1">
                  {isEditMode
                    ? "Update account identity, SRS role, managed major, or set a new password."
                    : "Create a Dean, Head of Department, or Instructor account. The Administrator account is unique and cannot be created here."}
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="space-y-5 px-6 py-5">
            <div className="space-y-1.5">
              <Label htmlFor="uf-full-name">
                Full Name *
              </Label>

              <Input
                id="uf-full-name"
                value={fullName}
                onChange={(event) => {
                  setFullName(
                    event.target.value,
                  )
                  setValidationError("")
                }}
                autoComplete="name"
                placeholder="e.g. Le Thanh Son"
                disabled={mutation.isPending}
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="uf-username">
                  Username *
                </Label>

                <Input
                  id="uf-username"
                  value={username}
                  onChange={(event) => {
                    setUsername(
                      event.target.value,
                    )
                    setValidationError("")
                  }}
                  autoComplete="off"
                  placeholder="e.g. instructor01"
                  disabled={mutation.isPending}
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="uf-email">
                  Email *
                </Label>

                <Input
                  id="uf-email"
                  type="email"
                  value={email}
                  onChange={(event) => {
                    setEmail(
                      event.target.value,
                    )
                    setValidationError("")
                  }}
                  autoComplete="off"
                  placeholder="name@hcmiu.edu.vn"
                  disabled={mutation.isPending}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="uf-password">
                {isEditMode
                  ? "New Password (optional)"
                  : "Password *"}
              </Label>

              <div className="relative">
                <Input
                  id="uf-password"
                  type={
                    showPassword
                      ? "text"
                      : "password"
                  }
                  value={password}
                  onChange={(event) => {
                    setPassword(
                      event.target.value,
                    )
                    setValidationError("")
                  }}
                  autoComplete="new-password"
                  placeholder={
                    isEditMode
                      ? "Leave blank to keep the current password"
                      : "Minimum 8 characters"
                  }
                  className="pr-10"
                  disabled={mutation.isPending}
                />

                <button
                  type="button"
                  aria-label={
                    showPassword
                      ? "Hide password"
                      : "Show password"
                  }
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700"
                  onClick={() =>
                    setShowPassword(
                      (value) => !value,
                    )
                  }
                >
                  {showPassword ? (
                    <EyeOff className="size-4" />
                  ) : (
                    <Eye className="size-4" />
                  )}
                </button>
              </div>

              {isEditMode && (
                <p className="text-xs text-slate-500">
                  Leave this field blank unless the administrator needs to reset the user's password.
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label>
                SRS Role *
              </Label>

              <Select
                value={role}
                onValueChange={
                  handleRoleChange
                }
                disabled={
                  mutation.isPending
                  || editingUser?.role
                    === "ADMIN"
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  {roleOptions.map(
                    (item) => (
                      <SelectItem
                        key={item}
                        value={item}
                      >
                        {getRoleLabel(
                          item,
                        )}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>

              <p className="text-xs text-slate-500">
                Program Coordinator and Student are not exposed as system roles in this SRS workflow.
              </p>
            </div>

            {role === "DEPT_HEAD" && (
              <div className="space-y-1.5">
                <Label>
                  Managed Major *
                </Label>

                <Select
                  value={
                    managedMajorId
                    || NONE
                  }
                  onValueChange={(
                    value,
                  ) => {
                    setManagedMajorId(
                      value === NONE
                        ? ""
                        : value,
                    )
                    setValidationError(
                      "",
                    )
                  }}
                  disabled={
                    mutation.isPending
                    || majorsLoading
                  }
                >
                  <SelectTrigger>
                    <SelectValue
                      placeholder={
                        majorsLoading
                          ? "Loading majors..."
                          : "Select a Major..."
                      }
                    />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem
                      value={NONE}
                    >
                      Select a Major...
                    </SelectItem>

                    {majors.map(
                      (major) => (
                        <SelectItem
                          key={
                            major.id
                          }
                          value={String(
                            major.id,
                          )}
                        >
                          {major.code}
                          {" — "}
                          {major.name}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>

                <p className="text-xs text-slate-500">
                  This Major controls syllabus catalog, dashboard, submission routing, and review scope.
                </p>
              </div>
            )}

            <div className="flex gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
              <UserRound className="mt-0.5 size-4 shrink-0" />

              <p>
                {role === "DEPT_HEAD"
                  ? "The Head of Department account is scoped directly by Managed Major. No Instructor Profile link is required."
                  : role === "INSTRUCTOR"
                    ? "Instructor accounts are used directly for teaching assignments. No separate Instructor Profile link is required."
                    : role === "ADMIN"
                      ? "Administrator accounts manage system configuration."
                      : "Dean accounts perform final syllabus review."}
              </p>
            </div>

            {errorMessage && (
              <Alert variant="destructive">
                <AlertDescription>
                  {errorMessage}
                </AlertDescription>
              </Alert>
            )}
          </div>

          <DialogFooter className="border-t bg-slate-50 px-6 py-4">
            <Button
              type="button"
              variant="outline"
              disabled={
                mutation.isPending
              }
              onClick={() =>
                onOpenChange(false)
              }
            >
              Cancel
            </Button>

            <Button
              type="submit"
              className="bg-[#007d84] text-white hover:bg-[#006d73]"
              disabled={
                mutation.isPending
              }
            >
              {mutation.isPending && (
                <Loader2 className="size-4 animate-spin" />
              )}

              {mutation.isPending
                ? "Saving..."
                : isEditMode
                  ? "Save Changes"
                  : "Create User"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
