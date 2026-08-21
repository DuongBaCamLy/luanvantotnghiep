import { useMemo, useState } from "react"
import type { ReactNode } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  MailCheck,
  RefreshCw,
  RotateCcw,
  Search,
  Send,
} from "lucide-react"
import { emailOutboxApi } from "@/api/emailOutboxApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import type {
  EmailDeliveryStatus,
  EmailOutboxItem,
} from "@/types/emailOutbox"

const STATUS_OPTIONS: Array<{
  value: "ALL" | EmailDeliveryStatus
  label: string
}> = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "PENDING", label: "Đang chờ" },
  { value: "RETRY", label: "Chờ gửi lại" },
  { value: "SENT", label: "Đã gửi" },
  { value: "FAILED", label: "Gửi thất bại" },
]

function formatDate(value: string | null) {
  if (!value) return "—"
  return new Date(value).toLocaleString("vi-VN")
}

function getErrorMessage(error: unknown) {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: { data?: { message?: string } }
      }
    ).response
    if (response?.data?.message) {
      return response.data.message
    }
  }
  return "Không thể xử lý yêu cầu. Vui lòng thử lại."
}

function statusBadge(status: EmailDeliveryStatus) {
  const classes: Record<EmailDeliveryStatus, string> = {
    PENDING: "border-blue-200 bg-blue-50 text-blue-700",
    RETRY: "border-amber-200 bg-amber-50 text-amber-700",
    SENT: "border-emerald-200 bg-emerald-50 text-emerald-700",
    FAILED: "border-rose-200 bg-rose-50 text-rose-700",
  }
  const labels: Record<EmailDeliveryStatus, string> = {
    PENDING: "Đang chờ",
    RETRY: "Chờ gửi lại",
    SENT: "Đã gửi",
    FAILED: "Thất bại",
  }

  return (
    <Badge variant="outline" className={classes[status]}>
      {labels[status]}
    </Badge>
  )
}

export default function EmailOutboxPage() {
  const queryClient = useQueryClient()
  const [status, setStatus] =
    useState<"ALL" | EmailDeliveryStatus>("ALL")
  const [searchTerm, setSearchTerm] = useState("")
  const [feedback, setFeedback] = useState<string | null>(null)

  const selectedStatus = status === "ALL" ? undefined : status

  const summaryQuery = useQuery({
    queryKey: ["email-outbox", "summary"],
    queryFn: emailOutboxApi.getSummary,
    refetchInterval: 15000,
  })

  const emailsQuery = useQuery({
    queryKey: ["email-outbox", "list", selectedStatus],
    queryFn: () => emailOutboxApi.getRecent(selectedStatus),
    refetchInterval: 15000,
  })

  const retryMutation = useMutation({
    mutationFn: (id: number) => emailOutboxApi.retry(id),
    onSuccess: () => {
      setFeedback("Email đã được đưa lại vào hàng đợi gửi.")
      queryClient.invalidateQueries({ queryKey: ["email-outbox"] })
    },
  })

  const filteredEmails = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()
    if (!keyword) return emailsQuery.data ?? []

    return (emailsQuery.data ?? []).filter((email) =>
      [
        email.recipientEmail,
        email.recipientName,
        email.subject,
        email.eventType,
      ].some((value) => value?.toLowerCase().includes(keyword))
    )
  }, [emailsQuery.data, searchTerm])

  const summary = summaryQuery.data
  const isRefreshing = emailsQuery.isFetching || summaryQuery.isFetching

  const refresh = () => {
    setFeedback(null)
    queryClient.invalidateQueries({ queryKey: ["email-outbox"] })
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-blue-700">
            <MailCheck className="h-4 w-4" />
            FR-05.4 · Email Notification
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Trung tâm gửi email
          </h1>
          <p className="mt-1 max-w-2xl text-muted-foreground">
            Theo dõi toàn bộ email workflow, kiểm tra lỗi SMTP và chủ động
            gửi lại mà không ảnh hưởng trạng thái phê duyệt đề cương.
          </p>
        </div>

        <Button
          variant="outline"
          onClick={refresh}
          disabled={isRefreshing}
          className="gap-2"
        >
          <RefreshCw
            className={`h-4 w-4 ${isRefreshing ? "animate-spin" : ""}`}
          />
          Làm mới
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Đang chờ"
          value={summary?.pending ?? 0}
          description="Sẵn sàng gửi"
          icon={<Clock3 className="h-6 w-6" />}
          className="border-blue-200 bg-gradient-to-br from-white to-blue-50 text-blue-700"
        />
        <SummaryCard
          title="Chờ gửi lại"
          value={summary?.retry ?? 0}
          description="Tự động retry"
          icon={<RotateCcw className="h-6 w-6" />}
          className="border-amber-200 bg-gradient-to-br from-white to-amber-50 text-amber-700"
        />
        <SummaryCard
          title="Đã gửi"
          value={summary?.sent ?? 0}
          description="Giao thành công"
          icon={<CheckCircle2 className="h-6 w-6" />}
          className="border-emerald-200 bg-gradient-to-br from-white to-emerald-50 text-emerald-700"
        />
        <SummaryCard
          title="Thất bại"
          value={summary?.failed ?? 0}
          description="Cần kiểm tra"
          icon={<AlertTriangle className="h-6 w-6" />}
          className="border-rose-200 bg-gradient-to-br from-white to-rose-50 text-rose-700"
        />
      </div>

      {feedback && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
          {feedback}
        </div>
      )}

      {(emailsQuery.isError || summaryQuery.isError || retryMutation.isError) && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
          {getErrorMessage(
            retryMutation.error ?? emailsQuery.error ?? summaryQuery.error
          )}
        </div>
      )}

      <Card className="border-slate-200 shadow-sm">
        <CardHeader className="pb-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <CardTitle>Nhật ký gửi email</CardTitle>
              <CardDescription>
                Hiển thị {filteredEmails.length} email gần nhất
              </CardDescription>
            </div>

            <div className="flex w-full flex-col gap-3 sm:flex-row lg:w-auto">
              <div className="relative sm:w-80">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-500" />
                <Input
                  value={searchTerm}
                  onChange={(event) => setSearchTerm(event.target.value)}
                  placeholder="Tìm người nhận, tiêu đề, sự kiện..."
                  className="pl-9"
                />
              </div>

              <Select
                value={status}
                onValueChange={(value) =>
                  setStatus(value as "ALL" | EmailDeliveryStatus)
                }
              >
                <SelectTrigger className="h-9 w-full sm:w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {STATUS_OPTIONS.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>

        <CardContent>
          <div className="overflow-hidden rounded-xl border">
            <Table>
              <TableHeader className="bg-slate-50">
                <TableRow>
                  <TableHead>Người nhận</TableHead>
                  <TableHead className="min-w-72">Nội dung</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Số lần thử</TableHead>
                  <TableHead>Thời gian</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {emailsQuery.isLoading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="h-28 text-center text-slate-500">
                      Đang tải nhật ký email...
                    </TableCell>
                  </TableRow>
                ) : filteredEmails.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="h-32 text-center">
                      <div className="mx-auto flex max-w-sm flex-col items-center text-slate-500">
                        <Send className="mb-2 h-9 w-9 text-blue-500" />
                        <p className="font-medium text-slate-700">
                          Chưa có email phù hợp
                        </p>
                        <p className="mt-1 text-sm">
                          Email workflow mới sẽ xuất hiện tại đây.
                        </p>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredEmails.map((email) => (
                    <EmailRow
                      key={email.id}
                      email={email}
                      retrying={
                        retryMutation.isPending
                        && retryMutation.variables === email.id
                      }
                      onRetry={() => retryMutation.mutate(email.id)}
                    />
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function SummaryCard({
  title,
  value,
  description,
  icon,
  className,
}: {
  title: string
  value: number
  description: string
  icon: ReactNode
  className: string
}) {
  return (
    <Card className={`${className} shadow-sm`}>
      <CardContent className="flex items-center justify-between p-5">
        <div>
          <p className="text-sm font-medium text-slate-500">{title}</p>
          <p className="mt-1 text-3xl font-bold text-slate-900">{value}</p>
          <p className="mt-1 text-xs text-slate-500">{description}</p>
        </div>
        <div className="rounded-2xl bg-white/80 p-3 shadow-sm">{icon}</div>
      </CardContent>
    </Card>
  )
}

function EmailRow({
  email,
  retrying,
  onRetry,
}: {
  email: EmailOutboxItem
  retrying: boolean
  onRetry: () => void
}) {
  const canRetry = email.status === "FAILED" || email.status === "RETRY"

  return (
    <TableRow className="align-top">
      <TableCell>
        <p className="font-medium text-slate-900">
          {email.recipientName || "Không có tên"}
        </p>
        <p className="mt-1 max-w-52 truncate text-xs text-slate-500">
          {email.recipientEmail}
        </p>
      </TableCell>
      <TableCell>
        <p className="font-medium text-slate-800">{email.subject}</p>
        <div className="mt-2 flex flex-wrap gap-2">
          <Badge variant="outline" className="font-mono text-[10px]">
            {email.eventType}
          </Badge>
          {email.syllabusId && (
            <Badge variant="outline" className="text-[10px]">
              Syllabus #{email.syllabusId}
            </Badge>
          )}
        </div>
        {email.lastError && (
          <p className="mt-2 line-clamp-2 max-w-xl text-xs leading-relaxed text-rose-600">
            {email.lastError}
          </p>
        )}
      </TableCell>
      <TableCell>{statusBadge(email.status)}</TableCell>
      <TableCell>
        <span className="font-semibold text-slate-700">
          {email.attemptCount}
        </span>
      </TableCell>
      <TableCell className="text-xs text-slate-500">
        <p>Tạo: {formatDate(email.createdAt)}</p>
        <p className="mt-1">
          {email.sentAt
            ? `Gửi: ${formatDate(email.sentAt)}`
            : `Lần tới: ${formatDate(email.nextAttemptAt)}`}
        </p>
      </TableCell>
      <TableCell className="text-right">
        <Button
          size="sm"
          variant="outline"
          disabled={!canRetry || retrying}
          onClick={onRetry}
          className="gap-2"
        >
          <RotateCcw className={`h-3.5 w-3.5 ${retrying ? "animate-spin" : ""}`} />
          Gửi lại
        </Button>
      </TableCell>
    </TableRow>
  )
}
