import { useEffect, useMemo, useState } from "react"
import { useMutation, useQuery } from "@tanstack/react-query"
import { useLocation, useNavigate, useParams } from "react-router-dom"
import {
  ArrowLeft,
  ArrowRightLeft,
  GitCompareArrows,
  Loader2,
  MinusCircle,
  PencilLine,
  PlusCircle,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { programApi } from "@/api/programApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

type FieldDiff = {
  oldValue?: unknown
  newValue?: unknown
}

type ProgramCourseDiff = {
  courseId: number
  courseCode: string
  courseName: string
  changes?: Record<string, FieldDiff>
}

type ProgramDiffResponse = {
  programCode?: string
  oldCohortYear?: string | number
  newCohortYear?: string | number
  courseDiff?: {
    added?: ProgramCourseDiff[]
    removed?: ProgramCourseDiff[]
    modified?: ProgramCourseDiff[]
  }
}

type DiffStatus = "added" | "removed" | "modified"

type DiffRow = ProgramCourseDiff & {
  status: DiffStatus
}

const FIELD_LABELS: Record<string, string> = {
  courseName: "Course Name",
  courseType: "Course Group",
  courseTypes: "Course Group",
  courseTypeId: "Course Group",
  semester: "Semester",
  semesterSuggest: "Recommended Semester",
  yearSuggest: "Recommended Year",
  isRequired: "Required",
  required: "Required",
  creditTheory: "Theory Credits",
  creditLab: "Lab Credits",
  totalCredits: "Total Credits",
  prerequisites: "Prerequisites",
}

function asText(value: unknown): string {
  if (value == null || value === "") {
    return "—"
  }

  if (typeof value === "boolean") {
    return value ? "Yes" : "No"
  }

  return String(value)
}

function formatFieldName(field: string): string {
  if (FIELD_LABELS[field]) {
    return FIELD_LABELS[field]
  }

  return field
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

function getErrorMessage(error: unknown): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: string
          }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return "Unable to compare the selected curriculum cohorts."
}

function baseProgramCode(value: unknown): string {
  const code = String(value ?? "").trim()
  if (!code) return "—"

  return (
    code
      .replace(/[-_\s]?20\d{2}$/i, "")
      .replace(/[-_\s]+$/g, "")
      .trim()
    || code
  )
}

function getRoleBase(pathname: string): "/admin" | "/dean" {
  return pathname.startsWith("/dean") ? "/dean" : "/admin"
}

export default function ProgramDiffPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const roleBase = getRoleBase(location.pathname)

  const programId = Number(id)
  const validProgramId =
    Number.isInteger(programId)
    && programId > 0

  const [oldCohortId, setOldCohortId] = useState("")
  const [newCohortId, setNewCohortId] = useState("")
  const [diff, setDiff] = useState<ProgramDiffResponse | null>(
    null
  )

  const {
    data: program,
    isLoading: isProgramLoading,
    isError: isProgramError,
  } = useQuery({
    queryKey: ["program", programId],
    queryFn: () => programApi.getById(programId),
    enabled: validProgramId,
  })

  const {
    data: cohorts = [],
    isLoading: isCohortsLoading,
    isError: isCohortsError,
  } = useQuery({
    queryKey: ["cohorts", "program", programId],
    queryFn: () => cohortApi.getByProgram(programId),
    enabled: validProgramId,
  })

  const cohortOptions = useMemo(
    () =>
      cohorts
        .filter((cohort) => cohort.isActive !== false)
        .slice()
        .sort((a, b) => {
          const yearCompare =
            Number(a.entryYear ?? 0)
            - Number(b.entryYear ?? 0)

          if (yearCompare !== 0) {
            return yearCompare
          }

          return a.name.localeCompare(
            b.name,
            "en",
            {
              numeric: true,
            }
          )
        }),
    [cohorts]
  )

  useEffect(() => {
    if (cohortOptions.length < 2) return
    if (oldCohortId || newCohortId) return

    const newest = cohortOptions[cohortOptions.length - 1]
    const previous = cohortOptions[cohortOptions.length - 2]

    setOldCohortId(String(previous.id))
    setNewCohortId(String(newest.id))
  }, [cohortOptions, oldCohortId, newCohortId])

  const oldCohort = cohortOptions.find(
    (cohort) => String(cohort.id) === oldCohortId
  )

  const newCohort = cohortOptions.find(
    (cohort) => String(cohort.id) === newCohortId
  )

  const compareMutation = useMutation({
    mutationFn: async () => {
      if (!oldCohortId || !newCohortId) {
        throw new Error(
          "Select both the source cohort and the target cohort."
        )
      }

      if (oldCohortId === newCohortId) {
        throw new Error(
          "Source cohort and target cohort must be different."
        )
      }

      const result = await programApi.getDiff(
        programId,
        Number(oldCohortId),
        Number(newCohortId)
      )

      return result as ProgramDiffResponse
    },

    onSuccess: (result) => {
      setDiff(result)
    },

    onError: () => {
      setDiff(null)
    },
  })

  const added = diff?.courseDiff?.added ?? []
  const removed = diff?.courseDiff?.removed ?? []
  const modified = diff?.courseDiff?.modified ?? []

  const rows = useMemo<DiffRow[]>(
    () => [
      ...added.map((item) => ({
        ...item,
        status: "added" as const,
      })),
      ...removed.map((item) => ({
        ...item,
        status: "removed" as const,
      })),
      ...modified.map((item) => ({
        ...item,
        status: "modified" as const,
      })),
    ],
    [added, removed, modified]
  )

  const totalChanges =
    added.length
    + removed.length
    + modified.length

  const clearResult = () => {
    setDiff(null)
    compareMutation.reset()
  }

  const handleOldCohortChange = (value: string) => {
    setOldCohortId(value)

    if (newCohortId === value) {
      setNewCohortId("")
    }

    clearResult()
  }

  const handleNewCohortChange = (value: string) => {
    setNewCohortId(value)

    if (oldCohortId === value) {
      setOldCohortId("")
    }

    clearResult()
  }

  const swapCohorts = () => {
    setOldCohortId(newCohortId)
    setNewCohortId(oldCohortId)
    clearResult()
  }

  if (!validProgramId) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
        Invalid curriculum program ID.
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-[1450px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-4 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <GitCompareArrows className="size-5" />
            </div>

            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                Curriculum Comparison
              </p>

              <h1 className="mt-1 text-[26px] font-bold tracking-[-0.4px] text-[#17343d]">
                Compare Curriculum Cohorts
              </h1>

              <p className="mt-1 text-sm text-[#687f89]">
                {isProgramLoading
                  ? "Loading curriculum program..."
                  : program
                    ? `${baseProgramCode(program.code)} · ${program.name}`
                    : "Compare course changes between two cohorts."}
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(`${roleBase}/programs`)}
          >
            <ArrowLeft className="size-4" />
            Back to Programs
          </Button>
        </div>
      </section>

      {(isProgramError || isCohortsError) && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          Unable to load the curriculum program or cohort data.
        </div>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-4">
          <h2 className="text-base font-semibold text-slate-900">
            Select Cohorts
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            Choose the older/source cohort and the newer/target cohort to compare.
          </p>
        </div>

        <div className="grid gap-4 lg:grid-cols-[1fr_auto_1fr_auto] lg:items-end">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              From Cohort
            </label>

            <Select
              value={oldCohortId}
              onValueChange={handleOldCohortChange}
              disabled={
                isCohortsLoading
                || cohortOptions.length === 0
              }
            >
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="Select source cohort..." />
              </SelectTrigger>

              <SelectContent>
                {cohortOptions.map((cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(cohort.id)}
                    disabled={String(cohort.id) === newCohortId}
                  >
                    {cohort.name} · {cohort.entryYear}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            variant="outline"
            size="icon"
            className="hidden lg:inline-flex"
            title="Swap cohorts"
            aria-label="Swap cohorts"
            disabled={!oldCohortId && !newCohortId}
            onClick={swapCohorts}
          >
            <ArrowRightLeft className="size-4" />
          </Button>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              To Cohort
            </label>

            <Select
              value={newCohortId}
              onValueChange={handleNewCohortChange}
              disabled={
                isCohortsLoading
                || cohortOptions.length === 0
              }
            >
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="Select target cohort..." />
              </SelectTrigger>

              <SelectContent>
                {cohortOptions.map((cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(cohort.id)}
                    disabled={String(cohort.id) === oldCohortId}
                  >
                    {cohort.name} · {cohort.entryYear}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            className="h-10 bg-[#007d84] px-5 text-white hover:bg-[#006d73]"
            disabled={
              !oldCohortId
              || !newCohortId
              || oldCohortId === newCohortId
              || compareMutation.isPending
            }
            onClick={() => compareMutation.mutate()}
          >
            {compareMutation.isPending ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Comparing...
              </>
            ) : (
              <>
                <GitCompareArrows className="size-4" />
                Compare
              </>
            )}
          </Button>
        </div>

        {cohortOptions.length < 2 && !isCohortsLoading && (
          <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            At least two active cohorts are required to compare curriculum versions.
          </div>
        )}

        {compareMutation.isError && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {getErrorMessage(compareMutation.error)}
          </div>
        )}
      </section>

      {!diff && !compareMutation.isPending && (
        <section className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-10 text-center">
          <GitCompareArrows className="mx-auto size-8 text-slate-300" />
          <p className="mt-3 font-medium text-slate-700">
            Select two cohorts to compare.
          </p>
          <p className="mt-1 text-sm text-slate-500">
            The result will show added, removed, and modified courses.
          </p>
        </section>
      )}

      {diff && (
        <>
          <section className="grid gap-4 md:grid-cols-4">
            <SummaryCard
              label="Total Changes"
              value={totalChanges}
              tone="neutral"
            />

            <SummaryCard
              label="Added Courses"
              value={added.length}
              tone="added"
            />

            <SummaryCard
              label="Removed Courses"
              value={removed.length}
              tone="removed"
            />

            <SummaryCard
              label="Modified Courses"
              value={modified.length}
              tone="modified"
            />
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="flex flex-col gap-2 border-b border-slate-100 px-5 py-4 md:flex-row md:items-center md:justify-between">
              <div>
                <h2 className="font-semibold text-slate-900">
                  Comparison Results
                </h2>

                <p className="mt-1 text-xs text-slate-500">
                  {oldCohort
                    ? `${oldCohort.name} · ${oldCohort.entryYear}`
                    : diff.oldCohortYear ?? "Source cohort"}
                  {" → "}
                  {newCohort
                    ? `${newCohort.name} · ${newCohort.entryYear}`
                    : diff.newCohortYear ?? "Target cohort"}
                </p>
              </div>

              <Badge
                variant="outline"
                className="w-fit border-[#b9dfe1] bg-[#f2fafa] text-[#007d84]"
              >
                {baseProgramCode(program?.code ?? diff.programCode ?? "Program")}
              </Badge>
            </div>

            {rows.length === 0 ? (
              <div className="px-6 py-12 text-center">
                <p className="font-medium text-emerald-700">
                  No curriculum differences found.
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  The selected cohorts contain the same curriculum configuration.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-slate-50/70">
                      <TableHead className="min-w-[120px]">
                        Course Code
                      </TableHead>
                      <TableHead className="min-w-[260px]">
                        Course Name
                      </TableHead>
                      <TableHead className="min-w-[360px]">
                        Changes
                      </TableHead>
                      <TableHead className="w-[130px] text-right">
                        Status
                      </TableHead>
                    </TableRow>
                  </TableHeader>

                  <TableBody>
                    {rows.map((item) => (
                      <TableRow
                        key={`${item.status}-${item.courseId}`}
                      >
                        <TableCell className="font-mono font-semibold text-slate-800">
                          {item.courseCode}
                        </TableCell>

                        <TableCell className="font-medium text-slate-800">
                          {item.courseName}
                        </TableCell>

                        <TableCell>
                          {item.status === "modified" ? (
                            <FieldChanges changes={item.changes} />
                          ) : (
                            <span className="text-sm text-slate-400">
                              —
                            </span>
                          )}
                        </TableCell>

                        <TableCell className="text-right">
                          <StatusBadge status={item.status} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function SummaryCard({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: "neutral" | "added" | "removed" | "modified"
}) {
  const styles = {
    neutral:
      "border-slate-200 bg-white text-slate-900",
    added:
      "border-emerald-200 bg-emerald-50/60 text-emerald-800",
    removed:
      "border-rose-200 bg-rose-50/60 text-rose-800",
    modified:
      "border-amber-200 bg-amber-50/60 text-amber-800",
  }

  return (
    <div
      className={`rounded-xl border p-4 shadow-sm ${styles[tone]}`}
    >
      <p className="text-[11px] font-semibold uppercase tracking-wide opacity-70">
        {label}
      </p>
      <p className="mt-2 text-2xl font-bold">
        {value}
      </p>
    </div>
  )
}

function FieldChanges({
  changes,
}: {
  changes?: Record<string, FieldDiff>
}) {
  const entries = Object.entries(changes ?? {})

  if (entries.length === 0) {
    return (
      <span className="text-sm text-slate-500">
        Updated curriculum configuration
      </span>
    )
  }

  return (
    <div className="space-y-2">
      {entries.map(([field, change]) => (
        <div
          key={field}
          className="flex flex-wrap items-center gap-2 text-xs"
        >
          <span className="min-w-[120px] font-semibold text-slate-600">
            {formatFieldName(field)}
          </span>

          <span className="rounded bg-rose-50 px-2 py-1 text-rose-700 line-through">
            {asText(change.oldValue)}
          </span>

          <span className="text-slate-400">
            →
          </span>

          <span className="rounded bg-emerald-50 px-2 py-1 font-semibold text-emerald-700">
            {asText(change.newValue)}
          </span>
        </div>
      ))}
    </div>
  )
}

function StatusBadge({
  status,
}: {
  status: DiffStatus
}) {
  if (status === "added") {
    return (
      <Badge
        variant="outline"
        className="gap-1 border-emerald-200 bg-emerald-50 text-emerald-700"
      >
        <PlusCircle className="size-3.5" />
        Added
      </Badge>
    )
  }

  if (status === "removed") {
    return (
      <Badge
        variant="outline"
        className="gap-1 border-rose-200 bg-rose-50 text-rose-700"
      >
        <MinusCircle className="size-3.5" />
        Removed
      </Badge>
    )
  }

  return (
    <Badge
      variant="outline"
      className="gap-1 border-amber-200 bg-amber-50 text-amber-700"
    >
      <PencilLine className="size-3.5" />
      Modified
    </Badge>
  )
}
