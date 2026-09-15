import {
  useMemo,
  useState,
} from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  Eye,
  EyeOff,
  Link2,
  Loader2,
  ShieldCheck,
  UserRound,
} from "lucide-react"

import {
  createUser,
  getUsers,
  updateUser,
} from "@/api/userApi"
import { instructorApi } from "@/api/instructorApi"
import { programApi } from "@/api/programApi"
import { Alert, AlertDescription } from "@/components/ui/alert"
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
import type { Instructor } from "@/types/instructor"
import type { UserAccountResponse } from "@/types/user"

interface Props {
  open: boolean
  onOpenChange:
    (open: boolean) => void
  editingUser:
    UserAccountResponse | null
}

const NONE = "__none__"

const SRS_ROLES: UserRole[] = [
  "ADMIN",
  "DEAN",
  "DEPT_HEAD",
  "INSTRUCTOR",
]

const ASSIGNABLE_ROLES: UserRole[] = [
  "DEAN",
  "DEPT_HEAD",
  "INSTRUCTOR",
]
const ROLE_LABELS: Partial<Record<UserRole, string>> = {
  ADMIN: "Administrator",
  DEAN: "Dean",
  DEPT_HEAD: "Head of Department",
  INSTRUCTOR: "Instructor",
}

const getRoleLabel = (role: UserRole) =>
  ROLE_LABELS[role] ?? role

const roleNeedsInstructorProfile =
  (role: UserRole) =>
    role === "INSTRUCTOR"
    || role === "DEPT_HEAD"

const getInstructorLabel = (
  instructor: Instructor,
) => {
  const parts = [
    instructor.staffCode,
    instructor.fullName,
  ]
    .map((value) => value?.trim())
    .filter(Boolean)

  return (
    parts.join(" — ")
    || `Instructor #${instructor.id}`
  )
}

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
  const [username, setUsername] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [role, setRole] =
    useState<UserRole>("INSTRUCTOR")
  const [instructorId, setInstructorId] =
    useState("")
  const [managedMajorId, setManagedMajorId] = useState("")
  const [showPassword, setShowPassword] =
    useState(false)
  const [validationError, setValidationError] =
    useState("")

  const {
    data: instructors = [],
    isLoading: instructorsLoading,
  } = useQuery({
    queryKey: ["instructors"],
    queryFn: instructorApi.getAll,
    enabled: open,
  })

  const {
    data: users = [],
  } = useQuery({
    queryKey: ["users"],
    queryFn: getUsers,
    enabled: open,
  })
  const { data: majors = [], isLoading: majorsLoading } = useQuery({
    queryKey: ["majors"],
    queryFn: programApi.getMajors,
    enabled: open,
  })

  const [formContext, setFormContext] = useState<{ open: boolean; editingUser: typeof editingUser }>({ open: false, editingUser: null })
  if (formContext.open !== open || formContext.editingUser !== editingUser) {
    setFormContext({ open, editingUser })
    if (open) {

    if (editingUser) {
      setUsername(editingUser.username)
      setEmail(editingUser.email)
      setPassword("")
      setRole(
        SRS_ROLES.includes(editingUser.role)
          ? editingUser.role
          : "INSTRUCTOR",
      )
      setInstructorId(
        editingUser.instructorId
          ? String(editingUser.instructorId)
          : "",
      )
      setManagedMajorId(editingUser.managedMajorId ? String(editingUser.managedMajorId) : "")
    } else {
      setUsername("")
      setEmail("")
      setPassword("")
      setRole("INSTRUCTOR")
      setInstructorId("")
      setManagedMajorId("")
    }

    setShowPassword(false)
    setValidationError("")
    }
  }

  const linkedInstructorIds = useMemo(() => {
    const result = new Set<number>()

    users.forEach((user) => {
      if (
        user.id === editingUser?.id
        || !user.instructorId
      ) {
        return
      }

      result.add(user.instructorId)
    })

    return result
  }, [
    editingUser?.id,
    users,
  ])

  const instructorOptions = useMemo(
    () =>
      instructors
        .filter(
          (instructor) =>
            !linkedInstructorIds.has(instructor.id)
            || instructor.id === editingUser?.instructorId,
        )
        .slice()
        .sort(
          (left, right) =>
            getInstructorLabel(left).localeCompare(
              getInstructorLabel(right),
              "en",
              {
                numeric: true,
              },
            ),
        ),
    [
      editingUser?.instructorId,
      instructors,
      linkedInstructorIds,
    ],
  )

  const mutation = useMutation({
    mutationFn: async () => {
      const profileRequired =
        roleNeedsInstructorProfile(role)

      const instructorIdValue =
        profileRequired && instructorId
          ? Number(instructorId)
          : null

      const normalizedUsername =
        username.trim()
      const normalizedEmail =
        email.trim()

      if (isEditMode) {
        return updateUser(
          editingUser!.id,
          {
            username: normalizedUsername,
            email: normalizedEmail,
            password:
              password
                ? password
                : undefined,
            role,
            instructorId:
              instructorIdValue,
            managedMajorId: role === "DEPT_HEAD" && managedMajorId ? Number(managedMajorId) : null,
          },
        )
      }

      return createUser({
        username: normalizedUsername,
        email: normalizedEmail,
        password,
        role,
        instructorId:
          instructorIdValue,
        managedMajorId: role === "DEPT_HEAD" && managedMajorId ? Number(managedMajorId) : null,
      })
    },

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["users"],
        }),

        queryClient.invalidateQueries({
          queryKey: ["instructors"],
        }),
      ])

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

    if (
      !roleNeedsInstructorProfile(nextRole)
    ) {
      setInstructorId("")
    }
    if (nextRole !== "DEPT_HEAD") setManagedMajorId("")
  }

  const validate = () => {
    const normalizedUsername =
      username.trim()
    const normalizedEmail =
      email.trim()

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
      roleNeedsInstructorProfile(role)
      && !instructorId
    ) {
      return role === "DEPT_HEAD"
        ? "A Head of Department account must be linked to an instructor profile."
        : "An Instructor account must be linked to an instructor profile before it can receive teaching assignments."
    }
    if (role === "DEPT_HEAD" && !managedMajorId) {
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
        ? getErrorMessage(mutation.error)
        : ""
    )

  const needsProfile =
    roleNeedsInstructorProfile(role)

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
                    ? "Update account identity, SRS role, linked staff profile, or set a new password."
                    : "Create a Dean, Head of Department, or Instructor account. The Administrator account is unique and cannot be created here."
                  }
                    </DialogDescription>
              </div>
            </div>
          </DialogHeader>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="space-y-5 px-6 py-5">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="uf-username">
                  Username *
                </Label>

                <Input
                  id="uf-username"
                  value={username}
                  onChange={(event) => {
                    setUsername(event.target.value)
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
                    setEmail(event.target.value)
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
                    setPassword(event.target.value)
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
  onValueChange={handleRoleChange}
  disabled={
    mutation.isPending
    || editingUser?.role === "ADMIN"
  }
>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  {roleOptions.map((item) => (
                    <SelectItem
                      key={item}
                      value={item}
                    >
                      {getRoleLabel(item)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <p className="text-xs text-slate-500">
                Program Coordinator and Student are not exposed as system roles in this SRS workflow.
              </p>
            </div>

            {needsProfile && (
              <div className="space-y-1.5">
                <Label>
                  Linked Instructor Profile *
                </Label>

                <Select
                  value={
                    instructorId || NONE
                  }
                  onValueChange={(value) => {
                    setInstructorId(
                      value === NONE
                        ? ""
                        : value,
                    )
                    setValidationError("")
                  }}
                  disabled={
                    mutation.isPending
                    || instructorsLoading
                  }
                >
                  <SelectTrigger>
                    <SelectValue
                      placeholder={
                        instructorsLoading
                          ? "Loading instructor profiles..."
                          : "Select an instructor profile..."
                      }
                    />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={NONE}>
                      Select a profile...
                    </SelectItem>

                    {instructorOptions.map((instructor) => (
                      <SelectItem
                        key={instructor.id}
                        value={String(instructor.id)}
                      >
                        {getInstructorLabel(instructor)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <div className="flex gap-2 rounded-xl border border-blue-100 bg-blue-50 p-3 text-xs leading-5 text-blue-800">
                  <Link2 className="mt-0.5 size-4 shrink-0" />

                  <p>
                    {role === "DEPT_HEAD"
                      ? "The linked instructor profile identifies the staff member. Managed Major determines the syllabus review scope."
                      : "The linked instructor profile connects this login account to teaching assignments, class sections, and syllabus ownership."}
                  </p>
                </div>
              </div>
            )}

            {role === "DEPT_HEAD" && (
              <div className="space-y-1.5">
                <Label>Managed Major *</Label>
                <Select value={managedMajorId || NONE} onValueChange={(value) => {
                  setManagedMajorId(value === NONE ? "" : value)
                  setValidationError("")
                }} disabled={mutation.isPending || majorsLoading}>
                  <SelectTrigger><SelectValue placeholder="Select a Major..." /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NONE}>Select a Major...</SelectItem>
                    {majors.map((major) => (
                      <SelectItem key={major.id} value={String(major.id)}>
                        {major.code} — {major.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-slate-500">This Major controls syllabus catalog, dashboard, submission routing, and review scope.</p>
              </div>
            )}

            {!needsProfile && (
              <div className="flex gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                <UserRound className="mt-0.5 size-4 shrink-0" />

                <p>
                  {role === "ADMIN"
                    ? "Administrator accounts manage system configuration and do not require an instructor profile."
                    : "Dean accounts perform final review and do not require an instructor profile in the current workflow."}
                </p>
              </div>
            )}

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
              disabled={mutation.isPending}
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              className="bg-[#007d84] text-white hover:bg-[#006d73]"
              disabled={mutation.isPending}
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
