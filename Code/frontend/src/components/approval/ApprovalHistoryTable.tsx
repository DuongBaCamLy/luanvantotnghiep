import { useQuery } from "@tanstack/react-query"
import {
  Clock3,
  History,
  LoaderCircle,
  MessageSquareText,
} from "lucide-react"

import { approvalRequestApi } from "@/api/approvalRequestApi"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { formatDateTime, t } from "@/i18n"
import {
  APPROVAL_STATUS_LABELS,
  APPROVAL_STEP_LABELS,
} from "@/i18n/labels"
import type { ApprovalStatus } from "@/types/approval"

interface ApprovalHistoryTableProps {
  syllabusId: number
}

const STATUS_CLASS_NAMES: Record<ApprovalStatus, string> = {
  PENDING:
    "border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-50",
  APPROVED:
    "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-50",
  REJECTED:
    "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-50",
  REVISION_REQUESTED:
    "border-orange-200 bg-orange-50 text-orange-700 hover:bg-orange-50",
}

function getErrorMessage(error: unknown): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: string
          }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }
  }

  if (error instanceof Error) {
    return error.message
  }

  return t("approval.history.loadError")
}

export default function ApprovalHistoryTable({
  syllabusId,
}: ApprovalHistoryTableProps) {
  const {
    data: historyItems = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["approval-history", syllabusId],
    queryFn: () => approvalRequestApi.getApprovalHistory(syllabusId),
    enabled: Number.isInteger(syllabusId) && syllabusId > 0,
  })

  return (
    <Card className="overflow-hidden border-slate-200 shadow-sm">
      <CardHeader className="border-b border-slate-100 bg-white">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2 text-lg text-[#17343d]">
              <History className="size-5 text-[#007d84]" />
              {t("approval.history.title")}
            </CardTitle>

            <CardDescription className="mt-1 max-w-3xl">
              {t("approval.history.description")}
            </CardDescription>
          </div>

          <Badge
            variant="outline"
            className="w-fit border-slate-200 bg-slate-50 text-slate-600"
          >
            {t("approval.history.records", {
              count: historyItems.length,
            })}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="p-0">
        {isError && (
          <div className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
            {getErrorMessage(error)}
          </div>
        )}

        <div className="overflow-x-auto">
          <Table className="min-w-[1100px]">
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead className="w-16 text-center">
                  {t("approval.history.sequence")}
                </TableHead>
                <TableHead>
                  {t("approval.history.reviewLevel")}
                </TableHead>
                <TableHead>
                  {t("approval.history.status")}
                </TableHead>
                <TableHead>
                  Initiated By
                </TableHead>
                <TableHead>
                  {t("approval.history.reviewedBy")}
                </TableHead>
                <TableHead className="min-w-72">
                  {t("approval.history.comment")}
                </TableHead>
                <TableHead>
                  Initiated At
                </TableHead>
                <TableHead>
                  {t("approval.history.resolvedAt")}
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-32 text-center text-slate-500"
                  >
                    <span className="inline-flex items-center gap-2">
                      <LoaderCircle className="size-5 animate-spin" />
                      {t("approval.history.loading")}
                    </span>
                  </TableCell>
                </TableRow>
              ) : historyItems.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={8}
                    className="h-44 text-center"
                  >
                    <div className="mx-auto flex max-w-md flex-col items-center text-slate-500">
                      <span className="flex size-12 items-center justify-center rounded-2xl bg-slate-50 text-slate-300">
                        <MessageSquareText className="size-7" />
                      </span>

                      <p className="mt-3 font-semibold text-slate-700">
                        {t("approval.history.emptyTitle")}
                      </p>

                      <p className="mt-1 text-sm leading-6">
                        {t("approval.history.emptyDescription")}
                      </p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                historyItems.map((item, index) => {
                  const stepLabel = item.step
                    ? APPROVAL_STEP_LABELS[item.step]
                    : t("common.notDetermined")

                  const statusLabel = item.status
                    ? APPROVAL_STATUS_LABELS[item.status]
                    : t("common.notDetermined")

                  return (
                    <TableRow
                      key={item.id}
                      className="align-top hover:bg-slate-50/70"
                    >
                      <TableCell className="text-center font-semibold text-slate-500">
                        {historyItems.length - index}
                      </TableCell>

                      <TableCell>
                        <div className="font-medium text-slate-900">
                          {stepLabel}
                        </div>
                      </TableCell>

                      <TableCell>
                        {item.status ? (
                          <Badge
                            variant="outline"
                            className={STATUS_CLASS_NAMES[item.status]}
                          >
                            {statusLabel}
                          </Badge>
                        ) : (
                          <Badge variant="outline">
                            {statusLabel}
                          </Badge>
                        )}
                      </TableCell>

                      <TableCell className="font-medium text-slate-700">
                        {item.requestedByUsername || "—"}
                      </TableCell>

                      <TableCell className="font-medium text-slate-700">
                        {item.reviewedByUsername
                          || t("approval.history.unreviewed")}
                      </TableCell>

                      <TableCell>
                        {item.comment?.trim() ? (
                          <div className="whitespace-pre-wrap rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm leading-6 text-slate-700">
                            {item.comment}
                          </div>
                        ) : (
                          <span className="text-sm italic text-slate-400">
                            {t("approval.history.noComment")}
                          </span>
                        )}
                      </TableCell>

                      <TableCell className="whitespace-nowrap text-slate-600">
                        <span className="inline-flex items-center gap-1.5">
                          <Clock3 className="size-3.5 text-slate-400" />
                          {formatDateTime(item.createdAt)}
                        </span>
                      </TableCell>

                      <TableCell className="whitespace-nowrap text-slate-600">
                        {formatDateTime(item.resolvedAt)}
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  )
}