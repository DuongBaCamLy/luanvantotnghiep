import { t } from "@/i18n"
import type { UserRole } from "@/types/auth"
import type {
  ApprovalStatus,
  ApprovalStep,
} from "@/types/approval"

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: t("role.admin"),
  DEAN: t("role.dean"),
  DEPT_HEAD: t("role.departmentHead"),
  INSTRUCTOR: t("role.instructor"),
}

export const APPROVAL_STATUS_LABELS: Record<ApprovalStatus, string> = {
  PENDING: t("approval.status.pending"),
  APPROVED: t("approval.status.approved"),
  REJECTED: t("approval.status.rejected"),
  REVISION_REQUESTED: t("approval.status.revisionRequested"),
}

export const APPROVAL_STEP_LABELS: Record<ApprovalStep, string> = {
  STEP1_DEPT_HEAD: t("approval.step.departmentHead"),
  STEP2_PROG_COORDINATOR: t("approval.step.programCoordinator"),
  STEP3_DEAN: t("approval.step.dean"),
}

const SYLLABUS_STATUS_LABELS: Record<string, string> = {
  DRAFT: t("syllabus.status.draft"),
  SUBMITTED: t("syllabus.status.submitted"),
  UNDER_REVIEW: t("syllabus.status.underReview"),
  APPROVED: t("syllabus.status.approved"),
  REJECTED: t("syllabus.status.rejected"),
  REVISION_REQUESTED: t("syllabus.status.revisionRequested"),
  ARCHIVED: t("syllabus.status.archived"),
}

export function getSyllabusStatusLabel(status: string): string {
  return SYLLABUS_STATUS_LABELS[status] ?? status
}