import { useMemo, useState } from "react"
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  Plus,
  Trash2,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  useAssessments,
  useCreateAssessment,
  useDeleteAssessment,
} from "@/hooks/useAssessments"
import {
  ASSESSMENT_TYPES,
} from "@/types/assessment"
import type {
  AssessmentComponent,
  AssessmentType,
  CreateAssessmentComponentRequest,
} from "@/types/assessment"

interface Props {
  syllabusId: number
  readOnly?: boolean
}

const emptyForm =
  (
    remainingWeight = 0,
  ):
    Omit<
      CreateAssessmentComponentRequest,
      "syllabusId"
    > => ({
      name: "",
      nameVn: "",
      assessmentType: "QUIZ",
      weightPercent:
        Math.max(
          0,
          remainingWeight,
        ),
      minScore: 0,
      maxScore: 10,
    })

const numberValue = (
  value: unknown,
) => {
  const parsed =
    Number(value)

  return Number.isFinite(parsed)
    ? parsed
    : 0
}

export default function Section6Assessment({
  syllabusId,
  readOnly = false,
}: Props) {
  const {
    data:
      assessments = [],
    isLoading,
  } =
    useAssessments(
      syllabusId,
    )

  const createMutation =
    useCreateAssessment()

  const deleteMutation =
    useDeleteAssessment(
      syllabusId,
    )

  const [
    showForm,
    setShowForm,
  ] = useState(false)

  const [
    form,
    setForm,
  ] =
    useState(
      emptyForm(),
    )

  const [
    errors,
    setErrors,
  ] =
    useState<
      Record<
        string,
        string
      >
    >({})

  const sortedAssessments =
    useMemo(
      () =>
        [...assessments].sort(
          (
            left,
            right,
          ) =>
            Number(
              left.orderIndex
              ?? 0,
            )
            - Number(
              right.orderIndex
              ?? 0,
            ),
        ),
      [assessments],
    )

  const totalWeight =
    sortedAssessments.reduce(
      (
        sum,
        assessment,
      ) =>
        sum
        + numberValue(
          assessment.weightPercent,
        ),
      0,
    )

  const remainingWeight =
    Math.max(
      0,
      100 - totalWeight,
    )

  const isTotalValid =
    Math.abs(
      totalWeight - 100,
    ) < 0.01

  const hasOverweight =
    totalWeight > 100.01

  const invalidScoreRanges =
    sortedAssessments.filter(
      (assessment) =>
        numberValue(
          assessment.maxScore,
        )
        <= numberValue(
          assessment.minScore,
        ),
    )

  const nextOrder =
    sortedAssessments.reduce(
      (
        max,
        assessment,
      ) =>
        Math.max(
          max,
          Number(
            assessment.orderIndex
            ?? 0,
          ),
        ),
      0,
    )
    + 1

  const openForm = () => {
    setForm(
      emptyForm(
        remainingWeight,
      ),
    )
    setErrors({})
    setShowForm(true)
  }

  const validate = () => {
    const nextErrors:
      Record<
        string,
        string
      > = {}

    if (!form.name.trim()) {
      nextErrors.name =
        "Component name is required."
    }

    if (
      !form.assessmentType
    ) {
      nextErrors.assessmentType =
        "Assessment type is required."
    }

    const weight =
      numberValue(
        form.weightPercent,
      )

    if (weight <= 0) {
      nextErrors.weightPercent =
        "Weight must be greater than 0."
    } else if (
      totalWeight
        + weight
      > 100.01
    ) {
      nextErrors.weightPercent =
        `Only ${remainingWeight}% remains before reaching 100%.`
    }

    if (
      numberValue(
        form.maxScore,
      )
      <= numberValue(
        form.minScore,
      )
    ) {
      nextErrors.scoreRange =
        "Maximum score must be greater than minimum score."
    }

    setErrors(nextErrors)

    return (
      Object.keys(
        nextErrors,
      ).length === 0
    )
  }

  const handleAdd = () => {
    if (!validate()) {
      return
    }

    createMutation.mutate(
      {
        ...form,
        syllabusId,
        name:
          form.name.trim(),
        nameVn:
          form.nameVn
            ?.trim()
          || undefined,
        orderIndex:
          nextOrder,
      },
      {
        onSuccess: () => {
          setShowForm(false)
          setForm(
            emptyForm(),
          )
          setErrors({})
        },
      },
    )
  }

  const handleDelete = (
    assessment:
      AssessmentComponent,
  ) => {
    const confirmed =
      window.confirm(
        `Delete assessment component "${assessment.name}"?`,
      )

    if (!confirmed) {
      return
    }

    deleteMutation.mutate(
      assessment.id,
    )
  }

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center gap-2 text-slate-400">
        <Loader2 className="size-5 animate-spin" />
        Loading assessment plan...
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <Card
        className={`border shadow-sm ${
          isTotalValid
            ? "border-emerald-200 bg-emerald-50/40"
            : hasOverweight
              ? "border-rose-200 bg-rose-50/40"
              : "border-amber-200 bg-amber-50/40"
        }`}
      >
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            {isTotalValid ? (
              <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-emerald-600" />
            ) : hasOverweight ? (
              <AlertTriangle className="mt-0.5 size-5 shrink-0 text-rose-600" />
            ) : (
              <AlertCircle className="mt-0.5 size-5 shrink-0 text-amber-600" />
            )}

            <div>
              <p
                className={`text-sm font-semibold ${
                  isTotalValid
                    ? "text-emerald-800"
                    : hasOverweight
                      ? "text-rose-800"
                      : "text-amber-800"
                }`}
              >
                Total Assessment Weight: {totalWeight}% / 100%
              </p>

              <p
                className={`mt-1 text-xs leading-5 ${
                  isTotalValid
                    ? "text-emerald-700"
                    : hasOverweight
                      ? "text-rose-700"
                      : "text-amber-700"
                }`}
              >
                {isTotalValid
                  ? "Assessment weighting is ready for submission."
                  : hasOverweight
                    ? "The current assessment plan exceeds 100%. Remove or correct components before submission."
                    : `${remainingWeight}% remains. Submission requires the assessment components to total exactly 100%.`}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {sortedAssessments.length === 0 && (
        <Card className="border border-rose-200 bg-rose-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-rose-600" />
              <p className="text-xs leading-5 text-rose-800">
                Assessment Plan is required before submission. Add at least one assessment component.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {invalidScoreRanges.length > 0 && (
        <Card className="border border-rose-200 bg-rose-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-rose-600" />
              <p className="text-xs leading-5 text-rose-800">
                Invalid score range in:{" "}
                {invalidScoreRanges
                  .map(
                    (item) =>
                      item.name,
                  )
                  .join(", ")}.
                Maximum score must be greater than minimum score.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-3 sm:grid-cols-3">
        <Metric
          label="Components"
          value={sortedAssessments.length}
        />
        <Metric
          label="Allocated"
          value={`${totalWeight}%`}
        />
        <Metric
          label="Remaining"
          value={`${remainingWeight}%`}
        />
      </div>

      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between gap-4 border-b border-slate-100 pb-3">
          <CardTitle className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <CheckCircle2 className="size-4 text-primary" />
            Assessment Components
          </CardTitle>

          {!readOnly
            && totalWeight < 100 && (
            <Button
              type="button"
              size="sm"
              onClick={openForm}
              disabled={showForm}
              className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
            >
              <Plus className="size-3.5" />
              Add Component
            </Button>
          )}
        </CardHeader>

        <CardContent className="p-0">
          {sortedAssessments.length === 0
            && !showForm ? (
            <div className="px-6 py-12 text-center">
              <CheckCircle2 className="mx-auto mb-3 size-10 text-slate-200" />
              <p className="text-sm font-medium text-slate-500">
                No assessment components defined
              </p>
              <p className="mt-1 text-xs text-slate-400">
                Build the assessment plan until the total weight reaches 100%.
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {sortedAssessments.map(
                (
                  assessment:
                    AssessmentComponent,
                  index,
                ) => (
                  <div
                    key={assessment.id}
                    className="group flex items-center gap-4 p-4 transition hover:bg-slate-50/50"
                  >
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-xs font-bold text-slate-500">
                      {index + 1}
                    </span>

                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-semibold text-slate-800">
                          {assessment.name}
                        </p>

                        <Badge
                          variant="outline"
                          className="border-slate-200 bg-slate-50 text-slate-600"
                        >
                          {ASSESSMENT_TYPES.find(
                            (type) =>
                              type.value
                              === assessment.assessmentType,
                          )?.label
                          ?? assessment.assessmentType}
                        </Badge>
                      </div>

                      {assessment.nameVn && (
                        <p className="mt-1 text-xs text-slate-500">
                          {assessment.nameVn}
                        </p>
                      )}

                      <p className="mt-2 text-xs text-slate-500">
                        Score range: {assessment.minScore} – {assessment.maxScore}
                      </p>
                    </div>

                    <div className="shrink-0 text-right">
                      <p className="text-xl font-bold text-primary">
                        {assessment.weightPercent}%
                      </p>
                      <p className="text-[10px] uppercase tracking-wide text-slate-400">
                        Weight
                      </p>
                    </div>

                    {!readOnly && (
                      <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        aria-label={`Delete ${assessment.name}`}
                        onClick={() =>
                          handleDelete(
                            assessment,
                          )
                        }
                        disabled={deleteMutation.isPending}
                        className="size-8 shrink-0 p-0 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    )}
                  </div>
                ),
              )}
            </div>
          )}

          {showForm
            && !readOnly && (
            <div className="border-t border-slate-200 bg-slate-50/70 p-4">
              <p className="mb-3 text-xs font-bold uppercase tracking-wider text-slate-600">
                Add Assessment Component
              </p>

              <div className="grid gap-3 md:grid-cols-2">
                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Component Name (English) *
                  </Label>
                  <Input
                    className="h-9 bg-white text-sm"
                    value={form.name}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        name:
                          event.target.value,
                      })
                    }
                    placeholder="Midterm Exam"
                  />
                  <FieldError message={errors.name} />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Component Name (Vietnamese)
                  </Label>
                  <Input
                    className="h-9 bg-white text-sm"
                    value={form.nameVn ?? ""}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        nameVn:
                          event.target.value,
                      })
                    }
                  />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Assessment Type *
                  </Label>
                  <Select
                    value={form.assessmentType ?? ""}
                    onValueChange={(value) =>
                      setForm({
                        ...form,
                        assessmentType:
                          value as AssessmentType,
                      })
                    }
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ASSESSMENT_TYPES.map(
                        (type) => (
                          <SelectItem
                            key={type.value}
                            value={type.value}
                          >
                            {type.label}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                  <FieldError message={errors.assessmentType} />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs font-semibold text-slate-600">
                    Weight (%) *
                  </Label>
                  <Input
                    type="number"
                    min={0.01}
                    max={remainingWeight}
                    step="0.01"
                    className="h-9 bg-white text-sm font-bold text-primary"
                    value={form.weightPercent}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        weightPercent:
                          Number(event.target.value),
                      })
                    }
                  />
                  <FieldError message={errors.weightPercent} />
                </div>

                <div className="grid grid-cols-2 gap-3 md:col-span-2">
                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Minimum Score
                    </Label>
                    <Input
                      type="number"
                      className="h-9 bg-white text-sm"
                      value={form.minScore}
                      onChange={(event) =>
                        setForm({
                          ...form,
                          minScore:
                            Number(event.target.value),
                        })
                      }
                    />
                  </div>

                  <div className="space-y-1">
                    <Label className="text-xs font-semibold text-slate-600">
                      Maximum Score
                    </Label>
                    <Input
                      type="number"
                      className="h-9 bg-white text-sm"
                      value={form.maxScore}
                      onChange={(event) =>
                        setForm({
                          ...form,
                          maxScore:
                            Number(event.target.value),
                        })
                      }
                    />
                  </div>

                  <div className="col-span-2">
                    <FieldError message={errors.scoreRange} />
                  </div>
                </div>
              </div>

              <div className="mt-4 flex gap-2">
                <Button
                  type="button"
                  size="sm"
                  onClick={handleAdd}
                  disabled={createMutation.isPending}
                  className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
                >
                  {createMutation.isPending && (
                    <Loader2 className="size-3.5 animate-spin" />
                  )}
                  Save Component
                </Button>

                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setShowForm(false)
                    setErrors({})
                  }}
                  disabled={createMutation.isPending}
                  className="h-8 text-xs"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="border border-blue-100 bg-blue-50/30 shadow-sm">
        <CardContent className="p-4">
          <p className="text-xs leading-5 text-blue-700">
            The official syllabus PDF supports CLO coverage for assessment components. This screen preserves the current component API and weighting rules; detailed Assessment–CLO contribution data, when configured by the existing mapping module/API, is included in the generated PDF.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

function Metric({
  label,
  value,
}: {
  label: string
  value:
    string
    | number
}) {
  return (
    <Card className="border border-slate-200 shadow-sm">
      <CardContent className="pb-3 pt-3 text-center">
        <p className="text-xl font-bold text-slate-800">
          {value}
        </p>
        <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          {label}
        </p>
      </CardContent>
    </Card>
  )
}

function FieldError({
  message,
}: {
  message?: string
}) {
  if (!message) {
    return null
  }

  return (
    <p className="text-xs font-medium text-rose-500">
      {message}
    </p>
  )
}