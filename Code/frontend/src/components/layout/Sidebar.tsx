import { useState } from "react"
import { NavLink, useLocation } from "react-router-dom"
import { ChevronDown, ChevronRight, LogOut, UserRound } from "lucide-react"

import { cn } from "@/lib/utils"
import { getRoleNav, ROLE_LABEL } from "@/config/navConfig"
import { useAuthStore } from "@/store/authStore"
import { t } from "@/i18n"
import logoImg from "@/assets/logo.jpg"

export default function Sidebar() {
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const location = useLocation()
  const [expandedMenus, setExpandedMenus] = useState<Record<string, boolean>>({})

  if (!user) return null

  const groups = getRoleNav(user.role)

  const toggleMenu = (path: string) => {
    setExpandedMenus((previous) => ({
      ...previous,
      [path]: !previous[path],
    }))
  }

  const isParentActive = (
    itemPath: string,
    children?: { path: string }[],
  ) => {
    if (location.pathname === itemPath) return true

    const specificChildActive = children?.some((child) =>
      child.path !== itemPath
      && (location.pathname === child.path || location.pathname.startsWith(`${child.path}/`)),
    ) ?? false

    if (specificChildActive) return true

    if (children?.some((child) => child.path === itemPath)
      && location.pathname.startsWith(`${itemPath}/`)) return true

    return false
  }

  return (
    <aside className="flex h-screen w-[236px] shrink-0 flex-col bg-[#006b72] text-white shadow-[3px_0_14px_rgba(0,71,78,0.08)]">
      <div className="flex h-[72px] items-center gap-3 border-b border-white/10 px-4">
        <div className="flex h-[40px] w-[40px] shrink-0 items-center justify-center overflow-hidden bg-white/95 p-[2px]">
          <img
            src={logoImg}
            alt="International University - VNU-HCM"
            className="h-full w-full object-contain"
          />
        </div>

        <div className="min-w-0 leading-tight">
          <p className="truncate text-[13px] font-bold tracking-[0.2px] text-white">
            {t("app.shortName")}
          </p>
          <p className="mt-[3px] truncate text-[9px] font-normal text-white/80">
            {t("app.name")}
          </p>
        </div>
      </div>

      <div className="px-4 pt-3">
        <span className="inline-flex rounded-full border border-white/20 bg-white/10 px-2.5 py-1 text-[9px] font-bold uppercase tracking-[0.14em] text-white">
          {ROLE_LABEL[user.role]}
        </span>
      </div>

      <nav className="flex-1 overflow-y-auto px-[7px] py-3">
        {groups.map((group) => (
          <section key={group.label} className="mb-4">
            <p className="px-3 pb-1.5 text-[9px] font-bold uppercase tracking-[0.16em] text-white/45">{group.label}</p>
            <div className="space-y-[3px]">
            {group.items.map((item) => {
          const Icon = item.icon
          const hasChildren = !!item.children?.length
          const active = isParentActive(item.path, item.children)
          const isExpanded = expandedMenus[item.path] ?? active

          if (hasChildren) {
            return (
              <div key={item.path} className="space-y-[3px]">
                <button
                  type="button"
                  onClick={() => toggleMenu(item.path)}
                  className={cn(
                    "flex h-[38px] w-full items-center justify-between rounded-[2px] px-3 text-[13px] transition-colors",
                    active
                      ? "bg-[#f0aa39] font-semibold text-white shadow-sm"
                      : "text-white/95 hover:bg-white/10 hover:text-white",
                  )}
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <Icon className="h-[16px] w-[16px] shrink-0 stroke-[1.7]" />
                    <span className="truncate">{item.label}</span>
                  </div>

                  {isExpanded ? (
                    <ChevronDown className="h-[14px] w-[14px] shrink-0" />
                  ) : (
                    <ChevronRight className="h-[14px] w-[14px] shrink-0" />
                  )}
                </button>

                {isExpanded && (
                  <div className="space-y-[2px] pl-[34px] pr-1">
                    {item.children?.map((child) => (
                      <NavLink
                        key={child.path}
                        to={child.path}
                        end
                        className={({ isActive }) =>
                          cn(
                            "block rounded-[2px] px-3 py-[7px] text-[12px] leading-4 transition-colors",
                            isActive
                              ? "bg-white/[0.16] font-semibold text-white"
                              : "text-white/78 hover:bg-white/10 hover:text-white",
                          )
                        }
                      >
                        {child.label}
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            )
          }

          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === `/${item.path.split("/")[1]}`}
              className={({ isActive }) =>
                cn(
                  "flex h-[38px] items-center gap-3 rounded-[2px] px-3 text-[13px] transition-colors",
                  isActive
                    ? "bg-[#f0aa39] font-semibold text-white shadow-sm"
                    : "text-white/95 hover:bg-white/10 hover:text-white",
                )
              }
            >
              <Icon className="h-[16px] w-[16px] shrink-0 stroke-[1.7]" />
              <span className="truncate">{item.label}</span>
            </NavLink>
          )
            })}
            </div>
          </section>
        ))}
      </nav>

      <div className="border-t border-white/10 p-3">
        <div className="flex items-center gap-3 rounded-lg bg-black/10 p-2.5">
          <span className="flex size-9 shrink-0 items-center justify-center rounded-full border border-white/20 bg-white/10"><UserRound className="size-4" /></span>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold">{user.username}</p>
            <p className="truncate text-[9px] uppercase tracking-wide text-white/60">{ROLE_LABEL[user.role]}</p>
          </div>
          <button type="button" onClick={logout} title="Sign out" className="rounded p-1.5 text-white/70 hover:bg-white/10 hover:text-white"><LogOut className="size-4" /></button>
        </div>
      </div>
    </aside>
  )
}
