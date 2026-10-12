import type { ds2021ImportChoices } from "@/lib/ds2021ImportMapping"

type Choice = ReturnType<typeof ds2021ImportChoices>[number]

export default function Ds2021ImportSelection({ choices, selected, onChange }: {
  choices: Choice[]; selected: number[]; onChange: (next: number[]) => void
}) {
  return <div className="space-y-3 rounded-xl border border-[#cfe2e4] p-4">
    <div>
      <h3 className="font-semibold text-[#006f76]">Choose DS2021 syllabuses to import</h3>
      <p className="mt-1 text-xs leading-5 text-slate-500">Missing syllabuses are selected by default. Select an existing syllabus only when you intend to import it again.</p>
      <p className="mt-1 text-xs text-slate-500">
        {choices.filter(c => c.state === "existing").length} already saved · {choices.filter(c => c.state === "missing").length} missing · {choices.filter(c => c.state === "unmapped" || c.state === "ambiguous").length} need curriculum review
      </p>
    </div>
    <div className="max-h-64 space-y-2 overflow-y-auto">
      {choices.map(choice => <label key={choice.index} className="flex items-start gap-3 rounded-lg border border-slate-200 p-3 text-sm">
        <input type="checkbox" className="mt-1" aria-label={`Import ${choice.code}`} checked={selected.includes(choice.index)}
          disabled={choice.state === "unmapped" || choice.state === "invalid" || choice.state === "ambiguous"}
          onChange={event => onChange(event.target.checked ? [...selected, choice.index] : selected.filter(index => index !== choice.index))} />
        <span><span className="font-medium">{choice.code} · {choice.name}</span>
          <span className={`mt-1 block text-xs ${choice.state === "unmapped" ? "text-amber-700" : "text-slate-500"}`}>
            {choice.state === "existing" ? "Already saved" : choice.state === "missing" ? "Ready to import" : choice.state === "invalid" ? "Source validation required" : choice.state === "ambiguous" ? "Ambiguous course mapping; verify the curriculum before importing." : "Not mapped in DS2021; verify the curriculum before importing. Different course codes are kept separate."}
          </span>
        </span>
      </label>)}
    </div>
  </div>
}
