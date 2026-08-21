import { useEffect, useMemo, useRef } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate, useSearchParams } from "react-router-dom"
import {
  Activity,
  AlertCircle,
  ArrowRight,
  BookOpenCheck,
  CalendarCheck,
  CheckCircle2,
  Clock3,
  FileWarning,
  GraduationCap,
  Layers3,
  PieChart as PieChartIcon,
  Target,
  Users,
} from "lucide-react"
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts"

import { auditLogApi } from "@/api/auditLogApi"
import { cohortApi } from "@/api/cohortApi"
import { dashboardApi } from "@/api/dashboardApi"
import { programApi } from "@/api/programApi"
import DashboardHero from "@/components/dashboard/DashboardHero"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
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

const ALL_TERMS = "__all__"
const ALL_PROGRAMS = "__all_programs__"
const ALL_COHORTS = "__all_cohorts__"
const DASHBOARD_TARGET_MS = 2_000
const RECENT_AUDIT_LOG_LIMIT = 8
const MISSING_ROW_LIMIT = 8

const syllabusStatusColors = [
  "#94a3b8",
  "#64748b",
  "#0ea5e9",
  "#6366f1",
  "#f59e0b",
  "#e11d48",
  "#10b981",
  "#475569",
]

const creditChartColors = [
  "#006b72",
  "#f0aa39",
  "#4fa3a7",
  "#7ebbc0",
  "#d88e2d",
  "#9ab3b8",
]

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    MISSING: "Missing",
    DRAFT: "Draft",
    SUBMITTED: "Submitted",
    UNDER_REVIEW: "Under Review",
    REVISION_REQUESTED: "Revision Requested",
    REJECTED: "Rejected",
    APPROVED: "Approved",
    ARCHIVED: "Archived",
    UNKNOWN: "Unknown",
  }

  return labels[status] ?? status
}

function actionLabel(action: string) {
  return action
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/(^|\s)\S/g, (value) => value.toUpperCase())
}

function clampPercentage(value: number) {
  return Math.max(0, Math.min(100, Number.isFinite(value) ? value : 0))
}

function ProgressMetric({
  label,
  value,
  note,
}: {
  label: string
  value: number
  note: string
}) {
  const percentage = clampPercentage(value)

  return (
    <div className="space-y-2">
      <div className="flex items-end justify-between gap-3">
        <div>
          <p className="text-[11px] font-semibold text-slate-700">{label}</p>
          <p className="mt-0.5 text-[10px] text-slate-500">{note}</p>
        </div>
        <span className="text-sm font-bold text-[#006b72]">
          {percentage.toFixed(1)}%
        </span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-slate-100">
        <div
          className="h-full rounded-full bg-[#0b858c] transition-all"
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  )
}

export default function AdminDashboardPage() {
  const navigate = useNavigate()
  const pageStartedAtRef = useRef<number | null>(null)
  const measuredDataUpdatedAtRef = useRef<number | null>(null)
  const [searchParams, setSearchParams] = useSearchParams()

  useEffect(() => {
    pageStartedAtRef.current = performance.now()
  }, [])

  const academicYearFromUrl = searchParams.get("academicYear") ?? undefined
  const semesterFromUrl = searchParams.get("semester")
  const parsedSemester = semesterFromUrl === null ? undefined : Number(semesterFromUrl)
  const semesterNumber = Number.isFinite(parsedSemester) ? parsedSemester : undefined

  const parsedProgramId = Number(searchParams.get("programId"))
  const programId =
    Number.isFinite(parsedProgramId) && parsedProgramId > 0
      ? parsedProgramId
      : undefined

  const parsedCohortId = Number(searchParams.get("cohortId"))
  const cohortId =
    programId && Number.isFinite(parsedCohortId) && parsedCohortId > 0
      ? parsedCohortId
      : undefined

  const query = useQuery({
    queryKey: [
      "admin-dashboard",
      academicYearFromUrl ?? null,
      semesterNumber ?? null,
      programId ?? null,
      cohortId ?? null,
    ],
    queryFn: () =>
      dashboardApi.getAdminDashboard({
        academicYear: academicYearFromUrl,
        semester: semesterNumber,
        programId,
        cohortId,
      }),
  })

  const programsQuery = useQuery({
    queryKey: ["programs", "admin-dashboard-filter"],
    queryFn: programApi.getAll,
  })

  const cohortsQuery = useQuery({
    queryKey: ["cohorts", "admin-dashboard-filter", programId ?? null],
    queryFn: () => cohortApi.getByProgram(programId!),
    enabled: Boolean(programId),
  })

  const creditDistributionQuery = useQuery({
    queryKey: ["credit-distribution", programId ?? null, cohortId ?? null],
    queryFn: () => dashboardApi.getCreditDistribution(programId!, cohortId!),
    enabled: Boolean(programId && cohortId),
  })

  const dashboard = query.data

  const heatmapQuery = useQuery({
    queryKey: [
      "admin-dashboard",
      "clo-plo-summary",
      programId ?? null,
      cohortId ?? null,
      dashboard?.academicYear ?? null,
      dashboard?.semester ?? null,
    ],
    queryFn: () =>
      dashboardApi.getHeatmapCoverage(programId!, {
        cohortId: cohortId!,
        academicYear: dashboard!.academicYear!,
        semester: String(dashboard!.semester),
      }),
    enabled: Boolean(
      programId &&
        cohortId &&
        dashboard?.academicYear &&
        dashboard.semester != null,
    ),
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })

  const {
    data: auditLogs,
    isLoading: isLoadingLogs,
    isError: isAuditLogError,
  } = useQuery({
    queryKey: ["audit-logs", "recent", RECENT_AUDIT_LOG_LIMIT],
    queryFn: () => auditLogApi.getRecent(RECENT_AUDIT_LOG_LIMIT),
    enabled: query.isSuccess,
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })

  const syllabusStatusData = useMemo(() => {
    const overview = dashboard?.syllabusStatusOverview
    if (!overview) return []

    const order = [
      "MISSING",
      "DRAFT",
      "SUBMITTED",
      "UNDER_REVIEW",
      "REVISION_REQUESTED",
      "REJECTED",
      "APPROVED",
      "ARCHIVED",
    ]

    return order.map((status) => ({
      status,
      name: statusLabel(status),
      value: overview[status] ?? 0,
    }))
  }, [dashboard])

  const courseGroupProgressData = useMemo(
    () =>
      dashboard?.courseGroupStatistics.map((group) => ({
        code: group.courseTypeCode,
        name: group.courseTypeName || group.courseTypeNameVn || group.courseTypeCode,
        submitted: group.submittedCourses,
        missing: group.notSubmittedCourses,
        overdue: group.overdueCourses,
        approval: group.submissionRate,
      })) ?? [],
    [dashboard],
  )

  const missingRows = useMemo(() => {
    const rows =
      dashboard?.facultiesNotSubmitted.flatMap((faculty) =>
        faculty.missingCourses.map((course) => ({
          key: `${faculty.instructorId}-${course.courseId}`,
          instructorId: faculty.instructorId,
          instructorName: faculty.instructorName,
          staffCode: faculty.staffCode,
          email: faculty.email,
          department:
            faculty.departmentCode ?? faculty.departmentName ?? "—",
          courseId: course.courseId,
          courseCode: course.courseCode,
          courseName: course.courseName || course.courseNameVn || course.courseCode,
          sectionCount: course.sectionCount,
          latestStatus: course.latestStatus,
          versionLabel: course.versionLabel,
          overdue: course.overdue,
        })),
      ) ?? []

    return rows
      .sort((a, b) => Number(b.overdue) - Number(a.overdue))
      .slice(0, MISSING_ROW_LIMIT)
  }, [dashboard])

  useEffect(() => {
    if (!dashboard) return

    const resolvedAcademicYear = dashboard.academicYear
    const resolvedSemester = dashboard.semester

    if (!resolvedAcademicYear || resolvedSemester == null) return
    if (academicYearFromUrl && semesterNumber != null) return

    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current)
        next.set("academicYear", resolvedAcademicYear)
        next.set("semester", String(resolvedSemester))
        return next
      },
      { replace: true },
    )
  }, [dashboard, academicYearFromUrl, semesterNumber, setSearchParams])

  useEffect(() => {
    if (
      !query.isSuccess ||
      !dashboard ||
      query.dataUpdatedAt <= 0 ||
      measuredDataUpdatedAtRef.current === query.dataUpdatedAt
    ) {
      return
    }

    measuredDataUpdatedAtRef.current = query.dataUpdatedAt

    let firstFrame = 0
    let secondFrame = 0

    firstFrame = window.requestAnimationFrame(() => {
      secondFrame = window.requestAnimationFrame(() => {
        const startedAt = pageStartedAtRef.current ?? performance.now()
        const durationMs = performance.now() - startedAt

        window.__LVTN_DASHBOARD_PERF__ = {
          durationMs: Number(durationMs.toFixed(2)),
          measuredAt: new Date().toISOString(),
          targetMs: DASHBOARD_TARGET_MS,
          passed: durationMs < DASHBOARD_TARGET_MS,
          academicYear: dashboard.academicYear,
          semester: dashboard.semester,
        }

        if (import.meta.env.DEV) {
          console.info(
            "[NFR-01.1] Dashboard performance",
            window.__LVTN_DASHBOARD_PERF__,
          )
        }
      })
    })

    return () => {
      window.cancelAnimationFrame(firstFrame)
      window.cancelAnimationFrame(secondFrame)
    }
  }, [query.isSuccess, query.dataUpdatedAt, dashboard])

  const selectedTermKey =
    dashboard?.academicYear && dashboard.semester != null
      ? `${dashboard.academicYear}-S${dashboard.semester}`
      : ALL_TERMS

  const setTerm = (termKey: string) => {
    const term = dashboard?.terms.find((item) => item.key === termKey)
    if (!term) return

    const next = new URLSearchParams(searchParams)
    next.set("academicYear", term.academicYear)
    next.set("semester", String(term.semester))
    setSearchParams(next)
  }

  const setProgram = (value: string) => {
    const next = new URLSearchParams(searchParams)
    next.delete("cohortId")

    if (value === ALL_PROGRAMS) {
      next.delete("programId")
    } else {
      next.set("programId", value)
    }

    next.delete("creditProgramId")
    next.delete("creditCohortId")
    setSearchParams(next)
  }

  const setCohort = (value: string) => {
    const next = new URLSearchParams(searchParams)

    if (value === ALL_COHORTS) {
      next.delete("cohortId")
    } else {
      next.set("cohortId", value)
    }

    next.delete("creditCohortId")
    setSearchParams(next)
  }

  const selectedProgram = programsQuery.data?.find(
    (program) => program.id === programId,
  )
  const selectedCohort = cohortsQuery.data?.find((cohort) => cohort.id === cohortId)
  const creditData = creditDistributionQuery.data
  const heatmap = heatmapQuery.data

  const stats = useMemo(
    () => [
      {
        label: "Assigned Instructors",
        value: dashboard?.totalInstructors ?? 0,
        helper: "Faculty with teaching assignments",
        icon: Users,
        tone: "border-[#cfe5e7] bg-[#eaf6f6] text-[#007d84]",
      },
      {
        label: "Assigned Courses",
        value: dashboard?.totalAssignedCourses ?? 0,
        helper: "Unique courses in selected term",
        icon: CalendarCheck,
        tone: "border-[#d7e3f5] bg-[#eef4ff] text-[#315b9a]",
      },
      {
        label: "Submitted Syllabi",
        value: dashboard?.submittedAssignments ?? 0,
        helper: "Submitted or already in review",
        icon: BookOpenCheck,
        tone: "border-[#f6ddb0] bg-[#fff7e7] text-[#b76c0b]",
      },
      {
        label: "Missing Syllabi",
        value: dashboard?.notSubmittedAssignments ?? 0,
        helper: `${dashboard?.overdueAssignments ?? 0} overdue`,
        icon: FileWarning,
        tone: "border-[#f3cfd5] bg-[#fff0f2] text-[#c73e56]",
      },
      {
        label: "Approved Syllabi",
        value: dashboard?.approvedAssignments ?? 0,
        helper: "Completed academic approval",
        icon: CheckCircle2,
        tone: "border-[#cdebdc] bg-[#eefbf5] text-emerald-700",
      },
      {
        label: "Approval Rate",
        value: `${dashboard?.approvalRate?.toFixed(1) ?? "0.0"}%`,
        helper: "Approved / assigned course responsibility",
        icon: PieChartIcon,
        tone: "border-[#cfe5e7] bg-[#eaf6f6] text-[#007d84]",
      },
    ],
    [dashboard],
  )

  const openHeatmap = () => {
    if (!programId || !cohortId || !dashboard?.academicYear || dashboard.semester == null) {
      navigate("/admin/clo-plo-heatmap")
      return
    }

    const params = new URLSearchParams({
      programId: String(programId),
      cohortId: String(cohortId),
      academicYear: dashboard.academicYear,
      semester: String(dashboard.semester),
    })

    navigate(`/admin/clo-plo-heatmap?${params.toString()}`)
  }

  return (
    <div
      className="space-y-5"
      data-testid="admin-dashboard"
      data-dashboard-loaded={query.isSuccess ? "true" : "false"}
    >
      <DashboardHero
        roleLabel="Administrator"
        title="Curriculum Management Dashboard"
        description="Faculty-wide monitoring of syllabus submission, approval progress, curriculum structure, CLO–PLO coverage, and traceable system activity."
      />

      <Card className="border-[#d6e1e6] bg-white shadow-[0_8px_22px_rgba(0,74,82,0.06)]">
        <CardContent className="p-4">
          <div className="grid w-full gap-3 md:grid-cols-3">
            <div>
              <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.45px] text-[#526a75]">
                Academic Year · Semester
              </label>
              <Select
                value={selectedTermKey}
                onValueChange={setTerm}
                disabled={!dashboard?.terms.length}
              >
                <SelectTrigger className="h-[38px] w-full bg-white">
                  <SelectValue placeholder="No academic term" />
                </SelectTrigger>
                <SelectContent>
                  {dashboard?.terms.map((term) => (
                    <SelectItem key={term.key} value={term.key}>
                      {term.label} ({term.sectionCount} sections)
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.45px] text-[#526a75]">
                Program
              </label>
              <Select
                value={programId ? String(programId) : ALL_PROGRAMS}
                onValueChange={setProgram}
              >
                <SelectTrigger className="h-[38px] w-full bg-white">
                  <SelectValue placeholder="All Programs" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_PROGRAMS}>All Programs</SelectItem>
                  {programsQuery.data
                    ?.filter((program) => program.isActive !== false)
                    .map((program) => (
                      <SelectItem key={program.id} value={String(program.id)}>
                        {program.code} · {program.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.45px] text-[#526a75]">
                Cohort
              </label>
              <Select
                value={cohortId ? String(cohortId) : ALL_COHORTS}
                onValueChange={setCohort}
                disabled={!programId}
              >
                <SelectTrigger className="h-[38px] w-full bg-white">
                  <SelectValue
                    placeholder={programId ? "All Cohorts" : "Select Program first"}
                  />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL_COHORTS}>All Cohorts</SelectItem>
                  {cohortsQuery.data
                    ?.filter((cohort) => cohort.isActive !== false)
                    .map((cohort) => (
                      <SelectItem key={cohort.id} value={String(cohort.id)}>
                        {cohort.name} · {cohort.entryYear}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {query.isError && (
        <div className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-[12px] text-rose-700">
          Unable to load Dashboard data. Please verify the backend connection and
          Administrator permissions.
        </div>
      )}

      {dashboard && (
        <div
          className={`rounded-md border px-4 py-3 text-[12px] shadow-sm ${
            dashboard.deadlineConfigured
              ? dashboard.deadlinePassed
                ? "border-rose-200 bg-rose-50 text-slate-700"
                : "border-[#cfe5e7] bg-[#eff9f9] text-slate-700"
              : "border-amber-200 bg-amber-50 text-slate-700"
          }`}
        >
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="font-semibold text-[#19313c]">Current scope:</span>
            <span>{dashboard.academicYear ?? "No academic year"}</span>
            {dashboard.semester != null && <span>· Semester {dashboard.semester}</span>}
            <span>·</span>
            <strong>
              {selectedProgram
                ? `${selectedProgram.code} · ${selectedProgram.name}`
                : "All Programs"}
            </strong>
            {programId && (
              <>
                <span>·</span>
                <strong>{selectedCohort ? selectedCohort.name : "All Cohorts"}</strong>
              </>
            )}
            <span className="text-slate-400">|</span>
            {dashboard.deadlineConfigured ? (
              <span>
                Submission deadline: {" "}
                <strong>
                  {dashboard.deadlineAt
                    ? new Date(dashboard.deadlineAt).toLocaleString("en-GB")
                    : "Not available"}
                </strong>{" "}
                {dashboard.deadlinePassed ? "(overdue)" : "(active)"}
              </span>
            ) : (
              <span>No submission deadline configured for this term.</span>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {stats.map((stat) => {
          const Icon = stat.icon
          return (
            <Card
              key={stat.label}
              className="overflow-hidden border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]"
            >
              <CardContent className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.55px] text-[#617780]">
                      {stat.label}
                    </p>
                    <p className="mt-2 text-[30px] font-bold leading-none text-[#17323d]">
                      {query.isLoading ? "…" : stat.value}
                    </p>
                    <p className="mt-2 text-[11px] text-slate-500">{stat.helper}</p>
                  </div>
                  <div
                    className={`flex h-[44px] w-[44px] shrink-0 items-center justify-center rounded-lg border ${stat.tone}`}
                  >
                    <Icon className="size-[20px]" />
                  </div>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      <div className="grid gap-5 xl:grid-cols-2">
        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
          <CardHeader className="border-b border-[#e1e9ec] pb-4">
            <CardTitle className="flex items-center gap-2 text-base">
              <PieChartIcon className="size-4 text-[#007d84]" />
              Syllabus Status Overview
            </CardTitle>
            <CardDescription>
              Latest syllabus state for each instructor–course responsibility in the
              selected term and academic scope.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5">
            {query.isLoading ? (
              <div className="py-16 text-center text-sm text-slate-400">Loading status data…</div>
            ) : !syllabusStatusData.some((item) => item.value > 0) ? (
              <div className="py-16 text-center text-sm text-slate-400">No syllabus status data.</div>
            ) : (
              <div className="grid gap-4 md:grid-cols-[1.1fr_0.9fr]">
                <div className="h-[285px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={syllabusStatusData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={66}
                        outerRadius={102}
                        paddingAngle={2}
                      >
                        {syllabusStatusData.map((item, index) => (
                          <Cell
                            key={item.status}
                            fill={syllabusStatusColors[index % syllabusStatusColors.length]}
                          />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => [Number(value), "Responsibilities"]} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="space-y-2 self-center">
                  {syllabusStatusData.map((item, index) => (
                    <div
                      key={item.status}
                      className="flex items-center justify-between rounded-md border border-[#e1e9ec] px-3 py-2"
                    >
                      <div className="flex items-center gap-2">
                        <span
                          className="h-2.5 w-2.5 rounded-full"
                          style={{
                            backgroundColor:
                              syllabusStatusColors[index % syllabusStatusColors.length],
                          }}
                        />
                        <span className="text-[11px] font-medium text-slate-700">{item.name}</span>
                      </div>
                      <span className="text-[12px] font-bold text-[#17323d]">{item.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
          <CardHeader className="border-b border-[#e1e9ec] pb-4">
            <CardTitle className="flex items-center gap-2 text-base">
              <GraduationCap className="size-4 text-[#f0aa39]" />
              Credit Distribution
            </CardTitle>
            <CardDescription>
              FR-06.5 curriculum credit distribution by Compulsory, Elective, and General
              course groups. This chart follows Program + Cohort scope.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5">
            {!programId || !cohortId ? (
              <div className="flex min-h-[285px] flex-col items-center justify-center text-center">
                <Layers3 className="mb-3 size-8 text-slate-300" />
                <p className="text-sm font-semibold text-slate-700">Select Program and Cohort</p>
                <p className="mt-1 max-w-sm text-[11px] leading-5 text-slate-500">
                  Credit structure belongs to a curriculum cohort, so Academic Year and
                  Semester do not change this chart.
                </p>
              </div>
            ) : creditDistributionQuery.isLoading ? (
              <div className="py-16 text-center text-sm text-slate-400">Loading credit distribution…</div>
            ) : creditDistributionQuery.isError || !creditData ? (
              <div className="py-16 text-center text-sm text-rose-600">Unable to load credit distribution.</div>
            ) : (
              <div className="grid gap-4 md:grid-cols-[1.05fr_0.95fr]">
                <div className="h-[285px]">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={creditData.groups}
                        dataKey="credits"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={66}
                        outerRadius={102}
                        paddingAngle={2}
                      >
                        {creditData.groups.map((group, index) => (
                          <Cell
                            key={group.code}
                            fill={creditChartColors[index % creditChartColors.length]}
                          />
                        ))}
                      </Pie>
                      <Tooltip formatter={(value) => [`${Number(value)} credits`, "Credits"]} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="space-y-3 self-center">
                  <div className="grid grid-cols-2 gap-2">
                    <div className="rounded-md border border-[#cfe5e7] bg-[#eff9f9] p-3">
                      <p className="text-[9px] font-semibold uppercase tracking-wide text-[#007d84]">Total credits</p>
                      <p className="mt-1 text-2xl font-bold text-[#006b72]">{creditData.calculatedTotalCredits}</p>
                    </div>
                    <div className="rounded-md border border-slate-200 bg-slate-50 p-3">
                      <p className="text-[9px] font-semibold uppercase tracking-wide text-slate-500">Unique courses</p>
                      <p className="mt-1 text-2xl font-bold text-[#17323d]">{creditData.uniqueCourseCount}</p>
                    </div>
                  </div>
                  {creditData.groups.map((group, index) => (
                    <div
                      key={group.code}
                      className="flex items-center justify-between rounded-md border border-[#e1e9ec] px-3 py-2"
                    >
                      <div className="flex min-w-0 items-center gap-2">
                        <span
                          className="h-2.5 w-2.5 shrink-0 rounded-full"
                          style={{ backgroundColor: creditChartColors[index % creditChartColors.length] }}
                        />
                        <div className="min-w-0">
                          <p className="truncate text-[11px] font-semibold text-slate-700">
                            {group.name || group.nameVn || group.code}
                          </p>
                          <p className="text-[10px] text-slate-500">
                            {group.courseCount} courses · {group.percentage.toFixed(1)}%
                          </p>
                        </div>
                      </div>
                      <span className="shrink-0 text-[12px] font-bold text-[#17323d]">{group.credits}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
        <CardHeader className="border-b border-[#e1e9ec] pb-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <Layers3 className="size-4 text-[#007d84]" />
                Syllabus Progress by Course Group
              </CardTitle>
              <CardDescription className="mt-1">
                FR-06.4 submission progress for curriculum course groups within the selected
                Dashboard scope.
              </CardDescription>
            </div>
            {dashboard && (
              <Badge variant="outline" className="border-[#cfe5e7] bg-[#eff9f9] text-[#006b72]">
                {dashboard.totalAssignedCourses} assigned courses
              </Badge>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-5">
          {!programId ? (
            <div className="py-14 text-center text-sm text-slate-500">
              Select a Program to view course-group progress.
            </div>
          ) : !courseGroupProgressData.length ? (
            <div className="py-14 text-center text-sm text-slate-400">No course-group data.</div>
          ) : (
            <div className="grid gap-5 xl:grid-cols-[1.6fr_0.9fr]">
              <div className="h-[310px]">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={courseGroupProgressData} margin={{ top: 8, right: 12, left: -18, bottom: 8 }}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8eef0" />
                    <XAxis dataKey="code" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 10 }} axisLine={false} tickLine={false} />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="submitted" name="Submitted" fill="#0b858c" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="missing" name="Not Submitted" fill="#e7a23b" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div className="space-y-2">
                {courseGroupProgressData.map((group) => (
                  <div key={group.code} className="rounded-md border border-[#e1e9ec] p-3">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-[12px] font-semibold text-slate-800">{group.name}</p>
                        <p className="mt-1 text-[10px] text-slate-500">
                          {group.submitted} submitted · {group.missing} not submitted
                        </p>
                      </div>
                      <span className="text-sm font-bold text-[#006b72]">{group.approval.toFixed(1)}%</span>
                    </div>
                    {group.overdue > 0 && (
                      <div className="mt-2 flex items-center gap-1.5 text-[10px] font-medium text-rose-600">
                        <AlertCircle className="size-3" />
                        {group.overdue} overdue course{group.overdue === 1 ? "" : "s"}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-5 xl:grid-cols-5">
        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)] xl:col-span-3">
          <CardHeader className="border-b border-[#e1e9ec] pb-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <CardTitle className="flex items-center gap-2 text-base">
                  <FileWarning className="size-4 text-[#d65a6f]" />
                  Missing / Overdue Syllabi
                </CardTitle>
                <CardDescription className="mt-1">
                  FR-06.4 identifies who has not completed syllabus submission and which
                  assigned courses still require action.
                </CardDescription>
              </div>
              <Badge variant="outline" className="border-rose-200 bg-rose-50 text-rose-700">
                {dashboard?.notSubmittedAssignments ?? 0} require action
              </Badge>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-[#f5f8f9]">
                    <TableHead>Instructor</TableHead>
                    <TableHead>Department</TableHead>
                    <TableHead>Course</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Sections</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {query.isLoading ? (
                    <TableRow>
                      <TableCell colSpan={5} className="py-10 text-center text-slate-400">
                        Loading syllabus responsibilities…
                      </TableCell>
                    </TableRow>
                  ) : !missingRows.length ? (
                    <TableRow>
                      <TableCell colSpan={5} className="py-10 text-center text-emerald-700">
                        All assigned course responsibilities have a submitted syllabus.
                      </TableCell>
                    </TableRow>
                  ) : (
                    missingRows.map((row) => (
                      <TableRow key={row.key}>
                        <TableCell>
                          <p className="text-[12px] font-semibold text-slate-800">{row.instructorName}</p>
                          <p className="text-[10px] text-slate-500">{row.staffCode} · {row.email}</p>
                        </TableCell>
                        <TableCell className="text-[11px] text-slate-600">{row.department}</TableCell>
                        <TableCell>
                          <p className="text-[12px] font-semibold text-slate-800">{row.courseCode}</p>
                          <p className="max-w-[260px] truncate text-[10px] text-slate-500">{row.courseName}</p>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap items-center gap-1.5">
                            <Badge
                              variant="outline"
                              className={
                                row.overdue
                                  ? "border-rose-200 bg-rose-50 text-rose-700"
                                  : "border-amber-200 bg-amber-50 text-amber-700"
                              }
                            >
                              {statusLabel(row.latestStatus)}
                            </Badge>
                            {row.overdue && <Badge variant="destructive">Overdue</Badge>}
                          </div>
                          {row.versionLabel && (
                            <p className="mt-1 text-[10px] text-slate-500">{row.versionLabel}</p>
                          )}
                        </TableCell>
                        <TableCell className="text-right text-[12px] font-semibold text-slate-700">
                          {row.sectionCount}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>

        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)] xl:col-span-2">
          <CardHeader className="border-b border-[#e1e9ec] pb-4">
            <CardTitle className="flex items-center gap-2 text-base">
              <Target className="size-4 text-[#007d84]" />
              CLO–PLO Coverage
            </CardTitle>
            <CardDescription>
              FR-06.6 summary from approved syllabi for the selected Program, Cohort,
              Academic Year, and Semester.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5">
            {!programId || !cohortId ? (
              <div className="flex min-h-[310px] flex-col items-center justify-center text-center">
                <Target className="mb-3 size-8 text-slate-300" />
                <p className="text-sm font-semibold text-slate-700">Select Program and Cohort</p>
                <p className="mt-1 max-w-xs text-[11px] leading-5 text-slate-500">
                  The heatmap requires a curriculum cohort and term before coverage can be
                  calculated.
                </p>
                <Button variant="outline" size="sm" className="mt-4" onClick={openHeatmap}>
                  Open CLO–PLO Matrix
                  <ArrowRight className="ml-2 size-3.5" />
                </Button>
              </div>
            ) : heatmapQuery.isLoading ? (
              <div className="py-16 text-center text-sm text-slate-400">Generating CLO–PLO coverage…</div>
            ) : heatmapQuery.isError || !heatmap ? (
              <div className="space-y-3 py-12 text-center">
                <p className="text-sm text-rose-600">Unable to calculate CLO–PLO coverage for this scope.</p>
                <Button variant="outline" size="sm" onClick={openHeatmap}>Open Full Matrix</Button>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-md border border-[#cfe5e7] bg-[#eff9f9] p-4">
                    <p className="text-[9px] font-semibold uppercase tracking-wide text-[#007d84]">PLO Coverage</p>
                    <p className="mt-1 text-2xl font-bold text-[#006b72]">
                      {heatmap.summary.ploCoveragePercentage.toFixed(1)}%
                    </p>
                    <p className="mt-1 text-[10px] text-slate-500">
                      {heatmap.summary.coveredPlos}/{heatmap.summary.totalPlos} PLOs covered
                    </p>
                  </div>
                  <div className="rounded-md border border-slate-200 bg-slate-50 p-4">
                    <p className="text-[9px] font-semibold uppercase tracking-wide text-slate-500">Uncovered PLOs</p>
                    <p className="mt-1 text-2xl font-bold text-[#17323d]">{heatmap.summary.uncoveredPlos}</p>
                    <p className="mt-1 text-[10px] text-slate-500">Require curriculum review</p>
                  </div>
                </div>

                <ProgressMetric
                  label="Approved syllabus availability"
                  value={heatmap.summary.approvedSyllabusPercentage}
                  note={`${heatmap.summary.coursesWithApprovedSyllabus}/${heatmap.summary.totalCourses} courses`}
                />
                <ProgressMetric
                  label="CLO mapping completeness"
                  value={heatmap.summary.cloMappingPercentage}
                  note={`${heatmap.summary.mappedClos}/${heatmap.summary.totalClos} CLOs mapped`}
                />

                {(heatmap.summary.errorCount > 0 || heatmap.summary.warningCount > 0) && (
                  <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] text-amber-800">
                    <strong>{heatmap.summary.errorCount + heatmap.summary.warningCount}</strong>{" "}
                    coverage issue(s) require attention before accreditation reporting.
                  </div>
                )}

                <Button className="w-full bg-[#006b72] hover:bg-[#005b61]" onClick={openHeatmap}>
                  View Full CLO–PLO Heatmap
                  <ArrowRight className="ml-2 size-4" />
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
        <CardHeader className="border-b border-[#e1e9ec] pb-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-base">
                <Activity className="size-4 text-[#007d84]" />
                Recent Activity Log
              </CardTitle>
              <CardDescription className="mt-1">
                System-wide audit trail of the {RECENT_AUDIT_LOG_LIMIT} most recent recorded
                changes. This panel is intentionally not filtered by curriculum scope.
              </CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={() => navigate("/admin/audit-log")}>
              View All Activity
              <ArrowRight className="ml-2 size-3.5" />
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow className="bg-[#f5f8f9]">
                  <TableHead>Time</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Target</TableHead>
                  <TableHead className="text-right">IP Address</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoadingLogs ? (
                  <TableRow>
                    <TableCell colSpan={5} className="py-9 text-center text-slate-400">Loading recent activity…</TableCell>
                  </TableRow>
                ) : isAuditLogError ? (
                  <TableRow>
                    <TableCell colSpan={5} className="py-9 text-center text-rose-600">Unable to load activity log.</TableCell>
                  </TableRow>
                ) : !auditLogs?.length ? (
                  <TableRow>
                    <TableCell colSpan={5} className="py-9 text-center text-slate-400">No recorded activity.</TableCell>
                  </TableRow>
                ) : (
                  auditLogs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell className="whitespace-nowrap text-[11px] text-slate-500">
                        <div className="flex items-center gap-1.5">
                          <Clock3 className="size-3 text-slate-400" />
                          {new Date(log.changedAt).toLocaleString("en-GB")}
                        </div>
                      </TableCell>
                      <TableCell>
                        <p className="text-[12px] font-semibold text-slate-800">
                          {log.changedByUsername || (log.changedById ? `User #${log.changedById}` : "System")}
                        </p>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="border-[#cfe5e7] bg-[#eff9f9] text-[#006b72]">
                          {actionLabel(log.action)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <p className="font-mono text-[11px] text-slate-700">{log.tableName}</p>
                        <p className="text-[10px] text-slate-500">Record #{log.recordId}</p>
                      </TableCell>
                      <TableCell className="text-right font-mono text-[10px] text-slate-500">
                        {log.ipAddress ?? "—"}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-[#e1e9ec] pt-3 text-[10px] text-[#78909a]">
        <span>Data source: {dashboard?.dataSource ?? "Loading"}. No hard-coded dashboard metrics.</span>
        <span>
          NFR-01.1 target: main Dashboard load &lt; {DASHBOARD_TARGET_MS / 1000} seconds.
        </span>
      </div>
    </div>
  )
}
