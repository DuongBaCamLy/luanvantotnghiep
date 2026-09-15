import type { ReactNode } from "react"

type ComparisonSectionProps = {
  title: string
  description?: string
  oldLabel: string
  newLabel: string
  children: ReactNode
}

export default function ComparisonSection({
  title,
  description,
  oldLabel,
  newLabel,
  children,
}: ComparisonSectionProps) {
  return (
    <section className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <header className="border-b border-slate-200 bg-slate-50/70 px-5 py-4">
        <h2 className="font-bold text-[#17343d]">
          {title}
        </h2>

        {description && (
          <p className="mt-1 text-xs leading-5 text-slate-500">
            {description}
          </p>
        )}
      </header>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[900px] border-collapse">
          <thead>
            <tr className="bg-[#f7fafb] text-left">
              <th className="w-[220px] border-b border-r border-slate-200 px-4 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-slate-500">
                Field
              </th>

              <th className="border-b border-r border-slate-200 px-4 py-3">
                <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-red-500">
                  Old Cohort
                </p>
                <p className="mt-1 text-sm font-black text-slate-800">
                  {oldLabel}
                </p>
              </th>

              <th className="border-b border-slate-200 px-4 py-3">
                <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-emerald-600">
                  New Cohort
                </p>
                <p className="mt-1 text-sm font-black text-slate-800">
                  {newLabel}
                </p>
              </th>
            </tr>
          </thead>

          <tbody>{children}</tbody>
        </table>
      </div>
    </section>
  )
}