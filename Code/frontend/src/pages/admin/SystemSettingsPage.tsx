import { useMemo, useState } from "react"
import axios from "axios"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  BellRing,
  CalendarClock,
  CheckCircle2,
  Eye,
  History,
  Pencil,
  Play,
  Power,
  RefreshCcw,
  Save,
  X,
} from "lucide-react"

import {
  syllabusDeadlineApi,
  type DeadlineDispatchResult,
  type DeadlineState,
  type SyllabusDeadline,
  type UpsertSyllabusDeadlineRequest,
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
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

interface DeadlineFormState {
  academicYear: string
  semester: string
  deadlineAt: string
  reminderDaysText: string
  escalationDaysText: string
  active: boolean
}

const DEFAULT_FORM: DeadlineFormState = {
  academicYear: "",
  semester: "1",
  deadlineAt: "",
  reminderDaysText: "14, 7, 3, 1, 0",
  escalationDaysText: "0, 1, 3, 7, 14",
  active: true,
}

const SEMESTERS = Array.from({ length: 8 }, (_, index) => index + 1)

export default function SystemSettingsPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<DeadlineFormState>(DEFAULT_FORM)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [dispatchResult, setDispatchResult] =
    useState<DeadlineDispatchResult | null>(null)
  const [feedback, setFeedback] = useState<{
    type: "success" | "error"
    message: string
  } | null>(null)

  const deadlinesQuery = useQuery({
    queryKey: ["syllabus-deadlines"],
    queryFn: syllabusDeadlineApi.getAll,
  })

  const previewQuery = useQuery({
    queryKey: ["syllabus-deadline-preview", selectedId],
    queryFn: () => syllabusDeadlineApi.preview(selectedId as number),
    enabled: selectedId !== null,
  })

  const logsQuery = useQuery({
    queryKey: ["syllabus-deadline-logs", selectedId],
    queryFn: () => syllabusDeadlineApi.getLogs(selectedId as number),
    enabled: selectedId !== null,
  })

  const saveMutation = useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: number | null
      request: UpsertSyllabusDeadlineRequest
    }) =>
      id === null
        ? syllabusDeadlineApi.create(request)
        : syllabusDeadlineApi.update(id, request),
    onSuccess: (saved) => {
      setFeedback({
        type: "success",
        message:
          editingId === null
            ? "Đã tạo deadline theo học kỳ."
            : "Đã cập nhật deadline và tăng revision khi lịch thay đổi.",
      })
      setEditingId(null)
      setForm(DEFAULT_FORM)
      setSelectedId(saved.id)
      void refreshAll(saved.id)
    },
    onError: (error) => {
      setFeedback({ type: "error", message: getErrorMessage(error) })
    },
  })

  const activeMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      syllabusDeadlineApi.setActive(id, active),
    onSuccess: (saved) => {
      setFeedback({
        type: "success",
        message: saved.active
          ? "Đã kích hoạt lịch nhắc."
          : "Đã tạm dừng lịch nhắc.",
      })
      void refreshAll(saved.id)
    },
    onError: (error) => {
      setFeedback({ type: "error", message: getErrorMessage(error) })
    },
  })

  const dispatchMutation = useMutation({
    mutationFn: ({ id, force }: { id: number; force: boolean }) =>
      syllabusDeadlineApi.dispatchNow(id, force),
    onSuccess: (result) => {
      setDispatchResult(result)
      setFeedback({
        type: result.failedDeliveries > 0 ? "error" : "success",
        message: result.message,
      })
      void refreshAll(result.deadlineId)
    },
    onError: (error) => {
      setFeedback({ type: "error", message: getErrorMessage(error) })
    },
  })

  const selectedDeadline = useMemo(
    () =>
      deadlinesQuery.data?.find((deadline) => deadline.id === selectedId) ??
      null,
    [deadlinesQuery.data, selectedId],
  )

  async function refreshAll(id?: number) {
    await queryClient.invalidateQueries({ queryKey: ["syllabus-deadlines"] })
    if (id !== undefined) {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["syllabus-deadline-preview", id],
        }),
        queryClient.invalidateQueries({
          queryKey: ["syllabus-deadline-logs", id],
        }),
      ])
    }
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFeedback(null)

    try {
      const request: UpsertSyllabusDeadlineRequest = {
        academicYear: form.academicYear.trim(),
        semester: Number(form.semester),
        deadlineAt: form.deadlineAt,
        reminderDays: parseReminderDays(form.reminderDaysText),
        escalationDays: parseEscalationDays(form.escalationDaysText),
        active: form.active,
      }

      if (!request.academicYear) {
        throw new Error("Vui lòng nhập năm học.")
      }
      if (!request.deadlineAt) {
        throw new Error("Vui lòng chọn ngày và giờ deadline.")
      }

      saveMutation.mutate({ id: editingId, request })
    } catch (error) {
      setFeedback({ type: "error", message: getErrorMessage(error) })
    }
  }

  function beginEdit(deadline: SyllabusDeadline) {
    setEditingId(deadline.id)
    setForm({
      academicYear: deadline.academicYear,
      semester: String(deadline.semester),
      deadlineAt: toDateTimeLocal(deadline.deadlineAt),
      reminderDaysText: deadline.reminderDays.join(", "),
      escalationDaysText: deadline.escalationDays.join(", "),
      active: deadline.active,
    })
    setFeedback(null)
    window.scrollTo({ top: 0, behavior: "smooth" })
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(DEFAULT_FORM)
    setFeedback(null)
  }

  function openDetails(id: number) {
    setSelectedId(id)
    setDispatchResult(null)
  }

  function dispatch(id: number, force: boolean) {
    const confirmation = force
      ? "Gửi thử ngay cho tất cả giảng viên đang thiếu đề cương? Lượt gửi thử có khóa riêng, không làm mất lượt nhắc tự động của scheduler."
      : "Chạy kiểm tra mốc nhắc ngay bây giờ? Chỉ gửi khi lịch đang đến hạn."
    if (window.confirm(confirmation)) {
      dispatchMutation.mutate({ id, force })
    }
  }

  const deadlines = deadlinesQuery.data ?? []

  return (
    <div data-admin-page="SystemSettingsPage" className="space-y-6">
      <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
        <div data-admin-page-header="SystemSettingsPage">
          <h1 className="text-3xl font-bold tracking-tight">
            Cấu hình hệ thống
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            FR-05.6–05.7 · Nhắc trước hạn và escalation tự động sau hạn.
          </p>
        </div>
        <Button
          variant="outline"
          onClick={() => void refreshAll(selectedId ?? undefined)}
          disabled={deadlinesQuery.isFetching}
        >
          <RefreshCcw
            className={deadlinesQuery.isFetching ? "animate-spin" : ""}
          />
          Làm mới
        </Button>
      </div>

      {feedback && (
        <div
          className={`rounded-lg border px-4 py-3 text-sm ${
            feedback.type === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          {feedback.message}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <CalendarClock className="size-5" />
            {editingId === null ? "Tạo deadline học kỳ" : "Cập nhật deadline"}
          </CardTitle>
          <CardDescription>
            Mỗi cặp năm học + học kỳ chỉ có một cấu hình. Revision bảo vệ
            cả reminder và escalation khỏi gửi trùng khi lịch được thay đổi.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
              <div className="space-y-2">
                <Label htmlFor="academicYear">Năm học *</Label>
                <Input
                  id="academicYear"
                  value={form.academicYear}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      academicYear: event.target.value,
                    }))
                  }
                  placeholder="Ví dụ: 2026-2027"
                  maxLength={50}
                />
              </div>

              <div className="space-y-2">
                <Label>Học kỳ *</Label>
                <Select
                  value={form.semester}
                  onValueChange={(value) =>
                    setForm((current) => ({ ...current, semester: value }))
                  }
                >
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SEMESTERS.map((semester) => (
                      <SelectItem key={semester} value={String(semester)}>
                        Học kỳ {semester}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="deadlineAt">Ngày và giờ deadline *</Label>
                <Input
                  id="deadlineAt"
                  type="datetime-local"
                  value={form.deadlineAt}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      deadlineAt: event.target.value,
                    }))
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="reminderDays">Mốc nhắc trước hạn *</Label>
                <Input
                  id="reminderDays"
                  value={form.reminderDaysText}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      reminderDaysText: event.target.value,
                    }))
                  }
                  placeholder="14, 7, 3, 1, 0"
                />
                <p className="text-xs text-slate-500">
                  Từ 0–60 ngày; 0 là nhắc đúng ngày deadline.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="escalationDays">Mốc escalation sau hạn *</Label>
                <Input
                  id="escalationDays"
                  value={form.escalationDaysText}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      escalationDaysText: event.target.value,
                    }))
                  }
                  placeholder="0, 1, 3, 7, 14"
                />
                <p className="text-xs text-slate-500">
                  Từ 0–365 ngày; 0 là ngay khi deadline vừa quá hạn.
                </p>
              </div>
            </div>

            <label className="flex w-fit cursor-pointer items-center gap-2 text-sm font-medium">
              <input
                type="checkbox"
                className="size-4 rounded border-slate-300"
                checked={form.active}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    active: event.target.checked,
                  }))
                }
              />
              Kích hoạt scheduler cho deadline này
            </label>

            <div className="flex flex-wrap gap-2">
              <Button type="submit" disabled={saveMutation.isPending}>
                <Save />
                {saveMutation.isPending
                  ? "Đang lưu..."
                  : editingId === null
                    ? "Tạo deadline"
                    : "Lưu thay đổi"}
              </Button>
              {editingId !== null && (
                <Button type="button" variant="outline" onClick={cancelEdit}>
                  <X /> Hủy chỉnh sửa
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Danh sách deadline</CardTitle>
          <CardDescription>
            Reminder chạy mỗi ngày lúc 08:00; escalation kiểm tra mỗi giờ ở
            phút 15 theo múi giờ Asia/Ho_Chi_Minh và đều có cơ chế gửi bù mốc.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          {deadlinesQuery.isLoading ? (
            <div className="p-8 text-center text-sm text-slate-500">
              Đang tải cấu hình...
            </div>
          ) : deadlines.length === 0 ? (
            <div className="p-8 text-center text-sm text-slate-500">
              Chưa có deadline nào được cấu hình.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Học kỳ</TableHead>
                    <TableHead>Deadline</TableHead>
                    <TableHead>Mốc nhắc</TableHead>
                    <TableHead>Mốc escalation</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead>Revision</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {deadlines.map((deadline) => (
                    <TableRow key={deadline.id}>
                      <TableCell>
                        <div className="font-semibold">
                          HK{deadline.semester} · {deadline.academicYear}
                        </div>
                        <div className="text-xs text-slate-500">
                          {deadline.active ? "Scheduler đang bật" : "Đã tạm dừng"}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="font-medium">
                          {formatDateTime(deadline.deadlineAt)}
                        </div>
                        <div className="text-xs text-slate-500">
                          {remainingText(deadline.daysRemaining, deadline.state)}
                        </div>
                      </TableCell>
                      <TableCell>{deadline.reminderDays.join(", ")} ngày</TableCell>
                      <TableCell>{deadline.escalationDays.join(", ")} ngày</TableCell>
                      <TableCell>
                        <DeadlineStateBadge state={deadline.state} />
                      </TableCell>
                      <TableCell>v{deadline.revision}</TableCell>
                      <TableCell>
                        <div className="flex flex-wrap justify-end gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => openDetails(deadline.id)}
                          >
                            <Eye /> Xem
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => beginEdit(deadline)}
                          >
                            <Pencil /> Sửa
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => dispatch(deadline.id, false)}
                            disabled={dispatchMutation.isPending}
                            title="Chỉ gửi nếu đang đến mốc nhắc"
                          >
                            <Play /> Chạy lịch
                          </Button>
                          <Button
                            size="sm"
                            variant={deadline.active ? "destructive" : "secondary"}
                            onClick={() =>
                              activeMutation.mutate({
                                id: deadline.id,
                                active: !deadline.active,
                              })
                            }
                            disabled={activeMutation.isPending}
                          >
                            <Power />
                            {deadline.active ? "Tắt" : "Bật"}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {selectedId !== null && (
        <Card>
          <CardHeader>
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <BellRing className="size-5" />
                  Kiểm tra người nhận
                </CardTitle>
                <CardDescription>
                  {selectedDeadline
                    ? `HK${selectedDeadline.semester} · ${selectedDeadline.academicYear}`
                    : "Đang tải cấu hình..."}
                </CardDescription>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  onClick={() => dispatch(selectedId, true)}
                  disabled={
                    dispatchMutation.isPending ||
                    !selectedDeadline?.active ||
                    selectedDeadline.state === "OVERDUE"
                  }
                >
                  <BellRing /> Gửi thử ngay
                </Button>
                <Button variant="ghost" onClick={() => setSelectedId(null)}>
                  <X /> Đóng
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            {dispatchResult && dispatchResult.deadlineId === selectedId && (
              <div className="grid gap-3 rounded-lg border bg-slate-50 p-4 sm:grid-cols-2 lg:grid-cols-5">
                <Metric label="Người nhận" value={dispatchResult.recipientCount} />
                <Metric label="Đã tạo" value={dispatchResult.notificationsQueued} />
                <Metric label="Đã gửi trước" value={dispatchResult.skippedAlreadySent} />
                <Metric label="Lỗi" value={dispatchResult.failedDeliveries} />
                <Metric
                  label="Thiếu tài khoản"
                  value={dispatchResult.skippedWithoutUserAccount}
                />
              </div>
            )}

            {previewQuery.isLoading ? (
              <div className="py-8 text-center text-sm text-slate-500">
                Đang phân tích phân công và trạng thái đề cương...
              </div>
            ) : previewQuery.isError ? (
              <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
                {getErrorMessage(previewQuery.error)}
              </div>
            ) : previewQuery.data ? (
              <>
                <div className="grid gap-3 sm:grid-cols-3">
                  <Metric
                    label="Giảng viên cần nhắc"
                    value={previewQuery.data.recipientCount}
                  />
                  <Metric
                    label="Môn chưa nộp"
                    value={previewQuery.data.missingCourseCount}
                  />
                  <Metric
                    label="Không có tài khoản"
                    value={previewQuery.data.skippedWithoutUserAccount}
                  />
                </div>

                {previewQuery.data.recipients.length === 0 ? (
                  <div className="flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
                    <CheckCircle2 className="size-5" />
                    Không có giảng viên nào còn thiếu đề cương ở học kỳ này.
                  </div>
                ) : (
                  <div className="overflow-x-auto rounded-lg border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Giảng viên</TableHead>
                          <TableHead>Email</TableHead>
                          <TableHead>Môn chưa nộp</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {previewQuery.data.recipients.map((recipient) => (
                          <TableRow key={recipient.userId}>
                            <TableCell>
                              <div className="font-medium">
                                {recipient.instructorName || recipient.username}
                              </div>
                              <div className="text-xs text-slate-500">
                                @{recipient.username}
                              </div>
                            </TableCell>
                            <TableCell>{recipient.email || "Chưa có email"}</TableCell>
                            <TableCell>
                              <div className="flex max-w-xl flex-wrap gap-1.5">
                                {recipient.missingCourses.map((course) => (
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
                )}
              </>
            ) : null}

            <div>
              <h3 className="mb-3 flex items-center gap-2 font-semibold">
                <History className="size-4" /> Nhật ký nhắc gần nhất
              </h3>
              {logsQuery.isLoading ? (
                <div className="text-sm text-slate-500">Đang tải nhật ký...</div>
              ) : (logsQuery.data?.length ?? 0) === 0 ? (
                <div className="rounded-lg border border-dashed p-5 text-center text-sm text-slate-500">
                  Chưa có reminder nào được tạo cho deadline này.
                </div>
              ) : (
                <div className="overflow-x-auto rounded-lg border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Thời gian</TableHead>
                        <TableHead>Người nhận</TableHead>
                        <TableHead>Mốc nhắc</TableHead>
                        <TableHead>Môn thiếu</TableHead>
                        <TableHead>Email</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {logsQuery.data?.map((log) => (
                        <TableRow key={log.id}>
                          <TableCell>{formatDateTime(log.createdAt)}</TableCell>
                          <TableCell>
                            <div className="font-medium">{log.recipientUsername}</div>
                            <div className="text-xs text-slate-500">
                              {log.recipientEmail}
                            </div>
                          </TableCell>
                          <TableCell>
                            {log.daysBefore === -1
                              ? "Gửi thử"
                              : log.daysBefore === 0
                                ? "Đúng ngày"
                                : `Trước ${log.daysBefore} ngày`}
                            <div className="text-xs text-slate-500">
                              revision {log.deadlineRevision}
                            </div>
                          </TableCell>
                          <TableCell>{log.missingCourseCodes}</TableCell>
                          <TableCell>
                            <Badge variant={log.emailQueued ? "secondary" : "outline"}>
                              {log.emailQueued ? "Đã vào outbox" : "Không có email"}
                            </Badge>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border bg-white px-4 py-3">
      <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
        {label}
      </div>
      <div className="mt-1 text-2xl font-bold text-slate-900">{value}</div>
    </div>
  )
}

function DeadlineStateBadge({ state }: { state: DeadlineState }) {
  const config: Record<
    DeadlineState,
    { label: string; variant: "default" | "secondary" | "destructive" | "outline" }
  > = {
    UPCOMING: { label: "Sắp tới", variant: "secondary" },
    DUE_TODAY: { label: "Đến hạn hôm nay", variant: "destructive" },
    OVERDUE: { label: "Đã quá hạn", variant: "destructive" },
    INACTIVE: { label: "Đang tắt", variant: "outline" },
  }
  const item = config[state]
  return <Badge variant={item.variant}>{item.label}</Badge>
}

function parseReminderDays(value: string): number[] {
  const tokens = value
    .split(/[;,\s]+/)
    .map((token) => token.trim())
    .filter(Boolean)

  if (tokens.length === 0) {
    throw new Error("Vui lòng nhập ít nhất một mốc nhắc.")
  }

  const days = tokens.map((token) => {
    const day = Number(token)
    if (!Number.isInteger(day) || day < 0 || day > 60) {
      throw new Error("Mốc nhắc phải là số nguyên từ 0 đến 60 ngày.")
    }
    return day
  })

  return [...new Set(days)].sort((a, b) => b - a)
}

function parseEscalationDays(value: string): number[] {
  const tokens = value
    .split(/[;,\s]+/)
    .map((token) => token.trim())
    .filter(Boolean)

  if (tokens.length === 0) {
    throw new Error("Vui lòng nhập ít nhất một mốc escalation.")
  }

  const days = tokens.map((token) => {
    const day = Number(token)
    if (!Number.isInteger(day) || day < 0 || day > 365) {
      throw new Error("Mốc escalation phải là số nguyên từ 0 đến 365 ngày.")
    }
    return day
  })

  return [...new Set(days)].sort((a, b) => a - b)
}

function toDateTimeLocal(value: string): string {
  return value ? value.slice(0, 16) : ""
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

function remainingText(days: number, state: DeadlineState): string {
  if (state === "INACTIVE") return "Lịch nhắc đang tắt"
  if (days < 0) return `Quá hạn ${Math.abs(days)} ngày`
  if (days === 0) return "Đến hạn hôm nay"
  return `Còn ${days} ngày`
}

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    return data?.message || error.message || "Không thể xử lý yêu cầu."
  }
  if (error instanceof Error) return error.message
  return "Không thể xử lý yêu cầu."
}
