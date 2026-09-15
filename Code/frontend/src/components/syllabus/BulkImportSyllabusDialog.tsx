import { useState } from "react"
import { FileStack, Loader2 } from "lucide-react"
import { toast } from "sonner"

import { syllabusImportApi } from "@/api/syllabusImportApi"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { BulkSyllabusImportPreviewResponse } from "@/types/syllabusImport"

type Props = {
  open: boolean
  onClose: () => void
  programId: number
  cohortId: number
  cohortName?: string
  allowedCourseCodes?: string[]
  onStart: (response: BulkSyllabusImportPreviewResponse) => void
}

export default function BulkImportSyllabusDialog({ open, onClose, programId, cohortId, cohortName, allowedCourseCodes, onStart }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [result, setResult] = useState<BulkSyllabusImportPreviewResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const close = () => {
    setFile(null)
    setResult(null)
    setLoading(false)
    onClose()
  }

  const extract = async () => {
    if (!file) return
    if (file.size > 200 * 1024 * 1024) {
      toast.error("The PDF exceeds the 200 MB upload limit.")
      return
    }
    try {
      setLoading(true)
      const response = await syllabusImportApi.previewBulk(file, programId, cohortId)
      setResult(response)
      const normalize = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, "")
      const allowed = new Set((allowedCourseCodes ?? []).map(normalize))
      const blocked = allowedCourseCodes
        ? response.items.map((item) => item.preview.data?.sourceCourseCode || "Unknown")
          .filter((code) => !allowed.has(normalize(code)))
        : []
      if (blocked.length > 0) {
        toast.error(`Import blocked. You are not assigned to: ${blocked.join(", ")}.`)
      } else {
        toast.success(`Detected ${response.syllabusCount} syllabuses.`)
      }
    } catch (error) {
      console.error(error)
      const status = (error as { response?: { status?: number; data?: { message?: string } } })?.response?.status
      const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(status === 413
        ? "The PDF exceeds the server upload limit."
        : message || "Upload failed. Check that the backend was restarted, then try again.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(value) => !value && close()}>
      <DialogContent className="overflow-hidden border-[#cfe2e4] bg-white p-0 sm:max-w-xl">
        <div className="h-1 bg-gradient-to-r from-[#007d84] via-[#20a0a5] to-[#f0a72f]" />
        <div className="space-y-5 px-6 pb-2 pt-5">
          <DialogHeader className="text-left">
            <DialogTitle className="text-xl text-[#006f76]">Import Program Document</DialogTitle>
            <DialogDescription>{cohortName} · PDF or DOCX containing multiple syllabuses</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label>PDF or Word document</Label>
            <Input type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" disabled={loading} onChange={(event) => {
              setFile(event.target.files?.[0] ?? null)
              setResult(null)
            }} />
          </div>
          {result && (() => {
            const normalize = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, "")
            const allowed = new Set((allowedCourseCodes ?? []).map(normalize))
            const blocked = allowedCourseCodes
              ? result.items.map((item) => item.preview.data?.sourceCourseCode || "Unknown").filter((code) => !allowed.has(normalize(code)))
              : []
            return <>
            <div className="grid grid-cols-3 gap-3 rounded-xl border border-[#d9e8ea] bg-[#f5fafa] p-4 text-center">
              <Stat value={result.pageCount} label="Pages" />
              <Stat value={result.syllabusCount} label="Detected" />
              <Stat value={result.items.filter((item) => item.preview.valid).length} label="Ready" />
            </div>
            <p className="text-xs font-semibold text-[#006f76]">Source type: {result.sourceType === "DOCX" ? "WORD" : "PDF"}</p>
            {blocked.length > 0 && <p className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-800">You are not assigned to these courses and cannot import their syllabuses: <b>{blocked.join(", ")}</b>. No syllabus will be saved.</p>}
            </>
          })()}
        </div>
        <DialogFooter className="border-t border-[#e2ecee] bg-[#f7fafb] px-6 py-4">
          <Button variant="ghost" onClick={close}>Cancel</Button>
          {!result ? (
            <Button className="bg-[#007d84] hover:bg-[#006b71]" disabled={!file || loading} onClick={extract}>
              {loading ? <Loader2 className="size-4 animate-spin" /> : <FileStack className="size-4" />}
              {loading ? "Extracting..." : "Extract"}
            </Button>
          ) : (
            <Button className="bg-[#007d84] hover:bg-[#006b71]" disabled={result.syllabusCount === 0 || Boolean(allowedCourseCodes && result.items.some((item) => !new Set(allowedCourseCodes.map((code) => code.replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, ""))).has(String(item.preview.data?.sourceCourseCode ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase().replace(/IU$/, ""))))} onClick={() => {
              onStart(result)
              close()
            }}>Import {result.syllabusCount}</Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function Stat({ value, label }: { value: number; label: string }) {
  return <div><div className="text-xl font-bold text-[#006f76]">{value}</div><div className="text-[11px] uppercase tracking-wide text-slate-500">{label}</div></div>
}
