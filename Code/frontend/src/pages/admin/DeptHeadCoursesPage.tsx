import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import {
  BookOpen,
  CheckCircle2,
  CircleAlert,
  RefreshCw,
  Search,
  SlidersHorizontal,
} from "lucide-react"

import { dashboardApi } from "@/api/dashboardApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
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
  SUBMITTED: "Pending DeptHead Review",
  UNDER_REVIEW: "Forwarded to Dean",
  APPROVED: "Approved",
  REJECTED: "Rejected / Revision Required",
  REVISION_REQUESTED: "Revision Requested",
  ARCHIVED: "Archived",
}

function humanizeStatus(status: string) {
  return (
    STATUS_LABELS[status]
    ?? status
      .replaceAll("_", " ")
      .toLowerCase()
      .replace(/\b\w/g, (character) => character.toUpperCase())
  )
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status?.toUpperCase() || "NOT_CREATED"
  const label = humanizeStatus(normalized)

  if (normalized === "APPROVED") {
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

  if (normalized === "SUBMITTED") {
    return (
      <Badge
        variant="outline"
        className="border-amber-200 bg-amber-50 text-amber-700"
      >
        {label}
      </Badge>
    )
  }

  if (normalized === "UNDER_REVIEW") {
    return (
      <Badge
        variant="outline"
        className="border-blue-200 bg-blue-50 text-blue-700"
      >
        {label}
      </Badge>
    )
  }

  if (
    normalized === "REJECTED"
    || normalized === "REVISION_REQUESTED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700"
      >
        {label}
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="border-slate-200 bg-slate-50 text-slate-700"
    >
      {label}
    </Badge>
  )
}

export default function DeptHeadCoursesPage() {
  const { user } = useAuthStore()
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState(ALL)

  const {
    data: dashboard,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ["depthead-courses", user?.userId],
    queryFn: () =>
      dashboardApi.getDeptHeadDashboard(user?.userId ?? 0),
    enabled: Boolean(user?.userId),
  })

  const statusOptions = useMemo(() => {
    if (!dashboard) return []

    return Array.from(
      new Set(
        dashboard.coursesStatus.map(
          (course) => course.status?.toUpperCase() || "NOT_CREATED",
        ),
      ),
    ).sort()
  }, [dashboard])

  const filteredCourses = useMemo(() => {
    if (!dashboard) return []

    const keyword = searchTerm.trim().toLowerCase()

    return dashboard.coursesStatus.filter((course) => {
      const status = course.status?.toUpperCase() || "NOT_CREATED"

      if (statusFilter !== ALL && status !== statusFilter) {
        return false
      }

      if (!keyword) return true

      return [
        course.courseCode,
        course.courseName,
        course.instructorName,
        humanizeStatus(status),
      ]
        .join(" ")
        .toLowerCase()
        .includes(keyword)
    })
  }, [dashboard, searchTerm, statusFilter])

  if (isLoading) {
    return (
      <div className="p-8 text-center text-sm text-slate-500">
        Loading department courses...
      </div>
    )
  }

  if (isError || !dashboard) {
    const message =
      error instanceof Error
        ? error.message
        : "Unable to load department courses."

    return (
      <Card className="border-rose-200 bg-rose-50">
        <CardContent className="flex items-start gap-3 p-6 text-rose-800">
          <CircleAlert className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">
              Unable to load department courses
            </p>
            <p className="mt-1 text-sm text-rose-700">
              {message}
            </p>
          </div>
        </CardContent>
      </Card>
    )
  }

  const clearFilters = () => {
    setSearchTerm("")
    setStatusFilter(ALL)
  }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-[#007b83]">
            <BookOpen className="size-5" />
            <span className="text-xs font-semibold uppercase tracking-[0.12em]">
              Course Directory
            </span>
          </div>

          <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
            Department Courses
          </h1>

          <p className="mt-1 text-sm text-slate-500">
            Browse and filter courses assigned to your department.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          onClick={() => refetch()}
          disabled={isFetching}
        >
          <RefreshCw
            className={
              isFetching
                ? "size-4 animate-spin"
                : "size-4"
            }
          />
          Refresh
        </Button>
      </section>

      <section className="grid gap-3 sm:grid-cols-3">
        <CompactStat
          label="Total Courses"
          value={dashboard.totalCoursesInDept}
        />

        <CompactStat
          label="Assigned"
          value={
            dashboard.coursesStatus.filter(
              (course) =>
                Boolean(course.instructorName)
                && course.instructorName !== "Not assigned",
            ).length
          }
        />

        <CompactStat
          label="Approved"
          value={
            dashboard.coursesStatus.filter(
              (course) => course.status?.toUpperCase() === "APPROVED",
            ).length
          }
        />
      </section>

      <Card className="border border-slate-200 shadow-sm">
        <CardContent className="p-5">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

              <Input
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Search course code, name, or instructor..."
                className="pl-9"
              />
            </div>

            <div className="flex items-center gap-2">
              <SlidersHorizontal className="size-4 text-slate-400" />

              <Select
                value={statusFilter}
                onValueChange={setStatusFilter}
              >
                <SelectTrigger className="w-[220px]">
                  <SelectValue placeholder="All syllabus statuses" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All syllabus statuses
                  </SelectItem>

                  {statusOptions.map((status) => (
                    <SelectItem
                      key={status}
                      value={status}
                    >
                      {humanizeStatus(status)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Button
                type="button"
                variant="ghost"
                onClick={clearFilters}
              >
                Clear
              </Button>
            </div>
          </div>

          <p className="mt-3 text-xs text-slate-500">
            Showing {filteredCourses.length} of {dashboard.coursesStatus.length} courses.
          </p>
        </CardContent>
      </Card>

      <Card className="overflow-hidden border border-slate-200 shadow-sm">
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table className="min-w-[950px]">
              <TableHeader className="bg-slate-50">
                <TableRow>
                  <TableHead>Course Code</TableHead>
                  <TableHead>Course Name</TableHead>
                  <TableHead>Responsible Instructor</TableHead>
                  <TableHead>Syllabus Status</TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {filteredCourses.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={4}
                      className="h-36 text-center"
                    >
                      <p className="font-semibold text-slate-700">
                        No courses found
                      </p>

                      <p className="mt-1 text-sm text-slate-500">
                        Try changing your search or status filter.
                      </p>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCourses.map((course) => (
                    <TableRow
                      key={course.courseCode}
                      className="hover:bg-[#f8fbfb]"
                    >
                      <TableCell className="font-mono text-xs font-bold text-[#007b83]">
                        {course.courseCode}
                      </TableCell>

                      <TableCell className="font-semibold text-slate-800">
                        {course.courseName}
                      </TableCell>

                      <TableCell>
                        {course.instructorName === "Not assigned" ? (
                          <span className="font-medium text-amber-700">
                            Not assigned
                          </span>
                        ) : (
                          <span className="text-slate-700">
                            {course.instructorName || "Not assigned"}
                          </span>
                        )}
                      </TableCell>

                      <TableCell>
                        <StatusBadge status={course.status} />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function CompactStat({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-1 text-2xl font-bold text-slate-900">
        {value}
      </p>
    </div>
  )
}
