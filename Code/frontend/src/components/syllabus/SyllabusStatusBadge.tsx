import { Badge } from "@/components/ui/badge"
import { getSyllabusStatusLabel } from "@/i18n/labels"

interface Props {
  status: string
}

export default function SyllabusStatusBadge({ status }: Props) {
  return <Badge>{getSyllabusStatusLabel(status)}</Badge>
}
