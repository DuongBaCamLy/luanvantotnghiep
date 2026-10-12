import { useState } from "react"
import { LoaderCircle, RotateCcw, Save } from "lucide-react"
import { Button } from "@/components/ui/button"
import New2027SyllabusView from "./New2027SyllabusView"
import { toNew2027DraftPayload } from "@/lib/new2027SyllabusDraft"
import type { CreateSyllabusRequest, Syllabus } from "@/types/syllabus"

type Props = {
  syllabus: Syllabus
  initialData: CreateSyllabusRequest
  onSubmit: (values: CreateSyllabusRequest) => void
  loading?: boolean
  lockAssignmentContext?: boolean
}

export default function New2027SyllabusForm({ syllabus, initialData, onSubmit, loading = false, lockAssignmentContext = false }: Props) {
  const [draft, setDraft] = useState(syllabus)
  return <form id="edit-syllabus-form" aria-label="NEW_2027 Draft editor" className="space-y-5"
    onSubmit={(event) => { event.preventDefault(); if (!loading) onSubmit(toNew2027DraftPayload(draft, initialData)) }}>
    <fieldset disabled={loading} className="min-w-0 space-y-5 border-0 p-0">
      <div className="flex flex-wrap items-end justify-between gap-4 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap gap-4">
          <label className="space-y-1 text-xs font-semibold text-slate-600">Draft semester
            <input aria-label="Draft semester" required disabled={lockAssignmentContext} className="block rounded-md border border-slate-200 px-3 py-2 text-sm"
              value={draft.semester ?? ""} placeholder="Semester 1–8 or Summer Semester"
              onChange={(event) => setDraft({ ...draft, semester: event.target.value })} />
          </label>
          <label className="space-y-1 text-xs font-semibold text-slate-600">Revision notes
            <input aria-label="Revision notes" className="block rounded-md border border-slate-200 px-3 py-2 text-sm"
              value={draft.changeSummary ?? ""} onChange={(event) => setDraft({ ...draft, changeSummary: event.target.value })} />
          </label>
        </div>
        <div className="flex gap-2">
          <Button type="button" variant="outline" onClick={() => setDraft(syllabus)}><RotateCcw className="size-4" />Reset changes</Button>
          <Button type="submit"><Save className="size-4" />Save syllabus Draft</Button>
        </div>
      </div>
      <New2027SyllabusView syllabus={draft} onChange={setDraft} />
      <div className="flex justify-end"><Button type="submit" disabled={loading}>
        {loading ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />}
        {loading ? "Saving..." : "Save syllabus Draft"}
      </Button></div>
    </fieldset>
  </form>
}
