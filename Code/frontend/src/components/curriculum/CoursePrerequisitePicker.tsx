// components/curriculum/CoursePrerequisitePicker.tsx
import { useState } from "react"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { courseApi } from "@/api/courseApi"
import { courseRelationshipApi, type RelationType } from "@/api/courseRelationshipApi"
import { X } from "lucide-react"
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select"

const RELATION_LABELS: Record<RelationType, string> = {
  PREREQUISITE: "Tiên quyết",
  COREQUISITE: "Song hành",
  RECOMMENDED: "Khuyến nghị",
  EQUIVALENT: "Tương đương",
}

interface Props {
  courseId: number          // the course currently being authored
  disabled?: boolean
}

export function CoursePrerequisitePicker({ courseId, disabled }: Props) {
  const queryClient = useQueryClient()
  const [pickCourseId, setPickCourseId] = useState("")
  const [pickType, setPickType] = useState<RelationType>("PREREQUISITE")

  const { data: courses = [] } = useQuery({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
  })

  const { data: relationships = [] } = useQuery({
    queryKey: ["course-relationships", courseId],
    queryFn: () => courseRelationshipApi.getByCourse(courseId),
    enabled: !!courseId,
  })

  const addMutation = useMutation({
    mutationFn: () =>
      courseRelationshipApi.create({
        courseId,
        relatedCourseId: Number(pickCourseId),
        relationType: pickType,
      }),
    onSuccess: () => {
      setPickCourseId("")
      queryClient.invalidateQueries({ queryKey: ["course-relationships", courseId] })
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "Không thể thêm ràng buộc học phần"
      alert(msg)
    },
  })

  const removeMutation = useMutation({
    mutationFn: (id: number) => courseRelationshipApi.delete(id),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["course-relationships", courseId] }),
  })

  const availableCourses = courses.filter(
    (c) => c.id !== courseId && !relationships.some((r) => r.relatedCourseId === c.id)
  )

  return (
    <div className="space-y-3">
      {/* existing relationships as removable tags */}
      <div className="flex flex-wrap gap-2">
        {relationships.map((rel) => (
          <span
            key={rel.id}
            className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700"
          >
            <span className="font-mono">{rel.relatedCourseCode}</span>
            <span className="text-slate-400">· {RELATION_LABELS[rel.relationType]}</span>
            {!disabled && (
              <button onClick={() => removeMutation.mutate(rel.id)} className="text-slate-400 hover:text-rose-500">
                <X className="size-3" />
              </button>
            )}
          </span>
        ))}
        {relationships.length === 0 && (
          <span className="text-xs text-slate-400 italic">Chưa có ràng buộc nào</span>
        )}
      </div>

      {/* add new tag */}
      {!disabled && (
        <div className="flex gap-2">
          <Select value={pickCourseId} onValueChange={setPickCourseId}>
            <SelectTrigger className="h-9 flex-1"><SelectValue placeholder="Chọn môn học..." /></SelectTrigger>
            <SelectContent>
              {availableCourses.map((c) => (
                <SelectItem key={c.id} value={String(c.id)}>{c.courseCode} — {c.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={pickType} onValueChange={(v) => setPickType(v as RelationType)}>
            <SelectTrigger className="h-9 w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              {(Object.keys(RELATION_LABELS) as RelationType[]).map((t) => (
                <SelectItem key={t} value={t}>{RELATION_LABELS[t]}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <button
            disabled={!pickCourseId || addMutation.isPending}
            onClick={() => addMutation.mutate()}
            className="h-9 px-3 rounded-lg bg-slate-900 text-white text-xs font-bold disabled:opacity-40"
          >
            Thêm
          </button>
        </div>
      )}
    </div>
  )
}