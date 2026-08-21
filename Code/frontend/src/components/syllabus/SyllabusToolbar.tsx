// src/components/syllabus/SyllabusToolbar.tsx
import { useSearchParams } from "react-router-dom"
import { Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { MAJORS } from "@/lib/syllabusHelper"

export default function SyllabusToolbar() {
  const [searchParams, setSearchParams] = useSearchParams()

  // Hàm cập nhật URL Params giúp đồng bộ 3 giao diện
  const updateFilter = (key: string, value: string) => {
    const newParams = new URLSearchParams(searchParams)
    if (value && value !== "all") {
      newParams.set(key, value)
    } else {
      newParams.delete(key)
    }
    // Ghi đè URL hiện tại mà không làm mất các params khác
    setSearchParams(newParams)
  }

  return (
    <div className="flex flex-col md:flex-row gap-4 mb-6 bg-white p-4 rounded-xl border border-slate-100 shadow-sm">
      {/* Tìm kiếm theo Mã hoặc Tên */}
      <div className="relative flex-1">
        <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
        <Input
          placeholder="Search by course code or name..."
          className="pl-9 bg-slate-50/50"
          value={searchParams.get("q") || ""}
          onChange={(e) => updateFilter("q", e.target.value)}
        />
      </div>

      {/* Lọc theo Học kỳ */}
      <Select 
        value={searchParams.get("semester") || "all"} 
        onValueChange={(v) => updateFilter("semester", v)}
      >
        <SelectTrigger className="w-full md:w-[150px]">
          <SelectValue placeholder="Semester" />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectItem value="all">All Semesters</SelectItem>
          <SelectItem value="1">Semester 1</SelectItem>
          <SelectItem value="2">Semester 2</SelectItem>
          <SelectItem value="3">Summer Semester</SelectItem>
        </SelectContent>
      </Select>

      {/* Lọc theo Khóa */}
      <Select 
        value={searchParams.get("year") || "all"} 
        onValueChange={(v) => updateFilter("year", v)}
      >
        <SelectTrigger className="w-full md:w-[150px]">
          <SelectValue placeholder="Cohort" />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectItem value="all">All Cohorts</SelectItem>
          <SelectItem value="K22">K22</SelectItem>
          <SelectItem value="K23">K23</SelectItem>
          <SelectItem value="K24">K24</SelectItem>
          <SelectItem value="K25">K25</SelectItem>
        </SelectContent>
      </Select>

      {/* Lọc theo Chuyên ngành */}
      <Select 
        value={searchParams.get("major") || "all"} 
        onValueChange={(v) => updateFilter("major", v)}
      >
        <SelectTrigger className="w-full md:w-[200px]">
          <SelectValue placeholder="Major" />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectItem value="all">All Majors</SelectItem>
          {MAJORS.map((m) => (
            <SelectItem key={m} value={m}>{m}</SelectItem>
          ))}
        </SelectContent>
      </Select>

      {/* Lọc theo Trạng thái */}
      <Select 
        value={searchParams.get("status") || "all"} 
        onValueChange={(v) => updateFilter("status", v)}
      >
        <SelectTrigger className="w-full md:w-[180px]">
          <SelectValue placeholder="Status" />
        </SelectTrigger>
        <SelectContent className="bg-white">
          <SelectItem value="all">All Statuses</SelectItem>
          <SelectItem value="DRAFT">Draft</SelectItem>
          <SelectItem value="SUBMITTED">Submitted</SelectItem>
          <SelectItem value="APPROVED">Approved</SelectItem>
          <SelectItem value="REJECTED">Rejected</SelectItem>
        </SelectContent>
      </Select>
    </div>
  )
}