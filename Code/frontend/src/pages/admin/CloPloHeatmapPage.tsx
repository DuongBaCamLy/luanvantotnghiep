import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"
import {
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import {
  AlertTriangle,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  FileSpreadsheet,
  FileText,
  Grid3X3,
  Info,
  Search,
  Target,
  XCircle,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { courseTypeApi } from "@/api/courseTypeApi"
import {
  dashboardApi,
  type HeatmapCellCoverage,
  type HeatmapCourseCoverage,
  type HeatmapPloColumn,
  type HeatmapWarning,
} from "@/api/dashboardApi"
import { programApi } from "@/api/programApi"
import { reportApi } from "@/api/reportApi"
import {
  formatSyllabusFilterStatus,
  SYLLABUS_CANONICAL_STATUSES,
  SYLLABUS_FILTER_ALL,
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { useSyllabuses } from "@/hooks/useSyllabuses"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

type CoverageLevel = "X" | "XX" | "XXX"

type SelectedCell = {
  course: HeatmapCourseCoverage
  plo: HeatmapPloColumn
  cell: HeatmapCellCoverage
}

type SelectedPlo = {
  plo: HeatmapPloColumn
  introduceCourses: number
  developCourses: number
  achieveCourses: number
}

type IssueSection = {
  key: string
  title: string
  description: string
  count: number
  references: string[]
  tone: "warning" | "danger" | "info"
}

type PageReadiness =
  | "NO_PLO"
  | "NO_COURSE"
  | "NO_APPROVED"
  | "NO_CLO"
  | "NO_MAPPING"
  | "READY"

function getBaseProgramCode(
  value: unknown,
): string {
  const code =
    String(value ?? "")
      .trim()

  if (!code) {
    return "—"
  }

  return (
    code
      .replace(
        /[-_\s]?20\d{2}$/i,
        "",
      )
      .replace(
        /[-_\s]+$/g,
        "",
      )
      .trim()
    || code
  )
}

function getErrorMessage(error: unknown): string {
  const responseError = error as {
    response?: {
      data?: {
        message?: string
      }
    }
    message?: string
  }

  return (
    responseError.response?.data?.message
    || responseError.message
    || "Unable to load CLO–PLO coverage data."
  )
}

function hasMojibake(
  value?: string | null,
): boolean {
  if (!value) return false

  return /(?:Ã|Â|â|�|├|┤|┬|┴|┼|╔|╗|╚|╝|║|═|ß)/.test(
    value,
  )
}

function getPloDisplayDescription(
  plo: Pick<
    HeatmapPloColumn,
    "description" | "descriptionVn"
  >,
): string {
  const vietnamese =
    plo.descriptionVn?.trim()

  if (
    vietnamese
    && !hasMojibake(vietnamese)
  ) {
    return vietnamese
  }

  return (
    plo.description?.trim()
    || vietnamese
    || "No PLO description"
  )
}

function levelLabel(
  level: HeatmapCellCoverage["level"],
): string {
  if (level === "X") return "X · Low contribution"
  if (level === "XX") return "XX · Medium contribution"
  if (level === "XXX") return "XXX · High contribution"
  return "Not mapped"
}

function levelShortLabel(
  level: HeatmapCellCoverage["level"],
): string {
  if (level === "X") return "X"
  if (level === "XX") return "XX"
  if (level === "XXX") return "XXX"
  return "—"
}

function levelClass(
  level: HeatmapCellCoverage["level"],
): string {
  if (level === "X") {
    return "border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-100"
  }

  if (level === "XX") {
    return "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100"
  }

  if (level === "XXX") {
    return "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
  }

  return "border-slate-200 bg-slate-50 text-slate-400"
}

function issueToneClass(
  tone: IssueSection["tone"],
): string {
  if (tone === "danger") {
    return "border-rose-200 bg-rose-50/70"
  }

  if (tone === "warning") {
    return "border-amber-200 bg-amber-50/70"
  }

  return "border-blue-200 bg-blue-50/70"
}

function issueTextClass(
  tone: IssueSection["tone"],
): string {
  if (tone === "danger") return "text-rose-800"
  if (tone === "warning") return "text-amber-800"
  return "text-blue-800"
}

function isDuplicateBusinessWarning(
  warning: HeatmapWarning,
  {
    hasUncoveredPlo,
    hasBlockedCourses,
    hasUnmappedClos,
  }: {
    hasUncoveredPlo: boolean
    hasBlockedCourses: boolean
    hasUnmappedClos: boolean
  },
): boolean {
  const knownCodes = new Set([
    "UNCOVERED_PLO",
    "UNCOVERED_PLOS",
    "MISSING_APPROVED_SYLLABUS",
    "COURSES_WITHOUT_APPROVED_SYLLABUS",
    "UNMAPPED_CLO",
    "UNMAPPED_CLOS",
    "NO_APPROVED_SYLLABUS",
    "NO_APPROVED_SYLLABUSES",
    "NO_APPROVED_SYLLABUS_IN_SCOPE",
    "PARTIAL_APPROVED_SYLLABUS_COVERAGE",
    "NO_CLO_PLO_MAPPINGS",
    "PARTIAL_CLO_MAPPING_COVERAGE",
  ])

  if (knownCodes.has(warning.code)) {
    return true
  }

  const message = warning.message.toUpperCase()

  if (
    hasBlockedCourses
    && message.includes("APPROVED")
    && (
      message.includes("SYLLABUS")
      || message.includes("ĐỀ CƯƠNG")
    )
  ) {
    return true
  }

  if (
    hasUncoveredPlo
    && message.includes("PLO")
    && (
      message.includes("UNCOVER")
      || message.includes("BAO PHỦ")
      || message.includes("CHƯA")
    )
  ) {
    return true
  }

  if (
    hasUnmappedClos
    && message.includes("CLO")
    && (
      message.includes("MAPPING")
      || message.includes("MAP")
      || message.includes("LIÊN KẾT")
    )
  ) {
    return true
  }

  return false
}

function downloadBlob(
  data: BlobPart,
  contentType: string,
  filename: string,
) {
  const blob =
    data instanceof Blob
      ? data
      : new Blob(
          [data],
          { type: contentType },
        )

  const url = URL.createObjectURL(blob)
  const anchor = document.createElement("a")

  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

function SummaryCard({
  title,
  value,
  description,
  icon,
  tone = "default",
}: {
  title: string
  value: string | number
  description: string
  icon: ReactNode
  tone?: "default" | "success" | "warning" | "danger"
}) {
  const cardClass =
    tone === "success"
      ? "border-emerald-200 bg-emerald-50/40"
      : tone === "warning"
        ? "border-amber-200 bg-amber-50/40"
        : tone === "danger"
          ? "border-rose-200 bg-rose-50/40"
          : "border-slate-200 bg-white"

  const iconClass =
    tone === "success"
      ? "bg-emerald-100 text-emerald-700"
      : tone === "warning"
        ? "bg-amber-100 text-amber-700"
        : tone === "danger"
          ? "bg-rose-100 text-rose-700"
          : "bg-[#eef8f8] text-[#007d84]"

  return (
    <Card className={`${cardClass} shadow-sm`}>
      <CardContent className="flex h-full items-start justify-between p-4">
        <div className="min-w-0">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
            {title}
          </p>

          <p className="mt-2 text-2xl font-bold text-slate-900">
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>

        <div className={`ml-3 rounded-xl p-3 ${iconClass}`}>
          {icon}
        </div>
      </CardContent>
    </Card>
  )
}

function IssueList({
  section,
}: {
  section: IssueSection
}) {
  const [expanded, setExpanded] = useState(false)
  const preview = section.references.slice(0, 10)
  const visibleReferences =
    expanded
      ? section.references
      : preview

  return (
    <div
      className={`rounded-xl border p-4 ${issueToneClass(section.tone)}`}
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <AlertTriangle
              className={`size-4 shrink-0 ${issueTextClass(section.tone)}`}
            />

            <p className={`font-semibold ${issueTextClass(section.tone)}`}>
              {section.title}
            </p>

            <Badge
              variant="outline"
              className="border-white/80 bg-white/80 text-slate-700"
            >
              {section.count}
            </Badge>
          </div>

          <p className="mt-1 text-xs leading-5 text-slate-600">
            {section.description}
          </p>
        </div>

        {section.references.length > 0 && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-8 shrink-0 text-slate-600"
            onClick={() =>
              setExpanded(
                (value) => !value,
              )
            }
          >
            {expanded
              ? <ChevronUp className="size-4" />
              : <ChevronDown className="size-4" />}

            {expanded
              ? "Hide details"
              : "View details"}
          </Button>
        )}
      </div>

      {visibleReferences.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {visibleReferences.map(
            (reference) => (
              <Badge
                key={`${section.key}-${reference}`}
                variant="outline"
                className="border-slate-200 bg-white text-slate-600"
              >
                {reference}
              </Badge>
            ),
          )}

          {!expanded
          && section.references.length > preview.length && (
            <Badge
              variant="outline"
              className="border-slate-200 bg-white text-slate-500"
            >
              +{section.references.length - preview.length} more
            </Badge>
          )}
        </div>
      )}
    </div>
  )
}

function CoverageLevelBadge({
  level,
  count,
}: {
  level: CoverageLevel
  count: number
}) {
  return (
    <Badge
      variant="outline"
      className={`${levelClass(level)} gap-1`}
    >
      {level}
      <span className="font-normal opacity-70">
        {count}
      </span>
    </Badge>
  )
}


function IssueSummaryGroup({
  title,
  description,
  count,
  tone,
  items,
}: {
  title: string
  description: string
  count: number
  tone: "warning" | "danger"
  items: Array<{
    label: string
    count: number
  }>
}) {
  const toneClass =
    tone === "danger"
      ? "border-rose-200 bg-rose-50/60"
      : "border-amber-200 bg-amber-50/60"

  const titleClass =
    tone === "danger"
      ? "text-rose-800"
      : "text-amber-800"

  return (
    <div className={`rounded-xl border p-4 ${toneClass}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className={`font-semibold ${titleClass}`}>
            {title}
          </p>
          <p className="mt-1 text-xs leading-5 text-slate-600">
            {description}
          </p>
        </div>

        <Badge
          variant="outline"
          className="border-white/80 bg-white/80 text-slate-700"
        >
          {count}
        </Badge>
      </div>

      {items.length > 0 ? (
        <div className="mt-3 space-y-2">
          {items.slice(0, 4).map(
            (item, index) => (
              <div
                key={`${item.label}-${index}`}
                className="flex items-start justify-between gap-3 rounded-lg border border-white/70 bg-white/70 px-3 py-2 text-xs"
              >
                <span className="line-clamp-2 text-slate-600">
                  {item.label}
                </span>
                <span className="shrink-0 font-semibold text-slate-700">
                  {item.count}
                </span>
              </div>
            ),
          )}

          {items.length > 4 && (
            <p className="text-[10px] text-slate-500">
              +{items.length - 4} more issue groups
            </p>
          )}
        </div>
      ) : (
        <p className="mt-3 text-xs font-medium text-emerald-700">
          No issues in this group.
        </p>
      )}
    </div>
  )
}

function AdditionalWarning({
  warning,
}: {
  warning: HeatmapWarning
}) {
  return (
    <div className="rounded-xl border border-blue-200 bg-blue-50/70 p-4">
      <div className="flex items-start gap-2">
        <Info className="mt-0.5 size-4 shrink-0 text-blue-700" />

        <div>
          <p className="font-semibold text-blue-800">
            {warning.message}
          </p>

          {warning.references.length > 0 && (
            <p className="mt-1 text-xs leading-5 text-blue-700">
              {warning.references
                .slice(0, 12)
                .join(", ")}
              {warning.references.length > 12
                ? ` +${warning.references.length - 12} more`
                : ""}
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

export function LiveCloPloHeatmapPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] =
    useSearchParams()

  const [programId, setProgramId] =
    useState(
      searchParams.get("programId")
      || "",
    )

  const [cohortId, setCohortId] =
    useState(
      searchParams.get("cohortId")
      || "",
    )

  const [courseSearch, setCourseSearch] =
    useState(searchParams.get("search") || "")

  const [majorCode, setMajorCode] =
    useState(searchParams.get("majorCode") || "")

  const [semesterFilter, setSemesterFilter] =
    useState(searchParams.get("semester") || SYLLABUS_FILTER_ALL)

  const [statusFilter, setStatusFilter] =
    useState(searchParams.get("status") || SYLLABUS_FILTER_ALL)

    const [courseTypeId, setCourseTypeId] =
  useState(
    searchParams.get("courseTypeId")
    || SYLLABUS_FILTER_ALL,
  )

  const [selectedCell, setSelectedCell] =
    useState<SelectedCell | null>(null)

  const [selectedPlo, setSelectedPlo] =
    useState<SelectedPlo | null>(null)

  const [showIssues, setShowIssues] =
    useState(false)

  const [exporting, setExporting] =
    useState<"excel" | "pdf" | null>(null)

  const [exportError, setExportError] =
    useState("")

  const { data: syllabusCatalogData = [] } = useSyllabuses()

  const statusOptions = useMemo(
    () => Array.from(new Set([
      ...SYLLABUS_CANONICAL_STATUSES,
      ...syllabusCatalogData
        .map((syllabus) => String(syllabus.status ?? "").trim().toUpperCase())
        .filter(Boolean),
    ])).sort(),
    [syllabusCatalogData],
  )

  const roleBase =
  location.pathname.startsWith("/dean")
    ? "/dean"
    : location.pathname.startsWith("/dept-head")
      ? "/dept-head"
      : "/admin"

  const {
    data: programs = [],
    isLoading: isLoadingPrograms,
    isError: isProgramsError,
  } = useQuery({
    queryKey: ["programs"],
    queryFn: programApi.getAll,
  })

  const {
    data: majors = [],
    isLoading: isLoadingMajors,
    isError: isMajorsError,
  } = useQuery({
    queryKey: ["majors"],
    queryFn: programApi.getMajors,
  })

  const {
  data: courseTypes = [],
  isLoading: isLoadingCourseTypes,
  isError: isCourseTypesError,
} = useQuery({
  queryKey: ["course-types"],
  queryFn: courseTypeApi.getAll,
})

const courseTypeExists =
  courseTypeId === SYLLABUS_FILTER_ALL
  || courseTypes.some(
    (type) =>
      String(type.id) === courseTypeId,
  )

const effectiveCourseTypeId =
  courseTypeExists
    ? courseTypeId
    : SYLLABUS_FILTER_ALL
  if (!majorCode && programId) {
    const inferredMajor = programs.find((program) => String(program.id) === programId)?.majorCode
    if (inferredMajor) setMajorCode(inferredMajor)
  }

  const programExists =
    programs.some(
      (program) =>
        String(program.id) === programId,
    )

  const effectiveProgramId =
    programExists
      ? programId
      : ""

  const {
    data: cohorts = [],
    isLoading: isLoadingCohorts,
    isError: isCohortsError,
  } = useQuery({
    queryKey: [
      "cohorts",
      "heatmap-filter",
    ],
    queryFn: cohortApi.getAll,
  })

  const availableCohorts =
    useMemo(
      () => {
        const matchingProgramIds = new Set(
          programs
            .filter(
              (program) =>
                !majorCode
                || program.majorCode.toLowerCase() === majorCode.toLowerCase(),
            )
            .map((program) => String(program.id)),
        )

        return cohorts
          .filter((cohort) => {
            if (effectiveProgramId) {
              return String(cohort.programId) === effectiveProgramId
            }
            return !majorCode || matchingProgramIds.has(String(cohort.programId))
          })
          .slice()
          .sort(
            (a, b) =>
              Number(b.entryYear ?? 0)
              - Number(a.entryYear ?? 0),
          )
      },
      [cohorts, effectiveProgramId, majorCode, programs],
    )

  const cohortExists =
    availableCohorts.some(
      (cohort) =>
        String(cohort.id) === cohortId,
    )

  const effectiveCohortId =
    cohortExists
      ? cohortId
      : ""

  const urlFilterSignature = searchParams.toString()
  const [appliedUrlFilters, setAppliedUrlFilters] = useState(urlFilterSignature)
  if (appliedUrlFilters !== urlFilterSignature) {
    setAppliedUrlFilters(urlFilterSignature)
    const urlProgramId =
      searchParams.get("programId") || ""
    const urlCohortId =
      searchParams.get("cohortId") || ""

    setProgramId((current) =>
      current === urlProgramId
        ? current
        : urlProgramId,
    )
    setCohortId((current) =>
      current === urlCohortId
        ? current
        : urlCohortId,
    )
    setMajorCode(searchParams.get("majorCode") || "")
    setCourseSearch(searchParams.get("search") || "")
    setSemesterFilter(searchParams.get("semester") || SYLLABUS_FILTER_ALL)
    setStatusFilter(searchParams.get("status") || SYLLABUS_FILTER_ALL)
    setCourseTypeId(
  searchParams.get("courseTypeId")
  || SYLLABUS_FILTER_ALL,
)
  }

  useEffect(() => {
    if (
      isLoadingPrograms
      || (
        Boolean(effectiveProgramId)
        && isLoadingCohorts
      )
    ) {
      return
    }

    const next =
      new URLSearchParams()

    if (effectiveProgramId) {
      next.set(
        "programId",
        effectiveProgramId,
      )
    }

    if (effectiveCohortId) {
      next.set(
        "cohortId",
        effectiveCohortId,
      )
    }

    if (majorCode) next.set("majorCode", majorCode)
    if (courseSearch.trim()) next.set("search", courseSearch.trim())
    if (semesterFilter !== SYLLABUS_FILTER_ALL) next.set("semester", semesterFilter)
    if (statusFilter !== SYLLABUS_FILTER_ALL) next.set("status", statusFilter)
if (
  effectiveCourseTypeId
  !== SYLLABUS_FILTER_ALL
) {
  next.set(
    "courseTypeId",
    effectiveCourseTypeId,
  )
}
    setSearchParams(
      next,
      { replace: true },
    )
  }, [
    effectiveProgramId,
    effectiveCourseTypeId,
    effectiveCohortId,
    majorCode,
    courseSearch,
    semesterFilter,
    statusFilter,
    isLoadingPrograms,
    isLoadingCohorts,
    setSearchParams,
  ])

  const {
    data: heatmap,
    isLoading: isLoadingHeatmap,
    isFetching: isFetchingHeatmap,
    isError: isHeatmapError,
    error: heatmapError,
  } = useQuery({
    queryKey: [
  "clo-plo-heatmap",
  effectiveProgramId,
  effectiveCohortId,
  effectiveCourseTypeId,
  courseSearch,
  semesterFilter,
  statusFilter,
],

    queryFn: () =>
      dashboardApi.getHeatmapCoverage(
        Number(effectiveProgramId),
        {
  cohortId:
    effectiveCohortId
      ? Number(effectiveCohortId)
      : undefined,

  courseTypeId:
    effectiveCourseTypeId
      === SYLLABUS_FILTER_ALL
      ? undefined
      : Number(effectiveCourseTypeId),

  search:
    courseSearch.trim()
    || undefined,

  semester:
    semesterFilter
      === SYLLABUS_FILTER_ALL
      ? undefined
      : semesterFilter,

  status:
    statusFilter
      === SYLLABUS_FILTER_ALL
      ? undefined
      : statusFilter,
},
      ),

    enabled: Boolean(
  effectiveProgramId
  && effectiveCohortId,
),
  })

  const approvedCourses =
    useMemo(
      () =>
        heatmap?.courseCoverages.filter(
          (course) =>
            course.hasApprovedSyllabus,
        ) ?? [],
      [heatmap],
    )

  const blockedCourses =
    useMemo(
      () =>
        heatmap?.courseCoverages.filter(
          (course) =>
            !course.hasApprovedSyllabus,
        ) ?? [],
      [heatmap],
    )

  const coursesWithUnmappedClos =
    useMemo(
      () =>
        approvedCourses.filter(
          (course) =>
            course.unmappedCloCodes.length
            > 0,
        ),
      [approvedCourses],
    )

  const uncoveredPloCodes =
    useMemo(
      () =>
        heatmap?.ploDetails
          .filter(
            (plo) => !plo.covered,
          )
          .map(
            (plo) => plo.code,
          ) ?? [],
      [heatmap],
    )

  const unmappedCloReferences =
    useMemo(
      () =>
        coursesWithUnmappedClos.flatMap(
          (course) =>
            course.unmappedCloCodes.map(
              (cloCode) =>
                `${course.courseCode} · ${cloCode}`,
            ),
        ),
      [coursesWithUnmappedClos],
    )

  const filteredApprovedCourses =
    useMemo(() => {
      const query =
        courseSearch
          .trim()
          .toLowerCase()

      if (!query) {
        return approvedCourses
      }

      return approvedCourses.filter(
        (course) =>
          course.courseCode
            .toLowerCase()
            .includes(query)
          || course.courseName
            .toLowerCase()
            .includes(query)
          || (
            course.courseNameVn
            ?? ""
          )
            .toLowerCase()
            .includes(query),
      )
    }, [
      approvedCourses,
      courseSearch,
    ])

  const ploProfiles =
    useMemo(() => {
      if (!heatmap) return []

      return heatmap.ploDetails.map(
        (plo) => {
          let introduceCourses = 0
          let developCourses = 0
          let achieveCourses = 0

          for (
            const course
            of approvedCourses
          ) {
            const cell =
              course.cells.find(
                (candidate) =>
                  candidate.ploId
                  === plo.id,
              )

            if (cell?.level === "X") {
              introduceCourses += 1
            }

            if (cell?.level === "XX") {
              developCourses += 1
            }

            if (cell?.level === "XXX") {
              achieveCourses += 1
            }
          }

          return {
            plo,
            introduceCourses,
            developCourses,
            achieveCourses,
          }
        },
      )
    }, [
      heatmap,
      approvedCourses,
    ])

  const issueSections =
    useMemo<IssueSection[]>(() => {
      if (!heatmap) return []

      const sections:
        IssueSection[] = []

      if (
        uncoveredPloCodes.length > 0
      ) {
        sections.push({
          key: "uncovered-plos",
          title:
            "PLOs without CLO coverage",
          description:
            "No CLO from an APPROVED syllabus covers these PLOs in the selected curriculum scope.",
          count:
            uncoveredPloCodes.length,
          references:
            uncoveredPloCodes,
          tone: "warning",
        })
      }

      if (
        blockedCourses.length > 0
      ) {
        sections.push({
          key:
            "courses-without-approved-syllabus",
          title:
            "Courses excluded from analysis",
          description:
            "These curriculum courses do not have an APPROVED syllabus linked to the selected curriculum.",
          count:
            blockedCourses.length,
          references:
            blockedCourses.map(
              (course) =>
                course.courseCode,
            ),
          tone: "danger",
        })
      }

      if (
        unmappedCloReferences.length
        > 0
      ) {
        sections.push({
          key: "unmapped-clos",
          title:
            "CLOs without PLO mappings",
          description:
            "These CLOs belong to APPROVED syllabuses but are not linked to any PLO.",
          count:
            unmappedCloReferences.length,
          references:
            unmappedCloReferences,
          tone: "warning",
        })
      }

      return sections
    }, [
      heatmap,
      uncoveredPloCodes,
      blockedCourses,
      unmappedCloReferences,
    ])

  const additionalWarnings =
    useMemo(() => {
      if (!heatmap) return []

      return heatmap.warnings.filter(
        (warning) =>
          !isDuplicateBusinessWarning(
            warning,
            {
              hasUncoveredPlo:
                uncoveredPloCodes.length > 0,
              hasBlockedCourses:
                blockedCourses.length > 0,
              hasUnmappedClos:
                unmappedCloReferences.length > 0,
            },
          ),
      )
    }, [
      heatmap,
      uncoveredPloCodes.length,
      blockedCourses.length,
      unmappedCloReferences.length,
    ])

  const coverageIssueSections =
    useMemo(
      () =>
        issueSections.filter(
          (section) =>
            section.key
            !== "courses-without-approved-syllabus"
            && (
              section.key
              !== "uncovered-plos"
              || (heatmap?.summary.totalClos ?? 0) > 0
            ),
        ),
      [
        issueSections,
        heatmap?.summary.totalClos,
      ],
    )

  const readinessIssueSections =
    useMemo(
      () =>
        issueSections.filter(
          (section) =>
            section.key
            === "courses-without-approved-syllabus",
        ),
      [issueSections],
    )

  const readinessWarnings =
    useMemo(
      () =>
        additionalWarnings.filter(
          (warning) => {
            const message =
              warning.message
                .toUpperCase()

            return (
              message.includes("APPROVED")
              || message.includes("SYLLABUS")
              || message.includes("COURSE")
            )
          },
        ),
      [additionalWarnings],
    )

  const coverageWarnings =
    useMemo(
      () =>
        additionalWarnings.filter(
          (warning) =>
            !readinessWarnings.includes(
              warning,
            ),
        ),
      [
        additionalWarnings,
        readinessWarnings,
      ],
    )

  const coverageIssueGroupCount =
    coverageIssueSections.length
    + coverageWarnings.length

  const readinessIssueGroupCount =
    readinessIssueSections.length
    + readinessWarnings.length

  const issueGroupCount =
    coverageIssueGroupCount
    + readinessIssueGroupCount

  const readiness: PageReadiness =
    useMemo(() => {
      if (!heatmap) return "READY"

      if (
        heatmap.summary.totalPlos === 0
      ) {
        return "NO_PLO"
      }

      if (
        heatmap.summary.totalCourses === 0
      ) {
        return "NO_COURSE"
      }

      if (
        heatmap.summary
          .coursesWithApprovedSyllabus
        === 0
      ) {
        return "NO_APPROVED"
      }

      if (
        heatmap.summary.totalClos === 0
      ) {
        return "NO_CLO"
      }

      if (
        heatmap.summary.mappedClos === 0
      ) {
        return "NO_MAPPING"
      }

      return "READY"
    }, [heatmap])

  const canRenderMatrix =
    readiness === "READY"

  const canExport =
  Boolean(
    effectiveProgramId
    && effectiveCohortId
    && heatmap
    && heatmap.summary.totalPlos > 0
    && heatmap.summary.totalCourses > 0,
  )

  const hasCloAnalysisData =
    Boolean(
      heatmap
      && heatmap.summary.totalPlos > 0
      && heatmap.summary.totalClos > 0,
    )

  async function exportMatrix(
    format: "excel" | "pdf",
  ) {
    if (
  !effectiveProgramId
  || !effectiveCohortId
  || !canExport
) {
  return
}

    setExporting(format)
    setExportError("")

    try {
      const scope = {
  cohortId:
    Number(effectiveCohortId),

  courseTypeId:
    effectiveCourseTypeId
      === SYLLABUS_FILTER_ALL
      ? undefined
      : Number(effectiveCourseTypeId),

  search:
    courseSearch.trim()
    || undefined,

  semester:
    semesterFilter
      === SYLLABUS_FILTER_ALL
      ? undefined
      : semesterFilter,

  status:
    statusFilter
      === SYLLABUS_FILTER_ALL
      ? undefined
      : statusFilter,
}

      const response =
        format === "excel"
          ? await reportApi
              .exportCloPloMatrixExcel(
                Number(
                  effectiveProgramId,
                ),
                scope,
              )
          : await reportApi
              .exportCloPloMatrixPdf(
                Number(
                  effectiveProgramId,
                ),
                scope,
              )

      const extension =
        format === "excel"
          ? "xlsx"
          : "pdf"

      const contentType =
        format === "excel"
          ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          : "application/pdf"

      downloadBlob(
        response.data,
        contentType,
        [
          "CLO-PLO",
          getBaseProgramCode(
            heatmap?.programCode
            ?? effectiveProgramId,
          ),
          heatmap?.cohortName
            ?? effectiveCohortId,
        ].join("_")
          + `.${extension}`,
      )
    } catch (error) {
      setExportError(
        getErrorMessage(error)
          .replace(
            "heatmap",
            "report",
          ),
      )
    } finally {
      setExporting(null)
    }
  }

  function navigateTo(
    section:
      | "syllabus"
      | "plo"
      | "programs",
  ) {
    if (section === "syllabus") {
      navigate(
        `${roleBase}/syllabus`,
      )
      return
    }

    if (section === "plo") {
      navigate(
        `${roleBase}/plo`,
      )
      return
    }

    navigate(
      `${roleBase}/programs`,
    )
  }

  const isInitialLoading =
  isLoadingPrograms
  || isLoadingMajors
  || isLoadingCourseTypes
  || (
    Boolean(effectiveProgramId)
    && isLoadingCohorts
  )

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-5 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <Grid3X3 className="size-6" />
            </div>

            <div data-admin-page-header="CloPloHeatmapPage">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Academic Outcome Analysis
              </p>

              <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
                CLO–PLO Coverage
              </h1>

              <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
                Analyze curriculum-wide CLO contributions to program learning outcomes, identify coverage gaps, and export the current matrix.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              disabled={
                !canExport
                || exporting !== null
              }
              title={
                canExport
                  ? "Export the exact current matrix scope to Excel."
                  : "Export becomes available when the selected scope contains curriculum courses and PLOs."
              }
              onClick={() =>
                exportMatrix("excel")
              }
            >
              <FileSpreadsheet className="size-4" />
              {exporting === "excel"
                ? "Exporting..."
                : "Export Excel"}
            </Button>

            <Button
              type="button"
              variant="outline"
              disabled={
                !canExport
                || exporting !== null
              }
              title={
                canExport
                  ? "Export the exact current matrix scope to PDF."
                  : "Export becomes available when the selected scope contains curriculum courses and PLOs."
              }
              onClick={() =>
                exportMatrix("pdf")
              }
            >
              <FileText className="size-4" />
              {exporting === "pdf"
                ? "Exporting..."
                : "Export PDF"}
            </Button>
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="font-semibold text-[#17343d]">
              Analysis Scope
            </h2>

            <p className="mt-1 text-xs text-slate-500">
              Select a major and cohort. The selected cohort determines the exact curriculum program used for CLO–PLO analysis.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {(isFetchingHeatmap
            && !isLoadingHeatmap) && (
              <Badge
                variant="outline"
                className="w-fit border-blue-200 bg-blue-50 text-blue-700"
              >
                Updating analysis...
              </Badge>
            )}

            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                setProgramId("")
                setCohortId("")
                setMajorCode("")
                setSemesterFilter(SYLLABUS_FILTER_ALL)
                setStatusFilter(SYLLABUS_FILTER_ALL)
                setCourseTypeId(SYLLABUS_FILTER_ALL)
                setSearchParams(
                  new URLSearchParams(),
                  { replace: true },
                )
                setCourseSearch("")
                setSelectedCell(null)
                setSelectedPlo(null)
              }}
            >
              Reset Scope
            </Button>
          </div>
        </div>

        <div className="mb-4">
          <label className="sr-only" htmlFor="heatmap-course-search">
            Search courses
          </label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <Input
              id="heatmap-course-search"
              value={courseSearch}
              onChange={(event) => setCourseSearch(event.target.value)}
              className="bg-white pl-9"
              placeholder="Search by course code, course name, version, or creator..."
            />
          </div>
        </div>

        <div data-admin-filter className="grid gap-4 md:grid-cols-5">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Major / Program
            </label>

            <Select
              value={majorCode || SYLLABUS_FILTER_ALL}
              onValueChange={(value) => {
  const nextMajorCode =
    value === SYLLABUS_FILTER_ALL
      ? ""
      : value

  setMajorCode(nextMajorCode)

  // Major chỉ dùng để thu hẹp danh sách cohort.
  // Chưa quyết định Program cho đến khi user chọn Cohort.
  setProgramId("")
  setCohortId("")

  const next =
    new URLSearchParams(searchParams)

  if (nextMajorCode) {
    next.set(
      "majorCode",
      nextMajorCode,
    )
  } else {
    next.delete("majorCode")
  }

  next.delete("programId")
  next.delete("cohortId")

  setSearchParams(
    next,
    { replace: true },
  )

  setSelectedCell(null)
  setSelectedPlo(null)
}}
              disabled={
                isLoadingPrograms || isLoadingMajors
                || majors.length === 0
              }
            >
              <SelectTrigger className="bg-white">
                <SelectValue placeholder="Select program..." />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={SYLLABUS_FILTER_ALL}>All Majors</SelectItem>
                {majors
                  .slice()
                  .sort((a, b) => a.code.localeCompare(b.code, "en", { numeric: true }))
                  .map(
                  (major) => (
                    <SelectItem
                      key={major.id}
                      value={major.code}
                    >
                      {major.code} — {major.name}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Cohort
            </label>

            <Select
              value={effectiveCohortId || SYLLABUS_FILTER_ALL}
              onValueChange={(value) => {
                if (value === SYLLABUS_FILTER_ALL) {
  setCohortId("")
  setProgramId("")

  const next =
    new URLSearchParams(searchParams)

  next.delete("cohortId")
  next.delete("programId")

  setSearchParams(
    next,
    { replace: true },
  )

  setSelectedCell(null)
  setSelectedPlo(null)

  return
}
                const selectedCohort = cohorts.find((item) => String(item.id) === value)
                const cohortProgram = selectedCohort
                  ? programs.find((program) => program.id === selectedCohort.programId)
                  : undefined
                if (cohortProgram) {
                  setProgramId(String(cohortProgram.id))
                  setMajorCode(cohortProgram.majorCode)
                }
                setCohortId(value)
                const next =
                  new URLSearchParams(searchParams)
                next.set(
                  "programId",
                  cohortProgram ? String(cohortProgram.id) : "",
                )
                if (cohortProgram) next.set("majorCode", cohortProgram.majorCode)
                next.set("cohortId", value)
                setSearchParams(
                  next,
                  { replace: true },
                )
                setSelectedCell(null)
                setSelectedPlo(null)
              }}
              disabled={
                isLoadingCohorts
                || availableCohorts.length === 0
              }
            >
              <SelectTrigger className="bg-white">
                <SelectValue placeholder="Select cohort..." />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={SYLLABUS_FILTER_ALL}>All Cohorts</SelectItem>
                {availableCohorts.map(
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
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Semester
            </label>
            <Select value={semesterFilter} onValueChange={setSemesterFilter}>
              <SelectTrigger className="bg-white"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value={SYLLABUS_FILTER_ALL}>All Semesters</SelectItem>
                {SYLLABUS_SEMESTER_OPTIONS.map((semester) => (
                  <SelectItem key={semester} value={semester}>Semester {semester}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
<div className="space-y-1.5">
  <label className="text-xs font-semibold text-slate-700">
    Course Group
  </label>

  <Select
    value={effectiveCourseTypeId}
    onValueChange={(value) => {
      setCourseTypeId(value)
      setSelectedCell(null)
      setSelectedPlo(null)
    }}
    disabled={isLoadingCourseTypes}
  >
    <SelectTrigger className="bg-white">
      <SelectValue />
    </SelectTrigger>

    <SelectContent>
      <SelectItem value={SYLLABUS_FILTER_ALL}>
        All Course Groups
      </SelectItem>

      {courseTypes
        .slice()
        .sort((a, b) =>
          a.code.localeCompare(
            b.code,
            "en",
            { numeric: true },
          ),
        )
        .map((type) => (
          <SelectItem
            key={type.id}
            value={String(type.id)}
          >
            {type.code} — {type.name}
          </SelectItem>
        ))}
    </SelectContent>
  </Select>
</div>
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Status
            </label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="bg-white"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value={SYLLABUS_FILTER_ALL}>All Statuses</SelectItem>
                {statusOptions.map((status) => (
                  <SelectItem key={status} value={status}>{formatSyllabusFilterStatus(status)}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-start gap-2 rounded-xl border border-[#d7e5e8] bg-[#f7fbfb] px-4 py-3 text-xs leading-5 text-[#5d747e] md:col-span-4">
            <Info className="mt-0.5 size-4 shrink-0 text-[#007d84]" />
            <span>
              All curriculum courses are included. Only CLOs from the curriculum-linked <strong>APPROVED</strong> syllabus contribute to coverage.
            </span>
          </div>
        </div>
      </section>

      {isProgramsError
|| isMajorsError
|| isCohortsError
|| isCourseTypesError ? (
        <section className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
          Unable to load program or cohort master data.
        </section>
      ) : isInitialLoading ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
          Loading curriculum scope...
        </section>
      ) : programs.length === 0 ? (
        <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
          No curriculum programs are available.
        </section>
      ) : !majorCode ? (
  <section className="rounded-2xl border border-blue-200 bg-blue-50 p-5 text-sm text-blue-800">
    Select a Major and Cohort to start curriculum-wide CLO–PLO analysis.
  </section>
) : availableCohorts.length === 0 ? (
  <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
    The selected Major has no available Cohort.
  </section>
) : !effectiveCohortId ? (
  <section className="rounded-2xl border border-blue-200 bg-blue-50 p-5 text-sm text-blue-800">
    Select a Cohort to analyze one exact curriculum snapshot.
  </section>
) : !effectiveProgramId ? (
  <section className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
    The selected Cohort is not linked to a valid curriculum Program.
  </section>
      ) : isHeatmapError ? (
        <section className="rounded-2xl border border-rose-200 bg-rose-50 p-5 shadow-sm">
          <div className="flex items-start gap-3">
            <XCircle className="mt-0.5 size-5 shrink-0 text-rose-700" />

            <div>
              <p className="font-semibold text-rose-800">
                Unable to calculate CLO–PLO coverage
              </p>

              <p className="mt-1 text-sm text-rose-700">
                {getErrorMessage(
                  heatmapError,
                )}
              </p>
            </div>
          </div>
        </section>
      ) : isLoadingHeatmap
      || !heatmap ? (
        <section className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500 shadow-sm">
          Calculating CLO–PLO coverage...
        </section>
      ) : (
        <>
          <section className="rounded-2xl border border-[#cfe1e4] bg-[#f7fbfb] px-5 py-4 shadow-sm">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <p className="font-semibold text-[#17343d]">
                  {getBaseProgramCode(
                    heatmap.programCode,
                  )}
                  {" · "}
                  {heatmap.programName}
                </p>

                <p className="mt-1 text-xs text-slate-500">
                  {heatmap.cohortName}
                  {" · Full Curriculum"}
                </p>
              </div>

              <div className="flex flex-wrap gap-2">
                <Badge
                  variant="outline"
                  className={
                    heatmap.summary
                      .uncoveredPlos === 0
                      ? "border-emerald-200 bg-white text-emerald-700"
                      : "border-amber-200 bg-white text-amber-700"
                  }
                >
                  {readiness !== "READY"
  ? "Coverage pending data"
  : heatmap.summary.uncoveredPlos === 0
    ? "All PLOs covered"
    : `${heatmap.summary.uncoveredPlos} PLOs need coverage`}
                </Badge>

                <Badge
                  variant="outline"
                  className="border-slate-200 bg-white text-slate-600"
                >
                  APPROVED syllabuses only
                </Badge>
              </div>
            </div>
          </section>

          <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              title="PLO Coverage"
              value={
                hasCloAnalysisData
                  ? `${heatmap.summary.ploCoveragePercentage}%`
                  : "—"
              }
              description={
                hasCloAnalysisData
                  ? `${heatmap.summary.coveredPlos} of ${heatmap.summary.totalPlos} PLOs are covered by at least one CLO`
                  : "Coverage is not yet evaluable from the current approved syllabus data"
              }
              icon={<Target className="size-5" />}
              tone={
                !hasCloAnalysisData
                  ? "default"
                  : heatmap.summary.uncoveredPlos === 0
                    ? "success"
                    : "warning"
              }
            />

            <SummaryCard
              title="Uncovered PLOs"
              value={
                hasCloAnalysisData
                  ? heatmap.summary.uncoveredPlos
                  : "—"
              }
              description={
                hasCloAnalysisData
                  ? "Program outcomes requiring additional CLO coverage"
                  : "Available after approved syllabus CLOs are present"
              }
              icon={<AlertTriangle className="size-5" />}
              tone={
                !hasCloAnalysisData
                  ? "default"
                  : heatmap.summary.uncoveredPlos === 0
                    ? "success"
                    : "warning"
              }
            />

            <SummaryCard
              title="CLO Mapping"
              value={
                heatmap.summary.totalClos > 0
                  ? `${heatmap.summary.cloMappingPercentage}%`
                  : "—"
              }
              description={
                heatmap.summary.totalClos > 0
                  ? `${heatmap.summary.mappedClos} of ${heatmap.summary.totalClos} CLOs are mapped to at least one PLO`
                  : "No CLOs are available from approved syllabuses in this scope"
              }
              icon={<CheckCircle2 className="size-5" />}
              tone={
                heatmap.summary.totalClos === 0
                  ? "default"
                  : heatmap.summary.unmappedClos === 0
                    ? "success"
                    : "warning"
              }
            />

            <SummaryCard
              title="Unmapped CLOs"
              value={
                heatmap.summary.totalClos > 0
                  ? heatmap.summary.unmappedClos
                  : "—"
              }
              description={
                heatmap.summary.totalClos > 0
                  ? "Approved CLOs that are not linked to any PLO"
                  : "Available after approved syllabus CLOs are present"
              }
              icon={<XCircle className="size-5" />}
              tone={
                heatmap.summary.totalClos === 0
                  ? "default"
                  : heatmap.summary.unmappedClos === 0
                    ? "success"
                    : "danger"
              }
            />
          </section>

          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h2 className="font-semibold text-[#17343d]">
                  Data Readiness
                </h2>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Coverage is calculated only from approved syllabus CLOs. Use these indicators to distinguish a true coverage gap from missing academic data.
                </p>
              </div>

              <Badge
                variant="outline"
                className={
                  readiness === "READY"
                    ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                    : "border-amber-200 bg-amber-50 text-amber-700"
                }
              >
                {readiness === "READY"
                  ? "Ready for analysis"
                  : "Data incomplete"}
              </Badge>
            </div>

            <div className="grid gap-px bg-slate-100 sm:grid-cols-3">
              <div className="bg-white px-5 py-4">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Approved Syllabuses
                </p>
                <p className="mt-1 text-xl font-bold text-slate-900">
                  {heatmap.summary.coursesWithApprovedSyllabus}
                  <span className="text-sm font-medium text-slate-400">
                    /{heatmap.summary.totalCourses}
                  </span>
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {heatmap.summary.approvedSyllabusPercentage}% of courses contribute
                </p>
              </div>

              <div className="bg-white px-5 py-4">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  Courses Excluded
                </p>
                <p className="mt-1 text-xl font-bold text-slate-900">
                  {blockedCourses.length}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  No curriculum-linked approved syllabus
                </p>
              </div>

              <div className="bg-white px-5 py-4">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                  CLOs Available
                </p>
                <p className="mt-1 text-xl font-bold text-slate-900">
                  {heatmap.summary.totalClos}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  CLOs from approved syllabuses available for mapping
                </p>
              </div>
            </div>
          </section>

          <section className="space-y-3">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                  Primary Analysis
                </p>

                <h2 className="mt-1 text-xl font-bold text-[#17343d]">
                  Course × PLO Matrix
                </h2>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Each cell shows the highest contribution level among the course CLOs mapped to that PLO. Click a mapped cell to inspect every contributing CLO.
                </p>
              </div>

              <div className="flex flex-wrap gap-2">
                <Badge
                  variant="outline"
                  className={levelClass("X")}
                >
                  X · Low
                </Badge>

                <Badge
                  variant="outline"
                  className={levelClass("XX")}
                >
                  XX · Medium
                </Badge>

                <Badge
                  variant="outline"
                  className={levelClass("XXX")}
                >
                  XXX · High
                </Badge>

                <Badge
                  variant="outline"
                  className={levelClass(null)}
                >
                  — · Not mapped
                </Badge>
              </div>
            </div>

            {!canRenderMatrix ? (
              <CoverageEmptyState
                readiness={readiness}
                blockedCourseCount={
                  blockedCourses.length
                }
                totalCloCount={
                  heatmap.summary.totalClos
                }
                mappedCloCount={
                  heatmap.summary.mappedClos
                }
                onManagePlo={() =>
                  navigateTo("plo")
                }
                onManageProgram={() =>
                  navigateTo("programs")
                }
                onOpenSyllabus={() =>
                  navigateTo("syllabus")
                }
              />
            ) : (
              <div className="overflow-hidden rounded-2xl border-2 border-[#b9dfe1] bg-white shadow-sm">
                <div className="flex flex-col gap-3 border-b border-slate-100 p-4 md:flex-row md:items-center md:justify-between">
                  <div className="relative w-full md:max-w-md">
                    <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

                    <Input
                      value={courseSearch}
                      onChange={(event) =>
                        setCourseSearch(
                          event.target.value,
                        )
                      }
                      placeholder="Search approved courses..."
                      className="pl-9"
                    />
                  </div>

                  <p className="text-xs text-slate-500">
                    Showing{" "}
                    <span className="font-semibold text-slate-700">
                      {filteredApprovedCourses.length}
                    </span>
                    {" "}of{" "}
                    <span className="font-semibold text-slate-700">
                      {approvedCourses.length}
                    </span>
                    {" "}approved curriculum courses
                  </p>
                </div>

                <div className="max-h-[68vh] overflow-auto">
                  <table className="min-w-max border-collapse text-sm">
                    <thead>
                      <tr>
                        <th className="sticky left-0 top-0 z-30 min-w-[320px] border-b border-r bg-slate-50 px-4 py-3 text-left font-semibold text-slate-700">
                          Approved Course / CLO Mapping
                        </th>

                        {heatmap.ploDetails.map(
                          (plo) => (
                            <th
                              key={plo.id}
                              className={
                                "sticky top-0 z-20 min-w-[126px] border-b border-r px-3 py-3 text-center "
                                + (
                                  plo.covered
                                    ? "bg-slate-50"
                                    : "bg-amber-50"
                                )
                              }
                            >
                              <button
                                type="button"
                                className="w-full"
                                onClick={() => {
                                  const profile =
                                    ploProfiles.find(
                                      (item) =>
                                        item.plo.id
                                        === plo.id,
                                    )

                                  if (profile) {
                                    setSelectedPlo(
                                      profile,
                                    )
                                  }
                                }}
                                title={getPloDisplayDescription(plo)}
                              >
                                <div className="font-bold text-slate-800">
                                  {plo.code}
                                </div>

                                <div className="mt-1 text-[10px] font-normal text-slate-500">
                                  {plo.courseCount} courses
                                  {" · "}
                                  {plo.cloCount} CLOs
                                </div>

                                {!plo.covered && (
                                  <Badge
                                    variant="outline"
                                    className="mt-1 border-amber-300 bg-white text-[10px] text-amber-700"
                                  >
                                    Uncovered
                                  </Badge>
                                )}
                              </button>
                            </th>
                          ),
                        )}
                      </tr>
                    </thead>

                    <tbody>
                      {filteredApprovedCourses.map(
                        (course) => {
                          const mappingPercentage =
                            course.totalClos > 0
                              ? Math.round(
                                  (
                                    course.mappedClos
                                    * 1000
                                  )
                                  / course.totalClos,
                                )
                                / 10
                              : 0

                          return (
                            <tr
                              key={course.courseId}
                              className="hover:bg-slate-50/50"
                            >
                              <td className="sticky left-0 z-10 border-b border-r bg-white px-4 py-3">
                                <div className="flex items-start justify-between gap-3">
                                  <div className="min-w-0">
                                    <p className="font-semibold text-slate-900">
                                      {course.courseCode}
                                    </p>

                                    <p className="mt-0.5 max-w-[220px] truncate text-xs text-slate-500">
                                      {course.courseNameVn
                                        || course.courseName}
                                    </p>
                                  </div>

                                  <Badge
                                    variant="outline"
                                    className={
                                      course.unmappedCloCodes.length === 0
                                        ? "border-emerald-200 bg-emerald-50 text-[10px] text-emerald-700"
                                        : "border-amber-200 bg-amber-50 text-[10px] text-amber-700"
                                    }
                                  >
                                    {course.mappedClos}/{course.totalClos} CLOs
                                  </Badge>
                                </div>

                                <div className="mt-2 flex flex-wrap items-center gap-1.5">
                                  <Badge
                                    variant="outline"
                                    className="border-emerald-200 bg-emerald-50 text-[10px] text-emerald-700"
                                  >
                                    APPROVED{" "}
                                    {course.syllabusVersionLabel
                                      || (
                                        course.syllabusVersion
                                          ? formatVersionLabel(course.syllabusVersion)
                                          : ""
                                      )}
                                  </Badge>

                                  <span className="text-[10px] text-slate-400">
                                    {mappingPercentage}% CLO mapped
                                  </span>
                                </div>

                                {course.unmappedCloCodes.length > 0 && (
                                  <p className="mt-2 text-[10px] leading-4 text-amber-700">
                                    Unmapped:{" "}
                                    {course.unmappedCloCodes
                                      .slice(0, 4)
                                      .join(", ")}
                                    {course.unmappedCloCodes.length > 4
                                      ? ` +${course.unmappedCloCodes.length - 4} more`
                                      : ""}
                                  </p>
                                )}
                              </td>

                              {heatmap.ploDetails.map(
                                (plo) => {
                                  const cell =
                                    course.cells.find(
                                      (candidate) =>
                                        candidate.ploId
                                        === plo.id,
                                    )

                                  const level =
                                    cell?.level
                                    ?? null

                                  return (
                                    <td
                                      key={`${course.courseId}-${plo.id}`}
                                      className="border-b border-r p-2 text-center"
                                    >
                                      <button
                                        type="button"
                                        disabled={!level}
                                        className={
                                          "mx-auto flex h-10 min-w-[64px] items-center justify-center gap-1 rounded-lg border px-2 text-xs font-bold transition "
                                          + levelClass(level)
                                          + (
                                            level
                                              ? ""
                                              : " cursor-default"
                                          )
                                        }
                                        onClick={() => {
                                          if (cell && level) {
                                            setSelectedCell({
                                              course,
                                              plo,
                                              cell,
                                            })
                                          }
                                        }}
                                        title={
                                          level
                                            ? `${levelLabel(level)} · ${cell?.mappingCount ?? 0} contributing CLOs`
                                            : `${course.courseCode} has no CLO mapping to ${plo.code}`
                                        }
                                      >
                                        <span>
                                          {levelShortLabel(level)}
                                        </span>

                                        {cell
                                        && cell.mappingCount > 1 && (
                                          <span className="text-[10px] font-normal opacity-70">
                                            ({cell.mappingCount})
                                          </span>
                                        )}
                                      </button>
                                    </td>
                                  )
                                },
                              )}
                            </tr>
                          )
                        },
                      )}
                    </tbody>
                  </table>
                </div>

                {filteredApprovedCourses.length === 0 && (
                  <div className="border-t border-slate-100 px-5 py-8 text-center text-sm text-slate-500">
                    No approved course matches the search.
                  </div>
                )}
              </div>
            )}
          </section>

          {canRenderMatrix && (
          <section className="space-y-3">
            <div className="flex flex-col gap-2 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                  Coverage Overview
                </p>

                <h2 className="mt-1 text-xl font-bold text-[#17343d]">
                  PLO Coverage Overview
                </h2>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Review the coverage profile of each PLO after the primary course × PLO matrix. Course and CLO counts come only from approved syllabuses in the selected scope.
                </p>
              </div>

              <div className="flex flex-wrap gap-2">
                <CoverageLevelBadge
                  level="X"
                  count={
                    approvedCourses.reduce(
                      (sum, course) =>
                        sum
                        + course.cells.filter(
                          (cell) =>
                            cell.level
                            === "X",
                        ).length,
                      0,
                    )
                  }
                />

                <CoverageLevelBadge
                  level="XX"
                  count={
                    approvedCourses.reduce(
                      (sum, course) =>
                        sum
                        + course.cells.filter(
                          (cell) =>
                            cell.level
                            === "XX",
                        ).length,
                      0,
                    )
                  }
                />

                <CoverageLevelBadge
                  level="XXX"
                  count={
                    approvedCourses.reduce(
                      (sum, course) =>
                        sum
                        + course.cells.filter(
                          (cell) =>
                            cell.level
                            === "XXX",
                        ).length,
                      0,
                    )
                  }
                />
              </div>
            </div>

            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
              {ploProfiles.map(
                ({
                  plo,
                  introduceCourses,
                  developCourses,
                  achieveCourses,
                }) => (
                  <button
                    key={plo.id}
                    type="button"
                    className={
                      "rounded-xl border bg-white p-4 text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md "
                      + (
                        plo.covered
                          ? "border-slate-200"
                          : "border-amber-200 bg-amber-50/30"
                      )
                    }
                    onClick={() =>
                      setSelectedPlo({
                        plo,
                        introduceCourses,
                        developCourses,
                        achieveCourses,
                      })
                    }
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="text-lg font-bold text-[#17343d]">
                          {plo.code}
                        </p>

                        <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
                          {getPloDisplayDescription(plo)}
                        </p>
                      </div>

                      <Badge
                        variant="outline"
                        className={
                          plo.covered
                            ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                            : "border-amber-200 bg-amber-50 text-amber-700"
                        }
                      >
                        {plo.covered
                          ? "Covered"
                          : "Uncovered"}
                      </Badge>
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                      <div className="rounded-lg bg-slate-50 px-3 py-2">
                        <p className="text-slate-400">
                          Courses
                        </p>
                        <p className="mt-1 font-bold text-slate-700">
                          {plo.courseCount}
                        </p>
                      </div>

                      <div className="rounded-lg bg-slate-50 px-3 py-2">
                        <p className="text-slate-400">
                          CLOs
                        </p>
                        <p className="mt-1 font-bold text-slate-700">
                          {plo.cloCount}
                        </p>
                      </div>
                    </div>

                    <div className="mt-3 flex flex-wrap gap-1.5">
                      <CoverageLevelBadge
                        level="X"
                        count={
                          introduceCourses
                        }
                      />
                      <CoverageLevelBadge
                        level="XX"
                        count={
                          developCourses
                        }
                      />
                      <CoverageLevelBadge
                        level="XXX"
                        count={
                          achieveCourses
                        }
                      />
                    </div>
                  </button>
                ),
              )}
            </div>
            </section>

          )}
          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="font-semibold text-slate-900">
                    Issues Requiring Action
                  </h2>

                  {issueGroupCount > 0 && (
                    <Badge
                      variant="outline"
                      className="border-amber-200 bg-amber-50 text-amber-700"
                    >
                      {issueGroupCount}{" "}
                      {issueGroupCount === 1
                        ? "issue group"
                        : "issue groups"}
                    </Badge>
                  )}
                </div>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Coverage gaps and data-readiness problems are separated so the next action is clear.
                </p>
              </div>

              {issueGroupCount > 0 && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setShowIssues(
                      (value) => !value,
                    )
                  }
                >
                  {showIssues
                    ? <ChevronUp className="size-4" />
                    : <ChevronDown className="size-4" />}

                  {showIssues
                    ? "Hide issue details"
                    : "Review issues"}
                </Button>
              )}
            </div>

            {issueGroupCount === 0 ? (
              <div className="px-5 py-4">
                <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
                  <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-700" />

                  <div>
                    <p className="font-semibold text-emerald-800">
                      No CLO–PLO coverage gaps detected
                    </p>

                    <p className="mt-1 text-xs leading-5 text-emerald-700">
                      Every active PLO is covered, all available CLOs are mapped, and no data-readiness warning remains in the selected scope.
                    </p>
                  </div>
                </div>
              </div>
            ) : !showIssues ? (
              <div className="grid gap-3 px-5 py-4 lg:grid-cols-2">
                <IssueSummaryGroup
                  title="Coverage Issues"
                  description="Outcome gaps that require academic review."
                  count={coverageIssueGroupCount}
                  tone="warning"
                  items={[
                    ...coverageIssueSections.map(
                      (section) => ({
                        label: section.title,
                        count: section.count,
                      }),
                    ),
                    ...coverageWarnings.map(
                      (warning) => ({
                        label: warning.message,
                        count: warning.references.length,
                      }),
                    ),
                  ]}
                />

                <IssueSummaryGroup
                  title="Data Readiness"
                  description="Missing approved syllabus data that prevents or limits analysis."
                  count={readinessIssueGroupCount}
                  tone="danger"
                  items={[
                    ...readinessIssueSections.map(
                      (section) => ({
                        label: section.title,
                        count: section.count,
                      }),
                    ),
                    ...readinessWarnings.map(
                      (warning) => ({
                        label: warning.message,
                        count: warning.references.length,
                      }),
                    ),
                  ]}
                />
              </div>
            ) : (
              <div className="grid gap-5 px-5 py-5 xl:grid-cols-2">
                <div className="space-y-3">
                  <div>
                    <h3 className="font-semibold text-[#17343d]">
                      Coverage Issues
                    </h3>
                    <p className="mt-1 text-xs text-slate-500">
                      Uncovered PLOs, unmapped CLOs, and other outcome-analysis warnings.
                    </p>
                  </div>

                  {coverageIssueGroupCount === 0 ? (
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
                      No outcome coverage issue requires action.
                    </div>
                  ) : (
                    <>
                      {coverageIssueSections.map(
                        (section) => (
                          <IssueList
                            key={section.key}
                            section={section}
                          />
                        ),
                      )}

                      {coverageWarnings.map(
                        (warning) => (
                          <AdditionalWarning
                            key={warning.code}
                            warning={warning}
                          />
                        ),
                      )}
                    </>
                  )}
                </div>

                <div className="space-y-3">
                  <div>
                    <h3 className="font-semibold text-[#17343d]">
                      Data Readiness
                    </h3>
                    <p className="mt-1 text-xs text-slate-500">
                      Courses or syllabus data that are excluded from the current analysis.
                    </p>
                  </div>

                  {readinessIssueGroupCount === 0 ? (
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
                      Required syllabus data are available for this scope.
                    </div>
                  ) : (
                    <>
                      {readinessIssueSections.map(
                        (section) => (
                          <IssueList
                            key={section.key}
                            section={section}
                          />
                        ),
                      )}

                      {readinessWarnings.map(
                        (warning) => (
                          <AdditionalWarning
                            key={warning.code}
                            warning={warning}
                          />
                        ),
                      )}
                    </>
                  )}
                </div>
              </div>
            )}
          </section>

          {exportError && (
            <section className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {exportError}
            </section>
          )}
        </>
      )}

      <Dialog
        open={selectedCell !== null}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedCell(null)
          }
        }}
      >
        <DialogContent className="bg-white sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              CLO Contribution Details
            </DialogTitle>

            <DialogDescription>
              Inspect the CLOs contributing to the selected course × PLO cell.
            </DialogDescription>
          </DialogHeader>

          {selectedCell && (
            <div className="space-y-5">
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                    Course
                  </p>

                  <p className="mt-1 font-semibold text-slate-900">
                    {selectedCell.course.courseCode}
                  </p>

                  <p className="mt-1 text-xs text-slate-500">
                    {selectedCell.course.courseNameVn
                      || selectedCell.course.courseName}
                  </p>

                  <p className="mt-2 text-xs text-slate-500">
                    Syllabus{" "}
                    {selectedCell.course.syllabusVersionLabel
                      || (
                        selectedCell.course.syllabusVersion
                          ? formatVersionLabel(selectedCell.course.syllabusVersion)
                          : ""
                      )}
                    {" · APPROVED"}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
                    Program Learning Outcome
                  </p>

                  <p className="mt-1 font-semibold text-slate-900">
                    {selectedCell.plo.code}
                  </p>

                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    {getPloDisplayDescription(selectedCell.plo)}
                  </p>
                </div>
              </div>

              <div className="rounded-xl border border-[#cfe1e4] bg-[#f7fbfb] p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold text-slate-600">
                      Highest contribution shown in the matrix
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      The matrix displays the highest X/XX/XXX contribution among the CLO mappings in this cell.
                    </p>
                  </div>

                  <Badge
                    variant="outline"
                    className={levelClass(
                      selectedCell.cell.level,
                    )}
                  >
                    {levelLabel(
                      selectedCell.cell.level,
                    )}
                  </Badge>
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between gap-3">
                  <h3 className="font-semibold text-slate-900">
                    Contributing CLOs
                  </h3>

                  <Badge variant="outline">
                    {selectedCell.cell.mappingCount}
                  </Badge>
                </div>

                {selectedCell.cell.cloContributions.length > 0 ? (
                  <div className="mt-3 space-y-2">
                    {selectedCell.cell.cloContributions.map(
                      (contribution) => (
                        <div key={contribution.cloId} className="rounded-xl border border-slate-200 bg-white p-3">
                          <div className="flex items-center justify-between gap-3">
                            <p className="font-semibold text-[#007d84]">{contribution.cloCode}</p>
                            <Badge variant="outline" className={levelClass(contribution.level)}>
                              {levelLabel(contribution.level)}
                            </Badge>
                          </div>
                          <p className="mt-2 text-xs leading-5 text-slate-600">
                            {contribution.descriptionVn || contribution.description}
                          </p>
                        </div>
                      ),
                    )}
                  </div>
                ) : (
                  <p className="mt-3 text-sm text-slate-500">
                    No CLO mapping exists for this cell.
                  </p>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <Dialog
        open={selectedPlo !== null}
        onOpenChange={(open) => {
          if (!open) {
            setSelectedPlo(null)
          }
        }}
      >
        <DialogContent className="bg-white sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {selectedPlo
                ? `${selectedPlo.plo.code} Coverage Profile`
                : "PLO Coverage Profile"}
            </DialogTitle>

            <DialogDescription>
              Review coverage status and X/XX/XXX contribution distribution for this PLO.
            </DialogDescription>
          </DialogHeader>

          {selectedPlo && (
            <div className="space-y-5">
              <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-900">
                      {getPloDisplayDescription(selectedPlo.plo)}
                    </p>

                    <p className="mt-2 text-xs text-slate-500">
                      Category:{" "}
                      {selectedPlo.plo.category
                        || "Not specified"}
                      {" · Version "}
                      {selectedPlo.plo.versionNumber
                        ?? "—"}
                    </p>
                  </div>

                  <Badge
                    variant="outline"
                    className={
                      selectedPlo.plo.covered
                        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                        : "border-amber-200 bg-amber-50 text-amber-700"
                    }
                  >
                    {selectedPlo.plo.covered
                      ? "Covered"
                      : "Uncovered"}
                  </Badge>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-xl border border-slate-200 p-4">
                  <p className="text-xs text-slate-500">
                    Contributing Courses
                  </p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {selectedPlo.plo.courseCount}
                  </p>
                </div>

                <div className="rounded-xl border border-slate-200 p-4">
                  <p className="text-xs text-slate-500">
                    Contributing CLOs
                  </p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {selectedPlo.plo.cloCount}
                  </p>
                </div>
              </div>

              <div>
                <h3 className="font-semibold text-slate-900">
                  Contribution Distribution
                </h3>

                <div className="mt-3 grid gap-3 sm:grid-cols-3">
                  <div className="rounded-xl border border-blue-200 bg-blue-50 p-4">
                    <p className="text-xs font-semibold text-blue-700">
                      Low (X)
                    </p>
                    <p className="mt-1 text-2xl font-bold text-blue-800">
                      {selectedPlo.introduceCourses}
                    </p>
                    <p className="text-[10px] text-blue-600">
                      course cells
                    </p>
                  </div>

                  <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                    <p className="text-xs font-semibold text-amber-700">
                      Medium (XX)
                    </p>
                    <p className="mt-1 text-2xl font-bold text-amber-800">
                      {selectedPlo.developCourses}
                    </p>
                    <p className="text-[10px] text-amber-600">
                      course cells
                    </p>
                  </div>

                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
                    <p className="text-xs font-semibold text-emerald-700">
                      High (XXX)
                    </p>
                    <p className="mt-1 text-2xl font-bold text-emerald-800">
                      {selectedPlo.achieveCourses}
                    </p>
                    <p className="text-[10px] text-emerald-600">
                      course cells
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

function CoverageEmptyState({
  readiness,
  blockedCourseCount,
  totalCloCount,
  mappedCloCount,
  onManagePlo,
  onManageProgram,
  onOpenSyllabus,
}: {
  readiness: PageReadiness
  blockedCourseCount: number
  totalCloCount: number
  mappedCloCount: number
  onManagePlo: () => void
  onManageProgram: () => void
  onOpenSyllabus: () => void
}) {
  const config: Record<
    Exclude<PageReadiness, "READY">,
    {
      title: string
      description: string
      nextStep: string
      actionLabel: string
      action: () => void
    }
  > = {
    NO_PLO: {
      title: "No active PLOs are defined",
      description:
        "The system cannot build a CLO–PLO matrix until the selected curriculum program has active PLOs.",
      nextStep:
        "Define the program PLO/ILO list first.",
      actionLabel:
        "Manage PLOs",
      action: onManagePlo,
    },

    NO_COURSE: {
      title: "No curriculum courses are available",
      description:
        "The selected program/cohort contains no curriculum courses in this course-group scope.",
      nextStep:
        "Add or review courses in the curriculum before analyzing outcome coverage.",
      actionLabel:
        "Manage Curriculum",
      action: onManageProgram,
    },

    NO_APPROVED: {
      title: "No APPROVED syllabuses can contribute",
      description:
        `${blockedCourseCount} curriculum course${blockedCourseCount === 1 ? "" : "s"} do not have a curriculum-linked APPROVED syllabus.`,
      nextStep:
        "Coverage is calculated only from approved syllabus CLOs, so approve the relevant syllabuses or choose another academic period.",
      actionLabel:
        "Open Syllabus Catalog",
      action: onOpenSyllabus,
    },

    NO_CLO: {
      title: "Approved syllabuses contain no CLOs",
      description:
        "At least one APPROVED syllabus exists, but no CLO is available in the selected scope.",
      nextStep:
        "Review the approved syllabus content and ensure CLOs are defined before CLO–PLO mapping.",
      actionLabel:
        "Open Syllabus Catalog",
      action: onOpenSyllabus,
    },

    NO_MAPPING: {
      title: "CLOs are ready, but no CLO–PLO mappings exist",
      description:
        `${totalCloCount} CLO${totalCloCount === 1 ? "" : "s"} are available from APPROVED syllabuses, but ${mappedCloCount} are mapped to PLOs.`,
      nextStep:
        "Faculty must map CLOs to PLOs with X/XX/XXX contribution levels before the matrix can analyze coverage.",
      actionLabel:
        "Open Syllabus Catalog",
      action: onOpenSyllabus,
    },
  }

  if (readiness === "READY") {
    return null
  }

  const state = config[readiness]

  return (
    <div className="overflow-hidden rounded-2xl border-2 border-dashed border-[#b9dfe1] bg-white shadow-sm">
      <div className="px-6 py-10 text-center">
        <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-[#eef8f8] text-[#007d84]">
          <BarChart3 className="size-6" />
        </div>

        <h3 className="mt-4 text-lg font-semibold text-[#17343d]">
          {state.title}
        </h3>

        <p className="mx-auto mt-2 max-w-2xl text-sm leading-6 text-slate-500">
          {state.description}
        </p>

        <div className="mx-auto mt-5 max-w-2xl rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-left">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
            Next step
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-600">
            {state.nextStep}
          </p>
        </div>

        <div className="mt-5">
          <Button
            type="button"
            onClick={state.action}
            className="bg-[#007d84] text-white hover:bg-[#006d73]"
          >
            {state.actionLabel}
            <ArrowRight className="size-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

export default LiveCloPloHeatmapPage
