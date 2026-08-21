import { useMemo, useState } from "react"
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  ListChecks,
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
  useClos,
  useCreateClo,
  useDeleteClo,
} from "@/hooks/useClos"
import {
  BLOOM_LEVELS,
  COMPETENCY_LEVELS,
} from "@/types/clo"
import type { Clo } from "@/types/clo"

interface Props {
  syllabusId: number
  readOnly?: boolean
}

interface CloFormState {
  code: string
  description: string
  descriptionVn: string
  bloomLevel: string
  competencyLevel: string
}

const EMPTY_FORM: CloFormState = {
  code: "",
  description: "",
  descriptionVn: "",
  bloomLevel: "",
  competencyLevel: "",
}

const normalizeCode = (
  value: unknown,
) =>
  String(value ?? "")
    .trim()
    .toUpperCase()

const nextCloCode = (
  clos: Clo[],
) => {
  const maxNumber =
    clos.reduce(
      (
        currentMax,
        clo,
      ) => {
        const matched =
          normalizeCode(
            clo.code,
          ).match(
            /^CLO\s*0*(\d+)$/,
          )

        if (!matched) {
          return currentMax
        }

        return Math.max(
          currentMax,
          Number(matched[1]),
        )
      },
      0,
    )

  return `CLO${maxNumber + 1}`
}

const competencyLabel = (
  value: string,
) =>
  COMPETENCY_LEVELS.find(
    (item) =>
      item.value === value,
  )?.label ?? value

export default function Section3CLOs({
  syllabusId,
  readOnly = false,
}: Props) {
  const {
    data: clos = [],
    isLoading,
    isError,
    refetch,
  } = useClos(syllabusId)

  const createMutation =
    useCreateClo()

  const deleteMutation =
    useDeleteClo(syllabusId)

  const [
    showForm,
    setShowForm,
  ] = useState(false)

  const [
    form,
    setForm,
  ] =
    useState<CloFormState>(
      EMPTY_FORM,
    )

  const [
    errors,
    setErrors,
  ] =
    useState<
      Partial<CloFormState>
    >({})

  const sortedClos =
    useMemo(
      () =>
        [...clos].sort(
          (
            left,
            right,
          ) => {
            const leftOrder =
              Number(
                left.orderIndex
                ?? 0,
              )

            const rightOrder =
              Number(
                right.orderIndex
                ?? 0,
              )

            if (
              leftOrder
              !== rightOrder
            ) {
              return (
                leftOrder
                - rightOrder
              )
            }

            return normalizeCode(
              left.code,
            ).localeCompare(
              normalizeCode(
                right.code,
              ),
              "en",
              {
                numeric: true,
              },
            )
          },
        ),
      [clos],
    )

  const suggestedCode =
    useMemo(
      () =>
        nextCloCode(
          sortedClos,
        ),
      [sortedClos],
    )

  const competencyCounts =
    useMemo(
      () => ({
        KNOWLEDGE:
          sortedClos.filter(
            (clo) =>
              clo.competencyLevel
              === "KNOWLEDGE",
          ).length,
        SKILL:
          sortedClos.filter(
            (clo) =>
              clo.competencyLevel
              === "SKILL",
          ).length,
        ATTITUDE:
          sortedClos.filter(
            (clo) =>
              clo.competencyLevel
              === "ATTITUDE",
          ).length,
      }),
      [sortedClos],
    )

  const openAddForm = () => {
    setForm({
      ...EMPTY_FORM,
      code: suggestedCode,
    })
    setErrors({})
    setShowForm(true)
  }

  const cancelAdd = () => {
    setForm(EMPTY_FORM)
    setErrors({})
    setShowForm(false)
  }

  const validate = () => {
    const nextErrors:
      Partial<CloFormState> = {}

    const code =
      normalizeCode(
        form.code,
      )

    if (!code) {
      nextErrors.code =
        "CLO code is required."
    } else if (
      !/^CLO\d+$/.test(code)
    ) {
      nextErrors.code =
        "Use a code such as CLO1, CLO2, CLO3."
    } else if (
      sortedClos.some(
        (clo) =>
          normalizeCode(
            clo.code,
          ) === code,
      )
    ) {
      nextErrors.code =
        "This CLO code already exists."
    }

    if (
      !form.description.trim()
    ) {
      nextErrors.description =
        "English description is required."
    }

    if (!form.bloomLevel) {
      nextErrors.bloomLevel =
        "Bloom level is required."
    }

    if (
      !form.competencyLevel
    ) {
      nextErrors.competencyLevel =
        "Competency type is required."
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
        syllabusId,
        code:
          normalizeCode(
            form.code,
          ),
        description:
          form.description.trim(),
        descriptionVn:
          form.descriptionVn
            .trim()
          || undefined,
        bloomLevel:
          form.bloomLevel,
        competencyLevel:
          form.competencyLevel,
        orderIndex:
          sortedClos.length + 1,
      },
      {
        onSuccess: () => {
          cancelAdd()
        },
      },
    )
  }

  const handleDelete = (
    id: number,
    code: string,
  ) => {
    const confirmed =
      window.confirm(
        `Delete ${code}?\n\nAny CLO–PLO mappings linked to this CLO may also be removed. This action should only be used when the outcome is no longer part of the syllabus.`,
      )

    if (!confirmed) {
      return
    }

    deleteMutation.mutate(id)
  }

  const getBloomConfig = (
    level: string,
  ) =>
    BLOOM_LEVELS.find(
      (item) =>
        item.value === level,
    )

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center gap-2 text-slate-400">
        <Loader2 className="size-5 animate-spin" />
        Loading CLOs...
      </div>
    )
  }

  if (isError) {
    return (
      <Card className="border border-rose-200 bg-rose-50/60 shadow-sm">
        <CardContent className="p-6">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 size-5 shrink-0 text-rose-600" />

            <div>
              <p className="font-semibold text-rose-800">
                Unable to load Course Learning Outcomes
              </p>

              <p className="mt-1 text-sm text-rose-700">
                Retry the request before continuing with CLO–PLO mapping.
              </p>

              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-3 bg-white"
                onClick={() =>
                  void refetch()
                }
              >
                Try Again
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-5">
      <Card className="border border-blue-100 bg-blue-50/40 shadow-sm">
        <CardContent className="pb-4 pt-4">
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 size-5 shrink-0 text-blue-500" />

            <div>
              <p className="text-sm font-semibold text-blue-800">
                Course Learning Outcomes
              </p>

              <p className="mt-1 text-xs leading-5 text-blue-700">
                Define measurable outcomes using Bloom&apos;s Taxonomy and classify each outcome as Knowledge, Skill, or Attitude. CLOs created here become the rows used in CLO–PLO mapping and assessment alignment.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {readOnly && (
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-xs leading-5 text-slate-600">
            This syllabus version is read-only. CLO content is preserved exactly as submitted or approved.
          </p>
        </div>
      )}

      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between gap-4 border-b border-slate-100 pb-3">
          <CardTitle className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <ListChecks className="size-4 text-primary" />
            CLO List

            <Badge
              variant="outline"
              className="border-cyan-200 bg-cyan-50 text-primary"
            >
              {sortedClos.length}{" "}
              {sortedClos.length === 1
                ? "CLO"
                : "CLOs"}
            </Badge>
          </CardTitle>

          {!readOnly && (
            <Button
              type="button"
              size="sm"
              onClick={openAddForm}
              disabled={
                showForm
                || createMutation.isPending
              }
              className="h-8 gap-1.5 bg-primary text-xs text-white shadow-sm hover:bg-primary/90"
            >
              <Plus className="size-3.5" />
              Add CLO
            </Button>
          )}
        </CardHeader>

        <CardContent className="p-0">
          {sortedClos.length === 0
            && !showForm ? (
            <div className="px-6 py-12 text-center">
              <ListChecks className="mx-auto mb-3 size-10 text-slate-200" />

              <p className="text-sm font-medium text-slate-500">
                No CLOs have been defined
              </p>

              <p className="mx-auto mt-1 max-w-lg text-xs leading-5 text-slate-400">
                At least one meaningful Course Learning Outcome is required before the syllabus can be submitted and mapped to Program Learning Outcomes.
              </p>

              {!readOnly && (
                <Button
                  type="button"
                  size="sm"
                  onClick={openAddForm}
                  className="mt-4 bg-primary text-white hover:bg-primary/90"
                >
                  <Plus className="size-4" />
                  Add First CLO
                </Button>
              )}
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {sortedClos.map(
                (
                  clo: Clo,
                  index: number,
                ) => {
                  const bloom =
                    getBloomConfig(
                      clo.bloomLevel
                      ?? "",
                    )

                  return (
                    <div
                      key={clo.id}
                      className="group flex items-start gap-4 p-4 transition-colors hover:bg-slate-50/50 sm:p-5"
                    >
                      <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-xs font-bold text-slate-500">
                        {index + 1}
                      </span>

                      <div className="min-w-0 flex-1">
                        <div className="mb-2 flex flex-wrap items-center gap-2">
                          <span className="font-mono text-sm font-bold text-primary">
                            {clo.code}
                          </span>

                          {bloom && (
                            <span
                              className={`rounded-full px-2 py-0.5 text-xs font-semibold ${bloom.color}`}
                            >
                              {bloom.label}
                            </span>
                          )}

                          {clo.competencyLevel && (
                            <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">
                              {competencyLabel(
                                clo.competencyLevel,
                              )}
                            </span>
                          )}
                        </div>

                        <p className="text-sm leading-6 text-slate-700">
                          {clo.description}
                        </p>

                        {clo.descriptionVn && (
                          <div className="mt-2 rounded-lg border border-slate-100 bg-slate-50/70 px-3 py-2">
                            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                              Vietnamese
                            </p>

                            <p className="mt-1 text-xs leading-5 text-slate-600">
                              {clo.descriptionVn}
                            </p>
                          </div>
                        )}
                      </div>

                      {!readOnly && (
                        <Button
                          type="button"
                          size="sm"
                          variant="ghost"
                          aria-label={`Delete ${clo.code}`}
                          title={`Delete ${clo.code}`}
                          onClick={() =>
                            handleDelete(
                              clo.id,
                              clo.code,
                            )
                          }
                          disabled={
                            deleteMutation.isPending
                          }
                          className="size-8 shrink-0 p-0 text-slate-400 opacity-100 hover:bg-rose-50 hover:text-rose-600 sm:opacity-0 sm:group-hover:opacity-100"
                        >
                          {deleteMutation.isPending ? (
                            <Loader2 className="size-3.5 animate-spin" />
                          ) : (
                            <Trash2 className="size-3.5" />
                          )}
                        </Button>
                      )}
                    </div>
                  )
                },
              )}
            </div>
          )}

          {showForm && !readOnly && (
            <div className="border-t border-slate-200 bg-slate-50/70 p-4 sm:p-5">
              <div className="mb-4 flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-800">
                    Add Course Learning Outcome
                  </p>

                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    Use a unique CLO code and a measurable English outcome statement. Vietnamese description is optional but recommended for bilingual documentation.
                  </p>
                </div>

                <Badge
                  variant="outline"
                  className="shrink-0 border-slate-200 bg-white text-slate-500"
                >
                  Next: {suggestedCode}
                </Badge>
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Field>
                  <Label className="text-xs font-semibold text-slate-600">
                    CLO Code
                    <span className="ml-1 text-rose-500">
                      *
                    </span>
                  </Label>

                  <Input
                    className="h-9 bg-white text-sm uppercase"
                    placeholder="CLO1"
                    value={form.code}
                    onChange={(event) => {
                      setForm({
                        ...form,
                        code:
                          event.target.value
                            .toUpperCase()
                            .replace(/\s+/g, ""),
                      })

                      if (errors.code) {
                        setErrors({
                          ...errors,
                          code: undefined,
                        })
                      }
                    }}
                  />

                  <FieldError
                    message={errors.code}
                  />
                </Field>

                <Field>
                  <Label className="text-xs font-semibold text-slate-600">
                    Bloom&apos;s Level
                    <span className="ml-1 text-rose-500">
                      *
                    </span>
                  </Label>

                  <Select
                    value={
                      form.bloomLevel
                    }
                    onValueChange={(
                      value,
                    ) => {
                      setForm({
                        ...form,
                        bloomLevel:
                          value,
                      })

                      if (
                        errors.bloomLevel
                      ) {
                        setErrors({
                          ...errors,
                          bloomLevel:
                            undefined,
                        })
                      }
                    }}
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue placeholder="Select Bloom level..." />
                    </SelectTrigger>

                    <SelectContent>
                      {BLOOM_LEVELS.map(
                        (bloom) => (
                          <SelectItem
                            key={bloom.value}
                            value={bloom.value}
                          >
                            {bloom.label}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>

                  <FieldError
                    message={
                      errors.bloomLevel
                    }
                  />
                </Field>

                <Field className="md:col-span-2">
                  <Label className="text-xs font-semibold text-slate-600">
                    Description (English)
                    <span className="ml-1 text-rose-500">
                      *
                    </span>
                  </Label>

                  <textarea
                    className="min-h-[86px] w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-primary/60 focus:ring-2 focus:ring-primary/10 placeholder:text-slate-400"
                    placeholder="Students will be able to apply..."
                    value={
                      form.description
                    }
                    onChange={(
                      event,
                    ) => {
                      setForm({
                        ...form,
                        description:
                          event.target
                            .value,
                      })

                      if (
                        errors.description
                      ) {
                        setErrors({
                          ...errors,
                          description:
                            undefined,
                        })
                      }
                    }}
                  />

                  <FieldError
                    message={
                      errors.description
                    }
                  />
                </Field>

                <Field>
                  <Label className="text-xs font-semibold text-slate-600">
                    Description (Vietnamese)
                  </Label>

                  <textarea
                    className="min-h-[78px] w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition focus:border-primary/60 focus:ring-2 focus:ring-primary/10 placeholder:text-slate-400"
                    placeholder="Sinh viên có khả năng..."
                    value={
                      form.descriptionVn
                    }
                    onChange={(
                      event,
                    ) =>
                      setForm({
                        ...form,
                        descriptionVn:
                          event.target
                            .value,
                      })
                    }
                  />
                </Field>

                <Field>
                  <Label className="text-xs font-semibold text-slate-600">
                    Competency Type
                    <span className="ml-1 text-rose-500">
                      *
                    </span>
                  </Label>

                  <Select
                    value={
                      form.competencyLevel
                    }
                    onValueChange={(
                      value,
                    ) => {
                      setForm({
                        ...form,
                        competencyLevel:
                          value,
                      })

                      if (
                        errors.competencyLevel
                      ) {
                        setErrors({
                          ...errors,
                          competencyLevel:
                            undefined,
                        })
                      }
                    }}
                  >
                    <SelectTrigger className="h-9 bg-white text-sm">
                      <SelectValue placeholder="Select competency type..." />
                    </SelectTrigger>

                    <SelectContent>
                      {COMPETENCY_LEVELS.map(
                        (competency) => (
                          <SelectItem
                            key={
                              competency.value
                            }
                            value={
                              competency.value
                            }
                          >
                            {
                              competency.label
                            }
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>

                  <FieldError
                    message={
                      errors.competencyLevel
                    }
                  />
                </Field>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <Button
                  type="button"
                  size="sm"
                  onClick={handleAdd}
                  disabled={
                    createMutation.isPending
                  }
                  className="h-9 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
                >
                  {createMutation.isPending ? (
                    <Loader2 className="size-3.5 animate-spin" />
                  ) : (
                    <Plus className="size-3.5" />
                  )}

                  Save CLO
                </Button>

                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={cancelAdd}
                  disabled={
                    createMutation.isPending
                  }
                  className="h-9 text-xs"
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {sortedClos.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-3">
          <SummaryCard
            label="Knowledge"
            value={
              competencyCounts.KNOWLEDGE
            }
          />

          <SummaryCard
            label="Skill"
            value={
              competencyCounts.SKILL
            }
          />

          <SummaryCard
            label="Attitude"
            value={
              competencyCounts.ATTITUDE
            }
          />
        </div>
      )}

      {sortedClos.length > 0 && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 px-4 py-3">
          <div className="flex items-start gap-2">
            <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" />

            <p className="text-xs leading-5 text-emerald-800">
              CLO definitions are available. Continue to the CLO–PLO tab and map each CLO to one or more Program Learning Outcomes with an appropriate I/D/A contribution level.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}

function Field({
  children,
  className = "",
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <div
      className={`space-y-1.5 ${className}`}
    >
      {children}
    </div>
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

function SummaryCard({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <Card className="border border-slate-200 shadow-sm">
      <CardContent className="pb-3 pt-3 text-center">
        <p className="text-2xl font-bold text-slate-800">
          {value}
        </p>

        <p className="mt-0.5 text-xs text-slate-500">
          {label}
        </p>
      </CardContent>
    </Card>
  )
}