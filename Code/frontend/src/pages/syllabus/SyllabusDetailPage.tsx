import SyllabusRevisionHistory from "@/components/syllabus/SyllabusRevisionHistory"
import { syllabusHistoryApi } from "@/api/syllabusHistoryApi"
import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useEffect, useMemo, useState } from "react"
import axios from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Download,
  Edit3,
  FileSearch2,
  GitCompareArrows,
  LoaderCircle,
  ShieldCheck,
  XCircle,
} from "lucide-react"
import { useLocation, useNavigate, useParams } from "react-router-dom"
import { toast } from "sonner"

import { syllabusApi } from "@/api/syllabusApi"
import { syllabusPdfApi } from "@/api/syllabusPdfApi"
import { approvalRequestApi } from "@/api/approvalRequestApi"
import SyllabusForm from "@/components/syllabus/SyllabusForm"
import SyllabusPdfPreviewDialog from "@/components/syllabus/SyllabusPdfPreviewDialog"
import ApprovalReviewDialog, { type ReviewDecision } from "@/components/approval/ApprovalReviewDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { useSyllabus } from "@/hooks/useSyllabus"
import { getSyllabusBasePath } from "@/lib/programContext"
import { toSyllabusFormData } from "@/lib/syllabusFormData"
import { useAuthStore } from "@/store/authStore"
import type { Syllabus } from "@/types/syllabus"

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under Review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  REVISION_REQUESTED: "Revision Requested",
  ARCHIVED: "Archived",
}

const normalizeRole = (value: unknown) => String(value ?? "")
  .replace(/^ROLE_/i, "")
  .trim()
  .toUpperCase()

const normalizeStatus = (value: unknown) => String(value ?? "")
  .trim()
  .toUpperCase()

const statusLabel = (status: unknown, role: string) => {
  const normalized = normalizeStatus(status)
  if (role === "DEPT_HEAD" && normalized === "SUBMITTED") return "Pending Department Review"
  if (role === "DEPT_HEAD" && normalized === "UNDER_REVIEW") return "Forwarded to Dean"
  if (role === "DEAN" && normalized === "UNDER_REVIEW") return "Pending Final Review"
  return STATUS_LABELS[normalized] || normalized || "Unknown"
}

const statusClass = (status: unknown) => {
  switch (normalizeStatus(status)) {
    case "APPROVED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700"
    case "SUBMITTED":
    case "UNDER_REVIEW":
      return "border-blue-200 bg-blue-50 text-blue-700"
    case "REJECTED":
    case "REVISION_REQUESTED":
      return "border-rose-200 bg-rose-50 text-rose-700"
    case "ARCHIVED":
      return "border-slate-300 bg-slate-100 text-slate-600"
    default:
      return "border-amber-200 bg-amber-50 text-amber-700"
  }
}

const versionLabel = (syllabus: Syllabus) => formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel)

const formatDate = (value?: string | null) => {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat("en-GB", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date)
}

const safeFilePart = (value: unknown, fallback = "NA") => String(value ?? "")
  .trim()
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/[^a-zA-Z0-9._-]+/g, "-")
  .replace(/^-+|-+$/g, "") || fallback

const pdfErrorMessage = async (error: unknown) => {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : "Unable to export the syllabus PDF."
  }

  const responseData = error.response?.data
  if (responseData instanceof Blob) {
    try {
      const text = await responseData.text()
      if (!text) return error.message
      try {
        const parsed = JSON.parse(text) as { message?: string; error?: string }
        return parsed.message || parsed.error || text
      } catch {
        return text
      }
    } catch {
      return error.message
    }
  }

  if (typeof responseData === "object" && responseData !== null) {
    const candidate = responseData as { message?: string; error?: string }
    return candidate.message || candidate.error || error.message
  }
  return typeof responseData === "string" ? responseData : error.message
}

export default function SyllabusDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)
  const syllabusBasePath = getSyllabusBasePath(location.pathname)
  const syllabusId = Number(id)
  const validId = Number.isInteger(syllabusId) && syllabusId > 0
  const role = normalizeRole(user?.role)
  const [previewOpen, setPreviewOpen] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [reviewDecision, setReviewDecision] = useState<ReviewDecision | null>(null)

  const {
    data: syllabus,
    isError,
    isLoading,
  } = useSyllabus(validId ? syllabusId : null)

  const currentStatus = normalizeStatus(syllabus?.status)
  const isApproved = currentStatus === "APPROVED"

  const {
  data: previousComparable,
  isLoading: previousComparableLoading,
} = useQuery({
  queryKey: [
    "syllabus-previous-comparable",
    syllabusId,
  ],

  queryFn: () =>
    syllabusApi.getPreviousComparable(
      syllabusId,
    ),

  enabled:
    validId
    && Boolean(syllabus)
    && isApproved,
})
  
  
  const formData = useMemo(
    () => syllabus ? toSyllabusFormData(syllabus) : undefined,
    [syllabus],
  )

  const revisionMutation = useMutation({
    mutationFn: () => syllabusHistoryApi.startRevision(syllabusId),
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      toast.success("Revision draft created")
      navigate(`${syllabusBasePath}/${syllabusId}/edit`)
    },
    onError: (error) => toast.error(axios.isAxiosError(error) ? error.response?.data?.message || error.message : "Unable to start revision"),
  })

  const isAdmin = role === "ADMIN"
  // A successful Instructor GET already passed the backend assignment scope.
  const canEditDraft = ["DRAFT", "REVISION_REQUESTED"].includes(currentStatus)
    && (isAdmin || role === "INSTRUCTOR")
  const needsReviewerAction = (
    role === "DEPT_HEAD" && currentStatus === "SUBMITTED"
  ) || (
    role === "DEAN" && currentStatus === "UNDER_REVIEW"
  )

  const reviewStep = role === "DEAN" ? "STEP3_DEAN" : "STEP1_DEPT_HEAD"
  const { data: pendingReviewRequests = [] } = useQuery({
    queryKey: ["approval-requests", "pending", reviewStep],
    queryFn: () => approvalRequestApi.getPendingByStep(reviewStep),
    enabled: needsReviewerAction,
  })
  const pendingReviewRequest = pendingReviewRequests.find(
    (request) => request.syllabusId === syllabusId,
  ) ?? null

  const reviewMutation = useMutation({
    mutationFn: ({ decision, comment }: { decision: ReviewDecision; comment: string }) => {
      if (!pendingReviewRequest) throw new Error("This syllabus is no longer pending your review.")
      return approvalRequestApi.review(pendingReviewRequest.id, { status: decision, comment })
    },
    onSuccess: async () => {
      setReviewDecision(null)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["syllabus", syllabusId] }),
        queryClient.invalidateQueries({ queryKey: ["syllabus-revision-history", syllabusId] }),
        queryClient.invalidateQueries({ queryKey: ["approval-history", syllabusId] }),
        queryClient.invalidateQueries({ queryKey: ["syllabuses"] }),
        queryClient.invalidateQueries({ queryKey: ["approval-requests"] }),
        queryClient.invalidateQueries({ queryKey: ["depthead-courses"] }),
        queryClient.invalidateQueries({ queryKey: ["notifications"] }),
      ])
      toast.success(reviewDecision === "APPROVED"
        ? role === "DEAN" ? "Syllabus received final approval." : "Syllabus forwarded to the Dean."
        : "Syllabus returned to the instructor for revision.")
    },
  })
  const isRevisionState = ["REJECTED", "REVISION_REQUESTED"].includes(currentStatus)

  useEffect(() => {
    if (!syllabus || location.hash !== "#approval-history") return
    const timeoutId = window.setTimeout(() => {
      document.getElementById("approval-history")?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      })
    }, 100)
    return () => window.clearTimeout(timeoutId)
  }, [location.hash, syllabus])

  if (!validId) {
    return <MessageState title="Invalid syllabus ID" description="Return to the catalog and select a syllabus again." onBack={() => navigate(syllabusBasePath)} />
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center gap-2 text-sm text-slate-500">
        <LoaderCircle className="size-5 animate-spin text-[#007d84]" />
        Loading the complete syllabus form...
      </div>
    )
  }

  if (isError || !syllabus || !formData) {
    return <MessageState title="Syllabus not found" description="This syllabus does not exist or you do not have permission to access it." onBack={() => navigate(syllabusBasePath)} />
  }

  const handleDownload = async () => {
    if (!isApproved) {
      toast.error("Only an approved syllabus can be downloaded as the official PDF.")
      return
    }

    try {
      setDownloading(true)
      await syllabusPdfApi.download(
        syllabus.id,
        `Syllabus_${safeFilePart(syllabus.courseCode)}_${safeFilePart(versionLabel(syllabus))}_Official.pdf`,
      )
    } catch (error) {
      toast.error(await pdfErrorMessage(error))
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div data-admin-page="SyllabusDetailPage" className="mx-auto w-full max-w-[1500px] space-y-6 pb-12">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />
        <div className="flex flex-col gap-5 px-6 py-5 xl:flex-row xl:items-start xl:justify-between">
          <div data-admin-page-header="SyllabusDetailPage" className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                SCSE / Syllabus Detail
              </p>
              <Badge variant="outline" className={statusClass(syllabus.status)}>
                {statusLabel(syllabus.status, role)}
              </Badge>
              {syllabus.isCurrent && (
                <Badge variant="outline" className="border-blue-200 bg-blue-50 text-blue-700">
                  Current Version
                </Badge>
              )}
            </div>
            <h1 className="mt-2 truncate text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              {syllabus.courseCode} — {syllabus.courseName}
            </h1>
            <p className="mt-1 text-sm text-[#687f89]">
              {versionLabel(syllabus)} · {syllabus.academicYear} · {syllabus.semester || "Semester not specified"}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" onClick={() => navigate(syllabusBasePath)}>
              <ArrowLeft className="size-4" />Back to Catalog
            </Button>
            <Button type="button" variant="outline" onClick={() => setPreviewOpen(true)}>
              <FileSearch2 className="size-4" />Preview PDF
            </Button>
            {isApproved && (
              <Button
                type="button"
                variant="outline"
                disabled={downloading}
                className="border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                onClick={() => void handleDownload()}
              >
                {downloading ? <LoaderCircle className="size-4 animate-spin" /> : <Download className="size-4" />}
                Official PDF
              </Button>
            )}
            {isApproved && previousComparableLoading && (
  <Button
    type="button"
    variant="outline"
    disabled
  >
    <LoaderCircle className="size-4 animate-spin" />
    Finding Previous Cohort...
  </Button>
)}
            {isApproved && previousComparable && (
  <Button
    type="button"
    variant="outline"
    onClick={() =>
      navigate(
        `${syllabusBasePath}/${syllabus.id}/diff?compareWith=${previousComparable.id}`,
      )
    }
  >
    <GitCompareArrows className="size-4" />
    Compare Previous Cohort
  </Button>
)}
            {(role === "DEPT_HEAD" || role === "DEAN") && pendingReviewRequest && (
              <>
                <Button
                  type="button"
                  className="bg-emerald-600 text-white hover:bg-emerald-700"
                  onClick={() => setReviewDecision("APPROVED")}
                >
                  <CheckCircle2 className="size-4" />{role === "DEAN" ? "Final Approve" : "Approve & Forward"}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  className="border-rose-200 text-rose-700 hover:bg-rose-50"
                  onClick={() => setReviewDecision("REJECTED")}
                >
                  <XCircle className="size-4" />Reject
                </Button>
              </>
            )}
            {needsReviewerAction && !pendingReviewRequest && (
              <Button
                type="button"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                onClick={() => navigate(role === "DEAN" ? "/dean/approvals" : "/dept-head/approvals")}
              >
                <ShieldCheck className="size-4" />Open Review Queue
              </Button>
            )}
            {canEditDraft && (
              <Button
                type="button"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                onClick={() => navigate(`${syllabusBasePath}/${syllabus.id}/edit`)}
              >
                <Edit3 className="size-4" />Edit Draft
              </Button>
            )}
          </div>
        </div>

        <div className="grid border-t border-slate-200 text-sm sm:grid-cols-2 xl:grid-cols-4">
          <HeaderFact label="Created / Imported By" value={syllabus.createdByUsername || "—"} />
          <HeaderFact label="Source" value={syllabus.sourceType?.replaceAll("_", " ") || "MANUAL"} />
          <HeaderFact label="Final Approval Date" value={formatDate(syllabus.finalApprovalDate || syllabus.approvedAt)} />
          <HeaderFact label="Last Updated" value={formatDate(syllabus.updatedAt)} />
        </div>
      </section>

      {reviewDecision && pendingReviewRequest && (
        <ApprovalReviewDialog
          open
          item={pendingReviewRequest}
          decision={reviewDecision}
          isDean={role === "DEAN"}
          isPending={reviewMutation.isPending}
          errorMessage={reviewMutation.error instanceof Error ? reviewMutation.error.message : null}
          onOpenChange={(open) => {
            if (!open) {
              setReviewDecision(null)
              reviewMutation.reset()
            }
          }}
          onConfirm={(comment) => reviewMutation.mutate({ decision: reviewDecision, comment })}
        />
      )}

      {isRevisionState && (
        <section className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 px-5 py-4 text-rose-800">
          <AlertTriangle className="mt-0.5 size-5 shrink-0" />
          <div>
            <p className="font-semibold">Reviewer feedback requires revision</p>
            <p className="mt-1 text-sm leading-6">Review the approval history below for the comments attached to this revision.</p>
            {currentStatus === "REJECTED" && (isAdmin || role === "INSTRUCTOR") && (
              <Button className="mt-3" disabled={revisionMutation.isPending} onClick={() => revisionMutation.mutate()}>
                {revisionMutation.isPending ? "Starting revision…" : "Start revision"}
              </Button>
            )}
          </div>
        </section>
      )}

      <section aria-label="Complete syllabus form" className="space-y-3">
        <div>
          <h2 className="text-xl font-bold text-[#17343d]">Complete Syllabus Form</h2>
          <p className="mt-1 text-xs leading-5 text-slate-500">
            This is the same standard form used to edit the Draft. Every saved field is displayed here in read-only mode.
          </p>
        </div>
        <SyllabusForm
          key={`${syllabus.id}:${syllabus.updatedAt}:view`}
          initialData={formData}
          onSubmit={() => undefined}
          autoPrefillExisting={false}
          lockProgramContext
          lockAssignmentContext
          readOnly
        />
      </section>

      <SyllabusRevisionHistory syllabusId={syllabus.id} />

      <SyllabusPdfPreviewDialog
        open={previewOpen}
        onOpenChange={setPreviewOpen}
        syllabusId={syllabus.id}
        courseCode={syllabus.courseCode}
        courseName={syllabus.courseName}
        status={statusLabel(syllabus.status, role)}
      />
    </div>
  )
}

function HeaderFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-b border-r border-slate-200 px-5 py-3 last:border-r-0 sm:border-b-0">
      <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">{label}</p>
      <p className="mt-1 truncate font-medium text-slate-700" title={value}>{value}</p>
    </div>
  )
}

function MessageState({
  description,
  onBack,
  title,
}: {
  description: string
  onBack: () => void
  title: string
}) {
  return (
    <div data-admin-page-header="SyllabusDetailPage" className="mx-auto max-w-3xl rounded-xl border border-rose-200 bg-rose-50 p-6 text-rose-800">
      <h1 className="font-bold">{title}</h1>
      <p className="mt-1 text-sm">{description}</p>
      <Button type="button" variant="outline" className="mt-4 bg-white" onClick={onBack}>
        <ArrowLeft className="size-4" />Back to Catalog
      </Button>
    </div>
  )
}