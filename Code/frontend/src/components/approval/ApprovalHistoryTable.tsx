import { useId, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { isAxiosError } from "axios"
import { ArrowUpRight, CheckCircle2, Clock3, History, MessageSquareText, RefreshCw, Send, ShieldCheck, XCircle } from "lucide-react"

import { approvalRequestApi } from "@/api/approvalRequestApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { formatDateTime } from "@/i18n"
import { approvalStageLabel, buildApprovalTimeline } from "@/lib/approvalTimeline"
import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useAuthStore } from "@/store/authStore"
import type { ApprovalStatus } from "@/types/approval"

interface ApprovalHistoryTableProps {
  syllabusId: number
  initialVersionNumber?: number
  onOpenSyllabus?: () => void
}

const statusStyles: Record<ApprovalStatus, string> = {
  PENDING: "border-amber-200 bg-amber-50 text-amber-800",
  APPROVED: "border-emerald-200 bg-emerald-50 text-emerald-800",
  REJECTED: "border-rose-200 bg-rose-50 text-rose-800",
  REVISION_REQUESTED: "border-orange-200 bg-orange-50 text-orange-800",
}
const statusLabels: Record<ApprovalStatus, string> = {
  PENDING: "Pending", APPROVED: "Approved", REJECTED: "Rejected", REVISION_REQUESTED: "Revision Requested",
}
const selectClass = "h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700 outline-none focus:border-[#007d84] focus:ring-2 focus:ring-[#007d84]/15"

export default function ApprovalHistoryTable({ syllabusId, initialVersionNumber, onOpenSyllabus }: ApprovalHistoryTableProps) {
  const id = useId()
  const user = useAuthStore(state => state.user)
  const [selectedVersion, setSelectedVersion] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState("all")
  const { data: history = [], isLoading, isError, error, isFetching, refetch } = useQuery({
    queryKey: ["approval-history", syllabusId, user?.userId],
    queryFn: () => approvalRequestApi.getApprovalHistory(syllabusId),
    enabled: Boolean(user) && Number.isInteger(syllabusId) && syllabusId > 0,
    retry: (count, failure) => !(isAxiosError(failure) && [401, 403].includes(failure.response?.status ?? 0)) && count < 2,
  })
  const versions = [...new Set([
    ...history.map(item => item.versionNumber),
    ...(initialVersionNumber ? [initialVersionNumber] : []),
  ])].filter(version => Number.isInteger(version) && version > 0).sort((a, b) => b - a)
  const version = Number(selectedVersion ?? initialVersionNumber ?? versions[0] ?? 0)
  const events = buildApprovalTimeline(history, version, statusFilter)
  const versionRecords = history.filter(item => item.versionNumber === version)
  const feedbackCount = versionRecords.filter(item => item.comment?.trim()).length
  const permissionError = isAxiosError(error) && [401, 403].includes(error.response?.status ?? 0)

  return (
    <section aria-label="Submission and review timeline" aria-busy={isFetching}
      className="overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
      <div className="border-b border-[#e1ecee] bg-[#f4faf9] px-5 py-5 sm:px-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex gap-3">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#007d84] text-white"><History className="size-5" /></span>
            <div>
              <h2 className="text-lg font-bold text-[#17343d]">Submission & Review Timeline</h2>
              <p className="mt-1 max-w-xl text-sm leading-6 text-slate-500">Follow each submission, decision and feedback for the selected syllabus version.</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {onOpenSyllabus && <Button variant="outline" size="sm" onClick={onOpenSyllabus}><ArrowUpRight className="size-4" />Open current syllabus</Button>}
            <Button variant="outline" size="sm" disabled={isFetching} onClick={() => void refetch()} aria-label="Refresh review history">
              <RefreshCw className={`size-4 ${isFetching ? "animate-spin" : ""}`} />Refresh
            </Button>
          </div>
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-[180px_220px_1fr] sm:items-end">
          <div><label htmlFor={`${id}-version`} className="mb-1.5 block text-xs font-semibold text-slate-600">Syllabus version</label>
            <select id={`${id}-version`} className={selectClass} value={version || ""} disabled={isLoading || isError || !versions.length}
              onChange={event => setSelectedVersion(event.target.value)}>
              {!versions.length && <option value="">No versions yet</option>}
              {versions.map(value => <option key={value} value={value}>{formatVersionLabel(value)}{value === initialVersionNumber ? " · current" : ""}</option>)}
            </select>
          </div>
          <div><label htmlFor={`${id}-status`} className="mb-1.5 block text-xs font-semibold text-slate-600">Review decision</label>
            <select id={`${id}-status`} className={selectClass} value={statusFilter} onChange={event => setStatusFilter(event.target.value)}>
              <option value="all">All decisions</option>
              {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
          <p className="text-xs leading-5 text-slate-500 sm:pb-2 sm:text-right">
            {!isLoading && !isError && <>{versionRecords.length} review requests · {feedbackCount} feedback notes</>}
          </p>
        </div>
        {initialVersionNumber && version !== initialVersionNumber && <p className="mt-3 text-xs leading-5 text-amber-800">
          Viewing feedback for {formatVersionLabel(version)}. The current syllabus content is {formatVersionLabel(initialVersionNumber)}.
        </p>}
      </div>

      {isLoading ? <div role="status" className="space-y-5 p-6">
        <p className="text-sm text-slate-500">Loading review history…</p>
        {[1, 2, 3].map(value => <div key={value} className="flex animate-pulse gap-4" aria-hidden="true">
          <div className="size-9 rounded-full bg-slate-100" /><div className="flex-1 space-y-2"><div className="h-4 w-2/3 rounded bg-slate-100" /><div className="h-14 rounded-xl bg-slate-50" /></div>
        </div>)}
      </div> : isError ? <div role="alert" className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-5">
        <p className="font-semibold text-rose-800">{permissionError ? "Review history is unavailable for this account." : "Unable to load review history."}</p>
        <p className="mt-2 text-sm text-rose-700">{isAxiosError<{ message?: string }>(error) ? error.response?.data?.message || error.message : "Please try again."}</p>
        <Button variant="outline" size="sm" className="mt-3" onClick={() => void refetch()}>Try again</Button>
      </div> : !events.length ? <div className="flex flex-col items-center px-6 py-12 text-center">
        <span className="mb-4 rounded-2xl bg-[#f0f7f6] p-4 text-[#007d84]"><MessageSquareText className="size-6" /></span>
        <h3 className="font-semibold text-[#17343d]">{statusFilter === "all" ? "No review activity for this version" : "No matching review decisions"}</h3>
        <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{statusFilter === "all" ? "Submission and feedback will appear here once this version enters the review workflow." : "Choose another decision or version to explore the review history."}</p>
        {statusFilter !== "all" && <Button variant="outline" size="sm" className="mt-4" onClick={() => setStatusFilter("all")}>Show all decisions</Button>}
      </div> : <ol className="px-5 py-6 sm:px-6">
        {events.map((event, index) => {
          const { item, kind } = event
          const status = item.status
          const Icon = kind === "submission" ? Send : kind === "pending" ? Clock3 : status === "APPROVED" ? CheckCircle2 : status === "REJECTED" ? XCircle : MessageSquareText
          const title = kind === "submission"
            ? item.step === "STEP1_DEPT_HEAD" ? "Submitted for department review" : item.step === "STEP2_PROG_COORDINATOR" ? "Sent for program review" : "Sent for final review"
            : kind === "pending" ? "Awaiting review" : status ? statusLabels[status] : "Review decision recorded"
          return <li key={event.key} className="relative flex gap-3 pb-6 last:pb-0 sm:gap-4">
            {index < events.length - 1 && <span aria-hidden="true" className="absolute left-[17px] top-9 bottom-0 w-px bg-slate-200" />}
            <span className={`relative z-10 flex size-9 shrink-0 items-center justify-center rounded-full border ${kind === "submission" ? "border-teal-200 bg-teal-50 text-[#007d84]" : status ? statusStyles[status] : "border-slate-200 bg-slate-50 text-slate-500"}`}><Icon className="size-4" /></span>
            <div className={`min-w-0 flex-1 rounded-xl border p-4 sm:p-5 ${kind === "pending" ? "border-dashed border-amber-200 bg-amber-50/40" : "border-slate-200 bg-white"}`}>
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div><h3 className="text-sm font-semibold text-[#17343d]">{title}</h3><p className="mt-1 text-xs text-slate-500">{approvalStageLabel(item.step)} · {formatVersionLabel(item.versionNumber, item.versionLabel)}</p></div>
                {kind !== "submission" && status && <Badge variant="outline" className={statusStyles[status]}>{statusLabels[status]}</Badge>}
              </div>
              <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-600">
                <span className="flex items-center gap-1.5"><ShieldCheck className="size-3.5 text-slate-400" />
                  {kind === "pending" ? `Review authority: ${approvalStageLabel(item.step)}` : `${kind === "submission" ? "Sent by" : "Reviewed by"}: ${event.actor || "Not recorded"}`}
                </span>
                {kind !== "pending" && <span className="flex items-center gap-1.5"><Clock3 className="size-3.5 text-slate-400" />{event.timestamp ? formatDateTime(event.timestamp) : "Time not recorded"}</span>}
              </div>
              {kind === "decision" && (item.comment?.trim() ? <div className="mt-4 rounded-lg border border-slate-100 bg-slate-50 p-3">
                <p className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-slate-500"><MessageSquareText className="size-3.5" />Reviewer feedback</p>
                <p className="whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">{item.comment}</p>
              </div> : <p className="mt-3 text-xs italic text-slate-400">No feedback comment recorded.</p>)}
            </div>
          </li>
        })}
      </ol>}
    </section>
  )
}
