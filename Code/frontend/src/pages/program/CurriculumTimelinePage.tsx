import { useMemo, useState } from "react"
import {
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  GitCompareArrows,
  History,
  MinusCircle,
  PlusCircle,
  RefreshCw,
} from "lucide-react"

import { programApi } from "@/api/programApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import type {
  CurriculumTimelineCourseChange,
  CurriculumTimelineItem,
} from "@/types/admin"

function baseProgramCode(value: unknown): string {
  const code = String(value ?? "").trim()
  if (!code) return "—"

  return (
    code
      .replace(/[-_\s]?20\d{2}$/i, "")
      .replace(/[-_\s]+$/g, "")
      .trim()
    || code
  )
}

function getRoleBase(pathname: string): "/admin" | "/dean" {
  return pathname.startsWith("/dean") ? "/dean" : "/admin"
}

function getErrorMessage(error: unknown): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: { message?: string }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return "Unable to load curriculum change history."
}

export default function CurriculumTimelinePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const roleBase = getRoleBase(location.pathname)
  const programId = Number(id)
  const validProgramId = Number.isInteger(programId) && programId > 0

  const [expandedCohorts, setExpandedCohorts] = useState<Set<number>>(
    () => new Set<number>(),
  )

  const {
    data: program,
    isLoading: isProgramLoading,
    isError: isProgramError,
  } = useQuery({
    queryKey: ["program", programId],
    queryFn: () => programApi.getById(programId),
    enabled: validProgramId,
  })

  const {
    data: timeline = [],
    isLoading: isTimelineLoading,
    isError: isTimelineError,
    error: timelineError,
  } = useQuery({
    queryKey: ["curriculum-timeline", programId],
    queryFn: () => programApi.getCurriculumTimeline(programId),
    enabled: validProgramId,
  })

  const totalChanges = useMemo(
    () =>
      timeline.reduce(
        (sum, item) => sum + Number(item.changeCount ?? 0),
        0,
      ),
    [timeline],
  )

  const latestCohort = timeline.at(-1)
  const baselineCohort = timeline.find((item) => item.baseline)

  const toggleExpanded = (cohortId: number) => {
    setExpandedCohorts((current) => {
      const next = new Set(current)
      if (next.has(cohortId)) {
        next.delete(cohortId)
      } else {
        next.add(cohortId)
      }
      return next
    })
  }

  if (!validProgramId) {
    return (
      <Card className="border-rose-200 bg-rose-50">
        <CardContent className="p-6 text-sm text-rose-700">
          Invalid curriculum program ID.
        </CardContent>
      </Card>
    )
  }

  const loading = isProgramLoading || isTimelineLoading
  const hasError = isProgramError || isTimelineError

  return (
    <div className="mx-auto w-full max-w-[1450px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <History className="size-5" />
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Curriculum Evolution
              </p>
              <h1 className="mt-1 text-[26px] font-bold tracking-[-0.4px] text-[#17343d]">
                Curriculum Change History
              </h1>
              <p className="mt-1 text-sm text-[#687f89]">
                {program
                  ? `${baseProgramCode(program.code)} · ${program.name}`
                  : "Track curriculum changes across consecutive cohorts."}
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(`${roleBase}/programs`)}
          >
            <ArrowLeft className="size-4" />
            Back to Programs
          </Button>
        </div>

        <div className="grid border-t border-slate-200 sm:grid-cols-3">
          <SummaryItem
            icon={CalendarDays}
            label="Cohorts"
            value={String(timeline.length)}
            helper={baselineCohort ? `Baseline: ${baselineCohort.cohortName}` : "No baseline yet"}
          />
          <SummaryItem
            icon={GitCompareArrows}
            label="Total Changes"
            value={String(totalChanges)}
            helper="Across consecutive cohorts"
          />
          <SummaryItem
            icon={History}
            label="Latest Cohort"
            value={latestCohort?.cohortName ?? "—"}
            helper={latestCohort ? `Entry year ${latestCohort.entryYear}` : "No cohort yet"}
          />
        </div>
      </section>

      {loading && (
        <Card>
          <CardContent className="p-8 text-center text-sm text-slate-500">
            Loading curriculum change history...
          </CardContent>
        </Card>
      )}

      {hasError && !loading && (
        <Card className="border-rose-200 bg-rose-50">
          <CardContent className="p-6 text-sm text-rose-700">
            {getErrorMessage(timelineError)}
          </CardContent>
        </Card>
      )}

      {!loading && !hasError && timeline.length === 0 && (
        <Card>
          <CardContent className="flex min-h-44 flex-col items-center justify-center p-6 text-center">
            <History className="size-10 text-slate-300" />
            <p className="mt-3 font-semibold text-slate-800">
              No curriculum history is available yet.
            </p>
            <p className="mt-1 max-w-xl text-sm text-slate-500">
              The timeline appears after at least one cohort is configured for this curriculum program.
            </p>
          </CardContent>
        </Card>
      )}

      {!loading && !hasError && timeline.length > 0 && totalChanges === 0 && (
        <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          <CheckCircle2 className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">No curriculum differences detected between consecutive cohorts.</p>
            <p className="mt-0.5 text-emerald-700">
              Cohorts are still shown below so reviewers can verify the baseline and course counts.
            </p>
          </div>
        </div>
      )}

      {!loading && !hasError && timeline.length > 0 && (
        <div className="relative space-y-4 before:absolute before:bottom-8 before:left-[21px] before:top-8 before:w-px before:bg-slate-200 sm:before:left-[29px]">
          {timeline.map((item, index) => {
            const expanded = expandedCohorts.has(item.cohortId)
            const latest = index === timeline.length - 1

            return (
              <article key={item.cohortId} className="relative pl-12 sm:pl-16">
                <div className="absolute left-0 top-6 flex size-11 items-center justify-center rounded-full border-4 border-white bg-[#007d84] text-sm font-bold text-white shadow-sm sm:left-2">
                  {index + 1}
                </div>

                <Card className="overflow-hidden border-slate-200 shadow-sm">
                  <div className="flex flex-col gap-4 border-b border-slate-100 bg-slate-50/60 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h2 className="text-lg font-bold text-slate-900">
                          {item.cohortName}
                        </h2>
                        <Badge variant="outline">Entry Year {item.entryYear}</Badge>
                        {item.baseline && (
                          <Badge className="border border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-50">
                            Baseline
                          </Badge>
                        )}
                        {latest && !item.baseline && (
                          <Badge className="border border-[#b9dfe1] bg-[#eef8f8] text-[#007d84] hover:bg-[#eef8f8]">
                            Latest
                          </Badge>
                        )}
                      </div>

                      <p className="mt-1.5 text-sm text-slate-500">
                        {item.baseline
                          ? "Starting curriculum snapshot used as the comparison baseline."
                          : `${item.previousCohortName ?? "Previous cohort"} → ${item.cohortName}`}
                      </p>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="outline" className="bg-white">
                        {item.totalCourses} courses
                      </Badge>
                      {!item.baseline && (
                        <Badge
                          className={
                            item.changeCount > 0
                              ? "border border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-50"
                              : "border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-50"
                          }
                        >
                          {item.changeCount} changes
                        </Badge>
                      )}
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => toggleExpanded(item.cohortId)}
                      >
                        {expanded ? (
                          <ChevronDown className="size-4" />
                        ) : (
                          <ChevronRight className="size-4" />
                        )}
                        {expanded ? "Hide Details" : "View Details"}
                      </Button>
                    </div>
                  </div>

                  {!item.baseline && (
                    <div className="grid gap-0 border-b border-slate-100 sm:grid-cols-3">
                      <MiniSummary
                        icon={PlusCircle}
                        label="Added"
                        value={item.addedCourses.length}
                        tone="added"
                      />
                      <MiniSummary
                        icon={MinusCircle}
                        label="Removed"
                        value={item.removedCourses.length}
                        tone="removed"
                      />
                      <MiniSummary
                        icon={RefreshCw}
                        label="Modified"
                        value={item.changedCourses.length}
                        tone="modified"
                      />
                    </div>
                  )}

                  {expanded && (
                    <CardContent className="p-5">
                      {item.baseline ? (
                        <BaselineCourseList item={item} />
                      ) : item.changeCount === 0 ? (
                        <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-sm text-emerald-800">
                          <CheckCircle2 className="mt-0.5 size-5 shrink-0" />
                          <div>
                            <p className="font-semibold">No curriculum configuration changes.</p>
                            <p className="mt-1 text-emerald-700">
                              Course membership, course group, required/elective flag, recommended year and semester are unchanged from the previous cohort.
                            </p>
                          </div>
                        </div>
                      ) : (
                        <div className="grid gap-4 xl:grid-cols-3">
                          <ChangeSection
                            title="Added Courses"
                            icon={PlusCircle}
                            items={item.addedCourses}
                            tone="added"
                          />
                          <ChangeSection
                            title="Removed Courses"
                            icon={MinusCircle}
                            items={item.removedCourses}
                            tone="removed"
                          />
                          <ChangeSection
                            title="Modified Courses"
                            icon={RefreshCw}
                            items={item.changedCourses}
                            tone="modified"
                          />
                        </div>
                      )}
                    </CardContent>
                  )}
                </Card>
              </article>
            )
          })}
        </div>
      )}
    </div>
  )
}

function BaselineCourseList({ item }: { item: CurriculumTimelineItem }) {
  if (item.currentCourses.length === 0) {
    return (
      <p className="text-sm text-slate-500">
        The baseline cohort does not contain any curriculum courses.
      </p>
    )
  }

  return (
    <div>
      <div className="mb-3 flex items-center gap-2">
        <BookOpen className="size-4 text-[#007d84]" />
        <h3 className="text-sm font-semibold text-slate-800">
          Baseline Courses
        </h3>
        <Badge variant="outline">{item.currentCourses.length}</Badge>
      </div>

      <div className="grid gap-2.5 md:grid-cols-2 xl:grid-cols-3">
        {item.currentCourses.map((course) => (
          <div key={course.courseId} className="rounded-xl border border-slate-200 bg-white p-3">
            <div className="flex items-start justify-between gap-2">
              <span className="font-mono text-sm font-bold text-[#007d84]">
                {course.courseCode}
              </span>
              <Badge variant="outline" className="shrink-0 text-xs">
                {course.required === true
                  ? "Required"
                  : course.required === false
                    ? "Elective"
                    : "Not specified"}
              </Badge>
            </div>
            <p className="mt-1.5 text-sm font-medium text-slate-800">
              {course.courseName}
            </p>
            <p className="mt-1 text-xs text-slate-500">
              {course.courseType || "Uncategorized"}
              {course.semesterSuggest
                ? ` · Semester ${course.semesterSuggest}`
                : ""}
            </p>
          </div>
        ))}
      </div>
    </div>
  )
}

function ChangeSection({
  title,
  icon: Icon,
  items,
  tone,
}: {
  title: string
  icon: React.ElementType
  items: CurriculumTimelineCourseChange[]
  tone: "added" | "removed" | "modified"
}) {
  const toneClass = {
    added: "border-emerald-200 bg-emerald-50/40",
    removed: "border-rose-200 bg-rose-50/40",
    modified: "border-amber-200 bg-amber-50/40",
  }[tone]

  return (
    <section className={`rounded-xl border p-4 ${toneClass}`}>
      <div className="mb-3 flex items-center justify-between gap-2">
        <h3 className="flex items-center gap-2 text-sm font-bold text-slate-800">
          <Icon className="size-4" />
          {title}
        </h3>
        <Badge variant="outline" className="bg-white">
          {items.length}
        </Badge>
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-slate-500">No courses in this category.</p>
      ) : (
        <div className="space-y-2.5">
          {items.map((item) => (
            <div
              key={`${item.changeType}-${item.courseId}`}
              className="rounded-lg border border-white/80 bg-white p-3 shadow-sm"
            >
              <p className="font-mono text-sm font-bold text-[#17343d]">
                {item.courseCode}
              </p>
              <p className="mt-1 text-sm text-slate-700">
                {item.courseName}
              </p>

              {item.changes.length > 0 && (
                <div className="mt-3 space-y-2 border-t border-slate-100 pt-3">
                  {item.changes.map((change) => (
                    <div key={change.field} className="text-xs">
                      <p className="font-semibold text-slate-600">
                        {change.label}
                      </p>
                      <div className="mt-1 flex flex-wrap items-center gap-1.5">
                        <span className="rounded bg-rose-50 px-2 py-1 text-rose-700">
                          {change.oldValue || "—"}
                        </span>
                        <span className="text-slate-400">→</span>
                        <span className="rounded bg-emerald-50 px-2 py-1 text-emerald-700">
                          {change.newValue || "—"}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function SummaryItem({
  icon: Icon,
  label,
  value,
  helper,
}: {
  icon: React.ElementType
  label: string
  value: string
  helper: string
}) {
  return (
    <div className="flex items-start gap-3 border-b border-slate-200 px-5 py-4 last:border-b-0 sm:border-b-0 sm:border-r sm:last:border-r-0">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-700">
        <Icon className="size-5" />
      </span>
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
          {label}
        </p>
        <p className="mt-1 text-base font-bold text-slate-900">
          {value}
        </p>
        <p className="mt-1 text-[11px] text-slate-500">
          {helper}
        </p>
      </div>
    </div>
  )
}

function MiniSummary({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: React.ElementType
  label: string
  value: number
  tone: "added" | "removed" | "modified"
}) {
  const toneClass = {
    added: "text-emerald-700 bg-emerald-50",
    removed: "text-rose-700 bg-rose-50",
    modified: "text-amber-700 bg-amber-50",
  }[tone]

  return (
    <div className="flex items-center gap-3 border-b border-slate-100 px-5 py-3 last:border-b-0 sm:border-b-0 sm:border-r sm:last:border-r-0">
      <span className={`flex size-8 items-center justify-center rounded-lg ${toneClass}`}>
        <Icon className="size-4" />
      </span>
      <div>
        <p className="text-xs text-slate-500">{label}</p>
        <p className="font-bold text-slate-900">{value}</p>
      </div>
    </div>
  )
}