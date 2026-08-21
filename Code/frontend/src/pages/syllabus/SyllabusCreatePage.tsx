import {
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom"
import { useMemo, useState } from "react"
import axios from "axios"
import { useQuery } from "@tanstack/react-query"
import {
  AlertTriangle,
  ArrowLeft,
  BookOpen,
  Copy,
  FilePlus2,
  GraduationCap,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react"

import { courseProgramApi } from "@/api/courseProgramApi"
import {
  getMyActiveAssignments,
  type ClassSectionResponse,
} from "@/api/classSectionApi"
import { Button } from "@/components/ui/button"
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
import SyllabusForm from "@/components/syllabus/SyllabusForm"
import { useCloneSyllabus } from "@/hooks/useCloneSyllabus"
import { useCreateSyllabus } from "@/hooks/useCreateSyllabus"
import { useSyllabuses } from "@/hooks/useSyllabuses"
import { useSyllabus } from "@/hooks/useSyllabus"
import { useAuthStore } from "@/store/authStore"
import type {
  CreateSyllabusRequest,
} from "@/types/syllabus"

const assignmentSemester = (
  semester: number,
) => `HK${semester}`

const normalizeRole = (
  value: unknown,
) =>
  String(value ?? "")
    .replace(/^ROLE_/i, "")
    .trim()
    .toUpperCase()

const normalizeStatus = (
  value: unknown,
) =>
  String(value ?? "")
    .trim()
    .toUpperCase()

const sectionTypeLabel = (
  value:
    ClassSectionResponse["sectionType"],
) => {
  if (value === "THEORY") {
    return "Theory"
  }

  if (value === "LAB") {
    return "Laboratory"
  }

  return "Combined"
}

const errorMessage = (
  error: unknown,
  fallback: string,
) => {
  if (
    axios.isAxiosError<{
      message?: string
      error?: string
    }>(error)
  ) {
    return (
      error.response?.data?.message
      || error.response?.data?.error
      || fallback
    )
  }

  if (error instanceof Error) {
    return (
      error.message
      || fallback
    )
  }

  return fallback
}

type InstructorStartMode =
  | "BLANK"
  | "CLONE"

export default function SyllabusCreatePage() {
  const location =
    useLocation()

  const navigate =
    useNavigate()

  const [searchParams] =
    useSearchParams()

  const autoImportAfterCreate =
    searchParams.get("import") === "1"

  const user =
    useAuthStore(
      (state) =>
        state.user,
    )

  const role =
    normalizeRole(
      user?.role,
    )

  const isInstructor =
    role === "INSTRUCTOR"

  const isAdmin =
    role === "ADMIN"

  const courseProgramId =
    Number(
      searchParams.get(
        "courseProgramId",
      ),
    )

  const programId =
    Number(
      searchParams.get(
        "programId",
      ),
    )

  const cohortId =
    Number(
      searchParams.get(
        "cohortId",
      ),
    )

  const requestedClassSectionId =
    Number(
      searchParams.get(
        "classSectionId",
      ),
    )

  const basePath =
    location.pathname.includes(
      "/syllabus",
    )
      ? location.pathname.slice(
          0,
          location.pathname.indexOf(
            "/syllabus",
          ),
        )
      : "/admin"

  const createMutation =
    useCreateSyllabus()

  const cloneMutation =
    useCloneSyllabus()

  const [
    selectedAssignmentId,
    setSelectedAssignmentId,
  ] =
    useState<number | null>(
      null,
    )

  const [
    startMode,
    setStartMode,
  ] =
    useState<InstructorStartMode>(
      "BLANK",
    )

  const [
    selectedSourceId,
    setSelectedSourceId,
  ] =
    useState<number | null>(
      null,
    )

  const {
    data:
      myAssignments = [],
    isLoading:
      assignmentsLoading,
    isError:
      assignmentsError,
    refetch:
      refetchAssignments,
  } = useQuery({
    queryKey: [
      "my-active-assignments",
      "syllabus-create",
    ],
    queryFn:
      getMyActiveAssignments,
    enabled: isInstructor,
    staleTime: 30_000,
  })

  const {
    data:
      allSyllabuses = [],
    isLoading:
      syllabusesLoading,
  } =
    useSyllabuses()

  const {
    data:
      courseProgram,
  } = useQuery({
    queryKey: [
      "course-program",
      courseProgramId,
    ],
    queryFn: () =>
      courseProgramApi.getById(
        courseProgramId,
      ),
    enabled:
      isAdmin
      && Number.isFinite(
        courseProgramId,
      )
      && courseProgramId > 0,
  })

  const {
    data:
      sourceSyllabus,
    isFetching:
      sourceLoading,
  } = useSyllabus(
    !isInstructor
      ? selectedSourceId
      : null,
  )

  const eligibleAssignments =
    useMemo(
      () =>
        myAssignments.filter(
          (assignment) =>
            assignment.isActive
            && assignment
              .readyForSyllabusCreation
            && assignment
              .syllabusId
              === null,
        ),
      [myAssignments],
    )

  const requestedAssignment =
    Number.isFinite(
      requestedClassSectionId,
    )
      && requestedClassSectionId
        > 0
      ? eligibleAssignments.find(
          (assignment) =>
            assignment.id
            === requestedClassSectionId,
        )
      : undefined

  const effectiveAssignmentId =
    selectedAssignmentId
    ?? requestedAssignment?.id
    ?? eligibleAssignments[0]?.id
    ?? null

  const selectedAssignment =
    eligibleAssignments.find(
      (assignment) =>
        assignment.id
        === effectiveAssignmentId,
    )

  const cloneSources =
    useMemo(() => {
      if (!selectedAssignment) {
        return []
      }

      return allSyllabuses
        .filter(
          (syllabus) => {
            if (
              syllabus.courseId
              !== selectedAssignment
                .courseId
            ) {
              return false
            }

            const status =
              normalizeStatus(
                syllabus.status,
              )

            if (
              status !== "DRAFT"
            ) {
              return true
            }

            return (
              syllabus.createdById
              === user?.userId
            )
          },
        )
        .slice()
        .sort(
          (
            left,
            right,
          ) => {
            const leftVersion =
              Number(
                left.versionNumber
                ?? 0,
              )

            const rightVersion =
              Number(
                right.versionNumber
                ?? 0,
              )

            if (
              leftVersion
              !== rightVersion
            ) {
              return (
                rightVersion
                - leftVersion
              )
            }

            return (
              right.id
              - left.id
            )
          },
        )
    }, [
      allSyllabuses,
      selectedAssignment,
      user?.userId,
    ])

  const selectedCloneSource =
    cloneSources.find(
      (syllabus) =>
        syllabus.id
        === selectedSourceId,
    )

  const createInstructorDraft =
    () => {
      if (!selectedAssignment) {
        return
      }

      if (
        startMode === "CLONE"
      ) {
        if (
          !selectedCloneSource
        ) {
          alert(
            "Select a previous syllabus version to clone.",
          )
          return
        }

        cloneMutation.mutate(
          {
            id:
              selectedCloneSource.id,
            request: {
              classSectionId:
                selectedAssignment.id,
              changeSummary:
                `Cloned from ${selectedCloneSource.versionLabel || `v${selectedCloneSource.versionNumber}`} for Semester ${selectedAssignment.semester} · ${selectedAssignment.academicYear}`,
            },
          },
          {
            onSuccess: (
              cloned,
            ) => {
              navigate(
                `/instructor/syllabus/${cloned.id}/editor`,
              )
            },

            onError: (
              error: unknown,
            ) => {
              alert(
                errorMessage(
                  error,
                  "Unable to clone the previous syllabus into this teaching assignment.",
                ),
              )
            },
          },
        )

        return
      }

      const payload:
        CreateSyllabusRequest = {
          classSectionId:
            selectedAssignment.id,
          courseId:
            selectedAssignment
              .courseId,
          versionNumber: 1,
          versionLabel: "v1.0",
          academicYear:
            selectedAssignment
              .academicYear,
          semester:
            assignmentSemester(
              selectedAssignment
                .semester,
            ),
          changeSummary:
            "Initial Draft",
          notes: "",
          clos: [],
          topics: [],
          assessments: [],
        }

      createMutation.mutate(
        payload,
        {
          onSuccess: (
            created,
          ) => {
            navigate(
              `/instructor/syllabus/${created.id}/editor`,
            )
          },

          onError: (
            error: unknown,
          ) => {
            alert(
              errorMessage(
                error,
                "Unable to create the Draft. Check the teaching assignment, academic year, and semester.",
              ),
            )
          },
        },
      )
    }

  const handleAdminCreate =
    (
      values:
        CreateSyllabusRequest,
    ) => {
      const safeValues = {
        ...values,
      }

      delete safeValues.createdBy

      const payload:
        CreateSyllabusRequest = {
          ...safeValues,
          courseId:
            Number(
              values.courseId,
            ),
          versionNumber:
            Number(
              values
                .versionNumber
              || 1,
            ),
          versionLabel:
            values.versionLabel
            || "v1.0",
          academicYear:
            values.academicYear
              ?.trim()
            || "",
          semester:
            values.semester
              ?.trim()
            || "",
          courseProgramId:
            courseProgramId > 0
              ? courseProgramId
              : values
                  .courseProgramId,
          changeSummary:
            values.changeSummary
            ?? "",
          notes:
            values.notes
            ?? "",
          clos:
            values.clos
            ?? [],
          topics:
            values.topics
            ?? [],
          assessments:
            values.assessments
            ?? [],
        }

      createMutation.mutate(
        payload,
        {
          onSuccess: (
            created,
          ) => {
            if (
              programId > 0
              && cohortId > 0
            ) {
              navigate(
                `${basePath}/programs/${programId}?cohortId=${cohortId}`,
              )
              return
            }

            navigate(
              `${basePath}/syllabus/${created.id}/editor${
                autoImportAfterCreate ? "?import=1" : ""
              }`,
            )
          },

          onError: (
            error: unknown,
          ) => {
            alert(
              errorMessage(
                error,
                "Unable to create the syllabus Draft.",
              ),
            )
          },
        },
      )
    }

  if (
    !isInstructor
    && !isAdmin
  ) {
    return (
      <div className="mx-auto max-w-2xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-800">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 shrink-0" />

          <div>
            <h1 className="font-semibold">
              Syllabus creation is not available for this role
            </h1>

            <p className="mt-1 text-sm leading-6">
              Syllabuses are created by an assigned Instructor or by an Administrator for controlled data administration.
            </p>

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-4 bg-white"
              onClick={() =>
                navigate(-1)
              }
            >
              <ArrowLeft className="size-4" />
              Back
            </Button>
          </div>
        </div>
      </div>
    )
  }

  if (isInstructor) {
    return (
      <div className="mx-auto w-full max-w-[1280px] space-y-6 pb-10">
        <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
          <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

          <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <GraduationCap className="size-5 text-[#007d84]" />

                <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                  Instructor / New Syllabus
                </span>
              </div>

              <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
                Create Syllabus Draft
              </h1>

              <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
                Choose one active teaching assignment, create a blank Draft or clone a previous syllabus for the same course, then complete CLOs, CLO–PLO mapping, teaching content, assessments, and reading materials in the Syllabus Editor.
              </p>
            </div>

            <Button
              type="button"
              variant="outline"
              onClick={() =>
                navigate(
                  "/instructor/class-sections",
                )
              }
            >
              <ArrowLeft className="size-4" />
              Back to Class Sections
            </Button>
          </div>
        </section>

        <section className="rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4 shadow-sm">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

            <div>
              <p className="font-semibold text-[#17343d]">
                Assignment-controlled creation
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Course, academic year, semester, section, and Instructor come from the teaching assignment and cannot be changed here. Curriculum Cohort is a separate concept and is not substituted with Academic Year.
              </p>
            </div>
          </div>
        </section>

        {assignmentsLoading ? (
          <Card className="border-slate-200 shadow-sm">
            <CardContent className="flex h-48 items-center justify-center text-sm text-slate-500">
              <LoaderCircle className="mr-2 size-5 animate-spin" />
              Loading eligible teaching assignments...
            </CardContent>
          </Card>
        ) : assignmentsError ? (
          <Card className="border-rose-200 bg-rose-50/70 shadow-sm">
            <CardContent className="p-6">
              <div className="flex items-start gap-3 text-rose-800">
                <AlertTriangle className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    Unable to load teaching assignments
                  </p>

                  <p className="mt-1 text-sm">
                    Confirm that this account is linked to an Instructor profile and that the assignment is active.
                  </p>

                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-3 bg-white"
                    onClick={() =>
                      void refetchAssignments()
                    }
                  >
                    Try Again
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : eligibleAssignments.length === 0 ? (
          <Card className="border-amber-200 bg-amber-50/60 shadow-sm">
            <CardContent className="p-6">
              <div className="flex items-start gap-3">
                <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-700" />

                <div>
                  <p className="font-semibold text-amber-950">
                    No assignment is ready for syllabus creation
                  </p>

                  <p className="mt-1 text-sm leading-6 text-amber-800">
                    Every active assignment is already linked to a syllabus, or no active assignment is available. Use My Syllabuses to continue an existing Draft.
                  </p>

                  <div className="mt-4 flex flex-wrap gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="bg-white"
                      onClick={() =>
                        navigate(
                          "/instructor/syllabus",
                        )
                      }
                    >
                      My Syllabuses
                    </Button>

                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="bg-white"
                      onClick={() =>
                        navigate(
                          "/instructor/class-sections",
                        )
                      }
                    >
                      Class Sections
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ) : (
          <>
            <Card className="border-slate-200 shadow-sm">
              <CardHeader className="border-b border-slate-100">
                <CardTitle className="flex items-center gap-2 text-base text-[#17343d]">
                  <BookOpen className="size-5 text-[#007d84]" />
                  1. Teaching Assignment
                </CardTitle>

                <p className="text-xs leading-5 text-slate-500">
                  Select the exact active assignment for which the new syllabus will be created.
                </p>
              </CardHeader>

              <CardContent className="space-y-5 p-6">
                <Select
                  value={
                    effectiveAssignmentId
                      ? String(
                          effectiveAssignmentId,
                        )
                      : undefined
                  }
                  onValueChange={(
                    value,
                  ) => {
                    setSelectedAssignmentId(
                      Number(value),
                    )
                    setSelectedSourceId(
                      null,
                    )
                    setStartMode(
                      "BLANK",
                    )
                  }}
                >
                  <SelectTrigger className="h-11 bg-white">
                    <SelectValue placeholder="Select teaching assignment" />
                  </SelectTrigger>

                  <SelectContent>
                    {eligibleAssignments.map(
                      (assignment) => (
                        <SelectItem
                          key={assignment.id}
                          value={String(
                            assignment.id,
                          )}
                        >
                          {assignment.courseCode} — Semester {assignment.semester} — {assignment.academicYear} — Group {assignment.groupNumber}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>

                {selectedAssignment && (
                  <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                    <ContextValue
                      label="Course"
                      value={`${selectedAssignment.courseCode} — ${selectedAssignment.courseName}`}
                    />

                    <ContextValue
                      label="Academic Year"
                      value={
                        selectedAssignment
                          .academicYear
                      }
                    />

                    <ContextValue
                      label="Semester"
                      value={`Semester ${selectedAssignment.semester}`}
                    />

                    <ContextValue
                      label="Class Section"
                      value={`Group ${selectedAssignment.groupNumber} · ${sectionTypeLabel(selectedAssignment.sectionType)}`}
                    />

                    <ContextValue
                      label="Instructor"
                      value={
                        selectedAssignment
                          .instructorName
                      }
                    />

                    <ContextValue
                      label="Schedule / Room"
                      value={
                        [
                          selectedAssignment
                            .schedule,
                          selectedAssignment
                            .room,
                        ]
                          .filter(Boolean)
                          .join(" · ")
                        || "Not recorded"
                      }
                    />
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="border-slate-200 shadow-sm">
              <CardHeader className="border-b border-slate-100">
                <CardTitle className="flex items-center gap-2 text-base text-[#17343d]">
                  <FilePlus2 className="size-5 text-[#007d84]" />
                  2. Start From
                </CardTitle>

                <p className="text-xs leading-5 text-slate-500">
                  A blank Draft starts with assignment context only. Clone copies the previous syllabus content into this new assignment using the protected clone workflow.
                </p>
              </CardHeader>

              <CardContent className="space-y-5 p-6">
                <div className="grid gap-3 md:grid-cols-2">
                  <button
                    type="button"
                    className={
                      startMode
                        === "BLANK"
                        ? "rounded-xl border-2 border-[#007d84] bg-[#f3fbfb] p-4 text-left"
                        : "rounded-xl border border-slate-200 bg-white p-4 text-left hover:border-slate-300"
                    }
                    onClick={() => {
                      setStartMode(
                        "BLANK",
                      )
                      setSelectedSourceId(
                        null,
                      )
                    }}
                  >
                    <div className="flex items-start gap-3">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-[#eaf7f7] text-[#007d84]">
                        <FilePlus2 className="size-4" />
                      </span>

                      <div>
                        <p className="font-semibold text-slate-900">
                          Blank syllabus
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                          Create Draft v1.0 and complete the syllabus in the structured editor.
                        </p>
                      </div>
                    </div>
                  </button>

                  <button
                    type="button"
                    className={
                      startMode
                        === "CLONE"
                        ? "rounded-xl border-2 border-[#007d84] bg-[#f3fbfb] p-4 text-left"
                        : "rounded-xl border border-slate-200 bg-white p-4 text-left hover:border-slate-300"
                    }
                    onClick={() =>
                      setStartMode(
                        "CLONE",
                      )
                    }
                  >
                    <div className="flex items-start gap-3">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-700">
                        <Copy className="size-4" />
                      </span>

                      <div>
                        <p className="font-semibold text-slate-900">
                          Clone previous syllabus
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                          Reuse a previous version of the same course for this target assignment.
                        </p>
                      </div>
                    </div>
                  </button>
                </div>

                {startMode
                  === "CLONE" && (
                  <div className="rounded-xl border border-blue-100 bg-blue-50/40 p-4">
                    <label className="mb-2 block text-[10px] font-bold uppercase tracking-[0.1em] text-slate-500">
                      Previous syllabus version
                    </label>

                    {syllabusesLoading ? (
                      <div className="flex h-10 items-center text-sm text-slate-500">
                        <LoaderCircle className="mr-2 size-4 animate-spin" />
                        Loading available versions...
                      </div>
                    ) : cloneSources.length === 0 ? (
                      <p className="text-sm leading-6 text-slate-500">
                        No previous syllabus version for this course is available to clone. Choose Blank syllabus.
                      </p>
                    ) : (
                      <Select
                        value={
                          selectedSourceId
                            ? String(
                                selectedSourceId,
                              )
                            : undefined
                        }
                        onValueChange={(
                          value,
                        ) =>
                          setSelectedSourceId(
                            Number(value),
                          )
                        }
                      >
                        <SelectTrigger className="bg-white">
                          <SelectValue placeholder="Select a previous syllabus version" />
                        </SelectTrigger>

                        <SelectContent>
                          {cloneSources.map(
                            (syllabus) => (
                              <SelectItem
                                key={syllabus.id}
                                value={String(
                                  syllabus.id,
                                )}
                              >
                                {syllabus.versionLabel || `v${syllabus.versionNumber}`} · {syllabus.academicYear} · {syllabus.semester || "No semester"} · {normalizeStatus(syllabus.status)}
                              </SelectItem>
                            ),
                          )}
                        </SelectContent>
                      </Select>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-semibold text-[#17343d]">
                    Next step: Structured Syllabus Editor
                  </p>

                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    The Draft editor contains General Information, CLOs, CLO–PLO mapping with I/D/A contribution levels, teaching content, assessment, reading list, PDF preview, validation, and submission.
                  </p>
                </div>

                <Button
                  type="button"
                  size="lg"
                  className="shrink-0 bg-[#007d84] text-white hover:bg-[#006d73]"
                  disabled={
                    !selectedAssignment
                    || createMutation
                      .isPending
                    || cloneMutation
                      .isPending
                    || (
                      startMode
                        === "CLONE"
                      && !selectedCloneSource
                    )
                  }
                  onClick={
                    createInstructorDraft
                  }
                >
                  {createMutation
                    .isPending
                    || cloneMutation
                      .isPending ? (
                    <>
                      <LoaderCircle className="size-4 animate-spin" />
                      Creating Draft...
                    </>
                  ) : startMode
                    === "CLONE" ? (
                    <>
                      <Copy className="size-4" />
                      Clone & Open Editor
                    </>
                  ) : (
                    <>
                      <FilePlus2 className="size-4" />
                      Create Draft & Open Editor
                    </>
                  )}
                </Button>
              </div>
            </section>
          </>
        )}
      </div>
    )
  }

  let initialData:
    CreateSyllabusRequest
    | undefined

  if (courseProgram && !sourceSyllabus) {
    initialData = {
      courseProgramId: courseProgram.id,
      courseId: courseProgram.courseId,
      versionNumber: 1,
      versionLabel: "v1.0",
      academicYear: "",
      semester: courseProgram.semesterSuggest
        ? String(courseProgram.semesterSuggest)
        : "",
      courseTypes: courseProgram.courseTypeName
        ? JSON.stringify([courseProgram.courseTypeName])
        : JSON.stringify([]),
      changeSummary: "Initial Draft",
      notes: "",
      clos: [],
      topics: [],
      assessments: [],
    }
  }

  if (sourceSyllabus) {
    initialData = {
      courseId: sourceSyllabus.courseId,
      versionNumber: 1,
      versionLabel: "v1.0",
      academicYear: sourceSyllabus.academicYear ?? "",
      semester: sourceSyllabus.semester ?? "",
      courseDesignation: sourceSyllabus.courseDesignation ?? "",
      courseTypes: sourceSyllabus.courseTypes ?? "",
      language: sourceSyllabus.language ?? "",
      relation: sourceSyllabus.relation ?? "",
      teachingMethods: sourceSyllabus.teachingMethods ?? "",
      workloadTotal: sourceSyllabus.workloadTotal ?? "",
      workloadContact: sourceSyllabus.workloadContact ?? "",
      workloadPrivate: sourceSyllabus.workloadPrivate ?? "",
      prerequisites: sourceSyllabus.prerequisites ?? "",
      objectives: sourceSyllabus.objectives ?? "",
      examForms: sourceSyllabus.examForms ?? "",
      examRequirements: sourceSyllabus.examRequirements ?? "",
      rubrics: sourceSyllabus.rubrics ?? "",
      major: sourceSyllabus.major ?? "",
      changeSummary: `Imported from ${sourceSyllabus.versionLabel || `v${sourceSyllabus.versionNumber}`}`,
      notes: sourceSyllabus.notes ?? "",
      clos: sourceSyllabus.clos ?? [],
      topics: sourceSyllabus.topics ?? [],
      assessments: sourceSyllabus.assessments ?? [],
    }
  }
  return (
    <div className="mx-auto w-full max-w-[1200px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              Administrator / New Syllabus
            </span>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              Create Syllabus Draft
            </h1>

            <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
              Select a course, let the system prefill data from the latest syllabus when available, create the Draft, then import a PDF/Word/Excel file to automatically populate the structured syllabus. You can review and edit everything before submission.
            </p>
          </div>

          <div className="flex flex-wrap justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                navigate(-1)
              }
            >
              <ArrowLeft className="size-4" />
              Back
            </Button>

            {autoImportAfterCreate && (
              <Button
                type="submit"
                form="new-syllabus-form"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                disabled={createMutation.isPending}
              >
                <FilePlus2 className="size-4" />
                Create Draft & Import File
              </Button>
            )}
          </div>
        </div>
      </section>

      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="border-b border-slate-100">
          <CardTitle className="text-base text-[#17343d]">
            Start from existing syllabus (optional)
          </CardTitle>

          <p className="text-xs leading-5 text-slate-500">
            Choose an existing syllabus only when you want to clone its metadata and structured content. Otherwise select a course below and the system will automatically prefill the latest available syllabus template.
          </p>
        </CardHeader>

        <CardContent className="p-6">
          <Select
            value={
              selectedSourceId
                ? String(
                    selectedSourceId,
                  )
                : "NONE"
            }
            onValueChange={(
              value,
            ) =>
              setSelectedSourceId(
                value === "NONE"
                  ? null
                  : Number(value),
              )
            }
          >
            <SelectTrigger className="bg-white">
              <SelectValue placeholder="Select source syllabus" />
            </SelectTrigger>

            <SelectContent>
              <SelectItem value="NONE">
                Start without source metadata
              </SelectItem>

              {allSyllabuses.map(
                (syllabus) => (
                  <SelectItem
                    key={syllabus.id}
                    value={String(
                      syllabus.id,
                    )}
                  >
                    {syllabus.courseCode} — {syllabus.versionLabel || `v${syllabus.versionNumber}`} — {syllabus.academicYear}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>

          {sourceLoading && (
            <p className="mt-2 flex items-center text-xs text-slate-500">
              <LoaderCircle className="mr-2 size-3.5 animate-spin" />
              Loading source metadata...
            </p>
          )}
        </CardContent>
      </Card>

      <SyllabusForm
        key={
          sourceSyllabus
            ? `source-${sourceSyllabus.id}`
            : `program-${courseProgramId || "none"}`
        }
        initialData={
          initialData
        }
        onSubmit={
          handleAdminCreate
        }
        formId="new-syllabus-form"
        allowCreateCourse={isAdmin}
        submitLabel={
          autoImportAfterCreate
            ? "Create Draft & Import File"
            : "Create Syllabus Draft"
        }
        loading={
          createMutation.isPending
        }
        lockProgramContext={
          courseProgramId > 0
        }
      />
    </div>
  )
}

function ContextValue({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div className="rounded-xl border border-slate-100 bg-slate-50/70 px-4 py-3">
      <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
        {label}
      </p>

      <p className="mt-1 text-sm font-semibold leading-5 text-slate-800">
        {value}
      </p>
    </div>
  )
}