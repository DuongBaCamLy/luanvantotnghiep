import { Navigate, Outlet } from "react-router-dom"
import { useAuthStore } from "@/store/authStore"
import type { UserRole } from "@/types/auth"
import { hasPermission, type Permission } from "@/config/navConfig"

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
  permission?: Permission
}

export default function ProtectedRoute({ allowedRoles, permission }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate to="/" replace />
  }

  if (permission && !hasPermission(user?.role, permission)) {
    return <Navigate to="/" replace />
  }

  return <Outlet />
}
