import { useEffect, useMemo, useState } from "react"
import { useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import {
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  GraduationCap,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import {
  courseProgramApi,
  type CourseProgramItem,
} from "@/api/courseProgramApi"
import { programApi } from "@/api/programApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

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

function courseGroupLabel(course: CourseProgramItem): string {
  return course.courseTypeName?.trim()
    || (course.required ? "Compulsory / Required" : "Elective")
}

function semesterLabel(semester: number): string {
  return `Semester ${semester}`
}

function getRoleBase(pathname: string): "/admin" | "/dean" {
  return pathname.startsWith("/dean") ? "/dean" : "/admin"
}

export default function ProgramTimelinePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()

  const programId = Number(id)
  const validProgramId = Number.isInteger(programId) && programId > 0
  const roleBase = getRoleBase(location.pathname)

  const [cohortId, setCohortId] = useState(
    searchParams.get("cohortId") ?? "",
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
    data: cohorts = [],
    isLoading: isCohortsLoading,
    isError: isCohortsError,
  } = useQuery({
    queryKey: ["cohorts", "program", programId],
    queryFn: () => cohortApi.getByProgram(programId),
    enabled: validProgramId,
  })

  const activeCohorts = useMemo(
    () =>
      cohorts
        .filter((cohort) => cohort.isActive !== false)
        .slice()
        .sort(
          (left, right) =>
            Number(right.entryYear ?? 0) - Number(left.entryYear ?? 0)
            || right.id - left.id,
        ),
    [cohorts],
  )

  useEffect(() => {
    if (activeCohorts.length === 0) return

    const currentIsValid = activeCohorts.some(
      (cohort) => String(cohort.id) === cohortId,
    )

    if (currentIsValid) return

    const latest = String(activeCohorts[0].id)
    setCohortId(latest)

    const next = new URLSearchParams(searchParams)
    next.set("cohortId", latest)
    setSearchParams(next, { replace: true })
  }, [activeCohorts, cohortId, searchParams, setSearchParams])

  const selectedCohort = activeCohorts.find(
    (cohort) => String(cohort.id) === cohortId,
  )

  const {
    data: curriculum = [],
    isLoading: isCurriculumLoading,
    isError: isCurriculumError,
  } = useQuery({
    queryKey: ["program-timeline", programId, cohortId],
    queryFn: () =>
      courseProgramApi.getCurriculum(
        programId,
        Number(cohortId),
      ),
    enabled: validProgramId && Boolean(cohortId),
  })

  const grouped = useMemo(() => {
    const groups = new Map<
      number,
      Map<number, CourseProgramItem[]>
    >()

    curriculum.forEach((course) => {
      const semester = Number(course.semesterSuggest ?? 0)
      const year = Number(
        course.yearSuggest
        ?? (semester > 0 ? Math.ceil(semester / 2) : 0),
      )

      if (year <= 0 || semester <= 0) return

      const yearMap = groups.get(year) ?? new Map<number, CourseProgramItem[]>()
      const semesterCourses = yearMap.get(semester) ?? []
      semesterCourses.push(course)
      yearMap.set(semester, semesterCourses)
      groups.set(year, yearMap)
    })

    return Array.from(groups.entries())
      .sort(([left], [right]) => left - right)
      .map(([year, semesterMap]) => ({
        year,
        semesters: Array.from(semesterMap.entries())
          .sort(([left], [right]) => left - right)
          .map(([semester, courses]) => ({
            semester,
            courses: courses
              .slice()
              .sort((left, right) =>
                left.courseCode.localeCompare(
                  right.courseCode,
                  "en",
                  { numeric: true },
                ),
              ),
          })),
      }))
  }, [curriculum])

  const unassigned = useMemo(
    () =>
      curriculum.filter(
        (course) =>
          !course.semesterSuggest
          || Number(course.semesterSuggest) <= 0,
      ),
    [curriculum],
  )

  const totalCredits = useMemo(
    () =>
      curriculum.reduce(
        (sum, course) => sum + Number(course.totalCredits ?? 0),
        0,
      ),
    [curriculum],
  )

  const requiredCount = curriculum.filter(
    (course) => course.required,
  ).length

  const electiveCount = curriculum.length - requiredCount

  const handleCohortChange = (value: string) => {
    setCohortId(value)

    const next = new URLSearchParams(searchParams)
    next.set("cohortId", value)
    setSearchParams(next, { replace: true })
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

  const loading =
    isProgramLoading
    || isCohortsLoading
    || (Boolean(cohortId) && isCurriculumLoading)

  const hasLoadError =
    isProgramError
    || isCohortsError
    || isCurriculumError

  return (
    <div className="mx-auto w-full max-w-[1450px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <CalendarDays className="size-5" />
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Curriculum Planning
              </p>
              <h1 className="mt-1 text-[26px] font-bold tracking-[-0.4px] text-[#17343d]">
                Semester Timeline
              </h1>
              <p className="mt-1 text-sm text-[#687f89]">
                {program
                  ? `${baseProgramCode(program.code)} · ${program.name}`
                  : "Review courses by recommended year and semester."}
              </p>
            </div>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="w-full min-w-[220px] sm:w-[250px]">
              <label className="mb-1.5 block text-xs font-semibold text-slate-700">
                Cohort
              </label>
              <Select
                value={cohortId}
                onValueChange={handleCohortChange}
                disabled={isCohortsLoading || activeCohorts.length === 0}
              >
                <SelectTrigger className="bg-white">
                  <SelectValue placeholder="Select cohort..." />
                </SelectTrigger>
                <SelectContent>
                  {activeCohorts.map((cohort) => (
                    <SelectItem key={cohort.id} value={String(cohort.id)}>
                      {cohort.name} · {cohort.entryYear}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
        </div>
      </section>

      {hasLoadError && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Unable to load the curriculum semester timeline.
        </div>
      )}

      {!isCohortsLoading && activeCohorts.length === 0 && (
        <Card className="border-amber-200 bg-amber-50">
          <CardContent className="p-6 text-sm text-amber-800">
            No active cohort is available for this curriculum program.
            {roleBase === "/dean"
              ? " Please contact an Administrator to configure the cohort."
              : " Create a cohort before planning the semester timeline."}
          </CardContent>
        </Card>
      )}

      {selectedCohort && !hasLoadError && (
        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryCard
            icon={BookOpen}
            label="Courses"
            value={String(curriculum.length)}
            helper={selectedCohort.name}
          />
          <SummaryCard
            icon={GraduationCap}
            label="Curriculum Credits"
            value={String(totalCredits)}
            helper="Calculated from this cohort"
          />
          <SummaryCard
            icon={CheckCircle2}
            label="Required"
            value={String(requiredCount)}
            helper="Required course assignments"
          />
          <SummaryCard
            icon={Clock3}
            label="Elective / Other"
            value={String(electiveCount)}
            helper="Non-required course assignments"
          />
        </section>
      )}

      {loading && (
        <Card>
          <CardContent className="p-8 text-center text-sm text-slate-500">
            Loading semester plan...
          </CardContent>
        </Card>
      )}

      {!loading
        && !hasLoadError
        && cohortId
        && curriculum.length === 0 && (
          <Card>
            <CardContent className="p-8 text-center">
              <BookOpen className="mx-auto size-9 text-slate-300" />
              <p className="mt-3 font-semibold text-slate-800">
                No courses are assigned to this cohort.
              </p>
              <p className="mt-1 text-sm text-slate-500">
                Select another cohort or ask an Administrator to configure the curriculum structure.
              </p>
            </CardContent>
          </Card>
        )}

      {!loading && !hasLoadError && grouped.length > 0 && (
        <div className="space-y-5">
          {grouped.map((yearGroup) => (
            <section
              key={yearGroup.year}
              className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
            >
              <div className="flex flex-col gap-2 border-b border-slate-100 bg-slate-50/70 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">
                    Year {yearGroup.year}
                  </h2>
                  <p className="mt-0.5 text-xs text-slate-500">
                    Courses are grouped by their recommended semester.
                  </p>
                </div>
                <Badge variant="outline" className="w-fit bg-white">
                  {yearGroup.semesters.reduce(
                    (sum, item) => sum + item.courses.length,
                    0,
                  )} courses
                </Badge>
              </div>

              <div className="grid gap-0 lg:grid-cols-2">
                {yearGroup.semesters.map((semesterGroup) => (
                  <div
                    key={semesterGroup.semester}
                    className="border-b border-slate-100 p-5 lg:border-r lg:[&:nth-child(even)]:border-r-0"
                  >
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <h3 className="font-semibold text-[#17343d]">
                        {semesterLabel(semesterGroup.semester)}
                      </h3>
                      <Badge variant="outline">
                        {semesterGroup.courses.length}
                      </Badge>
                    </div>

                    <div className="space-y-2.5">
                      {semesterGroup.courses.map((course) => (
                        <CourseCard key={course.id} course={course} />
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}

      {!loading && !hasLoadError && unassigned.length > 0 && (
        <section className="rounded-2xl border border-amber-200 bg-amber-50/50 p-5">
          <h2 className="font-semibold text-amber-900">
            Courses without a recommended semester
          </h2>
          <p className="mt-1 text-xs text-amber-700">
            These courses are part of the curriculum but need semester planning.
          </p>
          <div className="mt-4 grid gap-2.5 md:grid-cols-2 xl:grid-cols-3">
            {unassigned.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function SummaryCard({
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
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-[#eef8f8] text-[#007d84]">
          <Icon className="size-4" />
        </span>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
            {label}
          </p>
          <p className="mt-1 text-xl font-bold text-slate-900">
            {value}
          </p>
          <p className="mt-1 text-[11px] text-slate-500">
            {helper}
          </p>
        </div>
      </div>
    </div>
  )
}

function CourseCard({ course }: { course: CourseProgramItem }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-3.5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-mono text-sm font-bold text-[#007d84]">
            {course.courseCode}
          </p>
          <p className="mt-1 text-sm font-semibold leading-5 text-slate-900">
            {course.courseName}
          </p>
        </div>

        <Badge
          variant="outline"
          className={
            course.required
              ? "shrink-0 border-emerald-200 bg-emerald-50 text-emerald-700"
              : "shrink-0 border-amber-200 bg-amber-50 text-amber-700"
          }
        >
          {course.required ? "Required" : "Elective"}
        </Badge>
      </div>

      <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
        <span>{courseGroupLabel(course)}</span>
        <span>
          {course.totalCredits ?? 0} credits
          {` (${course.creditTheory ?? 0} theory + ${course.creditLab ?? 0} practice)`}
        </span>
      </div>
    </div>
  )
}
