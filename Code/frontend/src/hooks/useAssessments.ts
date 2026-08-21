import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { assessmentApi } from "@/api/assessmentApi"
import type { CreateAssessmentComponentRequest } from "@/types/assessment"

export function useAssessments(syllabusId: number) {
  return useQuery({
    queryKey: ["assessments", syllabusId],
    queryFn: () => assessmentApi.getBySyllabus(syllabusId),
    enabled: !!syllabusId,
  })
}

export function useCreateAssessment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateAssessmentComponentRequest) => assessmentApi.create(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["assessments", vars.syllabusId] })
    },
  })
}

export function useDeleteAssessment(syllabusId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => assessmentApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["assessments", syllabusId] })
    },
  })
}
