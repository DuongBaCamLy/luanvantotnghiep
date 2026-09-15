import { Outlet } from "react-router-dom"

import Sidebar from "@/components/layout/Sidebar"
import Header from "@/components/layout/Header"
import { useAuthStore } from "@/store/authStore"
import "@/styles/admin-workspace.css"

export default function DashboardLayout() {
  const adminPresentation = useAuthStore((state) => state.user?.role === "ADMIN")
  return (
    <div data-admin-ui={adminPresentation ? "true" : undefined} className="iu-dashboard-theme flex h-screen overflow-hidden bg-[#edf5f7] text-[#19313c]">
      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header />

        <main className="flex-1 overflow-y-auto bg-[#edf5f7] px-5 py-5 lg:px-7 lg:py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
