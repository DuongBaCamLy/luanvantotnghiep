import { useMemo, useState, type ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  CircleAlert,
  Clock3,
  FileCheck2,
  FilePenLine,
  GraduationCap,
  Layers3,
  RefreshCw,
  Search,
  ShieldCheck,
  TimerReset,
  UserRoundCheck,
} from "lucide-react"

import {
  dashboardApi,
  type FacultyCourseAssignment,
  type FacultyDeadlineState,
  type FacultyRecommendedAction,
} from "@/api/dashboardApi"
import DashboardHero from "@/components/dashboard/DashboardHero"
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

const ALL_TERMS = "__ALL_TERMS__"
const PRIORITY_TERM = "__PRIORITY_TERM__"

type StatusFilter =
  | "ALL"
  | "ACTION_REQUIRED"
  | "OVERDUE"
  | "IN_PROGRESS"
  | "APPROVED"
  | "NO_DEADLINE"

const STATUS_LABELS: Record<string, string> = {
  NOT_CREATED: "Not created",
  DRAFT: "Draft",
  SUBMITTED: "Pending Department Review",
  UNDER_REVIEW: "Pending Dean Review",
  APPROVED: "Approved",
  REJECTED: "Returned for Revision",
  REVISION_REQUESTED: "Revision Required",
  ARCHIVED: "Archived",
}

function normalizeStatus(
  value: unknown,
) {
  return String(value ?? "")
    .trim()
    .toUpperCase()
}

function statusLabel(
  value: unknown,
) {
  const status =
    normalizeStatus(value)

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
    ?? "Unknown"
  )
}

function isRevisionStatus(
  value: unknown,
) {
  const status =
    normalizeStatus(value)

  return (
    status === "REJECTED"
    || status
      === "REVISION_REQUESTED"
  )
}

function isDraftLike(
  value: unknown,
) {
  const status =
    normalizeStatus(value)

  return (
    status === "NOT_CREATED"
    || status === "DRAFT"
    || status === "REJECTED"
    || status
      === "REVISION_REQUESTED"
  )
}

export default function FacultyDashboardPage() {
  const [
    termFilter,
    setTermFilter,
  ] = useState(PRIORITY_TERM)

  const [
    statusFilter,
    setStatusFilter,
  ] =
    useState<StatusFilter>("ALL")

  const [
    keyword,
    setKeyword,
  ] = useState("")

  const {
    data: dashboard,
    isLoading,
    isError,
    isFetching,
    refetch,
  } = useQuery({
    queryKey: [
      "faculty-dashboard",
      "me",
    ],
    queryFn:
      dashboardApi
        .getMyFacultyDashboard,
    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  const effectiveTerm =
    termFilter === PRIORITY_TERM
      ? (
          dashboard
            ?.defaultTermKey
          ?? ALL_TERMS
        )
      : termFilter

  const visibleAssignments =
    useMemo(() => {
      if (!dashboard) {
        return []
      }

      const normalizedKeyword =
        keyword
          .trim()
          .toLowerCase()

      return dashboard
        .upcomingDeadlines
        .filter(
          (item) => {
            if (
              effectiveTerm
                !== ALL_TERMS
              && item.termKey
                !== effectiveTerm
            ) {
              return false
            }

            if (
              !matchesStatusFilter(
                item,
                statusFilter,
              )
            ) {
              return false
            }

            if (
              !normalizedKeyword
            ) {
              return true
            }

            return [
              item.courseCode,
              item.courseName,
              item.courseNameVn,
              item.departmentCode,
              item.departmentName,
              item.academicYear,
              `Semester ${item.semester}`,
              ...item.rooms,
              ...item.schedules,
            ]
              .filter(Boolean)
              .some(
                (value) =>
                  String(value)
                    .toLowerCase()
                    .includes(
                      normalizedKeyword,
                    ),
              )
          },
        )
        .slice()
        .sort(
          (left, right) => {
            const leftPriority =
              assignmentPriority(
                left,
              )

            const rightPriority =
              assignmentPriority(
                right,
              )

            if (
              leftPriority
              !== rightPriority
            ) {
              return (
                leftPriority
                - rightPriority
              )
            }

            return String(
              left.courseCode,
            ).localeCompare(
              String(
                right.courseCode,
              ),
              "en",
              {
                numeric: true,
              },
            )
          },
        )
    }, [
      dashboard,
      effectiveTerm,
      keyword,
      statusFilter,
    ])

  const dashboardStats =
    useMemo(() => {
      if (!dashboard) {
        return {
          approved: 0,
          inReview: 0,
          draftOrRevision: 0,
          revisionRequired: 0,
          noSyllabus: 0,
        }
      }

      const assignments =
        dashboard.upcomingDeadlines

      return {
        approved:
          assignments.filter(
            (item) =>
              normalizeStatus(
                item.status,
              ) === "APPROVED",
          ).length,

        inReview:
          assignments.filter(
            (item) => [
              "SUBMITTED",
              "UNDER_REVIEW",
            ].includes(
              normalizeStatus(
                item.status,
              ),
            ),
          ).length,

        draftOrRevision:
          assignments.filter(
            (item) =>
              isDraftLike(
                item.status,
              ),
          ).length,

        revisionRequired:
          assignments.filter(
            (item) =>
              isRevisionStatus(
                item.status,
              ),
          ).length,

        noSyllabus:
          assignments.filter(
            (item) =>
              normalizeStatus(
                item.status,
              )
              === "NOT_CREATED",
          ).length,
      }
    }, [
      dashboard,
    ])

  if (isLoading) {
    return (
      <FacultyDashboardSkeleton />
    )
  }

  if (
    isError
    || !dashboard
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
                  Check the backend connection and confirm that this account is linked to an instructor profile with active teaching assignments.
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

  const completionRate =
    dashboard.assignedCourses
      ? Math.round(
          (
            dashboard
              .completedSyllabuses
            / dashboard
                .assignedCourses
          )
          * 100,
        )
      : 0

  const selectedTermSummary =
    dashboard.terms.find(
      (term) =>
        term.key
        === effectiveTerm,
    )

  const hasUrgentWork =
    dashboard.overdueCourses
      > 0
    || dashboard.dueSoonCourses
      > 0
    || dashboardStats
      .revisionRequired
      > 0

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <DashboardHero
        roleLabel="Instructor"
        title="Faculty Workspace"
        description="Manage assigned courses, prepare and revise syllabuses, follow official submission deadlines, and track each syllabus through department and Dean review."
        actions={
          <>
            <Button
              asChild
              className="bg-white text-[#07515a] hover:bg-white/90"
            >
              <Link to="/instructor/syllabus">
                <FilePenLine className="size-4" />
                My Syllabuses
              </Link>
            </Button>

            <Button
              asChild
              variant="outline"
              className="border-white/50 bg-white/10 text-white hover:bg-white/20 hover:text-white"
            >
              <Link to="/instructor/class-sections">
                <BookOpen className="size-4" />
                Class Sections
              </Link>
            </Button>

            <Button
              type="button"
              variant="outline"
              className="border-white/50 bg-white/10 text-white hover:bg-white/20 hover:text-white"
              disabled={isFetching}
              onClick={() =>
                void refetch()
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
          </>
        }
      />

      <section className="rounded-xl border border-[#cfe1e4] bg-[#f7fbfb] px-5 py-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-3">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#eaf7f7] text-[#007d84]">
              <GraduationCap className="size-5" />
            </span>

            <div>
              <p className="font-semibold text-[#17343d]">
                {dashboard.instructorName}
                {dashboard.staffCode
                  ? ` · ${dashboard.staffCode}`
                  : ""}
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                {dashboard.departmentCode
                  ?? "Department not assigned"}
                {dashboard.departmentName
                  ? ` · ${dashboard.departmentName}`
                  : ""}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-x-5 gap-y-2 text-[11px] text-slate-500">
            <span>
              Last synchronized:{" "}
              <strong className="font-semibold text-slate-700">
                {formatDateTime(
                  dashboard.generatedAt,
                )}
              </strong>
            </span>

            <span>
              Business time zone:{" "}
              <strong className="font-semibold text-slate-700">
                {dashboard.timeZone}
              </strong>
            </span>
          </div>
        </div>
      </section>

      {hasUrgentWork && (
        <section className="rounded-xl border border-amber-200 bg-amber-50 px-5 py-4 shadow-sm">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" />

              <div>
                <p className="font-semibold text-amber-950">
                  Academic work requires your attention
                </p>

                <p className="mt-1 text-sm leading-6 text-amber-800">
                  {dashboardStats
                    .revisionRequired
                    > 0 && (
                    <>
                      <strong>
                        {dashboardStats
                          .revisionRequired}
                      </strong>{" "}
                      syllabus/syllabi require revision.{" "}
                    </>
                  )}

                  {dashboard.overdueCourses
                    > 0 && (
                    <>
                      <strong>
                        {dashboard
                          .overdueCourses}
                      </strong>{" "}
                      course(s) are past the official deadline.{" "}
                    </>
                  )}

                  {dashboard.dueSoonCourses
                    > 0 && (
                    <>
                      <strong>
                        {dashboard
                          .dueSoonCourses}
                      </strong>{" "}
                      course(s) are due within 7 days.
                    </>
                  )}
                </p>
              </div>
            </div>

            <Button
              asChild
              variant="outline"
              size="sm"
              className="shrink-0 border-amber-300 bg-white text-amber-800 hover:bg-amber-100"
            >
              <Link to="/instructor/syllabus">
                Open My Syllabuses
                <ArrowRight className="size-4" />
              </Link>
            </Button>
          </div>
        </section>
      )}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
        <SummaryCard
          label="Assigned Courses"
          value={
            dashboard.assignedCourses
          }
          helper={`${dashboard.assignedSections} active class section(s)`}
          icon={
            <BookOpen className="size-5" />
          }
          tone="navy"
        />

        <SummaryCard
          label="Approved"
          value={
            dashboard
              .completedSyllabuses
          }
          helper={`${completionRate}% complete`}
          icon={
            <FileCheck2 className="size-5" />
          }
          tone="emerald"
        />

        <SummaryCard
          label="In Review"
          value={
            dashboard
              .inProgressSyllabuses
          }
          helper="Department / Dean review"
          icon={
            <Clock3 className="size-5" />
          }
          tone="blue"
        />

        <SummaryCard
          label="Action Required"
          value={
            dashboard
              .actionRequiredCourses
          }
          helper="Create, edit or revise"
          icon={
            <FilePenLine className="size-5" />
          }
          tone="amber"
        />

        <SummaryCard
          label="Overdue"
          value={
            dashboard
              .overdueCourses
          }
          helper="Past official deadline"
          icon={
            <TimerReset className="size-5" />
          }
          tone="rose"
        />

        <SummaryCard
          label="No Deadline"
          value={
            dashboard
              .unconfiguredDeadlineCourses
          }
          helper="Awaiting administrator configuration"
          icon={
            <CalendarClock className="size-5" />
          }
          tone="slate"
        />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
        <Card className="border-slate-200 shadow-sm">
          <CardHeader className="border-b border-slate-100 pb-4">
            <CardTitle className="text-base font-bold text-[#17343d]">
              Syllabus Completion
            </CardTitle>

            <p className="text-xs leading-5 text-slate-500">
              A course is counted as complete only when the current syllabus version has received final approval.
            </p>
          </CardHeader>

          <CardContent className="p-6">
            <div className="flex items-end justify-between gap-4">
              <div>
                <p className="text-4xl font-bold text-slate-950">
                  {completionRate}%
                </p>

                <p className="mt-1 text-sm text-slate-500">
                  {dashboard
                    .completedSyllabuses}
                  /
                  {dashboard
                    .assignedCourses}{" "}
                  assigned courses approved
                </p>
              </div>

              <Badge
                variant="outline"
                className="border-[#cfe1e4] bg-[#f5fbfb] text-[#007d84]"
              >
                Final approval only
              </Badge>
            </div>

            <div className="mt-5 h-2.5 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-[#007d84] transition-all"
                style={{
                  width:
                    `${Math.min(
                      Math.max(
                        completionRate,
                        0,
                      ),
                      100,
                    )}%`,
                }}
              />
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <WorkflowStat
                label="Preparation / Revision"
                value={
                  dashboardStats
                    .draftOrRevision
                }
                icon={
                  <FilePenLine className="size-4" />
                }
              />

              <WorkflowStat
                label="In Review"
                value={
                  dashboardStats
                    .inReview
                }
                icon={
                  <Clock3 className="size-4" />
                }
              />

              <WorkflowStat
                label="Approved"
                value={
                  dashboardStats
                    .approved
                }
                icon={
                  <CheckCircle2 className="size-4" />
                }
              />
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 shadow-sm">
          <CardHeader className="border-b border-slate-100 pb-4">
            <CardTitle className="text-base font-bold text-[#17343d]">
              Priority Academic Term
            </CardTitle>

            <p className="text-xs leading-5 text-slate-500">
              Official syllabus deadline and workload for the term currently prioritized by the dashboard service.
            </p>
          </CardHeader>

          <CardContent className="p-6">
            {selectedTermSummary ? (
              <div className="space-y-4">
                <TermRow
                  label="Academic Term"
                  value={`Semester ${selectedTermSummary.semester} · ${selectedTermSummary.academicYear}`}
                />

                <TermRow
                  label="Assigned Courses"
                  value={String(
                    selectedTermSummary
                      .courseCount,
                  )}
                />

                <TermRow
                  label="Class Sections"
                  value={String(
                    selectedTermSummary
                      .sectionCount,
                  )}
                />

                <TermRow
                  label="Official Deadline"
                  value={
                    selectedTermSummary
                      .deadline
                      ? formatDateTime(
                          selectedTermSummary
                            .deadline,
                        )
                      : "Not configured"
                  }
                  warning={
                    !selectedTermSummary
                      .deadlineConfigured
                  }
                />

                <TermRow
                  label="Action Required"
                  value={`${selectedTermSummary.actionRequiredCount} course(s)`}
                  warning={
                    selectedTermSummary
                      .actionRequiredCount
                      > 0
                  }
                />

                <TermRow
                  label="Overdue"
                  value={`${selectedTermSummary.overdueCount} course(s)`}
                  warning={
                    selectedTermSummary
                      .overdueCount
                      > 0
                  }
                />
              </div>
            ) : (
              <div className="rounded-xl border border-slate-100 bg-slate-50 p-5 text-sm text-slate-500">
                No priority academic term is available. Select another term in the course table below.
              </div>
            )}
          </CardContent>
        </Card>
      </section>

      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="border-b border-slate-100 bg-white pb-5">
          <div className="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
            <div>
              <CardTitle className="flex items-center gap-2 text-xl text-[#17343d]">
                <Layers3 className="size-5 text-[#007d84]" />
                My Course Responsibilities
              </CardTitle>

              <p className="mt-1 text-sm leading-6 text-slate-500">
                One row per assigned course and academic term, with syllabus status, official deadline, remaining time, and the next recommended action.
              </p>
            </div>

            <div className="grid gap-2 sm:grid-cols-3 xl:min-w-[780px]">
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

                <Input
                  value={keyword}
                  onChange={(
                    event,
                  ) =>
                    setKeyword(
                      event.target.value,
                    )
                  }
                  placeholder="Search course, room, term..."
                  className="pl-9"
                />
              </div>

              <Select
                value={termFilter}
                onValueChange={
                  setTermFilter
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Academic term" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem
                    value={PRIORITY_TERM}
                  >
                    Priority term
                  </SelectItem>

                  <SelectItem
                    value={ALL_TERMS}
                  >
                    All academic terms
                  </SelectItem>

                  {dashboard.terms.map(
                    (term) => (
                      <SelectItem
                        key={term.key}
                        value={term.key}
                      >
                        Semester {term.semester} · {term.academicYear}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>

              <Select
                value={statusFilter}
                onValueChange={(value) =>
                  setStatusFilter(
                    value as StatusFilter,
                  )
                }
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Syllabus status" />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value="ALL">
                    All statuses
                  </SelectItem>

                  <SelectItem value="ACTION_REQUIRED">
                    Action required
                  </SelectItem>

                  <SelectItem value="OVERDUE">
                    Overdue
                  </SelectItem>

                  <SelectItem value="IN_PROGRESS">
                    In review
                  </SelectItem>

                  <SelectItem value="APPROVED">
                    Approved
                  </SelectItem>

                  <SelectItem value="NO_DEADLINE">
                    No deadline
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {visibleAssignments.length
            === 0 ? (
            <div className="px-6 py-16 text-center">
              <div className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500">
                <BookOpen className="size-6" />
              </div>

              <p className="mt-4 font-semibold text-slate-900">
                No assigned courses match the selected filters.
              </p>

              <p className="mt-1 text-sm text-slate-500">
                Change the academic term, status, or search keyword to view more assignments.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table className="min-w-[1280px]">
                <TableHeader>
                  <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
                    <TableHead className="min-w-[270px]">
                      Course
                    </TableHead>

                    <TableHead className="min-w-[210px]">
                      Teaching Assignment
                    </TableHead>

                    <TableHead className="min-w-[190px]">
                      Syllabus
                    </TableHead>

                    <TableHead className="min-w-[210px]">
                      Official Deadline
                    </TableHead>

                    <TableHead className="min-w-[175px]">
                      Time Remaining
                    </TableHead>

                    <TableHead className="min-w-[160px] text-right">
                      Next Action
                    </TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {visibleAssignments.map(
                    (item) => (
                      <AssignmentRow
                        key={
                          `${item.courseId}-${item.termKey}`
                        }
                        item={item}
                      />
                    ),
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      <section className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#708894]">
              Syllabus Workflow
            </p>

            <p className="mt-1 text-sm font-semibold text-[#17343d]">
              Draft → Department Review → Dean Review → Approved
            </p>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              A returned syllabus remains traceable in version and review history. Continue work only on the new or current Draft revision assigned to you.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              asChild
              variant="outline"
              size="sm"
            >
              <Link to="/instructor/syllabus">
                <FileCheck2 className="size-3.5" />
                Open My Syllabuses
              </Link>
            </Button>

            <Button
              asChild
              variant="outline"
              size="sm"
            >
              <Link to="/instructor/class-sections">
                <UserRoundCheck className="size-3.5" />
                View Class Sections
              </Link>
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}

function AssignmentRow({
  item,
}: {
  item: FacultyCourseAssignment
}) {
  const action =
    getAssignmentAction(item)

  return (
    <TableRow className="align-top hover:bg-[#f8fbfb]">
      <TableCell className="py-4">
        <div className="flex items-start gap-3">
          <div className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-xl bg-[#007d84] text-xs font-bold text-white">
            {item.courseCode
              .slice(0, 2)
              .toUpperCase()}
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-mono text-sm font-bold text-[#007d84]">
                {item.courseCode}
              </span>

              {item.totalCredits
                > 0 && (
                <Badge variant="outline">
                  {item.totalCredits} credits
                </Badge>
              )}
            </div>

            <p className="mt-1 font-semibold leading-5 text-slate-900">
              {item.courseName}
            </p>

            {item.courseNameVn
              && item.courseNameVn
                !== item.courseName && (
                <p className="mt-0.5 text-xs leading-5 text-slate-500">
                  {item.courseNameVn}
                </p>
              )}

            {item.dataQualityState
              !== "CONSISTENT"
              && item.dataQualityState
                !== "UNLINKED" && (
                <div className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-amber-700">
                  <AlertTriangle className="size-3.5" />
                  Teaching assignment linkage requires administrative review
                </div>
              )}
          </div>
        </div>
      </TableCell>

      <TableCell className="py-4 text-sm">
        <p className="font-semibold text-slate-900">
          Semester {item.semester} · {item.academicYear}
        </p>

        <p className="mt-1 text-slate-500">
          {item.sectionCount} section(s)
          {item.groupNumbers.length
            > 0
            ? ` · Group ${item.groupNumbers.join(", ")}`
            : ""}
        </p>

        {item.sectionTypes.length
          > 0 && (
          <p className="mt-1 text-xs text-slate-400">
            {item.sectionTypes.join(", ")}
          </p>
        )}

        {item.rooms.length > 0 && (
          <p className="mt-1 text-xs text-slate-400">
            Room: {item.rooms.join(", ")}
          </p>
        )}

        {item.schedules.length
          > 0 && (
          <p className="mt-1 text-xs text-slate-400">
            {item.schedules.join(" · ")}
          </p>
        )}
      </TableCell>

      <TableCell className="py-4">
        <div className="space-y-2">
          <SyllabusStatusBadge
            status={item.status}
          />

          {item.syllabusVersionLabel
            && (
            <p className="text-xs text-slate-500">
              {item.syllabusVersionLabel}
              {item.currentVersion
                ? " · Current"
                : ""}
            </p>
          )}

          {!item.syllabusId && (
            <p className="text-xs text-slate-400">
              No syllabus version linked yet
            </p>
          )}
        </div>
      </TableCell>

      <TableCell className="py-4 text-sm">
        {item.deadline ? (
          <div>
            <p className="font-semibold text-slate-800">
              {formatDateTime(
                item.deadline,
              )}
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Official submission deadline
            </p>
          </div>
        ) : (
          <div>
            <p className="font-medium text-slate-500">
              Not configured
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Awaiting administrator setup
            </p>
          </div>
        )}
      </TableCell>

      <TableCell className="py-4">
        <DeadlineStatusBadge
          state={item.deadlineState}
          minutesRemaining={
            item.minutesRemaining
          }
        />
      </TableCell>

      <TableCell className="py-4">
        <div className="flex justify-end">
          <Button
            asChild
            size="sm"
            className={
              item.actionRequired
                ? "bg-[#007d84] text-white hover:bg-[#006d73]"
                : ""
            }
            variant={
              item.actionRequired
                ? "default"
                : "outline"
            }
          >
            <Link to={action.path}>
              {action.label}
              <ArrowRight className="size-3.5" />
            </Link>
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}

function SyllabusStatusBadge({
  status,
}: {
  status: string
}) {
  const normalized =
    normalizeStatus(status)

  if (
    normalized === "APPROVED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-emerald-200 bg-emerald-50 text-emerald-700"
      >
        <CheckCircle2 className="mr-1 size-3" />
        Approved
      </Badge>
    )
  }

  if (
    normalized === "SUBMITTED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-amber-200 bg-amber-50 text-amber-700"
      >
        <Clock3 className="mr-1 size-3" />
        Pending Department Review
      </Badge>
    )
  }

  if (
    normalized
      === "UNDER_REVIEW"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-blue-200 bg-blue-50 text-blue-700"
      >
        <ShieldCheck className="mr-1 size-3" />
        Pending Dean Review
      </Badge>
    )
  }

  if (
    normalized === "REJECTED"
    || normalized
      === "REVISION_REQUESTED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700"
      >
        <AlertTriangle className="mr-1 size-3" />
        {statusLabel(normalized)}
      </Badge>
    )
  }

  if (
    normalized === "DRAFT"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-orange-200 bg-orange-50 text-orange-700"
      >
        <FilePenLine className="mr-1 size-3" />
        Draft
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="border-slate-200 bg-slate-50 text-slate-600"
    >
      {statusLabel(normalized)}
    </Badge>
  )
}

function DeadlineStatusBadge({
  state,
  minutesRemaining,
}: {
  state: FacultyDeadlineState
  minutesRemaining: number | null
}) {
  const countdown =
    formatCountdown(
      minutesRemaining,
    )

  if (
    state === "COMPLETED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-emerald-200 bg-emerald-50 text-emerald-700"
      >
        <CheckCircle2 className="mr-1 size-3" />
        Completed
      </Badge>
    )
  }

  if (
    state === "SUBMITTED"
    || state === "IN_REVIEW"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-blue-200 bg-blue-50 text-blue-700"
      >
        <Clock3 className="mr-1 size-3" />
        {state === "IN_REVIEW"
          ? "In review"
          : "Submitted"}
      </Badge>
    )
  }

  if (
    state === "NOT_CONFIGURED"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-slate-200 bg-slate-50 text-slate-600"
      >
        No deadline
      </Badge>
    )
  }

  if (
    state === "OVERDUE"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700"
      >
        <AlertTriangle className="mr-1 size-3" />
        Overdue {countdown}
      </Badge>
    )
  }

  if (
    state === "DUE_TODAY"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-rose-200 bg-rose-50 text-rose-700"
      >
        Due today
      </Badge>
    )
  }

  if (
    state === "DUE_SOON"
  ) {
    return (
      <Badge
        variant="outline"
        className="border-amber-200 bg-amber-50 text-amber-700"
      >
        {countdown} remaining
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="border-slate-200 bg-white text-slate-700"
    >
      {countdown} remaining
    </Badge>
  )
}

function getAssignmentAction(
  item: FacultyCourseAssignment,
): {
  label: string
  path: string
} {
  const base =
    "/instructor/syllabus"

  const actions:
    Record<
      FacultyRecommendedAction,
      string
    > = {
      CREATE:
        "Create Syllabus",

      EDIT:
        "Continue Draft",

      REVISE:
        "Revise Syllabus",

      VIEW_PROGRESS:
        "View Progress",

      VIEW_APPROVED:
        "View Approved",

      VIEW_HISTORY:
        "View History",
    }

  if (
    item.recommendedAction
      === "CREATE"
  ) {
    return {
      label: actions.CREATE,
      path:
        `${base}/create`
        + `?classSectionId=${item.primaryClassSectionId}`,
    }
  }

  if (
    [
      "EDIT",
      "REVISE",
    ].includes(
      item.recommendedAction,
    )
    && item.syllabusId
  ) {
    return {
      label:
        actions[
          item.recommendedAction
        ],
      path:
        `${base}/${item.syllabusId}/editor`,
    }
  }

  return {
    label:
      actions[
        item.recommendedAction
      ],

    path:
      item.syllabusId
        ? `${base}/${item.syllabusId}`
        : base,
  }
}

function matchesStatusFilter(
  item: FacultyCourseAssignment,
  filter: StatusFilter,
): boolean {
  switch (filter) {
    case "ACTION_REQUIRED":
      return item.actionRequired

    case "OVERDUE":
      return (
        item.deadlineState
        === "OVERDUE"
      )

    case "IN_PROGRESS":
      return [
        "SUBMITTED",
        "UNDER_REVIEW",
      ].includes(
        normalizeStatus(
          item.status,
        ),
      )

    case "APPROVED":
      return (
        normalizeStatus(
          item.status,
        ) === "APPROVED"
      )

    case "NO_DEADLINE":
      return (
        item.deadlineState
        === "NOT_CONFIGURED"
      )

    default:
      return true
  }
}

function assignmentPriority(
  item: FacultyCourseAssignment,
) {
  if (
    item.deadlineState
      === "OVERDUE"
  ) {
    return 0
  }

  if (
    item.deadlineState
      === "DUE_TODAY"
  ) {
    return 1
  }

  if (
    isRevisionStatus(
      item.status,
    )
  ) {
    return 2
  }

  if (
    item.deadlineState
      === "DUE_SOON"
  ) {
    return 3
  }

  if (
    item.actionRequired
  ) {
    return 4
  }

  if (
    normalizeStatus(
      item.status,
    ) === "UNDER_REVIEW"
  ) {
    return 5
  }

  if (
    normalizeStatus(
      item.status,
    ) === "SUBMITTED"
  ) {
    return 6
  }

  if (
    normalizeStatus(
      item.status,
    ) === "APPROVED"
  ) {
    return 7
  }

  return 8
}

function formatCountdown(
  minutes: number | null,
): string {
  if (
    minutes === null
  ) {
    return "—"
  }

  const absoluteMinutes =
    Math.abs(minutes)

  const days =
    Math.floor(
      absoluteMinutes
      / 1_440,
    )

  const hours =
    Math.floor(
      (
        absoluteMinutes
        % 1_440
      )
      / 60,
    )

  const remainingMinutes =
    absoluteMinutes % 60

  if (days > 0) {
    return hours > 0
      ? `${days}d ${hours}h`
      : `${days}d`
  }

  if (hours > 0) {
    return remainingMinutes > 0
      ? `${hours}h ${remainingMinutes}m`
      : `${hours}h`
  }

  return `${remainingMinutes}m`
}

function formatDateTime(
  value: string,
): string {
  const date =
    new Date(value)

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return value.replace(
      "T",
      " ",
    )
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

function SummaryCard({
  label,
  value,
  helper,
  icon,
  tone,
}: {
  label: string
  value: number
  helper: string
  icon: ReactNode
  tone:
    | "navy"
    | "emerald"
    | "blue"
    | "amber"
    | "rose"
    | "slate"
}) {
  const toneClass =
    tone === "emerald"
      ? "bg-emerald-50 text-emerald-700"
      : tone === "blue"
        ? "bg-blue-50 text-blue-700"
        : tone === "amber"
          ? "bg-amber-50 text-amber-700"
          : tone === "rose"
            ? "bg-rose-50 text-rose-700"
            : tone === "slate"
              ? "bg-slate-100 text-slate-600"
              : "bg-[#007d84] text-white"

  return (
    <Card className="border-slate-200 bg-white shadow-sm">
      <CardContent className="flex min-h-[126px] items-start justify-between gap-4 p-5">
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-[0.08em] text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-3xl font-bold text-slate-950">
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {helper}
          </p>
        </div>

        <span
          className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${toneClass}`}
        >
          {icon}
        </span>
      </CardContent>
    </Card>
  )
}

function WorkflowStat({
  label,
  value,
  icon,
}: {
  label: string
  value: number
  icon: ReactNode
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-100 bg-slate-50 px-4 py-3">
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <span className="text-[#007d84]">
          {icon}
        </span>

        <span>{label}</span>
      </div>

      <strong className="text-slate-900">
        {value}
      </strong>
    </div>
  )
}

function TermRow({
  label,
  value,
  warning = false,
}: {
  label: string
  value: string
  warning?: boolean
}) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-3 last:border-b-0 last:pb-0">
      <span className="text-sm text-slate-500">
        {label}
      </span>

      <span
        className={
          warning
            ? "text-right text-sm font-semibold text-amber-700"
            : "text-right text-sm font-semibold text-slate-900"
        }
      >
        {value}
      </span>
    </div>
  )
}

function FacultyDashboardSkeleton() {
  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10 animate-pulse">
      <div className="h-60 rounded-2xl bg-slate-200" />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-6">
        {Array.from({
          length: 6,
        }).map(
          (_, index) => (
            <div
              key={index}
              className="h-32 rounded-xl bg-slate-100"
            />
          ),
        )}
      </div>

      <div className="h-72 rounded-xl bg-slate-100" />

      <div className="h-96 rounded-xl bg-slate-100" />
    </div>
  )
}