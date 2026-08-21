import { api } from "./axios"

export type DeadlineState = "UPCOMING" | "DUE_TODAY" | "OVERDUE" | "INACTIVE"

export interface SyllabusDeadline {
  id: number
  academicYear: string
  semester: number
  deadlineAt: string
  reminderDays: number[]
  escalationDays: number[]
  active: boolean
  revision: number
  daysRemaining: number
  state: DeadlineState
  createdByUsername?: string | null
  updatedByUsername?: string | null
  createdAt: string
  updatedAt: string
}

export interface UpsertSyllabusDeadlineRequest {
  academicYear: string
  semester: number
  deadlineAt: string
  reminderDays: number[]
  escalationDays: number[]
  active: boolean
}

export interface DeadlinePreview {
  deadline: SyllabusDeadline
  recipientCount: number
  missingCourseCount: number
  skippedWithoutUserAccount: number
  recipients: {
    userId: number
    username: string
    email: string
    instructorId: number
    instructorName: string
    missingCourses: {
      courseId: number
      courseCode: string
      courseName: string
    }[]
  }[]
}

export interface DeadlineDispatchResult {
  deadlineId: number
  deadlineRevision: number
  daysRemaining: number
  forced: boolean
  reminderDue: boolean
  reminderDay?: number | null
  recipientCount: number
  missingCourseCount: number
  notificationsQueued: number
  skippedAlreadySent: number
  failedDeliveries: number
  skippedWithoutUserAccount: number
  message: string
}

export interface DeadlineReminderLog {
  id: number
  deadlineRevision: number
  recipientUserId: number
  recipientUsername: string
  recipientEmail: string
  daysBefore: number
  missingCourseCount: number
  missingCourseCodes: string
  notificationId?: number | null
  emailQueued: boolean
  createdAt: string
}

export interface DeadlineEscalationPreview {
  deadline: SyllabusDeadline
  overdue: boolean
  daysOverdue: number
  recipientCount: number
  departmentCount: number
  overdueInstructorCount: number
  missingCourseCount: number
  departmentsWithoutHead: number
  missingDean: boolean
  departments: {
    departmentId: number
    departmentCode: string
    departmentName: string
    overdueInstructorCount: number
    missingCourseCount: number
    departmentHeads: {
      userId: number
      username: string
      email: string
    }[]
    overdueInstructors: {
      instructorId: number
      instructorName: string
      instructorEmail: string
      missingCourses: {
        courseId: number
        courseCode: string
        courseName: string
      }[]
    }[]
  }[]
  recipients: {
    userId: number
    username: string
    email: string
    role: "DEPT_HEAD" | "DEAN"
    scopeKey: string
    scopeLabel: string
    overdueInstructorCount: number
    missingCourseCount: number
  }[]
}

export interface DeadlineEscalationDispatchResult {
  deadlineId: number
  deadlineRevision: number
  daysOverdue: number
  forced: boolean
  escalationDue: boolean
  escalationDay?: number | null
  recipientCount: number
  departmentCount: number
  overdueInstructorCount: number
  missingCourseCount: number
  notificationsQueued: number
  skippedAlreadySent: number
  failedDeliveries: number
  departmentsWithoutHead: number
  missingDean: boolean
  message: string
}

export interface DeadlineEscalationLog {
  id: number
  deadlineRevision: number
  recipientUserId: number
  recipientUsername: string
  recipientEmail: string
  recipientRole: "DEPT_HEAD" | "DEAN"
  scopeKey: string
  departmentId?: number | null
  departmentCode?: string | null
  departmentName?: string | null
  escalationDay: number
  actualDaysOverdue: number
  overdueInstructorCount: number
  missingCourseCount: number
  instructorNames: string
  missingCourseCodes: string
  notificationId?: number | null
  emailQueued: boolean
  createdAt: string
}

export const syllabusDeadlineApi = {
  getAll: () =>
    api
      .get<SyllabusDeadline[]>("/api/admin/syllabus-deadlines")
      .then((response) => response.data),

  create: (request: UpsertSyllabusDeadlineRequest) =>
    api
      .post<SyllabusDeadline>("/api/admin/syllabus-deadlines", request)
      .then((response) => response.data),

  update: (id: number, request: UpsertSyllabusDeadlineRequest) =>
    api
      .put<SyllabusDeadline>(`/api/admin/syllabus-deadlines/${id}`, request)
      .then((response) => response.data),

  setActive: (id: number, active: boolean) =>
    api
      .patch<SyllabusDeadline>(
        `/api/admin/syllabus-deadlines/${id}/active`,
        undefined,
        { params: { active } },
      )
      .then((response) => response.data),

  preview: (id: number) =>
    api
      .get<DeadlinePreview>(`/api/admin/syllabus-deadlines/${id}/preview`)
      .then((response) => response.data),

  dispatchNow: (id: number, force = false) =>
    api
      .post<DeadlineDispatchResult>(
        `/api/admin/syllabus-deadlines/${id}/dispatch-now`,
        undefined,
        { params: { force } },
      )
      .then((response) => response.data),

  getLogs: (id: number) =>
    api
      .get<DeadlineReminderLog[]>(`/api/admin/syllabus-deadlines/${id}/logs`)
      .then((response) => response.data),

  getEscalationPreview: (id: number) =>
    api
      .get<DeadlineEscalationPreview>(
        `/api/admin/syllabus-deadlines/${id}/escalation-preview`,
      )
      .then((response) => response.data),

  dispatchEscalation: (id: number, force = false) =>
    api
      .post<DeadlineEscalationDispatchResult>(
        `/api/admin/syllabus-deadlines/${id}/escalate-now`,
        undefined,
        { params: { force } },
      )
      .then((response) => response.data),

  getEscalationLogs: (id: number) =>
    api
      .get<DeadlineEscalationLog[]>(
        `/api/admin/syllabus-deadlines/${id}/escalation-logs`,
      )
      .then((response) => response.data),
}
