import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { topicApi } from "@/api/topicApi"
import type { CreateTopicRequest } from "@/types/topic"

export function useTopics(syllabusId: number) {
  return useQuery({
    queryKey: ["topics", syllabusId],
    queryFn: () => topicApi.getBySyllabus(syllabusId),
    enabled: !!syllabusId,
  })
}

export function useCreateTopic() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateTopicRequest) => topicApi.create(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["topics", vars.syllabusId] })
    },
  })
}

export function useUpdateTopic(syllabusId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateTopicRequest }) =>
      topicApi.update(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["topics", syllabusId] })
    },
  })
}

export function useDeleteTopic(syllabusId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => topicApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["topics", syllabusId] })
    },
  })
}
