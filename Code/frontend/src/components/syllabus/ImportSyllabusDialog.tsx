import { useRef, useState } from "react"
import { AlertTriangle, CheckCircle2, FileSearch, Loader2 } from "lucide-react"
import { toast } from "sonner"

import { syllabusImportApi } from "@/api/syllabusImportApi"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { isUsableSyllabusImportPreview } from "@/lib/syllabusImportDraft"
import type { SyllabusImportPreviewResponse } from "@/types/syllabusImport"
import xlsxTemplateUrl from "../../../../templates/syllabus-import-template.xlsx?url"

type Props = {
  open: boolean
  onClose: () => void
  expectedCourseCode?: string
  expectedCourseName?: string
  onPreviewConfirmed: (preview: SyllabusImportPreviewResponse) => void
}

const normalizeCourseCode = (value?: string) => String(value ?? "")
  .trim()
  .toUpperCase()
  .replace(/[\s_-]+/g, "")

const normalizeCourseName = (value?: string) => String(value ?? "")
  .trim()
  .toUpperCase()
  .replace(/[^A-Z0-9+#]+/g, "")

const codesMatch = (source?: string, target?: string) => {
  const left = normalizeCourseCode(source)
  const right = normalizeCourseCode(target)
  if (!left || !right) return true
  if (left === right) return true
  // The course master uses an institutional IU suffix (IT116IU) while the
  // approved CS programme PDF prints the academic code (IT116).
  return left.replace(/IU$/, "") === right.replace(/IU$/, "")
}

export default function ImportSyllabusDialog({ open, onClose, expectedCourseCode, expectedCourseName, onPreviewConfirmed }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<SyllabusImportPreviewResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const previewRequestId = useRef(0)

  const close = () => {
    previewRequestId.current += 1
    setFile(null)
    setPreview(null)
    setLoading(false)
    onClose()
  }

  const handlePreview = async () => {
    if (!file) {
      toast.error("Please select a DOCX, XLSX, or PDF syllabus file.")
      return
    }
    if (!/\.(docx|xlsx|pdf)$/i.test(file.name)) {
      toast.error("Only DOCX, XLSX, and PDF syllabus files are supported.")
      return
    }
    const requestId = ++previewRequestId.current
    try {
      setLoading(true)
      setPreview(null)
      const result = await syllabusImportApi.preview(file)
      if (requestId !== previewRequestId.current) return
      setPreview(result)
      if (isUsableSyllabusImportPreview(result)) {
        toast.success("Syllabus extracted successfully. Review every field before saving.")
      } else {
        toast.warning("The file is missing one or more required syllabus sections.")
      }
    } catch (error) {
      if (requestId !== previewRequestId.current) return
      console.error(error)
      toast.error("Cannot extract syllabus data from this file.")
    } finally {
      if (requestId === previewRequestId.current) setLoading(false)
    }
  }

  const importedCourseCode = preview?.data?.sourceCourseCode
  const importedCourseName = preview?.data?.sourceCourseName
  const previewReady = isUsableSyllabusImportPreview(preview)
  const exactCourseCodeMatch = normalizeCourseCode(importedCourseCode) === normalizeCourseCode(expectedCourseCode)
  const institutionalAliasMatch = Boolean(
    importedCourseCode
    && expectedCourseCode
    && !exactCourseCodeMatch
    && codesMatch(importedCourseCode, expectedCourseCode)
    && (!importedCourseName || !expectedCourseName
      || normalizeCourseName(importedCourseName) === normalizeCourseName(expectedCourseName)),
  )
  const courseCodeMismatch = Boolean(
    importedCourseCode
    && expectedCourseCode
    && !exactCourseCodeMatch
    && !institutionalAliasMatch,
  )

  const handleContinue = () => {
    if (!previewReady || courseCodeMismatch) return
    onPreviewConfirmed(preview)
  }

  return (
    <Dialog open={open} onOpenChange={(value) => !value && close()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Import Syllabus File</DialogTitle>
          <DialogDescription>
            Upload a DOCX, XLSX, or PDF syllabus file. XLSX files must follow the syllabus import template. Nothing is saved until you review and press Save.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <div data-admin-upload className="space-y-2">
            <Label htmlFor="syllabus-import-file">Syllabus file</Label>
            <a href={xlsxTemplateUrl} download="syllabus-import-template.xlsx" className="block text-sm text-teal-700 underline">
              Download XLSX Template
            </a>
            <p className="text-xs text-slate-500">
              For XLSX, fill course code and course name in General Info with the selected course's identity, then replace the example syllabus content before importing.
            </p>
            <Input id="syllabus-import-file" type="file" accept=".docx,.xlsx,.pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/pdf" disabled={loading} onChange={(event) => {
              previewRequestId.current += 1
              setFile(event.target.files?.[0] ?? null)
              setPreview(null)
              setLoading(false)
            }} />
          </div>
          <Button type="button" disabled={loading || !file} onClick={handlePreview}>
            {loading ? <Loader2 className="size-4 animate-spin" /> : <FileSearch className="size-4" />}
            {loading ? "Extracting..." : "Extract & Preview"}
          </Button>

          {preview && (
            <div className="space-y-4 rounded-xl border border-slate-200 bg-slate-50/60 p-4">
              <div className="flex items-start gap-2">
                {previewReady ? <CheckCircle2 className="mt-0.5 size-5 text-emerald-600" /> : <AlertTriangle className="mt-0.5 size-5 text-amber-600" />}
                <div>
                  <p className="font-semibold text-slate-900">{preview.fileName}</p>
                  <p className="text-xs text-slate-500">
                    {previewReady ? "Ready to auto-fill the complete Syllabus Form." : `${preview.errorCount} validation issue(s); required sections are incomplete.`}
                    {preview.warningCount > 0 ? ` ${preview.warningCount} warning(s) require review.` : ""}
                  </p>
                </div>
              </div>
              {preview.issues?.length > 0 && <ul className="space-y-1 text-xs text-amber-800">{preview.issues.map((issue, index) => {
                const location = [issue.section, issue.row ? `row ${issue.row}` : "", issue.field].filter(Boolean).join(" / ")
                return <li key={`${issue.message}-${index}`}>- {location ? `[${location}] ` : ""}{issue.message}</li>
              })}</ul>}
              {courseCodeMismatch && (
                <div className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs leading-5 text-rose-800">
                  <AlertTriangle className="mt-0.5 size-4 shrink-0" />
                  <span>
                    File belongs to course <b>{importedCourseCode}</b>, but the selected course is <b>{expectedCourseCode}</b>. Choose the matching course before continuing.
                  </span>
                </div>
              )}
              {institutionalAliasMatch && (
                <div className="rounded-lg border border-sky-200 bg-sky-50 px-3 py-2 text-xs leading-5 text-sky-800">
                  The PDF uses academic course code <b>{importedCourseCode}</b>. It matches selected institutional course <b>{expectedCourseCode}</b> ({expectedCourseName}); the selected course identity will be kept when saving.
                </div>
              )}
              <div className="grid gap-3 text-sm sm:grid-cols-4">
                <PreviewCount label="CLOs" value={preview.data?.clos?.length ?? 0} />
                <PreviewCount label="Topics" value={Math.max(preview.data?.topics?.length ?? 0, preview.data?.weeklyActivities?.length ?? 0)} />
                <PreviewCount label="Assessments" value={preview.data?.assessments?.length ?? 0} />
                <PreviewCount label="Readings" value={preview.data?.readings?.length ?? 0} />
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={close}>Cancel</Button>
          {preview && <Button type="button" disabled={loading || !previewReady || courseCodeMismatch} onClick={handleContinue}>Review in Syllabus Form</Button>}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function PreviewCount({ label, value }: { label: string; value: number }) {
  return <div className="rounded-lg border bg-white px-3 py-2"><p className="text-xs text-slate-500">{label}</p><p className="font-semibold text-slate-900">{value}</p></div>
}
