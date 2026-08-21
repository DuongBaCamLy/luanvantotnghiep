import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  CheckCircle2,
  Edit3,
  Eye,
  FileSpreadsheet,
  Link2,
  Loader2,
  Plus,
  Power,
  Search,
  ShieldCheck,
  UserRound,
  Users,
} from "lucide-react"

import {
  instructorApi,
  type InstructorExportFilters,
} from "@/api/instructorApi"
import { departmentApi } from "@/api/departmentApi"
import { Badge } from "@/components/ui/badge"
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type { UserRole } from "@/types/auth"
import type {
  Instructor,
  InstructorRequest,
} from "@/types/instructor"

const ALL = "all"

type InstructorWithAccount =
  Instructor & {
    userAccountId?: number | null
    username?: string | null
    accountActive?: boolean | null
  }

const ROLE_LABELS: Partial<
  Record<UserRole, string>
> = {
  ADMIN: "Administrator",
  DEAN: "Dean",
  DEPT_HEAD: "Head of Department",
  INSTRUCTOR: "Instructor",
}

const AVATAR_COLORS = [
  "bg-blue-100 text-blue-800 border-blue-200",
  "bg-purple-100 text-purple-800 border-purple-200",
  "bg-emerald-100 text-emerald-800 border-emerald-200",
  "bg-amber-100 text-amber-800 border-amber-200",
  "bg-rose-100 text-rose-800 border-rose-200",
  "bg-indigo-100 text-indigo-800 border-indigo-200",
  "bg-cyan-100 text-cyan-800 border-cyan-200",
]

const getRoleLabel = (
  role: UserRole | null | undefined,
) =>
  role
    ? (
        ROLE_LABELS[role]
        ?? role
      )
    : "Not linked"

const getInitials = (
  name: string,
) => {
  const parts =
    name
      .trim()
      .split(/\s+/)
      .filter(Boolean)

  if (parts.length === 0) {
    return "?"
  }

  if (parts.length === 1) {
    return parts[0]!
      .charAt(0)
      .toUpperCase()
  }

  return (
    parts[parts.length - 2]!
      .charAt(0)
    + parts[parts.length - 1]!
      .charAt(0)
  ).toUpperCase()
}

const getAvatarColor = (
  id: number,
) =>
  AVATAR_COLORS[
    id % AVATAR_COLORS.length
  ]!

const getErrorMessage = (
  error: unknown,
) => {
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

  return "Unable to process the instructor profile."
}

const todayFileStamp = () => {
  const date = new Date()

  const pad = (
    value: number,
  ) =>
    String(value)
      .padStart(2, "0")

  return (
    `${date.getFullYear()}`
    + `${pad(date.getMonth() + 1)}`
    + `${pad(date.getDate())}_`
    + `${pad(date.getHours())}`
    + `${pad(date.getMinutes())}`
  )
}

const downloadBlob = (
  blob: Blob,
  filename: string,
) => {
  const url =
    URL.createObjectURL(blob)

  const anchor =
    document.createElement("a")

  anchor.href = url
  anchor.download = filename

  document.body.appendChild(
    anchor,
  )

  anchor.click()
  anchor.remove()

  URL.revokeObjectURL(url)
}

export default function InstructorManagementPage() {
  const queryClient =
    useQueryClient()

  const [
    searchQuery,
    setSearchQuery,
  ] = useState("")

  const [
    filterDept,
    setFilterDept,
  ] = useState(ALL)

  const [
    filterRole,
    setFilterRole,
  ] = useState(ALL)

  const [
    filterDegree,
    setFilterDegree,
  ] = useState(ALL)

  const [
    filterStatus,
    setFilterStatus,
  ] = useState(ALL)

  const [
    dialogOpen,
    setDialogOpen,
  ] = useState(false)

  const [
    dialogMode,
    setDialogMode,
  ] =
    useState<
      "create" | "edit" | "view"
    >("create")

  const [
    selectedInstructor,
    setSelectedInstructor,
  ] =
    useState<InstructorWithAccount | null>(
      null,
    )

  const [
    staffCode,
    setStaffCode,
  ] = useState("")

  const [
    fullName,
    setFullName,
  ] = useState("")

  const [
    email,
    setEmail,
  ] = useState("")

  const [
    degree,
    setDegree,
  ] = useState("PhD")

  const [
    academicRank,
    setAcademicRank,
  ] = useState("Lecturer")

  const [
    departmentId,
    setDepartmentId,
  ] = useState("")

  const [
    isActive,
    setIsActive,
  ] = useState(true)

  const [
    validationError,
    setValidationError,
  ] = useState("")

  const [
    notice,
    setNotice,
  ] =
    useState<
      | {
          type: "success" | "error"
          message: string
        }
      | null
    >(null)

  const {
    data: instructors = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["instructors"],
    queryFn: instructorApi.getAll,
  })

  const {
    data: departments = [],
  } = useQuery({
    queryKey: ["departments"],
    queryFn: departmentApi.getAll,
  })

  const createMutation =
    useMutation({
      mutationFn:
        instructorApi.create,

      onSuccess: async (
        created,
      ) => {
        await queryClient
          .invalidateQueries({
            queryKey: [
              "instructors",
            ],
          })

        setDialogOpen(false)
        resetForm()

        setNotice({
          type: "success",
          message:
            `Instructor profile "${created.fullName}" was created. Create or link the login account from User Management when access is required.`,
        })
      },

      onError: (
        mutationError,
      ) => {
        setValidationError(
          getErrorMessage(
            mutationError,
          ),
        )
      },
    })

  const updateMutation =
    useMutation({
      mutationFn: ({
        id,
        data,
      }: {
        id: number
        data: InstructorRequest
      }) =>
        instructorApi.update(
          id,
          data,
        ),

      onSuccess: async (
        updated,
      ) => {
        await queryClient
          .invalidateQueries({
            queryKey: [
              "instructors",
            ],
          })

        setDialogOpen(false)
        resetForm()

        setNotice({
          type: "success",
          message:
            `Instructor profile "${updated.fullName}" was updated.`,
        })
      },

      onError: (
        mutationError,
      ) => {
        setValidationError(
          getErrorMessage(
            mutationError,
          ),
        )
      },
    })

  const toggleStatusMutation =
    useMutation({
      mutationFn: ({
        id,
        data,
      }: {
        id: number
        data: InstructorRequest
      }) =>
        instructorApi.update(
          id,
          data,
        ),

      onSuccess: async (
        updated,
      ) => {
        await queryClient
          .invalidateQueries({
            queryKey: [
              "instructors",
            ],
          })

        setNotice({
          type: "success",
          message:
            `Instructor profile "${updated.fullName}" is now ${updated.isActive ? "Active" : "Inactive"}. Login access is managed separately in User Management.`,
        })
      },

      onError: (
        mutationError,
      ) => {
        setNotice({
          type: "error",
          message:
            getErrorMessage(
              mutationError,
            ),
        })
      },
    })

  const exportMutation =
    useMutation({
      mutationFn:
        (
          filters:
            InstructorExportFilters,
        ) =>
          instructorApi
            .exportExcel(filters),

      onSuccess: (blob) => {
        downloadBlob(
          blob,
          `Instructor_List_SCSE_${todayFileStamp()}.xlsx`,
        )

        setNotice({
          type: "success",
          message:
            "The filtered instructor directory was exported as a formatted .xlsx workbook ready for printing.",
        })
      },

      onError: (
        mutationError,
      ) => {
        setNotice({
          type: "error",
          message:
            getErrorMessage(
              mutationError,
            ),
        })
      },
    })

  const stats =
    useMemo(() => {
      let phd = 0
      let msc = 0
      let active = 0
      let deptHeads = 0
      let unlinked = 0

      instructors.forEach(
        (rawInstructor) => {
          const instructor =
            rawInstructor as
              InstructorWithAccount

          const normalizedDegree =
            (
              instructor.degree
              ?? ""
            ).toUpperCase()

          if (
            normalizedDegree
              .includes("PHD")
            || normalizedDegree
              .includes("TIẾN SĨ")
            || normalizedDegree
              === "TS"
          ) {
            phd++
          } else if (
            normalizedDegree
              .includes("MSC")
            || normalizedDegree
              .includes("THẠC SĨ")
            || normalizedDegree
              === "THS"
          ) {
            msc++
          }

          if (
            instructor.isActive
          ) {
            active++
          }

          if (
            instructor.role
            === "DEPT_HEAD"
          ) {
            deptHeads++
          }

          if (
            !instructor
              .userAccountId
          ) {
            unlinked++
          }
        },
      )

      return {
        total:
          instructors.length,
        phd,
        msc,
        active,
        deptHeads,
        unlinked,
      }
    }, [instructors])

  const filteredInstructors =
    useMemo(() => {
      const searchLower =
        searchQuery
          .trim()
          .toLowerCase()

      return instructors
        .map(
          (item) =>
            item as
              InstructorWithAccount,
        )
        .filter(
          (instructor) => {
            const matchesSearch =
              !searchLower
              || instructor
                .staffCode
                .toLowerCase()
                .includes(
                  searchLower,
                )
              || instructor
                .fullName
                .toLowerCase()
                .includes(
                  searchLower,
                )
              || instructor
                .email
                .toLowerCase()
                .includes(
                  searchLower,
                )

            const matchesDept =
              filterDept === ALL
              || String(
                instructor
                  .departmentId,
              ) === filterDept

            const matchesRole =
              filterRole === ALL
              || (
                filterRole
                === "UNLINKED"
                  ? !instructor
                      .userAccountId
                  : instructor.role
                    === filterRole
              )

            const normalizedDegree =
              (
                instructor.degree
                ?? ""
              ).toUpperCase()

            const matchesDegree =
              filterDegree === ALL
              || (
                filterDegree
                  === "PHD"
                && (
                  normalizedDegree
                    .includes("PHD")
                  || normalizedDegree
                    .includes("TIẾN SĨ")
                  || normalizedDegree
                    === "TS"
                )
              )
              || (
                filterDegree
                  === "MSC"
                && (
                  normalizedDegree
                    .includes("MSC")
                  || normalizedDegree
                    .includes("THẠC SĨ")
                  || normalizedDegree
                    === "THS"
                )
              )

            const matchesStatus =
              filterStatus === ALL
              || (
                filterStatus
                  === "active"
                && instructor
                  .isActive
              )
              || (
                filterStatus
                  === "inactive"
                && !instructor
                  .isActive
              )

            return (
              matchesSearch
              && matchesDept
              && matchesRole
              && matchesDegree
              && matchesStatus
            )
          },
        )
    }, [
      filterDegree,
      filterDept,
      filterRole,
      filterStatus,
      instructors,
      searchQuery,
    ])

  const resetForm = () => {
    setStaffCode("")
    setFullName("")
    setEmail("")
    setDegree("PhD")
    setAcademicRank(
      "Lecturer",
    )
    setDepartmentId("")
    setIsActive(true)
    setSelectedInstructor(
      null,
    )
    setValidationError("")
  }

  const openDialog = (
    mode:
      | "create"
      | "edit"
      | "view",
    instructor?:
      InstructorWithAccount,
  ) => {
    setDialogMode(mode)
    setValidationError("")
    setNotice(null)

    if (instructor) {
      setSelectedInstructor(
        instructor,
      )
      setStaffCode(
        instructor.staffCode,
      )
      setFullName(
        instructor.fullName,
      )
      setEmail(
        instructor.email,
      )
      setDegree(
        instructor.degree
        || "PhD",
      )
      setAcademicRank(
        instructor.academicRank
        || "Lecturer",
      )
      setDepartmentId(
        String(
          instructor
            .departmentId
          ?? "",
        ),
      )
      setIsActive(
        instructor.isActive,
      )
    } else {
      resetForm()
    }

    setDialogOpen(true)
  }

  const validateForm = () => {
    if (
      !staffCode.trim()
      || !fullName.trim()
      || !email.trim()
      || !departmentId
    ) {
      return "Staff Code, Full Name, Email, and Department are required."
    }

    if (
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/
        .test(
          email.trim(),
        )
    ) {
      return "Please enter a valid work email address."
    }

    return ""
  }

  const buildPayload =
    (): InstructorRequest => {
      /*
       * role remains in the legacy request DTO for compatibility,
       * but backend Instructor Management no longer changes account roles.
       */
      const legacyRole =
        selectedInstructor
          ?.role
        ?? "INSTRUCTOR"

      return {
        staffCode:
          staffCode.trim(),
        fullName:
          fullName.trim(),
        email:
          email.trim(),
        degree:
          degree.trim(),
        academicRank:
          academicRank.trim(),
        departmentId:
          Number(
            departmentId,
          ),
        isActive,
        role: legacyRole,
      }
    }

  const handleSubmit = (
    event: React.FormEvent,
  ) => {
    event.preventDefault()

    const validation =
      validateForm()

    if (validation) {
      setValidationError(
        validation,
      )
      return
    }

    setValidationError("")

    const payload =
      buildPayload()

    if (
      dialogMode
      === "create"
    ) {
      createMutation.mutate(
        payload,
      )
      return
    }

    if (
      dialogMode === "edit"
      && selectedInstructor
    ) {
      updateMutation.mutate({
        id:
          selectedInstructor.id,
        data: payload,
      })
    }
  }

  const handleToggleStatus = (
    instructor:
      InstructorWithAccount,
  ) => {
    const nextActive =
      !instructor.isActive

    const confirmed =
      window.confirm(
        nextActive
          ? `Activate instructor profile "${instructor.fullName}"?`
          : `Deactivate instructor profile "${instructor.fullName}"?\n\nThis changes the staff profile only. Login access remains managed in User Management.`,
      )

    if (!confirmed) {
      return
    }

    toggleStatusMutation
      .mutate({
        id: instructor.id,
        data: {
          staffCode:
            instructor.staffCode,
          fullName:
            instructor.fullName,
          email:
            instructor.email,
          degree:
            instructor.degree,
          academicRank:
            instructor
              .academicRank,
          departmentId:
            instructor
              .departmentId,
          isActive:
            nextActive,
          role:
            instructor.role
            ?? "INSTRUCTOR",
        },
      })
  }

  const clearFilters = () => {
    setSearchQuery("")
    setFilterDept(ALL)
    setFilterRole(ALL)
    setFilterDegree(ALL)
    setFilterStatus(ALL)
  }

  const exportCurrentView =
    () => {
      const departmentLabel =
        filterDept === ALL
          ? "All"
          : (
              departments.find(
                (department) =>
                  String(
                    department.id,
                  ) === filterDept,
              )?.nameVn
              ?? filterDept
            )

      exportMutation.mutate({
        instructorIds:
          filteredInstructors.map(
            (instructor) =>
              instructor.id,
          ),
        search:
          searchQuery.trim(),
        department:
          departmentLabel,
        role:
          filterRole,
        degree:
          filterDegree,
        status:
          filterStatus,
      })
    }

  const isSaving =
    createMutation.isPending
    || updateMutation.isPending

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-4 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              Instructor Profiles · Admin
            </p>

            <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              Instructor Management
            </h1>

            <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
              Maintain SCSE instructor profiles. Login accounts and system roles are managed separately in User Management; teaching responsibility is managed in Teaching Assignments.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={
                exportMutation
                  .isPending
                || filteredInstructors
                  .length === 0
              }
              onClick={
                exportCurrentView
              }
            >
              {exportMutation
                .isPending ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <FileSpreadsheet className="size-4 text-emerald-600" />
                )}

              Export Excel
            </Button>

            <Button
              type="button"
              className="bg-[#007d84] text-white hover:bg-[#006d73]"
              onClick={() =>
                openDialog(
                  "create",
                )
              }
            >
              <Plus className="size-4" />
              Add Instructor
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Total Instructors"
          value={stats.total}
          description={`${stats.phd} PhD · ${stats.msc} MSc`}
          icon={
            <Users className="size-5" />
          }
        />

        <MetricCard
          label="Active Profiles"
          value={stats.active}
          description={`${stats.total - stats.active} inactive profiles`}
          icon={
            <CheckCircle2 className="size-5" />
          }
        />

        <MetricCard
          label="Department Heads"
          value={
            stats.deptHeads
          }
          description="Resolved from linked user accounts"
          icon={
            <ShieldCheck className="size-5" />
          }
        />

        <MetricCard
          label="Unlinked Accounts"
          value={stats.unlinked}
          description="Profiles that still need a login-account link"
          icon={
            <Link2 className="size-5" />
          }
        />
      </section>

      {notice && (
        <section
          className={
            notice.type
              === "success"
              ? "rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
              : "rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
          }
        >
          {notice.message}
        </section>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <div className="space-y-1.5">
            <Label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Department
            </Label>

            <Select
              value={
                filterDept
              }
              onValueChange={
                setFilterDept
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Departments" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Departments
                </SelectItem>

                {departments.map(
                  (department) => (
                    <SelectItem
                      key={
                        department.id
                      }
                      value={String(
                        department.id,
                      )}
                    >
                      {department.nameVn}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Account Role
            </Label>

            <Select
              value={
                filterRole
              }
              onValueChange={
                setFilterRole
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Roles" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Roles
                </SelectItem>

                <SelectItem value="INSTRUCTOR">
                  Instructor
                </SelectItem>

                <SelectItem value="DEPT_HEAD">
                  Head of Department
                </SelectItem>

                <SelectItem value="DEAN">
                  Dean
                </SelectItem>

                <SelectItem value="UNLINKED">
                  Not Linked
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Degree
            </Label>

            <Select
              value={
                filterDegree
              }
              onValueChange={
                setFilterDegree
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Degrees" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Degrees
                </SelectItem>

                <SelectItem value="PHD">
                  PhD
                </SelectItem>

                <SelectItem value="MSC">
                  MSc
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Profile Status
            </Label>

            <Select
              value={
                filterStatus
              }
              onValueChange={
                setFilterStatus
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Statuses
                </SelectItem>

                <SelectItem value="active">
                  Active
                </SelectItem>

                <SelectItem value="inactive">
                  Inactive
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Search
            </Label>

            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

              <Input
                value={
                  searchQuery
                }
                onChange={(
                  event,
                ) =>
                  setSearchQuery(
                    event.target.value,
                  )
                }
                placeholder="Code, name, or email..."
                className="pl-9"
              />
            </div>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
          <p className="text-xs text-slate-500">
            Showing {filteredInstructors.length} of {instructors.length} instructor profiles.
          </p>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={
              clearFilters
            }
          >
            Clear Filters
          </Button>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isError && (
          <div className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
            {getErrorMessage(
              error,
            )}
          </div>
        )}

        <div className="overflow-x-auto">
          <Table className="min-w-[1250px]">
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>
                  Staff Code
                </TableHead>

                <TableHead>
                  Instructor
                </TableHead>

                <TableHead>
                  Department
                </TableHead>

                <TableHead>
                  Login Account
                </TableHead>

                <TableHead>
                  Degree
                </TableHead>

                <TableHead>
                  Courses
                </TableHead>

                <TableHead>
                  Profile Status
                </TableHead>

                <TableHead className="text-right">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-32 text-center text-slate-500"
                  >
                    <div className="flex items-center justify-center gap-2">
                      <Loader2 className="size-4 animate-spin" />
                      Loading instructor profiles...
                    </div>
                  </TableCell>
                </TableRow>
              ) : filteredInstructors
                  .length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={8}
                        className="h-40 text-center"
                      >
                        <p className="font-semibold text-slate-700">
                          No instructor profiles match the selected filters.
                        </p>

                        <p className="mt-1 text-sm text-slate-500">
                          Clear or adjust the filters to view more profiles.
                        </p>
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredInstructors.map(
                      (instructor) => (
                        <TableRow
                          key={
                            instructor.id
                          }
                          className="hover:bg-[#f8fbfb]"
                        >
                          <TableCell className="font-mono font-semibold text-slate-800">
                            {instructor.staffCode}
                          </TableCell>

                          <TableCell>
                            <div className="flex items-center gap-3">
                              <div
                                className={`flex size-9 shrink-0 items-center justify-center rounded-full border text-xs font-bold ${getAvatarColor(instructor.id)}`}
                              >
                                {getInitials(
                                  instructor.fullName,
                                )}
                              </div>

                              <div>
                                <p className="font-semibold text-slate-900">
                                  {instructor.fullName}
                                </p>

                                <p className="mt-0.5 text-xs text-slate-400">
                                  {instructor.email}
                                </p>
                              </div>
                            </div>
                          </TableCell>

                          <TableCell className="text-slate-700">
                            {instructor.departmentNameVn
                              || instructor.departmentName
                              || "—"}
                          </TableCell>

                          <TableCell>
                            {instructor.userAccountId ? (
                              <div>
                                <div className="flex items-center gap-2">
                                  <Link2 className="size-3.5 text-[#007d84]" />

                                  <span className="font-medium text-slate-800">
                                    {instructor.username
                                      || "Linked"}
                                  </span>
                                </div>

                                <div className="mt-1 flex flex-wrap items-center gap-2">
                                  <Badge
                                    variant="outline"
                                    className="border-blue-200 bg-blue-50 text-blue-700"
                                  >
                                    {getRoleLabel(
                                      instructor.role,
                                    )}
                                  </Badge>

                                  <span
                                    className={
                                      instructor.accountActive
                                        ? "text-xs font-medium text-emerald-700"
                                        : "text-xs font-medium text-rose-700"
                                    }
                                  >
                                    {instructor.accountActive
                                      ? "Account Active"
                                      : "Account Deactivated"}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <div>
                                <Badge
                                  variant="outline"
                                  className="border-amber-200 bg-amber-50 text-amber-700"
                                >
                                  Not linked
                                </Badge>

                                <p className="mt-1 text-xs text-slate-400">
                                  Link in User Management
                                </p>
                              </div>
                            )}
                          </TableCell>

                          <TableCell>
                            <div>
                              <p className="font-medium text-slate-700">
                                {instructor.degree
                                  || "—"}
                              </p>

                              {instructor.academicRank && (
                                <p className="mt-0.5 text-xs text-slate-400">
                                  {instructor.academicRank}
                                </p>
                              )}
                            </div>
                          </TableCell>

                          <TableCell className="font-semibold text-slate-700">
                            {instructor.courseCount
                              || 0}
                          </TableCell>

                          <TableCell>
                            {instructor.isActive ? (
                              <Badge
                                variant="outline"
                                className="border-emerald-200 bg-emerald-50 text-emerald-700"
                              >
                                Active
                              </Badge>
                            ) : (
                              <Badge
                                variant="outline"
                                className="border-slate-300 bg-slate-100 text-slate-600"
                              >
                                Inactive
                              </Badge>
                            )}
                          </TableCell>

                          <TableCell>
                            <div className="flex justify-end gap-1">
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() =>
                                  openDialog(
                                    "view",
                                    instructor,
                                  )
                                }
                              >
                                <Eye className="size-3.5" />
                                View
                              </Button>

                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                onClick={() =>
                                  openDialog(
                                    "edit",
                                    instructor,
                                  )
                                }
                              >
                                <Edit3 className="size-3.5" />
                                Edit
                              </Button>

                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                disabled={
                                  toggleStatusMutation
                                    .isPending
                                }
                                className={
                                  instructor.isActive
                                    ? "text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                                    : "text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
                                }
                                onClick={() =>
                                  handleToggleStatus(
                                    instructor,
                                  )
                                }
                              >
                                <Power className="size-3.5" />

                                {instructor.isActive
                                  ? "Deactivate"
                                  : "Activate"}
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ),
                    )
                  )}
            </TableBody>
          </Table>
        </div>
      </section>

      <Dialog
        open={
          dialogOpen
        }
        onOpenChange={(
          nextOpen,
        ) => {
          if (!isSaving) {
            setDialogOpen(
              nextOpen,
            )
          }
        }}
      >
        <DialogContent className="sm:max-w-[640px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-[#17343d]">
              {dialogMode
                === "create" ? (
                  <>
                    <Users className="size-5 text-[#007d84]" />
                    Add Instructor Profile
                  </>
                ) : dialogMode
                  === "edit" ? (
                    <>
                      <Edit3 className="size-5 text-[#007d84]" />
                      Edit Instructor Profile
                    </>
                  ) : (
                    <>
                      <UserRound className="size-5 text-[#007d84]" />
                      Instructor Profile
                    </>
                  )}
            </DialogTitle>

            <DialogDescription>
              {dialogMode
                === "create"
                ? "Create the staff profile only. Login access is created or linked separately in User Management."
                : dialogMode
                  === "edit"
                  ? "Update academic and organizational profile information. Account role/password/status are not changed here."
                  : "Review instructor profile information and the linked login-account state."}
            </DialogDescription>
          </DialogHeader>

          {dialogMode
            === "view"
            && selectedInstructor ? (
              <div className="space-y-5">
                <div className="flex items-center gap-4 rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <div
                    className={`flex size-14 items-center justify-center rounded-full border text-lg font-bold ${getAvatarColor(selectedInstructor.id)}`}
                  >
                    {getInitials(
                      selectedInstructor.fullName,
                    )}
                  </div>

                  <div>
                    <h3 className="font-bold text-slate-900">
                      {selectedInstructor.fullName}
                    </h3>

                    <p className="text-sm text-slate-500">
                      {selectedInstructor.email}
                    </p>
                  </div>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <Info
                    label="Staff Code"
                    value={
                      selectedInstructor.staffCode
                    }
                  />

                  <Info
                    label="Department"
                    value={
                      selectedInstructor.departmentNameVn
                      || selectedInstructor.departmentName
                    }
                  />

                  <Info
                    label="Degree"
                    value={
                      selectedInstructor.degree
                    }
                  />

                  <Info
                    label="Academic Rank"
                    value={
                      selectedInstructor.academicRank
                    }
                  />

                  <Info
                    label="Assigned Courses"
                    value={String(
                      selectedInstructor.courseCount
                      || 0,
                    )}
                  />

                  <Info
                    label="Profile Status"
                    value={
                      selectedInstructor.isActive
                        ? "Active"
                        : "Inactive"
                    }
                  />

                  <Info
                    label="Login Account"
                    value={
                      selectedInstructor.userAccountId
                        ? selectedInstructor.username
                          || "Linked"
                        : "Not linked"
                    }
                  />

                  <Info
                    label="Account Role"
                    value={
                      selectedInstructor.userAccountId
                        ? getRoleLabel(
                            selectedInstructor.role,
                          )
                        : "—"
                    }
                  />
                </div>

                <div className="rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm leading-6 text-blue-800">
                  Passwords are never displayed here. Use User Management to create/link the login account, reset a password, change the SRS role, or deactivate sign-in access.
                </div>

                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() =>
                      setDialogOpen(
                        false,
                      )
                    }
                  >
                    Close
                  </Button>
                </DialogFooter>
              </div>
            ) : (
              <form
                onSubmit={
                  handleSubmit
                }
                className="space-y-4"
              >
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="inst-code">
                      Staff Code *
                    </Label>

                    <Input
                      id="inst-code"
                      value={
                        staffCode
                      }
                      onChange={(
                        event,
                      ) => {
                        setStaffCode(
                          event.target.value,
                        )
                        setValidationError(
                          "",
                        )
                      }}
                      placeholder="e.g. GV014"
                      disabled={
                        isSaving
                      }
                    />
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="inst-name">
                      Full Name *
                    </Label>

                    <Input
                      id="inst-name"
                      value={
                        fullName
                      }
                      onChange={(
                        event,
                      ) => {
                        setFullName(
                          event.target.value,
                        )
                        setValidationError(
                          "",
                        )
                      }}
                      placeholder="e.g. Nguyen Van B"
                      disabled={
                        isSaving
                      }
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="inst-email">
                    Work Email *
                  </Label>

                  <Input
                    id="inst-email"
                    type="email"
                    value={email}
                    onChange={(
                      event,
                    ) => {
                      setEmail(
                        event.target.value,
                      )
                      setValidationError(
                        "",
                      )
                    }}
                    placeholder="name@hcmiu.edu.vn"
                    disabled={
                      isSaving
                    }
                  />
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label>
                      Degree
                    </Label>

                    <Select
                      value={
                        degree
                      }
                      onValueChange={
                        setDegree
                      }
                      disabled={
                        isSaving
                      }
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>

                      <SelectContent>
                        <SelectItem value="PhD">
                          PhD
                        </SelectItem>

                        <SelectItem value="MSc">
                          MSc
                        </SelectItem>

                        <SelectItem value="BSc">
                          BSc
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="inst-rank">
                      Academic Rank / Title
                    </Label>

                    <Input
                      id="inst-rank"
                      value={
                        academicRank
                      }
                      onChange={(
                        event,
                      ) =>
                        setAcademicRank(
                          event.target.value,
                        )
                      }
                      placeholder="Lecturer, Associate Professor..."
                      disabled={
                        isSaving
                      }
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label>
                    Department *
                  </Label>

                  <Select
                    value={
                      departmentId
                    }
                    onValueChange={(
                      value,
                    ) => {
                      setDepartmentId(
                        value,
                      )
                      setValidationError(
                        "",
                      )
                    }}
                    disabled={
                      isSaving
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select Department" />
                    </SelectTrigger>

                    <SelectContent>
                      {departments.map(
                        (department) => (
                          <SelectItem
                            key={
                              department.id
                            }
                            value={String(
                              department.id,
                            )}
                          >
                            {department.nameVn}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </div>

                <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <input
                    type="checkbox"
                    checked={
                      isActive
                    }
                    onChange={(
                      event,
                    ) =>
                      setIsActive(
                        event.target.checked,
                      )
                    }
                    className="mt-1 size-4"
                    disabled={
                      isSaving
                    }
                  />

                  <div>
                    <p className="font-semibold text-slate-800">
                      Active instructor profile
                    </p>

                    <p className="mt-1 text-xs leading-5 text-slate-500">
                      This controls staff-profile availability only. Login account activation is managed independently in User Management.
                    </p>
                  </div>
                </label>

                {validationError && (
                  <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                    {validationError}
                  </div>
                )}

                <div className="rounded-xl border border-blue-100 bg-blue-50 p-4 text-xs leading-5 text-blue-800">
                  <strong>Account separation:</strong> this form never creates a default password and never changes the user's SRS role. After creating a profile, link it from User Management when login access is needed.
                </div>

                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={
                      isSaving
                    }
                    onClick={() =>
                      setDialogOpen(
                        false,
                      )
                    }
                  >
                    Cancel
                  </Button>

                  <Button
                    type="submit"
                    className="bg-[#007d84] text-white hover:bg-[#006d73]"
                    disabled={
                      isSaving
                    }
                  >
                    {isSaving && (
                      <Loader2 className="size-4 animate-spin" />
                    )}

                    {isSaving
                      ? "Saving..."
                      : dialogMode
                        === "create"
                        ? "Create Profile"
                        : "Save Changes"}
                  </Button>
                </DialogFooter>
              </form>
            )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function MetricCard({
  label,
  value,
  description,
  icon,
}: {
  label: string
  value: number
  description: string
  icon: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-2xl font-bold text-slate-900">
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>

        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
          {icon}
        </span>
      </div>
    </div>
  )
}

function Info({
  label,
  value,
}: {
  label: string
  value?: string | null
}) {
  return (
    <div>
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-sm font-semibold text-slate-800">
        {value || "—"}
      </p>
    </div>
  )
}