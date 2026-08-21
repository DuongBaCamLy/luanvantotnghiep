export type EmailDeliveryStatus =
  | "PENDING"
  | "RETRY"
  | "SENT"
  | "FAILED"

export interface EmailOutboxItem {
  id: number
  recipientEmail: string
  recipientName: string | null
  subject: string
  eventType: string
  syllabusId: number | null
  status: EmailDeliveryStatus
  attemptCount: number
  nextAttemptAt: string
  lastError: string | null
  sentAt: string | null
  createdAt: string
  updatedAt: string
}

export interface EmailOutboxSummary {
  pending: number
  retry: number
  sent: number
  failed: number
  total: number
}
