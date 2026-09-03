import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  Clock3,
  FilePenLine,
  FileText,
  GraduationCap,
  LoaderCircle,
  MapPin,
  RefreshCw,
  Search,
  ShieldCheck,
  UsersRound,
} from "lucide-react"

import {
  getMyActiveAssignments,
  type ClassSectionResponse,
} from "@/api/classSectionApi"
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

const ALL = "__all__"

type SectionTypeFilter =
  | typeof ALL
  | "THEORY"
  | "LAB"
  | "COMBINED"

type SyllabusFilter =
  | typeof ALL
  | "NOT_CREATED"
  | "DRAFT"
  | "IN_REVIEW"
  | "REVISION"
  | "APPROVED"

function normalizeStatus(
  value: unknown,
) {
  return String(value ?? "")
    .trim()
    .toUpperCase()
}

function syllabusStatusLabel(
  value: unknown,
) {
  const status =
    normalizeStatus(value)

  if (!status) {
    return "Not Started"
  }

  if (status === "DRAFT") {
    return "Draft"
  }

  if (status === "SUBMITTED") {
    return "Pending Department Review"
  }

  if (status === "UNDER_REVIEW") {
    return "Pending Dean Review"
  }

  if (status === "APPROVED") {
    return "Approved"
  }

  if (
    status === "REJECTED"
  ) {
    return "Returned for Revision"
  }

  if (
    status
      === "REVISION_REQUESTED"
  ) {
    return "Revision Required"
  }

  if (status === "ARCHIVED") {
    return "Archived"
  }

  return status
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(
      /\b\w/g,
      (character) =>
        character.toUpperCase(),
    )
}

function sectionTypeLabel(
  value: ClassSectionResponse["sectionType"],
) {
  if (value === "THEORY") {
    return "Theory"
  }

  if (value === "LAB") {
    return "Laboratory"
  }

  return "Combined"
}

function matchesSyllabusFilter(
  section: ClassSectionResponse,
  filter: SyllabusFilter,
) {
  if (filter === ALL) {
    return true
  }

  const status =
    normalizeStatus(
      section.syllabusStatus,
    )

  if (
    filter === "NOT_CREATED"
  ) {
    return (
      section.syllabusId === null
      || !status
    )
  }

  if (filter === "DRAFT") {
    return status === "DRAFT"
  }

  if (
    filter === "IN_REVIEW"
  ) {
    return [
      "SUBMITTED",
      "UNDER_REVIEW",
    ].includes(status)
  }

  if (
    filter === "REVISION"
  ) {
    return [
      "REJECTED",
      "REVISION_REQUESTED",
    ].includes(status)
  }

  return status === "APPROVED"
}

export default function InstructorAssignmentsPage() {
  const navigate =
    useNavigate()

  const [
    searchTerm,
    setSearchTerm,
  ] = useState("")

  const [
    academicYearFilter,
    setAcademicYearFilter,
  ] = useState(ALL)

  const [
    semesterFilter,
    setSemesterFilter,
  ] = useState(ALL)

  const [
    sectionTypeFilter,
    setSectionTypeFilter,
  ] =
    useState<SectionTypeFilter>(
      ALL,
    )

  const [
    syllabusFilter,
    setSyllabusFilter,
  ] =
    useState<SyllabusFilter>(
      ALL,
    )

  const query =
    useQuery({
      queryKey: [
        "my-active-assignments",
      ],
      queryFn:
        getMyActiveAssignments,
      staleTime: 30_000,
      refetchInterval: 60_000,
    })

  const assignments =
    query.data ?? []

  const academicYears =
    useMemo(
      () =>
        Array.from(
          new Set(
            assignments
              .map(
                (item) =>
                  item.academicYear,
              )
              .filter(Boolean),
          ),
        ).sort(
          (
            left,
            right,
          ) =>
            right.localeCompare(
              left,
            ),
        ),
      [assignments],
    )

  const semesters =
    useMemo(
      () =>
        Array.from(
          new Set(
            assignments.map(
              (item) =>
                item.semester,
            ),
          ),
        ).sort(
          (left, right) =>
            left - right,
        ),
      [assignments],
    )

  const summary =
    useMemo(() => {
      const uniqueCourses =
        new Set(
          assignments.map(
            (item) =>
              item.courseId,
          ),
        ).size

      const linkedSyllabi =
        assignments.filter(
          (item) =>
            item.syllabusId
            !== null,
        ).length

      const readyToCreate =
        assignments.filter(
          (item) =>
            item.readyForSyllabusCreation,
        ).length

      const approved =
        assignments.filter(
          (item) =>
            normalizeStatus(
              item.syllabusStatus,
            ) === "APPROVED",
        ).length

      return {
        uniqueCourses,
        activeSections:
          assignments.length,
        linkedSyllabi,
        readyToCreate,
        approved,
      }
    }, [
      assignments,
    ])

  const filteredAssignments =
    useMemo(() => {
      const keyword =
        searchTerm
          .trim()
          .toLowerCase()

      return assignments
        .filter(
          (item) => {
            if (
              academicYearFilter
                !== ALL
              && item.academicYear
                !== academicYearFilter
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
              sectionTypeFilter
                !== ALL
              && item.sectionType
                !== sectionTypeFilter
            ) {
              return false
            }

            if (
              !matchesSyllabusFilter(
                item,
                syllabusFilter,
              )
            ) {
              return false
            }

            if (!keyword) {
              return true
            }

            return [
              item.courseCode,
              item.courseName,
              item.instructorName,
              item.academicYear,
              `semester ${item.semester}`,
              `group ${item.groupNumber}`,
              item.room,
              item.schedule,
              item.sectionType,
              item.syllabusStatus,
            ]
              .filter(Boolean)
              .join(" ")
              .toLowerCase()
              .includes(keyword)
          },
        )
        .slice()
        .sort(
          (
            left,
            right,
          ) => {
            const yearCompare =
              right.academicYear
                .localeCompare(
                  left.academicYear,
                )

            if (yearCompare !== 0) {
              return yearCompare
            }

            if (
              left.semester
              !== right.semester
            ) {
              return (
                left.semester
                - right.semester
              )
            }

            const courseCompare =
              left.courseCode
                .localeCompare(
                  right.courseCode,
                  "en",
                  {
                    numeric: true,
                  },
                )

            if (
              courseCompare
              !== 0
            ) {
              return courseCompare
            }

            return (
              left.groupNumber
              - right.groupNumber
            )
          },
        )
    }, [
      academicYearFilter,
      assignments,
      searchTerm,
      sectionTypeFilter,
      semesterFilter,
      syllabusFilter,
    ])

  const hasFilters =
    Boolean(
      searchTerm.trim(),
    )
    || academicYearFilter
      !== ALL
    || semesterFilter
      !== ALL
    || sectionTypeFilter
      !== ALL
    || syllabusFilter
      !== ALL

  const clearFilters = () => {
    setSearchTerm("")
    setAcademicYearFilter(ALL)
    setSemesterFilter(ALL)
    setSectionTypeFilter(ALL)
    setSyllabusFilter(ALL)
  }

  if (query.isLoading) {
    return (
      <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
        <div className="h-36 animate-pulse rounded-2xl border border-slate-200 bg-white" />

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          {Array.from({
            length: 5,
          }).map(
            (_, index) => (
              <div
                key={index}
                className="h-28 animate-pulse rounded-xl border border-slate-200 bg-white"
              />
            ),
          )}
        </div>

        <div className="h-80 animate-pulse rounded-xl border border-slate-200 bg-white" />
      </div>
    )
  }

  if (query.isError) {
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
                  Unable to load Teaching Assignments
                </p>

                <p className="mt-1 text-sm leading-6 text-slate-600">
                  The system could not load the active teaching assignments for this Instructor account.
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Confirm that the account is linked to an instructor profile and that active class-section assignments exist.
                </p>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4 gap-2 bg-white"
                  onClick={() =>
                    void query.refetch()
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

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <GraduationCap className="size-5 text-[#007d84]" />

              <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Instructor Teaching Assignment
              </span>
            </div>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              My Teaching Assignments
            </h1>

            <p className="mt-1 max-w-4xl text-sm leading-6 text-[#687f89]">
              Review your active class-section assignments and open the correct syllabus action for each assigned course, academic year, semester, and teaching group.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                navigate(
                  "/instructor/syllabus",
                )
              }
            >
              <FileText className="size-4" />
              My Syllabuses
            </Button>

            <Button
              type="button"
              variant="outline"
              disabled={
                query.isFetching
              }
              onClick={() =>
                void query.refetch()
              }
            >
              {query.isFetching ? (
                <LoaderCircle className="size-4 animate-spin" />
              ) : (
                <RefreshCw className="size-4" />
              )}
              Refresh
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
              This page shows only active class sections assigned to the signed-in Instructor. Course, academic year, semester, group, and instructor are read-only; assignment changes are managed by the Administrator.
            </p>
          </div>
        </div>

        <Badge
          variant="outline"
          className="shrink-0 border-[#cfe1e4] bg-white text-[#007d84]"
        >
          Read-only assignment data
        </Badge>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <MetricCard
          label="Assigned Courses"
          value={
            summary.uniqueCourses
          }
          helper="Unique courses in active assignments"
          icon={
            <BookOpen className="size-5" />
          }
        />

        <MetricCard
          label="Active Sections"
          value={
            summary.activeSections
          }
          helper="Class sections currently assigned"
          icon={
            <CalendarDays className="size-5" />
          }
        />

        <MetricCard
          label="Ready to Create"
          value={
            summary.readyToCreate
          }
          helper="Assignments without a linked syllabus"
          icon={
            <FilePenLine className="size-5" />
          }
          tone={
            summary.readyToCreate
              > 0
              ? "warning"
              : "default"
          }
        />

        <MetricCard
          label="Linked Syllabi"
          value={
            summary.linkedSyllabi
          }
          helper="Sections linked to a syllabus version"
          icon={
            <FileText className="size-5" />
          }
        />

        <MetricCard
          label="Approved"
          value={
            summary.approved
          }
          helper="Sections with an approved syllabus"
          icon={
            <CheckCircle2 className="size-5" />
          }
          tone="success"
        />
      </section>

      <Card className="border border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
            <div>
              <CardTitle className="text-base font-bold text-[#17343d]">
                Assignment Filters
              </CardTitle>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Academic Year and Semester are teaching-assignment fields. They are intentionally separate from curriculum Cohort.
              </p>
            </div>

            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={!hasFilters}
              onClick={clearFilters}
            >
              Clear Filters
            </Button>
          </div>
        </CardHeader>

        <CardContent className="p-5">
          <div className="grid gap-3 xl:grid-cols-[minmax(280px,1fr)_180px_160px_180px_230px]">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

              <Input
                value={searchTerm}
                onChange={(event) =>
                  setSearchTerm(
                    event.target.value,
                  )
                }
                placeholder="Course, room, schedule..."
                className="pl-9"
              />
            </div>

            <Select
              value={
                academicYearFilter
              }
              onValueChange={
                setAcademicYearFilter
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Academic Year" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Academic Years
                </SelectItem>

                {academicYears.map(
                  (year) => (
                    <SelectItem
                      key={year}
                      value={year}
                    >
                      {year}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>

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

                {semesters.map(
                  (semester) => (
                    <SelectItem
                      key={semester}
                      value={String(
                        semester,
                      )}
                    >
                      Semester {semester}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>

            <Select
              value={
                sectionTypeFilter
              }
              onValueChange={(value) =>
                setSectionTypeFilter(
                  value as SectionTypeFilter,
                )
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Section Type" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Section Types
                </SelectItem>

                <SelectItem value="THEORY">
                  Theory
                </SelectItem>

                <SelectItem value="LAB">
                  Laboratory
                </SelectItem>

                <SelectItem value="COMBINED">
                  Combined
                </SelectItem>
              </SelectContent>
            </Select>

            <Select
              value={
                syllabusFilter
              }
              onValueChange={(value) =>
                setSyllabusFilter(
                  value as SyllabusFilter,
                )
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Syllabus Status" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Syllabus States
                </SelectItem>

                <SelectItem value="NOT_CREATED">
                  Not Started
                </SelectItem>

                <SelectItem value="DRAFT">
                  Draft
                </SelectItem>

                <SelectItem value="IN_REVIEW">
                  In Review
                </SelectItem>

                <SelectItem value="REVISION">
                  Revision Required
                </SelectItem>

                <SelectItem value="APPROVED">
                  Approved
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <p className="mt-3 text-xs text-slate-500">
            Showing{" "}
            <strong className="text-slate-700">
              {filteredAssignments.length}
            </strong>{" "}
            of{" "}
            <strong className="text-slate-700">
              {assignments.length}
            </strong>{" "}
            active class-section assignments.
          </p>
        </CardContent>
      </Card>

      <Card className="overflow-hidden border border-slate-200 bg-white shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-4">
          <CardTitle className="text-base font-bold text-[#17343d]">
            Active Teaching Assignments
          </CardTitle>

          <p className="text-xs leading-5 text-slate-500">
            Use the syllabus action only for the assignment shown in the row. The backend verifies that the selected assignment belongs to this Instructor and matches its Program, Cohort, and Course.
          </p>
        </CardHeader>

        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <Table className="min-w-[1320px]">
              <TableHeader className="bg-slate-50/80">
                <TableRow>
                  <TableHead className="min-w-[270px]">
                    Course
                  </TableHead>

                  <TableHead className="min-w-[170px]">
                    Academic Term
                  </TableHead>

                  <TableHead className="min-w-[165px]">
                    Section
                  </TableHead>

                  <TableHead className="min-w-[190px]">
                    Schedule / Room
                  </TableHead>

                  <TableHead className="min-w-[135px]">
                    Capacity
                  </TableHead>

                  <TableHead className="min-w-[220px]">
                    Syllabus
                  </TableHead>

                  <TableHead className="min-w-[170px] text-right">
                    Action
                  </TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {filteredAssignments.length
                  === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="h-44 text-center"
                    >
                      <div className="mx-auto max-w-lg">
                        <p className="font-semibold text-slate-700">
                          {assignments.length
                            === 0
                            ? "No active class sections are assigned to this Instructor."
                            : "No class sections match the selected filters."}
                        </p>

                        <p className="mt-1 text-sm leading-6 text-slate-500">
                          {assignments.length
                            === 0
                            ? "Teaching assignments must be created and activated by the Administrator before an Instructor can create a syllabus."
                            : "Clear the filters or search for another assigned course."}
                        </p>

                        {hasFilters && (
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="mt-3"
                            onClick={
                              clearFilters
                            }
                          >
                            Clear Filters
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredAssignments.map(
                    (item) => (
                      <AssignmentRow
                        key={item.id}
                        item={item}
                        onNavigate={
                          navigate
                        }
                      />
                    ),
                  )
                )}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col gap-2 border-t border-slate-100 bg-slate-50/50 px-4 py-3 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
            <span>
              Assignment metadata is read-only for Instructor accounts.
            </span>

            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-8 text-[#007d84] hover:bg-[#eef8f8] hover:text-[#006b72]"
              onClick={() =>
                navigate(
                  "/instructor/syllabus",
                )
              }
            >
              Open My Syllabuses
              <ArrowRight className="size-3.5" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function AssignmentRow({
  item,
  onNavigate,
}: {
  item: ClassSectionResponse
  onNavigate: (
    path: string,
  ) => void
}) {
  const action =
    getSyllabusAction(item)

  return (
    <TableRow className="align-top hover:bg-[#f8fbfb]">
      <TableCell className="py-4">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-xs font-bold text-[#007d84]">
              {item.courseCode}
            </span>

            <Badge
              variant="outline"
              className="border-slate-200 bg-white text-slate-600"
            >
              {sectionTypeLabel(
                item.sectionType,
              )}
            </Badge>
          </div>

          <p className="mt-1 max-w-[260px] font-semibold text-slate-800">
            {item.courseName}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {item.programCode && item.cohortName
              ? `${item.programCode} · ${item.cohortName}`
              : "Curriculum context missing — contact the Administrator"}
          </p>
        </div>
      </TableCell>

      <TableCell className="py-4">
        <p className="font-semibold text-slate-700">
          Semester {item.semester}
        </p>

        <p className="mt-1 text-xs text-slate-500">
          {item.academicYear}
        </p>
      </TableCell>

      <TableCell className="py-4">
        <p className="font-semibold text-slate-700">
          Group {item.groupNumber}
        </p>

        <p className="mt-1 text-xs text-slate-500">
          Section ID #{item.id}
        </p>

        {item.labGroup !== null && (
          <p className="mt-1 text-xs text-slate-500">
            Lab Group {item.labGroup}
          </p>
        )}
      </TableCell>

      <TableCell className="py-4">
        <div className="space-y-1.5">
          <p className="flex items-start gap-2 text-sm text-slate-700">
            <Clock3 className="mt-0.5 size-3.5 shrink-0 text-slate-400" />
            <span>
              {item.schedule
                || "Schedule not recorded"}
            </span>
          </p>

          <p className="flex items-start gap-2 text-xs text-slate-500">
            <MapPin className="mt-0.5 size-3.5 shrink-0 text-slate-400" />
            <span>
              {item.room
                || "Room not recorded"}
            </span>
          </p>
        </div>
      </TableCell>

      <TableCell className="py-4">
        <div className="flex items-center gap-2">
          <UsersRound className="size-4 text-slate-400" />

          <span className="font-medium text-slate-700">
            {item.maxStudents
              ?? "Not specified"}
          </span>
        </div>
      </TableCell>

      <TableCell className="py-4">
        <div className="space-y-2">
          <SyllabusStatusBadge
            status={
              item.syllabusStatus
            }
          />

          {item.syllabusId !== null ? (
            <p className="text-xs text-slate-500">
              {item.syllabusVersionNumber
                !== null
                ? `Version ${item.syllabusVersionNumber}`
                : "Linked syllabus"}
            </p>
          ) : (
            <p className="text-xs text-slate-400">
              No syllabus linked yet
            </p>
          )}

          {item.readyForSyllabusCreation && (
            <p className="text-[10px] font-semibold text-[#007d84]">
              Ready for syllabus creation
            </p>
          )}

          {!item.isActive && (
            <p className="text-[10px] font-semibold text-rose-700">
              Assignment inactive
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
            className={
              action.primary
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
            <ArrowRight className="size-3.5" />
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}

function SyllabusStatusBadge({
  status,
}: {
  status: string | null
}) {
  const normalized =
    normalizeStatus(status)

  if (!normalized) {
    return (
      <Badge
        variant="outline"
        className="border-slate-200 bg-slate-50 text-slate-600"
      >
        Not Started
      </Badge>
    )
  }

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
        {syllabusStatusLabel(
          normalized,
        )}
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="border-slate-200 bg-slate-50 text-slate-600"
    >
      {syllabusStatusLabel(
        normalized,
      )}
    </Badge>
  )
}

function getSyllabusAction(
  item: ClassSectionResponse,
) {
  const base =
    "/instructor/syllabus"

  const status =
    normalizeStatus(
      item.syllabusStatus,
    )

  if (
    item.readyForSyllabusCreation
    && item.syllabusId === null
  ) {
    const params = new URLSearchParams()
    if (item.programId) params.set("programId", String(item.programId))
    if (item.cohortId) params.set("cohortId", String(item.cohortId))
    params.set("courseId", String(item.courseId))
    params.set("assignmentId", String(item.id))

    return {
      label: "Create Syllabus",
      path: `${base}?${params.toString()}`,
      primary: true,
    }
  }

  if (
    item.syllabusId !== null
    && (
      status === "DRAFT"
      || status === "REJECTED"
      || status
        === "REVISION_REQUESTED"
    )
  ) {
    return {
      label:
        status === "DRAFT"
          ? "Continue Draft"
          : "Revise Syllabus",
      path:
        `${base}/${item.syllabusId}/edit`,
      primary: true,
    }
  }

  if (
    item.syllabusId !== null
    && status === "APPROVED"
  ) {
    return {
      label: "View Approved",
      path:
        `${base}/${item.syllabusId}`,
      primary: false,
    }
  }

  if (
    item.syllabusId !== null
  ) {
    return {
      label: "View Progress",
      path:
        `${base}/${item.syllabusId}`,
      primary: false,
    }
  }

  return {
    label: "My Syllabuses",
    path: base,
    primary: false,
  }
}

function MetricCard({
  label,
  value,
  helper,
  icon,
  tone = "default",
}: {
  label: string
  value: number
  helper: string
  icon: React.ReactNode
  tone?:
    | "default"
    | "success"
    | "warning"
}) {
  const iconClass =
    tone === "success"
      ? "bg-emerald-50 text-emerald-700"
      : tone === "warning"
        ? "bg-amber-50 text-amber-700"
        : "bg-[#eef8f8] text-[#007d84]"

  return (
    <Card className="border border-slate-200 bg-white shadow-sm">
      <CardContent className="flex min-h-[120px] items-start justify-between gap-4 p-5">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-3xl font-bold text-slate-950">
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {helper}
          </p>
        </div>

        <span className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${iconClass}`}>
          {icon}
        </span>
      </CardContent>
    </Card>
  )
}
