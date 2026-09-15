import { BrowserRouter, Navigate, Outlet, Route, Routes } from "react-router-dom"
import type { UserRole } from "@/types/auth"
import { useAuthStore } from "@/store/authStore"
import ProtectedRoute from "@/routes/ProtectedRoute"
import DashboardLayout from "@/components/layout/DashboardLayout"
import LoginPage from "@/pages/LoginPage"
import ForgotPasswordPage from "@/pages/ForgotPasswordPage"
import ResetPasswordPage from "@/pages/ResetPasswordPage"
import AdminDashboardPage from "@/pages/admin/AdminDashboardPage"
import FacultyDashboardPage from "@/pages/admin/FacultyDashboardPage"
import UserManagementPage from "@/pages/admin/UserManagementPage"
import ProgramManagementPage from "@/pages/admin/ProgramManagementPage"
import ClassSectionManagementPage from "@/pages/admin/ClassSectionManagementPage"
import InstructorAssignmentsPage from "@/pages/syllabus/InstructorAssignmentsPage"
import ReportManagementPage from "@/pages/admin/ReportManagementPage"
import AuditLogPage from "@/pages/admin/AuditLogPage"
import SystemSettingsPage from "@/pages/admin/SystemSettingsPage"
import PloManagementPage from "@/pages/admin/PloManagementPage"
import CloPloHeatmapPage from "@/pages/admin/CloPloHeatmapPage"
import EmailOutboxPage from "@/pages/admin/EmailOutboxPage"
import EscalationCenterPage from "@/pages/admin/EscalationCenterPage"
import CreateCurriculumPage from "@/pages/admin/CreateCurriculumPage"
import SyllabusListPage from "@/pages/syllabus/SyllabusListPage"
import SyllabusDetailPage from "@/pages/syllabus/SyllabusDetailPage"
import SyllabusEditPage from "@/pages/syllabus/SyllabusEditPage"
import SyllabusCreatePage from "@/pages/syllabus/SyllabusCreatePage"
import SyllabusDiffPage from "@/pages/syllabus/SyllabusDiffPage"
import CohortSyllabusDiffPage from "@/pages/syllabus/CohortSyllabusDiffPage"
import SyllabusImportPage from "@/pages/syllabus/SyllabusImportPage"
import ProgramDiffPage from "@/pages/program/ProgramDiffPage"
import ProgramTimelinePage from "@/pages/program/ProgramTimelinePage"
import CurriculumTimelinePage from "@/pages/program/CurriculumTimelinePage"
import ProgramCurriculumPage from "@/pages/program/ProgramCurriculumPage"
import SyllabusCurriculumMapPage from "@/pages/program/SyllabusCurriculumMapPage"
import DeptHeadDashboardPage from "@/pages/admin/DeptHeadDashboardPage"
import DeanDashboardPage from "@/pages/admin/DeanDashboardPage"
import DeptHeadCoursesPage from "@/pages/admin/DeptHeadCoursesPage"
import DeptHeadApprovalPage from "@/pages/admin/DeptHeadApprovalPage"

const ROLE_HOME: Record<UserRole, string> = {
  ADMIN: "/admin", DEAN: "/dean", DEPT_HEAD: "/dept-head", INSTRUCTOR: "/instructor",
}

function RootRedirect() {
  const { isAuthenticated, user } = useAuthStore()
  return <Navigate to={isAuthenticated && user ? ROLE_HOME[user.role] : "/login"} replace />
}

/** Keeps every role inside its own URL namespace while reusing the exact same pages. */
function OwnWorkspace() {
  const role = useAuthStore((state) => state.user?.role)
  const expected = role ? ROLE_HOME[role].slice(1) : ""
  const actual = window.location.pathname.split("/")[1]
  return expected === actual ? <Outlet /> : <Navigate to="/" replace />
}

function sharedWorkspaceRoutes(root: string) {
  const isAdmin = root === "/admin"
  const isDean = root === "/dean"
  const isDeptHead = root === "/dept-head"
  const isInstructor = root === "/instructor"

  return (
    <>
      <Route
        path={root}
        element={
          isInstructor
            ? <FacultyDashboardPage />
            : isDean
              ? <DeanDashboardPage />
              : isDeptHead
                ? <DeptHeadDashboardPage />
                : <AdminDashboardPage />
        }
      />
      {(isAdmin || isInstructor || isDeptHead) && (
        <Route
          path={`${root}/class-sections`}
          element={
            isInstructor
              ? <InstructorAssignmentsPage />
              : isDeptHead
                ? <DeptHeadCoursesPage />
                : <ClassSectionManagementPage />
          }
        />
      )}

      <Route
        path={`${root}/programs`}
        element={<ProgramManagementPage />}
      />

      {(isAdmin || isDean) && (
        <Route
          path={`${root}/plo`}
          element={<PloManagementPage />}
        />
      )}

      {!isInstructor && (
        <Route
          path={`${root}/clo-plo-heatmap`}
          element={<CloPloHeatmapPage />}
        />
      )}

      <Route
        path={`${root}/syllabus`}
        element={<SyllabusListPage />}
      />

      <Route
        path={`${root}/syllabus/diff`}
        element={<CohortSyllabusDiffPage />}
      />

      <Route
        path={`${root}/syllabus/curriculum-map`}
        element={<SyllabusCurriculumMapPage />}
      />

      <Route
        path={`${root}/syllabus/create`}
        element={<SyllabusCreatePage />}
      />

      <Route
        path={`${root}/syllabus/:id`}
        element={<SyllabusDetailPage />}
      />

      <Route
        path={`${root}/syllabus/:id/edit`}
        element={<SyllabusEditPage />}
      />

      <Route
        path={`${root}/syllabus/:id/editor`}
        element={<SyllabusEditPage />}
      />

      <Route
        path={`${root}/syllabus/:id/diff`}
        element={<SyllabusDiffPage />}
      />

      <Route
        path={`${root}/programs/:id/diff`}
        element={<ProgramDiffPage />}
      />

      <Route
        path={`${root}/programs/:id/curriculum`}
        element={<ProgramCurriculumPage />}
      />

      <Route
        path={`${root}/programs/:id/timeline`}
        element={<ProgramTimelinePage />}
      />

      <Route
        path={`${root}/programs/:id/history`}
        element={<CurriculumTimelinePage />}
      />

      <Route
        path={`${root}/programs/:id`}
        element={
          <Navigate
            to={`${root}/programs`}
            replace
          />
        }
      />

      {isAdmin && (
        <>
          <Route
            path={`${root}/curricula/create`}
            element={<CreateCurriculumPage />}
          />

          <Route
            path={`${root}/email-outbox`}
            element={<EmailOutboxPage />}
          />

          <Route
            path={`${root}/escalations`}
            element={<EscalationCenterPage />}
          />

          <Route
            path={`${root}/settings`}
            element={<SystemSettingsPage />}
          />
        </>
      )}

      {(isDeptHead || isDean) && (
        <Route
          path={`${root}/approvals`}
          element={<DeptHeadApprovalPage />}
        />
      )}
    </>
  )
}
export default function App() {
  return <BrowserRouter><Routes>
    <Route path="/" element={<RootRedirect />} />
    <Route path="/login" element={<LoginPage />} />
    <Route path="/forgot-password" element={<ForgotPasswordPage />} />
    <Route path="/reset-password" element={<ResetPasswordPage />} />
    <Route element={<ProtectedRoute />}>
      <Route path="/syllabus/import" element={<SyllabusImportPage />} />
      <Route element={<DashboardLayout />}>
        <Route path="/curriculum-map" element={<SyllabusCurriculumMapPage />} />
        <Route element={<OwnWorkspace />}>
          {sharedWorkspaceRoutes("/admin")}
          {sharedWorkspaceRoutes("/instructor")}
          {sharedWorkspaceRoutes("/dean")}
          {sharedWorkspaceRoutes("/dept-head")}
        </Route>
        <Route element={<ProtectedRoute permission="MANAGE_USERS" />}>
          <Route path="/admin/users" element={<UserManagementPage />} />
        </Route>
        <Route
  element={
    <ProtectedRoute
      allowedRoles={["ADMIN"]}
      permission="VIEW_REPORTS"
    />
  }
>
  <Route
    path="/admin/reports"
    element={<ReportManagementPage />}
  />
</Route>

<Route
  element={
    <ProtectedRoute
      allowedRoles={["DEAN"]}
      permission="VIEW_REPORTS"
    />
  }
>
  <Route
    path="/dean/reports"
    element={<ReportManagementPage />}
  />
</Route>
        <Route element={<ProtectedRoute permission="VIEW_AUDIT" />}>
          <Route path="/admin/audit-log" element={<AuditLogPage />} />
        </Route>
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></BrowserRouter>
}
