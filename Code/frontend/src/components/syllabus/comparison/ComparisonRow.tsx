import type { ReactNode } from "react"

export type ComparisonState =
  | "UNCHANGED"
  | "REWORDING"
  | "MODIFIED"
  | "ADDED"
  | "REMOVED"
  | "PENDING_AI"
  | "DATA_MISSING"

type ComparisonRowProps = {
  field: ReactNode
  oldValue: ReactNode
  newValue: ReactNode
  state?: ComparisonState
  detail?: ReactNode
}

const oldCellClass: Record<ComparisonState, string> = {
  UNCHANGED: "bg-white text-slate-700",
  REWORDING: "bg-blue-50/70 text-slate-800",
  MODIFIED: "bg-red-50 text-red-800",
  ADDED: "bg-slate-50 text-slate-400",
  REMOVED: "bg-red-50 text-red-800",
  PENDING_AI: "bg-amber-50/70 text-slate-800",
  DATA_MISSING: "bg-amber-50/70 text-amber-900",
}

const newCellClass: Record<ComparisonState, string> = {
  UNCHANGED: "bg-white text-slate-700",
  REWORDING: "bg-blue-50/70 text-slate-800",
  MODIFIED: "bg-emerald-50 text-emerald-800",
  ADDED: "bg-emerald-50 text-emerald-800",
  REMOVED: "bg-slate-50 text-slate-400",
  PENDING_AI: "bg-amber-50/70 text-slate-800",
  DATA_MISSING: "bg-amber-50/70 text-amber-900",
}

export default function ComparisonRow({
  field,
  oldValue,
  newValue,
  state = "UNCHANGED",
  detail,
}: ComparisonRowProps) {
  return (
    <>
      <tr className="align-top">
        <td className="w-[220px] border-b border-r border-slate-200 bg-slate-50 px-4 py-3">
          <div>
  <div className="text-xs font-bold text-slate-700">
    {field}
  </div>

  {state === "DATA_MISSING" && (
    <div className="mt-1 text-[10px] font-semibold text-amber-700">
      Data unavailable
    </div>
  )}
</div>
        </td>

        <td
          className={`w-[calc((100%-220px)/2)] border-b border-r border-slate-200 px-4 py-3 ${oldCellClass[state]}`}
        >
          <div className="whitespace-pre-wrap break-words text-sm leading-6">
            {oldValue || <span className="text-slate-400">—</span>}
          </div>
        </td>

        <td
          className={`w-[calc((100%-220px)/2)] border-b border-slate-200 px-4 py-3 ${newCellClass[state]}`}
        >
          <div className="whitespace-pre-wrap break-words text-sm leading-6">
            {newValue || <span className="text-slate-400">—</span>}
          </div>
        </td>
      </tr>

      {detail && (
        <tr>
          <td className="border-b border-r border-slate-200 bg-slate-50" />

          <td
            colSpan={2}
            className="border-b border-slate-200 bg-slate-50/60 px-4 py-3"
          >
            {detail}
          </td>
        </tr>
      )}
    </>
  )
}