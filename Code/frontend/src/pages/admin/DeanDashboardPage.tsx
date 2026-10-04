import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  AlertTriangle,
  BookOpen,
  ChevronRight,
  ClipboardCheck,
  FileWarning,
  RefreshCw,
  ShieldCheck,
} from "lucide-react"

import { dashboardApi } from "@/api/dashboardApi"
import { approvalRequestApi } from "@/api/approvalRequestApi"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const ALL_SEMESTERS = "ALL"

export default function DeanDashboardPage() {
  const navigate = useNavigate()

  const [majorId, setMajorId] =
    useState<number | undefined>()

  const [cohortId, setCohortId] =
    useState<number | undefined>()

  const [semester, setSemester] =
    useState<number | undefined>()

  const dashboardQuery = useQuery({
    queryKey: [
      "dean-dashboard",
      majorId,
      cohortId,
      semester,
    ],
    queryFn: () =>
      dashboardApi.getDeanDashboard({
        majorId,
        cohortId,
        semester,
      }),
    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  const finalReviewQuery = useQuery({
    queryKey: [
      "approval-requests",
      "pending",
      "STEP3_DEAN",
    ],
    queryFn: () =>
      approvalRequestApi.getPendingByStep(
        "STEP3_DEAN",
      ),
    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const dashboard = dashboardQuery.data
  const pendingFinalReviews =
    finalReviewQuery.data ?? []

  if (
    dashboardQuery.isLoading
    && !dashboard
  ) {
    return (
      <div className="space-y-5">
        <div className="h-32 animate-pulse rounded-2xl bg-slate-100" />

        <div className="grid gap-4 md:grid-cols-4">
          {[1, 2, 3, 4].map((item) => (
            <div
              key={item}
              className="h-28 animate-pulse rounded-xl bg-slate-100"
            />
          ))}
        </div>
      </div>
    )
  }

  if (!dashboard) {
    return (
      <Card className="border-rose-200 bg-rose-50">
        <CardContent className="flex flex-col items-center gap-4 p-8 text-center">
          <AlertTriangle className="size-8 text-rose-600" />

          <div>
            <h2 className="font-semibold text-slate-900">
              Unable to load Dean Dashboard
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              Please refresh the dashboard.
            </p>
          </div>

          <Button
            onClick={() =>
              dashboardQuery.refetch()
            }
          >
            <RefreshCw className="size-4" />
            Refresh
          </Button>
        </CardContent>
      </Card>
    )
  }

  const summary = dashboard.summary

  const selectedMajor =
    majorId
    ?? dashboard.scope.majorId
    ?? undefined

  const selectedCohort =
    cohortId
    ?? dashboard.scope.cohortId
    ?? undefined

  return (
    <div
      data-admin-page="DeanDashboardPage"
      className="space-y-5 pb-8"
    >
      {/* =====================================================
          HEADER
         ===================================================== */}
      <Card className="border-[#d6e1e6] bg-white shadow-sm">
        <CardContent className="flex flex-col gap-4 p-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#007d84]">
              SCSE / Academic Oversight
            </p>

            <h1 className="mt-1 text-2xl font-bold text-slate-950">
              Dean Dashboard
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              Monitor syllabus readiness and complete final academic approvals.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              variant="outline"
              onClick={() =>
                navigate("/dean/syllabus")
              }
            >
              <BookOpen className="size-4" />
              Syllabus Catalog
            </Button>

            <Button
              className="bg-[#007d84] text-white hover:bg-[#006d74]"
              onClick={() =>
                navigate("/dean/approvals")
              }
            >
              <ClipboardCheck className="size-4" />
              Final Review Queue
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* =====================================================
          FILTERS
         ===================================================== */}
      <Card className="border-[#d6e1e6] bg-white shadow-none">
        <CardContent className="p-5">
          <div className="mb-4">
            <h2 className="font-semibold text-slate-900">
              Academic Scope
            </h2>

            <p className="text-xs text-slate-500">
              Select the curriculum scope you want to monitor.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-3">
            <FilterField label="Major">
              <Select
                value={
                  selectedMajor
                    ? String(selectedMajor)
                    : undefined
                }
                onValueChange={(value) => {
                  setMajorId(Number(value))
                  setCohortId(undefined)
                  setSemester(undefined)
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select major" />
                </SelectTrigger>

                <SelectContent>
                  {dashboard.majors.map(
                    (major) => (
                      <SelectItem
                        key={major.id}
                        value={String(major.id)}
                      >
                        {major.code} — {major.name}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </FilterField>

            <FilterField label="Cohort">
              <Select
                value={
                  selectedCohort
                    ? String(selectedCohort)
                    : undefined
                }
                onValueChange={(value) => {
                  setCohortId(Number(value))
                  setSemester(undefined)
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select cohort" />
                </SelectTrigger>

                <SelectContent>
                  {dashboard.cohorts.map(
                    (cohort) => (
                      <SelectItem
                        key={cohort.id}
                        value={String(cohort.id)}
                      >
                        {cohort.name}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </FilterField>

            <FilterField label="Semester">
              <Select
                value={
                  semester
                    ? String(semester)
                    : ALL_SEMESTERS
                }
                onValueChange={(value) =>
                  setSemester(
                    value === ALL_SEMESTERS
                      ? undefined
                      : Number(value),
                  )
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem
                    value={ALL_SEMESTERS}
                  >
                    All Semesters
                  </SelectItem>

                  {dashboard.semesters.map(
                    (item) => (
                      <SelectItem
                        key={item.value}
                        value={String(
                          item.value,
                        )}
                      >
                        {item.label}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </FilterField>
          </div>
        </CardContent>
      </Card>

      {/* =====================================================
          DEAN KPIs
         ===================================================== */}
      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Courses"
          value={summary.expectedCourses}
          description={`${summary.expectedCredits} curriculum credits`}
          icon={
            <BookOpen className="size-5" />
          }
          iconClass="bg-slate-100 text-slate-700"
        />

        <MetricCard
          label="Approved"
          value={summary.approvedSyllabuses}
          description={`${formatPercent(
            summary.approvalRate,
          )} approved`}
          icon={
            <ShieldCheck className="size-5" />
          }
          iconClass="bg-emerald-50 text-emerald-700"
        />

        <MetricCard
          label="Final Review Pending"
          value={pendingFinalReviews.length}
          description="Waiting for Dean decision"
          icon={
            <ClipboardCheck className="size-5" />
          }
          iconClass="bg-blue-50 text-blue-700"
        />

        <MetricCard
          label="Missing Syllabi"
          value={summary.missingSyllabuses}
          description="Courses without syllabus"
          icon={
            <FileWarning className="size-5" />
          }
          iconClass="bg-amber-50 text-amber-700"
        />
      </section>

      {/* =====================================================
          ONLY ACTION-ORIENTED SECTION
         ===================================================== */}
      <Card className="overflow-hidden border-[#d6e1e6] bg-white shadow-none">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-[#007d84]">
              Final Academic Review
            </p>

            <h2 className="mt-1 text-lg font-semibold text-slate-950">
              Syllabi Waiting for Dean
            </h2>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() =>
              navigate("/dean/approvals")
            }
          >
            View All
            <ChevronRight className="size-4" />
          </Button>
        </div>

        <CardContent className="p-0">
          {pendingFinalReviews.length === 0 ? (
            <div className="py-12 text-center">
              <ShieldCheck className="mx-auto mb-3 size-8 text-emerald-500" />

              <p className="font-medium text-slate-800">
                No syllabus is waiting for final approval.
              </p>

              <p className="mt-1 text-sm text-slate-500">
                All current Dean review items have been processed.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {pendingFinalReviews
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
                          {item.cohortName ?? "—"}
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
                          `/dean/syllabus/${item.syllabusId}`,
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

function FilterField({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
        {label}
      </p>

      {children}
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

function formatPercent(
  value: number | null | undefined,
) {
  if (
    value === null
    || value === undefined
    || Number.isNaN(value)
  ) {
    return "0%"
  }

  return `${value.toLocaleString(
    "vi-VN",
    {
      maximumFractionDigits: 1,
    },
  )}%`
}
