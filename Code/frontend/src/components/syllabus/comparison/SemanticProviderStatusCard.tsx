import {
  Bot,
  CircleAlert,
  CircleCheck,
  CircleOff,
  LoaderCircle,
} from "lucide-react"

import { Card } from "@/components/ui/card"
import { useSemanticProviderStatus } from "@/hooks/useSemanticProviderStatus"
import { cn } from "@/lib/utils"

type Props = {
  enabled?: boolean
}

const statusPresentation = (
  status?: string,
) => {
  switch (status) {
    case "READY":
      return {
        label: "Ready",
        className:
          "border-emerald-200 bg-emerald-50 text-emerald-700",
        icon: CircleCheck,
      }

    case "DISABLED":
      return {
        label: "Disabled",
        className:
          "border-slate-200 bg-slate-100 text-slate-600",
        icon: CircleOff,
      }

    case "NOT_CONFIGURED":
      return {
        label: "Not configured",
        className:
          "border-amber-200 bg-amber-50 text-amber-700",
        icon: CircleAlert,
      }

    case "MODEL_UNAVAILABLE":
      return {
        label: "Model unavailable",
        className:
          "border-amber-200 bg-amber-50 text-amber-700",
        icon: CircleAlert,
      }

    case "UNREACHABLE":
      return {
        label: "Provider unavailable",
        className:
          "border-rose-200 bg-rose-50 text-rose-700",
        icon: CircleAlert,
      }

    default:
      return {
        label: "Unknown",
        className:
          "border-slate-200 bg-slate-50 text-slate-600",
        icon: CircleAlert,
      }
  }
}

export default function SemanticProviderStatusCard({
  enabled = true,
}: Props) {
  const query =
    useSemanticProviderStatus(
      enabled,
    )

  if (!enabled) {
    return null
  }

  if (query.isLoading) {
    return (
      <Card className="border-slate-200 px-4 py-3 shadow-sm">
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <LoaderCircle className="size-4 animate-spin text-[#007d84]" />
          Verifying semantic AI provider...
        </div>
      </Card>
    )
  }

  if (query.isError || !query.data) {
    return (
      <Card className="border-rose-200 bg-rose-50/40 px-4 py-3 shadow-sm">
        <div className="flex items-start gap-3">
          <CircleAlert className="mt-0.5 size-4 shrink-0 text-rose-600" />

          <div>
            <p className="text-sm font-bold text-rose-700">
              Unable to verify semantic AI provider
            </p>

            <p className="mt-1 text-xs leading-5 text-rose-600">
              Structural comparison still works. Semantic analysis status
              could not be loaded from the backend.
            </p>
          </div>
        </div>
      </Card>
    )
  }

  const data =
    query.data

  const presentation =
    statusPresentation(
      data.status,
    )

  const StatusIcon =
    presentation.icon

  const provider =
    data.provider?.trim()
    || "AI provider"

  return (
    <Card className="border-slate-200 px-4 py-3 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-start gap-3">
          <Bot className="mt-0.5 size-4 shrink-0 text-[#007d84]" />

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-bold text-[#17343d]">
                Semantic AI Provider
              </p>

              <span className="text-xs font-semibold text-slate-600">
                {provider}
              </span>

              <span
                className={cn(
                  "inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-bold",
                  presentation.className,
                )}
              >
                <StatusIcon className="size-3" />
                {presentation.label}
              </span>
            </div>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              {data.message
                || "Semantic provider status loaded."}
            </p>
          </div>
        </div>

        <div className="shrink-0 text-left text-[11px] text-slate-500 sm:text-right">
          <p>
            Model:{" "}
            <span className="font-mono font-semibold text-slate-700">
              {data.model || "—"}
            </span>
          </p>

          <p className="mt-0.5">
            Provider probe:{" "}
            <span className="font-semibold text-slate-700">
              {data.reachable === true
                ? "reachable"
                : data.reachable === false
                  ? "unreachable"
                  : "not run"}
            </span>
          </p>
        </div>
      </div>
    </Card>
  )
}
