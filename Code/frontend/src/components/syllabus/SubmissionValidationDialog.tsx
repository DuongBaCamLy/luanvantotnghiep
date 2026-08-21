import { useMemo } from "react"
import {
  AlertCircle,
  CheckCircle2,
  ClipboardCheck,
  ExternalLink,
  Send,
} from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import type {
  SubmissionValidationIssue,
  SubmissionValidationResponse,
} from "@/types/syllabus"

interface Props {
  open: boolean
  validation: SubmissionValidationResponse | null
  submitting?: boolean
  onOpenChange: (open: boolean) => void
  onSubmit?: () => void
  onGoToSection?: (tabId: number) => void
}

const groupIssues = (issues: SubmissionValidationIssue[]) =>
  issues.reduce<Record<string, SubmissionValidationIssue[]>>((groups, issue) => {
    if (!groups[issue.section]) groups[issue.section] = []
    groups[issue.section].push(issue)
    return groups
  }, {})

export default function SubmissionValidationDialog({
  open,
  validation,
  submitting = false,
  onOpenChange,
  onSubmit,
  onGoToSection,
}: Props) {
  const grouped = useMemo(
    () => groupIssues(validation?.issues ?? []),
    [validation]
  )

  if (!validation) return null

  const firstTabId = validation.issues[0]?.tabId

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[86vh] overflow-hidden p-0 gap-0">
        <DialogHeader className={`p-5 border-b ${
          validation.valid
            ? "bg-emerald-50 border-emerald-100"
            : "bg-rose-50 border-rose-100"
        }`}>
          <div className="flex items-start gap-3 pr-7">
            <div className={`size-10 rounded-full flex items-center justify-center shrink-0 ${
              validation.valid
                ? "bg-emerald-100 text-emerald-700"
                : "bg-rose-100 text-rose-700"
            }`}>
              {validation.valid
                ? <CheckCircle2 className="size-5" />
                : <AlertCircle className="size-5" />}
            </div>
            <div>
              <DialogTitle className={validation.valid ? "text-emerald-900" : "text-rose-900"}>
                {validation.valid
                  ? "The syllabus is ready for submission"
                  : `The syllabus cannot be submitted — ${validation.errorCount} errors remaining`}
              </DialogTitle>
              <DialogDescription className={validation.valid ? "text-emerald-700" : "text-rose-700"}>
                {validation.message}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="overflow-y-auto px-5 py-4 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <p className="text-xs text-slate-500">Total Assessment Weight</p>
              <p className={`text-lg font-bold ${
                validation.assessmentTotalWeight === 100
                  ? "text-emerald-700"
                  : "text-rose-700"
              }`}>
                {validation.assessmentTotalWeight ?? 0}% / 100%
              </p>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <p className="text-xs text-slate-500">Declared Workload</p>
              <p className="text-sm font-semibold text-slate-800 mt-1">
                Total {validation.workloadTotal ?? "—"} · Contact {validation.workloadContact ?? "—"} · Self-study {validation.workloadPrivate ?? "—"}
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Topics: contact {validation.topicContactHours ?? 0} · self-study {validation.topicPrivateHours ?? 0}
              </p>
            </div>
          </div>

          {validation.valid ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50/60 p-4 flex gap-3">
              <ClipboardCheck className="size-5 text-emerald-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-emerald-900">All mandatory requirements are satisfied</p>
                <p className="text-sm text-emerald-700 mt-1">
                  When you submit, the system will create an immutable snapshot and forward the syllabus to the Head of Department.
                </p>
              </div>
            </div>
          ) : (
            (Object.entries(grouped) as [string, SubmissionValidationIssue[]][]).map(([section, issues]) => (
              <div key={section} className="rounded-lg border border-slate-200 overflow-hidden">
                <div className="px-4 py-2.5 bg-slate-50 border-b border-slate-200 flex items-center justify-between gap-3">
                  <p className="font-semibold text-sm text-slate-800">{section}</p>
                  {onGoToSection && issues[0]?.tabId && (
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 text-xs gap-1 text-primary"
                      onClick={() => onGoToSection(issues[0].tabId)}
                    >
                      <ExternalLink className="size-3.5" />
                      Go to this section
                    </Button>
                  )}
                </div>
                <ul className="divide-y divide-slate-100">
                  {issues.map((issue, index) => (
                    <li key={`${issue.code}-${index}`} className="px-4 py-3 flex items-start gap-2.5">
                      <span className="mt-1 size-1.5 rounded-full bg-rose-500 shrink-0" />
                      <div>
                        <p className="text-sm text-slate-800">{issue.message}</p>
                        <p className="text-[11px] text-slate-400 mt-0.5 font-mono">{issue.code}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            ))
          )}
        </div>

        <DialogFooter className="m-0 rounded-none px-5 py-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {validation.valid ? "Cancel" : "Close"}
          </Button>
          {!validation.valid && onGoToSection && firstTabId && (
            <Button
              onClick={() => onGoToSection(firstTabId)}
              className="bg-slate-900 text-white hover:bg-slate-800"
            >
              Fix First Error
            </Button>
          )}
          {validation.valid && onSubmit && (
            <Button
              onClick={onSubmit}
              disabled={submitting}
              className="bg-primary text-white hover:bg-primary/90 gap-2"
            >
              <Send className="size-4" />
              {submitting ? "Submitting..." : "Confirm Submission"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
