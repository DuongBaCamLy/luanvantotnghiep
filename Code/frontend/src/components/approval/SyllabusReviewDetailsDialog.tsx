import { useNavigate } from "react-router-dom"
import { History } from "lucide-react"
import ApprovalHistoryTable from "./ApprovalHistoryTable"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { formatVersionLabel } from "@/lib/syllabusVersion"

export interface ReviewSyllabusContext {
  id: number
  courseCode: string
  courseName: string
  versionNumber: number
  versionLabel: string
}

export default function SyllabusReviewDetailsDialog({
  syllabus, syllabusPath, onClose,
}: { syllabus: ReviewSyllabusContext; syllabusPath: string; onClose: () => void }) {
  const navigate = useNavigate()
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}>
    <DialogContent className="max-h-[90dvh] overflow-y-auto p-0 sm:max-w-4xl">
      <DialogHeader className="border-b border-slate-100 px-5 py-5 pr-12 sm:px-6">
        <DialogTitle className="flex items-center gap-2 text-xl text-[#17343d]"><History className="size-5 text-[#007d84]" />Review Details</DialogTitle>
        <DialogDescription className="leading-6">
          <span className="font-semibold text-[#007d84]">{syllabus.courseCode}</span> · {syllabus.courseName}
          <span className="ml-2 whitespace-nowrap text-slate-500">Current version {formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel)}</span>
        </DialogDescription>
      </DialogHeader>
      <div className="px-3 pb-4 sm:px-5">
        <ApprovalHistoryTable key={syllabus.id} syllabusId={syllabus.id} initialVersionNumber={syllabus.versionNumber}
          onOpenSyllabus={() => { onClose(); navigate(syllabusPath) }} />
      </div>
    </DialogContent>
  </Dialog>
}

