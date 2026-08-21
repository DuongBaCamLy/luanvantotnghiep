import { useMutation, useQueryClient } from "@tanstack/react-query"

import { syllabusApi } from "@/api/syllabusApi"

/**
 * Workflow hiện tại không có endpoint ép một đề cương đã nộp về DRAFT.
 * Hook này được giữ cho tương thích và chỉ làm mới bản DRAFT hiện có.
 */
export const useDraftSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (id: number) => {
      const syllabus = await syllabusApi.getById(id)
      if (syllabus.status !== "DRAFT") {
        throw new Error(
          "Không thể chuyển trực tiếp về DRAFT. Hãy dùng quy trình yêu cầu chỉnh sửa.",
        )
      }
      return syllabus
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["syllabuses"] })
      void queryClient.invalidateQueries({ queryKey: ["curriculum-map"] })
    },
  })
}
