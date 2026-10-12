import { useEffect, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Search, RefreshCw, LoaderCircle } from "lucide-react"
import { isAxiosError } from "axios"
import type { AuditLog } from "@/types/admin"
import { useAuthStore } from "@/store/authStore"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"

import { auditLogApi } from "@/api/auditLogApi"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

export default function AuditLogPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [filters, setFilters] = useState({ q: "", action: "", page: 0 })
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null)
  const user = useAuthStore((state) => state.user)

  useEffect(() => {
    const timer = window.setTimeout(() => setFilters((current) => ({ ...current, q: searchTerm.trim(), page: 0 })), 300)
    return () => window.clearTimeout(timer)
  }, [searchTerm])

  const { data, isLoading, isFetching, isError, error, refetch } = useQuery({
    queryKey: ["audit-logs-page", user?.userId, filters],
    queryFn: ({ signal }) => auditLogApi.getPage({ ...filters, action: filters.action || undefined, size: 25 }, signal),
    enabled: Boolean(user),
    retry: false,
  })
  const auditLogs = data?.content ?? []
  const errorMessage = isAxiosError(error) && error.response?.status === 403
    ? "Bạn không có quyền xem nhật ký hoạt động. Chỉ tài khoản Administrator được phép truy cập."
    : isAxiosError(error) && (error.code === "ECONNABORTED" || !error.response)
      ? "Không thể kết nối hoặc máy chủ phản hồi quá lâu. Vui lòng kiểm tra kết nối và thử lại."
      : "Không thể tải nhật ký hoạt động. Vui lòng thử lại."
  const formatValue = (value: string | null) => {
    if (value === null) return "Không có dữ liệu"
    try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value }
  }
  const formatTime = (value: string) => {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? "Không ghi nhận" : date.toLocaleString("vi-VN")
  }

  const getActionBadge = (action: string) => {
    switch (action.toUpperCase()) {
      case "CREATE":
        return <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">TẠO MỚI</Badge>
      case "UPDATE":
        return <Badge className="bg-amber-50 text-amber-700 border border-amber-200">CẬP NHẬT</Badge>
      case "DELETE":
        return <Badge className="bg-rose-50 text-rose-700 border border-rose-200">XÓA</Badge>
      case "STATUS_CHANGE":
        return <Badge className="bg-blue-50 text-blue-700 border border-blue-200">ĐỔI TRẠNG THÁI</Badge>
      default:
        return <Badge className="bg-slate-50 text-slate-700 border border-slate-200">{action}</Badge>
    }
  }

  return (
    <div data-admin-page="AuditLogPage" className="space-y-4">
      {/* Header */}
      <div data-admin-page-header="AuditLogPage">
        <h1 className="text-2xl font-bold text-primary font-heading">
          Nhật ký hoạt động (Audit Log)
        </h1>
        <p className="text-sm text-slate-500">
          Theo dõi lịch sử thay đổi cấu trúc dữ liệu, các hành động thêm/sửa/xóa của toàn bộ người dùng trong hệ thống
        </p>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center gap-3 bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 size-4 text-slate-400" />
          <Input
            placeholder="Tìm theo bảng, CREATE/UPDATE/DELETE/STATUS_CHANGE, người thực hiện hoặc mã ID..."
            aria-label="Tìm kiếm nhật ký hoạt động"
            className="pl-9 h-9"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select aria-label="Lọc hành động" className="h-9 rounded-md border border-slate-200 bg-white px-3 text-sm"
          value={filters.action} onChange={(event) => setFilters((current) => ({ ...current, action: event.target.value, page: 0 }))}>
          <option value="">Tất cả hành động</option>
          <option value="CREATE">Tạo mới</option><option value="UPDATE">Cập nhật</option>
          <option value="DELETE">Xóa</option><option value="STATUS_CHANGE">Đổi trạng thái</option>
        </select>
        <Button variant="outline" disabled={isFetching} onClick={() => void refetch()}>
          {isFetching ? <LoaderCircle className="size-4 animate-spin" /> : <RefreshCw className="size-4" />}Làm mới
        </Button>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-slate-100 bg-white overflow-hidden shadow-sm">
        <Table className="min-w-[1000px]">
          <TableHeader>
            <TableRow className="bg-slate-50/50">
              <TableHead className="font-semibold text-slate-700 w-[180px]">Thời gian</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[120px]">Hành động</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[150px]">Bảng dữ liệu</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[100px]">Mã ID</TableHead>
              <TableHead className="font-semibold text-slate-700">Giá trị cũ (Old Value)</TableHead>
              <TableHead className="font-semibold text-slate-700">Giá trị mới (New Value)</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[120px] text-right">Người sửa</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-slate-400">
                  <span role="status" className="inline-flex items-center gap-2"><LoaderCircle className="size-4 animate-spin" />Đang tải nhật ký hoạt động...</span>
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-rose-500">
                  <div role="alert" className="space-y-3"><p>{errorMessage}</p><Button variant="outline" disabled={isFetching} onClick={() => void refetch()}>Thử lại</Button></div>
                </TableCell>
              </TableRow>
            ) : !auditLogs || auditLogs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-slate-400">
                  Không tìm thấy hoạt động nào phù hợp.
                </TableCell>
              </TableRow>
            ) : (
              auditLogs.map((log) => (
                <TableRow key={log.id} className="hover:bg-slate-50/30 text-sm">
                  <TableCell className="text-slate-500 font-mono text-xs">
                    {formatTime(log.changedAt)}
                  </TableCell>
                  <TableCell>{getActionBadge(log.action)}</TableCell>
                  <TableCell className="font-mono text-xs font-semibold text-slate-700">
                    {log.tableName}
                  </TableCell>
                  <TableCell className="text-slate-500 font-medium">#{log.recordId}</TableCell>
                  <TableCell className="max-w-[200px] truncate text-xs text-slate-400 font-mono" title={log.oldValue || ""}>
                    <button type="button" className="max-w-full truncate text-left hover:text-[#007d84] hover:underline" onClick={() => setSelectedLog(log)} aria-label={`Xem giá trị cũ nhật ký ${log.id}`}>{log.oldValue ?? "—"}</button>
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate text-xs text-slate-600 font-mono" title={log.newValue || ""}>
                    <button type="button" className="max-w-full truncate text-left hover:text-[#007d84] hover:underline" onClick={() => setSelectedLog(log)} aria-label={`Xem giá trị mới nhật ký ${log.id}`}>{log.newValue ?? "—"}</button>
                  </TableCell>
                  <TableCell className="text-right font-medium text-slate-700">
                    <button type="button" className="hover:text-[#007d84] hover:underline" onClick={() => setSelectedLog(log)} aria-label={`Xem chi tiết nhật ký ${log.id}`}>{log.changedByUsername || "Không ghi nhận"}<span className="mt-1 block text-xs font-normal text-[#007d84]">Xem chi tiết</span></button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        {!isError && data && <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 p-4 text-sm text-slate-500">
          <span role="status">{data.totalElements.toLocaleString("vi-VN")} nhật ký · Trang {data.totalPages ? data.number + 1 : 0}/{data.totalPages}</span>
          <div className="flex gap-2">
            <Button variant="outline" disabled={isFetching || filters.page === 0} onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}>Trang trước</Button>
            <Button variant="outline" disabled={isFetching || filters.page + 1 >= data.totalPages} onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}>Trang sau</Button>
          </div>
        </div>}
      </div>
      <Dialog open={Boolean(selectedLog)} onOpenChange={(open) => { if (!open) setSelectedLog(null) }}>
        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-5xl">
          <DialogHeader><DialogTitle>Chi tiết nhật ký #{selectedLog?.id}</DialogTitle>
            <DialogDescription>{selectedLog ? `${selectedLog.tableName} · Bản ghi #${selectedLog.recordId} · ${selectedLog.changedByUsername} · ${formatTime(selectedLog.changedAt)}` : ""}</DialogDescription>
          </DialogHeader>
          {selectedLog && <>
            <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500">{getActionBadge(selectedLog.action)}<span>IP: {selectedLog.ipAddress || "Không ghi nhận"}</span></div>
            <p className="break-all text-xs text-slate-500">User agent: {selectedLog.userAgent || "Không ghi nhận"}</p>
            <div className="grid gap-4 md:grid-cols-2">
              {[{ label: "Giá trị cũ", value: selectedLog.oldValue }, { label: "Giá trị mới", value: selectedLog.newValue }].map(({ label, value }) => <section key={label} className="min-w-0"><h3 className="mb-2 font-semibold">{label}</h3><pre className="max-h-[55vh] overflow-auto whitespace-pre-wrap break-words rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs leading-5">{formatValue(value)}</pre></section>)}
            </div>
          </>}
        </DialogContent>
      </Dialog>
    </div>
  )
}
