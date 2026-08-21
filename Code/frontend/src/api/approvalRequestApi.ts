import { api } from "./axios"

import type {
  ApprovalHistoryItem,
  ApprovalRequestItem,
  ApprovalStep,
  ReviewApprovalPayload,
} from "@/types/approval"

export const approvalRequestApi = {
  getPendingByStep: async (
    step: ApprovalStep
  ): Promise<ApprovalRequestItem[]> => {
    const response = await api.get<ApprovalRequestItem[]>(
      `/api/approval-requests/pending/${step}`
    )

    return response.data
  },

  review: async (
    id: number,
    payload: ReviewApprovalPayload
  ): Promise<ApprovalRequestItem> => {
    const response = await api.put<ApprovalRequestItem>(
      `/api/approval-requests/${id}/review`,
      payload
    )

    return response.data
  },

  getBySyllabus: async (
    syllabusId: number
  ): Promise<ApprovalRequestItem[]> => {
    const response = await api.get<ApprovalRequestItem[]>(
      `/api/approval-requests/syllabus/${syllabusId}`
    )

    return response.data
  },

  /**
   * FR-05.8:
   * Lấy toàn bộ lịch sử review/comment của syllabus.
   */
  getApprovalHistory: async (
    syllabusId: number
  ): Promise<ApprovalHistoryItem[]> => {
    const response = await api.get<ApprovalHistoryItem[]>(
      `/api/approval-requests/syllabus/${syllabusId}/history`
    )

    return response.data
  },
}