import { isAxiosError } from "axios"
import { useMemo, useState } from "react"
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
import {
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"
type Props = {
  source: Syllabus | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onCloned: (syllabus: Syllabus) => void
}

export default function CloneSyllabusDialog({
  source,
  open,
  onOpenChange,
  onCloned,
}: Props) {
  const isInstructor = useAuthStore((state) => state.user?.role === "INSTRUCTOR")
const normalizeSemesterLabel = (
  value: unknown,
) => {
  const text = String(value ?? "").trim()

  if (!text) return ""

  const match = text.match(
    /^(?:(?:semester|hk)\s*)?([1-8])$/i,
  )

  return match
    ? `Semester ${match[1]}`
    : text
}
  const cloneMutation = useCloneSyllabus()
  const [assignmentId, setAssignmentId] = useState<number | null>(null)
  const [semester, setSemester] =
  useState("")
const [cohortId, setCohortId] =
  useState("")
const [changeSummary, setChangeSummary] = useState("")
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
const targetCohorts =
  useMemo(() => {
    if (!source) {
      return []
    }

    return cohorts
      .filter(
        (cohort) =>
          cohort.isActive
          && (
            !source.programId
            || cohort.programId
              === source.programId
          ),
      )
      .slice()
      .sort(
        (a, b) =>
          a.entryYear
          - b.entryYear,
      )
  }, [
    cohorts,
    source,
  ])

const selectedCohort =
  useMemo(
    () =>
      targetCohorts.find(
        (cohort) =>
          String(cohort.id)
          === cohortId,
      ),
    [
      cohortId,
      targetCohorts,
    ],
  )
  const targetAssignments = useMemo(() => {
    if (!source) return []

    return assignments.filter(
      (assignment) =>
        assignment.isActive
        && assignment.readyForSyllabusCreation
        && assignment.courseId === source.courseId
    )
  }, [assignments, source])

  const [formContext, setFormContext] = useState<{ open: boolean; source: typeof source }>({ open: false, source: null })
  if (formContext.open !== open || formContext.source !== source) {
    setFormContext({ open, source })
    if (open && source) {

    setSemester(
  normalizeSemesterLabel(
    source.semester,
  ),
)

setCohortId(
  source.cohortId
    ? String(source.cohortId)
    : "",
)
    setChangeSummary(
  `Clone from ${source.versionLabel} (${source.academicYear} - ${normalizeSemesterLabel(
    source.semester,
  )})`,
)
    setAssignmentId(null)
    }
  }

  const effectiveAssignmentId = assignmentId ?? (isInstructor ? targetAssignments[0]?.id ?? null : null)

  const handleClone = () => {
    if (!source) return

    if (isInstructor && effectiveAssignmentId === null) {
      alert("Please select the target-semester teaching assignment.")
      return
    }

    if (
  !isInstructor
  && (
    !selectedCohort
    || !semester
  )
) {
  alert(
    "Please select the target academic year and semester.",
  )
  return
}

    cloneMutation.mutate(
      {
        id: source.id,
        request: isInstructor
          ? {
              classSectionId: effectiveAssignmentId ?? undefined,
              changeSummary: changeSummary.trim(),
            }
          : {
    cohortId: selectedCohort?.id,
    academicYear: selectedCohort?.name,
    semester,
    changeSummary: changeSummary.trim(),
  },
      },
      {
        onSuccess: (cloned) => {
          onOpenChange(false)
          onCloned(cloned)
        },
        onError: (error: unknown) => {
          alert(
            (isAxiosError<{ message?: string }>(error) ? error.response?.data?.message : undefined)
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
              Source: {source.versionLabel}
{" · "}
{source.cohortName || source.academicYear}
{" · "}
{normalizeSemesterLabel(source.semester)}
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
                  value={effectiveAssignmentId ? String(effectiveAssignmentId) : undefined}
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
                        Semester {assignment.semester}
{" — "}
{assignment.cohortName || assignment.academicYear}
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
                        {cohort.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-3">
  <div className="grid grid-cols-2 gap-3">

    <div className="space-y-2">
      <Label>Target Academic Year</Label>

      <Select
        value={cohortId}
        onValueChange={setCohortId}
      >
        <SelectTrigger>
          <SelectValue placeholder="Select academic year" />
        </SelectTrigger>

        <SelectContent>
          {targetCohorts.map((cohort) => (
            <SelectItem
              key={cohort.id}
              value={String(cohort.id)}
            >
              {cohort.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>

    <div className="space-y-2">
      <Label>Target Semester</Label>

      <Select
        value={semester}
        onValueChange={setSemester}
      >
        <SelectTrigger>
          <SelectValue placeholder="Select semester" />
        </SelectTrigger>

        <SelectContent>
          {SYLLABUS_SEMESTER_OPTIONS.map(
            (value) => (
              <SelectItem
                key={value}
                value={`Semester ${value}`}
              >
                Semester {value}
              </SelectItem>
            ),
          )}
        </SelectContent>
      </Select>
    </div>

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
              || (
  !isInstructor
  && (
    !selectedCohort
    || !semester
  )
)
            }
          >
            {cloneMutation.isPending ? "Cloning..." : "Clone, Import and Edit"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
