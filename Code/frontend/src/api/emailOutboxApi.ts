import { api } from "./axios"
import type {
  EmailDeliveryStatus,
  EmailOutboxItem,
  EmailOutboxSummary,
} from "@/types/emailOutbox"

export const emailOutboxApi = {
  getRecent: async (
    status?: EmailDeliveryStatus
  ): Promise<EmailOutboxItem[]> => {
    const response = await api.get<EmailOutboxItem[]>(
      "/api/admin/email-outbox",
      {
        params: {
          status: status || undefined,
          limit: 200,
        },
      }
    )
    return response.data
  },

  getSummary: async (): Promise<EmailOutboxSummary> => {
    const response = await api.get<EmailOutboxSummary>(
      "/api/admin/email-outbox/summary"
    )
    return response.data
  },

  retry: async (id: number): Promise<EmailOutboxItem> => {
    const response = await api.post<EmailOutboxItem>(
      `/api/admin/email-outbox/${id}/retry`
    )
    return response.data
  },
}
