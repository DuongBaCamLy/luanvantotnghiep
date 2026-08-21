import {
  t,
} from "@/i18n"

interface Props {
  title: string
}

export default function DashboardPlaceholder({
  title,
}: Props) {
  return (
    <div>
      <h1 className="mb-2 text-2xl font-bold text-brand-700">
        {title}
      </h1>

      <p className="text-sm text-muted-foreground">
        {t(
          "placeholder.development"
        )}
      </p>
    </div>
  )
}