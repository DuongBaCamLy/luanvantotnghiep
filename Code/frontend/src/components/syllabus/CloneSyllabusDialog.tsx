import { useEffect, useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"

import { getMyActiveAssignments } from "@/api/classSectionApi"
import { cohortApi } from "@/api/cohortApi"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useCloneSyllabus } from "@/hooks/useCloneSyllabus"
import { useAuthStore } from "@/store/authStore"
import type { Syllabus } from "@/types/syllabus"

type Props = {
  source: Syllabus | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onCloned: (syllabus: Syllabus) => void
}

const cohortLabel = (cohort: { name?: string | null; entryYear?: number | null }) => {
  const explicit = cohort.name?.match(/\bK\s*\d{2}\b/i)?.[0]
  if (explicit) return explicit.replace(/\s+/g, "").toUpperCase()

  return cohort.entryYear
    ? `K${String(cohort.entryYear).slice(-2)}`
    : cohort.name || "Unknown cohort"
}

export default function CloneSyllabusDialog({
  source,
  open,
  onOpenChange,
  onCloned,
}: Props) {
  const user = useAuthStore((state) => state.user)
  const role = String(user?.role ?? "")
    .replace(/^ROLE_/i, "")
    .toUpperCase()
  const isInstructor = role === "INSTRUCTOR"

  const cloneMutation = useCloneSyllabus()
  const [assignmentId, setAssignmentId] = useState<number | null>(null)
  const [academicYear, setAcademicYear] = useState("")
  const [semester, setSemester] = useState("")
  const [changeSummary, setChangeSummary] = useState("")
  const [cohortId, setCohortId] = useState("")

  const { data: assignments = [], isLoading } = useQuery({
    queryKey: ["my-active-assignments"],
    queryFn: getMyActiveAssignments,
    enabled: open && isInstructor,
  })

  const { data: cohorts = [] } = useQuery({
    queryKey: ["cohorts", "syllabus-clone"],
    queryFn: cohortApi.getAll,
    enabled: open && !isInstructor,
  })

  const targetAssignments = useMemo(() => {
    if (!source) return []

    return assignments.filter(
      (assignment) =>
        assignment.isActive
        && assignment.readyForSyllabusCreation
        && assignment.courseId === source.courseId
    )
  }, [assignments, source])

  useEffect(() => {
    if (!open || !source) return

    setAcademicYear(source.academicYear ?? "")
    setSemester(source.semester ?? "")
    setCohortId(source.cohortId ? String(source.cohortId) : "")
    setChangeSummary(
      `Clone from ${source.versionLabel} (${source.academicYear} - ${source.semester ?? ""})`
    )
    setAssignmentId(null)
  }, [open, source])

  useEffect(() => {
    if (!open || !isInstructor || assignmentId !== null) return
    if (targetAssignments.length > 0) {
      setAssignmentId(targetAssignments[0].id)
    }
  }, [assignmentId, isInstructor, open, targetAssignments])

  const handleClone = () => {
    if (!source) return

    if (isInstructor && assignmentId === null) {
      alert("Please select the target-semester teaching assignment.")
      return
    }

    if (!isInstructor && (!academicYear.trim() || !semester.trim())) {
      alert("Please enter the target academic year and semester.")
      return
    }

    cloneMutation.mutate(
      {
        id: source.id,
        request: isInstructor
          ? {
              classSectionId: assignmentId ?? undefined,
              changeSummary: changeSummary.trim(),
            }
          : {
              academicYear: academicYear.trim(),
              semester: semester.trim(),
              cohortId: cohortId ? Number(cohortId) : undefined,
              changeSummary: changeSummary.trim(),
            },
      },
      {
        onSuccess: (cloned) => {
          onOpenChange(false)
          onCloned(cloned)
        },
        onError: (error: any) => {
          alert(
            error?.response?.data?.message
            || "Unable to clone the syllabus."
          )
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Clone Syllabus to a New Cohort</DialogTitle>
          <DialogDescription>
            All General Information, CLOs, CLO–PLO mappings, Topics, Topic–CLO mappings,
            Assessments, Assessment–CLO mappings, and the Reading List will be copied.
          </DialogDescription>
        </DialogHeader>

        {source && (
          <div className="rounded-lg border bg-slate-50 p-3 text-sm">
            <div className="font-semibold">
              {source.courseCode} — {source.courseName}
            </div>
            <div className="text-slate-500">
              Source: {source.versionLabel} · {source.academicYear} · {source.semester}
            </div>
          </div>
        )}

        <div className="space-y-4">
          {isInstructor ? (
            <div className="space-y-2">
              <Label>Target-Semester Assignment</Label>
              {isLoading ? (
                <div className="text-sm text-slate-500">Loading assignments...</div>
              ) : targetAssignments.length === 0 ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                  No active assignment for the same course is available without an attached syllabus.
                  An administrator must create the ClassSection for the new semester first.
                </div>
              ) : (
                <Select
                  value={assignmentId ? String(assignmentId) : undefined}
                  onValueChange={(value) => setAssignmentId(Number(value))}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select target assignment" />
                  </SelectTrigger>
                  <SelectContent>
                    {targetAssignments.map((assignment) => (
                      <SelectItem
                        key={assignment.id}
                        value={String(assignment.id)}
                      >
                        HK{assignment.semester} — {assignment.academicYear}
                        {` — Group ${assignment.groupNumber}`}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              <div className="space-y-2">
                <Label>Target Cohort</Label>
                <Select value={cohortId} onValueChange={setCohortId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select target cohort" />
                  </SelectTrigger>
                  <SelectContent>
                    {cohorts.map((cohort) => (
                      <SelectItem key={cohort.id} value={String(cohort.id)}>
                        {cohortLabel(cohort)} · {cohort.name} — Entry {cohort.entryYear}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="clone-academic-year">Target Academic Year</Label>
                  <Input
                    id="clone-academic-year"
                    value={academicYear}
                    onChange={(event) => setAcademicYear(event.target.value)}
                    placeholder="2026-2027"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="clone-semester">Target Semester</Label>
                  <Input
                    id="clone-semester"
                    value={semester}
                    onChange={(event) => setSemester(event.target.value)}
                    placeholder="HK2"
                  />
                </div>
              </div>
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="clone-change-summary">Change Summary</Label>
            <Input
              id="clone-change-summary"
              value={changeSummary}
              onChange={(event) => setChangeSummary(event.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
          >
            Cancel
          </Button>
          <Button
            type="button"
            onClick={handleClone}
            disabled={
              cloneMutation.isPending
              || !source
              || (isInstructor && targetAssignments.length === 0)
              || (!isInstructor && !cohortId)
            }
          >
            {cloneMutation.isPending ? "Cloning..." : "Clone, Import and Edit"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
