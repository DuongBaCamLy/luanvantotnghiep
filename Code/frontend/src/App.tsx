import type { ReactElement } from "react"
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import LoginPage from "@/pages/LoginPage"
import ForgotPasswordPage from "@/pages/ForgotPasswordPage"
import ResetPasswordPage from "@/pages/ResetPasswordPage"
import DashboardPlaceholder from "@/pages/DashboardPlaceholder"
import UserManagementPage from "@/pages/admin/UserManagementPage"
import AdminDashboardPage from "@/pages/admin/AdminDashboardPage"
import DepartmentManagementPage from "@/pages/admin/DepartmentManagementPage"
import ProgramManagementPage from "@/pages/admin/ProgramManagementPage"
import CourseManagementPage from "@/pages/admin/CourseManagementPage"
import ClassSectionManagementPage from "@/pages/admin/ClassSectionManagementPage"
import AuditLogPage from "@/pages/admin/AuditLogPage"
import SystemSettingsPage from "@/pages/admin/SystemSettingsPage"
import InstructorManagementPage from "@/pages/admin/InstructorManagementPage"
import ReportManagementPage from "@/pages/admin/ReportManagementPage"
import PloManagementPage from "@/pages/admin/PloManagementPage"
import CloPloHeatmapPage from "@/pages/admin/CloPloHeatmapPage"
import ProtectedRoute from "@/routes/ProtectedRoute"
import DashboardLayout from "@/components/layout/DashboardLayout"
import { useAuthStore } from "@/store/authStore"
import { NAV_CONFIG } from "@/config/navConfig"
import type { UserRole } from "@/types/auth"
import SyllabusListPage from "@/pages/syllabus/SyllabusListPage"
import InstructorAssignmentsPage from "@/pages/syllabus/InstructorAssignmentsPage"
import SyllabusDetailPage from "@/pages/syllabus/SyllabusDetailPage"
import SyllabusEditPage from "@/pages/syllabus/SyllabusEditPage"
import SyllabusEditorPage from "@/pages/syllabus/SyllabusEditorPage"
import SyllabusCreatePage from "@/pages/syllabus/SyllabusCreatePage"
import SyllabusDiffPage from "@/pages/syllabus/SyllabusDiffPage"
import CohortSyllabusDiffPage from "@/pages/syllabus/CohortSyllabusDiffPage"
import ProgramDiffPage from "@/pages/program/ProgramDiffPage"
import ProgramTimelinePage from "@/pages/program/ProgramTimelinePage"
import CurriculumTimelinePage from "@/pages/program/CurriculumTimelinePage"
import ProgramCurriculumPage from "@/pages/program/ProgramCurriculumPage"
import SyllabusCurriculumMapPage from "@/pages/program/SyllabusCurriculumMapPage"
import DeanDashboardPage from "@/pages/admin/DeanDashboardPage"
import DeptHeadDashboardPage from "@/pages/admin/DeptHeadDashboardPage"
import DeptHeadCoursesPage from "@/pages/admin/DeptHeadCoursesPage"
import DeptHeadApprovalPage from "@/pages/admin/DeptHeadApprovalPage"
import FacultyDashboardPage from "@/pages/admin/FacultyDashboardPage"
import EmailOutboxPage from "@/pages/admin/EmailOutboxPage"
import EscalationCenterPage from "@/pages/admin/EscalationCenterPage"
import CreateCurriculumPage from "@/pages/admin/CreateCurriculumPage"

const ROLE_HOME: Record<UserRole, string> = {
  ADMIN: "/admin",
  DEAN: "/dean",
  DEPT_HEAD: "/dept-head",
  INSTRUCTOR: "/instructor",
}

function RootRedirect() {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />
  return <Navigate to={ROLE_HOME[user.role] ?? "/login"} replace />
}

const PAGE_OVERRIDES: Record<string, React.ComponentType> = {

  "/admin": AdminDashboardPage,
  "/admin/users": UserManagementPage,
  "/admin/departments": DepartmentManagementPage,
  "/admin/programs": ProgramManagementPage,
  "/admin/plo": PloManagementPage,
  "/admin/clo-plo-heatmap": CloPloHeatmapPage,
  "/admin/courses": CourseManagementPage,
  "/admin/class-sections": ClassSectionManagementPage,
  "/admin/instructors": InstructorManagementPage,
  "/admin/reports": ReportManagementPage,
  "/admin/audit-log": AuditLogPage,
  "/admin/settings": SystemSettingsPage,
  "/admin/email-outbox": EmailOutboxPage,
  "/admin/escalations": EscalationCenterPage,
  "/admin/syllabus": SyllabusListPage,
  "/admin/syllabus/diff": CohortSyllabusDiffPage,
  "/admin/syllabus/curriculum-map": SyllabusCurriculumMapPage,

  "/dean": DeanDashboardPage,
  "/dean/programs": ProgramManagementPage,
  "/dean/plo": PloManagementPage,
  "/dean/clo-plo-heatmap": CloPloHeatmapPage,

  "/dean/syllabus": SyllabusListPage,
  "/dean/syllabus/curriculum-map": SyllabusCurriculumMapPage,

  "/dean/reports": ReportManagementPage,
  "/dean/approvals": DeptHeadApprovalPage,
  "/dept-head": DeptHeadDashboardPage,
  "/dept-head/courses": DeptHeadCoursesPage,
  "/dept-head/approvals": DeptHeadApprovalPage,
  "/dept-head/syllabus": SyllabusListPage,
  "/dept-head/syllabus/diff": CohortSyllabusDiffPage,
  "/dept-head/syllabus/curriculum-map": SyllabusCurriculumMapPage,
  "/instructor": FacultyDashboardPage,
  "/instructor/syllabus": SyllabusListPage,
  "/instructor/class-sections": InstructorAssignmentsPage,
}

function buildRoleRoutes(role: UserRole) {
  const routes: ReactElement[] = []

  NAV_CONFIG[role].forEach((item) => {
    // Add parent route
    const Component = PAGE_OVERRIDES[item.path]
    routes.push(
      <Route
        key={item.path}
        path={item.path}
        element={
          Component ? (
            <Component />
          ) : (
            <DashboardPlaceholder title={item.label} />
          )
        }
      />
    )

    // Add children routes
    if (item.children) {
      item.children.forEach((child) => {
        // Skip if child path is same as parent (already added)
        if (child.path === item.path) return

        const ChildComponent = PAGE_OVERRIDES[child.path]
        routes.push(
          <Route
            key={child.path}
            path={child.path}
            element={
              ChildComponent ? (
                <ChildComponent />
              ) : (
                <DashboardPlaceholder title={child.label} />
              )
            }
          />
        )
      })
    }
  })

  return routes
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<DashboardLayout />}>
            <Route
              path="/curriculum-map"
              element={<SyllabusCurriculumMapPage />}
            />
            <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
              {buildRoleRoutes("ADMIN")}
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["DEAN", "ADMIN"]} />}>
              {buildRoleRoutes("DEAN")}
            </Route>

            <Route element={<ProtectedRoute allowedRoles={["DEPT_HEAD", "ADMIN"]} />}>
              {buildRoleRoutes("DEPT_HEAD")}
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["INSTRUCTOR", "ADMIN"]} />}>
              {buildRoleRoutes("INSTRUCTOR")}
            </Route>
            <Route
              path="/dean/syllabus/:id"
              element={<SyllabusDetailPage />}
            />

            <Route
              path="/dean/syllabus/:id/diff"
              element={<SyllabusDiffPage />}
            />
            <Route
              path="/dept-head/syllabus/:id"
              element={<SyllabusDetailPage />}
            />

            <Route
              path="/dept-head/syllabus/:id/diff"
              element={<SyllabusDiffPage />}
            />

            {/* Instructor Syllabus Routes */}
            <Route
              path="/instructor/syllabus/create"
              element={<SyllabusCreatePage />}
            />
            <Route
              path="/instructor/syllabus/:id"
              element={<SyllabusDetailPage />}
            />
            <Route
              path="/instructor/syllabus/:id/edit"
              element={<SyllabusEditPage />}
            />
            <Route
              path="/instructor/syllabus/:id/editor"
              element={<SyllabusEditorPage />}
            />
            <Route
              path="/instructor/syllabus/:id/diff"
              element={<SyllabusDiffPage />}
            />

            {/* Admin Syllabus Routes */}
            <Route
              path="/admin/syllabus/:id"
              element={<SyllabusDetailPage />}
            />
            <Route
              path="/admin/syllabus/:id/edit"
              element={<SyllabusEditPage />}
            />
            <Route
              path="/admin/syllabus/:id/editor"
              element={<SyllabusEditorPage />}
            />
            <Route
              path="/admin/syllabus/:id/diff"
              element={<SyllabusDiffPage />}
            />
            <Route
              path="/admin/syllabus/create"
              element={<SyllabusCreatePage />}
            />
            <Route
              path="/admin/programs/:id/diff"
              element={<ProgramDiffPage />}
            />
            <Route
              path="/dean/programs/:id/diff"
              element={<ProgramDiffPage />}
            />
            <Route
              path="/admin/programs/:id/curriculum"
              element={<ProgramCurriculumPage />}
            />
            <Route path="/admin/curricula/create" element={<CreateCurriculumPage />} />

            <Route
              path="/admin/programs/:id/timeline"
              element={<ProgramTimelinePage />}
            />
            <Route
              path="/dean/programs/:id/timeline"
              element={<ProgramTimelinePage />}
            />
            <Route
              path="/admin/programs/:id/history"
              element={<CurriculumTimelinePage />}
            />

            <Route
              path="/dean/programs/:id/history"
              element={<CurriculumTimelinePage />}
            />

            {/* Trang chi tiết CTĐT đã bỏ; URL cũ quay về danh sách CTĐT */}
            <Route
              path="/admin/programs/:id"
              element={<Navigate to="/admin/programs" replace />}
            />
            <Route
              path="/dean/programs/:id"
              element={<Navigate to="/dean/programs" replace />}
            />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />


      </Routes>
    </BrowserRouter>
  )
}