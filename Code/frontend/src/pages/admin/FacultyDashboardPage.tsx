import {
  useQuery,
} from "@tanstack/react-query"
import {
  CircleAlert,
  GraduationCap,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
} from "lucide-react"

import {
  getMyActiveAssignments,
} from "@/api/classSectionApi"
import {
  dashboardApi,
} from "@/api/dashboardApi"
import InstructorCourseWorklist from "@/components/instructor/InstructorCourseWorklist"
import {
  Badge,
} from "@/components/ui/badge"
import {
  Button,
} from "@/components/ui/button"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import {
  useAuthStore,
} from "@/store/authStore"

function formatDateTime(
  value:
    | string
    | null
    | undefined,
) {
  if (!value) {
    return "Not available"
  }

  const date =
    new Date(value)

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return value
  }

  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    },
  ).format(date)
}

export default function FacultyDashboardPage() {
  const user =
    useAuthStore(
      (state) =>
        state.user,
    )

  const dashboardQuery =
    useQuery({
      queryKey: [
        "faculty-dashboard",
        "me",
        user?.userId,
      ],
      queryFn:
        dashboardApi
          .getMyFacultyDashboard,
      enabled:
        user?.role
          === "INSTRUCTOR",
      staleTime: 30_000,
      refetchInterval: 60_000,
      refetchOnMount:
        "always",
    })

  const assignmentQuery =
    useQuery({
      queryKey: [
        "my-active-assignments",
      ],
      queryFn:
        getMyActiveAssignments,
      enabled:
        user?.role
          === "INSTRUCTOR",
      staleTime: 30_000,
      refetchInterval: 60_000,
      refetchOnMount:
        "always",
    })

  const refreshAll =
    async () => {
      await Promise.all([
        dashboardQuery.refetch(),
        assignmentQuery.refetch(),
      ])
    }

  if (
    dashboardQuery.isLoading
    || assignmentQuery.isLoading
  ) {
    return (
      <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
        <div className="h-28 animate-pulse rounded-xl border border-slate-200 bg-white" />

        <div className="h-[460px] animate-pulse rounded-xl border border-slate-200 bg-white" />
      </div>
    )
  }

  if (
    dashboardQuery.isError
    || assignmentQuery.isError
    || !dashboardQuery.data
  ) {
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
                  Unable to load Instructor Dashboard
                </p>

                <p className="mt-1 text-sm leading-6 text-slate-600">
                  The system could not load the signed-in Instructor profile or assigned courses.
                </p>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4 bg-white"
                  onClick={() =>
                    void refreshAll()
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

  const dashboard =
    dashboardQuery.data

  const assignments =
    assignmentQuery.data
    ?? []

  const isRefreshing =
    dashboardQuery.isFetching
    || assignmentQuery.isFetching

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <Card className="border border-[#cfe1e4] bg-white shadow-sm">
        <CardContent className="p-5">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-start gap-4">
              <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#eaf7f7] text-[#007d84]">
                <GraduationCap className="size-6" />
              </span>

              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-lg font-bold text-[#17343d]">
                    {dashboard.instructorName}

                    {dashboard.staffCode
                      ? ` · ${dashboard.staffCode}`
                      : ""}
                  </p>

                  <Badge
                    variant="outline"
                    className="border-emerald-200 bg-emerald-50 text-emerald-700"
                  >
                    <ShieldCheck className="mr-1 size-3" />
                    Signed-in Instructor
                  </Badge>
                </div>

                <p className="mt-1 text-sm text-slate-500">
                  {dashboard.departmentCode
                    || "Department not assigned"}

                  {dashboard.departmentName
                    ? ` · ${dashboard.departmentName}`
                    : ""}
                </p>

                <p className="mt-2 text-xs text-slate-500">
                  This dashboard shows assignments belonging to the currently authenticated Instructor account.
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="text-xs leading-5 text-slate-500">
                <p>
                  Last synchronized:{" "}
                  <strong className="font-semibold text-slate-700">
                    {formatDateTime(
                      dashboard.generatedAt,
                    )}
                  </strong>
                </p>

                <p>
                  Time zone:{" "}
                  <strong className="font-semibold text-slate-700">
                    {dashboard.timeZone}
                  </strong>
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                disabled={
                  isRefreshing
                }
                onClick={() =>
                  void refreshAll()
                }
              >
                {isRefreshing ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <RefreshCw className="size-4" />
                )}

                Refresh
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <InstructorCourseWorklist
        assignments={
          assignments
        }
        title="Assigned Courses"
        description="Courses assigned to you and their current syllabus status. Use Major, Cohort, Semester, and Status to refine the list."
      />
    </div>
  )
}