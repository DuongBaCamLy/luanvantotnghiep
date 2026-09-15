import type { ReactNode } from "react"

type DashboardHeroProps = {
  roleLabel: string
  title: string
  description: string
  actions?: ReactNode
}

export default function DashboardHero({
  roleLabel,
  title,
  description,
  actions,
}: DashboardHeroProps) {
  return (
    <section data-admin-hero className="relative min-h-[250px] overflow-hidden rounded-2xl border border-slate-200 bg-slate-900 shadow-sm">
      <img
        src="/images/iu-campus.png"
        alt="International University campus"
        className="absolute inset-0 h-full w-full object-cover object-[center_58%]"
      />

      <div className="absolute inset-0 bg-gradient-to-r from-[#052f38]/95 via-[#063f48]/78 to-[#052f38]/28" />
      <div className="absolute inset-0 bg-gradient-to-t from-black/35 via-transparent to-black/10" />

      <div className="relative z-10 flex min-h-[250px] items-end px-7 py-7 sm:px-9 sm:py-8">
        <div className="max-w-3xl">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-white/70">
            {roleLabel}
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-[-0.5px] text-white sm:text-4xl">
            {title}
          </h1>

          <p className="mt-3 max-w-2xl text-sm leading-6 text-white/82 sm:text-[15px]">
            {description}
          </p>

          {actions ? (
            <div className="mt-5 flex flex-wrap gap-2">
              {actions}
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}