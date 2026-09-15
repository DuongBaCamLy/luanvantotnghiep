import { useMutation, useQueryClient } from "@tanstack/react-query"
import { syllabusApi } from "@/api/syllabusApi"
import type { CreateSyllabusRequest } from "@/types/syllabus"

export const useCreateSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateSyllabusRequest) =>
      syllabusApi.create(payload),

    onSuccess: () => {
      for (const key of ["faculty-dashboard", "depthead-courses", "my-active-assignments", "course-programs", "program-timeline"]) {
        void queryClient.invalidateQueries({ queryKey: [key] })
      }
      queryClient.invalidateQueries({
        queryKey: ["syllabuses"],
      })
    },
  })
}