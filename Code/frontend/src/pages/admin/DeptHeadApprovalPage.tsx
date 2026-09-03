import type { ReactNode } from "react"
import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"
import {
  ArrowRight,
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

  const isDeptHead =
    role === "DEPT_HEAD"

  const canAccess =
    isDean
    || isDeptHead

  const approvalStep:
    ApprovalStep =
      isDean
        ? "STEP3_DEAN"
        : "STEP1_DEPT_HEAD"

  const detailBasePath =
    isDean
      ? "/dean/syllabus"
      : "/dept-head/syllabus"

  const pageTitle =
    isDean
      ? "Dean Approval Queue"
      : "Department Review Queue"

  const pageEyebrow =
    isDean
      ? "Final Academic Review"
      : "Department Academic Review"

  const pageDescription =
    isDean
      ? "Perform the final syllabus decision. Approve to publish the official current version, or return the syllabus to the instructor with mandatory revision feedback."
      : "Review syllabuses submitted by instructors in your department. Approve to forward them to the Dean, or return them to the instructor with mandatory revision feedback."

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

  const filteredRequests =
    useMemo(() => {
      const keyword =
        searchTerm
          .trim()
          .toLowerCase()

      if (!keyword) {
        return approvalRequests
      }

      return approvalRequests
        .filter(
          (item) => {
            const searchable =
              [
                item.courseCode,
                item.courseName,
                item.requestedByUsername,
                item.versionLabel,
              ]
                .map(
                  (value) =>
                    String(
                      value ?? "",
                    )
                      .toLowerCase(),
                )
                .join(" ")

            return searchable
              .includes(keyword)
          },
        )
    }, [
      approvalRequests,
      searchTerm,
    ])

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
              isDean
                ? "Syllabus approved successfully. This version is now the official current syllabus."
                : "Department review completed. The syllabus has been forwarded to the Dean for final review."
          } else {
            message =
              "The same syllabus version was returned to the instructor for revision."
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
    return (
      <div className="mx-auto max-w-2xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-700">
        This page is available only to the Department Head or Dean.
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2 text-[#007d84]">
              <ShieldCheck className="size-5" />

              <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                {pageEyebrow}
              </span>
            </div>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              {pageTitle}
            </h1>

            <p className="mt-1 max-w-4xl text-sm leading-6 text-[#687f89]">
              {pageDescription}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                navigate(
                  `${detailBasePath}?history=1`,
                )
              }
            >
              <History className="size-4" />
              Review History
            </Button>

            <Button
              type="button"
              variant="outline"
              disabled={isFetching}
              onClick={() =>
                void refetch()
              }
            >
              {isFetching ? (
                <LoaderCircle className="size-4 animate-spin" />
              ) : (
                <RefreshCw className="size-4" />
              )}
              Refresh Queue
            </Button>
          </div>
        </div>
      </section>

      <section className="rounded-xl border border-[#cfe1e4] bg-[#f7fbfb] px-5 py-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

            <div>
              <p className="font-semibold text-[#17343d]">
                Two-level syllabus approval workflow
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-600">
                Instructor submission
                {" → "}
                Department review
                {" → "}
                Dean final review.
                Returning a syllabus for revision requires reviewer feedback and preserves the reviewed version in history.
              </p>
            </div>
          </div>

          <div className="hidden items-center gap-2 text-[11px] lg:flex">
            <span className="rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-600">
              Instructor submitted
            </span>

            <ArrowRight className="size-3.5 text-slate-400" />

            <span
              className={
                isDeptHead
                  ? "rounded-md bg-[#eaf7f7] px-2.5 py-1.5 font-semibold text-[#007d84]"
                  : "rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-600"
              }
            >
              Department review
            </span>

            <ArrowRight className="size-3.5 text-slate-400" />

            <span
              className={
                isDean
                  ? "rounded-md bg-blue-50 px-2.5 py-1.5 font-semibold text-blue-700"
                  : "rounded-md bg-slate-100 px-2.5 py-1.5 text-slate-600"
              }
            >
              Dean review
            </span>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <MetricCard
          label="Pending Review"
          value={
            approvalRequests.length
          }
          description="Requests waiting at your current approval step"
          tone={
            approvalRequests.length
              > 0
              ? "warning"
              : "success"
          }
          icon={
            <Clock3 className="size-5" />
          }
        />

        <MetricCard
          label="Current Review Level"
          value={
            isDean
              ? "Dean"
              : "Department Head"
          }
          description={
            isDean
              ? "Final academic decision"
              : "Department decision before Dean"
          }
          tone="info"
          icon={
            <ShieldCheck className="size-5" />
          }
        />

        <MetricCard
          label="Oldest Waiting"
          value={oldestWaiting}
          description="Elapsed time for the oldest request in this queue"
          tone={
            approvalRequests.length
              > 0
              ? "warning"
              : "success"
          }
          icon={
            <Clock3 className="size-5" />
          }
        />
      </section>

      {feedback && (
        <section
          className={
            feedback.type
              === "success"
              ? "flex items-start justify-between gap-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
              : "flex items-start justify-between gap-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
          }
        >
          <div className="flex gap-2">
            {feedback.type
              === "success" ? (
                <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
              ) : (
                <XCircle className="mt-0.5 size-4 shrink-0" />
              )}

            <span>
              {feedback.message}
            </span>
          </div>

          <button
            type="button"
            aria-label="Close notification"
            onClick={() =>
              setFeedback(null)
            }
            className="opacity-70 transition hover:opacity-100"
          >
            <X className="size-4" />
          </button>
        </section>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-bold text-[#17343d]">
              Review Queue
            </h2>

            <p className="mt-1 text-xs text-slate-500">
              Only pending requests assigned to your authorized review step and Managed Major scope are shown.
            </p>
          </div>

          <div className="relative w-full sm:max-w-[340px]">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

            <Input
              value={searchTerm}
              onChange={(event) =>
                setSearchTerm(
                  event.target.value,
                )
              }
              placeholder="Search course, version, submitter..."
              className="h-9 pl-9"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <Table className="min-w-[1450px]">
            <TableHeader className="bg-slate-50/80">
              <TableRow>
                <TableHead className="min-w-[280px]">
                  Course
                </TableHead>

                <TableHead className="w-[120px]">
                  Version
                </TableHead>

                <TableHead className="min-w-[190px]">
                  Program / Cohort
                </TableHead>

                <TableHead className="min-w-[150px]">
                  Department
                </TableHead>

                <TableHead className="min-w-[170px]">
                  Instructor
                </TableHead>

                <TableHead className="min-w-[170px]">
                  Submitted At
                </TableHead>

                <TableHead className="w-[100px]">
                  Waiting
                </TableHead>

                <TableHead className="min-w-[190px]">
                  Workflow
                </TableHead>

                <TableHead className="min-w-[320px] text-right">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="h-36 text-center text-slate-500"
                  >
                    <div className="flex items-center justify-center gap-2">
                      <LoaderCircle className="size-4 animate-spin" />
                      Loading approval queue...
                    </div>
                  </TableCell>
                </TableRow>
              ) : isError ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="h-40 text-center"
                  >
                    <div className="mx-auto max-w-lg">
                      <p className="font-semibold text-rose-700">
                        Unable to load approval requests
                      </p>

                      <p className="mt-1 text-sm leading-6 text-slate-500">
                        {getErrorMessage(
                          error,
                        )}
                      </p>

                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="mt-3"
                        onClick={() =>
                          void refetch()
                        }
                      >
                        <RefreshCw className="size-3.5" />
                        Try Again
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ) : filteredRequests.length
                === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="h-48 text-center"
                  >
                    <div className="mx-auto flex max-w-md flex-col items-center">
                      <span className="flex size-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                        <Inbox className="size-5" />
                      </span>

                      <p className="mt-3 font-semibold text-slate-800">
                        {searchTerm
                          ? "No requests match your search."
                          : "No syllabus is waiting for your review."}
                      </p>

                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        {searchTerm
                          ? "Clear the search to view all pending requests."
                          : isDean
                            ? "New requests will appear after a Department Head approves a submitted syllabus."
                            : "New requests will appear after an instructor submits a syllabus in your department."}
                      </p>

                      {searchTerm && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="mt-3"
                          onClick={() =>
                            setSearchTerm("")
                          }
                        >
                          Clear Search
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                filteredRequests.map(
                  (item) => (
                    <TableRow
                      key={item.id}
                      className="align-top hover:bg-[#f8fbfb]"
                    >
                      <TableCell>
                        <div>
                          <p className="font-mono text-xs font-bold text-[#007d84]">
                            {item.courseCode}
                          </p>

                          <p className="mt-1 font-semibold text-slate-800">
                            {item.courseName}
                          </p>
                        </div>
                      </TableCell>

                      <TableCell>
                        <Badge
                          variant="outline"
                          className="border-slate-200 bg-white text-slate-700"
                        >
                          {item.versionLabel
                            || `v${item.versionNumber}`}
                        </Badge>
                      </TableCell>

                      <TableCell>
                        <p className="text-sm font-semibold text-slate-700">{item.programCode || "—"}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{item.cohortName || "—"} · {item.semester || "—"}</p>
                      </TableCell>

                      <TableCell>
                        <p className="text-sm font-semibold text-slate-700">{item.departmentCode || "—"}</p>
                        <p className="mt-0.5 text-xs text-slate-500">{item.departmentName || "—"}</p>
                      </TableCell>

                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="flex size-7 items-center justify-center rounded-full bg-slate-100 text-slate-600">
                            <UserRound className="size-3.5" />
                          </span>

                          <span className="text-sm font-medium text-slate-700">
                            {(item.instructorUsername || item.requestedByUsername)
                              || "—"}
                          </span>
                        </div>
                      </TableCell>

                      <TableCell className="text-sm text-slate-600">
                        {formatDateTime(
                          item.submittedAt
                          ?? item.createdAt,
                        )}
                      </TableCell>

                      <TableCell>
                        <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-amber-700">
                          <Clock3 className="size-3.5" />
                          {getWaitingTime(
                            item.createdAt
                            ?? item.submittedAt,
                          )}
                        </span>
                      </TableCell>

                      <TableCell>
                        <div>
                          <p className="text-sm font-semibold text-slate-700">
                            {isDean
                              ? "Dean Final Review"
                              : "Department Review"}
                          </p>

                          <p className="mt-0.5 text-[10px] text-slate-400">
                            {isDean
                              ? `Forwarded by ${item.requestedByUsername || "Department Head"}`
                              : "Approve to forward"}
                          </p>
                        </div>
                      </TableCell>

                      <TableCell>
                        <div className="flex flex-wrap justify-end gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              navigate(
                                `${detailBasePath}/${item.syllabusId}`,
                              )
                            }
                          >
                            <Eye className="size-3.5" />
                            View
                          </Button>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              navigate(
                                `${detailBasePath}/${item.syllabusId}#approval-history`,
                              )
                            }
                          >
                            <History className="size-3.5" />
                            History
                          </Button>

                          <Button
                            type="button"
                            size="sm"
                            className="bg-emerald-600 text-white hover:bg-emerald-700"
                            disabled={
                              reviewMutation.isPending
                            }
                            onClick={() =>
                              openReviewDialog(
                                item,
                                "APPROVED",
                              )
                            }
                          >
                            <CheckCircle2 className="size-3.5" />
                            {isDean
                              ? "Final Approve"
                              : "Approve & Forward"}
                          </Button>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            className="border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                            disabled={
                              reviewMutation.isPending
                            }
                            onClick={() =>
                              openReviewDialog(
                                item,
                                "REJECTED",
                              )
                            }
                          >
                            <XCircle className="size-3.5" />
                            Return for Revision
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ),
                )
              )}
            </TableBody>
          </Table>
        </div>
      </section>

      {reviewDialog && (
        <ApprovalReviewDialog
          key={
            `${reviewDialog.item.id}`
            + `-${reviewDialog.decision}`
          }
          open
          item={
            reviewDialog.item
          }
          decision={
            reviewDialog.decision
          }
          isDean={isDean}
          isPending={
            reviewMutation.isPending
          }
          errorMessage={
            reviewMutation.isError
              ? getErrorMessage(
                  reviewMutation.error,
                )
              : null
          }
          onOpenChange={(
            open,
          ) => {
            if (!open) {
              setReviewDialog(
                null,
              )

              reviewMutation.reset()
            }
          }}
          onConfirm={
            handleReviewConfirm
          }
        />
      )}
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
