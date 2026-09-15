import type {
  SemanticChangeType,
  SemanticDiffResult,
  SemanticSectionType,
  SemanticSyllabusDiffResponse,
} from "@/types/syllabusDiff"

const asText = (
  value: unknown
) => {
  const text =
    String(value ?? "").trim()

  return text || "-"
}

const formatEnum = (
  value: string | null | undefined
) =>
  value
    ? value
        .toLowerCase()
        .split("_")
        .map(
          (part) =>
            part.charAt(0).toUpperCase()
            + part.slice(1)
        )
        .join(" ")
    : "-"

const sectionLabel = (
  section: SemanticSectionType
) => {
  const labels: Record<
    SemanticSectionType,
    string
  > = {
    COURSE_OBJECTIVE:
      "Course Objective",

    CLO:
      "Course Learning Outcome",

    TOPIC:
      "Topic",

    TEACHING_METHOD:
      "Teaching Method",

    LEARNING_ACTIVITY:
      "Learning Activity",

    EXAM_REQUIREMENT:
      "Examination Requirement",
  }

  return labels[section]
}

const classificationClass = (
  classification:
    | SemanticChangeType
    | null
    | undefined,
  requiresAi: boolean
) => {

  if (requiresAi) {
    return "border-amber-200 bg-amber-50 text-amber-700"
  }

  switch (classification) {

    case "MEANINGFUL_CHANGE":
      return "border-rose-200 bg-rose-50 text-rose-700"

    case "MINOR_REWORDING":
      return "border-blue-200 bg-blue-50 text-blue-700"

    case "NO_MEANINGFUL_CHANGE":
      return "border-emerald-200 bg-emerald-50 text-emerald-700"

    default:
      return "border-slate-200 bg-slate-50 text-slate-600"
  }
}

const classificationLabel = (
  item: SemanticDiffResult
) => {

  if (item.requiresAi) {
    return "Needs AI analysis"
  }

  return formatEnum(
    item.classification
  )
}

const SemanticItem = ({
  item,
}: {
  item: SemanticDiffResult
}) => {

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">

      <div className="flex flex-wrap items-start justify-between gap-3">

        <div>
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">
            {sectionLabel(
              item.sectionType
            )}
          </p>

          <p className="mt-1 font-semibold text-slate-900">
            {item.fieldName}
          </p>

          <p className="mt-1 font-mono text-[11px] text-slate-400">
            {item.itemId}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">

          <span
            className={`rounded-full border px-3 py-1 text-xs font-bold ${classificationClass(
              item.classification,
              item.requiresAi
            )}`}
          >
            {classificationLabel(item)}
          </span>

          {item.changeNature && (
            <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-semibold text-slate-600">
              {formatEnum(
                item.changeNature
              )}
            </span>
          )}

          {item.significance && (
            <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold text-slate-600">
              {formatEnum(
                item.significance
              )} significance
            </span>
          )}

        </div>
      </div>


      <div className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-2">

        <div className="rounded-lg border border-red-100 bg-red-50/50 p-3">
          <p className="mb-2 text-xs font-bold uppercase tracking-wide text-red-600">
            Previous Version
          </p>

          <p className="whitespace-pre-wrap text-sm leading-6 text-slate-700">
            {asText(item.oldText)}
          </p>
        </div>


        <div className="rounded-lg border border-emerald-100 bg-emerald-50/50 p-3">
          <p className="mb-2 text-xs font-bold uppercase tracking-wide text-emerald-600">
            New Version
          </p>

          <p className="whitespace-pre-wrap text-sm leading-6 text-slate-700">
            {asText(item.newText)}
          </p>
        </div>

      </div>


      {(item.oldMeaning
        || item.newMeaning) && (

        <div className="mt-4 grid grid-cols-1 gap-3 lg:grid-cols-2">

          <div className="rounded-lg bg-slate-50 p-3">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
              Previous Meaning
            </p>

            <p className="mt-2 text-sm leading-6 text-slate-700">
              {asText(
                item.oldMeaning
              )}
            </p>
          </div>


          <div className="rounded-lg bg-slate-50 p-3">
            <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
              New Meaning
            </p>

            <p className="mt-2 text-sm leading-6 text-slate-700">
              {asText(
                item.newMeaning
              )}
            </p>
          </div>

        </div>
      )}


      {item.summary && (
        <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-xs font-bold uppercase tracking-wide text-slate-500">
            Analysis
          </p>

          <p className="mt-1 text-sm leading-6 text-slate-700">
            {item.summary}
          </p>
        </div>
      )}

    </div>
  )
}


export default function SemanticSyllabusDiffDetails({
  diff,
}: {
  diff: SemanticSyllabusDiffResponse
}) {

  const meaningful =
    diff.items.filter(
      (item) =>
        item.classification
        === "MEANINGFUL_CHANGE"
    )

  const rewording =
    diff.items.filter(
      (item) =>
        item.classification
        === "MINOR_REWORDING"
    )

  const unresolved =
    diff.items.filter(
      (item) =>
        item.requiresAi
    )

  const unchangedCount =
    diff.items.filter(
      (item) =>
        item.classification
        === "NO_MEANINGFUL_CHANGE"
    ).length

  /*
   * Keep the main UI focused:
   * unchanged items are counted,
   * not expanded into large cards.
   */
  const visibleItems = [
    ...meaningful,
    ...rewording,
    ...unresolved,
  ]

  return (
    <div className="space-y-6">

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">

        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4">
          <p className="text-2xl font-black text-rose-700">
            {meaningful.length}
          </p>
          <p className="mt-1 text-xs font-semibold text-rose-700">
            Meaningful Changes
          </p>
        </div>

        <div className="rounded-xl border border-blue-200 bg-blue-50 p-4">
          <p className="text-2xl font-black text-blue-700">
            {rewording.length}
          </p>
          <p className="mt-1 text-xs font-semibold text-blue-700">
            Minor Rewordings
          </p>
        </div>

        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <p className="text-2xl font-black text-emerald-700">
            {unchangedCount}
          </p>
          <p className="mt-1 text-xs font-semibold text-emerald-700">
            Same Meaning
          </p>
        </div>

        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
          <p className="text-2xl font-black text-amber-700">
            {unresolved.length}
          </p>
          <p className="mt-1 text-xs font-semibold text-amber-700">
            Unresolved
          </p>
        </div>

      </div>


      <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">

        <div className="flex flex-wrap items-center gap-2">

          <span className="text-sm font-bold text-slate-900">
            Analysis status:
          </span>

          <span className="rounded-full border border-slate-300 bg-white px-3 py-1 text-xs font-bold text-slate-700">
            {formatEnum(
              diff.status
            )}
          </span>

          {diff.overallSignificance && (
            <span className="rounded-full border border-slate-300 bg-white px-3 py-1 text-xs font-bold text-slate-700">
              Overall:{" "}
              {formatEnum(
                diff.overallSignificance
              )}
            </span>
          )}

        </div>

        {diff.summary && (
          <p className="mt-3 text-sm leading-6 text-slate-600">
            {diff.summary}
          </p>
        )}

      </div>


      {diff.status === "DISABLED" && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          AI semantic analysis is currently disabled.
          Deterministic results are still shown,
          while unresolved items are not classified.
        </div>
      )}


      {(
        diff.status === "PARTIAL"
        || diff.status === "UNAVAILABLE"
      ) && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          AI analysis was not fully available.
          Unresolved items are intentionally left
          unclassified instead of being treated as
          no meaningful change.
        </div>
      )}


      {visibleItems.length === 0 ? (

        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-6 text-center">
          <p className="font-bold text-emerald-800">
            No meaningful academic changes were detected.
          </p>
        </div>

      ) : (

        <div className="space-y-4">

          {visibleItems.map(
            (item) => (
              <SemanticItem
                key={item.itemId}
                item={item}
              />
            )
          )}

        </div>

      )}

    </div>
  )
}