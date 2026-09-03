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
  History,
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
import SyllabusDiffDetails from "@/components/syllabus/SyllabusDiffDetails"
import SyllabusPdfPreviewDialog from "@/components/syllabus/SyllabusPdfPreviewDialog"
import ApprovalReviewDialog, { type ReviewDecision } from "@/components/approval/ApprovalReviewDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useSyllabusDiff } from "@/hooks/useSyllabusDiff"
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

const versionLabel = (syllabus: Syllabus) => syllabus.versionLabel || `v${syllabus.versionNumber}.0`

const compareVersionAscending = (left: Syllabus, right: Syllabus) => {
  const versionDifference = Number(left.versionNumber ?? 0) - Number(right.versionNumber ?? 0)
  return versionDifference || left.id - right.id
}

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

  const { data: courseVersions = [], isLoading: versionsLoading } = useQuery({
    queryKey: ["syllabuses-course", syllabus?.courseId],
    queryFn: () => syllabusApi.getByCourse(syllabus!.courseId),
    enabled: Boolean(syllabus?.courseId),
  })

  const sortedVersions = useMemo(
    () => courseVersions.slice().sort(compareVersionAscending),
    [courseVersions],
  )
  const currentVersionIndex = sortedVersions.findIndex((version) => version.id === syllabus?.id)
  const previousVersion = currentVersionIndex > 0
    ? sortedVersions[currentVersionIndex - 1]
    : undefined
  const versionsNewestFirst = useMemo(
    () => sortedVersions.slice().reverse(),
    [sortedVersions],
  )
  const formData = useMemo(
    () => syllabus ? toSyllabusFormData(syllabus) : undefined,
    [syllabus],
  )

  const currentStatus = normalizeStatus(syllabus?.status)
  const isAdmin = role === "ADMIN"
  // A successful Instructor GET already passed the backend assignment scope.
  const canEditDraft = ["DRAFT", "REVISION_REQUESTED"].includes(currentStatus)
    && (isAdmin || role === "INSTRUCTOR")
  const isApproved = currentStatus === "APPROVED"
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
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-12">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />
        <div className="flex flex-col gap-5 px-6 py-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="min-w-0">
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
            {previousVersion && (
              <Button
                type="button"
                variant="outline"
                onClick={() => document.getElementById("version-comparison")?.scrollIntoView({ behavior: "smooth", block: "start" })}
              >
                <GitCompareArrows className="size-4" />Compare Previous
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
            <p className="mt-1 text-sm leading-6">Review the approval history below for the comments attached to this version.</p>
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

      <EmbeddedVersionComparison versions={versionsNewestFirst} />

      <VersionHistory
        current={syllabus}
        loading={versionsLoading}
        versions={versionsNewestFirst}
        basePath={syllabusBasePath}
        navigate={navigate}
        role={role}
        showReviews={Boolean(user)}
      />

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
    <div className="mx-auto max-w-3xl rounded-xl border border-rose-200 bg-rose-50 p-6 text-rose-800">
      <h1 className="font-bold">{title}</h1>
      <p className="mt-1 text-sm">{description}</p>
      <Button type="button" variant="outline" className="mt-4 bg-white" onClick={onBack}>
        <ArrowLeft className="size-4" />Back to Catalog
      </Button>
    </div>
  )
}

function EmbeddedVersionComparison({ versions }: { versions: Syllabus[] }) {
  const ordered = useMemo(
    () => versions.slice().sort((a, b) => (b.versionNumber ?? 0) - (a.versionNumber ?? 0)),
    [versions],
  )
  const [olderId, setOlderId] = useState(() => ordered[1]?.id ?? 0)
  const [newerId, setNewerId] = useState(() => ordered[0]?.id ?? 0)

  useEffect(() => {
    const ids = new Set(ordered.map((item) => item.id))
    if (!ids.has(newerId)) setNewerId(ordered[0]?.id ?? 0)
    if (!ids.has(olderId) || olderId === newerId) setOlderId(ordered.find((item) => item.id !== (ids.has(newerId) ? newerId : ordered[0]?.id))?.id ?? 0)
  }, [newerId, olderId, ordered])

  const { data: diff, isLoading, isError } = useSyllabusDiff(newerId, olderId)

  return (
    <section id="version-comparison" className="scroll-mt-24 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 px-5 py-4">
        <div className="flex items-start gap-2">
          <GitCompareArrows className="mt-0.5 size-5 text-[#007d84]" />
          <div>
            <h2 className="font-semibold text-slate-900">Syllabus Version Comparison</h2>
            <p className="mt-1 text-xs leading-5 text-slate-500">Compare two saved versions of this course without leaving the syllabus form.</p>
          </div>
        </div>
      </div>

      {ordered.length < 2 ? (
        <div className="px-5 py-10 text-center text-sm text-slate-500">
          Version comparison becomes available after a second version is saved.
        </div>
      ) : (
        <div className="space-y-5 p-5">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold uppercase tracking-wider text-slate-500">Previous version</label>
              <Select value={String(olderId)} onValueChange={(value) => setOlderId(Number(value))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{ordered.filter((item) => item.id !== newerId).map((item) => <SelectItem key={item.id} value={String(item.id)}>{versionLabel(item)} · {item.academicYear} · {item.semester}</SelectItem>)}</SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold uppercase tracking-wider text-slate-500">New version</label>
              <Select value={String(newerId)} onValueChange={(value) => setNewerId(Number(value))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>{ordered.filter((item) => item.id !== olderId).map((item) => <SelectItem key={item.id} value={String(item.id)}>{versionLabel(item)} · {item.academicYear} · {item.semester}</SelectItem>)}</SelectContent>
              </Select>
            </div>
          </div>

          {isLoading && <div className="flex items-center justify-center gap-2 py-10 text-sm text-slate-500"><LoaderCircle className="size-4 animate-spin" />Comparing saved versions...</div>}
          {isError && <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">Unable to compare these versions.</div>}
          {diff && !isLoading && <SyllabusDiffDetails diff={diff} />}
        </div>
      )}
    </section>
  )
}

function VersionHistory({
  basePath,
  current,
  loading,
  navigate,
  role,
  showReviews,
  versions,
}: {
  basePath: string
  current: Syllabus
  loading: boolean
  navigate: ReturnType<typeof useNavigate>
  role: string
  showReviews: boolean
  versions: Syllabus[]
}) {
  const { data: reviews = [], isLoading: reviewsLoading } = useQuery({
    queryKey: ["approval-history", current.id],
    queryFn: () => approvalRequestApi.getApprovalHistory(current.id),
    enabled: showReviews,
  })

  const reviewLevelLabel = (step: string | null) => ({
    STEP1_DEPT_HEAD: "Department Head",
    STEP2_PROG_COORDINATOR: "Program Coordinator",
    STEP3_DEAN: "Dean",
  }[step || ""] || "Review")

  return (
    <section id="approval-history" className="scroll-mt-24 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-start gap-2 border-b border-slate-100 px-5 py-4">
        <History className="mt-0.5 size-5 text-[#007d84]" />
        <div>
          <h2 className="font-semibold text-slate-900">Syllabus History</h2>
          <p className="mt-1 text-xs leading-5 text-slate-500">
            Preserved versions, review decisions, comments, and timestamps for {current.courseCode}.
          </p>
        </div>
      </div>

      {loading || reviewsLoading ? (
        <div className="flex items-center justify-center gap-2 px-5 py-10 text-sm text-slate-500">
          <LoaderCircle className="size-4 animate-spin" />Loading syllabus history...
        </div>
      ) : versions.length === 0 ? (
        <div className="px-5 py-8 text-center text-sm text-slate-500">No version history is available.</div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b bg-slate-50 text-[10px] uppercase tracking-[0.1em] text-slate-500">
              <tr>
                <th className="px-5 py-3">Event</th>
                <th className="px-5 py-3">Version / Review level</th>
                <th className="px-5 py-3">Context / Comment</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Performed by</th>
                <th className="px-5 py-3">Time</th>
                <th className="px-5 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {versions.map((version, index) => {
                const compareTarget = versions[index + 1]
                return (
                  <tr key={version.id} className={version.id === current.id ? "bg-[#f2fbfb]" : "hover:bg-slate-50"}>
                    <td className="px-5 py-3"><Badge variant="outline">Version</Badge></td>
                    <td className="px-5 py-3 font-semibold text-slate-800">
                      {versionLabel(version)}
                      {version.id === current.id && <span className="ml-2 text-[10px] uppercase text-[#007d84]">Viewing</span>}
                    </td>
                    <td className="px-5 py-3 text-slate-600">{version.academicYear} · {version.semester || "—"}</td>
                    <td className="px-5 py-3"><Badge variant="outline" className={statusClass(version.status)}>{statusLabel(version.status, role)}</Badge></td>
                    <td className="px-5 py-3 text-slate-600">{version.createdByUsername || "—"}</td>
                    <td className="px-5 py-3 text-slate-500">{formatDate(version.updatedAt)}</td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end gap-2">
                        {version.id !== current.id && (
                          <Button type="button" variant="outline" size="sm" onClick={() => navigate(`${basePath}/${version.id}`)}>
                            View
                          </Button>
                        )}
                        {compareTarget && (
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`${basePath}/${version.id}/diff?compareWith=${compareTarget.id}`)}
                          >
                            <GitCompareArrows className="size-3.5" />Compare
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
              {reviews.map((review) => (
                <tr key={`review-${review.id}`} className="align-top hover:bg-slate-50">
                  <td className="px-5 py-3"><Badge variant="outline" className="border-blue-200 bg-blue-50 text-blue-700">Review</Badge></td>
                  <td className="px-5 py-3 font-medium text-slate-800">{versionLabel(current)} · {reviewLevelLabel(review.step)}</td>
                  <td className="max-w-md px-5 py-3 text-slate-600">
                    {review.comment?.trim() ? <span className="whitespace-pre-wrap">{review.comment}</span> : <span className="italic text-slate-400">No comment</span>}
                  </td>
                  <td className="px-5 py-3"><Badge variant="outline" className={statusClass(review.status)}>{statusLabel(review.status, role)}</Badge></td>
                  <td className="px-5 py-3 text-slate-600">{review.reviewedByUsername || review.requestedByUsername || "—"}</td>
                  <td className="px-5 py-3 whitespace-nowrap text-slate-500">{formatDate(review.resolvedAt || review.createdAt)}</td>
                  <td className="px-5 py-3 text-right text-slate-400">—</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
