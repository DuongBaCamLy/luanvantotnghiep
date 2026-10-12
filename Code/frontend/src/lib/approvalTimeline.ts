import type { ApprovalHistoryItem, ApprovalStep } from "@/types/approval"

export function approvalStageLabel(step: ApprovalStep | null): string {
  switch (step) {
    case "STEP1_DEPT_HEAD": return "Department Head"
    case "STEP3_DEAN": return "Dean / Dean Secretary"
    case "STEP2_DEAN_SECRETARY": return "Dean / Dean Secretary · legacy step"
    case "STEP2_PROG_COORDINATOR": return "Program Coordinator · legacy step"
    default: return "Review level not recorded"
  }
}

export interface ApprovalTimelineEvent {
  key: string
  kind: "submission" | "decision" | "pending"
  item: ApprovalHistoryItem
  timestamp: string | null
  actor: string | null
}

export function buildApprovalTimeline(
  history: ApprovalHistoryItem[],
  versionNumber: number,
  status = "all",
): ApprovalTimelineEvent[] {
  const events: ApprovalTimelineEvent[] = []
  for (const item of history) {
    if (item.versionNumber !== versionNumber || (status !== "all" && item.status !== status)) continue
    events.push({
      key: `${item.id}-submission`, kind: "submission", item,
      timestamp: item.createdAt, actor: item.requestedByUsername,
    })
    const pending = item.status === "PENDING"
    // No decision is invented for incomplete historical records.
    if (pending || item.resolvedAt || item.status) {
      events.push({
        key: `${item.id}-${pending ? "pending" : "decision"}`,
        kind: pending ? "pending" : "decision", item,
        timestamp: pending ? null : item.resolvedAt,
        actor: pending ? null : item.reviewedByUsername,
      })
    }
  }
  const order = (event: ApprovalTimelineEvent) => {
    if (!event.timestamp) return Number.POSITIVE_INFINITY
    const time = Date.parse(event.timestamp)
    return Number.isNaN(time) ? Number.POSITIVE_INFINITY : time
  }
  return events.sort((left, right) => order(left) - order(right))
}

