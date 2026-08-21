import { useRef, useState } from "react"
import axios from "axios"
import {
  AlertTriangle,
  CheckCircle2,
  FileText,
  Upload,
  X,
} from "lucide-react"
import { useQueryClient } from "@tanstack/react-query"

import { Button } from "@/components/ui/button"
import { syllabusImportApi } from "@/api/syllabusImportApi"
import type {
  SyllabusImportMode,
  SyllabusImportPreviewResponse,
} from "@/types/syllabusImport"

interface Props {
  syllabusId: number
  open: boolean
  onClose: () => void
  onImported?: () => void
}

const messageOf = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string }
      | undefined

    return data?.message ?? "Không thể import file."
  }

  return error instanceof Error
    ? error.message
    : "Không thể import file."
}

export default function ImportSyllabusDialog({
  syllabusId,
  open,
  onClose,
  onImported,
}: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const queryClient = useQueryClient()

  const [preview, setPreview] =
    useState<SyllabusImportPreviewResponse | null>(null)

  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [importMode, setImportMode] = useState<SyllabusImportMode>("MERGE")

  if (!open) {
    return null
  }

  const chooseFile = async (file?: File) => {
    if (!file) {
      return
    }

    setBusy(true)
    setError("")
    setPreview(null)

    try {
      const result =
        await syllabusImportApi.preview(
          syllabusId,
          file
        )

      setPreview(result)
    } catch (e) {
      setError(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  const confirm = async () => {
    if (!preview?.valid) {
      return
    }

    setBusy(true)
    setError("")

    try {
      await syllabusImportApi.confirm(
        syllabusId,
        preview.data,
        importMode,
      )

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["syllabus", syllabusId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["clos", syllabusId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["topics", syllabusId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["assessments", syllabusId],
        }),
        queryClient.invalidateQueries({
          queryKey: ["syllabus-books", syllabusId],
        }),
      ])

      alert("Import đề cương thành công.")

      onImported?.()
      onClose()
    } catch (e) {
      setError(messageOf(e))
    } finally {
      setBusy(false)
    }
  }

  const summaryItems = preview
    ? [
        {
          label: "CLO",
          value: preview.data.clos.length,
        },
        {
          label: "Topics",
          value: preview.data.topics.length,
        },
        {
          label: "Assessments",
          value: preview.data.assessments.length,
        },
        {
          label: "Reading List",
          value: preview.data.readingList.length,
        },
        {
          label: "CLO–PLO",
          value: preview.data.cloPloMappings?.length ?? 0,
        },
        {
          label: "Topic–CLO",
          value: preview.data.topicCloMappings?.length ?? 0,
        },
        {
          label: "Assessment–CLO",
          value: preview.data.assessmentCloMappings?.length ?? 0,
        },
      ]
    : []

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-xl bg-white shadow-xl">
        <div className="flex items-start justify-between border-b p-5">
          <div>
            <h2 className="flex items-center gap-2 text-xl font-semibold">
              <FileText className="h-5 w-5" />
              Import syllabus từ PDF / Word / Excel
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Xem trước dữ liệu, kiểm tra lỗi rồi mới xác nhận
              ghi vào bản Draft.
            </p>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            disabled={busy}
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </Button>
        </div>

        <div className="space-y-5 p-5">
          <div className="rounded-lg border border-dashed p-6 text-center">
            <Upload className="mx-auto h-8 w-8 text-muted-foreground" />

            <p className="mt-2 font-medium">
              Chọn file .pdf, .docx hoặc .xlsx, tối đa 10 MB
            </p>

            <input
              ref={inputRef}
              type="file"
              accept=".pdf,.docx,.xlsx"
              className="hidden"
              onChange={(event) => {
                const file =
                  event.target.files?.[0]

                void chooseFile(file)

                event.target.value = ""
              }}
            />

            <Button
              type="button"
              className="mt-3 gap-2"
              variant="outline"
              disabled={busy}
              onClick={() =>
                inputRef.current?.click()
              }
            >
              <Upload className="h-4 w-4" />

              {busy
                ? "Đang đọc file..."
                : "Chọn file"}
            </Button>
          </div>

          {error && (
            <div className="flex gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {preview && (
            <>
              <div
                className={`rounded-lg border p-4 ${
                  preview.valid
                    ? "border-emerald-200 bg-emerald-50"
                    : "border-red-200 bg-red-50"
                }`}
              >
                <div className="flex items-start gap-3">
                  {preview.valid ? (
                    <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
                  ) : (
                    <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
                  )}

                  <div>
                    <p className="font-medium">
                      {preview.fileName}
                    </p>

                    <p className="mt-1 text-sm">
                      {preview.errorCount} lỗi,{" "}
                      {preview.warningCount} cảnh báo
                    </p>
                  </div>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {summaryItems.map((item) => (
                  <div
                    key={item.label}
                    className="rounded-lg border p-4"
                  >
                    <p className="text-sm text-muted-foreground">
                      {item.label}
                    </p>

                    <p className="mt-1 text-2xl font-semibold">
                      {item.value}
                    </p>
                  </div>
                ))}
              </div>

              <div className="rounded-lg border border-sky-200 bg-sky-50/60 p-4">
                <label
                  htmlFor="syllabus-import-mode"
                  className="block text-sm font-medium text-slate-900"
                >
                  Import mode
                </label>
                <select
                  id="syllabus-import-mode"
                  value={importMode}
                  onChange={(event) =>
                    setImportMode(event.target.value as SyllabusImportMode)
                  }
                  className="mt-2 h-9 w-full rounded-md border border-slate-300 bg-white px-3 text-sm sm:max-w-md"
                >
                  <option value="MERGE">Merge with existing Draft (recommended)</option>
                  <option value="UPDATE_DETECTED_FIELDS_ONLY">Update detected fields only</option>
                  <option value="REPLACE_ALL">Replace all imported sections</option>
                </select>
                <p className="mt-2 text-xs text-slate-600">
                  The default keeps sections that the file does not contain. Replace all removes existing structured sections before applying the preview.
                </p>
              </div>

              {preview.issues.length > 0 && (
                <div className="overflow-hidden rounded-lg border">
                  <div className="border-b bg-muted/40 px-4 py-3">
                    <h3 className="font-medium">
                      Chi tiết kiểm tra
                    </h3>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead className="bg-muted/30">
                        <tr>
                          <th className="px-4 py-3 text-left">
                            Mức
                          </th>

                          <th className="px-4 py-3 text-left">
                            Phần
                          </th>

                          <th className="px-4 py-3 text-left">
                            Dòng
                          </th>

                          <th className="px-4 py-3 text-left">
                            Thông báo
                          </th>
                        </tr>
                      </thead>

                      <tbody>
                        {preview.issues.map(
                          (issue, index) => (
                            <tr
                              key={`${issue.section}-${issue.row ?? "none"}-${index}`}
                              className="border-t"
                            >
                              <td className="px-4 py-3">
                                {issue.severity}
                              </td>

                              <td className="px-4 py-3">
                                {issue.section}
                              </td>

                              <td className="px-4 py-3">
                                {issue.row ?? "—"}
                              </td>

                              <td className="px-4 py-3">
                                {issue.message}
                              </td>
                            </tr>
                          )
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              <div className="rounded-lg border p-4">
                <h3 className="mb-4 font-medium">
                  Xem trước thông tin chung
                </h3>

                <div className="grid gap-3 sm:grid-cols-2">
                  <div>
                    <span className="text-sm text-muted-foreground">
                      Imported course:
                    </span>{" "}
                    {preview.data.sourceCourseCode || "—"}
                    {preview.data.sourceCourseName ? ` — ${preview.data.sourceCourseName}` : ""}
                  </div>

                  <div>
                    <span className="text-sm text-muted-foreground">
                      Language:
                    </span>{" "}
                    {preview.data.language || "—"}
                  </div>

                  <div>
                    <span className="text-sm text-muted-foreground">
                      Semester:
                    </span>{" "}
                    {preview.data.semester || "—"}
                  </div>

                  <div>
                    <span className="text-sm text-muted-foreground">
                      Major:
                    </span>{" "}
                    {preview.data.major || "—"}
                  </div>

                  <div>
                    <span className="text-sm text-muted-foreground">
                      Workload total:
                    </span>{" "}
                    {preview.data.workloadTotal || "—"}
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        <div className="flex justify-end gap-2 border-t p-5">
          <Button
            type="button"
            variant="outline"
            disabled={busy}
            onClick={onClose}
          >
            Hủy
          </Button>

          <Button
            type="button"
            disabled={!preview?.valid || busy}
            onClick={() => void confirm()}
          >
            {busy
              ? "Đang import..."
              : "Xác nhận import"}
          </Button>
        </div>
      </div>
    </div>
  )
}