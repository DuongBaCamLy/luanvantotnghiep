import { useMemo } from "react"
import { useNavigate } from "react-router-dom"
import {
  AlertTriangle,
  Bell,
  CheckCheck,
  Clock3,
  FileCheck2,
  Inbox,
  LoaderCircle,
  LogOut,
  RefreshCw,
  ShieldAlert,
  User,
} from "lucide-react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"

import { notificationApi } from "@/api/notificationApi"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { ROLE_LABEL } from "@/config/navConfig"
import { formatDateTime, t } from "@/i18n"
import { useAuthStore } from "@/store/authStore"

const POLL_INTERVAL_MS = 30_000
const MAX_VISIBLE_NOTIFICATIONS = 12

type NotificationTone =
  | "review"
  | "success"
  | "warning"
  | "deadline"
  | "default"

export default function Header() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const notificationQuery = useQuery({
    queryKey: [
      "notifications",
      user?.userId,
    ],
    queryFn: () =>
      notificationApi.getUserNotifications(
        user?.userId ?? 0,
      ),
    enabled:
      Boolean(user?.userId),
    refetchInterval:
      POLL_INTERVAL_MS,
    refetchIntervalInBackground:
      false,
  })

  const notifications = useMemo(() => notificationQuery.data ?? [], [notificationQuery.data])

  const sortedNotifications =
    useMemo(
      () =>
        [...notifications].sort(
          (left, right) => {
            const leftTime =
              new Date(
                left.createdAt,
              ).getTime()

            const rightTime =
              new Date(
                right.createdAt,
              ).getTime()

            return (
              (Number.isNaN(rightTime)
                ? 0
                : rightTime)
              - (Number.isNaN(leftTime)
                ? 0
                : leftTime)
            )
          },
        ),
      [notifications],
    )

  const unreadCount =
    useMemo(
      () =>
        notifications.filter(
          (notification) =>
            !notification.isRead,
        ).length,
      [notifications],
    )

  const markAsReadMutation =
    useMutation({
      mutationFn:
        (id: number) =>
          notificationApi.markAsRead(
            id,
          ),
      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: [
            "notifications",
            user?.userId,
          ],
        })
      },
    })

  const markAllAsReadMutation =
    useMutation({
      mutationFn: () =>
        notificationApi.markAllAsRead(
          user?.userId ?? 0,
        ),
      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: [
            "notifications",
            user?.userId,
          ],
        })
      },
    })

  if (!user) {
    return null
  }

  const handleLogout = () => {
    logout()
    navigate(
      "/login",
      {
        replace: true,
      },
    )
  }

  const openNotification =
    (notification: {
      id: number
      type: string
      isRead: boolean
    }) => {
      if (
        !notification.isRead
      ) {
        markAsReadMutation.mutate(
          notification.id,
        )
      }

      const workflowRoute =
        getWorkflowNotificationRoute(
          notification.type,
          user.role,
        )

      if (workflowRoute) {
        navigate(workflowRoute)
      }
    }

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-[#dce7e9] bg-white/95 px-4 shadow-[0_1px_8px_rgba(15,23,42,0.04)] backdrop-blur md:px-6">
      <div className="admin-header-context">
        <span>SCSE</span>
        <span aria-hidden="true">/</span>
        <span>Administration</span>
      </div>

      <div className="flex items-center gap-3 md:gap-5">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="relative h-9 w-9 rounded-full border border-transparent text-slate-600 transition-colors hover:border-[#cfe1e3] hover:bg-[#f1f8f8] hover:text-[#007d84]"
              aria-label={
                unreadCount > 0
                  ? `${t("header.notifications")}: ${unreadCount} unread`
                  : t("header.notifications")
              }
            >
              <Bell className="h-[18px] w-[18px]" />

              {unreadCount > 0 && (
                <span className="absolute -right-0.5 -top-0.5 flex min-h-[17px] min-w-[17px] items-center justify-center rounded-full border-2 border-white bg-[#e59a22] px-1 text-[9px] font-bold leading-none text-white shadow-sm">
                  {unreadCount > 99
                    ? "99+"
                    : unreadCount}
                </span>
              )}
            </Button>
          </DropdownMenuTrigger>

          <DropdownMenuContent
            align="end"
            sideOffset={10}
            className="w-[360px] overflow-hidden rounded-xl border border-[#d9e5e7] bg-white p-0 shadow-[0_18px_48px_rgba(15,23,42,0.14)] sm:w-[390px]"
          >
            <div className="border-b border-slate-100 bg-gradient-to-r from-[#f8fbfb] to-white px-4 py-3.5">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <DropdownMenuLabel className="p-0 text-[15px] font-bold text-[#17343d]">
                    Notifications
                  </DropdownMenuLabel>

                  <p className="mt-0.5 text-[11px] leading-4 text-slate-500">
                    Academic workflow alerts and actions requiring your attention.
                  </p>
                </div>

                {unreadCount > 0 ? (
                  <span className="shrink-0 rounded-full border border-[#c9dfe2] bg-[#eef7f7] px-2 py-1 text-[10px] font-semibold text-[#007d84]">
                    {unreadCount} unread
                  </span>
                ) : (
                  <span className="shrink-0 rounded-full border border-emerald-200 bg-emerald-50 px-2 py-1 text-[10px] font-semibold text-emerald-700">
                    All read
                  </span>
                )}
              </div>

              <div className="mt-3 flex items-center gap-2">
                {unreadCount > 0 && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-8 gap-1.5 border-[#cfe1e3] bg-white px-2.5 text-[11px] font-semibold text-[#007d84] hover:bg-[#eef7f7] hover:text-[#006a70]"
                    onClick={(
                      event,
                    ) => {
                      event.preventDefault()
                      event.stopPropagation()
                      markAllAsReadMutation.mutate()
                    }}
                    disabled={
                      markAllAsReadMutation.isPending
                    }
                  >
                    {markAllAsReadMutation.isPending ? (
                      <LoaderCircle className="size-3.5 animate-spin" />
                    ) : (
                      <CheckCheck className="size-3.5" />
                    )}
                    Mark all as read
                  </Button>
                )}

                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 gap-1.5 px-2.5 text-[11px] font-medium text-slate-500 hover:bg-slate-50 hover:text-slate-700"
                  onClick={(
                    event,
                  ) => {
                    event.preventDefault()
                    event.stopPropagation()
                    notificationQuery.refetch()
                  }}
                  disabled={
                    notificationQuery.isFetching
                  }
                >
                  {notificationQuery.isFetching ? (
                    <LoaderCircle className="size-3.5 animate-spin" />
                  ) : (
                    <RefreshCw className="size-3.5" />
                  )}
                  Refresh
                </Button>
              </div>
            </div>

            {notificationQuery.isError ? (
              <div className="px-5 py-7 text-center">
                <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-full bg-rose-50 text-rose-600">
                  <AlertTriangle className="size-5" />
                </div>

                <p className="mt-3 text-sm font-semibold text-slate-800">
                  Unable to load notifications
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Check your connection and refresh the notification list.
                </p>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-3 h-8 gap-1.5"
                  onClick={() =>
                    notificationQuery.refetch()
                  }
                >
                  <RefreshCw className="size-3.5" />
                  Try again
                </Button>
              </div>
            ) : notificationQuery.isLoading ? (
              <div className="flex items-center justify-center gap-2 px-5 py-8 text-sm text-slate-500">
                <LoaderCircle className="size-4 animate-spin" />
                Loading notifications...
              </div>
            ) : sortedNotifications.length === 0 ? (
              <div className="px-5 py-8 text-center">
                <div className="mx-auto flex h-11 w-11 items-center justify-center rounded-full border border-[#d8e7e9] bg-[#f5fafa] text-[#007d84]">
                  <Inbox className="size-5" />
                </div>

                <p className="mt-3 text-sm font-semibold text-[#17343d]">
                  No notifications
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  New approval, revision, deadline and escalation updates will appear here.
                </p>
              </div>
            ) : (
              <>
                <div className="max-h-[430px] overflow-y-auto">
                  {sortedNotifications
                    .slice(
                      0,
                      MAX_VISIBLE_NOTIFICATIONS,
                    )
                    .map(
                      (notification) => {
                        const tone =
                          getNotificationTone(
                            notification.type,
                          )

                        const route =
                          getWorkflowNotificationRoute(
                            notification.type,
                            user.role,
                          )

                        return (
                          <DropdownMenuItem
                            key={
                              notification.id
                            }
                            className={`group cursor-pointer gap-0 border-b border-slate-100 p-0 last:border-b-0 focus:bg-transparent ${
                              !notification.isRead
                                ? "bg-[#f7fbfb]"
                                : "bg-white"
                            }`}
                            onSelect={(
                              event,
                            ) => {
                              event.preventDefault()
                              openNotification(
                                notification,
                              )
                            }}
                          >
                            <div className="flex w-full gap-3 px-4 py-3.5 transition-colors group-hover:bg-slate-50/80">
                              <NotificationIcon
                                tone={tone}
                              />

                              <div className="min-w-0 flex-1">
                                <div className="flex items-start justify-between gap-2">
                                  <p
                                    className={`line-clamp-2 text-[12px] leading-[18px] ${
                                      !notification.isRead
                                        ? "font-bold text-[#17343d]"
                                        : "font-semibold text-slate-700"
                                    }`}
                                  >
                                    {notification.title}
                                  </p>

                                  {!notification.isRead && (
                                    <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#e59a22]" />
                                  )}
                                </div>

                                <p className="mt-1 line-clamp-2 text-[11px] leading-[17px] text-slate-500">
                                  {notification.message}
                                </p>

                                <div className="mt-2 flex items-center justify-between gap-3">
                                  <span className="inline-flex items-center gap-1 text-[10px] text-slate-400">
                                    <Clock3 className="size-3" />
                                    {formatDateTime(
                                      notification.createdAt,
                                    )}
                                  </span>

                                  {route && (
                                    <span className="text-[10px] font-semibold text-[#007d84]">
                                      Open
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          </DropdownMenuItem>
                        )
                      },
                    )}
                </div>

                {sortedNotifications.length
                  > MAX_VISIBLE_NOTIFICATIONS && (
                  <>
                    <DropdownMenuSeparator />

                    <div className="px-4 py-2.5 text-center text-[10px] text-slate-500">
                      Showing the latest{" "}
                      {MAX_VISIBLE_NOTIFICATIONS} of{" "}
                      {sortedNotifications.length} notifications.
                    </div>
                  </>
                )}
              </>
            )}
          </DropdownMenuContent>
        </DropdownMenu>

        <div className="hidden h-7 w-px bg-slate-200 sm:block" />

        <div className="flex items-center gap-2">
          <div className="hidden items-center gap-2 sm:flex">
            <div className="flex h-8 w-8 items-center justify-center rounded-full border border-[#cfe1e3] bg-[#f1f8f8] text-[#007d84]">
              <User className="size-4" />
            </div>

            <div className="leading-tight">
              <p className="max-w-[160px] truncate text-[12px] font-semibold text-[#17343d]">
                {user.username}
              </p>

              <p className="mt-0.5 text-[10px] text-slate-400">
                {ROLE_LABEL[user.role]}
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleLogout}
            className="h-9 gap-1.5 rounded-md px-2.5 text-[11px] font-semibold text-[#007d84] hover:bg-[#f1f8f8] hover:text-[#006a70]"
          >
            <LogOut className="size-3.5" />
            <span className="hidden md:inline">
              {t("header.logout")}
            </span>
          </Button>
        </div>
      </div>
    </header>
  )
}

function NotificationIcon({
  tone,
}: {
  tone: NotificationTone
}) {
  const baseClass =
    "flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border"

  if (
    tone === "review"
  ) {
    return (
      <div
        className={`${baseClass} border-[#c9dfe2] bg-[#eef7f7] text-[#007d84]`}
      >
        <FileCheck2 className="size-4" />
      </div>
    )
  }

  if (
    tone === "success"
  ) {
    return (
      <div
        className={`${baseClass} border-emerald-200 bg-emerald-50 text-emerald-700`}
      >
        <CheckCheck className="size-4" />
      </div>
    )
  }

  if (
    tone === "warning"
  ) {
    return (
      <div
        className={`${baseClass} border-amber-200 bg-amber-50 text-amber-700`}
      >
        <AlertTriangle className="size-4" />
      </div>
    )
  }

  if (
    tone === "deadline"
  ) {
    return (
      <div
        className={`${baseClass} border-rose-200 bg-rose-50 text-rose-700`}
      >
        <ShieldAlert className="size-4" />
      </div>
    )
  }

  return (
    <div
      className={`${baseClass} border-slate-200 bg-slate-50 text-slate-600`}
    >
      <Bell className="size-4" />
    </div>
  )
}

function getNotificationTone(
  type: string,
): NotificationTone {
  const normalized =
    String(type ?? "")
      .trim()
      .toUpperCase()

  if (
    normalized.includes(
      "WAITING_FOR_DEAN",
    )
    || normalized.includes(
      "SUBMITTED",
    )
    || normalized.includes(
      "UNDER_REVIEW",
    )
  ) {
    return "review"
  }

  if (
    normalized.includes(
      "APPROVED",
    )
    || normalized.includes(
      "CONFIRMED",
    )
  ) {
    return "success"
  }

  if (
    normalized.includes(
      "REVISION",
    )
    || normalized.includes(
      "REJECT",
    )
  ) {
    return "warning"
  }

  if (
    normalized.includes(
      "DEADLINE",
    )
    || normalized.includes(
      "OVERDUE",
    )
    || normalized.includes(
      "ESCALAT",
    )
  ) {
    return "deadline"
  }

  return "default"
}

function getWorkflowNotificationRoute(
  type: string,
  role: string,
): string | null {
  const normalizedType =
    String(type ?? "")
      .trim()
      .toUpperCase()

  const normalizedRole =
    String(role ?? "")
      .replace(/^ROLE_/i, "")
      .trim()
      .toUpperCase()

  if (
    normalizedType
      === "SYLLABUS_DEADLINE_REMINDER"
    || normalizedType
      === "SYLLABUS_DEADLINE_DUE_TODAY"
  ) {
    return normalizedRole
      === "INSTRUCTOR"
      ? "/instructor/syllabus"
      : null
  }

  if (
    normalizedType
      === "SYLLABUS_SUBMITTED"
  ) {
    return normalizedRole
      === "DEPT_HEAD"
      ? "/dept-head/approvals"
      : null
  }

  if (
    normalizedType
      === "SYLLABUS_WAITING_FOR_DEAN"
  ) {
    return normalizedRole
      === "DEAN"
      ? "/dean/approvals"
      : null
  }

  if (
    normalizedType
      === "SYLLABUS_DEAN_REVISION_REQUESTED"
    || normalizedType
      === "SYLLABUS_APPROVED_FOR_DEPT_HEAD"
  ) {
    return normalizedRole
      === "DEPT_HEAD"
      ? "/dept-head/syllabus"
      : null
  }

  if (
    normalizedType.includes(
      "OVERDUE",
    )
    || normalizedType.includes(
      "ESCALAT",
    )
  ) {
    if (
      normalizedRole
      === "DEAN"
    ) {
      return "/dean/syllabus"
    }

    if (
      normalizedRole
      === "DEPT_HEAD"
    ) {
      return "/dept-head/syllabus"
    }

    if (
      normalizedRole
      === "ADMIN"
    ) {
      return "/admin/syllabus"
    }
  }

  if (
    normalizedType
      === "SYLLABUS_SUBMISSION_CONFIRMED"
    || normalizedType
      === "SYLLABUS_REVISION_REQUESTED"
    || normalizedType
      === "SYLLABUS_DEPT_HEAD_APPROVED"
    || normalizedType
      === "SYLLABUS_APPROVED"
  ) {
    if (
      normalizedRole === "ADMIN"
    ) {
      return "/admin/syllabus"
    }

    if (
      normalizedRole === "DEAN"
    ) {
      return "/dean/syllabus"
    }

    if (
      normalizedRole === "DEPT_HEAD"
    ) {
      return "/dept-head/syllabus"
    }

    return "/instructor/syllabus"
  }

  return null
}