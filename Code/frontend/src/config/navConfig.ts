import type { UserRole } from "@/types/auth"
import { ROLE_LABELS } from "@/i18n/labels"
import { t } from "@/i18n"
import {
  LayoutDashboard,
  BookOpen,
  GraduationCap,
  Users,
  ClipboardCheck,
  FileText,
  Target,
  Layers,
  CalendarDays,
  UserCog,
  History,
  FileDown,
  Settings,
  MailCheck,
  ShieldAlert,
  type LucideIcon,
} from "lucide-react"

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  children?: { label: string; path: string }[]
}

export const NAV_CONFIG: Record<UserRole, NavItem[]> = {
  ADMIN: [
    { label: t("nav.dashboard"), path: "/admin", icon: LayoutDashboard },
    { label: t("nav.users"), path: "/admin/users", icon: UserCog },
    { label: t("nav.programs"), path: "/admin/programs", icon: GraduationCap },
    { label: t("nav.plo"), path: "/admin/plo", icon: Target },
    { label: t("nav.cloPloHeatmap"), path: "/admin/clo-plo-heatmap", icon: Layers },
    {
      label: t("nav.syllabusManagement"),
      path: "/admin/syllabus",
      icon: FileText,
      children: [
        { label: t("nav.syllabusCatalog"), path: "/admin/syllabus" },
        { label: t("nav.curriculumMap"), path: "/admin/syllabus/curriculum-map" },
        { label: t("nav.versionComparison"), path: "/admin/syllabus/diff" },
      ],
    },
    { label: t("nav.instructors"), path: "/admin/instructors", icon: Users },
    { label: t("nav.teachingAssignments"), path: "/admin/class-sections", icon: CalendarDays },
    { label: t("nav.reports"), path: "/admin/reports", icon: FileDown },
    { label: t("nav.auditLog"), path: "/admin/audit-log", icon: History },
    { label: t("nav.emailNotifications"), path: "/admin/email-outbox", icon: MailCheck },
    { label: t("nav.overdueEscalations"), path: "/admin/escalations", icon: ShieldAlert },
    { label: t("nav.systemSettings"), path: "/admin/settings", icon: Settings },

    // ADMIN is the system super-user. These entries expose the role-specific
    // workspaces as well, while backend authorization still remains the source
    // of truth for permissions.
    { label: "Dean Dashboard", path: "/dean", icon: LayoutDashboard },
    { label: "Dean Approvals", path: "/dean/approvals", icon: ClipboardCheck },
    { label: "Dean Syllabus", path: "/dean/syllabus", icon: FileText },
    { label: "Dean Curriculum Map", path: "/dean/syllabus/curriculum-map", icon: Layers },
    { label: "Dean Reports", path: "/dean/reports", icon: FileDown },
    { label: "Department Head Dashboard", path: "/dept-head", icon: LayoutDashboard },
    { label: "Department Courses", path: "/dept-head/courses", icon: BookOpen },
    { label: "Department Approvals", path: "/dept-head/approvals", icon: ClipboardCheck },
    { label: "Department Syllabus", path: "/dept-head/syllabus", icon: FileText },
    { label: "Instructor Dashboard", path: "/instructor", icon: LayoutDashboard },
    { label: "Instructor Syllabus", path: "/instructor/syllabus", icon: FileText },
    { label: "Teaching Assignments", path: "/instructor/class-sections", icon: CalendarDays },
  ],

  DEAN: [
    { label: t("nav.dashboard"), path: "/dean", icon: LayoutDashboard },
    { label: t("nav.programs"), path: "/dean/programs", icon: GraduationCap },
    { label: t("nav.plo"), path: "/dean/plo", icon: Target },
    { label: t("nav.cloPloHeatmap"), path: "/dean/clo-plo-heatmap", icon: Layers },
    {
      label: t("nav.syllabusManagement"),
      path: "/dean/syllabus",
      icon: FileText,
      children: [
        { label: t("nav.syllabusCatalog"), path: "/dean/syllabus" },
        { label: t("nav.curriculumMap"), path: "/dean/syllabus/curriculum-map" },
      ],
    },
    { label: t("nav.reports"), path: "/dean/reports", icon: FileDown },
    { label: t("nav.approvals"), path: "/dean/approvals", icon: ClipboardCheck },
  ],

  DEPT_HEAD: [
    { label: t("nav.dashboard"), path: "/dept-head", icon: LayoutDashboard },
    { label: t("nav.courses"), path: "/dept-head/courses", icon: BookOpen },
    {
      label: t("nav.syllabusManagement"),
      path: "/dept-head/syllabus",
      icon: FileText,
      children: [
        { label: t("nav.syllabusCatalog"), path: "/dept-head/syllabus" },
        { label: t("nav.curriculumMap"), path: "/dept-head/syllabus/curriculum-map" },
        { label: t("nav.versionComparison"), path: "/dept-head/syllabus/diff" },
      ],
    },
    { label: t("nav.approvals"), path: "/dept-head/approvals", icon: ClipboardCheck },
  ],

  INSTRUCTOR: [
    { label: t("nav.dashboard"), path: "/instructor", icon: LayoutDashboard },
    { label: t("nav.mySyllabuses"), path: "/instructor/syllabus", icon: FileText },
    { label: t("nav.classSections"), path: "/instructor/class-sections", icon: CalendarDays },
  ],

}

export const ROLE_LABEL: Record<UserRole, string> = ROLE_LABELS