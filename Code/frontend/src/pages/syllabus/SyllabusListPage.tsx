import { useEffect, useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import {
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom"
import {
  AlertTriangle,
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
import { getMyActiveAssignments } from "@/api/classSectionApi"
import { dashboardApi } from "@/api/dashboardApi"

import { courseProgramApi } from "@/api/courseProgramApi"
import { programApi } from "@/api/programApi"
import { syllabusPdfApi, syllabusWordApi } from "@/api/syllabusPdfApi"
import { syllabusApi } from "@/api/syllabusApi"
import { syllabusImportApi } from "@/api/syllabusImportApi"
import AddSyllabusDialog from "@/components/syllabus/AddSyllabusDialog"
import CloneSyllabusDialog from "@/components/syllabus/CloneSyllabusDialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
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
import { saveSyllabusImportDraft } from "@/lib/syllabusImportDraft"
import {
  SYLLABUS_CANONICAL_STATUSES,
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"
import { useAuthStore } from "@/store/authStore"
import type {
  Cohort,
  Major,
  Program,
} from "@/types/admin"
import type { SubmissionValidationIssue, SubmissionValidationResponse, Syllabus } from "@/types/syllabus"
import type { BulkSyllabusImportPreviewResponse } from "@/types/syllabusImport"

type CourseProgram = Record<string, any>

type CourseProgramIndex = {
  bySyllabusId: Map<number, CourseProgram[]>
  byCourseId: Map<number, CourseProgram[]>
}

type CatalogRow = {
  item: Syllabus
  programCodes: string[]
  cohortNames: string[]
  majorCodes: string[]
  semesterValues: string[]
  programLabels: string[]
  matchesCurriculumContext: boolean
  displayProgram: string
  displayCohort: string
  displayMajor: string
  displaySemester: string
}

const ALL = "all"

const normalizeImportCourseCode = (value?: string) => String(value ?? "")
  .replace(/[^A-Z0-9]/gi, "")
  .toUpperCase()
  .replace(/IU$/, "")

type BulkImportProgressItem = {
  code: string
  name: string
  status: "pending" | "importing" | "success" | "error"
  error?: string
}

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

const getCourseProgramSyllabusId = (
  courseProgram: CourseProgram,
) =>
  toNumber(
    courseProgram.syllabusId
    ?? courseProgram.syllabus?.id,
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
    text.match(/^(?:(?:semester|hk)\s*)?([1-8])$/i)

  if (matched) {
    return matched[1]
  }

  return ""
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

const baseProgramCode = (value: unknown) =>
  normalize(value)
    .replace(/[-_\s]?20\d{2}$/i, "")
    .replace(/[-_\s]+$/g, "")
    .trim()

const getCohortOptionLabel = (
  cohort: Cohort,
) => normalize(cohort.name) || `CS${cohort.entryYear}`

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

function SummaryCard({
  label,
  value,
  tone = "default",
}: {
  label: string
  value: number
  tone?: "default" | "success" | "info" | "warning"
}) {
  const toneClass = tone === "success"
    ? "border-emerald-200 bg-emerald-50/40"
    : tone === "info"
      ? "border-blue-200 bg-blue-50/40"
      : tone === "warning"
        ? "border-amber-200 bg-amber-50/40"
        : "border-slate-200 bg-white"

  return (
    <div className={`rounded-xl border p-4 shadow-sm ${toneClass}`}>
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
    </div>
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
) => index.bySyllabusId.get(item.id) ?? []

export default function SyllabusListPage() {
  const {
    data,
    isLoading,
    isError,
    refetch: refetchSyllabuses,
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

  const isInstructor = role === "INSTRUCTOR"

  const isAdmin = role === "ADMIN"

  const isSystemAdmin = role === "ADMIN"

  const isDean = false

  const isDeptHead = role === "DEPT_HEAD"

  const {
    data: facultyDashboard,
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

  const { data: ownActiveAssignments = [] } = useQuery({
    queryKey: ["my-active-assignments", "add-syllabus"],
    queryFn: getMyActiveAssignments,
    enabled: isInstructor,
    staleTime: 30_000,
  })

  const instructorAvailableAssignments = useMemo(
    () => ownActiveAssignments.filter((assignment) => assignment.readyForSyllabusCreation),
    [ownActiveAssignments],
  )

  const instructorAssignedCourseIds = useMemo(
    () => Array.from(new Set(instructorAvailableAssignments.map((assignment) => assignment.courseId))),
    [instructorAvailableAssignments],
  )

  const singleInstructorAssignmentId = instructorAvailableAssignments.length === 1
    ? instructorAvailableAssignments[0].id
    : undefined

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

  const contextCourseId = toNumber(searchParams.get("courseId"))
  const contextAssignmentId = toNumber(searchParams.get("assignmentId"))

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
    addDialogOpen,
    setAddDialogOpen,
  ] = useState(false)

  const [bulkImportProgress, setBulkImportProgress] = useState<BulkImportProgressItem[]>([])
  const [bulkImportRunning, setBulkImportRunning] = useState(false)
  const [deleteAllOpen, setDeleteAllOpen] = useState(false)
  const [deleteAllConfirmation, setDeleteAllConfirmation] = useState("")
  const [deleteAllRunning, setDeleteAllRunning] = useState(false)
  const [deleteAllError, setDeleteAllError] = useState("")
  const [submissionValidation, setSubmissionValidation] = useState<SubmissionValidationResponse | null>(null)

  const [
    downloadingPdfId,
    setDownloadingPdfId,
  ] = useState<
    number | null
  >(null)
  const [downloadingWordId, setDownloadingWordId] = useState<number | null>(null)

  const deleteMutation =
    useDeleteSyllabus()

  const submitMutation =
    useSubmitSyllabus()

  const handleDeleteAllSyllabi = async () => {
    if (deleteAllConfirmation !== "DELETE ALL") return
    setDeleteAllRunning(true)
    setDeleteAllError("")
    try {
      const deletedCount = await syllabusApi.deleteAll()
      setCoursePrograms((current) => current.map((item) => ({
        ...item,
        syllabusId: null,
        syllabus: null,
      })))
      await refetchSyllabuses()
      setDeleteAllOpen(false)
      setDeleteAllConfirmation("")
      window.alert(`${deletedCount} syllabus record(s) were deleted.`)
    } catch (error: any) {
      setDeleteAllError(error?.response?.data?.message || "Unable to delete all syllabuses.")
    } finally {
      setDeleteAllRunning(false)
    }
  }

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

  const runBulkImport = async (response: BulkSyllabusImportPreviewResponse) => {
    if (!selectedProgramId || !selectedCohortId || bulkImportRunning) return

    const progress = response.items.map((item) => ({
      code: item.preview.data?.sourceCourseCode || "Unknown",
      name: item.preview.data?.sourceCourseName || "Unnamed syllabus",
      status: "pending" as const,
    }))
    setBulkImportProgress(progress)
    setBulkImportRunning(true)

    for (let index = 0; index < response.items.length; index++) {
      const item = response.items[index]
      setBulkImportProgress((current) => current.map((row, rowIndex) =>
        rowIndex === index ? { ...row, status: "importing", error: undefined } : row))

      try {
        if (!item.preview.valid || !item.preview.data) {
          throw new Error(item.preview.issues?.map((issue) => issue.message).join("; ") || "Extracted data is incomplete")
        }

        const sourceCode = normalizeImportCourseCode(item.preview.data.sourceCourseCode)
        const mappings = coursePrograms
          .filter((mapping) =>
            normalizeImportCourseCode(mapping.courseCode) === sourceCode
            &&
            Number(mapping.programId) === selectedProgramId
            && (mapping.cohortId == null || Number(mapping.cohortId) === selectedCohortId)
          )
          .sort((left, right) => Number(right.cohortId === selectedCohortId) - Number(left.cohortId === selectedCohortId))
        const mapping = mappings[0]
        const targetAssignment = isInstructor
          ? instructorAvailableAssignments.find((assignment) =>
              mapping != null && assignment.courseId === Number(mapping.courseId))
          : undefined
        await syllabusImportApi.confirmBulkItem({
          assignmentId: targetAssignment?.id,
          sourceSnapshotId: item.sourceSnapshotId,
          programId: selectedProgramId,
          cohortId: selectedCohortId,
          data: item.preview.data,
          importMode: "CREATE",
          originalFileName: response.fileName,
          originalFileType: response.sourceType === "DOCX"
            ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            : "application/pdf",
        })

        const [, refreshedMappings] = await Promise.all([
          refetchSyllabuses(),
          courseProgramApi.getAll(),
        ])
        setCoursePrograms(toArray<CourseProgram>(refreshedMappings))
        setBulkImportProgress((current) => current.map((row, rowIndex) =>
          rowIndex === index ? { ...row, status: "success", error: undefined } : row))
      } catch (error) {
        const serverMessage = (error as { response?: { data?: { message?: string; error?: string } } })
          ?.response?.data
        const message = serverMessage?.message
          || serverMessage?.error
          || (error instanceof Error ? error.message : "Import failed")
        setBulkImportProgress((current) => current.map((row, rowIndex) =>
          rowIndex === index ? { ...row, status: "error", error: message } : row))
      }
    }

    setBulkImportRunning(false)
  }

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

    if (selectedCohortId === undefined && searchParams.get("cohort")) {
      const legacyCohort = cohorts.find((cohort) =>
        normalizeKey(cohort.name) === normalizeKey(searchParams.get("cohort")),
      )
      if (legacyCohort) {
        params.set("cohortId", String(legacyCohort.id))
        params.delete("cohort")
        changed = true
      }
    }

    if (selectedProgramId === undefined) {
      const programCode = searchParams.get("programCode")

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

    if (selectedProgramId !== undefined) {
      const selectedProgram = programById.get(selectedProgramId)
      const currentMajorCode = normalize(searchParams.get("majorCode"))

      if (selectedProgram && normalizeKey(currentMajorCode) !== normalizeKey(selectedProgram.majorCode)) {
        params.set("programCode", selectedProgram.code)
        params.set("majorId", String(selectedProgram.majorId))
        params.set("majorCode", selectedProgram.majorCode)
        params.delete("major")
        changed = true
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
        const bySyllabusId =
          new Map<
            number,
            CourseProgram[]
          >()
        const byCourseId = new Map<number, CourseProgram[]>()

        coursePrograms.forEach(
          (courseProgram) => {
            const courseId = toNumber(courseProgram.courseId ?? courseProgram.course?.id)
            if (courseId !== undefined) {
              byCourseId.set(courseId, [...(byCourseId.get(courseId) ?? []), courseProgram])
            }
            const syllabusId =
              getCourseProgramSyllabusId(courseProgram)

            if (syllabusId !== undefined) {
              const current =
                bySyllabusId.get(
                  syllabusId,
                )
                ?? []

              current.push(
                courseProgram,
              )

              bySyllabusId.set(
                syllabusId,
                current,
              )
            }
          },
        )

        return {
          bySyllabusId,
          byCourseId,
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

  const cohortOptions =
    useMemo(() => {
      return cohorts
        .filter(
          (cohort) =>
            (selectedProgramId === undefined
              || cohort.programId === selectedProgramId),
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
      return SYLLABUS_SEMESTER_OPTIONS
    }, [])

  const statusOptions =
    useMemo(() => {
      return uniqueSort([
        ...SYLLABUS_CANONICAL_STATUSES,
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

            const programLabels = uniqueSort(displaySource.map((courseProgram) => {
              const programId = getCourseProgramProgramId(courseProgram)
              const program = programId !== undefined ? programById.get(programId) : undefined
              const code = baseProgramCode(getCourseProgramProgramCode(courseProgram) || program?.code)
              const name = normalize(program?.nameVn || program?.name || courseProgram.programName)
              return code && name ? `${code} — ${name}` : code || name
            }))

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

            // An existing syllabus owns the editable semester value. Curriculum
            // mapping data is only a fallback for records without that value.
            const semesterValues = syllabusSemester
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
              programLabels,

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
                programLabels[0]
                || programCodes[0]
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
              item.versionLabel,
              `v${item.versionNumber}`,
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

  const catalogRows = useMemo(() => {
    const grouped = new Map<string, CatalogRow>()
    for (const row of filteredData) {
      const courseId = getSyllabusCourseId(row.item)
      const courseKey = courseId !== undefined ? `id:${courseId}` : `code:${normalizeKey(row.item.courseCode)}`
      const key = `${courseKey}|program:${normalizeKey(row.displayProgram)}|cohort:${normalizeKey(row.displayCohort)}`
      const current = grouped.get(key)
      const shouldReplace = !current
        || Boolean(row.item.isCurrent) && !current.item.isCurrent
        || Boolean(row.item.isCurrent) === Boolean(current.item.isCurrent)
          && Number(row.item.versionNumber ?? 0) > Number(current.item.versionNumber ?? 0)
      if (shouldReplace) grouped.set(key, row)
    }
    return Array.from(grouped.values())
  }, [filteredData])

  const totalCourses = useMemo(() => new Set(enrichedData.map((row) => {
    const courseId = getSyllabusCourseId(row.item)
    const courseKey = courseId !== undefined ? `id:${courseId}` : `code:${normalizeKey(row.item.courseCode)}`
    return `${courseKey}|program:${normalizeKey(row.displayProgram)}|cohort:${normalizeKey(row.displayCohort)}`
  })).size, [enrichedData])

  const summary =
    useMemo(() => {
      const rows =
        catalogRows

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
      catalogRows,
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
      ["DRAFT", "REVISION_REQUESTED"].includes(
        normalize(item.status).toUpperCase(),
      )
      && (
        isAdmin
        || (
          isInstructor
          && Boolean(facultyAssignmentForSyllabus(item))
        )
      )

  const canSubmit =
    (
      item: Syllabus,
    ) =>
      (isAdmin && ["DRAFT", "REVISION_REQUESTED"].includes(normalize(item.status).toUpperCase()))
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

        onError: (error: any) => {
          const response = error?.response?.data as SubmissionValidationResponse | undefined
          if (error?.response?.status === 422 && Array.isArray(response?.issues)) {
            setSubmissionValidation(response)
            return
          }
          alert(response?.message || "Unable to submit the syllabus.")
        },
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

      // Preserve the canonical Catalog filter context in the URL so the Map
      // receives the identical program/cohort/semester/status after navigation
      // and after a browser refresh. Search intentionally remains Catalog-only.
      const params = new URLSearchParams(searchParams)
      params.delete("q")

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
          status === "APPROVED" ? "Official" : "Preview",
        ].join("_")
        + ".pdf"

      try {
        setDownloadingPdfId(
          item.id,
        )

        if (status === "APPROVED") {
          await syllabusPdfApi.download(item.id, fallbackName)
        } else {
          await syllabusPdfApi.downloadPreview(item.id, fallbackName)
        }
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

  const handleDownloadWord = async (item: Syllabus, original = false) => {
    if (downloadingWordId !== null) return
    try {
      setDownloadingWordId(item.id)
      if (original) await syllabusWordApi.downloadOriginal(item.id)
      else await syllabusWordApi.downloadCurrent(item.id)
    } catch (error: any) {
      const blob = error?.response?.data
      let message = "Word export is available for syllabuses imported from a DOCX source."
      if (blob instanceof Blob) {
        try { message = JSON.parse(await blob.text())?.message || message } catch { /* keep fallback */ }
      }
      alert(message)
    } finally {
      setDownloadingWordId(null)
    }
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
      ? "Managed Major Syllabus Oversight"
      : isDean
        ? "Syllabus Oversight"
        : isAdmin
          ? "Syllabus Administration"
          : "My Syllabi"

  const pageDescription =
    isDeptHead
      ? "Review syllabus versions within your managed Major, track the Department Head and Dean workflow, inspect review history, and access approved official PDFs."
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
              {isSystemAdmin && (
                <Button
                  type="button"
                  variant="outline"
                  className="border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                  disabled={!data?.length || deleteAllRunning}
                  onClick={() => {
                    setDeleteAllConfirmation("")
                    setDeleteAllError("")
                    setDeleteAllOpen(true)
                  }}
                >
                  <Trash2 className="size-4" />
                  Delete All Syllabi
                </Button>
              )}
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
                  title={!selectedCohortId ? "Select a Cohort before adding a syllabus." : undefined}
                  onClick={() => {
                    if (!selectedCohortId || !selectedProgramId) {
                      alert("Please select a Cohort before adding or importing a syllabus.")
                      return
                    }
                    setAddDialogOpen(true)
                  }}
                >
                  <Plus className="size-4" />
                  Add Syllabus
                </Button>
              )}
            </div>
          </div>
        </section>

        {isDeptHead && (
          <section className="flex flex-col gap-3 rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#007d84]">
                Managed Major Scope
              </p>

              <p className="mt-1 text-sm font-semibold text-[#17343d]">
                {summary.total} visible syllabus version(s) across {summary.visibleCourses} department course(s)
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Access is restricted to the Major assigned to this Head account in User Management. Curriculum filters refine this authorized data; they do not expand access outside that Major.
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
                  Filter by Major, Cohort, Semester, and workflow status. Curriculum Program context is retained automatically when you arrive from View Syllabi.
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
                placeholder="Search by course code, course name, version, or creator..."
                value={searchValue}
                onChange={(event) =>
                  updateFilter(
                    "q",
                    event.target.value,
                  )
                }
              />
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
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
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Cohort" />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value={ALL}>
                      All Cohorts
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
                {catalogRows.length}
              </span>
              {" "}of{" "}
              <span className="font-semibold text-slate-700">
                {totalCourses}
              </span>
              {" "}courses.
            </p>
          </div>
        </section>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1470px] text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
                <tr>
                  <th className="w-14 px-4 py-3 text-center">
                    No.
                  </th>

                  <th className="px-4 py-3 text-left">
                    Course
                  </th>

                  <th className="px-4 py-3 text-left">
                    Version
                  </th>

                  <th className="px-4 py-3 text-left">
                    Program
                  </th>

                  <th className="px-4 py-3 text-left">
                    Cohort
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
                {catalogRows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={10}
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
                  catalogRows.map(
                    ({
                      item,
                      displayCohort,
                      displayMajor,
                      displayProgram,
                      displaySemester,
                    }, index) => {
                      const itemStatus =
                        normalize(
                          item.status,
                        ).toUpperCase()

                      return (
                        <tr
                          key={item.id}
                          className="transition-colors hover:bg-[#f8fbfb]"
                        >
                          <td className="w-14 px-4 py-4 text-center font-mono text-xs font-semibold text-slate-500">
                            {index + 1}
                          </td>

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
                              {item.versionLabel
                                || `v${item.versionNumber}`}
                            </span>

                            {item.isCurrent && (
                              <span className="ml-1.5 rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                                Current
                              </span>
                            )}
                          </td>

                          <td className="px-4 py-4">
                            <span className="inline-flex max-w-[300px] rounded-full border border-sky-200 bg-sky-50 px-2.5 py-1 text-xs font-semibold text-sky-700">
                              {displayProgram !== "N/A" ? displayProgram : displayMajor}
                            </span>
                          </td>

                          <td className="px-4 py-4">
                            <p className="font-medium text-slate-700">
                              {displayCohort}
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

                              {isDeptHead && itemStatus === "SUBMITTED" && (
                                <Button
                                  type="button"
                                  size="sm"
                                  className="h-8 gap-1.5 bg-[#007d84] px-2.5 text-white hover:bg-[#006d73]"
                                  title={`Review ${item.courseCode}`}
                                  onClick={() => navigate(`${basePath}/${item.id}`)}
                                >
                                  <ShieldCheck className="size-3.5" />
                                  <span className="text-xs">Review</span>
                                </Button>
                              )}

                              {(
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  className="h-8 gap-1.5 border-emerald-200 bg-emerald-50 px-2.5 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                                  disabled={
                                    downloadingPdfId
                                    !== null
                                  }
                                  title={itemStatus === "APPROVED" ? "Export official PDF" : "Export preview PDF"}
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
                                    Export PDF
                                  </span>
                                </Button>
                              )}

                              {String(item.sourceType ?? "").toUpperCase() === "IMPORT_DOCX" && <>
                                <Button type="button" variant="outline" size="sm"
                                  className="h-8 gap-1.5 border-blue-200 bg-blue-50 px-2.5 text-blue-700 hover:bg-blue-100"
                                  disabled={downloadingWordId !== null}
                                  title="Export Word using the imported source template"
                                  onClick={() => void handleDownloadWord(item)}>
                                  {downloadingWordId === item.id ? <LoaderCircle className="size-3.5 animate-spin" /> : <Download className="size-3.5" />}
                                  <span className="text-xs">Export Word</span>
                                </Button>
                                <Button type="button" variant="ghost" size="sm"
                                  className="h-8 px-2 text-xs text-blue-700"
                                  disabled={downloadingWordId !== null}
                                  title="Download the untouched imported syllabus snapshot"
                                  onClick={() => void handleDownloadWord(item, true)}>
                                  Original Word
                                </Button>
                              </>}

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
                                        `${basePath}/${item.id}/edit`,
                                        { state: { major: displayMajor } },
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
                                    title="Add DOCX/PDF template"
                                    onClick={() =>
                                      navigate(
                                        `${basePath}/${item.id}/edit?import=1`,
                                        { state: { major: displayMajor } },
                                      )
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
              `${basePath}/${newSyllabus.id}/edit`,
            )
          }
        />
      )}

      <Dialog
        open={submissionValidation !== null}
        onOpenChange={(open) => { if (!open) setSubmissionValidation(null) }}
      >
        <DialogContent className="max-h-[85vh] overflow-y-auto bg-white sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-amber-700"><AlertTriangle className="size-5" />Cannot Submit Syllabus</DialogTitle>
            <DialogDescription>
              {submissionValidation?.errorCount ?? 0} issues must be fixed. Open the Draft, update the indicated sections, and save before submitting again.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            {Object.entries((submissionValidation?.issues ?? []).reduce<Record<string, SubmissionValidationIssue[]>>((groups, issue) => {
              ;(groups[issue.section] ??= []).push(issue)
              return groups
            }, {})).map(([section, issues]) => (
              <section key={section} className="rounded-lg border border-amber-200 bg-amber-50/50 p-4">
                <h3 className="text-xs font-bold uppercase tracking-wide text-amber-800">{section}</h3>
                <ul className="mt-2 space-y-2 text-sm text-slate-700">
                  {issues.map((issue, index) => <li key={`${issue.code}-${issue.field}-${index}`} className="flex gap-2"><span className="text-amber-600">•</span><span>{issue.message}</span></li>)}
                </ul>
              </section>
            ))}
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setSubmissionValidation(null)}>Close</Button>
            <Button type="button" onClick={() => { const id = submissionValidation?.syllabusId; setSubmissionValidation(null); if (id) navigate(`${basePath}/${id}/edit`) }}>Open Draft</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={isSystemAdmin && deleteAllOpen}
        onOpenChange={(open) => {
          if (deleteAllRunning) return
          setDeleteAllOpen(open)
          if (!open) {
            setDeleteAllConfirmation("")
            setDeleteAllError("")
          }
        }}
      >
        <DialogContent className="border-rose-200 bg-white sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-rose-700">Delete all syllabuses?</DialogTitle>
            <DialogDescription className="leading-6">
              This permanently deletes every syllabus version and its CLOs, topics, assessments, readings, approvals, and import history. Courses, Programs, Cohorts, semesters, and curriculum relationships remain.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <label htmlFor="delete-all-syllabi-confirmation" className="text-xs font-semibold text-slate-700">
              Type <span className="font-mono text-rose-700">DELETE ALL</span> to confirm
            </label>
            <Input
              id="delete-all-syllabi-confirmation"
              value={deleteAllConfirmation}
              onChange={(event) => setDeleteAllConfirmation(event.target.value)}
              disabled={deleteAllRunning}
              autoComplete="off"
              placeholder="DELETE ALL"
            />
            {deleteAllError && <p className="text-sm text-rose-600">{deleteAllError}</p>}
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" disabled={deleteAllRunning} onClick={() => setDeleteAllOpen(false)}>Cancel</Button>
            <Button
              type="button"
              className="bg-rose-600 text-white hover:bg-rose-700"
              disabled={deleteAllConfirmation !== "DELETE ALL" || deleteAllRunning}
              onClick={handleDeleteAllSyllabi}
            >
              {deleteAllRunning ? <LoaderCircle className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
              {deleteAllRunning ? "Deleting..." : "Delete Everything"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {(isAdmin || isInstructor) && (
        <AddSyllabusDialog
          open={addDialogOpen}
          onOpenChange={setAddDialogOpen}
          scopeProgramId={selectedProgramId}
          scopeCohortId={selectedCohortId}
          allowedCourseIds={isInstructor ? instructorAssignedCourseIds : undefined}
          contextCourseId={isInstructor ? contextCourseId : undefined}
          allowedAssignments={isInstructor ? instructorAvailableAssignments : undefined}
          assignmentId={isInstructor ? (contextAssignmentId ?? singleInstructorAssignmentId) : undefined}
          onCreateRequested={({ courseId, courseProgramId, assignmentId, importPreview, previewOnly }) => {
            const importDraftId = importPreview && courseId ? saveSyllabusImportDraft(importPreview, courseId, courseProgramId) : undefined
            navigate(
              `${basePath}/create?${courseId ? `courseId=${courseId}&` : ""}${courseProgramId ? `courseProgramId=${courseProgramId}&` : ""}${assignmentId ? `assignmentId=${assignmentId}&` : ""}${selectedProgramId ? `programId=${selectedProgramId}&` : ""}${selectedCohortId ? `cohortId=${selectedCohortId}&` : ""}${importDraftId ? `importDraft=${encodeURIComponent(importDraftId)}` : ""}`.replace(/[?&]$/, ""),
              { state: importPreview ? { importPreview, targetCourseId: courseId, targetCourseProgramId: courseProgramId, previewOnly } : undefined },
            )
          }}
          onBulkImportRequested={(response) => {
            setAddDialogOpen(false)
            void runBulkImport(response)
          }}
        />
      )}

      {bulkImportProgress.length > 0 && (
        <aside className="fixed right-5 top-20 z-50 flex max-h-[calc(100vh-6rem)] w-[390px] flex-col overflow-hidden rounded-2xl border border-[#cfe2e4] bg-white shadow-2xl">
          <div className="h-1 bg-gradient-to-r from-[#007d84] via-[#20a0a5] to-[#f0a72f]" />
          <div className="flex items-start justify-between border-b px-5 py-4">
            <div>
              <h2 className="font-bold text-[#006f76]">Program Document Import</h2>
              <p className="mt-1 text-xs text-slate-500">
                {bulkImportProgress.filter((item) => item.status === "success").length} saved · {bulkImportProgress.filter((item) => item.status === "error").length} failed · {bulkImportProgress.length} total
              </p>
            </div>
            {!bulkImportRunning && <Button variant="ghost" size="sm" onClick={() => setBulkImportProgress([])}>Close</Button>}
          </div>
          <div className="overflow-y-auto p-3">
            {bulkImportProgress.map((item, index) => (
              <div key={`${item.code}-${index}`} className={`mb-2 rounded-xl border p-3 last:mb-0 ${item.status === "error" ? "border-rose-200 bg-rose-50" : item.status === "success" ? "border-emerald-200 bg-emerald-50" : "border-slate-200 bg-white"}`}>
                <div className="flex items-start gap-3">
                  {item.status === "importing" ? <LoaderCircle className="mt-0.5 size-4 shrink-0 animate-spin text-[#007d84]" />
                    : item.status === "success" ? <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" />
                    : item.status === "error" ? <AlertTriangle className="mt-0.5 size-4 shrink-0 text-rose-600" />
                    : <Clock3 className="mt-0.5 size-4 shrink-0 text-slate-400" />}
                  <div className="min-w-0">
                    <p className="font-mono text-xs font-bold text-slate-800">{item.code}</p>
                    <p className="truncate text-xs text-slate-500">{item.name}</p>
                    {item.error && <p className="mt-1 text-xs leading-5 text-rose-700">{item.error}</p>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </aside>
      )}
    </div>
  )
}
