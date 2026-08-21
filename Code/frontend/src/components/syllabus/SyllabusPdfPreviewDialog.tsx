import { useCallback, useEffect, useState } from "react"
import axios from "axios"
import {
  Download,
  ExternalLink,
  FileSearch2,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
  TriangleAlert,
} from "lucide-react"

import { syllabusPdfApi } from "@/api/syllabusPdfApi"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  syllabusId: number
  courseCode?: string
  courseName?: string
  status?: string
}

const errorMessage = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    if (error.response?.data instanceof Blob) {
      return "Unable to generate the PDF preview. Check the syllabus data and try again."
    }
    const data = error.response?.data as { message?: string } | undefined
    return data?.message ?? error.message
  }
  return error instanceof Error
    ? error.message
    : "Unable to generate the PDF preview."
}

export default function SyllabusPdfPreviewDialog({
  open,
  onOpenChange,
  syllabusId,
  courseCode,
  courseName,
  status,
}: Props) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  const loadPreview = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const blob = await syllabusPdfApi.fetchPreview(syllabusId)
      const nextUrl = window.URL.createObjectURL(blob)
      setPreviewUrl((current) => {
        if (current) window.URL.revokeObjectURL(current)
        return nextUrl
      })
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [syllabusId])

  useEffect(() => {
    if (!open) return
    // The request synchronizes the dialog with the server-generated PDF.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadPreview()
  }, [open, loadPreview, reloadToken])

  useEffect(() => {
    return () => {
      if (previewUrl) window.URL.revokeObjectURL(previewUrl)
    }
  }, [previewUrl])

  const handleDownload = async () => {
    try {
      setDownloading(true)
      setError(null)
      await syllabusPdfApi.download(
        syllabusId,
        `Syllabus_${courseCode || syllabusId}.pdf`
      )
    } catch (requestError) {
      setError(errorMessage(requestError))
    } finally {
      setDownloading(false)
    }
  }

  const handleOpenNewTab = () => {
    if (!previewUrl) return
    window.open(previewUrl, "_blank", "noopener,noreferrer")
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="h-[94vh] w-[97vw] max-w-[1500px] overflow-hidden p-0 gap-0 bg-slate-100"
        showCloseButton
      >
        <DialogHeader className="border-b border-slate-200 bg-white px-6 py-4 pr-14">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <div className="min-w-0">
              <DialogTitle className="flex items-center gap-2 text-lg font-bold text-slate-950">
                <span className="flex size-9 items-center justify-center rounded-xl bg-slate-950 text-white">
                  <FileSearch2 className="size-5" />
                </span>
                SCSE Syllabus Preview
              </DialogTitle>
              <DialogDescription className="mt-2 truncate text-sm">
                <span className="font-semibold text-slate-700">
                  {courseCode || `Syllabus #${syllabusId}`}
                </span>
                {courseName ? ` - ${courseName}` : ""}
              </DialogDescription>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
                <ShieldCheck className="size-3.5" />
                Data generated directly from the system
              </span>
              {status && (
                <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-600">
                  {status}
                </span>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={() => setReloadToken((value) => value + 1)}
                disabled={loading}
                className="gap-1.5"
              >
                <RefreshCw className={`size-4 ${loading ? "animate-spin" : ""}`} />
                Refresh
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleOpenNewTab}
                disabled={!previewUrl || loading}
                className="gap-1.5"
              >
                <ExternalLink className="size-4" />
                Open in New Tab
              </Button>
              <Button
                size="sm"
                onClick={handleDownload}
                disabled={downloading}
                className="gap-1.5 bg-slate-950 text-white hover:bg-slate-800"
              >
                {downloading
                  ? <LoaderCircle className="size-4 animate-spin" />
                  : <Download className="size-4" />}
                Download PDF
              </Button>
            </div>
          </div>
        </DialogHeader>

        <div className="relative flex-1 min-h-0 bg-slate-200/70 p-3 sm:p-5">
          {loading && (
            <div className="absolute inset-0 z-10 flex items-center justify-center bg-slate-100/85 backdrop-blur-sm">
              <div className="rounded-2xl border border-slate-200 bg-white px-8 py-6 text-center shadow-xl">
                <LoaderCircle className="mx-auto size-9 animate-spin text-slate-900" />
                <p className="mt-3 font-semibold text-slate-800">Generating PDF...</p>
                <p className="mt-1 text-xs text-slate-500">The system is compiling CLOs, PLOs, topics, and assessments.</p>
              </div>
            </div>
          )}

          {error ? (
            <div className="flex h-full items-center justify-center">
              <div className="max-w-xl rounded-2xl border border-rose-200 bg-white p-8 text-center shadow-sm">
                <TriangleAlert className="mx-auto size-10 text-rose-600" />
                <h3 className="mt-3 text-base font-bold text-slate-900">Unable to Display PDF</h3>
                <p className="mt-2 text-sm leading-6 text-slate-600">{error}</p>
                <Button
                  className="mt-5 gap-2"
                  onClick={() => setReloadToken((value) => value + 1)}
                >
                  <RefreshCw className="size-4" />
                  Try Again
                </Button>
              </div>
            </div>
          ) : previewUrl ? (
            <iframe
              title={`PDF preview ${courseCode || syllabusId}`}
              src={previewUrl}
              className="h-full w-full rounded-xl border border-slate-300 bg-white shadow-xl"
            />
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
