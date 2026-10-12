import { formatVersionLabel } from "@/lib/syllabusVersion"
import type { ReactNode } from "react"
import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  CheckCircle2,
  Clock3,
  Eye,
  History,
  Inbox,
  LoaderCircle,
  RefreshCw,
  Search,
  ShieldCheck,
  UserRound,
  X,
  XCircle,
} from "lucide-react"

import { approvalRequestApi } from "@/api/approvalRequestApi"
import SyllabusReviewDetailsDialog from "@/components/approval/SyllabusReviewDetailsDialog"
import ApprovalReviewDialog, {
  type ReviewDecision,
} from "@/components/approval/ApprovalReviewDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useAuthStore } from "@/store/authStore"
import type {
  ApprovalRequestItem,
  ApprovalStep,
} from "@/types/approval"

interface ReviewDialogState {
  item: ApprovalRequestItem
  decision: ReviewDecision
}

interface FeedbackState {
  type: "success" | "error"
  message: string
}

const normalizeRole = (
  value: unknown,
) =>
  String(value ?? "")
    .replace(/^ROLE_/i, "")
    .trim()
    .toUpperCase()

const formatDateTime = (
  value: string | null | undefined,
) => {
  if (!value) {
    return "—"
  }

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

const getWaitingTime = (
  value: string | null | undefined,
) => {
  if (!value) {
    return "—"
  }

  const created =
    new Date(value)

  if (
    Number.isNaN(
      created.getTime(),
    )
  ) {
    return "—"
  }

  const elapsedMs =
    Date.now()
    - created.getTime()

  const hours =
    Math.max(
      0,
      Math.floor(
        elapsedMs
        / (
          1000
          * 60
          * 60
        ),
      ),
    )

  if (hours < 24) {
    return `${hours}h`
  }

  return `${Math.floor(hours / 24)}d`
}

const getErrorMessage = (
  error: unknown,
) => {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response =
      (
        error as {
          response?: {
            data?: {
              message?: string
              error?: string
            }
          }
        }
      ).response

    if (
      response?.data?.message
    ) {
      return response
        .data.message
    }

    if (
      response?.data?.error
    ) {
      return response
        .data.error
    }
  }

  if (
    error instanceof Error
  ) {
    return error.message
  }

  return "Unable to process the approval request."
}

export default function DeptHeadApprovalPage() {
  const user =
    useAuthStore(
      (state) =>
        state.user,
    )

  const navigate =
    useNavigate()

  const queryClient =
    useQueryClient()

  const [reviewDetails, setReviewDetails] = useState<ApprovalRequestItem | null>(null)
  const [departmentFilter, setDepartmentFilter] = useState("all")
  const [sortOrder, setSortOrder] = useState("oldest")

  const [
    searchTerm,
    setSearchTerm,
  ] = useState("")

  const [
    reviewDialog,
    setReviewDialog,
  ] =
    useState<ReviewDialogState | null>(
      null,
    )

  const [
    feedback,
    setFeedback,
  ] =
    useState<FeedbackState | null>(
      null,
    )

  const role =
    normalizeRole(
      user?.role,
    )

  const isDean =
    role === "DEAN"

  const isDeanSecretary =
    role === "DEAN_SECRETARY"

  const isDeptHead =
    role === "DEPT_HEAD"

  const canAccess =
    isDean
    || isDeanSecretary
    || isDeptHead

  const approvalStep: ApprovalStep =
    isDean || isDeanSecretary
      ? "STEP3_DEAN"
      : "STEP1_DEPT_HEAD"

  const detailBasePath =
    isDean || isDeanSecretary
      ? "/dean/syllabus"
      : "/dept-head/syllabus"

  const pageTitle =
    isDean || isDeanSecretary
      ? "Final Approval Queue"
      : "Department Review Queue"

  const pageEyebrow =
    isDean || isDeanSecretary
      ? "Final Academic Review"
      : "Department Academic Review"

  const pageDescription =
    isDean || isDeanSecretary
      ? "Both Dean and Dean Secretary can perform final syllabus approval or return the syllabus for revision."
      : "Review submitted syllabuses and forward approved requests to the shared final approval queue."
  const {
    data:
    approvalRequests = [],
    isLoading,
    isFetching,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: [
      "approval-requests",
      "pending",
      approvalStep,
    ],

    queryFn: () =>
      approvalRequestApi
        .getPendingByStep(
          approvalStep,
        ),

    enabled: canAccess,
    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const reviewMutation =
    useMutation({
      mutationFn: ({
        approvalId,
        status,
        comment,
      }: {
        approvalId: number
        status: ReviewDecision
        comment: string
      }) =>
        approvalRequestApi
          .review(
            approvalId,
            {
              status,
              comment,
            },
          ),

      onSuccess: async () => {
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: ["approval-history"] }),
          ...["my-active-assignments", "program-timeline", "syllabus", "faculty-dashboard", "depthead-courses", "course-programs", "curriculum-map", "clo-plo-heatmap"].map((key) =>
            queryClient.invalidateQueries({ queryKey: [key] }),
          ),
          queryClient
            .invalidateQueries({
              queryKey: [
                "approval-requests",
              ],
            }),

          queryClient
            .invalidateQueries({
              queryKey: [
                "syllabuses",
              ],
            }),

          queryClient
            .invalidateQueries({
              queryKey: [
                "notifications",
              ],
            }),

          queryClient
            .invalidateQueries({
              queryKey: [
                "dean-dashboard",
              ],
            }),

          queryClient
            .invalidateQueries({
              queryKey: [
                "dept-head-dashboard",
              ],
            }),
        ])
      },
    })

  const filteredRequests = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()
    return approvalRequests.filter(item => {
      const matchesDepartment = departmentFilter === "all" || item.departmentCode === departmentFilter
      const searchable = [item.courseCode, item.courseName, item.instructorUsername, item.requestedByUsername,
        item.programCode, item.cohortName, item.departmentName, item.versionLabel].join(" ").toLowerCase()
      return matchesDepartment && (!keyword || searchable.includes(keyword))
    }).sort((left, right) => {
      if (sortOrder === "course") return left.courseCode.localeCompare(right.courseCode)
      const age = (Date.parse(left.createdAt) || 0) - (Date.parse(right.createdAt) || 0)
      return sortOrder === "newest" ? -age : age
    })
  }, [approvalRequests, searchTerm, departmentFilter, sortOrder])

  const oldestWaiting =
    useMemo(() => {
      if (
        approvalRequests.length === 0
      ) {
        return "—"
      }

      const sorted =
        approvalRequests
          .slice()
          .sort(
            (left, right) =>
              new Date(
                left.createdAt
                ?? left.submittedAt
                ?? 0,
              ).getTime()
              - new Date(
                right.createdAt
                ?? right.submittedAt
                ?? 0,
              ).getTime(),
          )

      const oldest =
        sorted[0]

      return getWaitingTime(
        oldest.createdAt
        ?? oldest.submittedAt,
      )
    }, [
      approvalRequests,
    ])

  const openReviewDialog = (
    item: ApprovalRequestItem,
    decision: ReviewDecision,
  ) => {
    reviewMutation.reset()
    setFeedback(null)

    setReviewDialog({
      item,
      decision,
    })
  }

  const handleReviewConfirm = (
    comment: string,
  ) => {
    if (!reviewDialog) {
      return
    }

    const {
      item,
      decision,
    } = reviewDialog

    reviewMutation.mutate(
      {
        approvalId:
          item.id,
        status:
          decision,
        comment,
      },
      {
        onSuccess: () => {
          const isApproval =
            decision
            === "APPROVED"

          let message: string

          if (isApproval) {
            message =
              isDean || isDeanSecretary
                ? "Syllabus approved successfully. This version is now the official current syllabus."
                : "Department review completed. The syllabus is now waiting for final approval."
          } else {
            message =
              "Syllabus returned for revision. The instructor can review the feedback and start a revision."
          }

          setFeedback({
            type: "success",
            message,
          })

          setReviewDialog(
            null,
          )
        },
      },
    )
  }

  if (!canAccess) {
    return <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-700">
      This page is available only to the Department Head, Dean Secretary, or Dean.
    </div>
  }

  const requestActions = (item: ApprovalRequestItem) => <div className="flex flex-wrap gap-2 md:justify-end">
    <Button variant="outline" size="sm" onClick={() => navigate(`${detailBasePath}/${item.syllabusId}`)}>
      <Eye className="size-3.5" />Syllabus
    </Button>
    <Button variant="outline" size="sm" className="border-teal-200 bg-teal-50 text-[#007d84] hover:bg-teal-100"
      onClick={() => setReviewDetails(item)} aria-label={`View review for ${item.courseCode}`}>
      <History className="size-3.5" />View Review
    </Button>
    <Button size="sm" className="bg-[#007d84] text-white hover:bg-[#006d73]" disabled={reviewMutation.isPending}
      onClick={() => openReviewDialog(item, "APPROVED")}>
      <CheckCircle2 className="size-3.5" />{isDean || isDeanSecretary ? "Final Approve" : "Approve & Forward"}
    </Button>
    <Button variant="outline" size="sm" className="border-rose-200 text-rose-700 hover:bg-rose-50"
      disabled={reviewMutation.isPending} onClick={() => openReviewDialog(item, "REJECTED")}>
      <XCircle className="size-3.5" />Return for Revision
    </Button>
  </div>

  const pendingLabel = (item: ApprovalRequestItem) => <Badge variant="outline" className="border-amber-200 bg-amber-50 text-amber-800">
    <Clock3 className="mr-1 size-3" />{item.status === "PENDING" ? "Pending" : item.status.replaceAll("_", " ")}
  </Badge>

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#cfe1e4] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-1 bg-[#007d84]" />
        <div className="flex flex-col gap-5 p-5 sm:p-7 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-widest text-[#007d84]"><ShieldCheck className="size-4" />{pageEyebrow}</p>
            <h1 className="mt-3 text-2xl font-bold tracking-tight text-[#17343d] sm:text-3xl">{pageTitle}</h1>
            <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-500">{pageDescription}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => navigate(`${detailBasePath}?history=1`)}><History className="size-4" />Review History</Button>
            <Button variant="outline" disabled={isFetching} onClick={() => void refetch()}><RefreshCw className={`size-4 ${isFetching ? "animate-spin" : ""}`} />Refresh Queue</Button>
          </div>
        </div>
        <ol aria-label="Syllabus approval workflow" className="grid gap-3 border-t border-[#e1ecee] bg-[#f4faf9] p-5 sm:grid-cols-3 sm:px-7">
          {[
            ["01", "Instructor submission", "A syllabus version is submitted for review."],
            ["02", "Department review", "Department Head reviews and forwards."],
            ["03", "Shared final review", "Dean or Dean Secretary makes the final decision."],
          ].map(([number, title, description]) => <li key={number} className="flex gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-lg border border-teal-200 bg-white text-xs font-bold text-[#007d84]">{number}</span>
            <div><p className="text-xs font-semibold text-[#17343d]">{title}</p><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></div>
          </li>)}
        </ol>
      </section>

      <section className="grid gap-3 sm:grid-cols-3">
        <MetricCard label="Pending requests" value={isLoading || isError ? "—" : approvalRequests.length}
          description="Syllabuses awaiting a decision" tone="info" icon={<Inbox className="size-5" />} />
        <MetricCard label="Your review authority" value={isDeptHead ? "Department Head" : isDeanSecretary ? "Dean Secretary" : "Dean"}
          description={isDeptHead ? "Department academic review" : "Final academic approval"} tone="success" icon={<ShieldCheck className="size-5" />} />
        <MetricCard label="Longest waiting" value={isLoading || isError ? "—" : oldestWaiting}
          description="Time since this review request was received" tone="warning" icon={<Clock3 className="size-5" />} />
      </section>

      {feedback && <section role={feedback.type === "error" ? "alert" : "status"}
        className={`flex items-start justify-between gap-4 rounded-xl border px-4 py-3 text-sm ${feedback.type === "success" ? "border-emerald-200 bg-emerald-50 text-emerald-800" : "border-rose-200 bg-rose-50 text-rose-800"}`}>
        <div className="flex gap-2">{feedback.type === "success" ? <CheckCircle2 className="mt-0.5 size-4 shrink-0" /> : <XCircle className="mt-0.5 size-4 shrink-0" />}<span>{feedback.message}</span></div>
        <button type="button" aria-label="Dismiss review feedback" onClick={() => setFeedback(null)} className="rounded p-1 hover:bg-white/60"><X className="size-4" /></button>
      </section>}

      <section aria-busy={isFetching} className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="space-y-4 border-b border-slate-100 p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div><h2 className="font-bold text-[#17343d]">Syllabuses awaiting review</h2>
              <p className="mt-1 text-xs text-slate-500">Open the syllabus or inspect its review trail before recording your decision.</p></div>
            {!isLoading && !isError && <Badge variant="outline" className="text-slate-500">{filteredRequests.length} of {approvalRequests.length} requests</Badge>}
          </div>
          <div className="grid gap-3 md:grid-cols-[1fr_220px_180px]">
            <div className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input aria-label="Search review queue" value={searchTerm} onChange={event => setSearchTerm(event.target.value)}
                placeholder="Search course, instructor, program, version…" className="h-10 pl-9" /></div>
            <select aria-label="Filter review queue by department" value={departmentFilter} onChange={event => setDepartmentFilter(event.target.value)}
              className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700 focus:ring-2 focus:ring-[#007d84]/20">
              <option value="all">All departments</option>
              {[...new Set(approvalRequests.map(item => item.departmentCode).filter((value): value is string => Boolean(value)))].sort()
                .map(value => <option key={value} value={value}>{value}</option>)}
            </select>
            <select aria-label="Sort review queue" value={sortOrder} onChange={event => setSortOrder(event.target.value)}
              className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700 focus:ring-2 focus:ring-[#007d84]/20">
              <option value="oldest">Oldest received first</option><option value="newest">Newest received first</option><option value="course">Course code A–Z</option>
            </select>
          </div>
          {(searchTerm || departmentFilter !== "all") && <Button size="sm" variant="ghost" className="text-[#007d84]"
            onClick={() => { setSearchTerm(""); setDepartmentFilter("all") }}><X className="size-3.5" />Clear filters</Button>}
        </div>

        {isLoading ? <div role="status" className="space-y-4 p-6">
          <p className="flex items-center gap-2 text-sm text-slate-500"><LoaderCircle className="size-4 animate-spin" />Loading review queue…</p>
          {[1, 2, 3].map(value => <div key={value} aria-hidden="true" className="h-24 animate-pulse rounded-xl bg-slate-50" />)}
        </div> : isError ? <div role="alert" className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-5">
          <p className="font-semibold text-rose-800">Unable to load approval requests</p><p className="mt-2 text-sm text-rose-700">{getErrorMessage(error)}</p>
          <Button size="sm" variant="outline" className="mt-4" onClick={() => void refetch()}><RefreshCw className="size-4" />Try again</Button>
        </div> : !filteredRequests.length ? <div className="flex flex-col items-center px-5 py-14 text-center">
          <span className="rounded-2xl bg-[#eef7f5] p-4 text-[#007d84]"><Inbox className="size-7" /></span>
          <h3 className="mt-4 font-semibold text-[#17343d]">{searchTerm || departmentFilter !== "all" ? "No requests match your filters" : "Your review queue is up to date"}</h3>
          <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{searchTerm || departmentFilter !== "all"
            ? "Adjust the search or department to see more requests."
            : isDeptHead ? "New requests will appear when an instructor submits a syllabus in your managed major."
              : "New requests will appear after Department Head approval."}</p>
        </div> : <>
          <div className="divide-y divide-slate-100 md:hidden">
            {filteredRequests.map(item => <article key={item.id} className="space-y-4 p-5">
              <div className="flex items-start justify-between gap-3"><div><p className="font-mono text-xs font-bold text-[#007d84]">{item.courseCode}</p>
                <h3 className="mt-1 font-semibold text-[#17343d]">{item.courseName}</h3></div>{pendingLabel(item)}</div>
              <dl className="grid grid-cols-2 gap-3 text-xs">
                <div><dt className="text-slate-500">Version</dt><dd className="mt-1 font-semibold text-slate-700">{formatVersionLabel(item.versionNumber, item.versionLabel)}</dd></div>
                <div><dt className="text-slate-500">Instructor</dt><dd className="mt-1 break-words font-semibold text-slate-700">{item.instructorUsername || "Not recorded"}</dd></div>
                <div><dt className="text-slate-500">Program / Cohort</dt><dd className="mt-1 text-slate-700">{item.programCode || "—"} · {item.cohortName || "—"}</dd></div>
                <div><dt className="text-slate-500">Received</dt><dd className="mt-1 text-slate-700">{formatDateTime(item.createdAt)} · {getWaitingTime(item.createdAt)}</dd></div>
              </dl>
              <p className="text-xs text-slate-500">Review authority: {isDeptHead ? "Department Head" : "Dean / Dean Secretary"}</p>
              {requestActions(item)}
            </article>)}
          </div>
          <div className="hidden overflow-x-auto md:block">
            <Table className="min-w-[1020px]">
              <TableHeader className="bg-slate-50"><TableRow>
                <TableHead className="min-w-[240px]">Course / Curriculum</TableHead><TableHead>Instructor</TableHead>
                <TableHead>Version</TableHead><TableHead>Received</TableHead><TableHead>Status / Review authority</TableHead><TableHead className="min-w-[270px] text-right">Actions</TableHead>
              </TableRow></TableHeader>
              <TableBody>{filteredRequests.map(item => <TableRow key={item.id} className="align-top hover:bg-[#f7fbfa]">
                <TableCell className="py-5"><p className="font-mono text-xs font-bold text-[#007d84]">{item.courseCode}</p>
                  <p className="mt-1 max-w-xs font-semibold text-[#17343d]">{item.courseName}</p>
                  <p className="mt-2 text-xs text-slate-500">{item.programCode || "—"} · {item.cohortName || "—"} · {item.semester || "—"}</p>
                  <p className="mt-1 text-xs text-slate-400">{item.departmentName || item.departmentCode || "Department not recorded"}</p></TableCell>
                <TableCell className="py-5"><div className="flex items-center gap-2"><UserRound className="size-4 shrink-0 text-slate-400" /><span className="break-words text-sm text-slate-700">{item.instructorUsername || "Not recorded"}</span></div>
                  <p className="mt-2 text-xs text-slate-400">Sent by {item.requestedByUsername || "Not recorded"}</p></TableCell>
                <TableCell className="py-5"><Badge variant="outline">{formatVersionLabel(item.versionNumber, item.versionLabel)}</Badge></TableCell>
                <TableCell className="py-5"><p className="whitespace-nowrap text-xs text-slate-600">{formatDateTime(item.createdAt)}</p>
                  <p className="mt-2 flex items-center gap-1 text-xs text-amber-700"><Clock3 className="size-3.5" />Waiting {getWaitingTime(item.createdAt)}</p></TableCell>
                <TableCell className="py-5">{pendingLabel(item)}<p className="mt-2 text-xs text-slate-500">{isDeptHead ? "Department Head" : "Dean / Dean Secretary"}</p>
                  {item.reviewedByUsername && <p className="mt-1 text-xs text-slate-500">Reviewed by {item.reviewedByUsername}</p>}</TableCell>
                <TableCell className="py-5">{requestActions(item)}</TableCell>
              </TableRow>)}</TableBody>
            </Table>
          </div>
        </>}
      </section>

      {reviewDetails && <SyllabusReviewDetailsDialog
        syllabus={{ id: reviewDetails.syllabusId, courseCode: reviewDetails.courseCode, courseName: reviewDetails.courseName,
          versionNumber: reviewDetails.versionNumber, versionLabel: reviewDetails.versionLabel }}
        syllabusPath={`${detailBasePath}/${reviewDetails.syllabusId}`} onClose={() => setReviewDetails(null)} />}
      {reviewDialog && <ApprovalReviewDialog key={`${reviewDialog.item.id}-${reviewDialog.decision}`} open
        item={reviewDialog.item} decision={reviewDialog.decision} isDean={isDean} isDeanSecretary={isDeanSecretary}
        isPending={reviewMutation.isPending} errorMessage={reviewMutation.isError ? getErrorMessage(reviewMutation.error) : null}
        onOpenChange={open => { if (!open) { setReviewDialog(null); reviewMutation.reset() } }} onConfirm={handleReviewConfirm} />}
    </div>
  )
}

function MetricCard({
  label,
  value,
  description,
  icon,
  tone,
}: {
  label: string
  value: string | number
  description: string
  icon: ReactNode
  tone:
  | "warning"
  | "info"
  | "success"
}) {
  const toneClass =
    tone === "warning"
      ? "border-amber-200 bg-amber-50/40"
      : tone === "info"
        ? "border-blue-200 bg-blue-50/40"
        : "border-emerald-200 bg-emerald-50/40"

  const iconClass =
    tone === "warning"
      ? "bg-amber-100 text-amber-700"
      : tone === "info"
        ? "bg-blue-100 text-blue-700"
        : "bg-emerald-100 text-emerald-700"

  return (
    <Card
      className={`${toneClass} shadow-sm`}
    >
      <CardContent className="flex min-h-[120px] items-start justify-between p-5">
        <div className="min-w-0">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-2xl font-bold text-slate-900">
            {value}
          </p>

          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        </div>

        <span className={`ml-3 flex size-10 shrink-0 items-center justify-center rounded-xl ${iconClass}`}>
          {icon}
        </span>
      </CardContent>
    </Card>
  )
}
