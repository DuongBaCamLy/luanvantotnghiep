import { useEffect, useMemo } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate, useSearchParams } from "react-router-dom"
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  Clock3,
  FileWarning,
  PieChart as PieChartIcon,
  Users,
} from "lucide-react"
import {
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts"
import { syllabusApi } from "@/api/syllabusApi"
import { getUsers } from "@/api/userApi"

import { cohortApi } from "@/api/cohortApi"
import { dashboardApi } from "@/api/dashboardApi"
import { programApi } from "@/api/programApi"
import {
  SYLLABUS_FILTER_ALL,
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"
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

const ALL = SYLLABUS_FILTER_ALL


const SYLLABUS_STATUS_ORDER = [
  "DRAFT",
  "SUBMITTED",
  "UNDER_REVIEW",
  "REJECTED",
  "APPROVED",
] as const

const syllabusStatusColors: Record<string, string> = {
  DRAFT: "#64748b",
  SUBMITTED: "#0ea5e9",
  UNDER_REVIEW: "#6366f1",
  REJECTED: "#e11d48",
  APPROVED: "#10b981",
}
const accountStatusColors: Record<string, string> = {
  ACTIVE: "#10b981",
  DEACTIVATED: "#e11d48",
}

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


export default function AdminDashboardPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()


  const semesterFromUrl =
  searchParams.get("semester")

const parsedSemester =
  semesterFromUrl === null
    ? undefined
    : Number(semesterFromUrl)

const semesterNumber =
  typeof parsedSemester === "number"
  && Number.isInteger(parsedSemester)
  && parsedSemester >= 1
  && parsedSemester <= 8
    ? parsedSemester
    : undefined

const requestedMajorId =
  Number(searchParams.get("majorId"))

const majorIdFromUrl =
  Number.isInteger(requestedMajorId)
  && requestedMajorId > 0
    ? requestedMajorId
    : undefined

const majorCodeFromUrl =
  searchParams.get("majorCode")?.trim()
  || searchParams.get("major")?.trim()
  || undefined



const requestedCohortId =
  Number(searchParams.get("cohortId"))

const cohortIdFromUrl =
  Number.isInteger(requestedCohortId)
  && requestedCohortId > 0
    ? requestedCohortId
    : undefined

const programsQuery = useQuery({
  queryKey: ["programs"],
  queryFn: programApi.getAll,
})

const majorsQuery = useQuery({
  queryKey: ["majors"],
  queryFn: programApi.getMajors,
})

const cohortsQuery = useQuery({
  queryKey: ["cohorts"],
  queryFn: cohortApi.getAll,
})

const programs = useMemo(
  () =>
    (programsQuery.data ?? [])
      .filter(
        (program) =>
          program.isActive !== false,
      ),
  [programsQuery.data],
)

const majors = useMemo(
  () =>
    [...(majorsQuery.data ?? [])]
      .sort(
        (left, right) =>
          left.code.localeCompare(
            right.code,
            "en",
            { numeric: true },
          ),
      ),
  [majorsQuery.data],
)

const cohorts = useMemo(
  () =>
    (cohortsQuery.data ?? [])
      .filter(
        (cohort) =>
          cohort.isActive !== false,
      ),
  [cohortsQuery.data],
)

const programById = useMemo(
  () =>
    new Map(
      programs.map(
        (program) => [
          program.id,
          program,
        ],
      ),
    ),
  [programs],
)

const cohortById = useMemo(
  () =>
    new Map(
      cohorts.map(
        (cohort) => [
          cohort.id,
          cohort,
        ],
      ),
    ),
  [cohorts],
)

const selectedCohort =
  cohortIdFromUrl !== undefined
    ? cohortById.get(cohortIdFromUrl)
    : undefined

/*
 * Cohort owns the concrete curriculum Program.
 * If no Cohort is selected, Dashboard has no hidden
 * Program filter.
 */
const selectedProgram =
  selectedCohort
    ? programById.get(
        selectedCohort.programId,
      )
    : undefined

const selectedMajor =
  selectedProgram
    ? majors.find(
        (major) =>
          major.id ===
          selectedProgram.majorId,
      )
    : majors.find(
        (major) =>
          major.id === majorIdFromUrl
          || major.code.toLowerCase()
            === majorCodeFromUrl
              ?.toLowerCase(),
      )

const majorId =
  selectedMajor?.id

const programId =
  selectedProgram?.id

const cohortId =
  selectedCohort?.id

const selectedMajorCode =
  selectedMajor?.code
  ?? ALL

const cohortOptions = useMemo(
  () =>
    cohorts
      .filter((cohort) => {
        if (!selectedMajor) {
          return true
        }

        const program =
          programById.get(
            cohort.programId,
          )

        return (
          program?.majorId
          === selectedMajor.id
        )
      })
      .sort(
        (left, right) =>
          right.entryYear
          - left.entryYear,
      ),
  [
    cohorts,
    programById,
    selectedMajor,
  ],
)
const query = useQuery({
  queryKey: [
    "admin-dashboard",
    semesterNumber ?? null,
    majorId ?? null,
    programId ?? null,
    cohortId ?? null,
  ],
  queryFn: () =>
    dashboardApi.getAdminDashboard({
      semester: semesterNumber,
      majorId,
      programId,
      cohortId,
    }),
  enabled:
    !programsQuery.isLoading
    && !majorsQuery.isLoading
    && !cohortsQuery.isLoading,
})


  const dashboard = query.data
const accountsQuery = useQuery({
  queryKey: ["users"],
  queryFn: getUsers,
  staleTime: 60_000,
  refetchOnWindowFocus: false,
})
const syllabusesQuery = useQuery({
  queryKey: ["syllabuses"],
  queryFn: syllabusApi.getAll,
  staleTime: 60_000,
  refetchOnWindowFocus: false,
})


const filteredDashboardSyllabuses =
  useMemo(() => {
    return (syllabusesQuery.data ?? [])
      .filter((syllabus) => {
        if (majorId) {
          const syllabusProgram =
            syllabus.programId
              ? programById.get(
                  syllabus.programId,
                )
              : undefined

          if (
            syllabusProgram?.majorId
            !== majorId
          ) {
            return false
          }
        }

        if (
          programId
          && syllabus.programId !== programId
        ) {
          return false
        }

        if (
          cohortId
          && syllabus.cohortId !== cohortId
        ) {
          return false
        }

        if (semesterNumber) {
          const syllabusSemester =
            Number(
              String(
                syllabus.semester ?? "",
              ).replace(/\D+/g, ""),
            )

          if (
            syllabusSemester
            !== semesterNumber
          ) {
            return false
          }
        }

        return true
      })
  }, [
    syllabusesQuery.data,
    majorId,
    programId,
    cohortId,
    semesterNumber,
    programById,
  ])

const syllabusStatusData =
  useMemo(() => {
    const counts: Record<string, number> = {
      DRAFT: 0,
      SUBMITTED: 0,
      UNDER_REVIEW: 0,
      REJECTED: 0,
      APPROVED: 0,
    }

    for (
      const syllabus
      of filteredDashboardSyllabuses
    ) {
      const status =
        String(
          syllabus.status ?? "",
        )
          .trim()
          .toUpperCase()

      if (
        Object.prototype.hasOwnProperty.call(
          counts,
          status,
        )
      ) {
        counts[status] += 1
      }
    }

    const total =
      SYLLABUS_STATUS_ORDER.reduce(
        (sum, status) =>
          sum + counts[status],
        0,
      )

    return SYLLABUS_STATUS_ORDER.map(
      (status) => ({
        status,
        name: statusLabel(status),
        value: counts[status],
        percentage:
          total === 0
            ? 0
            : counts[status]
                * 100
                / total,
      }),
    )
  }, [filteredDashboardSyllabuses])

const syllabusStatusTotal =
  useMemo(
    () =>
      syllabusStatusData.reduce(
        (sum, item) =>
          sum + item.value,
        0,
      ),
    [syllabusStatusData],
  )
const accountStatusData = useMemo(() => {
  const users =
    accountsQuery.data ?? []

  const active =
    users.filter(
      (user) =>
        user.isActive === true,
    ).length

  const deactivated =
    users.filter(
      (user) =>
        user.isActive !== true,
    ).length

  const total =
    active + deactivated

  return [
    {
      status: "ACTIVE",
      name: "Active",
      value: active,
      percentage:
        total === 0
          ? 0
          : active * 100 / total,
    },
    {
      status: "DEACTIVATED",
      name: "Deactivated",
      value: deactivated,
      percentage:
        total === 0
          ? 0
          : deactivated * 100 / total,
    },
  ]
}, [accountsQuery.data])

const accountStatusTotal =
  useMemo(
    () =>
      accountStatusData.reduce(
        (sum, item) =>
          sum + item.value,
        0,
      ),
    [accountStatusData],
  )
  const accountByInstructorId =
  useMemo(
    () =>
      new Map(
        (accountsQuery.data ?? [])
          .filter(
            (account) =>
              account.instructorId != null,
          )
          .map(
            (account) => [
              account.instructorId!,
              account,
            ],
          ),
      ),
    [accountsQuery.data],
  )
  const overdueSubmissionRows =
  useMemo(() => {
    const rows =
      dashboard?.facultiesNotSubmitted
        .flatMap((faculty) =>
          faculty.missingCourses
            .filter(
              (course) =>
                course.overdue === true,
            )
            .map((course) => {
              const account =
                accountByInstructorId.get(
                  faculty.instructorId,
                )

              return {
                key:
                  `${faculty.instructorId}-${course.courseId}`,
                instructorId:
                  faculty.instructorId,
                account:
                  account?.username ?? "—",
                instructorName:
                  faculty.instructorName,
                courseId:
                  course.courseId,
                courseCode:
                  course.courseCode,
                courseName:
                  course.courseName
                  || course.courseNameVn
                  || course.courseCode,
                syllabusId:
                  course.syllabusId,
                versionLabel:
                  course.versionLabel,
                status:
                  course.latestStatus
                  || "MISSING",
                deadlineAt:
                  dashboard.deadlineAt,
              }
            }),
        ) ?? []

    return rows.slice(0, 5)
  }, [
    dashboard,
    accountByInstructorId,
  ])
  const recentLoginRows = useMemo(() => {
  return (accountsQuery.data ?? [])
    .filter((account) =>
      Boolean(account.lastLogin)
    )
    .sort((left, right) => {
      const rightTime =
        right.lastLogin
          ? new Date(
              right.lastLogin,
            ).getTime()
          : 0

      const leftTime =
        left.lastLogin
          ? new Date(
              left.lastLogin,
            ).getTime()
          : 0

      return rightTime - leftTime
    })
    .slice(0, 5)
}, [accountsQuery.data])
  const awaitingApprovalRows =
  useMemo(() => {
    const allowedStatuses =
      new Set([
        "DRAFT",
        "SUBMITTED",
        "UNDER_REVIEW",
        "REVISION_REQUESTED",
        "REJECTED",
      ])

    return filteredDashboardSyllabuses
      .filter((syllabus) => {
        const status =
          String(
            syllabus.status ?? "",
          )
            .trim()
            .toUpperCase()

        return allowedStatuses.has(
          status,
        )
      })
      .sort((left, right) => {
        const rightTime =
          right.updatedAt
            ? new Date(
                right.updatedAt,
              ).getTime()
            : 0

        const leftTime =
          left.updatedAt
            ? new Date(
                left.updatedAt,
              ).getTime()
            : 0

        return rightTime - leftTime
      })
      .slice(0, 5)
  }, [
    filteredDashboardSyllabuses,
  ])
const syllabusCatalogUrl =
  useMemo(() => {
    const params =
      new URLSearchParams()

    if (majorId) {
      params.set(
        "majorId",
        String(majorId),
      )
    }

    if (selectedMajor?.code) {
      params.set(
        "majorCode",
        selectedMajor.code,
      )
    }

    if (programId) {
      params.set(
        "programId",
        String(programId),
      )
    }

    if (selectedProgram?.code) {
      params.set(
        "programCode",
        selectedProgram.code,
      )
    }

    if (cohortId) {
      params.set(
        "cohortId",
        String(cohortId),
      )
    }

    if (semesterNumber) {
      params.set(
        "semester",
        String(semesterNumber),
      )
    }

    const queryString =
      params.toString()

    return queryString
      ? `/admin/syllabus?${queryString}`
      : "/admin/syllabus"
  }, [
    majorId,
    selectedMajor,
    programId,
    selectedProgram,
    cohortId,
    semesterNumber,
  ])


  useEffect(() => {
  if (
    programsQuery.isLoading
    || majorsQuery.isLoading
    || cohortsQuery.isLoading
  ) {
    return
  }

  const next =
    new URLSearchParams(searchParams)

  let changed = false

  if (next.has("academicYear")) {
    next.delete("academicYear")
    changed = true
  }

  if (next.has("major")) {
    next.delete("major")
    changed = true
  }

  if (selectedCohort) {
    const program =
      programById.get(
        selectedCohort.programId,
      )

    if (program) {
      if (
        next.get("programId")
        !== String(program.id)
      ) {
        next.set(
          "programId",
          String(program.id),
        )
        changed = true
      }

      if (
        next.get("programCode")
        !== program.code
      ) {
        next.set(
          "programCode",
          program.code,
        )
        changed = true
      }

      if (
        next.get("majorId")
        !== String(program.majorId)
      ) {
        next.set(
          "majorId",
          String(program.majorId),
        )
        changed = true
      }

      if (
        next.get("majorCode")
        !== program.majorCode
      ) {
        next.set(
          "majorCode",
          program.majorCode,
        )
        changed = true
      }
    }
  } else {
    if (next.has("programId")) {
      next.delete("programId")
      changed = true
    }

    if (next.has("programCode")) {
      next.delete("programCode")
      changed = true
    }

    if (selectedMajor) {
      if (
        next.get("majorId")
        !== String(selectedMajor.id)
      ) {
        next.set(
          "majorId",
          String(selectedMajor.id),
        )
        changed = true
      }

      if (
        next.get("majorCode")
        !== selectedMajor.code
      ) {
        next.set(
          "majorCode",
          selectedMajor.code,
        )
        changed = true
      }
    }
  }

  if (changed) {
    setSearchParams(
      next,
      { replace: true },
    )
  }
}, [
  cohortsQuery.isLoading,
  majorsQuery.isLoading,
  programById,
  programsQuery.isLoading,
  searchParams,
  selectedCohort,
  selectedMajor,
  setSearchParams,
])


  const setMajor = (
  value: string,
) => {
  const next =
    new URLSearchParams(
      searchParams,
    )

  if (value === ALL) {
    next.delete("majorId")
    next.delete("majorCode")
    next.delete("major")
    next.delete("programId")
    next.delete("programCode")
    next.delete("cohortId")

    setSearchParams(next)
    return
  }

  const major =
    majors.find(
      (item) =>
        item.code === value,
    )

  if (!major) {
    return
  }

  next.set(
    "majorId",
    String(major.id),
  )

  next.set(
    "majorCode",
    major.code,
  )

  next.delete("major")
  next.delete("programId")
  next.delete("programCode")
  next.delete("cohortId")

  setSearchParams(next)
}

const setCohort = (
  value: string,
) => {
  const next =
    new URLSearchParams(
      searchParams,
    )

  if (value === ALL) {
    next.delete("cohortId")
    next.delete("programId")
    next.delete("programCode")

    setSearchParams(next)
    return
  }

  const cohort =
    cohortById.get(
      Number(value),
    )

  if (!cohort) {
    return
  }

  const program =
    programById.get(
      cohort.programId,
    )

  if (!program) {
    return
  }

  next.set(
    "cohortId",
    String(cohort.id),
  )

  next.set(
    "programId",
    String(program.id),
  )

  next.set(
    "programCode",
    program.code,
  )

  next.set(
    "majorId",
    String(program.majorId),
  )

  next.set(
    "majorCode",
    program.majorCode,
  )

  next.delete("major")

  setSearchParams(next)
}

const setSemester = (
  value: string,
) => {
  const next =
    new URLSearchParams(
      searchParams,
    )

  next.delete("academicYear")

  if (value === ALL) {
    next.delete("semester")
  } else {
    next.set(
      "semester",
      value,
    )
  }

  setSearchParams(next)
}

  return (
    <div data-admin-page="AdminDashboardPage"
      className="space-y-5"
      data-testid="admin-dashboard"
      data-dashboard-loaded={query.isSuccess ? "true" : "false"}
    >
      <DashboardHero
  roleLabel="Curriculum & Syllabus Administration"
  title="Admin Dashboard"
  description="Monitor syllabus approval status, overdue submissions, account status, and recent system access."
/>

      <Card data-admin-filter className="border-[#d6e1e6] bg-white shadow-[0_8px_22px_rgba(0,74,82,0.06)]">
        <CardContent className="p-4">
  <div className="grid w-full gap-3 md:grid-cols-3">
    <div>
      <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.45px] text-[#526a75]">
        Major
      </label>

      <Select
        value={selectedMajorCode}
        onValueChange={setMajor}
        disabled={majorsQuery.isLoading}
      >
        <SelectTrigger className="h-[38px] w-full bg-white">
          <SelectValue placeholder="All Majors" />
        </SelectTrigger>

        <SelectContent>
          <SelectItem value={ALL}>
            All Majors
          </SelectItem>

          {majors.map((major) => (
            <SelectItem
              key={major.id}
              value={major.code}
            >
              {major.code}
              {major.name
                ? ` — ${major.name}`
                : major.nameVn
                  ? ` — ${major.nameVn}`
                  : ""}
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
        value={
          cohortId
            ? String(cohortId)
            : ALL
        }
        onValueChange={setCohort}
        disabled={
          cohortsQuery.isLoading
          || cohortOptions.length === 0
        }
      >
        <SelectTrigger className="h-[38px] w-full bg-white">
          <SelectValue placeholder="All Cohorts" />
        </SelectTrigger>

        <SelectContent>
          <SelectItem value={ALL}>
            All Cohorts
          </SelectItem>

          {cohortOptions.map((cohort) => (
            <SelectItem
              key={cohort.id}
              value={String(cohort.id)}
            >
              {cohort.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>

    <div>
      <label className="mb-1.5 block text-[10px] font-semibold uppercase tracking-[0.45px] text-[#526a75]">
        Semester
      </label>

      <Select
        value={
          semesterNumber
            ? String(semesterNumber)
            : ALL
        }
        onValueChange={setSemester}
      >
        <SelectTrigger className="h-[38px] w-full bg-white">
          <SelectValue placeholder="All Semesters" />
        </SelectTrigger>

        <SelectContent>
          <SelectItem value={ALL}>
            All Semesters
          </SelectItem>

          {SYLLABUS_SEMESTER_OPTIONS.map(
            (semester) => (
              <SelectItem
                key={semester}
                value={semester}
              >
                Semester {semester}
              </SelectItem>
            ),
          )}
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

      <div className="grid gap-5 xl:grid-cols-2">
        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
  <CardHeader className="border-b border-[#e1e9ec] pb-4">
    <CardTitle className="flex items-center gap-2 text-base">
      <PieChartIcon className="size-4 text-[#007d84]" />
      Syllabus Status
    </CardTitle>

    <CardDescription>
      Distribution of the latest syllabus workflow status
      in the selected Major, Cohort, and Semester.
    </CardDescription>
  </CardHeader>

  <CardContent className="p-5">
    {syllabusesQuery.isLoading ? (
  <div className="py-16 text-center text-sm text-slate-400">
    Loading syllabus status…
  </div>
) : syllabusesQuery.isError ? (
  <div className="py-16 text-center text-sm text-rose-600">
    Unable to load syllabus status.
  </div>
) : syllabusStatusTotal === 0 ? (
      <div className="py-16 text-center text-sm text-slate-400">
        No syllabus status data.
      </div>
    ) : (
      <div className="grid gap-5 md:grid-cols-[1.15fr_0.85fr]">
        <div className="relative h-[300px]">
          <ResponsiveContainer
            width="100%"
            height="100%"
          >
            <PieChart>
              <Pie
                data={syllabusStatusData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={72}
                outerRadius={108}
                paddingAngle={2}
                strokeWidth={0}
              >
                {syllabusStatusData.map(
                  (item) => (
                    <Cell
                      key={item.status}
                      fill={
                        syllabusStatusColors[
                          item.status
                        ]
                      }
                    />
                  ),
                )}
              </Pie>

              <Tooltip
  formatter={(value) => [
    Number(value),
    "Syllabuses",
  ]}
/>
            </PieChart>
          </ResponsiveContainer>

          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-[30px] font-bold leading-none text-[#17323d]">
              {syllabusStatusTotal}
            </span>

            <span className="mt-2 text-[11px] font-medium text-slate-500">
              Total
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-center gap-2">
          {syllabusStatusData.map(
            (item) => (
              <div
                key={item.status}
                className="flex items-center justify-between gap-3 rounded-lg border border-[#e1e9ec] bg-white px-3 py-2.5"
              >
                <div className="flex min-w-0 items-center gap-2.5">
                  <span
                    className="h-2.5 w-2.5 shrink-0 rounded-full"
                    style={{
                      backgroundColor:
                        syllabusStatusColors[
                          item.status
                        ],
                    }}
                  />

                  <span className="truncate text-[12px] font-medium text-slate-700">
                    {item.name}
                  </span>
                </div>

                <div className="flex shrink-0 items-baseline gap-2">
                  <span className="text-[12px] font-bold text-[#17323d]">
                    {item.value}
                  </span>

                  <span className="min-w-[48px] text-right text-[11px] font-semibold text-slate-500">
                    {item.percentage.toFixed(1)}%
                  </span>
                </div>
              </div>
            ),
          )}
        </div>
      </div>
    )}
  </CardContent>
</Card>

        <Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
  <CardHeader className="border-b border-[#e1e9ec] pb-4">
    <CardTitle className="flex items-center gap-2 text-base">
      <Users className="size-4 text-[#007d84]" />
      Account Status
    </CardTitle>

    <CardDescription>
      Current sign-in account status across the system.
    </CardDescription>
  </CardHeader>

  <CardContent className="p-5">
    {accountsQuery.isLoading ? (
      <div className="py-16 text-center text-sm text-slate-400">
        Loading account status…
      </div>
    ) : accountsQuery.isError ? (
      <div className="py-16 text-center text-sm text-rose-600">
        Unable to load account status.
      </div>
    ) : accountStatusTotal === 0 ? (
      <div className="py-16 text-center text-sm text-slate-400">
        No account data.
      </div>
    ) : (
      <div className="grid gap-5 md:grid-cols-[1.15fr_0.85fr]">
        <div className="relative h-[300px]">
          <ResponsiveContainer
            width="100%"
            height="100%"
          >
            <PieChart>
              <Pie
                data={accountStatusData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={72}
                outerRadius={108}
                paddingAngle={2}
                strokeWidth={0}
              >
                {accountStatusData.map(
                  (item) => (
                    <Cell
                      key={item.status}
                      fill={
                        accountStatusColors[
                          item.status
                        ]
                      }
                    />
                  ),
                )}
              </Pie>

              <Tooltip
                formatter={(value) => [
                  Number(value),
                  "Accounts",
                ]}
              />
            </PieChart>
          </ResponsiveContainer>

          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-[30px] font-bold leading-none text-[#17323d]">
              {accountStatusTotal}
            </span>

            <span className="mt-2 text-[11px] font-medium text-slate-500">
              Accounts
            </span>
          </div>
        </div>

        <div className="flex flex-col justify-center gap-2">
          {accountStatusData.map(
            (item) => (
              <div
                key={item.status}
                className="flex items-center justify-between gap-3 rounded-lg border border-[#e1e9ec] bg-white px-3 py-2.5"
              >
                <div className="flex items-center gap-2.5">
                  <span
                    className="h-2.5 w-2.5 shrink-0 rounded-full"
                    style={{
                      backgroundColor:
                        accountStatusColors[
                          item.status
                        ],
                    }}
                  />

                  <span className="text-[12px] font-medium text-slate-700">
                    {item.name}
                  </span>
                </div>

                <div className="flex shrink-0 items-baseline gap-2">
                  <span className="text-[12px] font-bold text-[#17323d]">
                    {item.value}
                  </span>

                  <span className="min-w-[48px] text-right text-[11px] font-semibold text-slate-500">
                    {item.percentage.toFixed(1)}%
                  </span>
                </div>
              </div>
            ),
          )}
        </div>
      </div>
    )}
  </CardContent>
</Card>
      </div>
<Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
  <CardHeader className="border-b border-[#e1e9ec] pb-4">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <CardTitle className="flex items-center gap-2 text-base">
          <FileWarning className="size-4 text-amber-600" />
          Syllabuses Awaiting Approval
        </CardTitle>

        <CardDescription className="mt-1">
          Syllabuses that have not reached final approval
          in the selected Major, Cohort, and Semester.
        </CardDescription>
      </div>

      <Button
        variant="outline"
        size="sm"
        onClick={() =>
          navigate(
            syllabusCatalogUrl,
          )
        }
        className="gap-2"
      >
        View All
        <ArrowRight className="size-4" />
      </Button>
    </div>
  </CardHeader>

  <CardContent className="p-0">
    {syllabusesQuery.isLoading ? (
      <div className="px-6 py-12 text-center text-sm text-slate-400">
        Loading syllabuses…
      </div>
    ) : syllabusesQuery.isError ? (
      <div className="px-6 py-12 text-center text-sm text-rose-600">
        Unable to load syllabus data.
      </div>
    ) : awaitingApprovalRows.length === 0 ? (
      <div className="px-6 py-12 text-center">
        <CheckCircle2 className="mx-auto size-8 text-emerald-500" />

        <p className="mt-3 text-sm font-semibold text-slate-700">
          No syllabus is awaiting approval.
        </p>

        <p className="mt-1 text-xs text-slate-500">
          All syllabuses in the selected scope have reached
          final approval.
        </p>
      </div>
    ) : (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
              <TableHead>
                Course
              </TableHead>

              <TableHead>
                Version
              </TableHead>

              <TableHead>
                Instructor
              </TableHead>

              <TableHead>
                Status
              </TableHead>

              <TableHead>
                Last Updated
              </TableHead>

              <TableHead className="text-right">
                Action
              </TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {awaitingApprovalRows.map(
              (syllabus) => {
                const status =
                  String(
                    syllabus.status,
                  ).toUpperCase()

                const statusClass =
                  status === "REJECTED"
                    ? "border-rose-200 bg-rose-50 text-rose-700"
                    : status === "UNDER_REVIEW"
                      ? "border-indigo-200 bg-indigo-50 text-indigo-700"
                      : status === "SUBMITTED"
                        ? "border-sky-200 bg-sky-50 text-sky-700"
                        : status === "REVISION_REQUESTED"
                          ? "border-amber-200 bg-amber-50 text-amber-700"
                          : "border-slate-200 bg-slate-50 text-slate-700"

                return (
                  <TableRow
                    key={syllabus.id}
                  >
                    <TableCell>
                      <div>
                        <p className="font-semibold text-[#17323d]">
                          {syllabus.courseCode}
                        </p>

                        <p className="mt-0.5 max-w-[260px] truncate text-[11px] text-slate-500">
                          {syllabus.courseName}
                        </p>
                      </div>
                    </TableCell>

                    <TableCell className="text-sm">
                      {syllabus.versionLabel
                        || `v${syllabus.versionNumber}`}
                    </TableCell>

                    <TableCell>
                      <span className="text-sm text-slate-700">
                        {syllabus.responsibleInstructors
                          || "Not assigned"}
                      </span>
                    </TableCell>

                    <TableCell>
                      <Badge
                        variant="outline"
                        className={statusClass}
                      >
                        {statusLabel(status)}
                      </Badge>
                    </TableCell>

                    <TableCell className="text-sm text-slate-600">
                      {syllabus.updatedAt
                        ? new Date(
                            syllabus.updatedAt,
                          ).toLocaleString(
                            "en-GB",
                          )
                        : "—"}
                    </TableCell>

                    <TableCell className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          navigate(
                            `/admin/syllabus/${syllabus.id}`,
                          )
                        }
                      >
                        View
                      </Button>
                    </TableCell>
                  </TableRow>
                )
              },
            )}
          </TableBody>
        </Table>
      </div>
    )}
  </CardContent>
</Card>
<Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
  <CardHeader className="border-b border-[#e1e9ec] pb-4">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <CardTitle className="flex items-center gap-2 text-base">
          <Clock3 className="size-4 text-rose-600" />
          Overdue Syllabus Submissions
        </CardTitle>

        <CardDescription className="mt-1">
          Instructors whose syllabus submission is still
          incomplete after the official deadline.
        </CardDescription>
      </div>

      <Button
        variant="outline"
        size="sm"
        onClick={() =>
          navigate("/admin/escalations")
        }
        className="gap-2"
      >
        View All
        <ArrowRight className="size-4" />
      </Button>
    </div>
  </CardHeader>

  <CardContent className="p-0">
    {query.isLoading
    || accountsQuery.isLoading ? (
      <div className="px-6 py-12 text-center text-sm text-slate-400">
        Loading overdue submissions…
      </div>
    ) : overdueSubmissionRows.length === 0 ? (
      <div className="px-6 py-12 text-center">
        <CheckCircle2 className="mx-auto size-8 text-emerald-500" />

        <p className="mt-3 text-sm font-semibold text-slate-700">
          No overdue syllabus submissions.
        </p>

        <p className="mt-1 text-xs text-slate-500">
          No instructor in the selected scope is currently
          overdue.
        </p>
      </div>
    ) : (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
              <TableHead>
                Account
              </TableHead>

              <TableHead>
                Instructor Name
              </TableHead>

              <TableHead>
                Course
              </TableHead>

              <TableHead>
                Status
              </TableHead>

              <TableHead>
                Deadline
              </TableHead>

              <TableHead className="text-right">
                Action
              </TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {overdueSubmissionRows.map(
              (row) => (
                <TableRow key={row.key}>
                  <TableCell>
                    <span className="font-mono text-[12px] font-semibold text-[#17323d]">
                      {row.account}
                    </span>
                  </TableCell>

                  <TableCell>
                    <div>
                      <p className="font-medium text-slate-800">
                        {row.instructorName}
                      </p>

                      <p className="mt-0.5 text-[10px] text-slate-400">
                        Instructor #{row.instructorId}
                      </p>
                    </div>
                  </TableCell>

                  <TableCell>
                    <div>
                      <p className="font-semibold text-[#17323d]">
                        {row.courseCode}
                      </p>

                      <p className="mt-0.5 max-w-[260px] truncate text-[11px] text-slate-500">
                        {row.courseName}
                      </p>
                    </div>
                  </TableCell>

                  <TableCell>
                    <Badge
                      variant="outline"
                      className="border-rose-200 bg-rose-50 text-rose-700"
                    >
                      {statusLabel(
                        row.status,
                      )}
                    </Badge>
                  </TableCell>

                  <TableCell>
                    <div>
                      <p className="text-sm font-medium text-slate-700">
                        {row.deadlineAt
                          ? new Date(
                              row.deadlineAt,
                            ).toLocaleString(
                              "en-GB",
                            )
                          : "Not configured"}
                      </p>

                      <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-rose-600">
                        Overdue
                      </p>
                    </div>
                  </TableCell>

                  <TableCell className="text-right">
                    {row.syllabusId ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          navigate(
                            `/admin/syllabus/${row.syllabusId}`,
                          )
                        }
                      >
                        View
                      </Button>
                    ) : (
                      <span className="text-xs text-slate-400">
                        Not created
                      </span>
                    )}
                  </TableCell>
                </TableRow>
              ),
            )}
          </TableBody>
        </Table>
      </div>
    )}
  </CardContent>
</Card>
<Card className="border-[#d6e1e6] shadow-[0_8px_22px_rgba(0,74,82,0.07)]">
  <CardHeader className="border-b border-[#e1e9ec] pb-4">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <CardTitle className="flex items-center gap-2 text-base">
          <Activity className="size-4 text-[#007d84]" />
          Recent Account Logins
        </CardTitle>

        <CardDescription className="mt-1">
          Five accounts that most recently signed in to the system.
        </CardDescription>
      </div>

      <Button
        variant="outline"
        size="sm"
        onClick={() =>
          navigate("/admin/users")
        }
        className="gap-2"
      >
        View All Users
        <ArrowRight className="size-4" />
      </Button>
    </div>
  </CardHeader>

  <CardContent className="p-0">
    {accountsQuery.isLoading ? (
      <div className="px-6 py-12 text-center text-sm text-slate-400">
        Loading recent logins…
      </div>
    ) : accountsQuery.isError ? (
      <div className="px-6 py-12 text-center text-sm text-rose-600">
        Unable to load account activity.
      </div>
    ) : recentLoginRows.length === 0 ? (
      <div className="px-6 py-12 text-center text-sm text-slate-400">
        No login activity recorded.
      </div>
    ) : (
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50/80 hover:bg-slate-50/80">
              <TableHead>
                Account
              </TableHead>

              <TableHead>
                Role
              </TableHead>

              <TableHead>
                Status
              </TableHead>

              <TableHead>
                Last Login
              </TableHead>

              <TableHead className="text-right">
                Action
              </TableHead>
            </TableRow>
          </TableHeader>

          <TableBody>
            {recentLoginRows.map(
              (account) => (
                <TableRow key={account.id}>
                  <TableCell>
                    <div>
                      <p className="font-mono text-[12px] font-semibold text-[#17323d]">
                        {account.username}
                      </p>

                      <p className="mt-0.5 text-[11px] text-slate-500">
                        {account.email}
                      </p>
                    </div>
                  </TableCell>

                  <TableCell>
                    <Badge
                      variant="outline"
                      className="border-slate-200 bg-slate-50 text-slate-700"
                    >
                      {account.role
                        .replaceAll(
                          "_",
                          " ",
                        )
                        .toLowerCase()
                        .replace(
                          /(^|\s)\S/g,
                          (value) =>
                            value.toUpperCase(),
                        )}
                    </Badge>
                  </TableCell>

                  <TableCell>
                    {account.isActive ? (
                      <Badge
                        variant="outline"
                        className="border-emerald-200 bg-emerald-50 text-emerald-700"
                      >
                        Active
                      </Badge>
                    ) : (
                      <Badge
                        variant="outline"
                        className="border-rose-200 bg-rose-50 text-rose-700"
                      >
                        Deactivated
                      </Badge>
                    )}
                  </TableCell>

                  <TableCell>
                    <div>
                      <p className="text-sm font-medium text-slate-700">
                        {account.lastLogin
                          ? new Date(
                              account.lastLogin,
                            ).toLocaleString(
                              "en-GB",
                            )
                          : "Never signed in"}
                      </p>
                    </div>
                  </TableCell>

                  <TableCell className="text-right">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        navigate(
                          "/admin/users",
                        )
                      }
                    >
                      View Account
                    </Button>
                  </TableCell>
                </TableRow>
              ),
            )}
          </TableBody>
        </Table>
      </div>
    )}
  </CardContent>
</Card>

    </div>
  )
}
