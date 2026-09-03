import type { UserRole } from "@/types/auth"
import { ROLE_LABELS } from "@/i18n/labels"
import {
  ClipboardCheck, FileDown, FileText, GraduationCap, History,
  LayoutDashboard, Layers, MailCheck, Settings, ShieldAlert, Target,
  UserCog, type LucideIcon,
} from "lucide-react"

/** Phase permission model: all roles share the application; only three modules are admin-only. */
export type Permission = "APP_ACCESS" | "MANAGE_USERS" | "VIEW_REPORTS" | "VIEW_AUDIT"
const COMMON_PERMISSIONS: ReadonlySet<Permission> = new Set(["APP_ACCESS"])
export const ROLE_PERMISSIONS: Record<UserRole, ReadonlySet<Permission>> = {
  ADMIN: new Set(["APP_ACCESS", "MANAGE_USERS", "VIEW_REPORTS", "VIEW_AUDIT"]),
  INSTRUCTOR: COMMON_PERMISSIONS,
  DEAN: COMMON_PERMISSIONS,
  DEPT_HEAD: COMMON_PERMISSIONS,
}
export const hasPermission = (role: UserRole | undefined, permission: Permission) =>
  Boolean(role && ROLE_PERMISSIONS[role].has(permission))

export interface NavItem { label: string; path: string; icon: LucideIcon; permission: Permission; children?: Array<{ label: string; path: string }> }
export interface NavGroup { label: string; items: NavItem[] }
const prefixFor = (role: UserRole) => `/${role.toLowerCase().replace("dept_head", "dept-head")}`

const buildNav = (role: UserRole): NavGroup[] => {
  const root = prefixFor(role)
  const app = "APP_ACCESS" as const
  return [
    { label: "Main", items: [{ label: "Dashboard", path: root, icon: LayoutDashboard, permission: app }] },
    { label: "Administration", items: [
      ...(role === "ADMIN" ? [{ label: "User Management", path: `${root}/users`, icon: UserCog, permission: "MANAGE_USERS" as const }] : []),
      { label: role === "DEPT_HEAD" ? "Managed Major Courses" : "Teaching Assignments", path: `${root}/class-sections`, icon: ClipboardCheck, permission: app },
    ] },
    { label: "Curriculum", items: [
      { label: "Curriculum Programs", path: `${root}/programs`, icon: GraduationCap, permission: app },
      ...(role !== "INSTRUCTOR" ? [
        { label: "PLO Management", path: `${root}/plo`, icon: Target, permission: app },
        { label: "CLO–PLO Heatmap", path: `${root}/clo-plo-heatmap`, icon: Layers, permission: app },
      ] : []),
    ] },
    { label: "Syllabus", items: [
      { label: "Syllabus Management", path: `${root}/syllabus`, icon: FileText, permission: app, children: [{ label: "Syllabus Catalog", path: `${root}/syllabus` }] },
      ...(role === "DEPT_HEAD" || role === "DEAN" ? [{ label: role === "DEAN" ? "Final Review Queue" : "Review Queue", path: `${root}/approvals`, icon: ClipboardCheck, permission: app }] : []),
    ] },
    ...(role !== "INSTRUCTOR" ? [{ label: "Workflow", items: [
      { label: "Email Notifications", path: `${root}/email-outbox`, icon: MailCheck, permission: app },
      { label: "Overdue Escalations", path: `${root}/escalations`, icon: ShieldAlert, permission: app },
    ] }] : []),
    ...(role === "ADMIN" ? [{ label: "Reports", items: [{ label: "Reports", path: `${root}/reports`, icon: FileDown, permission: "VIEW_REPORTS" as const }] }] : []),
    { label: "System", items: [
      ...(role === "ADMIN" ? [{ label: "Audit Log", path: `${root}/audit-log`, icon: History, permission: "VIEW_AUDIT" as const }] : []),
      ...(role !== "INSTRUCTOR" ? [{ label: "System Settings", path: `${root}/settings`, icon: Settings, permission: app }] : []),
    ] },
  ]
}

export const NAV_CONFIG: Record<UserRole, NavGroup[]> = {
  ADMIN: buildNav("ADMIN"), INSTRUCTOR: buildNav("INSTRUCTOR"),
  DEAN: buildNav("DEAN"), DEPT_HEAD: buildNav("DEPT_HEAD"),
}
export const getRoleNav = (role: UserRole) => NAV_CONFIG[role]
  .map((group) => ({ ...group, items: group.items.filter((item) => hasPermission(role, item.permission)) }))
  .filter((group) => group.items.length > 0)
export const ROLE_LABEL: Record<UserRole, string> = { ...ROLE_LABELS, INSTRUCTOR: "Faculty" }
