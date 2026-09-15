import { formatVersionLabel } from "@/lib/syllabusVersion"
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
  LoaderCircle,
  ShieldCheck,
} from "lucide-react"

import { courseProgramApi } from "@/api/courseProgramApi"
import { syllabusApi } from "@/api/syllabusApi"
import {
  getMyActiveAssignments,
} from "@/api/classSectionApi"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
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
import {
  buildImportedSyllabusInitialData,
} from "@/lib/syllabusImportForm"
import {
  isUsableSyllabusImportPreview,
  loadSyllabusImportDraft,
  removeSyllabusImportDraft,
} from "@/lib/syllabusImportDraft"
import {
  SyllabusRelationSyncError,
  syncStandardSyllabusRelations,
} from "@/lib/syllabusRelationSync"
import { useCreateSyllabus } from "@/hooks/useCreateSyllabus"
import { useSyllabuses } from "@/hooks/useSyllabuses"
import { useSyllabus } from "@/hooks/useSyllabus"
import { useAuthStore } from "@/store/authStore"
import type {
  CreateSyllabusRequest,
} from "@/types/syllabus"
import type { SyllabusImportPreviewResponse } from "@/types/syllabusImport"

const assignmentSemester = (
  semester: number,
) => `Semester ${semester}`

const normalizeRole = (
  value: unknown,
) =>
  String(value ?? "")
    .replace(/^ROLE_/i, "")
    .trim()
    .toUpperCase()

const normalizeCourseIdentity = (value: unknown) => String(value ?? "")
  .trim()
  .toUpperCase()
  .replace(/[\s_-]+/g, "")

const normalizeCourseName = (value: unknown) => String(value ?? "")
  .trim()
  .toUpperCase()
  .replace(/[^A-Z0-9+#]+/g, "")

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

export default function SyllabusCreatePage() {
  const location =
    useLocation()

  const navigate =
    useNavigate()

  const [searchParams] =
    useSearchParams()

  const requestedCourseId = Number(searchParams.get("courseId"))
  const hasRequestedCourseId = Number.isFinite(requestedCourseId) && requestedCourseId > 0
  const importDraftId = searchParams.get("importDraft")
  const importLocationState = location.state as {
    importPreview?: SyllabusImportPreviewResponse
    targetCourseId?: number
    targetCourseProgramId?: number
    previewOnly?: boolean
  } | null
  const previewOnly = Boolean(importLocationState?.previewOnly)
  const requestedImportCourseProgramId = Number(searchParams.get("courseProgramId")) || undefined
  const previewFromLocation = importLocationState?.targetCourseId === requestedCourseId
    && (importLocationState.targetCourseProgramId === undefined
      || importLocationState.targetCourseProgramId === requestedImportCourseProgramId)
    ? importLocationState.importPreview
    : undefined
  const importPreviewCandidate = useMemo(
    () => previewFromLocation ?? loadSyllabusImportDraft(
      importDraftId,
      requestedCourseId,
      requestedImportCourseProgramId,
    ),
    [importDraftId, previewFromLocation, requestedCourseId, requestedImportCourseProgramId],
  )
  const importPreview = isUsableSyllabusImportPreview(importPreviewCandidate)
    ? importPreviewCandidate
    : undefined

  const user =
    useAuthStore(
      (state) =>
        state.user,
    )

  const role =
    normalizeRole(
      user?.role,
    )

  const isInstructor = role === "INSTRUCTOR"

  const isAdmin = role === "ADMIN"

  // Course master data stays read-only for Instructor. The broader isAdmin
  // flag above is temporary and only reuses the global syllabus creation flow.
  const canManageCourseMaster = role === "ADMIN"

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

  const requestedAssignmentId = Number(searchParams.get("assignmentId"))

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

  const [
    selectedSourceId,
    setSelectedSourceId,
  ] =
    useState<number | null>(
      null,
    )

  const { data: myAssignments = [] } = useQuery({
    queryKey: [
      "my-active-assignments",
      "syllabus-create",
    ],
    queryFn:
      getMyActiveAssignments,
    enabled: isInstructor,
    staleTime: 30_000,
  })

  const { data: allSyllabuses = [] } = useSyllabuses()

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
      (isAdmin || isInstructor)
      && Number.isFinite(
        courseProgramId,
      )
      && courseProgramId > 0,
  })

  const {
    data: scopedCoursePrograms = [],
  } = useQuery({
    queryKey: ["course-programs", "curriculum", programId, cohortId],
    queryFn: () => courseProgramApi.getCurriculum(programId, cohortId),
    enabled: (isAdmin || isInstructor) && programId > 0 && cohortId > 0,
  })

  const {
    data: requestedCourseContext,
    isLoading: requestedCourseContextLoading,
    isError: requestedCourseContextError,
    refetch: refetchRequestedCourseContext,
  } = useQuery({
    queryKey: ["syllabus-create-context", requestedCourseId],
    queryFn: () => syllabusApi.getCreateContext(requestedCourseId),
    enabled: (isAdmin || isInstructor) && hasRequestedCourseId,
  })

  const {
    data:
      sourceSyllabus,
    isFetching:
      sourceLoading,
  } = useSyllabus(
    selectedSourceId,
  )

  const curriculumEntriesByCourse = useMemo(
    () => new Map(
      scopedCoursePrograms.map((item) => [item.courseId, item]),
    ),
    [scopedCoursePrograms],
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
        ).filter((assignment) => {
          if (!(programId > 0 && cohortId > 0)) return true

          return assignment.programId === programId
            && assignment.cohortId === cohortId
            && curriculumEntriesByCourse.has(assignment.courseId)
        }),
      [cohortId, curriculumEntriesByCourse, myAssignments, programId],
    )

  const handleAdminCreate =
    (
      values:
        CreateSyllabusRequest,
    ) => {
      const targetCourseId = Number(values.courseId)
      const instructorAssignment = isInstructor
        ? eligibleAssignments.find((assignment) =>
            assignment.courseId === targetCourseId
            && (!(requestedAssignmentId > 0) || assignment.id === requestedAssignmentId),
          )
        : undefined

      if (isInstructor && !instructorAssignment) {
        alert("You are not assigned to this course and cannot create or import its syllabus.")
        return
      }

      const safeValues = {
        ...values,
      }

      delete safeValues.createdBy

      const payload:
        CreateSyllabusRequest = {
          ...safeValues,
          assignmentId: isInstructor
            ? instructorAssignment!.id
            : values.assignmentId,
          courseId:
            targetCourseId,
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
            isInstructor
              ? instructorAssignment!.academicYear
              : values.academicYear?.trim() || "",
          semester:
            isInstructor
              ? assignmentSemester(instructorAssignment!.semester)
              : values.semester?.trim() || "",
          courseProgramId:
            courseProgramId > 0
              ? courseProgramId
              : values.courseProgramId
                ?? scopedCoursePrograms.find((item) => item.courseId === Number(values.courseId))?.id,
          programId: programId > 0 ? programId : values.programId,
          cohortId: cohortId > 0 ? cohortId : values.cohortId,
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
          onSuccess: async (
            created,
          ) => {
            let relatedDataSaved = true
            try {
              await syncStandardSyllabusRelations(created.id, payload)
            } catch (relationError) {
              relatedDataSaved = false
              console.error(relationError)
              const relationMessage = relationError instanceof SyllabusRelationSyncError
                ? `${relationError.result.failures.length} linked item(s) could not be synchronized.`
                : "One or more linked items could not be synchronized."
              alert(`The syllabus Draft was created, but ${relationMessage} Open the Draft in the Syllabus Form and save again to retry.`)
            }

            if (relatedDataSaved) {
              removeSyllabusImportDraft(importDraftId)
            }

            if (
              programId > 0
              && cohortId > 0
            ) {
              navigate(
                `${basePath}/syllabus?programId=${programId}&cohortId=${cohortId}`,
                { replace: Boolean(importPreview) },
              )
              return
            }

            navigate(
              `${basePath}/syllabus/${created.id}/edit`,
              { replace: Boolean(importPreview) },
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

          <div data-admin-page-header="SyllabusCreatePage">
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

  if ((isAdmin || isInstructor) && (!(programId > 0) || !(cohortId > 0))) {
    return (
      <div className="mx-auto max-w-3xl rounded-2xl border border-amber-200 bg-amber-50 p-6 text-amber-900">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div data-admin-page-header="SyllabusCreatePage">
            <h1 className="font-semibold">Cohort is required</h1>
            <p className="mt-1 text-sm leading-6">Return to Syllabus Catalog and select a Cohort before importing or creating a syllabus. This prevents Major and Cohort from being saved as N/A.</p>
            <Button type="button" size="sm" variant="outline" className="mt-4 bg-white" onClick={() => navigate(`${basePath}/syllabus`)}>
              <ArrowLeft className="size-4" /> Back to Syllabus Catalog
            </Button>
          </div>
        </div>
      </div>
    )
  }

  if ((isAdmin || isInstructor) && hasRequestedCourseId && requestedCourseContextLoading) {
    return (
      <div className="mx-auto flex min-h-72 max-w-3xl items-center justify-center rounded-2xl border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
        <LoaderCircle className="mr-2 size-5 animate-spin text-[#007d84]" />
        Loading the selected course and its latest syllabus version...
      </div>
    )
  }

  if ((isAdmin || isInstructor) && hasRequestedCourseId && (requestedCourseContextError || !requestedCourseContext)) {
    return (
      <div className="mx-auto max-w-3xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-800">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div data-admin-page-header="SyllabusCreatePage">
            <h1 className="font-semibold">Cannot load the selected course</h1>
            <p className="mt-1 text-sm leading-6">The form was not opened, so imported data cannot be attached to the wrong course or version.</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Button type="button" size="sm" onClick={() => refetchRequestedCourseContext()}>Try again</Button>
              <Button type="button" size="sm" variant="outline" className="bg-white" onClick={() => navigate(-1)}>Back</Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  if ((isAdmin || isInstructor) && importDraftId && !importPreview) {
    return (
      <div className="mx-auto max-w-3xl rounded-2xl border border-amber-200 bg-amber-50 p-6 text-amber-900">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div data-admin-page-header="SyllabusCreatePage">
            <h1 className="font-semibold">Imported PDF data is unavailable</h1>
            <p className="mt-1 text-sm leading-6">The preview expired, is incomplete, or belongs to another course. Return to the catalog and import the PDF again; no syllabus has been saved.</p>
            <Button type="button" size="sm" variant="outline" className="mt-4 bg-white" onClick={() => navigate(-1)}>
              <ArrowLeft className="size-4" /> Back to import
            </Button>
          </div>
        </div>
      </div>
    )
  }

  const importedCourseMismatch = !previewOnly && Boolean(
    importPreview
    && requestedCourseContext
    && (() => {
      const sourceCode = normalizeCourseIdentity(importPreview.data.sourceCourseCode)
      const targetCode = normalizeCourseIdentity(requestedCourseContext.course.courseCode)
      if (sourceCode === targetCode) return false
      const aliasMatches = sourceCode.replace(/IU$/, "") === targetCode.replace(/IU$/, "")
      const sourceName = normalizeCourseName(importPreview.data.sourceCourseName)
      const targetName = normalizeCourseName(requestedCourseContext.course.name)
      return !aliasMatches || Boolean(sourceName && targetName && sourceName !== targetName)
    })()
  )

  if (importedCourseMismatch) {
    return (
      <div className="mx-auto max-w-3xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-800">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div data-admin-page-header="SyllabusCreatePage">
            <h1 className="font-semibold">The imported PDF belongs to another course</h1>
            <p className="mt-1 text-sm leading-6">PDF course {importPreview?.data.sourceCourseCode} does not match selected course {requestedCourseContext?.course.courseCode}. Nothing has been saved.</p>
            <Button type="button" size="sm" variant="outline" className="mt-4 bg-white" onClick={() => navigate(-1)}>
              <ArrowLeft className="size-4" /> Choose the matching course
            </Button>
          </div>
        </div>
      </div>
    )
  }

  const automaticallyResolvedCourseProgramId = courseProgramId > 0
    ? courseProgramId
    : scopedCoursePrograms.find((item) => item.courseId === requestedCourseId)?.id
      ?? requestedCourseContext?.coursePrograms?.find((item) => item.cohortId === cohortId)?.id
      ?? (requestedCourseContext?.coursePrograms?.length === 1
        ? requestedCourseContext.coursePrograms[0].id
        : undefined)

  let initialData:
    CreateSyllabusRequest
    | undefined

  if (Number.isFinite(requestedCourseId) && requestedCourseId > 0) {
    const selectedCourseProgramId = automaticallyResolvedCourseProgramId
    const nextVersionNumber = 1

    initialData = importPreview
      ? buildImportedSyllabusInitialData(importPreview, {
          courseId: requestedCourseId,
          courseProgramId: selectedCourseProgramId,
          nextVersionNumber,
        })
      : {
      courseId: requestedCourseId,
      courseProgramId: selectedCourseProgramId,
      versionNumber: nextVersionNumber,
      versionLabel: formatVersionLabel(nextVersionNumber),
      academicYear: "",
      semester: "",
      changeSummary: "Initial Draft",
      notes: "",
      clos: [],
      topics: [],
      assessments: [],
    }
  }

  if (!initialData && courseProgram && !sourceSyllabus) {
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
      major: courseProgram.majorName || courseProgram.majorCode || "",
      changeSummary: "Initial Draft",
      notes: "",
      clos: [],
      topics: [],
      assessments: [],
    }
  }

  if (sourceSyllabus) {
    const nextVersion = 1
    initialData = {
      courseProgramId: courseProgramId > 0
        ? courseProgramId
        : requestedCourseContext?.coursePrograms?.length === 1
          ? requestedCourseContext.coursePrograms[0].id
          : undefined,
      courseId: Number.isFinite(requestedCourseId) && requestedCourseId > 0
        ? requestedCourseId
        : sourceSyllabus.courseId,
      versionNumber: nextVersion,
      versionLabel: formatVersionLabel(nextVersion),
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
      major: sourceSyllabus.major ?? "",
      changeSummary: `Imported from ${formatVersionLabel(sourceSyllabus.versionNumber, sourceSyllabus.versionLabel)}`,
      notes: sourceSyllabus.notes ?? "",
      clos: sourceSyllabus.clos ?? [],
      topics: sourceSyllabus.topics ?? [],
      assessments: sourceSyllabus.assessments ?? [],
    }
  }
  return (
    <div data-admin-page="SyllabusCreatePage" className="mx-auto w-full max-w-[1200px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div data-admin-page-header="SyllabusCreatePage">
            <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              New Syllabus
            </span>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              Create Full Syllabus Draft
            </h1>

            <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
              Complete the standard CS syllabus below: General Information, CLOs, CLO-PLO Matrix, Planned Learning Activities, Assessment Plan, and Reading List. Imported data remains editable before saving.
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

          </div>
        </div>
      </section>

      {!importPreview && <Card className="border-slate-200 shadow-sm">
        <CardHeader className="border-b border-slate-100">
          <CardTitle className="text-base text-[#17343d]">
            Start from an existing syllabus (optional)
          </CardTitle>

          <p className="text-xs leading-5 text-slate-500">
            Choose an existing syllabus only when you want to reuse its content. Saving always creates a separate server-controlled syllabus version and keeps previous versions unchanged.
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

              {allSyllabuses
                .filter((syllabus) => !Number.isFinite(requestedCourseId) || requestedCourseId <= 0 || syllabus.courseId === requestedCourseId)
                .map(
                (syllabus) => (
                  <SelectItem
                    key={syllabus.id}
                    value={String(
                      syllabus.id,
                    )}
                  >
                    {syllabus.courseCode} — {formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel)} — {syllabus.academicYear}
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
      </Card>}

      {previewOnly && (
        <Alert className="border-sky-200 bg-sky-50 text-sky-900">
          <AlertTriangle className="size-4" />
          <AlertTitle>Template preview only</AlertTitle>
          <AlertDescription>
            Course {importPreview?.data.sourceCourseCode || "from this template"} is not in the selected curriculum. A temporary course context is used only to display the imported form; saving is disabled.
          </AlertDescription>
        </Alert>
      )}

      <SyllabusForm
        key={
          importPreview
            ? `import-${importDraftId || requestedCourseId}`
            : sourceSyllabus
            ? `source-${sourceSyllabus.id}`
            : requestedCourseId > 0
              ? `course-${requestedCourseId}`
              : `program-${courseProgramId || "none"}`
        }
        initialData={
          initialData
        }
        importedContentTopics={importPreview?.data.topics}
        readOnly={previewOnly}
        previewCourseIdentity={previewOnly ? {
          code: importPreview?.data.sourceCourseCode,
          name: importPreview?.data.sourceCourseName,
        } : undefined}
        onSubmit={
          handleAdminCreate
        }
        formId="new-syllabus-form"
        allowCreateCourse={canManageCourseMaster}
        autoPrefillExisting={!(Number.isFinite(requestedCourseId) && requestedCourseId > 0)}
        submitLabel="Lưu syllabus"
        loading={
          createMutation.isPending
        }
        lockProgramContext={
          courseProgramId > 0
          || (Number.isFinite(requestedCourseId) && requestedCourseId > 0)
        }
        curriculumContext={courseProgram?.cohortName ? {
          program: `${courseProgram.programCode} — ${courseProgram.programName}`,
          major: courseProgram.majorName || courseProgram.majorCode || "N/A",
          cohort: courseProgram.cohortName,
        } : scopedCoursePrograms[0]?.cohortName ? {
          program: `${scopedCoursePrograms[0].programCode} — ${scopedCoursePrograms[0].programName}`,
          major: scopedCoursePrograms[0].majorName || scopedCoursePrograms[0].majorCode || "N/A",
          cohort: scopedCoursePrograms[0].cohortName,
        } : undefined}
        allowedCourseIds={isInstructor
          ? Array.from(new Set(eligibleAssignments.map((assignment) => assignment.courseId)))
          : programId > 0 && cohortId > 0
            ? Array.from(new Set(scopedCoursePrograms.map((item) => item.courseId)))
            : undefined}
      />
    </div>
  )
}
