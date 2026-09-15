import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useMemo, useState, type ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  AlertTriangle,
  BarChart3,
  BookOpen,
  ChevronRight,
  CircleDashed,
  ClipboardCheck,
  Clock3,
  FileWarning,
  Target,
  Layers3,
  RefreshCw,
  Search,
  ShieldCheck,
  Sparkles,
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

import {
  dashboardApi,
  type DeanCourseProgress,
  type DeanDashboardWarning,
} from "@/api/dashboardApi"
import { approvalRequestApi } from "@/api/approvalRequestApi"
import DashboardHero from "@/components/dashboard/DashboardHero"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { cn } from "@/lib/utils"

const STATUS_COLORS: Record<string, string> = {
  APPROVED: "#10b981",
  UNDER_REVIEW: "#3b82f6",
  SUBMITTED: "#6366f1",
  REVISION_REQUESTED: "#f59e0b",
  DRAFT: "#f97316",
  REJECTED: "#ef4444",
  ARCHIVED: "#64748b",
  MISSING: "#cbd5e1",
}

const STATUS_STYLES: Record<string, string> = {
  APPROVED: "border-emerald-200 bg-emerald-50 text-emerald-700",
  UNDER_REVIEW: "border-blue-200 bg-blue-50 text-blue-700",
  SUBMITTED: "border-indigo-200 bg-indigo-50 text-indigo-700",
  REVISION_REQUESTED: "border-amber-200 bg-amber-50 text-amber-700",
  DRAFT: "border-orange-200 bg-orange-50 text-orange-700",
  REJECTED: "border-rose-200 bg-rose-50 text-rose-700",
  ARCHIVED: "border-slate-200 bg-slate-100 text-slate-600",
  MISSING: "border-slate-300 bg-white text-slate-600",
}

const ALL_SEMESTERS = "ALL"
const ALL_STATUSES = "ALL"

export default function DeanDashboardPage() {
  const navigate = useNavigate()
  const [majorId, setMajorId] = useState<number | undefined>()
  const [cohortId, setCohortId] = useState<number | undefined>()
  const [semester, setSemester] = useState<number | undefined>()
  const [statusFilter, setStatusFilter] = useState(ALL_STATUSES)
  const [search, setSearch] = useState("")

  const dashboardQuery = useQuery({
    queryKey: ["dean-dashboard", majorId, cohortId, semester],
    queryFn: () =>
      dashboardApi.getDeanDashboard({ majorId, cohortId, semester }),
    refetchInterval: 60_000,
    staleTime: 30_000,
  })
  const finalReviewQuery = useQuery({
    queryKey: ["approval-requests", "pending", "STEP3_DEAN"],
    queryFn: () => approvalRequestApi.getPendingByStep("STEP3_DEAN"),
    staleTime: 15_000,
    refetchInterval: 30_000,
  })
  const pendingFinalReviews = finalReviewQuery.data ?? []

  const dashboard = dashboardQuery.data
  const selectedMajor = majorId ?? dashboard?.scope.majorId ?? undefined
  const selectedCohort = cohortId ?? dashboard?.scope.cohortId ?? undefined

  const filteredCourses = useMemo(() => {
    if (!dashboard) return []
    const keyword = search.trim().toLocaleLowerCase("vi")
    return dashboard.courses.filter((course) => {
      const matchesStatus =
        statusFilter === ALL_STATUSES || course.status === statusFilter
      const matchesKeyword =
        !keyword ||
        [
          course.courseCode,
          course.courseName,
          course.courseNameVn,
          course.courseType,
          course.preparedBy,
        ]
          .filter(Boolean)
          .some((value) =>
            String(value).toLocaleLowerCase("vi").includes(keyword),
          )
      return matchesStatus && matchesKeyword
    })
  }, [dashboard, search, statusFilter])

  if (dashboardQuery.isLoading && !dashboard) {
    return <DeanDashboardSkeleton />
  }

  if (dashboardQuery.isError || !dashboard) {
    return (
      <div className="mx-auto flex min-h-[55vh] max-w-xl items-center justify-center p-6">
        <Card className="w-full border-rose-200 bg-rose-50/70 shadow-sm">
          <CardContent className="space-y-4 p-6 text-center">
            <AlertTriangle className="mx-auto size-9 text-rose-600" />
            <div>
              <h2 className="font-semibold text-slate-900">
                Không thể tải Dean Dashboard
              </h2>
              <p className="mt-1 text-sm text-slate-600">
                Kiểm tra backend, quyền DEAN và dữ liệu CTĐT rồi thử lại.
              </p>
            </div>
            <Button onClick={() => dashboardQuery.refetch()}>
              <RefreshCw /> Tải lại dữ liệu
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  const summary = dashboard.summary
  const activeWarnings = dashboard.warnings.filter(
    (warning) => warning.severity !== "INFO",
  )

  const stats = [
    {
      label: "Môn trong phạm vi",
      value: summary.expectedCourses,
      note: `${summary.expectedCredits} tín chỉ theo CTĐT`,
      icon: BookOpen,
      iconClass: "bg-slate-950 text-white",
    },
    {
      label: "Đã phê duyệt",
      value: summary.approvedSyllabuses,
      note: `${formatPercent(summary.approvalRate)} hoàn tất`,
      icon: ShieldCheck,
      iconClass: "bg-emerald-50 text-emerald-700",
    },
    {
  label: "Final Reviews · School-wide",
  value: pendingFinalReviews.length,
  note: "Across SCSE · awaiting Dean decision",
  icon: Clock3,
  iconClass: "bg-blue-50 text-blue-700",
},
    {
      label: "Chưa có đề cương",
      value: summary.missingSyllabuses,
      note: "Thiếu so với CourseProgram",
      icon: FileWarning,
      iconClass: "bg-rose-50 text-rose-700",
    },
    {
      label: "Cần theo dõi",
      value: summary.actionRequiredCourses,
      note: "Chưa tạo, bản nháp, cần sửa hoặc bị từ chối",
      icon: AlertTriangle,
      iconClass: "bg-amber-50 text-amber-700",
    },
  ]

  return (
    <div className="space-y-5 pb-8">
      <DashboardHero
        roleLabel="Dean"
        title="Academic Quality Oversight"
        description="Monitor curriculum quality, syllabus approval progress, academic coverage, and program readiness across the school."
        actions={
          <>
            <Button
              className="bg-white text-[#07515a] hover:bg-white/90"
              onClick={() => navigate("/dean/approvals")}
            >
              <ClipboardCheck className="size-4" />
              Final Approval Queue
            </Button>

            <Button
              variant="outline"
              className="border-white/50 bg-white/10 text-white hover:bg-white/20 hover:text-white"
              onClick={() => navigate("/dean/syllabus")}
            >
              <BookOpen className="size-4" />
              Syllabus Catalog
            </Button>

            <Button
              variant="outline"
              className="border-white/50 bg-white/10 text-white hover:bg-white/20 hover:text-white"
              disabled={dashboardQuery.isFetching}
              onClick={() => dashboardQuery.refetch()}
            >
              <RefreshCw className={cn(dashboardQuery.isFetching && "animate-spin")} />
              Refresh
            </Button>
          </>
        }
      />

      <Card className="border-slate-200 shadow-none">
        <CardContent className="p-4">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-sm font-semibold text-slate-900">
                Academic Scope
              </p>
              <p className="text-xs text-slate-500">
                {dashboard.scope.majorCode ?? "No major"} · {dashboard.scope.cohortName ?? "No cohort"} · {dashboard.scope.semesterLabel}
              </p>
            </div>

            <p className="text-[11px] text-slate-400">
              Updated {formatDateTime(dashboard.generatedAt)} · {dashboard.timeZone}
            </p>
          </div>

          <div className="grid gap-3 md:grid-cols-3">
            <FilterSelect
              label="Ngành đào tạo"
              value={selectedMajor ? String(selectedMajor) : ""}
              placeholder="Chọn ngành"
              onValueChange={(value) => {
                setMajorId(Number(value))
                setCohortId(undefined)
                setSemester(undefined)
                setStatusFilter(ALL_STATUSES)
              }}
              options={dashboard.majors.map((major) => ({
                value: String(major.id),
                label: `${major.code} · ${major.name}`,
                note: `${major.activeCohortCount} Cohort`,
              }))}
            />

            <FilterSelect
              label="Cohort áp dụng"
              value={selectedCohort ? String(selectedCohort) : ""}
              placeholder="Chọn Cohort"
              disabled={dashboard.cohorts.length === 0}
              onValueChange={(value) => {
                setCohortId(Number(value))
                setSemester(undefined)
                setStatusFilter(ALL_STATUSES)
              }}
              options={dashboard.cohorts.map((cohort) => ({
                value: String(cohort.id),
                label: `${cohort.name} · ${cohort.programCode}`,
                note: String(cohort.entryYear),
              }))}
            />

            <FilterSelect
              label="Học kỳ CTĐT"
              value={semester ? String(semester) : ALL_SEMESTERS}
              placeholder="Tất cả học kỳ"
              disabled={dashboard.semesters.length === 0}
              onValueChange={(value) => {
                setSemester(value === ALL_SEMESTERS ? undefined : Number(value))
                setStatusFilter(ALL_STATUSES)
              }}
              options={[
                {
                  value: ALL_SEMESTERS,
                  label: "Tất cả học kỳ",
                  note: `${dashboard.summary.expectedCourses} môn`,
                },
                ...dashboard.semesters.map((item) => ({
                  value: String(item.value),
                  label: item.label,
                  note: `${item.courseCount} môn`,
                })),
              ]}
            />
          </div>
        </CardContent>
      </Card>

      {activeWarnings.length > 0 && (
        <WarningPanel warnings={activeWarnings} />
      )}

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {stats.map((stat) => {
          const Icon = stat.icon
          return (
            <Card key={stat.label} className="border-slate-200 shadow-none">
              <CardContent className="flex min-h-28 items-start justify-between p-4">
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">
                    {stat.label}
                  </p>
                  <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
                    {stat.value}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">{stat.note}</p>
                </div>
                <span
                  className={cn(
                    "flex size-10 items-center justify-center rounded-xl",
                    stat.iconClass,
                  )}
                >
                  <Icon className="size-5" />
                </span>
              </CardContent>
            </Card>
          )
        })}
      </section>

      <section className="overflow-hidden rounded-2xl border border-blue-200 bg-white">
        <div className="flex items-center justify-between border-b border-blue-100 bg-blue-50/60 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-blue-600">Dean Review Queue</p>
            <h2 className="mt-1 text-lg font-semibold text-slate-950">Needs Final Approval</h2>
          </div>
          <Badge className="bg-blue-600 text-white">{pendingFinalReviews.length} pending</Badge>
        </div>
        <div className="divide-y divide-slate-100">
          {pendingFinalReviews.map((request) => {
            return (
              <div key={request.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-mono text-xs font-bold text-[#007d84]">{request.courseCode}</p>
                  <p className="font-semibold text-slate-900">{request.courseName}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    Program: {request.programCode ?? "—"} · Cohort: {request.cohortName ?? "—"} · Semester: {request.semester ?? "—"} · Instructor: {request.instructorUsername ?? "—"} · Reviewed by: {request.requestedByUsername ?? "—"}
                  </p>
                </div>
                <Button size="sm" onClick={() => navigate(`/dean/syllabus/${request.syllabusId}`)}>
                  Review <ChevronRight className="size-4" />
                </Button>
              </div>
            )
          })}
          {!finalReviewQuery.isLoading && pendingFinalReviews.length === 0 && (
            <p className="px-5 py-8 text-center text-sm text-slate-500">No syllabus is waiting for final approval.</p>
          )}
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
        <Card className="border-slate-200 shadow-none">
          <CardContent className="p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                  Dean Action Center
                </p>
                <h2 className="mt-1 text-lg font-semibold text-slate-950">
                  Academic management shortcuts
                </h2>
                <p className="mt-1 text-sm text-slate-500">
                  Open the main workflows used by the Dean without leaving the dashboard.
                </p>
              </div>

              <Badge
                variant="outline"
                className={
                  pendingFinalReviews.length > 0
                    ? "border-amber-200 bg-amber-50 text-amber-700"
                    : "border-emerald-200 bg-emerald-50 text-emerald-700"
                }
              >
                {pendingFinalReviews.length} final review pending
              </Badge>
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              <DeanShortcut
                title="Final Approval Queue"
                description="Approve or return syllabi after Department Head review"
                icon={<ClipboardCheck className="size-5" />}
                iconClass="bg-[#e9f5f6] text-[#007d84]"
                onClick={() => navigate("/dean/approvals")}
              />

              <DeanShortcut
                title="Syllabus Catalog"
                description="Review versions, status, comments, and approval history"
                icon={<BookOpen className="size-5" />}
                iconClass="bg-blue-50 text-blue-700"
                onClick={() => navigate("/dean/syllabus")}
              />

              <DeanShortcut
                title="Curriculum Programs"
                description="Review program structures, cohorts, and curriculum history"
                icon={<Layers3 className="size-5" />}
                iconClass="bg-slate-100 text-slate-700"
                onClick={() => navigate("/dean/programs")}
              />

              <DeanShortcut
                title="PLO Management"
                description="Review program learning outcomes used by academic programs"
                icon={<Target className="size-5" />}
                iconClass="bg-amber-50 text-amber-700"
                onClick={() => navigate("/dean/plo")}
              />

              <DeanShortcut
                title="CLO–PLO Coverage"
                description="Inspect outcome coverage across the selected curriculum"
                icon={<BarChart3 className="size-5" />}
                iconClass="bg-violet-50 text-violet-700"
                onClick={() => navigate("/dean/clo-plo-heatmap")}
              />

              <DeanShortcut
                title="Academic Reports"
                description="Open syllabus, coverage, and curriculum reports"
                icon={<ShieldCheck className="size-5" />}
                iconClass="bg-emerald-50 text-emerald-700"
                onClick={() => navigate("/dean/reports")}
              />
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-none">
          <CardContent className="p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                  Final Review Workload
                </p>
                <h2 className="mt-1 text-lg font-semibold text-slate-950">
                  Syllabi waiting for Dean decision
                </h2>
              </div>

              <span className="flex size-11 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
                <Clock3 className="size-5" />
              </span>
            </div>

            <div className="mt-5 flex items-end justify-between gap-4 rounded-2xl bg-slate-50 p-5">
              <div>
                <p className="text-4xl font-semibold tracking-tight text-slate-950">
                  {pendingFinalReviews.length}
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  under review at Dean step
                </p>
              </div>

              <Button
                variant="outline"
                size="sm"
                onClick={() => navigate("/dean/approvals")}
              >
                Open Queue
                <ChevronRight className="size-4" />
              </Button>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-3">
              <div className="rounded-xl border border-slate-200 p-3">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Approved
                </p>
                <p className="mt-1 text-xl font-semibold text-emerald-700">
                  {summary.approvedSyllabuses}
                </p>
              </div>

              <div className="rounded-xl border border-slate-200 p-3">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Needs follow-up
                </p>
                <p className="mt-1 text-xl font-semibold text-amber-700">
                  {summary.actionRequiredCourses}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-4 xl:grid-cols-[1.45fr_0.85fr]">
        <Card className="border-slate-200 shadow-none">
          <CardContent className="p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                  Tỷ lệ phê duyệt
                </p>
                <div className="mt-2 flex items-end gap-3">
                  <span className="text-4xl font-semibold tracking-tight text-slate-950">
                    {formatPercent(summary.approvalRate)}
                  </span>
                  <span className="pb-1 text-sm text-slate-500">
                    {summary.approvedSyllabuses}/{summary.expectedCourses} môn
                  </span>
                </div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-right">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                  Tiến độ tạo đề cương
                </p>
                <p className="mt-1 text-lg font-semibold text-slate-900">
                  {formatPercent(summary.creationRate)}
                </p>
              </div>
            </div>

            <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-[linear-gradient(90deg,#0f766e,#10b981)] transition-[width] duration-500"
                style={{ width: `${Math.min(summary.approvalRate, 100)}%` }}
              />
            </div>
            <div className="mt-2 flex justify-between text-xs text-slate-500">
              <span>0%</span>
              <span>
                {summary.approvedCredits}/{summary.expectedCredits} tín chỉ đã có
                bản duyệt
              </span>
              <span>100%</span>
            </div>

            <div className="mt-6 h-[270px]">
              {dashboard.semesterProgress.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={dashboard.semesterProgress}
                    margin={{ top: 10, right: 8, left: -20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    <Bar
                      dataKey="approved"
                      name="Đã phê duyệt"
                      fill="#10b981"
                      radius={[4, 4, 0, 0]}
                    />
                    <Bar
                      dataKey="notApproved"
                      name="Chưa phê duyệt"
                      fill="#cbd5e1"
                      radius={[4, 4, 0, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <EmptyChart message="Chưa có dữ liệu học kỳ để tổng hợp." />
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-none">
          <CardContent className="p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">
                  Cơ cấu trạng thái
                </p>
                <h2 className="mt-1 font-semibold text-slate-900">
                  Một môn · một trạng thái đại diện
                </h2>
              </div>
              <BarChart3 className="size-5 text-slate-400" />
            </div>

            <div className="mt-3 h-[220px]">
              {dashboard.statusDistribution.length > 0 ? (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={dashboard.statusDistribution}
                      dataKey="count"
                      nameKey="label"
                      innerRadius={58}
                      outerRadius={88}
                      paddingAngle={2}
                    >
                      {dashboard.statusDistribution.map((slice) => (
                        <Cell
                          key={slice.key}
                          fill={STATUS_COLORS[slice.key] ?? "#94a3b8"}
                        />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <EmptyChart message="Không có dữ liệu trạng thái." />
              )}
            </div>

            <div className="space-y-2.5">
              {dashboard.statusDistribution.map((slice) => (
                <div
                  key={slice.key}
                  className="flex items-center justify-between text-sm"
                >
                  <span className="flex items-center gap-2 text-slate-600">
                    <span
                      className="size-2.5 rounded-full"
                      style={{
                        backgroundColor:
                          STATUS_COLORS[slice.key] ?? "#94a3b8",
                      }}
                    />
                    {slice.label}
                  </span>
                  <span className="font-medium text-slate-900">
                    {slice.count} · {formatPercent(slice.percentage)}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div className="flex flex-col gap-4 border-b border-slate-200 p-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <ClipboardCheck className="size-5 text-blue-600" />
              <h2 className="font-semibold text-slate-950">
                Curriculum Syllabus Portfolio
              </h2>
            </div>
            <p className="mt-1 text-xs text-slate-500">
              One representative syllabus is shown for each course in the selected curriculum scope. Historical versions remain available in Syllabus Management.
            </p>
          </div>

          <div className="flex flex-col gap-2 sm:flex-row">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm mã môn, tên môn, loại môn..."
                className="w-full pl-8 sm:w-72"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-full sm:w-48">
                <SelectValue placeholder="Tất cả trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_STATUSES}>Tất cả trạng thái</SelectItem>
                {dashboard.statusDistribution.map((slice) => (
                  <SelectItem key={slice.key} value={slice.key}>
                    {slice.label} ({slice.count})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              onClick={() => navigate("/dean/approvals")}
            >
              Open Final Approval Queue <ChevronRight />
            </Button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[1080px] text-left text-sm">
            <thead className="bg-slate-50 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-3">Môn học</th>
                <th className="px-4 py-3">Phân loại</th>
                <th className="px-4 py-3">Học kỳ</th>
                <th className="px-4 py-3">Đề cương đại diện</th>
                <th className="px-4 py-3">Created / Imported By</th>
                <th className="px-4 py-3">Cập nhật</th>
                <th className="px-4 py-3 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredCourses.map((course) => (
                <CourseRow
                  key={course.courseProgramId}
                  course={course}
                  onOpen={() =>
                    course.syllabusId
                      ? navigate(`/dean/syllabus/${course.syllabusId}`)
                      : navigate(
                          `/dean/syllabus?programId=${dashboard.scope.programId ?? ""}&cohortId=${dashboard.scope.cohortId ?? ""}`,
                        )
                  }
                />
              ))}
            </tbody>
          </table>
        </div>

        {filteredCourses.length === 0 && (
          <div className="flex min-h-52 flex-col items-center justify-center p-8 text-center">
            <CircleDashed className="size-9 text-slate-300" />
            <p className="mt-3 font-medium text-slate-700">
              Không có môn học phù hợp
            </p>
            <p className="mt-1 text-sm text-slate-500">
              Thử thay đổi ngành, Cohort, học kỳ hoặc điều kiện tìm kiếm.
            </p>
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 bg-slate-50 px-4 py-3 text-xs text-slate-500">
          <span>
            Hiển thị {filteredCourses.length}/{dashboard.courses.length} môn
          </span>
          <span>
            Chương trình {dashboard.scope.programCode ?? "—"} · Cohort{" "}
            {dashboard.scope.cohortName ?? "—"}
          </span>
        </div>
      </section>
    </div>
  )
}


function DeanShortcut({
  title,
  description,
  icon,
  iconClass,
  onClick,
}: {
  title: string
  description: string
  icon: ReactNode
  iconClass: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="group flex min-h-[94px] items-center justify-between rounded-2xl border border-slate-200 bg-white p-4 text-left transition hover:border-[#9bcfd2] hover:bg-[#f7fbfb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#007d84]/30"
    >
      <span className="flex min-w-0 items-center gap-3">
        <span
          className={cn(
            "flex size-10 shrink-0 items-center justify-center rounded-xl",
            iconClass,
          )}
        >
          {icon}
        </span>

        <span className="min-w-0">
          <span className="block font-semibold text-slate-900">
            {title}
          </span>
          <span className="mt-0.5 block text-xs leading-5 text-slate-500">
            {description}
          </span>
        </span>
      </span>

      <ChevronRight className="ml-3 size-4 shrink-0 text-slate-400 transition group-hover:translate-x-0.5" />
    </button>
  )
}

function CourseRow({
  course,
  onOpen,
}: {
  course: DeanCourseProgress
  onOpen: () => void
}) {
  return (
    <tr className="transition-colors hover:bg-slate-50/70">
      <td className="px-4 py-3.5">
        <div className="flex items-start gap-3">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-slate-950 text-[11px] font-semibold text-white">
            {course.courseCode.slice(0, 2)}
          </span>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-semibold text-blue-700">
                {course.courseCode}
              </span>
              <Badge variant="outline" className="text-[10px]">
                {course.credits} tín chỉ
              </Badge>
            </div>
            <p className="mt-0.5 font-medium text-slate-900">
              {course.courseName}
            </p>
            {course.courseNameVn && (
              <p className="mt-0.5 text-xs text-slate-500">
                {course.courseNameVn}
              </p>
            )}
          </div>
        </div>
      </td>
      <td className="px-4 py-3.5">
        <p className="font-medium text-slate-700">
          {course.courseType ?? "Chưa phân loại"}
        </p>
        <p className="mt-1 text-xs text-slate-500">
          {course.required ? "Bắt buộc" : "Tự chọn"}
        </p>
      </td>
      <td className="px-4 py-3.5">
        <p className="font-medium text-slate-800">{course.semesterLabel}</p>
        <p className="mt-1 text-xs text-slate-500">
          {course.yearSuggest ? `Năm ${course.yearSuggest}` : "Chưa xếp năm"}
        </p>
      </td>
      <td className="px-4 py-3.5">
        <Badge
          variant="outline"
          className={cn(
            "font-medium",
            STATUS_STYLES[course.status] ?? STATUS_STYLES.MISSING,
          )}
        >
          {course.statusLabel}
        </Badge>
        <p className="mt-1.5 text-xs text-slate-500">
          {course.syllabusId
            ? `${formatVersionLabel(course.versionNumber, course.versionLabel)} · ${course.explicitCurriculumLink ? "Curriculum linked" : "Course matched"}`
            : "No syllabus version available"}
        </p>
      </td>
      <td className="px-4 py-3.5">
        <p className="font-medium text-slate-700">
          {course.syllabusId
            ? course.preparedBy ?? "Unknown"
            : "—"}
        </p>
        <p className="mt-1 text-xs text-slate-500">
          {course.syllabusId
            ? course.dataQualityState === "MULTIPLE_CURRENT"
              ? "Multiple current versions require review"
              : "Syllabus record"
            : "No syllabus yet"}
        </p>
      </td>
      <td className="px-4 py-3.5 text-slate-600">
        {formatDateTime(course.lastUpdatedAt)}
      </td>
      <td className="px-4 py-3.5 text-right">
        <Button
          size="sm"
          variant={course.syllabusId ? "outline" : "default"}
          onClick={onOpen}
        >
          {course.syllabusId ? "Review Syllabus" : "View Missing Syllabus"}
          <ChevronRight />
        </Button>
      </td>
    </tr>
  )
}

function FilterSelect({
  label,
  value,
  placeholder,
  options,
  disabled,
  onValueChange,
}: {
  label: string
  value: string
  placeholder: string
  options: { value: string; label: string; note?: string }[]
  disabled?: boolean
  onValueChange: (value: string) => void
}) {
  return (
    <div>
      <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.16em] text-slate-500">
        {label}
      </label>
      <Select
        value={value}
        disabled={disabled}
        onValueChange={onValueChange}
      >
        <SelectTrigger className="h-10 w-full border-slate-200 bg-white px-3 text-slate-800 hover:bg-slate-50 data-[placeholder]:text-slate-400">
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              <span className="flex w-full items-center justify-between gap-4">
                <span>{option.label}</span>
                {option.note && (
                  <span className="text-xs text-slate-400">{option.note}</span>
                )}
              </span>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}

function WarningPanel({ warnings }: { warnings: DeanDashboardWarning[] }) {
  return (
    <div className="rounded-2xl border border-amber-200 bg-amber-50/80 p-4">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-amber-100 text-amber-700">
          <AlertTriangle className="size-5" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="font-semibold text-amber-950">
            Cần kiểm tra chất lượng dữ liệu
          </p>
          <div className="mt-2 grid gap-2 lg:grid-cols-2">
            {warnings.map((warning) => (
              <div
                key={`${warning.code}-${warning.references.join("-")}`}
                className="rounded-xl border border-amber-200 bg-white/70 px-3 py-2"
              >
                <p className="text-sm font-medium text-slate-800">
                  {warning.message}
                </p>
                {warning.references.length > 0 && (
                  <p className="mt-1 truncate text-xs text-slate-500">
                    {warning.references.join(", ")}
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function EmptyChart({ message }: { message: string }) {
  return (
    <div className="flex h-full flex-col items-center justify-center text-center">
      <Sparkles className="size-8 text-slate-300" />
      <p className="mt-2 text-sm text-slate-500">{message}</p>
    </div>
  )
}

function DeanDashboardSkeleton() {
  return (
    <div className="space-y-5 animate-pulse">
      <div className="h-56 rounded-[22px] bg-slate-900" />
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <div key={index} className="h-28 rounded-2xl bg-slate-100" />
        ))}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1.45fr_0.85fr]">
        <div className="h-[430px] rounded-2xl bg-slate-100" />
        <div className="h-[430px] rounded-2xl bg-slate-100" />
      </div>
      <div className="h-80 rounded-2xl bg-slate-100" />
    </div>
  )
}

function formatPercent(value: number | null | undefined) {
  return `${Number(value ?? 0).toLocaleString("vi-VN", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 1,
  })}%`
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "—"
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date)
}
