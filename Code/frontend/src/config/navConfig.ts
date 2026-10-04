import type { UserRole } from "@/types/auth"
import { ROLE_LABELS } from "@/i18n/labels"

import {
  ClipboardCheck,
  FileText,
  GraduationCap,
  History,
  LayoutDashboard,
  Layers,
  Settings,
  Target,
  UserCog,
  type LucideIcon,
} from "lucide-react"

/**
 * Phase permission model:
 * all roles share the application;
 * only restricted modules require explicit permissions.
 */
export type Permission =
  | "APP_ACCESS"
  | "MANAGE_USERS"
  | "VIEW_REPORTS"
  | "VIEW_AUDIT"

const COMMON_PERMISSIONS: ReadonlySet<Permission> =
  new Set([
    "APP_ACCESS",
  ])

export const ROLE_PERMISSIONS: Record<
  UserRole,
  ReadonlySet<Permission>
> = {
  ADMIN: new Set([
    "APP_ACCESS",
    "MANAGE_USERS",
    "VIEW_REPORTS",
    "VIEW_AUDIT",
  ]),
DEAN_SECRETARY: new Set([
  "APP_ACCESS",
  "VIEW_REPORTS",
]),
  DEAN: new Set([
    "APP_ACCESS",
    "VIEW_REPORTS",
  ]),

  DEPT_HEAD: COMMON_PERMISSIONS,

  INSTRUCTOR: COMMON_PERMISSIONS,
}

export const hasPermission = (
  role: UserRole | undefined,
  permission: Permission,
) =>
  Boolean(
    role
    && ROLE_PERMISSIONS[role].has(
      permission,
    ),
  )

export interface NavItem {
  label: string
  path: string
  icon: LucideIcon
  permission: Permission
  children?: Array<{
    label: string
    path: string
  }>
}

export interface NavGroup {
  label: string
  items: NavItem[]
}

export const prefixFor = (
  role: UserRole,
) => {
  if (role === "DEAN_SECRETARY") {
    return "/dean"
  }

  return `/${role
    .toLowerCase()
    .replace(
      "dept_head",
      "dept-head",
    )}`
}

const buildNav = (
  role: UserRole,
): NavGroup[] => {
  const root =
    prefixFor(role)

  const app =
    "APP_ACCESS" as const

  return [
    /*
     * =====================================================
     * MAIN
     * =====================================================
     */
    {
      label: "Main",
      items: [
        {
          label: "Dashboard",
          path: root,
          icon: LayoutDashboard,
          permission: app,
        },
      ],
    },

    /*
     * =====================================================
     * ADMINISTRATION
     * =====================================================
     */
    {
      label: "Administration",
      items: [
        /*
         * ADMIN
         */
        ...(role === "ADMIN"
          ? [
              {
                label:
                  "User Management",
                path:
                  `${root}/users`,
                icon:
                  UserCog,
                permission:
                  "MANAGE_USERS" as const,
              },


              {
                label:
                  "Teaching Assignments",
                path:
                  `${root}/class-sections`,
                icon:
                  ClipboardCheck,
                permission:
                  app,
              },
            ]
          : []),

        /*
         * INSTRUCTOR
         */
        ...(role === "INSTRUCTOR"
          ? [
              {
                label:
                  "My Courses",
                path:
                  `${root}/class-sections`,
                icon:
                  ClipboardCheck,
                permission:
                  app,
              },
            ]
          : []),

        /*
         * HEAD OF DEPARTMENT
         */
        ...(role === "DEPT_HEAD"
          ? [
              {
                label:
                  "Managed Major Courses",
                path:
                  `${root}/class-sections`,
                icon:
                  ClipboardCheck,
                permission:
                  app,
              },
            ]
          : []),
      ],
    },

    /*
     * =====================================================
     * CURRICULUM
     * =====================================================
     */
    {
      label: "Curriculum",
      items: [
        {
          label:
            "Curriculum Programs",
          path:
            `${root}/programs`,
          icon:
            GraduationCap,
          permission:
            app,
        },

        ...(role === "ADMIN"
          ? [
              {
                label:
                  "Curriculum Archive",
                path:
                  `${root}/curriculum-archive`,
                icon:
                  History,
                permission:
                  app,
              },
            ]
          : []),

        /*
         * PLO Management:
         * ADMIN + DEAN only.
         */
        ...(role === "ADMIN"
|| role === "DEAN"
|| role === "DEAN_SECRETARY"
          ? [
              {
                label:
                  "PLO Management",
                path:
                  `${root}/plo`,
                icon:
                  Target,
                permission:
                  app,
              },
            ]
          : []),


/*
 * CLO–PLO Heatmap:
 * available to all application roles.
 */
{
  label: "CLO–PLO Heatmap",
  path: `${root}/clo-plo-heatmap`,
  icon: Layers,
  permission: app,
},
      ],
    },

    /*
     * =====================================================
     * SYLLABUS
     * =====================================================
     */
    {
      label: "Syllabus",
      items: [
        {
          label:
            role === "INSTRUCTOR"
              ? "Syllabus Catalog"
              : "Syllabus Management",

          path:
            `${root}/syllabus`,

          icon:
            FileText,

          permission:
            app,

          children: [
            {
              label:
                "Syllabus Catalog",
              path:
                `${root}/syllabus`,
            },
          ],
        },

        /*
         * Review workflow:
         * Department Head + Dean.
         */
        ...(role === "DEPT_HEAD"
|| role === "DEAN"
|| role === "DEAN_SECRETARY"
          ? [
              {
                label:
                  role === "DEAN"
|| role === "DEAN_SECRETARY"
  ? "Final Review Queue"
  : "Review Queue",

                path:
                  `${root}/approvals`,

                icon:
                  ClipboardCheck,

                permission:
                  app,
              },
            ]
          : []),
      ],
    },

    /*
     * =====================================================
     * SYSTEM
     * =====================================================
     */
    {
      label: "System",

      items: [
        ...(role === "ADMIN"
          ? [
              {
                label:
                  "Audit Log",
                path:
                  `${root}/audit-log`,
                icon:
                  History,
                permission:
                  "VIEW_AUDIT" as const,
              },

              {
                label:
                  "System Settings",
                path:
                  `${root}/settings`,
                icon:
                  Settings,
                permission:
                  app,
              },
            ]
          : []),
      ],
    },
  ]
}

/*
 * =========================================================
 * ROLE NAVIGATION CONFIGURATION
 * =========================================================
 */

export const NAV_CONFIG: Record<
  UserRole,
  NavGroup[]
> = {
  ADMIN:
    buildNav("ADMIN"),

  INSTRUCTOR:
    buildNav("INSTRUCTOR"),

  DEAN:
    buildNav("DEAN"),

  DEPT_HEAD:
    buildNav("DEPT_HEAD"),
    DEAN_SECRETARY:
  buildNav("DEAN_SECRETARY"),
}

/*
 * Return only navigation items
 * that the current role is allowed to access.
 *
 * Empty groups are removed automatically.
 */
export const getRoleNav = (
  role: UserRole,
) =>
  NAV_CONFIG[role]
    .map(
      (group) => ({
        ...group,

        items:
          group.items.filter(
            (item) =>
              hasPermission(
                role,
                item.permission,
              ),
          ),
      }),
    )
    .filter(
      (group) =>
        group.items.length > 0,
    )

/*
 * User-facing role labels.
 */
export const ROLE_LABEL: Record<
  UserRole,
  string
> = {
  ...ROLE_LABELS,

  INSTRUCTOR:
    "Faculty",
}
