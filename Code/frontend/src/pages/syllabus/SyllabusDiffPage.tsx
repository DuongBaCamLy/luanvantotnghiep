import { useParams, useSearchParams } from "react-router-dom"

import SyllabusDiffDetails from "@/components/syllabus/SyllabusDiffDetails"
import SyllabusToolbar from "@/components/syllabus/SyllabusToolbar"
import { Card } from "@/components/ui/card"
import { useSyllabusDiff } from "@/hooks/useSyllabusDiff"
import type { SyllabusDiffResponse } from "@/types/syllabusDiff"

export default function SyllabusDiffPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const compareWith = searchParams.get("compareWith")

  const { data, isLoading, isError } = useSyllabusDiff(
    Number(id),
    Number(compareWith)
  )

  const diff = data as SyllabusDiffResponse | undefined

  return (
    <div className="space-y-6">
      <SyllabusToolbar />

      {!compareWith ? (
        <Card className="p-6 text-center text-slate-500">
          Select another syllabus version to compare.
        </Card>
      ) : isLoading ? (
        <Card className="p-6 text-center text-slate-500">
          Loading comparison data...
        </Card>
      ) : isError || !diff ? (
        <Card className="p-6 text-center text-rose-500">
          Unable to load comparison data. Verify that both versions belong to the same course and that you have access to both versions.
        </Card>
      ) : (
        <Card className="border-slate-200 p-6 shadow-sm">
          <div className="mb-6">
            <p className="text-xs font-bold uppercase tracking-widest text-brand-600">
              FR-03.7
            </p>
            <h1 className="mt-1 text-2xl font-black text-slate-900">
              Syllabus Version Comparison
            </h1>
          </div>
          <SyllabusDiffDetails diff={diff} />
        </Card>
      )}
    </div>
  )
}
