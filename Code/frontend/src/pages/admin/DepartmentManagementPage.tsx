import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"

import { departmentApi } from "@/api/departmentApi"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

export default function DepartmentManagementPage() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  
  // Form fields
  const [code, setCode] = useState("")
  const [name, setName] = useState("")
  const [nameVn, setNameVn] = useState("")

  const { data: departments, isLoading, isError } = useQuery({
    queryKey: ["departments"],
    queryFn: departmentApi.getAll,
  })

  const createMutation = useMutation({
    mutationFn: departmentApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["departments"] })
      setOpen(false)
      setCode("")
      setName("")
      setNameVn("")
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "Đã xảy ra lỗi khi tạo bộ môn"
      alert(msg)
    }
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!code || !name || !nameVn) {
      alert("Vui lòng điền đầy đủ thông tin")
      return
    }
    createMutation.mutate({ code, name, nameVn })
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary font-heading">
            Quản lý Khoa / Bộ môn
          </h1>
          <p className="text-sm text-slate-500">
            Xem và thêm mới các khoa, tổ bộ môn đào tạo trong khoa
          </p>
        </div>
        <Button className="bg-primary text-white hover:bg-primary/90 flex items-center gap-1.5" onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Thêm khoa / bộ môn
        </Button>
      </div>

      {/* Main Table */}
      <div className="rounded-xl border border-slate-100 bg-white overflow-hidden shadow-sm">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50/50">
              <TableHead className="font-semibold text-slate-700 w-[120px]">Mã khoa/BM</TableHead>
              <TableHead className="font-semibold text-slate-700">Tên Tiếng Việt</TableHead>
              <TableHead className="font-semibold text-slate-700">Tên Tiếng Anh</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[150px]">Trạng thái</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[200px]">Ngày tạo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-slate-400">
                  Đang tải...
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-rose-500">
                  Lỗi khi tải dữ liệu khoa/bộ môn.
                </TableCell>
              </TableRow>
            ) : !departments || departments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-slate-400">
                  Chưa có khoa / bộ môn nào trong hệ thống.
                </TableCell>
              </TableRow>
            ) : (
              departments.map((dept) => (
                <TableRow key={dept.id} className="hover:bg-slate-50/30">
                  <TableCell className="font-mono text-sm font-semibold text-slate-700">
                    {dept.code}
                  </TableCell>
                  <TableCell className="font-semibold text-slate-900">
                    {dept.nameVn}
                  </TableCell>
                  <TableCell className="text-slate-600">
                    {dept.name}
                  </TableCell>
                  <TableCell>
                    {dept.isActive ? (
                      <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">
                        Đang hoạt động
                      </Badge>
                    ) : (
                      <Badge className="bg-slate-100 text-slate-500 border border-slate-200">
                        Ngưng hoạt động
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-sm text-slate-500">
                    {new Date(dept.createdAt).toLocaleDateString("vi-VN")}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Dialog creation */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md bg-white">
          <DialogHeader>
            <DialogTitle className="text-primary font-heading">Thêm Khoa / Bộ môn mới</DialogTitle>
            <DialogDescription>
              Nhập mã và tên khoa để tạo đơn vị bộ môn mới.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 pt-2">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="dept-code">Mã khoa/bộ môn (Code)</Label>
              <Input
                id="dept-code"
                placeholder="VD: CSE, CE, IT..."
                value={code}
                onChange={(e) => setCode(e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="dept-name-vn">Tên tiếng Việt</Label>
              <Input
                id="dept-name-vn"
                placeholder="VD: Khoa Khoa học Máy tính..."
                value={nameVn}
                onChange={(e) => setNameVn(e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="dept-name-en">Tên tiếng Anh (English Name)</Label>
              <Input
                id="dept-name-en"
                placeholder="VD: School of Computer Science..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Hủy
              </Button>
              <Button type="submit" className="bg-primary text-white hover:bg-primary/90" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Đang lưu..." : "Thêm mới"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
