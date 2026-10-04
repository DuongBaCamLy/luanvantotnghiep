import { useMemo } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  ChevronRight,
  ClipboardCheck,
  RefreshCw,
  Send,
} from "lucide-react"

import { dashboardApi } from "@/api/dashboardApi"
import { approvalRequestApi } from "@/api/approvalRequestApi"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import { useAuthStore } from "@/store/authStore"

export default function DeptHeadDashboardPage() {
  const navigate = useNavigate()

  const user =
    useAuthStore((state) => state.user)

  const dashboardQuery = useQuery({
    queryKey: [
      "dept-head-dashboard",
      user?.userId,
    ],

    queryFn: () =>
      dashboardApi.getDeptHeadDashboard(
        user?.userId ?? 0,
      ),

    enabled: Boolean(user?.userId),

    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  const reviewQuery = useQuery({
    queryKey: [
      "approval-requests",
      "pending",
      "STEP1_DEPT_HEAD",
      user?.userId,
    ],

    queryFn: () =>
      approvalRequestApi.getPendingByStep(
        "STEP1_DEPT_HEAD",
      ),

    enabled: Boolean(user?.userId),

    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const dashboard =
    dashboardQuery.data

  const pendingReviews =
    reviewQuery.data ?? []

  const summary = useMemo(() => {
    const courses =
      dashboard?.coursesStatus ?? []

    return {
      total:
        dashboard?.totalCoursesInDept
        ?? 0,

      pending:
        pendingReviews.length,

      forwarded:
        courses.filter(
          (course) =>
            String(course.status ?? "")
              .toUpperCase()
              === "UNDER_REVIEW",
        ).length,

      approved:
        courses.filter(
          (course) =>
            String(course.status ?? "")
              .toUpperCase()
              === "APPROVED",
        ).length,
    }
  }, [
    dashboard,
    pendingReviews,
  ])

  if (
    dashboardQuery.isLoading
    && !dashboard
  ) {
    return (
      <div className="space-y-5">
        <div className="h-32 animate-pulse rounded-2xl bg-slate-100" />

        <div className="grid gap-4 md:grid-cols-4">
          {[1, 2, 3, 4].map(
            (item) => (
              <div
                key={item}
                className="h-28 animate-pulse rounded-xl bg-slate-100"
              />
            ),
          )}
        </div>
      </div>
    )
  }

  if (
    dashboardQuery.isError
    || !dashboard
  ) {
    return (
      <div className="mx-auto flex min-h-[55vh] max-w-xl items-center justify-center">
        <Card className="w-full border-rose-200 bg-rose-50">
          <CardContent className="space-y-4 p-6 text-center">
            <AlertTriangle className="mx-auto size-9 text-rose-600" />

            <div>
              <h2 className="font-semibold text-slate-900">
                Unable to load Department Dashboard
              </h2>

              <p className="mt-1 text-sm text-slate-600">
                Check the managed Major assignment
                and try again.
              </p>
            </div>

            <Button
              variant="outline"
              onClick={() =>
                dashboardQuery.refetch()
              }
            >
              <RefreshCw className="size-4" />
              Try Again
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div
      data-admin-page="DeptHeadDashboardPage"
      className="space-y-5 pb-8"
    >
      {/* HEADER */}
      <Card className="border-[#d6e1e6] bg-white shadow-sm">
        <CardContent className="flex flex-col gap-4 p-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#007d84]">
              SCSE / Department Oversight
            </p>

            <h1 className="mt-1 text-2xl font-bold text-slate-950">
              Head of Department Dashboard
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              Monitor syllabi in your managed Major
              and complete department-level reviews.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              variant="outline"
              onClick={() =>
                navigate(
                  "/dept-head/class-sections",
                )
              }
            >
              <BookOpen className="size-4" />
              Managed Courses
            </Button>

            <Button
              className="bg-[#007d84] text-white hover:bg-[#006d74]"
              onClick={() =>
                navigate(
                  "/dept-head/approvals",
                )
              }
            >
              <ClipboardCheck className="size-4" />
              Review Queue
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* KPI */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Managed Courses"
          value={summary.total}
          description="Courses in your managed Major"
          icon={
            <BookOpen className="size-5" />
          }
          iconClass="bg-slate-100 text-slate-700"
        />

        <MetricCard
          label="Pending Review"
          value={summary.pending}
          description="Waiting for your decision"
          icon={
            <ClipboardCheck className="size-5" />
          }
          iconClass="bg-amber-50 text-amber-700"
        />

        <MetricCard
          label="Forwarded to Dean"
          value={summary.forwarded}
          description="Department review completed"
          icon={
            <Send className="size-5" />
          }
          iconClass="bg-blue-50 text-blue-700"
        />

        <MetricCard
          label="Approved"
          value={summary.approved}
          description="Final approval completed"
          icon={
            <CheckCircle2 className="size-5" />
          }
          iconClass="bg-emerald-50 text-emerald-700"
        />
      </section>

      {/* REVIEW QUEUE */}
      <Card className="overflow-hidden border-[#d6e1e6] bg-white shadow-none">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[#007d84]">
              Department Review
            </p>

            <h2 className="mt-1 text-lg font-semibold text-slate-950">
              Syllabi Waiting for Your Review
            </h2>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              navigate(
                "/dept-head/approvals",
              )
            }
          >
            View All
            <ChevronRight className="size-4" />
          </Button>
        </div>

        <CardContent className="p-0">
          {pendingReviews.length === 0 ? (
            <div className="py-12 text-center">
              <CheckCircle2 className="mx-auto mb-3 size-8 text-emerald-500" />

              <p className="font-medium text-slate-800">
                No syllabus is waiting for department review.
              </p>

              <p className="mt-1 text-sm text-slate-500">
                Your department review queue is clear.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {pendingReviews
                .slice(0, 5)
                .map((item) => (
                  <div
                    key={item.id}
                    className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-sm font-bold text-[#007d84]">
                          {item.courseCode}
                        </span>

                        <span className="text-xs text-slate-400">
                          {item.cohortName
                            ?? "—"}
                        </span>
                      </div>

                      <p className="mt-1 truncate font-medium text-slate-900">
                        {item.courseName}
                      </p>

                      <p className="mt-1 text-xs text-slate-500">
                        Instructor:{" "}
                        {item.instructorUsername
                          ?? "—"}
                      </p>
                    </div>

                    <Button
                      size="sm"
                      onClick={() =>
                        navigate(
                          `/dept-head/syllabus/${item.syllabusId}`,
                        )
                      }
                    >
                      Review
                      <ChevronRight className="size-4" />
                    </Button>
                  </div>
                ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function MetricCard({
  label,
  value,
  description,
  icon,
  iconClass,
}: {
  label: string
  value: number
  description: string
  icon: React.ReactNode
  iconClass: string
}) {
  return (
    <Card className="border-[#d6e1e6] bg-white shadow-none">
      <CardContent className="flex items-start justify-between p-5">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-400">
            {label}
          </p>

          <p className="mt-2 text-3xl font-semibold text-slate-950">
            {value}
          </p>

          <p className="mt-1 text-xs text-slate-500">
            {description}
          </p>
        </div>

        <span
          className={`flex size-10 items-center justify-center rounded-xl ${iconClass}`}
        >
          {icon}
        </span>
      </CardContent>
    </Card>
  )
}
