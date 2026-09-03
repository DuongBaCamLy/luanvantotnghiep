import { useMemo, useState } from "react"
import {
  CheckCircle2,
  Loader2,
  MailCheck,
  XCircle,
} from "lucide-react"

import type { ApprovalRequestItem } from "@/types/approval"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"

export type ReviewDecision =
  | "APPROVED"
  | "REJECTED"

interface ApprovalReviewDialogProps {
  open: boolean
  item: ApprovalRequestItem | null
  decision: ReviewDecision
  isDean: boolean
  isPending: boolean
  errorMessage?: string | null
  onOpenChange: (
    open: boolean,
  ) => void
  onConfirm: (
    comment: string,
  ) => void
}

export default function ApprovalReviewDialog({
  open,
  item,
  decision,
  isDean,
  isPending,
  errorMessage,
  onOpenChange,
  onConfirm,
}: ApprovalReviewDialogProps) {
  const [comment, setComment] =
    useState("")

  const [
    validationError,
    setValidationError,
  ] = useState("")

  const isApproval =
    decision === "APPROVED"

  const copy = useMemo(() => {
    if (isApproval) {
      return {
        title: isDean
          ? "Approve Syllabus as Final"
          : "Approve and Forward to Dean",

        description: isDean
          ? "Approve this syllabus as the final official version?"
          : "Confirm the Department Head review and forward this syllabus to the Dean for the final decision.",

        button: isDean
          ? "Final Approve"
          : "Approve & Forward",
      }
    }

    return {
      title: isDean ? "Reject Syllabus" : "Return Syllabus for Revision",

      description:
        "Return this syllabus to the instructor with a clear review comment explaining the required changes.",

      button: isDean ? "Reject & Return" : "Return to Instructor",
    }
  }, [isApproval, isDean])

  const actorLabel =
    isDean
      ? "Forwarded By"
      : "Submitted By"

  const stageLabel =
    isDean
      ? "Dean Final Review"
      : "Department Review"

  const handleConfirm = () => {
    const normalizedComment =
      comment.trim()

    if (
      !isApproval
      && !normalizedComment
    ) {
      setValidationError(
        "A review comment is required when returning a syllabus for revision.",
      )
      return
    }

    setValidationError("")
    onConfirm(normalizedComment)
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!isPending) {
          onOpenChange(nextOpen)
        }
      }}
    >
      <DialogContent className="overflow-hidden p-0 sm:max-w-[620px]">
        <div
          className={
            isApproval
              ? "bg-gradient-to-br from-emerald-600 to-teal-700 px-6 py-5 text-white"
              : "bg-gradient-to-br from-rose-600 to-orange-600 px-6 py-5 text-white"
          }
        >
          <div className="flex items-start gap-4">
            <div className="rounded-2xl bg-white/15 p-3 ring-1 ring-white/20">
              {isApproval ? (
                <CheckCircle2 className="size-7" />
              ) : (
                <XCircle className="size-7" />
              )}
            </div>

            <DialogHeader className="space-y-1 text-left">
              <DialogTitle className="text-xl text-white">
                {copy.title}
              </DialogTitle>

              <DialogDescription className="text-white/80">
                {copy.description}
              </DialogDescription>
            </DialogHeader>
          </div>
        </div>

        <div className="space-y-5 px-6 py-5">
          {item && (
            <div className="grid gap-3 rounded-xl border border-slate-200 bg-slate-50/80 p-4 sm:grid-cols-2">
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Course
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {item.courseCode}
                  {" · "}
                  {item.courseName}
                </p>
              </div>

              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Version
                </p>

                <p className="mt-1 font-semibold text-slate-900">
                  {item.versionLabel
                    || `v${item.versionNumber}`}
                </p>
              </div>

              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  {actorLabel}
                </p>

                <p className="mt-1 text-sm font-medium text-slate-800">
                  {item.requestedByUsername || "—"}
                </p>
              </div>

              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                  Review Stage
                </p>

                <p className="mt-1 text-sm font-medium text-slate-800">
                  {stageLabel}
                </p>
              </div>
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="review-comment">
              {isApproval
                ? "Review Comment (optional)"
                : "Required Revision Comment"}

              {!isApproval && (
                <span className="ml-1 text-rose-600">
                  *
                </span>
              )}
            </Label>

            <Textarea
              id="review-comment"
              value={comment}
              onChange={(event) => {
                setComment(
                  event.target.value,
                )

                if (validationError) {
                  setValidationError("")
                }
              }}
              maxLength={4000}
              rows={5}
              placeholder={
                isApproval
                  ? "Optional note for the approval history..."
                  : "Explain what needs to be revised and the expected correction..."
              }
              className="min-h-32 resize-y"
              disabled={isPending}
            />

            <div className="flex items-center justify-between text-xs">
              <span className="text-rose-600">
                {validationError
                  || errorMessage
                  || ""}
              </span>

              <span className="text-slate-400">
                {comment.length}/4000
              </span>
            </div>
          </div>

          {!isApproval && (
            <div className="rounded-xl border border-rose-100 bg-rose-50 p-4 text-sm text-rose-900">
              <p className="font-semibold">
                Revision outcome
              </p>

              <p className="mt-1 leading-6 text-rose-800">
                The syllabus will be returned to the instructor with your comment so it can be revised and submitted again.
              </p>
            </div>
          )}

          <div className="flex gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900">
            <MailCheck className="mt-0.5 size-5 shrink-0 text-blue-600" />

            <div>
              <p className="font-semibold">
                Notification
              </p>

              <p className="mt-1 leading-6 text-blue-800/80">
                Relevant users will be notified after the review decision is saved.
              </p>
            </div>
          </div>
        </div>

        <DialogFooter className="border-t bg-slate-50 px-6 py-4">
          <Button
            type="button"
            variant="outline"
            disabled={isPending}
            onClick={() =>
              onOpenChange(false)
            }
          >
            Cancel
          </Button>

          <Button
            type="button"
            disabled={isPending}
            className={
              isApproval
                ? "bg-emerald-600 hover:bg-emerald-700"
                : "bg-rose-600 hover:bg-rose-700"
            }
            onClick={handleConfirm}
          >
            {isPending && (
              <Loader2 className="mr-2 size-4 animate-spin" />
            )}

            {copy.button}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
