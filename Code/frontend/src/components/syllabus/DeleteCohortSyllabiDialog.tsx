import { useState } from "react"
import { AlertTriangle, LoaderCircle, Trash2 } from "lucide-react"

import { syllabusApi } from "@/api/syllabusApi"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"

type DeleteCohortSyllabiDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void

  programId: number
  programLabel: string

  cohortId: number
  cohortName: string

  onDeleted: (deletedCount: number) => void
}

export default function DeleteCohortSyllabiDialog({
  open,
  onOpenChange,
  programId,
  programLabel,
  cohortId,
  cohortName,
  onDeleted,
}: DeleteCohortSyllabiDialogProps) {
  const [confirmation, setConfirmation] =
    useState("")

  const [running, setRunning] =
    useState(false)

  const [error, setError] =
    useState("")

 
const resetDialogState = () => {
  setConfirmation("")
  setError("")
}
  const confirmationMatches =
    confirmation.trim() === cohortName.trim()

  const handleDelete = async () => {
    if (
      running
      || !confirmationMatches
    ) {
      return
    }

    try {
      setRunning(true)
      setError("")

      const deletedCount =
        await syllabusApi.deleteCohortForReimport(
          programId,
          cohortId,
          confirmation.trim(),
        )

      resetDialogState()
onOpenChange(false)
onDeleted(deletedCount)
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Unable to reset this cohort.",
      )
    } finally {
      setRunning(false)
    }
  }

  return (
    <Dialog
  open={open}
  onOpenChange={(nextOpen) => {
    if (running) {
      return
    }

    if (!nextOpen) {
      resetDialogState()
    }

    onOpenChange(nextOpen)
  }}
>
      <DialogContent className="border-rose-200 bg-white sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-rose-700">
            <Trash2 className="size-5" />
            Delete {cohortName} syllabi?
          </DialogTitle>

          <DialogDescription className="leading-6">
            This maintenance action deletes syllabus data only for
            the selected Program and Cohort so the cohort can be
            imported again.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="rounded-xl border border-slate-200 bg-slate-50">
            <div className="grid grid-cols-[110px_1fr] border-b border-slate-200 px-4 py-3">
              <span className="text-xs font-semibold text-slate-500">
                Program
              </span>

              <span className="text-sm font-semibold text-slate-900">
                {programLabel || "Selected program"}
              </span>
            </div>

            <div className="grid grid-cols-[110px_1fr] px-4 py-3">
              <span className="text-xs font-semibold text-slate-500">
                Cohort
              </span>

              <span className="font-mono text-sm font-bold text-slate-900">
                {cohortName}
              </span>
            </div>
          </div>

          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
            <div className="flex gap-3">
              <AlertTriangle className="mt-0.5 size-5 shrink-0 text-amber-600" />

              <div className="text-sm leading-6 text-amber-900">
                <p className="font-semibold">
                  Other cohorts are not affected.
                </p>

                <p className="mt-1">
                  Program, Course, Cohort and shared source documents
                  are retained. This action cannot be undone.
                </p>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">
              Type{" "}
              <span className="font-mono font-bold">
                {cohortName}
              </span>{" "}
              to confirm
            </label>

            <Input
              value={confirmation}
              disabled={running}
              autoComplete="off"
              placeholder={cohortName}
              onChange={(event) =>
                setConfirmation(event.target.value)
              }
            />
          </div>

          {error && (
            <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
              {error}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={running}
            onClick={() => {
  resetDialogState()
  onOpenChange(false)
}}
          >
            Cancel
          </Button>

          <Button
            type="button"
            disabled={
              running
              || !confirmationMatches
            }
            className="bg-rose-600 text-white hover:bg-rose-700"
            onClick={handleDelete}
          >
            {running ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <Trash2 className="size-4" />
            )}

            {running
              ? "Deleting..."
              : `Delete ${cohortName}`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}