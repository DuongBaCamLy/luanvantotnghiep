import { useMutation, useQueryClient } from "@tanstack/react-query"

import { approvalRequestApi } from "@/api/approvalRequestApi"

/**
 * Reject syllabus theo FR-03.4 / FR-05.2 / FR-05.3.
 * Backend bắt buộc comment khi reject.
 */
export const useRejectSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      comment,
    }: {
      id: number
      comment: string
      approverId?: number
    }) =>
      approvalRequestApi.review(id, {
        status: "REJECTED",
        comment,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["syllabuses"] })
      void queryClient.invalidateQueries({ queryKey: ["approval-requests"] })
    },
  })
}