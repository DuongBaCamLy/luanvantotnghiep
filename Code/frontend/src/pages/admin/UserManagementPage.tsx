import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  Clock3,
  KeyRound,
  Link2,
  Trash2,
  MoreHorizontal,
  Plus,
  Search,
  ShieldCheck,
  UserCheck,
  Users,
  UserX,
} from "lucide-react"

import {
  getUsers,
  toggleUserActive,
} from "@/api/userApi"
import { instructorApi } from "@/api/instructorApi"
import UserFormDialog from "@/components/UserFormDialog"
import DeleteUserDialog from "@/components/DeleteUserDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { formatDateTime } from "@/i18n"
import { useAuthStore } from "@/store/authStore"
import type { UserRole } from "@/types/auth"
import type { Instructor } from "@/types/instructor"
import type { UserAccountResponse } from "@/types/user"

const ALL = "__all__"

const SRS_ROLES: UserRole[] = [
  "ADMIN",
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
  ROLE_LABELS[role] ?? `Legacy: ${role}`

const getRoleBadgeClass = (role: UserRole) => {
  if (role === "ADMIN") {
    return "border-violet-200 bg-violet-50 text-violet-700"
  }

  if (role === "DEAN") {
    return "border-blue-200 bg-blue-50 text-blue-700"
  }

  if (role === "DEPT_HEAD") {
    return "border-cyan-200 bg-cyan-50 text-cyan-700"
  }

  if (role === "INSTRUCTOR") {
    return "border-[#b9dfe1] bg-[#f2fafa] text-[#007d84]"
  }

  return "border-amber-200 bg-amber-50 text-amber-700"
}

const getInstructorLabel = (instructor: Instructor) => {
  const staffCode = instructor.staffCode?.trim()
  const fullName = instructor.fullName?.trim()

  if (staffCode && fullName) {
    return `${staffCode} — ${fullName}`
  }

  return fullName || staffCode || `Instructor #${instructor.id}`
}

const getErrorMessage = (error: unknown) => {
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

  return "Unable to update the user account."
}

export default function UserManagementPage() {
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.user)

  const [searchTerm, setSearchTerm] = useState("")
  const [roleFilter, setRoleFilter] = useState(ALL)
  const [statusFilter, setStatusFilter] = useState(ALL)
  const [formOpen, setFormOpen] = useState(false)
  const [editingUser, setEditingUser] =
    useState<UserAccountResponse | null>(null)
    const [deletingUser, setDeletingUser] =
  useState<UserAccountResponse | null>(null)

const [deleteOpen, setDeleteOpen] =
  useState(false)
  const [notice, setNotice] =
    useState<
      | {
          type: "success" | "error"
          message: string
        }
      | null
    >(null)

  const {
    data: users = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["users"],
    queryFn: getUsers,
  })

  const { data: instructors = [] } = useQuery({
    queryKey: ["instructors"],
    queryFn: instructorApi.getAll,
  })

  const toggleMutation = useMutation({
    mutationFn: toggleUserActive,

    onSuccess: async (updatedUser) => {
      await queryClient.invalidateQueries({
        queryKey: ["users"],
      })

      setNotice({
        type: "success",
        message: updatedUser.isActive
          ? `Account "${updatedUser.username}" has been activated.`
          : `Account "${updatedUser.username}" has been deactivated.`,
      })
    },

    onError: (mutationError) => {
      setNotice({
        type: "error",
        message: getErrorMessage(mutationError),
      })
    },
  })

  const instructorById = useMemo(
    () =>
      new Map(
        instructors.map((instructor) => [
          instructor.id,
          instructor,
        ]),
      ),
    [instructors],
  )

  const stats = useMemo(() => {
    const active = users.filter((user) => user.isActive).length
    const inactive = users.length - active
    const neverSignedIn = users.filter((user) => !user.lastLogin).length

    return {
      total: users.length,
      active,
      inactive,
      neverSignedIn,
    }
  }, [users])

  const filteredUsers = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()

    return users.filter((user) => {
      if (
        roleFilter !== ALL
        && user.role !== roleFilter
      ) {
        return false
      }

      if (
        statusFilter === "ACTIVE"
        && !user.isActive
      ) {
        return false
      }

      if (
        statusFilter === "INACTIVE"
        && user.isActive
      ) {
        return false
      }

      if (!keyword) {
        return true
      }

      const linkedInstructor = user.instructorId
        ? instructorById.get(user.instructorId)
        : undefined

      const searchable = [
        user.username,
        user.email,
        getRoleLabel(user.role),
        linkedInstructor
          ? getInstructorLabel(linkedInstructor)
          : "",
        user.managedMajorCode,
        user.managedMajorName,
      ]
        .join(" ")
        .toLowerCase()

      return searchable.includes(keyword)
    })
  }, [
    instructorById,
    roleFilter,
    searchTerm,
    statusFilter,
    users,
  ])

  const currentUserId = currentUser?.userId

  const openCreate = () => {
    setEditingUser(null)
    setFormOpen(true)
    setNotice(null)
  }

  const openEdit = (user: UserAccountResponse) => {
    setEditingUser(user)
    setFormOpen(true)
    setNotice(null)
  }

  const openDelete = (
  user: UserAccountResponse,
) => {
  setDeletingUser(user)
  setDeleteOpen(true)
  setNotice(null)
}

  const handleToggle = (user: UserAccountResponse) => {
    if (
      user.id === currentUserId
      && user.isActive
    ) {
      setNotice({
        type: "error",
        message:
          "You cannot deactivate the account currently being used to administer the system.",
      })
      return
    }

    const action = user.isActive ? "deactivate" : "activate"

    const confirmed = window.confirm(
      `Are you sure you want to ${action} "${user.username}"?`,
    )

    if (!confirmed) {
      return
    }

    setNotice(null)
    toggleMutation.mutate(user.id)
  }

  const clearFilters = () => {
    setSearchTerm("")
    setRoleFilter(ALL)
    setStatusFilter(ALL)
  }

  return (
    <div data-admin-page="UserManagementPage" className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
          <div data-admin-page-header="UserManagementPage">
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              FR-01.1 / FR-01.2 · Account Administration
            </p>

            <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              User Management
            </h1>

            <p className="mt-1 text-sm text-[#687f89]">
              Create, edit, activate, or deactivate system accounts and maintain the four SRS business roles.
            </p>
          </div>

          <Button
            type="button"
            className="bg-[#007d84] text-white hover:bg-[#006d73]"
            onClick={openCreate}
          >
            <Plus className="size-4" />
            Add User
          </Button>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={<Users className="size-5" />}
          label="Total Users"
          value={stats.total}
          description="All system accounts"
        />

        <MetricCard
          icon={<UserCheck className="size-5" />}
          label="Active"
          value={stats.active}
          description="Accounts allowed to sign in"
        />

        <MetricCard
          icon={<UserX className="size-5" />}
          label="Deactivated"
          value={stats.inactive}
          description="Access disabled by administrator"
        />

        <MetricCard
          icon={<Clock3 className="size-5" />}
          label="Never Signed In"
          value={stats.neverSignedIn}
          description="Accounts with no login history"
        />
      </section>

      {notice && (
        <section
          className={
            notice.type === "success"
              ? "rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
              : "rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
          }
        >
          {notice.message}
        </section>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-5 py-4">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="size-5 text-[#007d84]" />

                <h2 className="font-semibold text-[#17343d]">
                  System Accounts
                </h2>
              </div>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                SRS roles available for new accounts: Administrator, Dean, Head of Department, and Instructor.
              </p>
            </div>

            <div data-admin-filter className="grid w-full gap-3 md:grid-cols-[minmax(260px,1fr)_210px_190px_auto] xl:max-w-[980px]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

                <Input
                  value={searchTerm}
                  onChange={(event) =>
                    setSearchTerm(event.target.value)
                  }
                  placeholder="Search username, email, or instructor..."
                  className="pl-9"
                />
              </div>

              <Select
                value={roleFilter}
                onValueChange={setRoleFilter}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All Roles" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All Roles
                  </SelectItem>

                  {SRS_ROLES.map((role) => (
                    <SelectItem
                      key={role}
                      value={role}
                    >
                      {getRoleLabel(role)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select
                value={statusFilter}
                onValueChange={setStatusFilter}
              >
                <SelectTrigger>
                  <SelectValue placeholder="All Statuses" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All Statuses
                  </SelectItem>

                  <SelectItem value="ACTIVE">
                    Active
                  </SelectItem>

                  <SelectItem value="INACTIVE">
                    Deactivated
                  </SelectItem>
                </SelectContent>
              </Select>

              <Button
                type="button"
                variant="outline"
                onClick={clearFilters}
              >
                Clear
              </Button>
            </div>
          </div>

          <p className="mt-3 text-xs text-slate-500">
            Showing {filteredUsers.length} of {users.length} users.
          </p>
        </div>

        {isError && (
          <div className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
            {getErrorMessage(error)}
          </div>
        )}

        <div className="overflow-x-auto">
          <Table className="min-w-[1280px]">
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>Username</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Managed Major / Scope</TableHead>
                <TableHead>Linked Instructor</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Last Login</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-32 text-center text-slate-500"
                  >
                    Loading users...
                  </TableCell>
                </TableRow>
              ) : filteredUsers.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-40 text-center"
                  >
                    <p className="font-semibold text-slate-700">
                      No users match the selected filters.
                    </p>

                    <p className="mt-1 text-sm text-slate-500">
                      Clear or adjust the filters to view more accounts.
                    </p>
                  </TableCell>
                </TableRow>
              ) : (
                filteredUsers.map((user) => {
                  const linkedInstructor = user.instructorId
                    ? instructorById.get(user.instructorId)
                    : undefined

                  const isSelf = user.id === currentUserId

                 

const isAdministrator =
  user.role === "ADMIN"
                  return (
                    <TableRow
                      key={user.id}
                      className="hover:bg-[#f8fbfb]"
                    >
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-900">
                            {user.username}
                          </span>

                          {isSelf && (
                            <Badge
                              variant="outline"
                              className="border-blue-200 bg-blue-50 text-blue-700"
                            >
                              You
                            </Badge>
                          )}
                        </div>
                      </TableCell>

                      <TableCell className="text-slate-600">
                        {user.email}
                      </TableCell>

                      <TableCell>
                        <Badge
                          variant="outline"
                          className={getRoleBadgeClass(user.role)}
                        >
                          {getRoleLabel(user.role)}
                        </Badge>
                      </TableCell>

                      <TableCell>
                        {user.role === "ADMIN" ? (
                          <span className="font-medium text-violet-700">System-wide</span>
                        ) : user.role === "DEAN" ? (
                          <span className="font-medium text-blue-700">SCSE / All Majors</span>
                        ) : user.role === "DEPT_HEAD" ? (
                          user.managedMajorId ? (
                            <div><p className="font-semibold text-slate-800">{user.managedMajorCode} — {user.managedMajorName}</p></div>
                          ) : <span className="font-medium text-rose-700">Managed Major required</span>
                        ) : <span className="text-slate-400">—</span>}
                      </TableCell>

                      <TableCell>
                        {linkedInstructor ? (
                          <div className="flex items-start gap-2">
                            <Link2 className="mt-0.5 size-4 shrink-0 text-[#007d84]" />

                            <div>
                              <p className="font-medium text-slate-800">
                                {getInstructorLabel(linkedInstructor)}
                              </p>

                              <p className="mt-0.5 text-xs text-slate-400">
                                Profile #{linkedInstructor.id}
                              </p>
                            </div>
                          </div>
                        ) : (
                          <span
                            className={
                              user.role === "INSTRUCTOR"
                              || user.role === "DEPT_HEAD"
                                ? "font-medium text-amber-700"
                                : "text-slate-400"
                            }
                          >
                            {user.role === "INSTRUCTOR"
                            || user.role === "DEPT_HEAD"
                              ? "Profile required"
                              : "Not required"}
                          </span>
                        )}
                      </TableCell>

                      <TableCell>
                        {user.isActive ? (
                          <Badge
                            variant="outline"
                            className="border-emerald-200 bg-emerald-50 text-emerald-700"
                          >
                            Active
                          </Badge>
                        ) : (
                          <Badge
                            variant="outline"
                            className="border-rose-200 bg-rose-50 text-rose-700"
                          >
                            Deactivated
                          </Badge>
                        )}
                      </TableCell>

                      <TableCell className="text-sm text-slate-500">
                        {user.lastLogin
                          ? formatDateTime(user.lastLogin)
                          : "Never signed in"}
                      </TableCell>

                      <TableCell className="text-right">
  <DropdownMenu>
    <DropdownMenuTrigger asChild>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label={`Actions for ${user.username}`}
      >
        <MoreHorizontal className="size-4" />
      </Button>
    </DropdownMenuTrigger>

    <DropdownMenuContent
      align="end"
      className="w-56"
    >
      <DropdownMenuItem
        onClick={() =>
          openEdit(user)
        }
      >
        <KeyRound className="mr-2 size-4" />
        Edit / Reset Password
      </DropdownMenuItem>

      {!isAdministrator && (
        <>
          <DropdownMenuSeparator />

          <DropdownMenuItem
            disabled={
              toggleMutation.isPending
            }
            className={
              user.isActive
                ? "text-rose-700"
                : "text-emerald-700"
            }
            onClick={() =>
              handleToggle(user)
            }
          >
            {user.isActive
              ? "Deactivate Account"
              : "Activate Account"}
          </DropdownMenuItem>

          {!user.isActive && (
            <>
              <DropdownMenuSeparator />

              <DropdownMenuItem
                className="text-rose-700 focus:bg-rose-50 focus:text-rose-700"
                onClick={() =>
                  openDelete(user)
                }
              >
                <Trash2 className="mr-2 size-4" />
                Delete User
              </DropdownMenuItem>
            </>
          )}
        </>
      )}
    </DropdownMenuContent>
  </DropdownMenu>
</TableCell>
                      
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </div>
      </section>

      <UserFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        editingUser={editingUser}
      />
      <DeleteUserDialog
  open={deleteOpen}
  onOpenChange={(open) => {
    setDeleteOpen(open)

    if (!open) {
      setDeletingUser(null)
    }
  }}
  user={deletingUser}
/>
    </div>
  )
}

function MetricCard({
  icon,
  label,
  value,
  description,
}: {
  icon: React.ReactNode
  label: string
  value: number
  description: string
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

          <p className="mt-1 text-xs text-slate-500">
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
