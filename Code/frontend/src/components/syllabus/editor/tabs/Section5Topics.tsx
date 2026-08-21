import { useMemo, useState } from "react"
import {
  AlertCircle,
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  Clock3,
  Loader2,
  Pencil,
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
import { useSyllabus } from "@/hooks/useSyllabus"
import {
  useCreateTopic,
  useDeleteTopic,
  useTopics,
  useUpdateTopic,
} from "@/hooks/useTopics"
import {
  TOPIC_TYPES,
} from "@/types/topic"
import type {
  CreateTopicRequest,
  Topic,
} from "@/types/topic"

interface Props {
  syllabusId: number
  readOnly?: boolean
}

type TopicDraft =
  Omit<
    CreateTopicRequest,
    "syllabusId"
  >

const newTopicDraft = (
  weekNumber = 1,
  orderInWeek = 1,
): TopicDraft => ({
  weekNumber,
  orderInWeek,
  name: "",
  nameVn: "",
  teachingHours: 0,
  labHours: 0,
  selfStudyHours: 0,
  topicType: "LECTURE",
  teachingMethod: "",
  learningActivity: "",
  notes: "",
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

const optionalNumber = (
  value: unknown,
) => {
  const text =
    String(value ?? "")
      .trim()

  if (!text) {
    return null
  }

  const parsed =
    Number(text)

  return Number.isFinite(parsed)
    ? parsed
    : null
}

const topicTypeLabel = (
  value: unknown,
) =>
  TOPIC_TYPES.find(
    (item) =>
      item.value === value,
  )?.label
  ?? String(
    value ?? "Topic",
  )

export default function Section5Topics({
  syllabusId,
  readOnly = false,
}: Props) {
  const {
    data: syllabus,
  } = useSyllabus(
    syllabusId,
  )

  const {
    data: topics = [],
    isLoading,
  } = useTopics(
    syllabusId,
  )

  const createMutation =
    useCreateTopic()

  const updateMutation =
    useUpdateTopic(
      syllabusId,
    )

  const deleteMutation =
    useDeleteTopic(
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
    useState<TopicDraft>(
      newTopicDraft(),
    )

  const [
    editingId,
    setEditingId,
  ] =
    useState<number | null>(
      null,
    )

  const [
    editForm,
    setEditForm,
  ] =
    useState<
      Partial<Topic>
    >({})

  const [
    formError,
    setFormError,
  ] =
    useState<string | null>(
      null,
    )

  const sortedTopics =
    useMemo(
      () =>
        [...topics].sort(
          (
            left,
            right,
          ) =>
            left.weekNumber
              - right.weekNumber
            || Number(
              left.orderInWeek
              ?? 0,
            )
              - Number(
                right.orderInWeek
                ?? 0,
              ),
        ),
      [topics],
    )

  const byWeek =
    useMemo(
      () => {
        const result:
          Record<
            number,
            Topic[]
          > = {}

        sortedTopics.forEach(
          (topic) => {
            const week =
              topic.weekNumber

            result[week] =
              result[week]
              ?? []

            result[week].push(
              topic,
            )
          },
        )

        return result
      },
      [sortedTopics],
    )

  const weeks =
    useMemo(
      () =>
        Object.keys(byWeek)
          .map(Number)
          .sort(
            (a, b) =>
              a - b,
          ),
      [byWeek],
    )

  const totalLecture =
    sortedTopics.reduce(
      (sum, topic) =>
        sum
        + numberValue(
          topic.teachingHours,
        ),
      0,
    )

  const totalLab =
    sortedTopics.reduce(
      (sum, topic) =>
        sum
        + numberValue(
          topic.labHours,
        ),
      0,
    )

  const totalSelfStudy =
    sortedTopics.reduce(
      (sum, topic) =>
        sum
        + numberValue(
          topic.selfStudyHours,
        ),
      0,
    )

  const totalTopicHours =
    totalLecture
    + totalLab
    + totalSelfStudy

  const expectedWorkload =
    optionalNumber(
      syllabus?.workloadTotal,
    )

  const workloadMatches =
    expectedWorkload === null
      ? null
      : Math.abs(
          expectedWorkload
          - totalTopicHours,
        ) < 0.01

  const nextWeek =
    sortedTopics.length > 0
      ? Math.max(
          ...sortedTopics.map(
            (topic) =>
              topic.weekNumber,
          ),
        )
      : 1

  const nextOrderForWeek = (
    week: number,
  ) =>
    (
      byWeek[week]?.reduce(
        (
          max,
          topic,
        ) =>
          Math.max(
            max,
            Number(
              topic.orderInWeek
              ?? 0,
            ),
          ),
        0,
      )
      ?? 0
    )
    + 1

  const openAddForm = () => {
    const week =
      Math.max(
        1,
        nextWeek,
      )

    setForm(
      newTopicDraft(
        week,
        nextOrderForWeek(
          week,
        ),
      ),
    )

    setFormError(null)
    setShowForm(true)
  }

  const validateDraft = (
    draft:
      Partial<TopicDraft>,
  ) => {
    if (
      !String(
        draft.name ?? "",
      ).trim()
    ) {
      return "Topic name in English is required."
    }

    if (
      numberValue(
        draft.weekNumber,
      ) < 1
    ) {
      return "Week number must be at least 1."
    }

    if (
      numberValue(
        draft.orderInWeek,
      ) < 1
    ) {
      return "Order within the week must be at least 1."
    }

    const lecture =
      numberValue(
        draft.teachingHours,
      )

    const lab =
      numberValue(
        draft.labHours,
      )

    const selfStudy =
      numberValue(
        draft.selfStudyHours,
      )

    if (
      lecture < 0
      || lab < 0
      || selfStudy < 0
    ) {
      return "Teaching, lab, and self-study hours cannot be negative."
    }

    if (
      lecture
        + lab
        + selfStudy
      <= 0
    ) {
      return "Each topic must allocate at least one hour across lecture, lab, or self-study."
    }

    return null
  }

  const handleAdd = () => {
    const error =
      validateDraft(form)

    if (error) {
      setFormError(error)
      return
    }

    setFormError(null)

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
        teachingMethod:
          form.teachingMethod
            ?.trim()
          || undefined,
        learningActivity:
          form.learningActivity
            ?.trim()
          || undefined,
        notes:
          form.notes
            ?.trim()
          || undefined,
      },
      {
        onSuccess: () => {
          setShowForm(false)
          setForm(
            newTopicDraft(),
          )
        },
      },
    )
  }

  const handleEdit = (
    topic: Topic,
  ) => {
    if (readOnly) {
      return
    }

    setEditingId(
      topic.id,
    )

    setEditForm({
      ...topic,
    })

    setFormError(null)
  }

  const handleSaveEdit =
    () => {
      if (!editingId) {
        return
      }

      const draft:
        Partial<TopicDraft> = {
          weekNumber:
            editForm.weekNumber,
          orderInWeek:
            editForm.orderInWeek,
          name:
            editForm.name,
          nameVn:
            editForm.nameVn,
          teachingHours:
            editForm.teachingHours,
          labHours:
            editForm.labHours,
          selfStudyHours:
            editForm.selfStudyHours,
          topicType:
            editForm.topicType,
          teachingMethod:
            editForm.teachingMethod,
          learningActivity:
            editForm.learningActivity,
          notes:
            editForm.notes,
        }

      const error =
        validateDraft(draft)

      if (error) {
        setFormError(error)
        return
      }

      setFormError(null)

      updateMutation.mutate(
        {
          id: editingId,
          data: {
            syllabusId,
            weekNumber:
              numberValue(
                editForm.weekNumber,
              ),
            orderInWeek:
              numberValue(
                editForm.orderInWeek,
              ),
            name:
              String(
                editForm.name
                ?? "",
              ).trim(),
            nameVn:
              editForm.nameVn
                ?.trim()
              || undefined,
            teachingHours:
              numberValue(
                editForm.teachingHours,
              ),
            labHours:
              numberValue(
                editForm.labHours,
              ),
            selfStudyHours:
              numberValue(
                editForm.selfStudyHours,
              ),
            topicType:
              String(
                editForm.topicType
                ?? "LECTURE",
              ),
            teachingMethod:
              editForm.teachingMethod
                ?.trim()
              || undefined,
            learningActivity:
              editForm.learningActivity
                ?.trim()
              || undefined,
            notes:
              editForm.notes
                ?.trim()
              || undefined,
          },
        },
        {
          onSuccess: () => {
            setEditingId(null)
            setEditForm({})
          },
        },
      )
    }

  const handleDelete = (
    topic: Topic,
  ) => {
    const confirmed =
      window.confirm(
        `Delete "${topic.name}" from Week ${topic.weekNumber}?`,
      )

    if (!confirmed) {
      return
    }

    deleteMutation.mutate(
      topic.id,
    )
  }

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center gap-2 text-slate-400">
        <Loader2 className="size-5 animate-spin" />
        Loading teaching content...
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <Card className="border border-blue-100 bg-blue-50/40 shadow-sm">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <Clock3 className="mt-0.5 size-5 shrink-0 text-blue-500" />

            <div>
              <p className="text-sm font-semibold text-blue-800">
                Weekly Teaching Content
              </p>

              <p className="mt-1 text-xs leading-5 text-blue-700">
                Organize the syllabus by week and topic. Each topic records lecture, laboratory, and self-study hours together with the teaching method and student learning activity.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
        <Metric
          label="Topics"
          value={sortedTopics.length}
        />
        <Metric
          label="Lecture Hours"
          value={totalLecture}
        />
        <Metric
          label="Lab Hours"
          value={totalLab}
        />
        <Metric
          label="Self-study"
          value={totalSelfStudy}
        />
        <Metric
          label="Total Hours"
          value={totalTopicHours}
        />
      </div>

      {sortedTopics.length === 0 ? (
        <Card className="border border-amber-200 bg-amber-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <AlertCircle className="mt-0.5 size-4 shrink-0 text-amber-600" />

              <p className="text-xs leading-5 text-amber-800">
                Teaching Content is required before submission. Add at least one topic and allocate its learning hours.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : workloadMatches === false ? (
        <Card className="border border-rose-200 bg-rose-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-rose-600" />

              <p className="text-xs leading-5 text-rose-800">
                Workload mismatch: Topics total {totalTopicHours} hours, while General Information records {expectedWorkload} total workload hours. Review either section before submission.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : workloadMatches === true ? (
        <Card className="border border-emerald-200 bg-emerald-50/40 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-start gap-2">
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" />

              <p className="text-xs leading-5 text-emerald-800">
                Topic hours match the Total Workload recorded in General Information.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="border border-slate-200 bg-slate-50/60 shadow-sm">
          <CardContent className="p-4">
            <p className="text-xs leading-5 text-slate-600">
              Total Workload has not yet been configured in General Information, so the system cannot compare workload against topic hours.
            </p>
          </CardContent>
        </Card>
      )}

      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between gap-4 border-b border-slate-100 pb-3">
          <CardTitle className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <BookOpen className="size-4 text-primary" />
            Teaching Schedule
          </CardTitle>

          {!readOnly && (
            <Button
              type="button"
              size="sm"
              onClick={openAddForm}
              disabled={showForm}
              className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
            >
              <Plus className="size-3.5" />
              Add Topic
            </Button>
          )}
        </CardHeader>

        <CardContent className="p-0">
          {sortedTopics.length === 0
            && !showForm ? (
            <div className="px-6 py-12 text-center">
              <BookOpen className="mx-auto mb-3 size-10 text-slate-200" />
              <p className="text-sm font-medium text-slate-500">
                No teaching topics defined
              </p>
              <p className="mt-1 text-xs text-slate-400">
                Add the first topic to begin the weekly teaching plan.
              </p>
            </div>
          ) : (
            weeks.map(
              (week) => (
                <section
                  key={week}
                >
                  <div className="flex items-center gap-2 border-y border-slate-100 bg-slate-50/80 px-4 py-2 first:border-t-0">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-700">
                      Week {week}
                    </span>

                    <Badge
                      variant="outline"
                      className="border-cyan-200 bg-cyan-50 text-primary"
                    >
                      {byWeek[week].length}{" "}
                      {byWeek[week].length === 1
                        ? "topic"
                        : "topics"}
                    </Badge>
                  </div>

                  <div className="divide-y divide-slate-100">
                    {byWeek[week].map(
                      (topic) => (
                        <div
                          key={topic.id}
                        >
                          {editingId === topic.id
                            && !readOnly ? (
                            <TopicEditor
                              value={editForm}
                              error={formError}
                              saving={updateMutation.isPending}
                              onChange={setEditForm}
                              onSave={handleSaveEdit}
                              onCancel={() => {
                                setEditingId(null)
                                setEditForm({})
                                setFormError(null)
                              }}
                            />
                          ) : (
                            <div className="group flex items-start gap-4 px-4 py-4 transition hover:bg-slate-50/50">
                              <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-xs font-bold text-slate-500">
                                {topic.orderInWeek ?? 1}
                              </span>

                              <div className="min-w-0 flex-1">
                                <div className="flex flex-wrap items-center gap-2">
                                  <p className="text-sm font-semibold text-slate-800">
                                    {topic.name}
                                  </p>

                                  <Badge
                                    variant="outline"
                                    className="border-slate-200 bg-slate-50 text-slate-600"
                                  >
                                    {topicTypeLabel(topic.topicType)}
                                  </Badge>
                                </div>

                                {topic.nameVn && (
                                  <p className="mt-1 text-xs text-slate-500">
                                    {topic.nameVn}
                                  </p>
                                )}

                                <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">
                                  <span>Lecture: {numberValue(topic.teachingHours)}h</span>
                                  <span>Lab: {numberValue(topic.labHours)}h</span>
                                  <span>Self-study: {numberValue(topic.selfStudyHours)}h</span>
                                </div>

                                {(topic.teachingMethod
                                  || topic.learningActivity
                                  || topic.notes) && (
                                  <div className="mt-3 grid gap-2 md:grid-cols-3">
                                    <InfoValue
                                      label="Teaching Method"
                                      value={topic.teachingMethod}
                                    />
                                    <InfoValue
                                      label="Learning Activity"
                                      value={topic.learningActivity}
                                    />
                                    <InfoValue
                                      label="Notes"
                                      value={topic.notes}
                                    />
                                  </div>
                                )}
                              </div>

                              {!readOnly && (
                                <div className="flex shrink-0 gap-1">
                                  <Button
                                    type="button"
                                    size="sm"
                                    variant="ghost"
                                    aria-label={`Edit ${topic.name}`}
                                    onClick={() =>
                                      handleEdit(topic)
                                    }
                                    className="size-8 p-0 text-slate-400 hover:bg-cyan-50 hover:text-primary"
                                  >
                                    <Pencil className="size-3.5" />
                                  </Button>

                                  <Button
                                    type="button"
                                    size="sm"
                                    variant="ghost"
                                    aria-label={`Delete ${topic.name}`}
                                    onClick={() =>
                                      handleDelete(topic)
                                    }
                                    disabled={deleteMutation.isPending}
                                    className="size-8 p-0 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                                  >
                                    <Trash2 className="size-3.5" />
                                  </Button>
                                </div>
                              )}
                            </div>
                          )}
                        </div>
                      ),
                    )}
                  </div>
                </section>
              ),
            )
          )}

          {showForm
            && !readOnly && (
            <div className="border-t border-slate-200 bg-slate-50/70 p-4">
              <p className="mb-3 text-xs font-bold uppercase tracking-wider text-slate-600">
                Add Teaching Topic
              </p>

              <TopicEditor
                value={form}
                error={formError}
                saving={createMutation.isPending}
                onChange={(value) =>
                  setForm(
                    value as TopicDraft,
                  )
                }
                onSave={handleAdd}
                onCancel={() => {
                  setShowForm(false)
                  setFormError(null)
                }}
              />
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function TopicEditor({
  value,
  error,
  saving,
  onChange,
  onSave,
  onCancel,
}: {
  value:
    Partial<TopicDraft>
    | Partial<Topic>
  error: string | null
  saving: boolean
  onChange: (
    value:
      Partial<TopicDraft>
      | Partial<Topic>,
  ) => void
  onSave: () => void
  onCancel: () => void
}) {
  return (
    <div className="space-y-4 p-4">
      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">
          {error}
        </div>
      )}

      <div className="grid gap-3 md:grid-cols-4">
        <div className="space-y-1">
          <Label className="text-xs font-semibold text-slate-600">
            Week *
          </Label>
          <Input
            type="number"
            min={1}
            max={52}
            className="h-9 bg-white text-sm"
            value={value.weekNumber ?? 1}
            onChange={(event) =>
              onChange({
                ...value,
                weekNumber:
                  Number(event.target.value),
              })
            }
          />
        </div>

        <div className="space-y-1">
          <Label className="text-xs font-semibold text-slate-600">
            Order in Week *
          </Label>
          <Input
            type="number"
            min={1}
            className="h-9 bg-white text-sm"
            value={value.orderInWeek ?? 1}
            onChange={(event) =>
              onChange({
                ...value,
                orderInWeek:
                  Number(event.target.value),
              })
            }
          />
        </div>

        <div className="space-y-1 md:col-span-2">
          <Label className="text-xs font-semibold text-slate-600">
            Topic Type
          </Label>
          <Select
            value={String(value.topicType ?? "LECTURE")}
            onValueChange={(next) =>
              onChange({
                ...value,
                topicType:
                  next as Topic["topicType"],
              })
            }
          >
            <SelectTrigger className="h-9 bg-white text-sm">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {TOPIC_TYPES.map(
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
        </div>

        <div className="space-y-1 md:col-span-2">
          <Label className="text-xs font-semibold text-slate-600">
            Topic Name (English) *
          </Label>
          <Input
            className="h-9 bg-white text-sm"
            value={value.name ?? ""}
            onChange={(event) =>
              onChange({
                ...value,
                name:
                  event.target.value,
              })
            }
            placeholder="Introduction to..."
          />
        </div>

        <div className="space-y-1 md:col-span-2">
          <Label className="text-xs font-semibold text-slate-600">
            Topic Name (Vietnamese)
          </Label>
          <Input
            className="h-9 bg-white text-sm"
            value={value.nameVn ?? ""}
            onChange={(event) =>
              onChange({
                ...value,
                nameVn:
                  event.target.value,
              })
            }
          />
        </div>

        {[
          ["teachingHours", "Lecture Hours"],
          ["labHours", "Lab Hours"],
          ["selfStudyHours", "Self-study Hours"],
        ].map(
          ([field, label]) => (
            <div
              key={field}
              className="space-y-1"
            >
              <Label className="text-xs font-semibold text-slate-600">
                {label}
              </Label>
              <Input
                type="number"
                min={0}
                step="0.5"
                className="h-9 bg-white text-sm"
                value={numberValue(value[field as keyof typeof value])}
                onChange={(event) =>
                  onChange({
                    ...value,
                    [field]:
                      Number(event.target.value),
                  })
                }
              />
            </div>
          ),
        )}

        <div />

        <div className="space-y-1 md:col-span-2">
          <Label className="text-xs font-semibold text-slate-600">
            Teaching Method
          </Label>
          <Input
            className="h-9 bg-white text-sm"
            value={value.teachingMethod ?? ""}
            onChange={(event) =>
              onChange({
                ...value,
                teachingMethod:
                  event.target.value,
              })
            }
            placeholder="Lecture, guided practice, discussion..."
          />
        </div>

        <div className="space-y-1 md:col-span-2">
          <Label className="text-xs font-semibold text-slate-600">
            Learning Activity
          </Label>
          <Input
            className="h-9 bg-white text-sm"
            value={value.learningActivity ?? ""}
            onChange={(event) =>
              onChange({
                ...value,
                learningActivity:
                  event.target.value,
              })
            }
            placeholder="Problem solving, lab exercise, group discussion..."
          />
        </div>

        <div className="space-y-1 md:col-span-4">
          <Label className="text-xs font-semibold text-slate-600">
            Notes
          </Label>
          <textarea
            className="min-h-[72px] w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-primary/60"
            value={value.notes ?? ""}
            onChange={(event) =>
              onChange({
                ...value,
                notes:
                  event.target.value,
              })
            }
          />
        </div>
      </div>

      <div className="flex gap-2">
        <Button
          type="button"
          size="sm"
          onClick={onSave}
          disabled={saving}
          className="h-8 gap-1.5 bg-primary text-xs text-white hover:bg-primary/90"
        >
          {saving && (
            <Loader2 className="size-3.5 animate-spin" />
          )}
          Save Topic
        </Button>

        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={onCancel}
          disabled={saving}
          className="h-8 text-xs"
        >
          Cancel
        </Button>
      </div>
    </div>
  )
}

function Metric({
  label,
  value,
}: {
  label: string
  value: number
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

function InfoValue({
  label,
  value,
}: {
  label: string
  value?: string | null
}) {
  return (
    <div className="rounded-lg border border-slate-100 bg-slate-50/70 px-3 py-2">
      <p className="text-[10px] font-bold uppercase tracking-wide text-slate-400">
        {label}
      </p>
      <p className="mt-1 text-xs leading-5 text-slate-600">
        {value || "Not recorded"}
      </p>
    </div>
  )
}