import { useEffect, useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import {
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom"
import {
  AlertTriangle,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  Clock3,
  Copy,
  Download,
  Edit2,
  Eye,
  Filter,
  History,
  LoaderCircle,
  Map as MapIcon,
  Plus,
  RotateCcw,
  Search,
  Send,
  ShieldCheck,
  Trash2,
  Upload,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import {
  dashboardApi,
  type DashboardFacultyResponse,
  type FacultyCourseAssignment,
  type FacultyDeadlineState,
  type FacultyRecommendedAction,
} from "@/api/dashboardApi"
import { courseProgramApi } from "@/api/courseProgramApi"
import { programApi } from "@/api/programApi"
import { syllabusPdfApi } from "@/api/syllabusPdfApi"
import CloneSyllabusDialog from "@/components/syllabus/CloneSyllabusDialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useDeleteSyllabus } from "@/hooks/useDeleteSyllabus"
import { useSubmitSyllabus } from "@/hooks/useSubmitSyllabus"
import { useSyllabuses } from "@/hooks/useSyllabuses"
import { getSyllabusBasePath } from "@/lib/programContext"
import { useAuthStore } from "@/store/authStore"
import type {
  Cohort,
  Major,
  Program,
} from "@/types/admin"
import type { Syllabus } from "@/types/syllabus"

type CourseProgram = Record<string, any>

type CourseProgramIndex = {
  byCourseId: Map<number, CourseProgram[]>
  byCourseCode: Map<string, CourseProgram[]>
}

type CatalogRow = {
  item: Syllabus
  programCodes: string[]
  cohortNames: string[]
  majorCodes: string[]
  semesterValues: string[]
  matchesCurriculumContext: boolean
  displayProgram: string
  displayCohort: string
  displayMajor: string
  displaySemester: string
}

const ALL = "all"

const CANONICAL_STATUSES = [
  "DRAFT",
  "SUBMITTED",
  "UNDER_REVIEW",
  "REVISION_REQUESTED",
  "REJECTED",
  "APPROVED",
  "ARCHIVED",
]

const BASE_SEMESTER_OPTIONS = Array.from(
  { length: 8 },
  (_, index) => String(index + 1),
)

const normalize = (value: unknown) =>
  String(value ?? "").trim()

const normalizeKey = (value: unknown) =>
  normalize(value).toLowerCase()

const normalizeRole = (value: unknown) =>
  normalize(value)
    .replace(/^ROLE_/i, "")
    .toUpperCase()

const toNumber = (
  value: unknown,
): number | undefined => {
  if (
    value === null
    || value === undefined
    || normalize(value) === ""
  ) {
    return undefined
  }

  const numberValue = Number(value)

  return Number.isFinite(numberValue)
    ? numberValue
    : undefined
}

const toArray = <T,>(
  payload: any,
): T[] => {
  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload?.data)) {
    return payload.data
  }

  if (Array.isArray(payload?.content)) {
    return payload.content
  }

  if (Array.isArray(payload?.items)) {
    return payload.items
  }

  return []
}

const uniqueSort = (
  values: string[],
) =>
  Array.from(
    new Set(
      values
        .map(normalize)
        .filter(Boolean),
    ),
  ).sort((a, b) =>
    a.localeCompare(
      b,
      "en",
      { numeric: true },
    ),
  )

const getCourseProgramCourseId = (
  courseProgram: CourseProgram,
) =>
  toNumber(
    courseProgram.courseId
    ?? courseProgram.course?.id
    ?? courseProgram.course?.courseId,
  )

const getCourseProgramCourseCode = (
  courseProgram: CourseProgram,
) =>
  normalize(
    courseProgram.courseCode
    ?? courseProgram.course?.code
    ?? courseProgram.course?.courseCode,
  )

const getCourseProgramProgramId = (
  courseProgram: CourseProgram,
) =>
  toNumber(
    courseProgram.programId
    ?? courseProgram.program?.id
    ?? courseProgram.program?.programId,
  )

const getCourseProgramProgramCode = (
  courseProgram: CourseProgram,
) =>
  normalize(
    courseProgram.programCode
    ?? courseProgram.program?.code,
  )

const getCourseProgramCohortId = (
  courseProgram: CourseProgram,
) =>
  toNumber(
    courseProgram.cohortId
    ?? courseProgram.cohort?.id
    ?? courseProgram.cohort?.cohortId,
  )

const getCourseProgramCohortName = (
  courseProgram: CourseProgram,
) =>
  normalize(
    courseProgram.cohortName
    ?? courseProgram.cohort?.name
    ?? courseProgram.program?.cohort?.name,
  )

const getCourseProgramMajorCode = (
  courseProgram: CourseProgram,
) =>
  normalize(
    courseProgram.majorCode
    ?? courseProgram.program?.majorCode
    ?? courseProgram.program?.major?.code
    ?? courseProgram.major?.code,
  )

const getSemesterSuggest = (
  courseProgram: CourseProgram,
) =>
  normalize(
    courseProgram.semesterSuggest
    ?? courseProgram.semester_suggest
    ?? courseProgram.semester,
  )

const getSyllabusCourseId = (
  item: Syllabus,
) =>
  toNumber(
    item.courseId
    ?? (item as any).course?.id,
  )

const normalizeSemesterValue = (
  value: unknown,
) => {
  const text = normalize(value)

  const matched =
    text.match(/semester\s*(\d+)/i)

  if (matched) {
    return matched[1]
  }

  return text
}

const formatSemesterLabel = (
  value: unknown,
) => {
  const semester =
    normalizeSemesterValue(value)

  if (!semester) {
    return "N/A"
  }

  return /^\d+$/.test(semester)
    ? `Semester ${semester}`
    : semester
}

const baseProgramCode = (
  value: unknown,
) => {
  const text =
    normalize(value)

  return (
    text
      .replace(
        /[-_\s]?20\d{2}$/i,
        "",
      )
      .replace(
        /[-_\s]+$/g,
        "",
      )
      .trim()
    || text
  )
}

const getProgramOptionLabel = (
  program: Program,
) => {
  const name =
    normalize(
      (program as any).nameVn,
    )
    || normalize(
      (program as any).name,
    )

  const code =
    baseProgramCode(
      program.code,
    )

  return name
    ? `${code} — ${name}`
    : code
}

const getCohortOptionLabel = (
  cohort: Cohort,
) => {
  const name =
    normalize(cohort.name)

  const entryYear =
    normalize(
      cohort.entryYear,
    )

  if (name && entryYear) {
    return `${name} — Entry ${entryYear}`
  }

  return name
    || (
      entryYear
        ? `Entry ${entryYear}`
        : `Cohort ${cohort.id}`
    )
}

const formatStatusLabel = (
  value: unknown,
) => {
  const status =
    normalize(value).toUpperCase()

  if (!status) {
    return "Unknown"
  }

  return status
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase()
        + part.slice(1),
    )
    .join(" ")
}

const formatStatusLabelForRole = (
  value: unknown,
  role: string,
) => {
  const status =
    normalize(value)
      .toUpperCase()

  if (
    role === "DEPT_HEAD"
  ) {
    if (status === "SUBMITTED") {
      return "Pending Department Review"
    }

    if (status === "UNDER_REVIEW") {
      return "Forwarded to Dean"
    }
  }

  if (
    role === "DEAN"
    && status === "UNDER_REVIEW"
  ) {
    return "Pending Final Review"
  }

  if (
    role === "INSTRUCTOR"
  ) {
    if (status === "SUBMITTED") {
      return "Pending Department Review"
    }

    if (status === "UNDER_REVIEW") {
      return "Pending Dean Review"
    }

    if (status === "REVISION_REQUESTED") {
      return "Revision Required"
    }

    if (status === "REJECTED") {
      return "Returned for Revision"
    }
  }

  return formatStatusLabel(
    value,
  )
}

const getStatusClass = (
  value: unknown,
  role = "",
) => {
  const status =
    normalize(value).toUpperCase()

  if (status === "APPROVED") {
    return "border-emerald-200 bg-emerald-50 text-emerald-700"
  }

  if (
    role === "DEPT_HEAD"
    && status === "SUBMITTED"
  ) {
    return "border-amber-200 bg-amber-50 text-amber-700"
  }

  if (
    status === "SUBMITTED"
    || status === "UNDER_REVIEW"
  ) {
    return "border-blue-200 bg-blue-50 text-blue-700"
  }

  if (
    status === "REJECTED"
    || status === "REVISION_REQUESTED"
  ) {
    return "border-rose-200 bg-rose-50 text-rose-700"
  }

  if (status === "ARCHIVED") {
    return "border-slate-300 bg-slate-100 text-slate-600"
  }

  return "border-amber-200 bg-amber-50 text-amber-700"
}

const formatDateTime = (
  value: string | null | undefined,
) => {
  if (!value) {
    return "—"
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    },
  ).format(date)
}

const safeFilePart = (
  value: unknown,
  fallback = "NA",
) => {
  const text = normalize(value)

  if (!text) {
    return fallback
  }

  return (
    text
      .normalize("NFD")
      .replace(
        /[\u0300-\u036f]/g,
        "",
      )
      .replace(
        /[^a-zA-Z0-9._-]+/g,
        "-",
      )
      .replace(
        /^-+|-+$/g,
        "",
      )
    || fallback
  )
}

const getPdfDownloadErrorMessage =
  async (
    error: any,
  ) => {
    const responseData =
      error?.response?.data

    if (
      responseData
      instanceof Blob
    ) {
      try {
        const text =
          await responseData.text()

        if (!text) {
          return undefined
        }

        try {
          const parsed =
            JSON.parse(text) as {
              message?: string
              error?: string
            }

          return (
            parsed.message
            || parsed.error
            || text
          )
        } catch {
          return text
        }
      } catch {
        return undefined
      }
    }

    return (
      responseData?.message
      || responseData?.error
      || error?.message
    )
  }

const getMatchedCoursePrograms = (
  item: Syllabus,
  index: CourseProgramIndex,
) => {
  const courseId =
    getSyllabusCourseId(item)

  if (courseId !== undefined) {
    const matchedById =
      index.byCourseId.get(
        courseId,
      )

    if (matchedById?.length) {
      return matchedById
    }
  }

  const courseCode =
    normalizeKey(item.courseCode)

  if (!courseCode) {
    return []
  }

  return (
    index.byCourseCode.get(
      courseCode,
    )
    ?? []
  )
}

export default function SyllabusListPage() {
  const {
    data,
    isLoading,
    isError,
  } = useSyllabuses()

  const navigate = useNavigate()
  const location = useLocation()
  const [
    searchParams,
    setSearchParams,
  ] = useSearchParams()

  const user =
    useAuthStore(
      (state) => state.user,
    )

  const role =
    normalizeRole(user?.role)

  const isInstructor =
    role === "INSTRUCTOR"

  const isAdmin =
    role === "ADMIN"

  const isDean =
    role === "DEAN"

  const isDeptHead =
    role === "DEPT_HEAD"

  const {
    data: facultyDashboard,
    isLoading: facultyDashboardLoading,
    isError: facultyDashboardError,
    isFetching: facultyDashboardFetching,
    refetch: refetchFacultyDashboard,
  } = useQuery({
    queryKey: [
      "faculty-dashboard",
      "me",
      "my-syllabi",
    ],
    queryFn:
      dashboardApi.getMyFacultyDashboard,
    enabled: isInstructor,
    staleTime: 30_000,
    refetchInterval: 60_000,
  })

  const basePath =
    getSyllabusBasePath(
      location.pathname,
    )

  const selectedProgramId =
    toNumber(
      searchParams.get(
        "programId",
      ),
    )

  const selectedCohortId =
    toNumber(
      searchParams.get(
        "cohortId",
      ),
    )

  const selectedMajorCode =
    searchParams.get(
      "majorCode",
    )
    || searchParams.get("major")
    || ALL

  const searchValue =
    searchParams.get("q")
    || ""

  const semesterValue =
    searchParams.get(
      "semester",
    )
    || ALL

  const statusValue =
    searchParams.get(
      "status",
    )
    || ALL

  const historyMode =
    searchParams.get(
      "history",
    ) === "1"

  const [
    coursePrograms,
    setCoursePrograms,
  ] = useState<
    CourseProgram[]
  >([])

  const [
    cohorts,
    setCohorts,
  ] = useState<
    Cohort[]
  >([])

  const [
    programs,
    setPrograms,
  ] = useState<
    Program[]
  >([])

  const [
    majors,
    setMajors,
  ] = useState<
    Major[]
  >([])

  const [
    referenceLoading,
    setReferenceLoading,
  ] = useState(true)

  const [
    cloneSource,
    setCloneSource,
  ] = useState<
    Syllabus | null
  >(null)

  const [
    downloadingPdfId,
    setDownloadingPdfId,
  ] = useState<
    number | null
  >(null)

  const deleteMutation =
    useDeleteSyllabus()

  const submitMutation =
    useSubmitSyllabus()

  useEffect(() => {
    let cancelled = false

    const loadReferenceData =
      async () => {
        setReferenceLoading(true)

        try {
          const [
            courseProgramPayload,
            cohortPayload,
            programPayload,
            majorPayload,
          ] =
            await Promise.all([
              courseProgramApi.getAll(),
              cohortApi.getAll(),
              programApi.getAll(),
              programApi.getMajors(),
            ])

          if (cancelled) {
            return
          }

          setCoursePrograms(
            toArray<CourseProgram>(
              courseProgramPayload,
            ),
          )

          setCohorts(
            toArray<Cohort>(
              cohortPayload,
            ),
          )

          setPrograms(
            toArray<Program>(
              programPayload,
            ),
          )

          setMajors(
            toArray<Major>(
              majorPayload,
            ),
          )
        } catch (error) {
          console.error(
            "Cannot load syllabus reference data:",
            error,
          )

          if (!cancelled) {
            setCoursePrograms([])
            setCohorts([])
            setPrograms([])
            setMajors([])
          }
        } finally {
          if (!cancelled) {
            setReferenceLoading(false)
          }
        }
      }

    void loadReferenceData()

    return () => {
      cancelled = true
    }
  }, [])

  const programById =
    useMemo(
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

  const cohortById =
    useMemo(
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

  const majorByCode =
    useMemo(
      () =>
        new Map(
          majors.map(
            (major) => [
              normalizeKey(
                major.code,
              ),
              major,
            ],
          ),
        ),
      [majors],
    )

  useEffect(() => {
    if (
      programs.length === 0
    ) {
      return
    }

    const params =
      new URLSearchParams(
        searchParams,
      )

    let changed = false

    if (
      params.has(
        "academicYear",
      )
    ) {
      params.delete(
        "academicYear",
      )
      changed = true
    }

    if (
      selectedProgramId
      === undefined
    ) {
      const programCode =
        searchParams.get(
          "programCode",
        )
        || searchParams.get(
          "cohort",
        )

      if (programCode) {
        const selectedProgram =
          programs.find(
            (program) =>
              normalizeKey(
                program.code,
              )
              === normalizeKey(
                programCode,
              ),
          )

        if (selectedProgram) {
          params.set(
            "programId",
            String(
              selectedProgram.id,
            ),
          )

          params.set(
            "programCode",
            selectedProgram.code,
          )

          params.set(
            "majorId",
            String(
              selectedProgram.majorId,
            ),
          )

          params.set(
            "majorCode",
            selectedProgram.majorCode,
          )

          params.delete("cohort")
          params.delete("major")
          changed = true
        }
      }
    }

    if (
      selectedProgramId
      === undefined
      && selectedCohortId
      !== undefined
    ) {
      const selectedCohort =
        cohortById.get(
          selectedCohortId,
        )

      const selectedProgram =
        selectedCohort
          ? programById.get(
              selectedCohort.programId,
            )
          : undefined

      if (
        selectedCohort
        && selectedProgram
      ) {
        params.set(
          "programId",
          String(
            selectedProgram.id,
          ),
        )

        params.set(
          "programCode",
          selectedProgram.code,
        )

        params.set(
          "majorId",
          String(
            selectedProgram.majorId,
          ),
        )

        params.set(
          "majorCode",
          selectedProgram.majorCode,
        )

        changed = true
      }
    }

    if (changed) {
      setSearchParams(
        params,
        { replace: true },
      )
    }
  }, [
    cohortById,
    programById,
    programs,
    searchParams,
    selectedCohortId,
    selectedProgramId,
    setSearchParams,
  ])

  const courseProgramIndex =
    useMemo<CourseProgramIndex>(
      () => {
        const byCourseId =
          new Map<
            number,
            CourseProgram[]
          >()

        const byCourseCode =
          new Map<
            string,
            CourseProgram[]
          >()

        coursePrograms.forEach(
          (courseProgram) => {
            const courseId =
              getCourseProgramCourseId(
                courseProgram,
              )

            const courseCode =
              normalizeKey(
                getCourseProgramCourseCode(
                  courseProgram,
                ),
              )

            if (
              courseId !== undefined
            ) {
              const current =
                byCourseId.get(
                  courseId,
                )
                ?? []

              current.push(
                courseProgram,
              )

              byCourseId.set(
                courseId,
                current,
              )
            }

            if (courseCode) {
              const current =
                byCourseCode.get(
                  courseCode,
                )
                ?? []

              current.push(
                courseProgram,
              )

              byCourseCode.set(
                courseCode,
                current,
              )
            }
          },
        )

        return {
          byCourseId,
          byCourseCode,
        }
      },
      [coursePrograms],
    )

  const selectedProgram =
    selectedProgramId !== undefined
      ? programById.get(
          selectedProgramId,
        )
      : undefined

  const selectedCohort =
    selectedCohortId !== undefined
      ? cohortById.get(
          selectedCohortId,
        )
      : undefined

  const programOptions =
    useMemo(() => {
      return programs
        .filter(
          (program) =>
            selectedMajorCode === ALL
            || normalizeKey(
              program.majorCode,
            )
            === normalizeKey(
              selectedMajorCode,
            ),
        )
        .slice()
        .sort(
          (a, b) =>
            a.code.localeCompare(
              b.code,
              "en",
              { numeric: true },
            ),
        )
    }, [
      programs,
      selectedMajorCode,
    ])

  const cohortOptions =
    useMemo(() => {
      if (
        selectedProgramId
        === undefined
      ) {
        return []
      }

      return cohorts
        .filter(
          (cohort) =>
            cohort.isActive !== false
            && cohort.programId
              === selectedProgramId,
        )
        .slice()
        .sort(
          (a, b) =>
            b.entryYear
            - a.entryYear,
        )
    }, [
      cohorts,
      selectedProgramId,
    ])

  const majorOptions =
    useMemo(
      () =>
        majors
          .slice()
          .sort(
            (a, b) =>
              a.code.localeCompare(
                b.code,
                "en",
                { numeric: true },
              ),
          ),
      [majors],
    )

  const semesterOptions =
    useMemo(() => {
      const values = [
        ...BASE_SEMESTER_OPTIONS,

        ...(data ?? []).map(
          (item) =>
            normalizeSemesterValue(
              item.semester,
            ),
        ),

        ...coursePrograms.map(
          (courseProgram) =>
            normalizeSemesterValue(
              getSemesterSuggest(
                courseProgram,
              ),
            ),
        ),
      ]

      return uniqueSort(values)
    }, [
      coursePrograms,
      data,
    ])

  const statusOptions =
    useMemo(() => {
      return uniqueSort([
        ...CANONICAL_STATUSES,
        ...(data ?? []).map(
          (item) =>
            normalize(
              item.status,
            ).toUpperCase(),
        ),
      ])
    }, [data])

  const enrichedData =
    useMemo<CatalogRow[]>(
      () => {
        if (!data) {
          return []
        }

        const hasCurriculumFilter =
          selectedProgramId !== undefined
          || selectedCohortId !== undefined
          || selectedMajorCode !== ALL

        return data.map(
          (item) => {
            const matched =
              getMatchedCoursePrograms(
                item,
                courseProgramIndex,
              )

            const contextMatched =
              matched.filter(
                (courseProgram) => {
                  const programId =
                    getCourseProgramProgramId(
                      courseProgram,
                    )

                  const cohortId =
                    getCourseProgramCohortId(
                      courseProgram,
                    )

                  const directMajorCode =
                    getCourseProgramMajorCode(
                      courseProgram,
                    )

                  const programMajorCode =
                    programId !== undefined
                      ? normalize(
                          programById.get(
                            programId,
                          )?.majorCode,
                        )
                      : ""

                  const programMatches =
                    selectedProgramId
                    === undefined
                    || programId
                      === selectedProgramId

                  const cohortMatches =
                    selectedCohortId
                    === undefined
                    || cohortId
                      === undefined
                    || cohortId
                      === selectedCohortId

                  const majorMatches =
                    selectedMajorCode === ALL
                    || normalizeKey(
                      directMajorCode
                      || programMajorCode,
                    )
                    === normalizeKey(
                      selectedMajorCode,
                    )

                  return (
                    programMatches
                    && cohortMatches
                    && majorMatches
                  )
                },
              )

            const displaySource =
              contextMatched.length > 0
                ? contextMatched
                : matched

            const programCodes =
              uniqueSort(
                displaySource.map(
                  (courseProgram) => {
                    const direct =
                      getCourseProgramProgramCode(
                        courseProgram,
                      )

                    if (direct) {
                      return baseProgramCode(
                        direct,
                      )
                    }

                    const programId =
                      getCourseProgramProgramId(
                        courseProgram,
                      )

                    return programId
                      !== undefined
                      ? baseProgramCode(
                          programById.get(
                            programId,
                          )?.code,
                        )
                      : ""
                  },
                ),
              )

            const cohortNames =
              uniqueSort(
                displaySource.map(
                  getCourseProgramCohortName,
                ),
              )

            const majorCodes =
              uniqueSort(
                displaySource.map(
                  (courseProgram) => {
                    const direct =
                      getCourseProgramMajorCode(
                        courseProgram,
                      )

                    if (direct) {
                      return direct
                    }

                    const programId =
                      getCourseProgramProgramId(
                        courseProgram,
                      )

                    return programId
                      !== undefined
                      ? normalize(
                          programById.get(
                            programId,
                          )?.majorCode,
                        )
                      : ""
                  },
                ),
              )

            const syllabusSemester =
              normalizeSemesterValue(
                item.semester,
              )

            const semesterSuggests =
              uniqueSort(
                displaySource.map(
                  (courseProgram) =>
                    normalizeSemesterValue(
                      getSemesterSuggest(
                        courseProgram,
                      ),
                    ),
                ),
              )

            const semesterValues =
              syllabusSemester
                ? [syllabusSemester]
                : semesterSuggests

            const fallbackMajor =
              normalize(item.major)

            const fallbackProgram =
              selectedProgram?.code
              || ""

            const fallbackCohort =
              selectedCohort?.name
              || ""

            const matchesMajorFallback =
              selectedMajorCode === ALL
              || normalizeKey(
                fallbackMajor,
              )
              === normalizeKey(
                selectedMajorCode,
              )

            return {
              item,
              programCodes,
              cohortNames,
              majorCodes,
              semesterValues,

              matchesCurriculumContext:
                !hasCurriculumFilter
                || contextMatched.length > 0
                || (
                  selectedProgramId
                    === undefined
                  && selectedCohortId
                    === undefined
                  && matchesMajorFallback
                ),

              displayProgram:
                programCodes.join(", ")
                || fallbackProgram
                || "N/A",

              displayCohort:
                cohortNames.join(", ")
                || fallbackCohort
                || (
                  displaySource.length > 0
                    ? "Shared / All cohorts"
                    : "N/A"
                ),

              displayMajor:
                majorCodes.join(", ")
                || fallbackMajor
                || selectedProgram
                  ?.majorCode
                || "N/A",

              displaySemester:
                semesterValues
                  .map(
                    formatSemesterLabel,
                  )
                  .join(", ")
                || "N/A",
            }
          },
        )
      },
      [
        courseProgramIndex,
        data,
        programById,
        selectedCohort,
        selectedCohortId,
        selectedMajorCode,
        selectedProgram,
        selectedProgramId,
      ],
    )

  const filteredData =
    useMemo(() => {
      return enrichedData.filter(
        (row) => {
          const item =
            row.item

          if (
            !row
              .matchesCurriculumContext
          ) {
            return false
          }

          if (searchValue) {
            const query =
              searchValue
                .toLowerCase()
                .trim()

            const searchable = [
              item.courseCode,
              item.courseName,
              item.courseNameVn,
              item.createdByUsername,
            ]
              .map(normalizeKey)
              .join(" ")

            if (
              !searchable.includes(
                query,
              )
            ) {
              return false
            }
          }

          if (
            semesterValue !== ALL
            && !row.semesterValues.some(
              (value) =>
                normalizeKey(value)
                === normalizeKey(
                  semesterValue,
                ),
            )
          ) {
            return false
          }

          if (
            statusValue !== ALL
            && normalizeKey(
              item.status,
            )
            !== normalizeKey(
              statusValue,
            )
          ) {
            return false
          }

          return true
        },
      )
    }, [
      enrichedData,
      searchValue,
      semesterValue,
      statusValue,
    ])

  const summary =
    useMemo(() => {
      const rows =
        filteredData

      const approved =
        rows.filter(
          ({ item }) =>
            normalize(
              item.status,
            ).toUpperCase()
            === "APPROVED",
        ).length

      const pendingDepartmentReview =
        rows.filter(
          ({ item }) =>
            normalize(
              item.status,
            ).toUpperCase()
            === "SUBMITTED",
        ).length

      const forwardedToDean =
        rows.filter(
          ({ item }) =>
            normalize(
              item.status,
            ).toUpperCase()
            === "UNDER_REVIEW",
        ).length

      const revision =
        rows.filter(
          ({ item }) => {
            const status =
              normalize(
                item.status,
              ).toUpperCase()

            return (
              status === "REJECTED"
              || status
                === "REVISION_REQUESTED"
            )
          },
        ).length

      const visibleCourses =
        new Set(
          rows
            .map(
              ({ item }) =>
                normalizeKey(
                  item.courseCode,
                ),
            )
            .filter(Boolean),
        ).size

      const pendingReview =
        isDeptHead
          ? pendingDepartmentReview
          : isDean
            ? forwardedToDean
            : (
                pendingDepartmentReview
                + forwardedToDean
              )

      return {
        total: rows.length,
        visibleCourses,
        approved,
        pendingReview,
        pendingDepartmentReview,
        forwardedToDean,
        revision,
      }
    }, [
      filteredData,
      isDean,
      isDeptHead,
    ])

  const updateFilter = (
    key: string,
    value: string,
  ) => {
    const params =
      new URLSearchParams(
        searchParams,
      )

    if (
      value
      && value !== ALL
    ) {
      params.set(
        key,
        value,
      )
    } else {
      params.delete(key)
    }

    setSearchParams(params)
  }

  const updateProgramFilter =
    (
      value: string,
    ) => {
      const params =
        new URLSearchParams(
          searchParams,
        )

      if (
        !value
        || value === ALL
      ) {
        params.delete(
          "programId",
        )
        params.delete(
          "programCode",
        )
        params.delete(
          "cohortId",
        )

        setSearchParams(params)
        return
      }

      const programId =
        Number(value)

      const program =
        programs.find(
          (candidate) =>
            candidate.id
            === programId,
        )

      if (!program) {
        return
      }

      params.set(
        "programId",
        String(program.id),
      )

      params.set(
        "programCode",
        program.code,
      )

      params.set(
        "majorId",
        String(program.majorId),
      )

      params.set(
        "majorCode",
        program.majorCode,
      )

      params.delete(
        "cohortId",
      )

      params.delete(
        "cohort",
      )

      params.delete(
        "major",
      )

      setSearchParams(params)
    }

  const updateMajorFilter =
    (
      value: string,
    ) => {
      const params =
        new URLSearchParams(
          searchParams,
        )

      if (
        !value
        || value === ALL
      ) {
        params.delete("majorId")
        params.delete("majorCode")
        params.delete("major")

        /*
         * Program implies a major.
         * Clear program/cohort as well so "All Majors"
         * cannot coexist with a narrower curriculum context.
         */
        params.delete("programId")
        params.delete("programCode")
        params.delete("cohortId")

        setSearchParams(params)
        return
      }

      const major =
        majorByCode.get(
          normalizeKey(value),
        )

      if (!major) {
        return
      }

      params.set(
        "majorId",
        String(major.id),
      )

      params.set(
        "majorCode",
        major.code,
      )

      params.delete("major")

      if (
        selectedProgram
        && selectedProgram.majorId
          !== major.id
      ) {
        params.delete(
          "programId",
        )
        params.delete(
          "programCode",
        )
        params.delete(
          "cohortId",
        )
      }

      setSearchParams(params)
    }

  const updateCohortFilter =
    (
      value: string,
    ) => {
      const params =
        new URLSearchParams(
          searchParams,
        )

      if (
        !value
        || value === ALL
      ) {
        params.delete(
          "cohortId",
        )

        setSearchParams(params)
        return
      }

      const cohortId =
        Number(value)

      const cohort =
        cohortById.get(
          cohortId,
        )

      if (!cohort) {
        return
      }

      const program =
        programById.get(
          cohort.programId,
        )

      params.set(
        "cohortId",
        String(cohort.id),
      )

      if (program) {
        params.set(
          "programId",
          String(program.id),
        )

        params.set(
          "programCode",
          program.code,
        )

        params.set(
          "majorId",
          String(program.majorId),
        )

        params.set(
          "majorCode",
          program.majorCode,
        )
      }

      setSearchParams(params)
    }

  const resetFilters = () => {
    const params =
      new URLSearchParams()

    if (historyMode) {
      params.set(
        "history",
        "1",
      )
    }

    setSearchParams(params)
  }

  const facultyAssignmentForSyllabus =
    (
      item: Syllabus,
    ) => {
      if (!facultyDashboard) {
        return undefined
      }

      const courseId =
        getSyllabusCourseId(item)

      const academicYear =
        normalizeKey(
          item.academicYear,
        )

      const semester =
        normalizeSemesterValue(
          item.semester,
        )

      return facultyDashboard
        .upcomingDeadlines
        .find(
          (assignment) => {
            if (
              assignment.syllabusId
              === item.id
            ) {
              return true
            }

            return (
              courseId !== undefined
              && assignment.courseId
                === courseId
              && normalizeKey(
                assignment.academicYear,
              ) === academicYear
              && String(
                assignment.semester,
              ) === semester
            )
          },
        )
    }

  const canViewApprovalHistory =
    (
      _item: Syllabus,
    ) =>
      Boolean(user)

  const canCloneSyllabus =
    () =>
      isInstructor || isAdmin

  const canEditDraft =
    (
      item: Syllabus,
    ) =>
      isAdmin
      || (
        isInstructor
        && Boolean(
          facultyAssignmentForSyllabus(
            item,
          ),
        )
        && normalize(
          item.status,
        ).toUpperCase()
          === "DRAFT"
      )

  const canSubmit =
    (
      item: Syllabus,
    ) =>
      (isAdmin && normalize(item.status).toUpperCase() === "DRAFT")
      || canEditDraft(item)

  const handleDelete = (
    item: Syllabus,
  ) => {
    if (!canEditDraft(item)) {
      return
    }

    const confirmed =
      window.confirm(
        `Delete draft syllabus ${item.courseCode}? This action cannot be undone.`,
      )

    if (!confirmed) {
      return
    }

    deleteMutation.mutate(
      item.id,
      {
        onSuccess: () =>
          alert(
            "Draft syllabus deleted successfully.",
          ),

        onError: (
          error: any,
        ) =>
          alert(
            error?.response
              ?.data
              ?.message
            || "Unable to delete the draft syllabus.",
          ),
      },
    )
  }

  const handleSubmit = (
    item: Syllabus,
  ) => {
    if (!canSubmit(item)) {
      return
    }

    const confirmed =
      window.confirm(
        `Submit syllabus ${item.courseCode} for Head of Department review?`,
      )

    if (!confirmed) {
      return
    }

    submitMutation.mutate(
      item.id,
      {
        onSuccess: () =>
          alert(
            "Syllabus submitted successfully. It is awaiting Head of Department review.",
          ),

        onError: (
          error: any,
        ) =>
          alert(
            error?.response
              ?.data
              ?.message
            || "Unable to submit the syllabus.",
          ),
      },
    )
  }

  const handleClone = (
    item: Syllabus,
  ) => {
    if (
      !canCloneSyllabus()
    ) {
      return
    }

    setCloneSource(item)
  }

  const handleViewCurriculumMap =
    () => {
      if (!selectedProgram) {
        alert(
          "Select a curriculum program before opening the curriculum map.",
        )

        return
      }

      const params =
        new URLSearchParams()

      params.set(
        "programId",
        String(
          selectedProgram.id,
        ),
      )

      params.set(
        "programCode",
        selectedProgram.code,
      )

      params.set(
        "majorId",
        String(
          selectedProgram.majorId,
        ),
      )

      params.set(
        "majorCode",
        selectedProgram.majorCode,
      )

      if (
        selectedCohortId
        !== undefined
      ) {
        params.set(
          "cohortId",
          String(
            selectedCohortId,
          ),
        )
      }

      navigate({
        pathname:
          `${basePath}/curriculum-map`,

        search:
          params.toString(),
      })
    }

  const handleDownloadApprovedPdf =
    async (
      item: Syllabus,
    ) => {
      if (
        downloadingPdfId !== null
      ) {
        return
      }

      const status =
        normalize(
          item.status,
        ).toUpperCase()

      if (
        status !== "APPROVED"
      ) {
        alert(
          "Only approved syllabi can be exported as official PDFs.",
        )

        return
      }

      const fallbackName =
        [
          "Syllabus",
          safeFilePart(
            item.courseCode,
          ),
          safeFilePart(
            item.academicYear,
          ),
          safeFilePart(
            item.semester,
          ),
          safeFilePart(
            item.versionLabel,
          ),
          "Official",
        ].join("_")
        + ".pdf"

      try {
        setDownloadingPdfId(
          item.id,
        )

        await syllabusPdfApi
          .download(
            item.id,
            fallbackName,
          )
      } catch (error: any) {
        const message =
          await getPdfDownloadErrorMessage(
            error,
          )

        alert(
          message
          || "Unable to export the official syllabus PDF.",
        )
      } finally {
        setDownloadingPdfId(
          null,
        )
      }
    }

  if (isInstructor) {
    return (
      <>
        <InstructorSyllabusWorkspace
          dashboard={facultyDashboard}
          dashboardLoading={
            facultyDashboardLoading
          }
          dashboardError={
            facultyDashboardError
          }
          dashboardFetching={
            facultyDashboardFetching
          }
          syllabuses={
            data ?? []
          }
          syllabusesLoading={
            isLoading
          }
          syllabusesError={
            isError
          }
          basePath={basePath}
          downloadingPdfId={
            downloadingPdfId
          }
          submitting={
            submitMutation.isPending
          }
          onRefresh={() => {
            void refetchFacultyDashboard()
          }}
          onNavigate={navigate}
          onSubmit={handleSubmit}
          onClone={handleClone}
          onDelete={handleDelete}
          onDownloadOfficial={
            handleDownloadApprovedPdf
          }
          canEditDraft={
            canEditDraft
          }
          canViewApprovalHistory={
            canViewApprovalHistory
          }
        />

        <CloneSyllabusDialog
          source={cloneSource}
          open={cloneSource !== null}
          onOpenChange={(open) => {
            if (!open) {
              setCloneSource(null)
            }
          }}
          onCloned={(
            newSyllabus,
          ) =>
            navigate(
              `${basePath}/${newSyllabus.id}/editor?import=1`,
            )
          }
        />
      </>
    )
  }

  if (
    isLoading
    || referenceLoading
  ) {
    return (
      <div className="flex h-64 items-center justify-center">
        <div className="text-sm font-medium text-slate-500">
          Loading syllabus catalog...
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700">
        <h3 className="text-lg font-bold">
          Unable to Load Syllabuses
        </h3>

        <p className="mt-2 text-sm">
          The syllabus catalog could not be loaded. Please try again later.
        </p>
      </div>
    )
  }

  const pageTitle =
    isDeptHead
      ? "Department Syllabus Oversight"
      : isDean
        ? "Syllabus Oversight"
        : isAdmin
          ? "Syllabus Administration"
          : "My Syllabi"

  const pageDescription =
    isDeptHead
      ? "Review syllabus versions within your department scope, track the Department Head and Dean workflow, inspect review history, and access approved official PDFs."
      : isDean
        ? "Monitor syllabus versions, final-review status, official PDFs, and complete review history across the School."
        : isAdmin
          ? "Monitor syllabus records, curriculum scope, workflow status, official PDFs, and review history."
          : "Create, edit, submit, clone, and track syllabuses for your assigned courses."

  return (
    <div className="min-h-screen -m-6 bg-[#f7faf9] p-6 text-slate-900 md:-m-10 md:p-8">
      <div className="mx-auto max-w-[1550px] space-y-5">
        <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
          <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

          <div className="flex flex-col gap-5 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                SCSE / Syllabus Management
              </p>

              <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
                {pageTitle}
              </h1>

              <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
                {pageDescription}
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                disabled={!selectedProgram}
                title={
                  selectedProgram
                    ? "Open the curriculum map for the selected program."
                    : "Select a program first."
                }
                onClick={
                  handleViewCurriculumMap
                }
              >
                <MapIcon className="size-4" />
                View Curriculum Map
              </Button>

              {(isInstructor || isAdmin) && (
                <Button
                  type="button"
                  className="bg-[#007d84] text-white hover:bg-[#006d73]"
                  onClick={() =>
                    navigate(
                      `${basePath}/create${isAdmin ? "?import=1" : ""}`,
                    )
                  }
                >
                  <Plus className="size-4" />
                  {isAdmin ? "Add Syllabus" : "Create New Syllabus"}
                </Button>
              )}
            </div>
          </div>
        </section>

        {isDeptHead && (
          <section className="flex flex-col gap-3 rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#007d84]">
                Department Scope
              </p>

              <p className="mt-1 text-sm font-semibold text-[#17343d]">
                {summary.total} visible syllabus version(s) across {summary.visibleCourses} department course(s)
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Access is restricted by the signed-in Department Head account. Curriculum filters refine this authorized data; they do not expand access outside the department.
              </p>
            </div>

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0 bg-white"
              onClick={() =>
                navigate(
                  "/dept-head/approvals",
                )
              }
            >
              <Send className="size-3.5" />
              Open Review Queue
            </Button>
          </section>
        )}

        {historyMode && (
          <section className="flex flex-col gap-3 rounded-xl border border-blue-200 bg-blue-50 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-3">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
                <History className="size-5" />
              </span>

              <div>
                <p className="font-semibold text-blue-950">
                  Syllabus History
                </p>

                <p className="mt-1 text-sm text-blue-700">
                  Use the History action on a syllabus to inspect its approval trail and review comments.
                </p>
              </div>
            </div>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="w-fit text-blue-700 hover:bg-blue-100"
              onClick={() => {
                const params =
                  new URLSearchParams(
                    searchParams,
                  )

                params.delete(
                  "history",
                )

                setSearchParams(
                  params,
                )
              }}
            >
              Close Guide
            </Button>
          </section>
        )}

        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard
            label={
              isDeptHead
                ? "Visible Versions"
                : "Visible Syllabuses"
            }
            value={summary.total}
          />

          <SummaryCard
            label="Approved"
            value={summary.approved}
            tone="success"
          />

          <SummaryCard
            label={
              isDeptHead
                ? "Pending Department Review"
                : isDean
                  ? "Pending Final Review"
                  : "Pending Review"
            }
            value={summary.pendingReview}
            tone="info"
          />

          <SummaryCard
            label="Forwarded to Dean"
            value={summary.forwardedToDean}
            tone="info"
          />

          <SummaryCard
            label="Rejected / Revision"
            value={summary.revision}
            tone="warning"
          />
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <Filter className="size-4 text-orange-600" />

                  <h2 className="font-semibold text-[#17343d]">
                    Filters
                  </h2>
                </div>

                <p className="mt-1 text-xs text-slate-500">
                  Major → Program → Cohort forms the curriculum hierarchy. Then refine by Semester and Status.
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={resetFilters}
              >
                <RotateCcw className="size-4" />
                Clear Filters
              </Button>
            </div>
          </div>

          <div className="space-y-4 p-5">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

              <Input
                className="h-10 bg-slate-50/50 pl-9"
                placeholder="Search by course code, course name, or instructor..."
                value={searchValue}
                onChange={(event) =>
                  updateFilter(
                    "q",
                    event.target.value,
                  )
                }
              />
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Major
                </label>

                <Select
                  value={
                    selectedMajorCode
                  }
                  onValueChange={
                    updateMajorFilter
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Major" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      All Majors
                    </SelectItem>

                    {majorOptions.map(
                      (major) => (
                        <SelectItem
                          key={major.id}
                          value={major.code}
                        >
                          {major.code}
                          {normalize((major as any).name)
                            ? ` — ${normalize((major as any).name)}`
                            : normalize((major as any).nameVn)
                              ? ` — ${normalize((major as any).nameVn)}`
                              : ""}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Program
                </label>

                <Select
                  value={
                    selectedProgramId
                      !== undefined
                      ? String(
                          selectedProgramId,
                        )
                      : ALL
                  }
                  onValueChange={
                    updateProgramFilter
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Program" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      {selectedMajorCode === ALL
                        ? "All Programs"
                        : "All Programs in Major"}
                    </SelectItem>

                    {programOptions.map(
                      (program) => (
                        <SelectItem
                          key={program.id}
                          value={String(
                            program.id,
                          )}
                        >
                          {getProgramOptionLabel(
                            program,
                          )}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Cohort
                </label>

                <Select
                  value={
                    selectedCohortId
                      !== undefined
                      ? String(
                          selectedCohortId,
                        )
                      : ALL
                  }
                  onValueChange={
                    updateCohortFilter
                  }
                  disabled={
                    selectedProgramId
                    === undefined
                  }
                >
                  <SelectTrigger
                    title={
                      selectedProgramId
                        === undefined
                        ? "Select a Program before choosing a Cohort."
                        : "Filter cohorts belonging to the selected Program."
                    }
                  >
                    <SelectValue placeholder="Cohort" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      {selectedProgramId === undefined
                        ? "Select Program First"
                        : "All Cohorts in Program"}
                    </SelectItem>

                    {cohortOptions.map(
                      (cohort) => (
                        <SelectItem
                          key={cohort.id}
                          value={String(
                            cohort.id,
                          )}
                        >
                          {getCohortOptionLabel(
                            cohort,
                          )}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Semester
                </label>

                <Select
                  value={
                    semesterValue
                  }
                  onValueChange={(value) =>
                    updateFilter(
                      "semester",
                      value,
                    )
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Semester" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      All Semesters
                    </SelectItem>

                    {semesterOptions.map(
                      (semester) => (
                        <SelectItem
                          key={semester}
                          value={semester}
                        >
                          {formatSemesterLabel(
                            semester,
                          )}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Status
                </label>

                <Select
                  value={
                    statusValue
                  }
                  onValueChange={(value) =>
                    updateFilter(
                      "status",
                      value,
                    )
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Status" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      All Statuses
                    </SelectItem>

                    {statusOptions.map(
                      (status) => (
                        <SelectItem
                          key={status}
                          value={status}
                        >
                          {formatStatusLabelForRole(
                            status,
                            role,
                          )}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <p className="text-xs text-slate-500">
              Showing{" "}
              <span className="font-semibold text-slate-700">
                {filteredData.length}
              </span>
              {" "}of{" "}
              <span className="font-semibold text-slate-700">
                {data?.length ?? 0}
              </span>
              {" "}syllabuses.
            </p>
          </div>
        </section>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1320px] text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
                <tr>
                  <th className="px-4 py-3 text-left">
                    Course
                  </th>

                  <th className="px-4 py-3 text-left">
                    Version
                  </th>

                  <th className="px-4 py-3 text-left">
                    Cohort
                  </th>

                  <th className="px-4 py-3 text-left">
                    Program / Cohort
                  </th>

                  <th className="px-4 py-3 text-left">
                    Semester
                  </th>

                  <th className="px-4 py-3 text-left">
                    Created / Imported By
                  </th>

                  <th className="px-4 py-3 text-left">
                    Status
                  </th>

                  <th className="px-4 py-3 text-left">
                    Final Approval Date
                  </th>

                  <th className="px-4 py-3 text-right">
                    Actions
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {filteredData.length === 0 ? (
                  <tr>
                    <td
                      colSpan={9}
                      className="px-5 py-12 text-center"
                    >
                      <p className="font-medium text-slate-600">
                        No syllabuses match the selected filters.
                      </p>

                      <p className="mt-1 text-xs text-slate-400">
                        Clear or adjust the filters to view more records.
                      </p>
                    </td>
                  </tr>
                ) : (
                  filteredData.map(
                    ({
                      item,
                      displayProgram,
                      displayCohort,
                      displayMajor,
                      displaySemester,
                    }) => {
                      const itemStatus =
                        normalize(
                          item.status,
                        ).toUpperCase()

                      return (
                        <tr
                          key={item.id}
                          className="transition-colors hover:bg-[#f8fbfb]"
                        >
                          <td className="px-4 py-4">
                            <button
                              type="button"
                              className="text-left"
                              onClick={() =>
                                navigate(
                                  `${basePath}/${item.id}`,
                                )
                              }
                            >
                              <p className="font-mono text-xs font-bold text-[#007d84] hover:underline">
                                {item.courseCode}
                              </p>

                              <p className="mt-1 max-w-[250px] font-semibold text-slate-800">
                                {item.courseName}
                              </p>

                              {item.courseNameVn && (
                                <p className="mt-0.5 max-w-[250px] text-xs text-slate-400">
                                  {item.courseNameVn}
                                </p>
                              )}
                            </button>
                          </td>

                          <td className="px-4 py-4">
                            <span className="font-medium text-slate-700">
                              {item.cohortName
                                || displayCohort
                                || "Not assigned"}
                            </span>
                          </td>

                          <td className="px-4 py-4">
                            <div className="flex flex-wrap items-center gap-1.5">
                              <span className="font-semibold text-slate-700">
                                {item.versionLabel
                                  || `v${item.versionNumber}`}
                              </span>

                              {item.isCurrent && (
                                <span className="rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                                  Current
                                </span>
                              )}
                            </div>
                          </td>

                          <td className="px-4 py-4">
                            <p className="font-medium text-slate-700">
                              {displayProgram}
                            </p>

                            <p
                              className={
                                "mt-1 text-xs "
                                + (
                                  displayCohort
                                  === "Shared / All cohorts"
                                    ? "font-medium text-blue-600"
                                    : "text-slate-500"
                                )
                              }
                            >
                              Cohort: {displayCohort}
                            </p>

                            <p className="mt-0.5 text-[10px] text-slate-400">
                              Major: {displayMajor}
                            </p>
                          </td>

                          <td className="px-4 py-4">
                            <p className="font-medium text-slate-700">
                              {displaySemester}
                            </p>
                          </td>

                          <td className="px-4 py-4">
                            <p className="font-medium text-slate-700">
                              {item.createdByUsername
                                || "N/A"}
                            </p>
                          </td>

                          <td className="px-4 py-4">
                            <span
                              className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-bold ${getStatusClass(item.status, role)}`}
                            >
                              {formatStatusLabelForRole(
                                item.status,
                                role,
                              )}
                            </span>
                          </td>

                          <td className="px-4 py-4 text-xs text-slate-600">
                            {itemStatus === "APPROVED"
                              ? (
                                  item.approvedAt
                                    ? formatDateTime(
                                        item.approvedAt,
                                      )
                                    : (
                                      <span className="font-medium text-amber-700">
                                        Not recorded
                                      </span>
                                    )
                                )
                              : "—"}
                          </td>

                          <td className="px-4 py-4">
                            <div className="flex flex-wrap justify-end gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 gap-1.5 px-2.5"
                                onClick={() =>
                                  navigate(
                                    `${basePath}/${item.id}`,
                                  )
                                }
                              >
                                <Eye className="size-3.5" />
                                <span className="text-xs">
                                  View
                                </span>
                              </Button>

                              {canViewApprovalHistory(
                                item,
                              ) && (
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="h-8 gap-1.5 border-blue-200 bg-blue-50 px-2.5 text-blue-700 hover:bg-blue-100 hover:text-blue-800"
                                  title={`View history for ${item.courseCode}`}
                                  onClick={() =>
                                    navigate(
                                      `${basePath}/${item.id}#approval-history`,
                                    )
                                  }
                                >
                                  <History className="size-3.5" />
                                  <span className="text-xs">
                                    History
                                  </span>
                                </Button>
                              )}

                              {itemStatus
                                === "APPROVED" && (
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="h-8 gap-1.5 border-emerald-200 bg-emerald-50 px-2.5 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                                  disabled={
                                    downloadingPdfId
                                    !== null
                                  }
                                  title="Export official PDF"
                                  onClick={() =>
                                    void handleDownloadApprovedPdf(
                                      item,
                                    )
                                  }
                                >
                                  {downloadingPdfId
                                    === item.id
                                    ? (
                                      <LoaderCircle className="size-3.5 animate-spin" />
                                    )
                                    : (
                                      <Download className="size-3.5" />
                                    )}

                                  <span className="text-xs">
                                    PDF
                                  </span>
                                </Button>
                              )}

                              {canSubmit(item) && (
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 w-8 p-0 text-blue-600 hover:bg-blue-50 hover:text-blue-700"
                                  disabled={
                                    submitMutation
                                      .isPending
                                  }
                                  title="Submit syllabus"
                                  onClick={() =>
                                    handleSubmit(
                                      item,
                                    )
                                  }
                                >
                                  <Send className="size-4" />
                                </Button>
                              )}

                              {canCloneSyllabus() && (
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 w-8 p-0"
                                  title="Clone syllabus"
                                  onClick={() =>
                                    handleClone(
                                      item,
                                    )
                                  }
                                >
                                  <Copy className="size-4" />
                                </Button>
                              )}

                              {canEditDraft(item) && (
                                <>
                                  <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="h-8 w-8 p-0"
                                    title="Edit draft"
                                    onClick={() =>
                                      navigate(
                                        `${basePath}/${item.id}/editor`,
                                      )
                                    }
                                  >
                                    <Edit2 className="size-4" />
                                  </Button>

                                  <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="h-8 w-8 p-0 text-violet-600 hover:bg-violet-50 hover:text-violet-700"
                                    title="Import PDF / Word"
                                    onClick={() =>
                                      navigate(`${basePath}/${item.id}/editor?import=1`)
                                    }
                                  >
                                    <Upload className="size-4" />
                                  </Button>

                                  <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="h-8 w-8 p-0 text-rose-500 hover:bg-rose-50 hover:text-rose-600"
                                    title={isAdmin ? "Delete syllabus" : "Delete draft"}
                                    onClick={() =>
                                      handleDelete(
                                        item,
                                      )
                                    }
                                  >
                                    <Trash2 className="size-4" />
                                  </Button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      )
                    },
                  )
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {(isInstructor || isAdmin) && (
        <CloneSyllabusDialog
          source={cloneSource}
          open={cloneSource !== null}
          onOpenChange={(open) => {
            if (!open) {
              setCloneSource(null)
            }
          }}
          onCloned={(
            newSyllabus,
          ) =>
            navigate(
              `${basePath}/${newSyllabus.id}/editor?import=1`,
            )
          }
        />
      )}
    </div>
  )
}


type InstructorWorkflowFilter =
  | "ALL"
  | "NOT_STARTED"
  | "DRAFT_REVISION"
  | "IN_REVIEW"
  | "APPROVED"

function InstructorSyllabusWorkspace({
  dashboard,
  dashboardLoading,
  dashboardError,
  dashboardFetching,
  syllabuses,
  syllabusesLoading,
  syllabusesError,
  basePath,
  downloadingPdfId,
  submitting,
  onRefresh,
  onNavigate,
  onSubmit,
  onClone,
  onDelete,
  onDownloadOfficial,
  canEditDraft,
  canViewApprovalHistory,
}: {
  dashboard?: DashboardFacultyResponse
  dashboardLoading: boolean
  dashboardError: boolean
  dashboardFetching: boolean
  syllabuses: Syllabus[]
  syllabusesLoading: boolean
  syllabusesError: boolean
  basePath: string
  downloadingPdfId: number | null
  submitting: boolean
  onRefresh: () => void
  onNavigate: (to: string) => void
  onSubmit: (item: Syllabus) => void
  onClone: (item: Syllabus) => void
  onDelete: (item: Syllabus) => void
  onDownloadOfficial: (
    item: Syllabus,
  ) => Promise<void>
  canEditDraft: (
    item: Syllabus,
  ) => boolean
  canViewApprovalHistory: (
    item: Syllabus,
  ) => boolean
}) {
  const [
    searchTerm,
    setSearchTerm,
  ] = useState("")

  const [
    termFilter,
    setTermFilter,
  ] = useState(ALL)

  const [
    workflowFilter,
    setWorkflowFilter,
  ] =
    useState<InstructorWorkflowFilter>(
      "ALL",
    )

  const assignments =
    dashboard?.upcomingDeadlines
    ?? []

  const filteredAssignments =
    useMemo(() => {
      const query =
        searchTerm
          .trim()
          .toLowerCase()

      return assignments
        .filter(
          (item) => {
            if (
              termFilter !== ALL
              && item.termKey
                !== termFilter
            ) {
              return false
            }

            if (
              !matchesInstructorWorkflow(
                item,
                workflowFilter,
              )
            ) {
              return false
            }

            if (!query) {
              return true
            }

            return [
              item.courseCode,
              item.courseName,
              item.courseNameVn,
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
                    .includes(query),
              )
          },
        )
        .slice()
        .sort(
          (left, right) => {
            const leftPriority =
              instructorAssignmentPriority(
                left,
              )

            const rightPriority =
              instructorAssignmentPriority(
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

            return left.courseCode
              .localeCompare(
                right.courseCode,
                "en",
                {
                  numeric: true,
                },
              )
          },
        )
    }, [
      assignments,
      searchTerm,
      termFilter,
      workflowFilter,
    ])

  const instructorSummary =
    useMemo(() => {
      const notStarted =
        assignments.filter(
          (item) =>
            !item.syllabusId
            || normalize(
              item.status,
            ).toUpperCase()
              === "NOT_CREATED",
        ).length

      const draftRevision =
        assignments.filter(
          (item) => {
            const status =
              normalize(
                item.status,
              ).toUpperCase()

            return (
              status === "DRAFT"
              || status
                === "REJECTED"
              || status
                === "REVISION_REQUESTED"
              || item.recommendedAction
                === "REVISE"
            )
          },
        ).length

      const inReview =
        assignments.filter(
          (item) =>
            [
              "SUBMITTED",
              "UNDER_REVIEW",
            ].includes(
              normalize(
                item.status,
              ).toUpperCase(),
            ),
        ).length

      const approved =
        assignments.filter(
          (item) =>
            normalize(
              item.status,
            ).toUpperCase()
              === "APPROVED",
        ).length

      return {
        assigned:
          dashboard
            ?.assignedCourses
          ?? assignments.length,
        notStarted,
        draftRevision,
        inReview,
        approved,
      }
    }, [
      assignments,
      dashboard?.assignedCourses,
    ])

  const uncreatedAssignments =
    assignments.filter(
      (item) =>
        !item.syllabusId
        || item.recommendedAction
          === "CREATE",
    )

  const createPath =
    uncreatedAssignments.length
      === 1
      ? (
          `${basePath}/create`
          + `?classSectionId=${uncreatedAssignments[0].primaryClassSectionId}`
        )
      : `${basePath}/create`

  const versionRows =
    useMemo(
      () =>
        syllabuses
          .slice()
          .sort(
            (left, right) => {
              const leftUpdated =
                new Date(
                  left.updatedAt
                  ?? left.submittedAt
                  ?? 0,
                ).getTime()

              const rightUpdated =
                new Date(
                  right.updatedAt
                  ?? right.submittedAt
                  ?? 0,
                ).getTime()

              return (
                rightUpdated
                - leftUpdated
              )
            },
          ),
      [syllabuses],
    )

  if (
    dashboardLoading
    || syllabusesLoading
  ) {
    return (
      <div className="flex h-64 items-center justify-center text-sm font-medium text-slate-500">
        <LoaderCircle className="mr-2 size-5 animate-spin" />
        Loading your syllabus workspace...
      </div>
    )
  }

  if (
    dashboardError
    || syllabusesError
    || !dashboard
  ) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-rose-700">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />

          <div>
            <h2 className="font-semibold">
              Unable to load My Syllabi
            </h2>

            <p className="mt-1 text-sm leading-6">
              Confirm that this account is linked to an Instructor profile and has active teaching assignments.
            </p>

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-3 bg-white"
              onClick={onRefresh}
            >
              <RotateCcw className="size-4" />
              Try Again
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen -m-6 bg-[#f7faf9] p-6 text-slate-900 md:-m-10 md:p-8">
      <div className="mx-auto max-w-[1550px] space-y-5">
        <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
          <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

          <div className="flex flex-col gap-5 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                SCSE / Instructor Syllabus Workspace
              </p>

              <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
                My Syllabi
              </h1>

              <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
                Work from your active teaching assignments: create or clone a syllabus, continue Draft work, submit for Department review, read reviewer feedback, and track every version through final approval.
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                disabled={
                  dashboardFetching
                }
                onClick={onRefresh}
              >
                {dashboardFetching ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <RotateCcw className="size-4" />
                )}
                Refresh
              </Button>

              <Button
                type="button"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                disabled={
                  uncreatedAssignments.length
                    === 0
                }
                title={
                  uncreatedAssignments.length
                    === 0
                    ? "All active assignments already have a syllabus."
                    : "Create a syllabus from an active teaching assignment."
                }
                onClick={() =>
                  onNavigate(
                    createPath,
                  )
                }
              >
                <Plus className="size-4" />
                Create from Assignment
              </Button>
            </div>
          </div>
        </section>

        <section className="flex flex-col gap-3 rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

            <div>
              <p className="font-semibold text-[#17343d]">
                Assignment-scoped access
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                You can create and edit syllabuses only for active teaching assignments that match the course, academic year, and semester. Submitted and approved snapshots remain read-only.
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="shrink-0 bg-white"
            onClick={() =>
              onNavigate(
                "/instructor/class-sections",
              )
            }
          >
            <BookOpen className="size-3.5" />
            View Class Sections
          </Button>
        </section>

        <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard
            label="Assigned Courses"
            value={
              instructorSummary.assigned
            }
          />

          <SummaryCard
            label="Not Started"
            value={
              instructorSummary.notStarted
            }
            tone="warning"
          />

          <SummaryCard
            label="Draft / Revision"
            value={
              instructorSummary.draftRevision
            }
            tone="warning"
          />

          <SummaryCard
            label="In Review"
            value={
              instructorSummary.inReview
            }
            tone="info"
          />

          <SummaryCard
            label="Approved"
            value={
              instructorSummary.approved
            }
            tone="success"
          />
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <Filter className="size-4 text-orange-600" />

                  <h2 className="font-semibold text-[#17343d]">
                    My Assignment Scope
                  </h2>
                </div>

                <p className="mt-1 text-xs text-slate-500">
                  Filter by academic term and workflow state. Curriculum Program/Cohort filters are intentionally omitted because Instructor access is defined by teaching assignment.
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={
                  !searchTerm
                  && termFilter
                    === ALL
                  && workflowFilter
                    === "ALL"
                }
                onClick={() => {
                  setSearchTerm("")
                  setTermFilter(ALL)
                  setWorkflowFilter(
                    "ALL",
                  )
                }}
              >
                <RotateCcw className="size-4" />
                Clear Filters
              </Button>
            </div>
          </div>

          <div className="grid gap-3 p-5 lg:grid-cols-[minmax(320px,1fr)_240px_240px]">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

              <Input
                className="pl-9"
                placeholder="Search course, room, or schedule..."
                value={searchTerm}
                onChange={(event) =>
                  setSearchTerm(
                    event.target.value,
                  )
                }
              />
            </div>

            <Select
              value={termFilter}
              onValueChange={
                setTermFilter
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Academic term" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Academic Terms
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
              value={workflowFilter}
              onValueChange={(value) =>
                setWorkflowFilter(
                  value as InstructorWorkflowFilter,
                )
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Workflow state" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value="ALL">
                  All Workflow States
                </SelectItem>

                <SelectItem value="NOT_STARTED">
                  Not Started
                </SelectItem>

                <SelectItem value="DRAFT_REVISION">
                  Draft / Revision
                </SelectItem>

                <SelectItem value="IN_REVIEW">
                  In Review
                </SelectItem>

                <SelectItem value="APPROVED">
                  Approved
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </section>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <h2 className="font-semibold text-[#17343d]">
              Assigned Course Work Queue
            </h2>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              One row per assigned course and academic term. This queue remains visible even before a syllabus has been created.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1180px] text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
                <tr>
                  <th className="px-4 py-3 text-left">
                    Course
                  </th>

                  <th className="px-4 py-3 text-left">
                    Teaching Assignment
                  </th>

                  <th className="px-4 py-3 text-left">
                    Syllabus
                  </th>

                  <th className="px-4 py-3 text-left">
                    Official Deadline
                  </th>

                  <th className="px-4 py-3 text-left">
                    Time Remaining
                  </th>

                  <th className="px-4 py-3 text-right">
                    Next Action
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {filteredAssignments.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-5 py-12 text-center"
                    >
                      <p className="font-medium text-slate-600">
                        No assigned courses match the selected filters.
                      </p>

                      <p className="mt-1 text-xs text-slate-400">
                        If the Dashboard shows an assignment, clear the filters. If no assignments exist, contact the administrator responsible for teaching assignments.
                      </p>
                    </td>
                  </tr>
                ) : (
                  filteredAssignments.map(
                    (item) => (
                      <InstructorAssignmentRow
                        key={`${item.courseId}-${item.termKey}`}
                        item={item}
                        basePath={basePath}
                        onNavigate={
                          onNavigate
                        }
                      />
                    ),
                  )
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-semibold text-[#17343d]">
                My Syllabus Versions
              </h2>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Preserved syllabus versions available within your assignment scope. Use History to read reviewer comments and View to compare preserved versions.
              </p>
            </div>

            <BadgeLike>
              {versionRows.length} version(s)
            </BadgeLike>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[1120px] text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
                <tr>
                  <th className="px-4 py-3 text-left">
                    Course
                  </th>

                  <th className="px-4 py-3 text-left">
                    Version
                  </th>

                  <th className="px-4 py-3 text-left">
                    Academic Term
                  </th>

                  <th className="px-4 py-3 text-left">
                    Status
                  </th>

                  <th className="px-4 py-3 text-left">
                    Final Approval Date
                  </th>

                  <th className="px-4 py-3 text-right">
                    Actions
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {versionRows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-5 py-12 text-center"
                    >
                      <p className="font-medium text-slate-600">
                        No syllabus version has been created yet.
                      </p>

                      <p className="mt-1 text-xs text-slate-400">
                        Your teaching assignment is still shown above. Use Create Syllabus from the work queue to start a Draft.
                      </p>
                    </td>
                  </tr>
                ) : (
                  versionRows.map(
                    (item) => {
                      const itemStatus =
                        normalize(
                          item.status,
                        ).toUpperCase()

                      return (
                        <tr
                          key={item.id}
                          className="hover:bg-[#f8fbfb]"
                        >
                          <td className="px-4 py-4">
                            <p className="font-mono text-xs font-bold text-[#007d84]">
                              {item.courseCode}
                            </p>

                            <p className="mt-1 max-w-[260px] font-semibold text-slate-800">
                              {item.courseName}
                            </p>
                          </td>

                          <td className="px-4 py-4">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="font-semibold text-slate-700">
                                {item.versionLabel
                                  || `v${item.versionNumber}`}
                              </span>

                              {item.isCurrent && (
                                <span className="rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                                  Current
                                </span>
                              )}
                            </div>
                          </td>

                          <td className="px-4 py-4">
                            <p className="font-medium text-slate-700">
                              {item.academicYear
                                || "Not specified"}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                              {formatSemesterLabel(
                                item.semester,
                              )}
                            </p>
                          </td>

                          <td className="px-4 py-4">
                            <span
                              className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-bold ${getStatusClass(item.status, "INSTRUCTOR")}`}
                            >
                              {formatStatusLabelForRole(
                                item.status,
                                "INSTRUCTOR",
                              )}
                            </span>
                          </td>

                          <td className="px-4 py-4 text-xs text-slate-600">
                            {itemStatus
                              === "APPROVED"
                              ? (
                                  item.approvedAt
                                    ? formatDateTime(
                                        item.approvedAt,
                                      )
                                    : "Not recorded"
                                )
                              : "—"}
                          </td>

                          <td className="px-4 py-4">
                            <div className="flex flex-wrap justify-end gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                className="h-8 gap-1.5"
                                onClick={() =>
                                  onNavigate(
                                    `${basePath}/${item.id}`,
                                  )
                                }
                              >
                                <Eye className="size-3.5" />
                                View
                              </Button>

                              {canViewApprovalHistory(
                                item,
                              ) && (
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="h-8 gap-1.5 border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-100 hover:text-blue-800"
                                  onClick={() =>
                                    onNavigate(
                                      `${basePath}/${item.id}#approval-history`,
                                    )
                                  }
                                >
                                  <History className="size-3.5" />
                                  History
                                </Button>
                              )}

                              {itemStatus
                                === "APPROVED" && (
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="h-8 gap-1.5 border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                                  disabled={
                                    downloadingPdfId
                                    !== null
                                  }
                                  onClick={() =>
                                    void onDownloadOfficial(
                                      item,
                                    )
                                  }
                                >
                                  {downloadingPdfId
                                    === item.id
                                    ? (
                                      <LoaderCircle className="size-3.5 animate-spin" />
                                    )
                                    : (
                                      <Download className="size-3.5" />
                                    )}
                                  Official PDF
                                </Button>
                              )}

                              {canEditDraft(
                                item,
                              ) && (
                                <>
                                  <Button
                                    type="button"
                                    size="sm"
                                    className="h-8 bg-[#007d84] text-white hover:bg-[#006d73]"
                                    onClick={() =>
                                      onNavigate(
                                        `${basePath}/${item.id}/editor`,
                                      )
                                    }
                                  >
                                    <Edit2 className="size-3.5" />
                                    Edit Draft
                                  </Button>

                                  <Button
                                    type="button"
                                    variant="outline"
                                    size="sm"
                                    className="h-8 border-blue-200 text-blue-700"
                                    disabled={
                                      submitting
                                    }
                                    onClick={() =>
                                      onSubmit(
                                        item,
                                      )
                                    }
                                  >
                                    <Send className="size-3.5" />
                                    Submit
                                  </Button>

                                  <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="h-8 text-rose-600 hover:bg-rose-50"
                                    onClick={() =>
                                      onDelete(
                                        item,
                                      )
                                    }
                                  >
                                    <Trash2 className="size-3.5" />
                                    Delete Draft
                                  </Button>
                                </>
                              )}

                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="h-8"
                                title="Clone this preserved syllabus into another active assignment for the same course."
                                onClick={() =>
                                  onClone(
                                    item,
                                  )
                                }
                              >
                                <Copy className="size-3.5" />
                                Clone
                              </Button>
                            </div>
                          </td>
                        </tr>
                      )
                    },
                  )
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

            <div>
              <p className="font-semibold text-[#17343d]">
                Syllabus workflow
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Draft → Pending Department Review → Pending Dean Review → Approved. If a reviewer returns a submission, the reviewed snapshot remains preserved and you continue on the new/current Draft revision.
              </p>
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}

function InstructorAssignmentRow({
  item,
  basePath,
  onNavigate,
}: {
  item: FacultyCourseAssignment
  basePath: string
  onNavigate: (to: string) => void
}) {
  const action =
    getInstructorAssignmentAction(
      item,
      basePath,
    )

  return (
    <tr className="align-top hover:bg-[#f8fbfb]">
      <td className="px-4 py-4">
        <p className="font-mono text-xs font-bold text-[#007d84]">
          {item.courseCode}
        </p>

        <p className="mt-1 max-w-[260px] font-semibold text-slate-800">
          {item.courseName}
        </p>

        {item.courseNameVn
          && item.courseNameVn
            !== item.courseName && (
            <p className="mt-0.5 max-w-[260px] text-xs text-slate-400">
              {item.courseNameVn}
            </p>
          )}
      </td>

      <td className="px-4 py-4">
        <p className="font-semibold text-slate-700">
          Semester {item.semester} · {item.academicYear}
        </p>

        <p className="mt-1 text-xs text-slate-500">
          {item.sectionCount} section(s)
          {item.groupNumbers.length
            > 0
            ? ` · Group ${item.groupNumbers.join(", ")}`
            : ""}
        </p>

        {item.rooms.length > 0 && (
          <p className="mt-1 text-xs text-slate-400">
            Room: {item.rooms.join(", ")}
          </p>
        )}
      </td>

      <td className="px-4 py-4">
        <InstructorSyllabusStatusBadge
          item={item}
        />

        {item.syllabusVersionLabel && (
          <p className="mt-1 text-xs text-slate-500">
            {item.syllabusVersionLabel}
            {item.currentVersion
              ? " · Current"
              : ""}
          </p>
        )}
      </td>

      <td className="px-4 py-4">
        {item.deadline ? (
          <>
            <p className="font-medium text-slate-700">
              {formatFullDateTime(
                item.deadline,
              )}
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Official submission deadline
            </p>
          </>
        ) : (
          <>
            <p className="font-medium text-slate-500">
              Not configured
            </p>

            <p className="mt-1 text-xs text-slate-400">
              Awaiting administrator configuration
            </p>
          </>
        )}
      </td>

      <td className="px-4 py-4">
        <InstructorDeadlineBadge
          state={
            item.deadlineState
          }
          minutesRemaining={
            item.minutesRemaining
          }
        />
      </td>

      <td className="px-4 py-4">
        <div className="flex justify-end">
          <Button
            type="button"
            size="sm"
            variant={
              item.actionRequired
                ? "default"
                : "outline"
            }
            className={
              item.actionRequired
                ? "bg-[#007d84] text-white hover:bg-[#006d73]"
                : ""
            }
            onClick={() =>
              onNavigate(
                action.path,
              )
            }
          >
            {action.label}
            <PlusOrArrow
              create={
                item.recommendedAction
                  === "CREATE"
              }
            />
          </Button>
        </div>
      </td>
    </tr>
  )
}

function InstructorSyllabusStatusBadge({
  item,
}: {
  item: FacultyCourseAssignment
}) {
  const status =
    normalize(
      item.status,
    ).toUpperCase()

  if (
    !item.syllabusId
    || status === "NOT_CREATED"
  ) {
    return (
      <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-[10px] font-bold text-slate-600">
        Not Started
      </span>
    )
  }

  if (
    item.recommendedAction
      === "REVISE"
  ) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-1 text-[10px] font-bold text-rose-700">
        <AlertTriangle className="size-3" />
        Revision Required
      </span>
    )
  }

  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-bold ${getStatusClass(item.status, "INSTRUCTOR")}`}
    >
      {formatStatusLabelForRole(
        item.status,
        "INSTRUCTOR",
      )}
    </span>
  )
}

function InstructorDeadlineBadge({
  state,
  minutesRemaining,
}: {
  state: FacultyDeadlineState
  minutesRemaining: number | null
}) {
  const value =
    formatMinutesRemaining(
      minutesRemaining,
    )

  if (
    state === "COMPLETED"
  ) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">
        <CheckCircle2 className="size-3" />
        Completed
      </span>
    )
  }

  if (
    state === "SUBMITTED"
    || state === "IN_REVIEW"
  ) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-[10px] font-bold text-blue-700">
        <Clock3 className="size-3" />
        In Review
      </span>
    )
  }

  if (
    state === "NOT_CONFIGURED"
  ) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-[10px] font-bold text-slate-600">
        <CalendarClock className="size-3" />
        No Deadline
      </span>
    )
  }

  if (
    state === "OVERDUE"
  ) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full border border-rose-200 bg-rose-50 px-2.5 py-1 text-[10px] font-bold text-rose-700">
        <AlertTriangle className="size-3" />
        Overdue {value}
      </span>
    )
  }

  if (
    state === "DUE_TODAY"
  ) {
    return (
      <span className="inline-flex rounded-full border border-rose-200 bg-rose-50 px-2.5 py-1 text-[10px] font-bold text-rose-700">
        Due Today
      </span>
    )
  }

  if (
    state === "DUE_SOON"
  ) {
    return (
      <span className="inline-flex rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-[10px] font-bold text-amber-700">
        {value} remaining
      </span>
    )
  }

  return (
    <span className="inline-flex rounded-full border border-slate-200 bg-white px-2.5 py-1 text-[10px] font-bold text-slate-600">
      {value} remaining
    </span>
  )
}

function getInstructorAssignmentAction(
  item: FacultyCourseAssignment,
  basePath: string,
) {
  const labelByAction:
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
      label:
        labelByAction.CREATE,
      path:
        `${basePath}/create`
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
        labelByAction[
          item.recommendedAction
        ],
      path:
        `${basePath}/${item.syllabusId}/editor`,
    }
  }

  if (
    item.recommendedAction
      === "VIEW_HISTORY"
    && item.syllabusId
  ) {
    return {
      label:
        labelByAction.VIEW_HISTORY,
      path:
        `${basePath}/${item.syllabusId}#approval-history`,
    }
  }

  return {
    label:
      labelByAction[
        item.recommendedAction
      ],
    path:
      item.syllabusId
        ? `${basePath}/${item.syllabusId}`
        : basePath,
  }
}

function matchesInstructorWorkflow(
  item: FacultyCourseAssignment,
  filter: InstructorWorkflowFilter,
) {
  if (filter === "ALL") {
    return true
  }

  const status =
    normalize(
      item.status,
    ).toUpperCase()

  if (
    filter === "NOT_STARTED"
  ) {
    return (
      !item.syllabusId
      || status === "NOT_CREATED"
    )
  }

  if (
    filter === "DRAFT_REVISION"
  ) {
    return (
      status === "DRAFT"
      || status === "REJECTED"
      || status
        === "REVISION_REQUESTED"
      || item.recommendedAction
        === "REVISE"
    )
  }

  if (
    filter === "IN_REVIEW"
  ) {
    return [
      "SUBMITTED",
      "UNDER_REVIEW",
    ].includes(status)
  }

  return status === "APPROVED"
}

function instructorAssignmentPriority(
  item: FacultyCourseAssignment,
) {
  if (
    item.deadlineState
      === "OVERDUE"
  ) {
    return 0
  }

  if (
    item.recommendedAction
      === "REVISE"
  ) {
    return 1
  }

  if (
    item.deadlineState
      === "DUE_TODAY"
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
    normalize(
      item.status,
    ).toUpperCase()
      === "UNDER_REVIEW"
  ) {
    return 5
  }

  if (
    normalize(
      item.status,
    ).toUpperCase()
      === "SUBMITTED"
  ) {
    return 6
  }

  if (
    normalize(
      item.status,
    ).toUpperCase()
      === "APPROVED"
  ) {
    return 7
  }

  return 8
}

function formatMinutesRemaining(
  minutes: number | null,
) {
  if (
    minutes === null
  ) {
    return "—"
  }

  const absolute =
    Math.abs(minutes)

  const days =
    Math.floor(
      absolute / 1440,
    )

  const hours =
    Math.floor(
      (
        absolute % 1440
      ) / 60,
    )

  const mins =
    absolute % 60

  if (days > 0) {
    return hours > 0
      ? `${days}d ${hours}h`
      : `${days}d`
  }

  if (hours > 0) {
    return mins > 0
      ? `${hours}h ${mins}m`
      : `${hours}h`
  }

  return `${mins}m`
}

function formatFullDateTime(
  value: string,
) {
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

function PlusOrArrow({
  create,
}: {
  create: boolean
}) {
  return create
    ? <Plus className="size-3.5" />
    : <Eye className="size-3.5" />
}

function BadgeLike({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-[10px] font-semibold text-slate-600">
      {children}
    </span>
  )
}

function SummaryCard({
  label,
  value,
  tone = "default",
}: {
  label: string
  value: number
  tone?:
    | "default"
    | "success"
    | "info"
    | "warning"
}) {
  const toneClass =
    tone === "success"
      ? "border-emerald-200 bg-emerald-50/40"
      : tone === "info"
        ? "border-blue-200 bg-blue-50/40"
        : tone === "warning"
          ? "border-amber-200 bg-amber-50/40"
          : "border-slate-200 bg-white"

  return (
    <div className={`rounded-xl border p-4 shadow-sm ${toneClass}`}>
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-2xl font-bold text-slate-900">
        {value}
      </p>
    </div>
  )
}