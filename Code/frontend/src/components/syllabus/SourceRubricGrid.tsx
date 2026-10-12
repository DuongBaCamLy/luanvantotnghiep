type SourceCell = { text?: string; colSpan?: number; rowSpan?: number }
type SourceBlock = { kind: string; page: number; text?: string; widths?: number[]; rows?: SourceCell[][] }
export type SourceRubric = { format?: string; title?: string; blocks?: SourceBlock[] }

/** Keep source columns, merged cells and unfilled score/comment fields as supplied. */
export function SourceRubricGrid({ source, onChange }: { source: SourceRubric; onChange?: (source: SourceRubric) => void }) {
  const updateBlock = (index: number, patch: Partial<SourceBlock>) => onChange?.({ ...source,
    blocks: source.blocks?.map((block, position) => position === index ? { ...block, ...patch } : block),
  })
  return <div className="space-y-4">
    <h3 className="text-sm font-semibold text-[#17343d]">{source.title}</h3>
    {source.blocks?.map((block, index) => <div key={index} className="space-y-2">
      {(index === 0 || source.blocks?.[index - 1].page !== block.page)
        && <p className="text-xs text-slate-500">Source PDF page: {block.page}</p>}
      {block.kind === "text"
        ? onChange ? <textarea aria-label={`Rubric text page ${block.page} block ${index + 1}`} className="min-h-20 w-full rounded-md border border-slate-200 p-2 text-sm leading-6"
            value={block.text ?? ""} onChange={(event) => updateBlock(index, { text: event.target.value })} />
          : <p className="whitespace-pre-wrap text-sm leading-6 text-slate-800">{block.text}</p>
        : <div className="overflow-x-auto rounded-xl border border-slate-200">
          <table aria-label={`Source rubric table, page ${block.page}`} className="w-full table-fixed border-collapse text-sm"
            style={{ minWidth: (block.widths?.length ?? 0) >= 5 ? 1100 : 760 }}>
            <colgroup>{block.widths?.map((width, col) => <col key={col}
              style={{ width: `${width / block.widths!.reduce((sum, item) => sum + item, 0) * 100}%` }} />)}</colgroup>
            <tbody>{block.rows?.map((row, rowIndex) => <tr key={rowIndex} className="align-top">
              {row.map((cell, cellIndex) => <td key={cellIndex} colSpan={cell.colSpan ?? 1} rowSpan={cell.rowSpan ?? 1}
                className="whitespace-pre-wrap break-words border border-slate-200 px-3 py-3 leading-6 text-slate-800">
                {onChange ? <textarea aria-label={`Rubric page ${block.page} block ${index + 1} row ${rowIndex + 1} cell ${cellIndex + 1}`}
                  className="min-h-20 w-full resize-y rounded-md border border-slate-200 bg-white p-2 text-sm leading-6"
                  value={cell.text ?? ""} onChange={(event) => updateBlock(index, {
                    rows: block.rows?.map((cells, row) => row === rowIndex ? cells.map((entry, col) => col === cellIndex ? { ...entry, text: event.target.value } : entry) : cells),
                  })} /> : cell.text ?? ""}
              </td>)}
            </tr>)}</tbody>
          </table>
        </div>}
    </div>)}
  </div>
}
