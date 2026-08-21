import { useMutation, useQueryClient } from "@tanstack/react-query"

import { approvalRequestApi } from "@/api/approvalRequestApi"

/**
 * Hook tương thích cho workflow mới: id là approval-request id,
 * reviewer luôn được backend lấy từ JWT nên không nhận approverId.
 */
export const useApproveSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      comment = "",
    }: {
      id: number
      comment?: string
      approverId?: number
    }) =>
      approvalRequestApi.review(id, {
        status: "APPROVED",
        comment,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["syllabuses"] })
      void queryClient.invalidateQueries({ queryKey: ["approval-requests"] })
      void queryClient.invalidateQueries({ queryKey: ["curriculum-map"] })
    },
  })
}
