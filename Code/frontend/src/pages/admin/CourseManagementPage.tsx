import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { Plus } from "lucide-react"

import { courseApi } from "@/api/courseApi"
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

export default function CourseManagementPage() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)

  // Form Fields
  const [courseCode, setCourseCode] = useState("")
  const [name, setName] = useState("")
  const [nameVn, setNameVn] = useState("")
  const [departmentId, setDepartmentId] = useState("")
  const [creditTheory, setCreditTheory] = useState("3")
  const [creditLab, setCreditLab] = useState("0")
  const [courseLevel, setCourseLevel] = useState("INTRODUCTORY")
  const [description, setDescription] = useState("")

  const { data: courses, isLoading, isError } = useQuery({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
  })

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: departmentApi.getAll,
  })

  const createMutation = useMutation({
    mutationFn: courseApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["courses"] })
      setOpen(false)
      // Reset
      setCourseCode("")
      setName("")
      setNameVn("")
      setDepartmentId("")
      setCreditTheory("3")
      setCreditLab("0")
      setCourseLevel("INTRODUCTORY")
      setDescription("")
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "Đã xảy ra lỗi khi tạo môn học"
      alert(msg)
    }
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!courseCode || !name || !nameVn || !departmentId || !courseLevel) {
      alert("Vui lòng điền đầy đủ các thông tin bắt buộc")
      return
    }

    createMutation.mutate({
      courseCode,
      name,
      nameVn,
      departmentId: Number(departmentId),
      creditTheory: Number(creditTheory),
      creditLab: Number(creditLab),
      courseLevel,
      description: description || undefined
    })
  }

  const getLevelLabel = (level: string) => {
    switch (level) {
      case "INTRODUCTORY":
        return "Cơ sở"
      case "INTERMEDIATE":
        return "Trung cấp"
      case "ADVANCED":
        return "Chuyên sâu"
      default:
        return level
    }
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary font-heading">
            Quản lý Môn học
          </h1>
          <p className="text-sm text-slate-500">
            Xem và cấu hình thông tin các môn học, số tín chỉ lý thuyết/thực hành trong hệ thống
          </p>
        </div>
        <Button className="bg-primary text-white hover:bg-primary/90 flex items-center gap-1.5" onClick={() => setOpen(true)}>
          <Plus className="size-4" />
          Thêm môn học mới
        </Button>
      </div>

      {/* Main Table */}
      <div className="rounded-xl border border-slate-100 bg-white overflow-hidden shadow-sm">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50/50">
              <TableHead className="font-semibold text-slate-700 w-[120px]">Mã môn</TableHead>
              <TableHead className="font-semibold text-slate-700">Tên môn học</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[120px]">Bộ môn</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[100px] text-center">LT (Credits)</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[100px] text-center">TH (Credits)</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[100px] text-center">Tổng TC</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[120px]">Cấp độ</TableHead>
              <TableHead className="font-semibold text-slate-700 w-[120px]">Trạng thái</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8 text-slate-400">
                  Đang tải...
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8 text-rose-500">
                  Lỗi khi tải dữ liệu môn học.
                </TableCell>
              </TableRow>
            ) : !courses || courses.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8 text-slate-400">
                  Chưa có môn học nào trong hệ thống.
                </TableCell>
              </TableRow>
            ) : (
              courses.map((course) => (
                <TableRow key={course.id} className="hover:bg-slate-50/30">
                  <TableCell className="font-mono text-sm font-semibold text-slate-700">
                    {course.courseCode}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-semibold text-slate-900">{course.nameVn}</span>
                      <span className="text-xs text-slate-400">{course.name}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-slate-600 text-sm">
                    {course.departmentCode}
                  </TableCell>
                  <TableCell className="text-center">{course.creditTheory}</TableCell>
                  <TableCell className="text-center">{course.creditLab}</TableCell>
                  <TableCell className="text-center font-semibold">{course.totalCredits} TC</TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-indigo-600 bg-indigo-50/30 border-indigo-200">
                      {getLevelLabel(course.courseLevel)}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {course.isActive ? (
                      <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200">
                        Đang mở
                      </Badge>
                    ) : (
                      <Badge className="bg-slate-100 text-slate-500 border border-slate-200">
                        Đóng
                      </Badge>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Dialog creation */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg bg-white">
          <DialogHeader>
            <DialogTitle className="text-primary font-heading">Thêm môn học mới</DialogTitle>
            <DialogDescription>
              Nhập mã môn, số tín chỉ và thông tin mô tả cơ bản của môn học.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 pt-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="c-code">Mã môn học</Label>
                <Input
                  id="c-code"
                  placeholder="VD: IT013IU, MA001IU..."
                  value={courseCode}
                  onChange={(e) => setCourseCode(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label>Bộ môn phụ trách</Label>
                <Select value={departmentId} onValueChange={setDepartmentId}>
                  <SelectTrigger className="bg-white border-slate-200">
                    <SelectValue placeholder="Chọn bộ môn..." />
                  </SelectTrigger>
                  <SelectContent>
                    {departments?.map((d) => (
                      <SelectItem key={d.id} value={String(d.id)}>
                        {d.code} - {d.nameVn}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="c-name-vn">Tên môn học (Tiếng Việt)</Label>
              <Input
                id="c-name-vn"
                placeholder="VD: Cấu trúc dữ liệu và Giải thuật..."
                value={nameVn}
                onChange={(e) => setNameVn(e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="c-name-en">Tên tiếng Anh (English Name)</Label>
              <Input
                id="c-name-en"
                placeholder="VD: Algorithms and Data Structures..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="c-theory">Tín chỉ lý thuyết</Label>
                <Input
                  id="c-theory"
                  type="number"
                  min={0}
                  value={creditTheory}
                  onChange={(e) => setCreditTheory(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="c-lab">Tín chỉ thực hành</Label>
                <Input
                  id="c-lab"
                  type="number"
                  min={0}
                  value={creditLab}
                  onChange={(e) => setCreditLab(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label>Cấp độ môn học</Label>
                <Select value={courseLevel} onValueChange={setCourseLevel}>
                  <SelectTrigger className="bg-white border-slate-200">
                    <SelectValue placeholder="Cấp độ..." />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="INTRODUCTORY">Cơ sở (Intro)</SelectItem>
                    <SelectItem value="INTERMEDIATE">Trung cấp (Intermediate)</SelectItem>
                    <SelectItem value="ADVANCED">Chuyên sâu (Advanced)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="c-desc">Mô tả môn học</Label>
              <textarea
                id="c-desc"
                placeholder="Nhập mô tả tóm tắt nội dung môn học..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="flex min-h-[80px] w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm transition-colors placeholder:text-slate-400 outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
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
