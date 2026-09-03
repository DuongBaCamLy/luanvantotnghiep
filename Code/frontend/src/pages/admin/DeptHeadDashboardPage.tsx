import type { ReactNode } from "react"
import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  CircleAlert,
  ClipboardCheck,
  Clock3,
  FileText,
  RefreshCw,
  Search,
  ShieldCheck,
  UserCheck,
} from "lucide-react"

import { dashboardApi } from "@/api/dashboardApi"
import { approvalRequestApi } from "@/api/approvalRequestApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
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
import { useAuthStore } from "@/store/authStore"

const ALL = "__all__"

const STATUS_LABELS: Record<string, string> = {
  NOT_CREATED: "Not created",
  DRAFT: "Draft",
  SUBMITTED: "Pending Department Review",
  UNDER_REVIEW: "Forwarded to Dean",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  REVISION_REQUESTED: "Revision requested",
  ARCHIVED: "Archived",
}

const STATUS_ORDER = [
  "SUBMITTED",
  "REVISION_REQUESTED",
  "REJECTED",
  "NOT_CREATED",
  "DRAFT",
  "UNDER_REVIEW",
  "APPROVED",
  "ARCHIVED",
]

function normalizeStatus(
  value: string | null | undefined,
) {
  const normalized =
    String(value ?? "")
      .trim()
      .toUpperCase()

  return normalized || "NOT_CREATED"
}

function humanizeStatus(
  status: string,
) {
  return (
    STATUS_LABELS[status]
    ?? status
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(
        /\b\w/g,
        (character) =>
          character.toUpperCase(),
      )
  )
}

function hasInstructor(
  value: string | null | undefined,
) {
  const normalized =
    String(value ?? "")
      .trim()
      .toLowerCase()

  return Boolean(
    normalized
    && normalized !== "not assigned"
    && normalized !== "n/a"
    && normalized !== "-",
  )
}

function workflowLabel(
  status: string,
) {
  if (
    status === "SUBMITTED"
  ) {
    return "Department decision required"
  }

  if (
    status === "UNDER_REVIEW"
  ) {
    return "Dean final review"
  }

  if (
    status === "APPROVED"
  ) {
    return "Workflow completed"
  }

  if (
    status === "REVISION_REQUESTED"
    || status === "REJECTED"
  ) {
    return "Returned for revision"
  }

  if (
    status === "DRAFT"
  ) {
    return "Instructor preparation"
  }

  if (
    status === "NOT_CREATED"
  ) {
    return "Syllabus not started"
  }

  if (
    status === "ARCHIVED"
  ) {
    return "Historical / archived"
  }

  return humanizeStatus(status)
}

function requiresAttention(
  status: string,
) {
  return [
    "NOT_CREATED",
    "DRAFT",
    "SUBMITTED",
    "REJECTED",
    "REVISION_REQUESTED",
  ].includes(status)
}

export default function DeptHeadCoursesPage() {
  const { user } = useAuthStore()

  const [
    searchTerm,
    setSearchTerm,
  ] = useState("")

  const [
    statusFilter,
    setStatusFilter,
  ] = useState(ALL)

  const [
    ownershipFilter,
    setOwnershipFilter,
  ] = useState(ALL)

  const {
    data: dashboard,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: [
      "depthead-courses",
      user?.userId,
    ],
    queryFn: () =>
      dashboardApi
        .getDeptHeadDashboard(
          user?.userId ?? 0,
        ),
    enabled:
      Boolean(user?.userId),
    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  const {
    data: pendingReviews = [],
    isLoading: pendingReviewsLoading,
    refetch: refetchPendingReviews,
  } = useQuery({
    queryKey: ["approval-requests", "pending", "STEP1_DEPT_HEAD"],
    queryFn: () => approvalRequestApi.getPendingByStep("STEP1_DEPT_HEAD"),
    enabled: Boolean(user?.userId),
    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const summary =
    useMemo(() => {
      if (!dashboard) {
        return {
          total: 0,
          assigned: 0,
          created: 0,
          pending: 0,
          forwarded: 0,
          rejected: 0,
          approved: 0,
          attention: 0,
        }
      }

      const courses =
        dashboard.coursesStatus

      const assigned =
        courses.filter(
          (course) =>
            hasInstructor(
              course.instructorName,
            ),
        ).length

      const created =
        courses.filter(
          (course) =>
            normalizeStatus(
              course.status,
            ) !== "NOT_CREATED",
        ).length

      const approved =
        courses.filter(
          (course) =>
            normalizeStatus(
              course.status,
            ) === "APPROVED",
        ).length

      const attention =
        courses.filter(
          (course) =>
            requiresAttention(
              normalizeStatus(
                course.status,
              ),
            ),
        ).length

      return {
        total:
          dashboard
            .totalCoursesInDept,
        assigned,
        created,
        pending: pendingReviews.length,
        forwarded: courses.filter((course) => normalizeStatus(course.status) === "UNDER_REVIEW").length,
        rejected: courses.filter((course) => ["REJECTED", "REVISION_REQUESTED"].includes(normalizeStatus(course.status))).length,
        approved,
        attention,
      }
    }, [
      dashboard,
      pendingReviews,
    ])

  const statusOptions =
    useMemo(() => {
      if (!dashboard) {
        return []
      }

      const found =
        new Set(
          dashboard
            .coursesStatus
            .map(
              (course) =>
                normalizeStatus(
                  course.status,
                ),
            ),
        )

      return STATUS_ORDER
        .filter(
          (status) =>
            found.has(status),
        )
        .concat(
          Array.from(found)
            .filter(
              (status) =>
                !STATUS_ORDER
                  .includes(status),
            )
            .sort(),
        )
    }, [
      dashboard,
    ])

  const filteredCourses =
    useMemo(() => {
      if (!dashboard) {
        return []
      }

      const keyword =
        searchTerm
          .trim()
          .toLowerCase()

      return dashboard
        .coursesStatus
        .filter(
          (course) => {
            const status =
              normalizeStatus(
                course.status,
              )

            if (
              statusFilter !== ALL
              && status
                !== statusFilter
            ) {
              return false
            }

            const assigned =
              hasInstructor(
                course.instructorName,
              )

            if (
              ownershipFilter
                === "assigned"
              && !assigned
            ) {
              return false
            }

            if (
              ownershipFilter
                === "unassigned"
              && assigned
            ) {
              return false
            }

            if (!keyword) {
              return true
            }

            const searchable =
              [
                course.courseCode,
                course.courseName,
                course.instructorName,
                humanizeStatus(
                  status,
                ),
                workflowLabel(
                  status,
                ),
              ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase()

            return searchable
              .includes(keyword)
          },
        )
        .slice()
        .sort(
          (
            left,
            right,
          ) =>
            String(
              left.courseCode
              ?? "",
            ).localeCompare(
              String(
                right.courseCode
                ?? "",
              ),
              "en",
              {
                numeric: true,
              },
            ),
        )
    }, [
      dashboard,
      ownershipFilter,
      searchTerm,
      statusFilter,
    ])

  const hasFilters =
    Boolean(
      searchTerm.trim(),
    )
    || statusFilter !== ALL
    || ownershipFilter
      !== ALL

  const clearFilters =
    () => {
      setSearchTerm("")
      setStatusFilter(ALL)
      setOwnershipFilter(ALL)
    }

  if (isLoading) {
    return (
      <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
        <div className="h-32 animate-pulse rounded-2xl border border-slate-200 bg-white" />

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({
            length: 4,
          }).map(
            (_, index) => (
              <div
                key={index}
                className="h-28 animate-pulse rounded-xl border border-slate-200 bg-white"
              />
            ),
          )}
        </section>

        <div className="h-80 animate-pulse rounded-xl border border-slate-200 bg-white" />
      </div>
    )
  }

  if (
    isError
    || !dashboard
  ) {
    const message =
      error
        instanceof Error
        ? error.message
        : "Unable to load department courses."

    return (
      <div className="mx-auto flex min-h-[55vh] max-w-2xl items-center justify-center p-6">
        <Card className="w-full border-rose-200 bg-rose-50/70 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-start gap-4">
              <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-700">
                <CircleAlert className="size-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="font-semibold text-slate-900">
                  Unable to load Managed Major Courses
                </p>

                <p className="mt-1 text-sm leading-6 text-slate-600">
                  {message}
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  The course directory is restricted to the department linked to your account. Confirm the department mapping if this error persists.
                </p>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4 gap-2 bg-white"
                  onClick={() =>
                    void refetch()
                  }
                >
                  <RefreshCw className="size-4" />
                  Try Again
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2 text-[#007d84]">
              <BookOpen className="size-5" />

              <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Managed Major Course Oversight
              </span>
            </div>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              Managed Major Courses
            </h1>

            <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
              Read-only academic view of courses in your authorized department, including responsible instructor, latest syllabus status, and current workflow position.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              asChild
              variant="outline"
              className="gap-2"
            >
              <Link to="/dept-head/syllabus">
                <FileText className="size-4" />
                Syllabus Catalog
              </Link>
            </Button>

            <Button
              asChild
              className="gap-2 bg-[#007d84] text-white hover:bg-[#006d73]"
            >
              <Link to="/dept-head/approvals">
                <ClipboardCheck className="size-4" />
                Review Queue
              </Link>
            </Button>

            <Button
              type="button"
              variant="outline"
              className="gap-2"
              onClick={() =>
                void Promise.all([refetch(), refetchPendingReviews()])
              }
              disabled={
                isFetching
              }
            >
              <RefreshCw
                className={`size-4 ${
                  isFetching
                    ? "animate-spin"
                    : ""
                }`}
              />
              Refresh
            </Button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Pending Reviews"
          value={summary.pending}
          description="Department decisions currently required"
          icon={
            <Clock3 className="size-5" />
          }
          tone={
            summary.pending > 0
              ? "warning"
              : "success"
          }
        />
        <MetricCard label="Forwarded to Dean" value={summary.forwarded} description="Department review completed" icon={<ArrowRight className="size-5" />} />
        <MetricCard label="Rejected / Revision" value={summary.rejected} description="Returned to the instructor" icon={<AlertTriangle className="size-5" />} tone={summary.rejected > 0 ? "warning" : "default"} />
        <MetricCard label="Approved" value={summary.approved} description="Final workflow completed" icon={<CheckCircle2 className="size-5" />} tone="success" />
      </section>

      <Card className="overflow-hidden border border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <CardTitle className="text-base font-bold text-[#17343d]">Needs Your Review</CardTitle>
              <p className="mt-1 text-xs text-slate-500">Live pending requests assigned to your managed Major scope.</p>
            </div>
            <Button asChild variant="outline" size="sm"><Link to="/dept-head/approvals">Open Review Queue</Link></Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {pendingReviewsLoading ? (
            <p className="p-5 text-sm text-slate-500">Loading pending reviews...</p>
          ) : pendingReviews.length === 0 ? (
            <p className="p-5 text-sm text-slate-500">No syllabus currently requires your department review.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {pendingReviews.map((review) => (
                <div key={review.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-semibold text-slate-800"><span className="font-mono text-[#007d84]">{review.courseCode}</span> — {review.courseName}</p>
                    <p className="mt-1 text-xs text-slate-500">Instructor: {review.requestedByUsername || "—"} · {review.versionLabel || `v${review.versionNumber}`} · Pending Department Review</p>
                  </div>
                  <Button asChild size="sm" className="bg-[#007d84] text-white hover:bg-[#006d73]">
                    <Link to={`/dept-head/syllabus/${review.syllabusId}`}>Review <ArrowRight className="size-4" /></Link>
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="border border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <CardTitle className="text-base font-bold text-[#17343d]">
                Course Directory
              </CardTitle>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Filter department courses without changing course master data or syllabus ownership.
              </p>
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-500">
              <ShieldCheck className="size-4 text-[#007d84]" />
              Managed-Major read-only view
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-5">
          <div className="grid gap-3 xl:grid-cols-[minmax(300px,1fr)_230px_200px_auto] xl:items-end">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600">
                Search
              </label>

              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

                <Input
                  value={searchTerm}
                  onChange={(event) =>
                    setSearchTerm(
                      event.target.value,
                    )
                  }
                  placeholder="Course code, name, instructor..."
                  className="h-10 pl-9"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600">
                Syllabus Status
              </label>

              <Select
                value={statusFilter}
                onValueChange={
                  setStatusFilter
                }
              >
                <SelectTrigger className="h-10 bg-white">
                  <SelectValue placeholder="All syllabus statuses" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem
                    value={ALL}
                  >
                    All syllabus statuses
                  </SelectItem>

                  {statusOptions.map(
                    (status) => (
                      <SelectItem
                        key={status}
                        value={status}
                      >
                        {humanizeStatus(
                          status,
                        )}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-600">
                Instructor Coverage
              </label>

              <Select
                value={ownershipFilter}
                onValueChange={
                  setOwnershipFilter
                }
              >
                <SelectTrigger className="h-10 bg-white">
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All courses
                  </SelectItem>

                  <SelectItem value="assigned">
                    Assigned
                  </SelectItem>

                  <SelectItem value="unassigned">
                    Not assigned
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button
              type="button"
              variant="outline"
              className="h-10"
              onClick={
                clearFilters
              }
              disabled={!hasFilters}
            >
              Clear
            </Button>
          </div>

          <p className="mt-3 text-xs text-slate-500">
            Showing{" "}
            <span className="font-semibold text-slate-700">
              {filteredCourses.length}
            </span>{" "}
            of{" "}
            <span className="font-semibold text-slate-700">
              {dashboard
                .coursesStatus
                .length}
            </span>{" "}
            department courses.
          </p>
        </CardContent>
      </Card>

      {summary.attention > 0 && (
        <div className="flex flex-col gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 size-5 shrink-0" />

            <div>
              <p className="font-semibold">
                {summary.attention} course(s) require follow-up
              </p>

              <p className="mt-0.5 text-xs leading-5">
                This includes syllabi not created, still in draft, waiting for department review, rejected, or requiring revision.
              </p>
            </div>
          </div>

          {summary.pending > 0 && (
            <Button
              asChild
              variant="outline"
              size="sm"
              className="shrink-0 border-amber-300 bg-white text-amber-800 hover:bg-amber-100"
            >
              <Link to="/dept-head/approvals">
                Review Pending
                <ArrowRight className="size-4" />
              </Link>
            </Button>
          )}
        </div>
      )}

      <Card className="overflow-hidden border border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 bg-white pb-4">
          <CardTitle className="text-base font-bold text-[#17343d]">
            Course & Syllabus Status
          </CardTitle>

          <p className="text-xs leading-5 text-slate-500">
            Each course appears once. Historical versions and review comments remain available from Syllabus Management.
          </p>
        </CardHeader>

        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table className="min-w-[1120px]">
              <TableHeader className="bg-slate-50/80">
                <TableRow>
                  <TableHead className="w-[130px] font-semibold text-slate-600">
                    Course Code
                  </TableHead>

                  <TableHead className="min-w-[300px] font-semibold text-slate-600">
                    Course
                  </TableHead>

                  <TableHead className="min-w-[240px] font-semibold text-slate-600">
                    Responsible Instructor
                  </TableHead>

                  <TableHead className="min-w-[220px] font-semibold text-slate-600">
                    Syllabus Status
                  </TableHead>

                  <TableHead className="min-w-[210px] font-semibold text-slate-600">
                    Workflow Position
                  </TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {filteredCourses.length
                  === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={5}
                      className="h-40 text-center"
                    >
                      <div className="mx-auto max-w-md">
                        <p className="font-semibold text-slate-700">
                          {dashboard
                            .coursesStatus
                            .length
                            === 0
                            ? "No department courses are available."
                            : "No courses match the selected filters."}
                        </p>

                        <p className="mt-1 text-sm leading-6 text-slate-500">
                          {dashboard
                            .coursesStatus
                            .length
                            === 0
                            ? "Confirm that this Head account has the correct Managed Major in User Management and that Teaching Assignments use a Program in that Major."
                            : "Clear the current filters or search for another course or instructor."}
                        </p>

                        {hasFilters && (
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="mt-3"
                            onClick={
                              clearFilters
                            }
                          >
                            Clear Filters
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCourses.map(
                    (
                      course,
                      index,
                    ) => {
                      const status =
                        normalizeStatus(
                          course.status,
                        )

                      const assigned =
                        hasInstructor(
                          course.instructorName,
                        )

                      return (
                        <TableRow
                          key={`${course.courseCode}-${index}`}
                          className="hover:bg-[#f8fbfb]"
                        >
                          <TableCell className="font-mono text-xs font-bold text-[#007d84]">
                            {course
                              .courseCode}
                          </TableCell>

                          <TableCell>
                            <p className="font-semibold text-slate-800">
                              {course
                                .courseName}
                            </p>
                          </TableCell>

                          <TableCell>
                            {assigned ? (
                              <div className="flex items-center gap-2">
                                <span className="flex size-7 items-center justify-center rounded-full bg-[#eef8f8] text-[#007d84]">
                                  <UserCheck className="size-3.5" />
                                </span>

                                <span className="font-medium text-slate-700">
                                  {course
                                    .instructorName}
                                </span>
                              </div>
                            ) : (
                              <div className="flex items-center gap-2 text-amber-700">
                                <AlertTriangle className="size-4" />

                                <span className="font-semibold">
                                  Not assigned
                                </span>
                              </div>
                            )}
                          </TableCell>

                          <TableCell>
                            <StatusBadge
                              status={status}
                            />
                          </TableCell>

                          <TableCell>
                            <WorkflowPosition
                              status={status}
                            />
                          </TableCell>
                        </TableRow>
                      )
                    },
                  )
                )}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col gap-2 border-t border-slate-100 bg-slate-50/50 px-4 py-3 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <span>
              Course master data is read-only for Department Head. Use Syllabus Management for version history and Approvals for pending review decisions.
            </span>

            <div className="flex flex-wrap gap-2">
              <Button
                asChild
                variant="ghost"
                size="sm"
                className="h-8 text-[#007d84] hover:bg-[#eef8f8] hover:text-[#006b72]"
              >
                <Link to="/dept-head/syllabus">
                  Open Syllabus Catalog
                  <ArrowRight className="size-3.5" />
                </Link>
              </Button>

              <Button
                asChild
                variant="ghost"
                size="sm"
                className="h-8 text-[#007d84] hover:bg-[#eef8f8] hover:text-[#006b72]"
              >
                <Link to="/dept-head/approvals">
                  Open Review Queue
                  <ArrowRight className="size-3.5" />
                </Link>
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function StatusBadge({
  status,
}: {
  status: string
}) {
  const label =
    humanizeStatus(status)

  if (
    status === "APPROVED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-emerald-200 bg-emerald-50 text-emerald-700"
      >
        <CheckCircle2 className="mr-1 size-3" />
        {label}
      </Badge>
    )
  }

  if (
    status === "SUBMITTED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-amber-200 bg-amber-50 text-amber-700"
      >
        <Clock3 className="mr-1 size-3" />
        {label}
      </Badge>
    )
  }

  if (
    status
      === "UNDER_REVIEW"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-blue-200 bg-blue-50 text-blue-700"
      >
        <ClipboardCheck className="mr-1 size-3" />
        {label}
      </Badge>
    )
  }

  if (
    status === "REJECTED"
    || status
      === "REVISION_REQUESTED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700"
      >
        <AlertTriangle className="mr-1 size-3" />
        {label}
      </Badge>
    )
  }

  if (
    status
      === "NOT_CREATED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-slate-200 bg-white text-slate-500"
      >
        {label}
      </Badge>
    )
  }

  if (
    status === "DRAFT"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-orange-200 bg-orange-50 text-orange-700"
      >
        <FileText className="mr-1 size-3" />
        {label}
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="border-slate-200 bg-slate-50 text-slate-600"
    >
      {label}
    </Badge>
  )
}

function WorkflowPosition({
  status,
}: {
  status: string
}) {
  const label =
    workflowLabel(status)

  if (
    status === "SUBMITTED"
  ) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-amber-700">
        <Clock3 className="size-3.5" />
        {label}
      </span>
    )
  }

  if (
    status
      === "UNDER_REVIEW"
  ) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-700">
        <ArrowRight className="size-3.5" />
        {label}
      </span>
    )
  }

  if (
    status === "APPROVED"
  ) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-700">
        <CheckCircle2 className="size-3.5" />
        {label}
      </span>
    )
  }

  if (
    status === "REJECTED"
    || status
      === "REVISION_REQUESTED"
  ) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-700">
        <AlertTriangle className="size-3.5" />
        {label}
      </span>
    )
  }

  return (
    <span className="text-xs font-medium text-slate-500">
      {label}
    </span>
  )
}

function MetricCard({
  label,
  value,
  description,
  icon,
  tone = "default",
}: {
  label: string
  value: number
  description: string
  icon: ReactNode
  tone?:
    | "default"
    | "success"
    | "warning"
}) {
  const style =
    tone === "success"
      ? {
          value:
            "text-emerald-700",
          icon:
            "bg-emerald-50 text-emerald-700",
        }
      : tone === "warning"
        ? {
            value:
              "text-amber-700",
            icon:
              "bg-amber-50 text-amber-700",
          }
        : {
            value:
              "text-slate-900",
            icon:
              "bg-[#eef8f8] text-[#007d84]",
          }

  return (
    <Card className="border border-slate-200 bg-white shadow-sm">
      <CardContent className="flex items-start justify-between gap-4 p-5">
        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
            {label}
          </p>

          <p className={`mt-2 text-3xl font-bold ${style.value}`}>
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>

        <span className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${style.icon}`}>
          {icon}
        </span>
      </CardContent>
    </Card>
  )
}
