import { useMemo, useState, type ReactNode } from "react"
import {
  AlertCircle,
  ArrowLeft,
  FileUp,
  Loader2,
  RefreshCcw,
  ShieldAlert,
  type LucideIcon,
} from "lucide-react"
import { useLocation, useNavigate, useParams } from "react-router-dom"
import { toast } from "sonner"

import ImportSyllabusDialog from "@/components/syllabus/ImportSyllabusDialog"
import SyllabusForm from "@/components/syllabus/SyllabusForm"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { useSyllabus } from "@/hooks/useSyllabus"
import { useUpdateSyllabus } from "@/hooks/useUpdateSyllabus"
import { getSyllabusBasePath } from "@/lib/programContext"
import { saveSyllabusImportDraft } from "@/lib/syllabusImportDraft"
import {
  SyllabusRelationSyncError,
  syncStandardSyllabusRelations,
} from "@/lib/syllabusRelationSync"
import {
  toSyllabusFormData,
  type SyllabusFormContext,
} from "@/lib/syllabusFormData"
import { useAuthStore } from "@/store/authStore"
import type { CreateSyllabusRequest } from "@/types/syllabus"
import type { SyllabusImportPreviewResponse } from "@/types/syllabusImport"

type EditNavigationState = {
  classSectionId?: number
  courseProgramId?: number
  programId?: number
  major?: string
}

const positiveNumber = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const normalizeRole = (role?: string) => String(role ?? "")
  .replace(/^ROLE_/i, "")
  .toUpperCase()

const removeImportFlag = (search: string) => {
  const params = new URLSearchParams(search)
  params.delete("import")
  return params.toString()
}

export default function SyllabusEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const basePath = getSyllabusBasePath(location.pathname)
  const syllabusId = Number(id)
  const validSyllabusId = Number.isInteger(syllabusId) && syllabusId > 0
  const role = normalizeRole(user?.role)
  const isAdmin = Boolean(role) && role !== "INSTRUCTOR"
  const importRequested = new URLSearchParams(location.search).get("import") === "1"
  const [importOpen, setImportOpen] = useState(false)
  const [synchronizingRelations, setSynchronizingRelations] = useState(false)

  const {
    data,
    error,
    isError,
    isLoading,
    refetch,
  } = useSyllabus(validSyllabusId ? syllabusId : null, true)
  const updateMutation = useUpdateSyllabus()

  const draftContext = useMemo<SyllabusFormContext>(() => {
    const params = new URLSearchParams(location.search)
    const state = (location.state ?? {}) as EditNavigationState

    return {
      classSectionId: positiveNumber(params.get("classSectionId"))
        ?? positiveNumber(state.classSectionId),
      courseProgramId: positiveNumber(params.get("courseProgramId"))
        ?? positiveNumber(state.courseProgramId),
      programId: positiveNumber(params.get("programId"))
        ?? positiveNumber(state.programId),
    }
  }, [location.search, location.state])

  const initialData = useMemo(
    () => data ? toSyllabusFormData(data, draftContext) : undefined,
    [data, draftContext],
  )

  const curriculumMajor = useMemo(() => {
    const state = (location.state ?? {}) as EditNavigationState
    const params = new URLSearchParams(location.search)
    return data?.major?.trim()
      || state.major?.trim()
      || params.get("majorCode")?.trim()
      || "N/A"
  }, [data?.major, location.search, location.state])

  const effectiveInitialData = useMemo(
    () => initialData
      ? { ...initialData, major: initialData.major?.trim() || curriculumMajor }
      : undefined,
    [curriculumMajor, initialData],
  )

  const closeImport = () => {
    setImportOpen(false)
    if (!importRequested) return

    navigate(
      {
        pathname: location.pathname,
        search: removeImportFlag(location.search),
      },
      { replace: true, state: location.state },
    )
  }

  if (!validSyllabusId) {
    return (
      <PageState
        icon={AlertCircle}
        title="Invalid syllabus address"
        description="The syllabus ID in this address is not valid. Return to the catalog and select a Draft again."
        actions={<Button variant="outline" onClick={() => navigate(basePath)}><ArrowLeft className="size-4" />Back to catalog</Button>}
      />
    )
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center px-6">
        <div className="flex items-center gap-3 text-sm font-medium text-slate-600">
          <Loader2 className="size-5 animate-spin text-[#007d84]" />
          Loading the complete syllabus form...
        </div>
      </div>
    )
  }

  if (isError || !data || !effectiveInitialData) {
    const message = error instanceof Error
      ? error.message
      : "The syllabus does not exist or your account cannot access it."

    return (
      <PageState
        icon={AlertCircle}
        title="Unable to load this syllabus"
        description={message}
        tone="error"
        actions={(
          <>
            <Button onClick={() => void refetch()}><RefreshCcw className="size-4" />Try again</Button>
            <Button variant="outline" onClick={() => navigate(basePath)}><ArrowLeft className="size-4" />Back to catalog</Button>
          </>
        )}
      />
    )
  }

  if (!["DRAFT", "REVISION_REQUESTED"].includes(data.status)) {
    return (
      <PageState
        icon={ShieldAlert}
        title="This version is read-only"
        description={`${data.versionLabel || `v${data.versionNumber}.0`} is currently ${data.status}. Only Draft or Revision Requested syllabuses can be changed.`}
        tone="warning"
        actions={(
          <>
            <Button onClick={() => navigate(`${basePath}/${data.id}`)}>View syllabus</Button>
            <Button variant="outline" onClick={() => navigate(basePath)}><ArrowLeft className="size-4" />Back to catalog</Button>
          </>
        )}
      />
    )
  }

  const handleUpdate = async (values: CreateSyllabusRequest) => {
    const payload: CreateSyllabusRequest = {
      ...values,
      // A Draft editor must never move an existing version to another course.
      courseId: data.courseId,
      versionNumber: data.versionNumber,
      versionLabel: values.versionLabel || data.versionLabel,
      cohortId: values.cohortId ?? data.cohortId,
      assignmentId: values.assignmentId ?? draftContext.assignmentId,
      courseProgramId: values.courseProgramId ?? draftContext.courseProgramId,
      programId: values.programId ?? draftContext.programId,
      sourceType: values.sourceType ?? data.sourceType ?? undefined,
      originalFileName: values.originalFileName ?? data.originalFileName ?? undefined,
      originalFileType: values.originalFileType ?? data.originalFileType ?? undefined,
    }

    try {
      setSynchronizingRelations(true)
      const saved = await updateMutation.mutateAsync({ id: syllabusId, data: payload })

      await syncStandardSyllabusRelations(saved.id, payload)

      toast.success("Syllabus Draft and all linked form data were saved successfully.")
      navigate(basePath)
    } catch (mutationError: unknown) {
      console.error("Update syllabus error:", mutationError)

      if (mutationError instanceof SyllabusRelationSyncError) {
        toast.error(
          "The form was saved, but one or more matrix/reading links could not be synchronized. The Draft remains open so you can retry.",
        )
        await refetch()
        return
      }

      const responseMessage = mutationError as {
        response?: { data?: { message?: string; error?: string } }
        message?: string
      }
      toast.error(
        responseMessage.response?.data?.message
          || responseMessage.response?.data?.error
          || responseMessage.message
          || "Unable to save this syllabus Draft.",
      )
    } finally {
      setSynchronizingRelations(false)
    }
  }

  const handleImportedPreview = (importPreview: SyllabusImportPreviewResponse) => {
    const importDraftId = saveSyllabusImportDraft(
      importPreview,
      data.courseId,
      draftContext.courseProgramId,
    )
    const params = new URLSearchParams({
      courseId: String(data.courseId),
      importDraft: importDraftId,
    })
    if (draftContext.courseProgramId) {
      params.set("courseProgramId", String(draftContext.courseProgramId))
    }

    setImportOpen(false)
    navigate(
      `${basePath}/create?${params.toString()}`,
      {
        state: {
          importPreview,
          targetCourseId: data.courseId,
          targetCourseProgramId: draftContext.courseProgramId,
        },
      },
    )
  }

  return (
    <div className="mx-auto w-full max-w-[1180px] space-y-6 px-4 pb-20 pt-6 sm:px-6">
      <header className="rounded-xl border border-[#cfdee1] border-t-4 border-t-[#007d84] bg-white px-5 py-4 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#607b83]">
                Syllabus administration
              </p>
              <Badge variant="outline" className="border-emerald-200 bg-emerald-50 text-emerald-700">
                Draft
              </Badge>
              {data.sourceType?.startsWith("IMPORT") && (
                <Badge variant="outline" className="border-violet-200 bg-violet-50 text-violet-700">
                  Imported
                </Badge>
              )}
            </div>
            <h1 className="truncate text-2xl font-bold tracking-tight text-[#006b72]">
              {data.courseCode} — {data.courseName}
            </h1>
            <p className="mt-1 text-sm text-slate-500">
              {data.academicYear} · {data.semester || "Semester not set"} · {data.versionLabel || `v${data.versionNumber}.0`}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            {isAdmin && (
              <Button type="button" variant="outline" onClick={() => setImportOpen(true)}>
                <FileUp className="size-4" />
                Import PDF as new version
              </Button>
            )}
            <Button type="button" variant="outline" onClick={() => navigate(basePath)}>
              <ArrowLeft className="size-4" />
              Back to catalog
            </Button>
          </div>
        </div>
      </header>

      <Alert className="border-sky-200 bg-sky-50/80 px-4 py-3 text-sky-900">
        <AlertCircle className="size-4" />
        <AlertTitle>One Draft, one complete editable form</AlertTitle>
        <AlertDescription className="text-sky-800">
          General information, CLOs, content, matrices, weekly activities, assessments, readings, and revision data are loaded from this Draft and saved together.
        </AlertDescription>
      </Alert>

      <SyllabusForm
        key={`${data.id}:${data.updatedAt}`}
        initialData={effectiveInitialData}
        onSubmit={handleUpdate}
        loading={updateMutation.isPending || synchronizingRelations}
        lockProgramContext
        lockAssignmentContext={false /* TEMPORARY: global Instructor testing */}
        autoPrefillExisting={false}
        formId="edit-syllabus-form"
        submitLabel="Save syllabus Draft"
        curriculumContext={data.cohortName ? {
          program: `${data.programCode || "Program"}${data.programName ? ` — ${data.programName}` : ""}`,
          major: curriculumMajor,
          cohort: data.cohortName,
        } : undefined}
      />

      <ImportSyllabusDialog
        open={importOpen || (importRequested && isAdmin)}
        onClose={closeImport}
        expectedCourseCode={data.courseCode}
        expectedCourseName={data.courseName}
        onPreviewConfirmed={handleImportedPreview}
      />
    </div>
  )
}

function PageState({
  actions,
  description,
  icon: Icon,
  title,
  tone = "default",
}: {
  actions: ReactNode
  description: string
  icon: LucideIcon
  title: string
  tone?: "default" | "error" | "warning"
}) {
  const iconClass = tone === "error"
    ? "bg-rose-100 text-rose-700"
    : tone === "warning"
      ? "bg-amber-100 text-amber-700"
      : "bg-sky-100 text-sky-700"

  return (
    <div className="mx-auto flex min-h-[420px] max-w-3xl items-center px-4 py-10 sm:px-6">
      <Card className="w-full rounded-xl">
        <CardContent className="flex flex-col items-start gap-4 p-6 sm:p-8">
          <span className={`inline-flex size-11 items-center justify-center rounded-full ${iconClass}`}>
            <Icon className="size-5" />
          </span>
          <div>
            <h1 className="text-xl font-bold text-slate-900">{title}</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">{description}</p>
          </div>
          <div className="flex flex-wrap gap-2">{actions}</div>
        </CardContent>
      </Card>
    </div>
  )
}
