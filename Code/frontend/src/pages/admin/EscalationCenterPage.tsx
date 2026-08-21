import { useMemo, useState } from "react"
import axios from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  AlertTriangle,
  BookOpenCheck,
  Building2,
  CheckCircle2,
  ChevronRight,
  CircleOff,
  Clock3,
  Gavel,
  History,
  MailCheck,
  RefreshCcw,
  Send,
  ShieldAlert,
  Users,
} from "lucide-react"

import {
  syllabusDeadlineApi,
  type DeadlineEscalationDispatchResult,
  type SyllabusDeadline,
} from "@/api/syllabusDeadlineApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
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

export default function EscalationCenterPage() {
  const queryClient = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [result, setResult] =
    useState<DeadlineEscalationDispatchResult | null>(null)
  const [feedback, setFeedback] = useState<{
    type: "success" | "warning" | "error"
    message: string
  } | null>(null)

  const deadlinesQuery = useQuery({
    queryKey: ["syllabus-deadlines"],
    queryFn: syllabusDeadlineApi.getAll,
  })

  const overdueDeadlines = useMemo(
    () =>
      (deadlinesQuery.data ?? [])
        .filter((deadline) => deadline.active && deadline.state === "OVERDUE")
        .sort((a, b) => a.deadlineAt.localeCompare(b.deadlineAt)),
    [deadlinesQuery.data],
  )

  const activeSelectedId =
    selectedId !== null && overdueDeadlines.some((item) => item.id === selectedId)
      ? selectedId
      : (overdueDeadlines[0]?.id ?? null)

  const previewQuery = useQuery({
    queryKey: ["deadline-escalation-preview", activeSelectedId],
    queryFn: () =>
      syllabusDeadlineApi.getEscalationPreview(activeSelectedId as number),
    enabled: activeSelectedId !== null,
  })

  const logsQuery = useQuery({
    queryKey: ["deadline-escalation-logs", activeSelectedId],
    queryFn: () =>
      syllabusDeadlineApi.getEscalationLogs(activeSelectedId as number),
    enabled: activeSelectedId !== null,
  })

  const dispatchMutation = useMutation({
    mutationFn: ({ id, force }: { id: number; force: boolean }) =>
      syllabusDeadlineApi.dispatchEscalation(id, force),
    onSuccess: (dispatchResult) => {
      setResult(dispatchResult)
      setFeedback({
        type:
          dispatchResult.failedDeliveries > 0
            ? "error"
            : dispatchResult.departmentsWithoutHead > 0 ||
                dispatchResult.missingDean
              ? "warning"
              : "success",
        message: dispatchResult.message,
      })
      void refresh(dispatchResult.deadlineId)
    },
    onError: (error) => {
      setFeedback({ type: "error", message: getErrorMessage(error) })
    },
  })

  const selectedDeadline = overdueDeadlines.find(
    (deadline) => deadline.id === activeSelectedId,
  )
  const preview = previewQuery.data
  const logs = logsQuery.data ?? []
  const deliveredCount = logs.filter((log) => log.notificationId != null).length

  async function refresh(id?: number) {
    await queryClient.invalidateQueries({ queryKey: ["syllabus-deadlines"] })
    if (id !== undefined) {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["deadline-escalation-preview", id],
        }),
        queryClient.invalidateQueries({
          queryKey: ["deadline-escalation-logs", id],
        }),
      ])
    }
  }

  function dispatch(force: boolean) {
    if (activeSelectedId === null) return
    const message = force
      ? "Thực hiện escalation thủ công ngay? Hệ thống chỉ gửi một lần cho mỗi người nhận ở revision hiện tại."
      : "Chạy kiểm tra mốc escalation hiện tại? Hệ thống chỉ gửi nếu đã đến mốc cấu hình."
    if (window.confirm(message)) {
      setFeedback(null)
      dispatchMutation.mutate({ id: activeSelectedId, force })
    }
  }

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-slate-950 text-white shadow-sm">
        <div className="relative px-6 py-7 md:px-8">
          <div className="absolute inset-y-0 right-0 w-2/5 bg-[radial-gradient(circle_at_center,rgba(190,24,93,0.28),transparent_65%)]" />
          <div className="relative flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-3xl">
              <div className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.22em] text-blue-200">
                <ShieldAlert className="size-4" /> FR-05.7 · Governance Control
              </div>
              <h1 className="text-3xl font-bold tracking-tight md:text-4xl">
                Trung tâm Escalation quá hạn
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-300">
                Giám sát đề cương chưa nộp sau deadline, xác định đúng phạm vi
                bộ môn và chuyển cảnh báo đến Trưởng bộ môn cùng Trưởng khoa.
              </p>
            </div>
            <Button
              variant="outline"
              className="border-white/20 bg-white/10 text-white hover:bg-white/20 hover:text-white"
              onClick={() => void refresh(activeSelectedId ?? undefined)}
              disabled={deadlinesQuery.isFetching || previewQuery.isFetching}
            >
              <RefreshCcw
                className={
                  deadlinesQuery.isFetching || previewQuery.isFetching
                    ? "animate-spin"
                    : ""
                }
              />
              Làm mới dữ liệu
            </Button>
          </div>
        </div>
      </section>

      {feedback && (
        <div
          className={`rounded-xl border px-4 py-3 text-sm font-medium ${
            feedback.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : feedback.type === "warning"
                ? "border-amber-200 bg-amber-50 text-amber-900"
                : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          {feedback.message}
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <ExecutiveMetric
          icon={Clock3}
          label="Kỳ đang quá hạn"
          value={overdueDeadlines.length}
          note="Deadline đang hoạt động"
        />
        <ExecutiveMetric
          icon={Users}
          label="Giảng viên quá hạn"
          value={preview?.overdueInstructorCount ?? 0}
          note={selectedDeadline ? deadlineLabel(selectedDeadline) : "Chưa chọn kỳ"}
        />
        <ExecutiveMetric
          icon={BookOpenCheck}
          label="Môn chưa nộp"
          value={preview?.missingCourseCount ?? 0}
          note={`${preview?.departmentCount ?? 0} bộ môn liên quan`}
        />
        <ExecutiveMetric
          icon={MailCheck}
          label="Lượt đã ghi nhận"
          value={deliveredCount}
          note="Notification và email outbox"
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[340px_minmax(0,1fr)]">
        <Card className="h-fit overflow-hidden">
          <CardHeader className="border-b bg-slate-50/80">
            <CardTitle className="text-lg">Deadline cần xử lý</CardTitle>
            <CardDescription>
              Chọn một học kỳ để xem phạm vi escalation.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-3">
            {deadlinesQuery.isLoading ? (
              <div className="p-8 text-center text-sm text-slate-500">
                Đang tải deadline...
              </div>
            ) : overdueDeadlines.length === 0 ? (
              <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-center">
                <CheckCircle2 className="mx-auto size-8 text-emerald-600" />
                <div className="mt-3 font-semibold text-emerald-900">
                  Không có deadline quá hạn
                </div>
                <p className="mt-1 text-sm text-emerald-700">
                  Hệ thống chưa phát hiện kỳ học nào cần escalation.
                </p>
              </div>
            ) : (
              <div className="space-y-2">
                {overdueDeadlines.map((deadline) => {
                  const selected = deadline.id === activeSelectedId
                  return (
                    <button
                      key={deadline.id}
                      type="button"
                      onClick={() => {
                        setSelectedId(deadline.id)
                        setResult(null)
                        setFeedback(null)
                      }}
                      className={`w-full rounded-xl border p-4 text-left transition ${
                        selected
                          ? "border-slate-900 bg-slate-900 text-white shadow-sm"
                          : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="font-bold">{deadlineLabel(deadline)}</div>
                          <div
                            className={`mt-1 text-xs ${
                              selected ? "text-slate-300" : "text-slate-500"
                            }`}
                          >
                            {formatDateTime(deadline.deadlineAt)}
                          </div>
                        </div>
                        <ChevronRight className="mt-0.5 size-4 shrink-0" />
                      </div>
                      <div className="mt-3 flex flex-wrap items-center gap-2">
                        <Badge
                          variant="destructive"
                          className={
                            selected ? "border-rose-400/30 bg-rose-500/20 text-rose-100" : ""
                          }
                        >
                          Quá hạn {Math.abs(deadline.daysRemaining)} ngày
                        </Badge>
                        <span
                          className={`text-xs ${
                            selected ? "text-slate-300" : "text-slate-500"
                          }`}
                        >
                          v{deadline.revision}
                        </span>
                      </div>
                    </button>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>

        <div className="space-y-6">
          {selectedId === null ? (
            <Card>
              <CardContent className="flex min-h-72 items-center justify-center p-8 text-center">
                <div>
                  <CircleOff className="mx-auto size-9 text-slate-400" />
                  <div className="mt-3 font-semibold text-slate-800">
                    Chưa có deadline được chọn
                  </div>
                  <p className="mt-1 text-sm text-slate-500">
                    Chọn một deadline quá hạn để xem chi tiết.
                  </p>
                </div>
              </CardContent>
            </Card>
          ) : previewQuery.isLoading ? (
            <Card>
              <CardContent className="p-10 text-center text-sm text-slate-500">
                Đang phân tích phân công, trạng thái đề cương và người nhận...
              </CardContent>
            </Card>
          ) : previewQuery.isError ? (
            <Card className="border-rose-200">
              <CardContent className="p-6 text-sm text-rose-700">
                {getErrorMessage(previewQuery.error)}
              </CardContent>
            </Card>
          ) : preview ? (
            <>
              <Card className="overflow-hidden border-slate-200">
                <div className="border-b bg-gradient-to-r from-slate-950 via-slate-900 to-rose-950 px-6 py-5 text-white">
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div>
                      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-slate-300">
                        <Gavel className="size-4" /> Hồ sơ escalation
                      </div>
                      <h2 className="mt-2 text-2xl font-bold">
                        {deadlineLabel(preview.deadline)}
                      </h2>
                      <p className="mt-1 text-sm text-slate-300">
                        Quá hạn {preview.daysOverdue} ngày · Mốc tự động:{" "}
                        {preview.deadline.escalationDays.join(", ")} ngày
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button
                        variant="outline"
                        className="border-white/20 bg-white/10 text-white hover:bg-white/20 hover:text-white"
                        onClick={() => dispatch(false)}
                        disabled={dispatchMutation.isPending}
                      >
                        <Clock3 /> Chạy đúng mốc
                      </Button>
                      <Button
                        className="bg-rose-600 text-white hover:bg-rose-700"
                        onClick={() => dispatch(true)}
                        disabled={dispatchMutation.isPending}
                      >
                        <Send />
                        {dispatchMutation.isPending
                          ? "Đang escalation..."
                          : "Escalate ngay"}
                      </Button>
                    </div>
                  </div>
                </div>
                <CardContent className="space-y-5 p-6">
                  {(preview.departmentsWithoutHead > 0 || preview.missingDean) && (
                    <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                      <AlertTriangle className="mt-0.5 size-5 shrink-0" />
                      <div>
                        <div className="font-semibold">Cấu hình lãnh đạo chưa đầy đủ</div>
                        <div className="mt-1 leading-6">
                          {preview.departmentsWithoutHead > 0 &&
                            `${preview.departmentsWithoutHead} bộ môn chưa có tài khoản Trưởng bộ môn hoạt động. `}
                          {preview.missingDean &&
                            "Chưa có tài khoản Trưởng khoa hoạt động."}
                        </div>
                      </div>
                    </div>
                  )}

                  {result?.deadlineId === selectedId && (
                    <div className="grid gap-3 rounded-xl border bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-4">
                      <CompactMetric label="Người nhận" value={result.recipientCount} />
                      <CompactMetric label="Đã tạo" value={result.notificationsQueued} />
                      <CompactMetric label="Đã gửi trước" value={result.skippedAlreadySent} />
                      <CompactMetric label="Lỗi" value={result.failedDeliveries} />
                    </div>
                  )}

                  <div>
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <div>
                        <h3 className="flex items-center gap-2 font-bold text-slate-900">
                          <Building2 className="size-4" /> Phạm vi theo bộ môn
                        </h3>
                        <p className="mt-1 text-sm text-slate-500">
                          Mỗi Trưởng bộ môn chỉ nhận dữ liệu thuộc phạm vi phụ trách.
                        </p>
                      </div>
                      <Badge variant="secondary">
                        {preview.departments.length} bộ môn
                      </Badge>
                    </div>

                    {preview.departments.length === 0 ? (
                      <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-sm text-emerald-800">
                        Không còn môn học nào thiếu đề cương trong kỳ này.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {preview.departments.map((department) => (
                          <div
                            key={department.departmentId}
                            className="overflow-hidden rounded-xl border border-slate-200"
                          >
                            <div className="flex flex-col gap-3 border-b bg-slate-50 px-5 py-4 md:flex-row md:items-center md:justify-between">
                              <div>
                                <div className="flex items-center gap-2">
                                  <span className="rounded-md bg-slate-900 px-2 py-1 text-xs font-bold text-white">
                                    {department.departmentCode}
                                  </span>
                                  <span className="font-bold text-slate-900">
                                    {department.departmentName}
                                  </span>
                                </div>
                                <div className="mt-2 text-xs text-slate-500">
                                  Người nhận:{" "}
                                  {department.departmentHeads.length > 0
                                    ? department.departmentHeads
                                        .map((head) => head.username)
                                        .join(", ")
                                    : "Chưa có Trưởng bộ môn"}
                                </div>
                              </div>
                              <div className="flex gap-2">
                                <Badge variant="outline">
                                  {department.overdueInstructorCount} giảng viên
                                </Badge>
                                <Badge variant="destructive">
                                  {department.missingCourseCount} môn
                                </Badge>
                              </div>
                            </div>
                            <div className="overflow-x-auto">
                              <Table>
                                <TableHeader>
                                  <TableRow>
                                    <TableHead>Giảng viên</TableHead>
                                    <TableHead>Email</TableHead>
                                    <TableHead>Môn chưa nộp</TableHead>
                                  </TableRow>
                                </TableHeader>
                                <TableBody>
                                  {department.overdueInstructors.map((instructor) => (
                                    <TableRow key={instructor.instructorId}>
                                      <TableCell className="font-semibold">
                                        {instructor.instructorName}
                                      </TableCell>
                                      <TableCell className="text-slate-600">
                                        {instructor.instructorEmail || "—"}
                                      </TableCell>
                                      <TableCell>
                                        <div className="flex max-w-2xl flex-wrap gap-1.5">
                                          {instructor.missingCourses.map((course) => (
                                            <Badge key={course.courseId} variant="outline">
                                              {course.courseCode} · {course.courseName}
                                            </Badge>
                                          ))}
                                        </div>
                                      </TableCell>
                                    </TableRow>
                                  ))}
                                </TableBody>
                              </Table>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div>
                    <h3 className="mb-3 flex items-center gap-2 font-bold text-slate-900">
                      <Users className="size-4" /> Danh sách lãnh đạo nhận cảnh báo
                    </h3>
                    <div className="grid gap-3 md:grid-cols-2">
                      {preview.recipients.map((recipient) => (
                        <div
                          key={`${recipient.scopeKey}-${recipient.userId}`}
                          className="rounded-xl border border-slate-200 bg-white p-4"
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div>
                              <div className="font-bold text-slate-900">
                                {recipient.username}
                              </div>
                              <div className="mt-1 text-sm text-slate-500">
                                {recipient.email}
                              </div>
                            </div>
                            <Badge
                              variant={
                                recipient.role === "DEAN" ? "default" : "secondary"
                              }
                            >
                              {recipient.role === "DEAN"
                                ? "Trưởng khoa"
                                : "Trưởng bộ môn"}
                            </Badge>
                          </div>
                          <div className="mt-3 border-t pt-3 text-xs text-slate-600">
                            Phạm vi <strong>{recipient.scopeLabel}</strong> ·{" "}
                            {recipient.overdueInstructorCount} giảng viên ·{" "}
                            {recipient.missingCourseCount} môn
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <History className="size-5" /> Nhật ký escalation
                  </CardTitle>
                  <CardDescription>
                    Snapshot phục vụ truy vết, chống gửi trùng và kiểm toán sau này.
                  </CardDescription>
                </CardHeader>
                <CardContent className="p-0">
                  {logsQuery.isLoading ? (
                    <div className="p-8 text-center text-sm text-slate-500">
                      Đang tải nhật ký...
                    </div>
                  ) : logs.length === 0 ? (
                    <div className="p-8 text-center text-sm text-slate-500">
                      Chưa có escalation nào được ghi nhận cho deadline này.
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Thời gian</TableHead>
                            <TableHead>Người nhận</TableHead>
                            <TableHead>Phạm vi</TableHead>
                            <TableHead>Mốc</TableHead>
                            <TableHead>Snapshot</TableHead>
                            <TableHead>Email</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {logs.map((log) => (
                            <TableRow key={log.id}>
                              <TableCell className="whitespace-nowrap">
                                {formatDateTime(log.createdAt)}
                              </TableCell>
                              <TableCell>
                                <div className="font-semibold">
                                  {log.recipientUsername}
                                </div>
                                <div className="text-xs text-slate-500">
                                  {log.recipientRole === "DEAN"
                                    ? "Trưởng khoa"
                                    : "Trưởng bộ môn"}
                                </div>
                              </TableCell>
                              <TableCell>
                                {log.departmentCode || "Toàn khoa"}
                              </TableCell>
                              <TableCell>
                                {log.escalationDay === -1
                                  ? "Thủ công"
                                  : `Sau ${log.escalationDay} ngày`}
                                <div className="text-xs text-slate-500">
                                  thực tế {log.actualDaysOverdue} ngày · v
                                  {log.deadlineRevision}
                                </div>
                              </TableCell>
                              <TableCell className="max-w-md">
                                <div className="text-sm">
                                  {log.overdueInstructorCount} GV ·{" "}
                                  {log.missingCourseCount} môn
                                </div>
                                <div className="mt-1 truncate text-xs text-slate-500">
                                  {log.missingCourseCodes}
                                </div>
                              </TableCell>
                              <TableCell>
                                <Badge
                                  variant={log.emailQueued ? "secondary" : "outline"}
                                >
                                  {log.emailQueued ? "Đã vào outbox" : "Không có email"}
                                </Badge>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </div>
                  )}
                </CardContent>
              </Card>
            </>
          ) : null}
        </div>
      </div>
    </div>
  )
}

function ExecutiveMetric({
  icon: Icon,
  label,
  value,
  note,
}: {
  icon: React.ComponentType<{ className?: string }>
  label: string
  value: number
  note: string
}) {
  return (
    <Card>
      <CardContent className="flex items-start gap-4 p-5">
        <div className="rounded-xl bg-slate-900 p-3 text-white">
          <Icon className="size-5" />
        </div>
        <div>
          <div className="text-xs font-bold uppercase tracking-[0.12em] text-slate-500">
            {label}
          </div>
          <div className="mt-1 text-3xl font-bold text-slate-950">{value}</div>
          <div className="mt-1 text-xs text-slate-500">{note}</div>
        </div>
      </CardContent>
    </Card>
  )
}

function CompactMetric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </div>
      <div className="mt-1 text-2xl font-bold text-slate-950">{value}</div>
    </div>
  )
}

function deadlineLabel(deadline: SyllabusDeadline): string {
  return `HK${deadline.semester} · ${deadline.academicYear}`
}

function formatDateTime(value: string): string {
  if (!value) return "—"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace("T", " ")
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date)
}

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message || error.message || "Không thể xử lý yêu cầu."
  }
  if (error instanceof Error) return error.message
  return "Không thể xử lý yêu cầu."
}
