import {
  useMemo,
  useState,
} from "react"
import {
  useQuery,
} from "@tanstack/react-query"
import {
  useNavigate,
} from "react-router-dom"
import {
  ArrowRight,
} from "lucide-react"

import {
  type ClassSectionResponse,
} from "@/api/classSectionApi"
import {
  cohortApi,
} from "@/api/cohortApi"
import {
  programApi,
} from "@/api/programApi"
import {
  Badge,
} from "@/components/ui/badge"
import {
  Button,
} from "@/components/ui/button"
import {
  Card,
  CardContent,
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
import {
  formatSyllabusFilterStatus,
  SYLLABUS_CANONICAL_STATUSES,
  SYLLABUS_FILTER_ALL,
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"
import {
  formatVersionLabel,
} from "@/lib/syllabusVersion"

const ALL =
  SYLLABUS_FILTER_ALL

const MISSING =
  "MISSING"

function normalize(
  value: unknown,
) {
  return String(
    value ?? "",
  )
    .trim()
    .toUpperCase()
}

function logicalCourseKey(
  item: ClassSectionResponse,
) {
  return [
    item.courseId,
    item.programId
      ?? "no-program",
    item.cohortId
      ?? "no-cohort",
    item.semester,
  ].join("|")
}

function groupAssignments(
  assignments:
    ClassSectionResponse[],
) {
  const grouped =
    new Map<
      string,
      ClassSectionResponse
    >()

  assignments.forEach(
    (assignment) => {
      const key =
        logicalCourseKey(
          assignment,
        )

      const current =
        grouped.get(key)

      if (!current) {
        grouped.set(
          key,
          assignment,
        )
        return
      }

      if (
        current.syllabusId
          === null
        && assignment.syllabusId
          !== null
      ) {
        grouped.set(
          key,
          assignment,
        )
      }
    },
  )

  return Array.from(
    grouped.values(),
  )
}

function actualStatus(
  item: ClassSectionResponse,
) {
  if (
    item.syllabusId === null
  ) {
    return MISSING
  }

  return normalize(
    item.syllabusStatus,
  )
}

function statusLabel(
  item: ClassSectionResponse,
) {
  const status =
    actualStatus(item)

  if (status === MISSING) {
    return "Not Started"
  }

  return formatSyllabusFilterStatus(
    status,
  )
}

function statusBadgeClass(
  item: ClassSectionResponse,
) {
  const status =
    actualStatus(item)

  switch (status) {
    case MISSING:
      return "border-slate-200 bg-slate-50 text-slate-700"

    case "DRAFT":
      return "border-orange-200 bg-orange-50 text-orange-700"

    case "SUBMITTED":
      return "border-amber-200 bg-amber-50 text-amber-700"

    case "UNDER_REVIEW":
      return "border-blue-200 bg-blue-50 text-blue-700"

    case "REVISION_REQUESTED":
    case "REJECTED":
      return "border-rose-200 bg-rose-50 text-rose-700"

    case "APPROVED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700"

    default:
      return "border-slate-200 bg-slate-50 text-slate-700"
  }
}

function workflowPriority(
  item: ClassSectionResponse,
) {
  const status =
    actualStatus(item)

  switch (status) {
    case MISSING:
      return 0

    case "REVISION_REQUESTED":
    case "REJECTED":
      return 1

    case "DRAFT":
      return 2

    case "SUBMITTED":
      return 3

    case "UNDER_REVIEW":
      return 4

    case "APPROVED":
      return 5

    default:
      return 6
  }
}

function getSyllabusAction(
  item: ClassSectionResponse,
) {
  const base =
    "/instructor/syllabus"

  const status =
    actualStatus(item)

  if (
    status === MISSING
    && item.readyForSyllabusCreation
  ) {
    const params =
      new URLSearchParams()

    if (item.programId) {
      params.set(
        "programId",
        String(
          item.programId,
        ),
      )
    }

    if (item.cohortId) {
      params.set(
        "cohortId",
        String(
          item.cohortId,
        ),
      )
    }

    params.set(
      "courseId",
      String(
        item.courseId,
      ),
    )

    params.set(
      "assignmentId",
      String(
        item.id,
      ),
    )

    return {
      label:
        "Create Syllabus",
      path:
        `${base}?${params.toString()}`,
      primary: true,
      disabled: false,
    }
  }

  if (status === MISSING) {
    return {
      label:
        "Awaiting Setup",
      path: "",
      primary: false,
      disabled: true,
    }
  }

  if (status === "DRAFT") {
    return {
      label:
        "Continue Draft",
      path:
        `${base}/${item.syllabusId}/edit`,
      primary: true,
      disabled: false,
    }
  }

  if (
    status
      === "REVISION_REQUESTED"
    || status
      === "REJECTED"
  ) {
    return {
      label:
        "Revise Syllabus",
      path:
        `${base}/${item.syllabusId}/edit`,
      primary: true,
      disabled: false,
    }
  }

  if (
    status === "APPROVED"
  ) {
    return {
      label:
        "View Approved",
      path:
        `${base}/${item.syllabusId}`,
      primary: false,
      disabled: false,
    }
  }

  return {
    label:
      "View Progress",
    path:
      `${base}/${item.syllabusId}`,
    primary: false,
    disabled: false,
  }
}

interface Props {
  assignments:
    ClassSectionResponse[]
  title?: string
  description?: string
}

export default function InstructorCourseWorklist({
  assignments,
  title = "Assigned Courses",
  description =
    "View your assigned courses, current syllabus status, and the next syllabus action.",
}: Props) {
  const navigate =
    useNavigate()

  const [
    majorFilter,
    setMajorFilter,
  ] =
    useState(ALL)

  const [
    cohortFilter,
    setCohortFilter,
  ] =
    useState(ALL)

  const [
    semesterFilter,
    setSemesterFilter,
  ] =
    useState(ALL)

  const [
    statusFilter,
    setStatusFilter,
  ] =
    useState(ALL)

  const {
    data: majors = [],
  } =
    useQuery({
      queryKey: [
        "majors",
      ],
      queryFn:
        programApi.getMajors,
      staleTime: 60_000,
    })

  const {
    data: programs = [],
  } =
    useQuery({
      queryKey: [
        "programs",
      ],
      queryFn:
        programApi.getAll,
      staleTime: 60_000,
    })

  const {
    data: cohorts = [],
  } =
    useQuery({
      queryKey: [
        "cohorts",
      ],
      queryFn:
        cohortApi.getAll,
      staleTime: 60_000,
    })

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

  const majorOptions =
    useMemo(
      () =>
        majors
          .slice()
          .sort(
            (left, right) =>
              left.code
                .localeCompare(
                  right.code,
                  "en",
                  {
                    numeric: true,
                  },
                ),
          ),
      [majors],
    )

  const cohortOptions =
    useMemo(
      () =>
        cohorts
          .filter(
            (cohort) => {
              if (
                majorFilter
                  === ALL
              ) {
                return true
              }

              const program =
                programById.get(
                  cohort.programId,
                )

              return (
                program?.majorCode
                === majorFilter
              )
            },
          )
          .slice()
          .sort(
            (left, right) =>
              Number(
                right.entryYear
                  ?? 0,
              )
              - Number(
                  left.entryYear
                    ?? 0,
              ),
          ),
      [
        cohorts,
        majorFilter,
        programById,
      ],
    )

  const courseRows =
    useMemo(
      () =>
        groupAssignments(
          assignments,
        ),
      [assignments],
    )

  const filteredRows =
    useMemo(
      () =>
        courseRows
          .filter(
            (item) => {
              if (
                majorFilter
                  !== ALL
              ) {
                const program =
                  item.programId
                    !== null
                    ? programById
                        .get(
                          item.programId,
                        )
                    : undefined

                if (
                  program
                    ?.majorCode
                  !== majorFilter
                ) {
                  return false
                }
              }

              if (
                cohortFilter
                  !== ALL
                && String(
                  item.cohortId,
                )
                  !== cohortFilter
              ) {
                return false
              }

              if (
                semesterFilter
                  !== ALL
                && String(
                  item.semester,
                )
                  !== semesterFilter
              ) {
                return false
              }

              if (
                statusFilter
                  !== ALL
                && actualStatus(
                  item,
                )
                  !== statusFilter
              ) {
                return false
              }

              return true
            },
          )
          .slice()
          .sort(
            (left, right) => {
              const priority =
                workflowPriority(
                  left,
                )
                - workflowPriority(
                    right,
                  )

              if (
                priority !== 0
              ) {
                return priority
              }

              return left
                .courseCode
                .localeCompare(
                  right.courseCode,
                  "en",
                  {
                    numeric: true,
                  },
                )
            },
          ),
      [
        cohortFilter,
        courseRows,
        majorFilter,
        programById,
        semesterFilter,
        statusFilter,
      ],
    )

  const handleMajorChange =
    (
      value: string,
    ) => {
      setMajorFilter(
        value,
      )

      if (
        cohortFilter
          !== ALL
      ) {
        const cohort =
          cohorts.find(
            (item) =>
              String(
                item.id,
              )
              === cohortFilter,
          )

        const program =
          cohort
            ? programById.get(
                cohort.programId,
              )
            : undefined

        if (
          value !== ALL
          && program
            ?.majorCode
            !== value
        ) {
          setCohortFilter(
            ALL,
          )
        }
      }
    }

  const handleCohortChange =
    (
      value: string,
    ) => {
      setCohortFilter(
        value,
      )

      if (value === ALL) {
        return
      }

      const cohort =
        cohorts.find(
          (item) =>
            String(item.id)
            === value,
        )

      const program =
        cohort
          ? programById.get(
              cohort.programId,
            )
          : undefined

      if (
        program?.majorCode
      ) {
        setMajorFilter(
          program.majorCode,
        )
      }
    }

  const resetFilters = () => {
    setMajorFilter(ALL)
    setCohortFilter(ALL)
    setSemesterFilter(ALL)
    setStatusFilter(ALL)
  }

  const hasFilters =
    majorFilter !== ALL
    || cohortFilter !== ALL
    || semesterFilter !== ALL
    || statusFilter !== ALL

  const statusOptions =
    useMemo(
      () => {
        const statuses =
          new Set<string>(
            SYLLABUS_CANONICAL_STATUSES,
          )

        if (
          courseRows.some(
            (item) =>
              item.syllabusId
                === null,
          )
        ) {
          statuses.add(
            MISSING,
          )
        }

        return Array.from(
          statuses,
        )
      },
      [courseRows],
    )

  return (
    <Card className="overflow-hidden border border-slate-200 bg-white shadow-sm">
      <CardHeader className="border-b border-slate-100 pb-4">
        <CardTitle className="text-lg font-bold text-[#17343d]">
          {title}
        </CardTitle>

        <p className="text-sm text-slate-500">
          {description}
        </p>

        <div className="grid gap-4 pt-3 md:grid-cols-4">
          <div className="space-y-1.5">
            <label className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
              Major
            </label>

            <Select
              value={
                majorFilter
              }
              onValueChange={
                handleMajorChange
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
                      key={
                        major.id
                      }
                      value={
                        major.code
                      }
                    >
                      {major.code}
                      {major.name
                        ? ` — ${major.name}`
                        : major.nameVn
                          ? ` — ${major.nameVn}`
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
                cohortFilter
              }
              onValueChange={
                handleCohortChange
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
                      key={
                        cohort.id
                      }
                      value={
                        String(
                          cohort.id,
                        )
                      }
                    >
                      {cohort.name}
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
                semesterFilter
              }
              onValueChange={
                setSemesterFilter
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Semester" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Semesters
                </SelectItem>

                {SYLLABUS_SEMESTER_OPTIONS.map(
                  (semester) => (
                    <SelectItem
                      key={
                        semester
                      }
                      value={
                        semester
                      }
                    >
                      Semester {semester}
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
                statusFilter
              }
              onValueChange={
                setStatusFilter
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
                      key={
                        status
                      }
                      value={
                        status
                      }
                    >
                      {status
                        === MISSING
                        ? "Not Started"
                        : formatSyllabusFilterStatus(
                            status,
                          )}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="flex flex-col gap-2 pt-1 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-slate-500">
            Showing{" "}
            <strong className="text-slate-700">
              {filteredRows.length}
            </strong>{" "}
            of{" "}
            <strong className="text-slate-700">
              {courseRows.length}
            </strong>{" "}
            course(s).
          </p>

          <Button
            type="button"
            size="sm"
            variant="outline"
            disabled={
              !hasFilters
            }
            onClick={
              resetFilters
            }
          >
            Clear Filters
          </Button>
        </div>
      </CardHeader>

      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <Table className="min-w-[1000px]">
            <TableHeader className="bg-slate-50/80">
              <TableRow>
                <TableHead className="min-w-[280px]">
                  Course
                </TableHead>

                <TableHead className="min-w-[220px]">
                  Program / Cohort
                </TableHead>

                <TableHead className="min-w-[160px]">
                  Semester
                </TableHead>

                <TableHead className="min-w-[220px]">
                  Syllabus Status
                </TableHead>

                <TableHead className="min-w-[180px] text-right">
                  Action
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {filteredRows.length
                === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-44 text-center"
                  >
                    <div className="mx-auto max-w-lg">
                      <p className="font-semibold text-slate-700">
                        {courseRows.length
                          === 0
                          ? "No courses are currently assigned to this Instructor."
                          : "No assigned courses match the selected filters."}
                      </p>

                      <p className="mt-1 text-sm text-slate-500">
                        {courseRows.length
                          === 0
                          ? "The Administrator must create and activate a teaching assignment before syllabus work can begin."
                          : "Change or clear Major, Cohort, Semester, or Status."}
                      </p>

                      {hasFilters && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="mt-3"
                          onClick={
                            resetFilters
                          }
                        >
                          Clear Filters
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                filteredRows.map(
                  (item) => {
                    const action =
                      getSyllabusAction(
                        item,
                      )

                    return (
                      <TableRow
                        key={
                          logicalCourseKey(
                            item,
                          )
                        }
                        className="hover:bg-[#f8fbfb]"
                      >
                        <TableCell className="py-4">
                          <p className="font-mono text-xs font-bold text-[#007d84]">
                            {item.courseCode}
                          </p>

                          <p className="mt-1 font-semibold text-slate-800">
                            {item.courseName}
                          </p>
                        </TableCell>

                        <TableCell className="py-4">
                          <p className="font-medium text-slate-700">
                            {item.programCode
                              ? item.programCode
                                  .replace(/[-_\s]?20\d{2}$/i, "")
                                  .replace(/[-_\s]+$/g, "")
                              : "Program not assigned"}
                          </p>

                          <p className="mt-1 text-xs text-slate-500">
                            {item.cohortName
                              || "Cohort not assigned"}
                          </p>
                        </TableCell>

                        <TableCell className="py-4">
                          <p className="font-semibold text-slate-700">
                            Semester {item.semester}
                          </p>
                        </TableCell>

                        <TableCell className="py-4">
                          <div className="space-y-2">
                            <Badge
                              variant="outline"
                              className={
                                statusBadgeClass(
                                  item,
                                )
                              }
                            >
                              {statusLabel(
                                item,
                              )}
                            </Badge>

                            {item.syllabusId
                              !== null
                              && (
                              <p className="text-xs text-slate-500">
                                {item.syllabusVersionNumber
                                  !== null
                                  ? formatVersionLabel(
                                      item.syllabusVersionNumber,
                                    )
                                  : "Linked syllabus"}
                              </p>
                            )}
                          </div>
                        </TableCell>

                        <TableCell className="py-4">
                          <div className="flex justify-end">
                            <Button
                              type="button"
                              size="sm"
                              variant={
                                action.primary
                                  ? "default"
                                  : "outline"
                              }
                              disabled={
                                action.disabled
                              }
                              className={
                                action.primary
                                  ? "bg-[#007d84] text-white hover:bg-[#006d73]"
                                  : ""
                              }
                              onClick={() => {
                                if (
                                  !action.disabled
                                  && action.path
                                ) {
                                  navigate(
                                    action.path,
                                  )
                                }
                              }}
                            >
                              {action.label}

                              {!action.disabled && (
                                <ArrowRight className="size-3.5" />
                              )}
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    )
                  },
                )
              )}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  )
}