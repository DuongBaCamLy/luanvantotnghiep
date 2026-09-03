import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"

import { syllabusApi } from "@/api/syllabusApi"

export const useSubmitSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      syllabusApi.submit(id),

    onSuccess: (_data, syllabusId) => {
      queryClient.invalidateQueries({
        queryKey: ["syllabuses"],
      })

      queryClient.invalidateQueries({
        queryKey: ["syllabus", syllabusId],
      })

      queryClient.invalidateQueries({
        queryKey: ["approval-requests"],
      })

      queryClient.invalidateQueries({
        queryKey: ["faculty-dashboard"],
      })

      queryClient.invalidateQueries({
        queryKey: ["dept-head-dashboard"],
      })

      queryClient.invalidateQueries({
        queryKey: ["depthead-courses"],
      })
    },
  })
}
