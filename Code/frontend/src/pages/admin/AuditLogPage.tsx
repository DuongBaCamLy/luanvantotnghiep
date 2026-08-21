import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Search } from "lucide-react"

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

  const { data: auditLogs, isLoading, isError } = useQuery({
    queryKey: ["audit-logs", searchTerm],
    queryFn: () => searchTerm ? auditLogApi.search(searchTerm) : auditLogApi.getAll(),
  })

  const getActionBadge = (action: string) => {
    switch (action.toUpperCase()) {
      case "CREATE":
        return <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">TẠO MỚI</Badge>
      case "UPDATE":
        return <Badge className="bg-amber-50 text-amber-700 border border-amber-200">CẬP NHẬT</Badge>
      case "DELETE":
        return <Badge className="bg-rose-50 text-rose-700 border border-rose-200">XÓA</Badge>
      default:
        return <Badge className="bg-slate-50 text-slate-700 border border-slate-200">{action}</Badge>
    }
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-primary font-heading">
          Nhật ký hoạt động (Audit Log)
        </h1>
        <p className="text-sm text-slate-500">
          Theo dõi lịch sử thay đổi cấu trúc dữ liệu, các hành động thêm/sửa/xóa của toàn bộ người dùng trong hệ thống
        </p>
      </div>

      {/* Filter Bar */}
      <div className="flex items-center gap-4 bg-white p-4 rounded-xl border border-slate-100 shadow-sm max-w-md">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 size-4 text-slate-400" />
          <Input
            placeholder="Tìm kiếm theo bảng, hành động..."
            className="pl-9 h-9"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-slate-100 bg-white overflow-hidden shadow-sm">
        <Table>
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
                  Đang tải...
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-rose-500">
                  Lỗi khi tải nhật ký hoạt động.
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
                    {new Date(log.changedAt).toLocaleString("vi-VN")}
                  </TableCell>
                  <TableCell>{getActionBadge(log.action)}</TableCell>
                  <TableCell className="font-mono text-xs font-semibold text-slate-700">
                    {log.tableName}
                  </TableCell>
                  <TableCell className="text-slate-500 font-medium">#{log.recordId}</TableCell>
                  <TableCell className="max-w-[200px] truncate text-xs text-slate-400 font-mono" title={log.oldValue || ""}>
                    {log.oldValue || "-"}
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate text-xs text-slate-600 font-mono" title={log.newValue || ""}>
                    {log.newValue || "-"}
                  </TableCell>
                  <TableCell className="text-right font-medium text-slate-700">
                    {log.changedByUsername}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  )
}
