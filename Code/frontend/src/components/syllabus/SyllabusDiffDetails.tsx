import type { ReactNode } from "react"

import { Card } from "@/components/ui/card"
import type {
  FieldDiff,
  ListDiff,
  SyllabusDiffResponse,
} from "@/types/syllabusDiff"

const asText = (value: unknown) => {
  const text = String(value ?? "").trim()
  return text || "-"
}

const fieldLabel = (field: string) => {
  const labels: Record<string, string> = {
    courseName: "Course Name",
    academicYear: "Academic Year / Applicable Cohort",
    courseDesignation: "Course Designation",
    courseTypes: "Course Types",
    semester: "Semester",
    language: "Language",
    relation: "Course Relationship",
    teachingMethods: "Teaching Methods",
    workloadTotal: "Total Workload",
    workloadContact: "Contact Hours",
    workloadPrivate: "Self-study Hours",
    prerequisites: "Prerequisites",
    objectives: "Course Objectives",
    examForms: "Examination Forms",
    examRequirements: "Examination Requirements",
    rubrics: "Rubric",
    major: "Major",
    notes: "Notes",
    code: "Code",
    description: "Description",
    descriptionVn: "Vietnamese Description",
    competencyLevel: "Competency Level",
    bloomLevel: "Bloom Level",
    orderIndex: "Order",
    plos: "Mapped PLOs",
    weekNumber: "Week",
    orderInWeek: "Order Within Week",
    name: "Name",
    nameVn: "Vietnamese Name",
    teachingHours: "Lecture Hours",
    labHours: "Lab Hours",
    selfStudyHours: "Self-study Hours",
    topicType: "Topic Type",
    teachingMethod: "Teaching Method",
    learningActivity: "Learning Activity",
    assessmentType: "Assessment Type",
    weightPercent: "Weight",
    minScore: "Minimum Score",
    maxScore: "Maximum Score",
    cloCode: "CLO",
    ploCode: "PLO",
    level: "Contribution Level",
    contributionWeight: "Contribution Weight",
    topicName: "Topic",
    teachingLevel: "Teaching Level",
    assessmentName: "Assessment Component",
    contributionPercent: "Contribution Percentage",
    title: "Resource Title",
    author: "Author",
    publisher: "Publisher",
    year: "Publication Year",
    edition: "Edition",
    isbn: "ISBN",
    url: "URL",
    bookType: "Resource Type",
    usageType: "Usage Type",
  }

  return labels[field] ?? field
}

const renderChanges = (changes?: Record<string, FieldDiff>) => {
  if (!changes || Object.keys(changes).length === 0) {
    return (
      <p className="text-sm text-slate-500">
        No detailed field changes.
      </p>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[620px] text-sm">
        <thead>
          <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wide text-slate-500">
            <th className="px-3 py-2 font-semibold">Field</th>
            <th className="px-3 py-2 font-semibold">Previous Version</th>
            <th className="px-3 py-2 font-semibold">New Version</th>
          </tr>
        </thead>
        <tbody>
          {Object.entries(changes).map(([key, change]) => (
            <tr key={key} className="border-b border-slate-100 align-top last:border-0">
              <td className="px-3 py-3 font-semibold text-slate-700">
                {fieldLabel(key)}
              </td>
              <td className="px-3 py-3">
                <span className="inline-block whitespace-pre-wrap rounded bg-red-50 px-2 py-1 text-red-700 line-through">
                  {asText(change.oldValue)}
                </span>
              </td>
              <td className="px-3 py-3">
                <span className="inline-block whitespace-pre-wrap rounded bg-emerald-50 px-2 py-1 font-semibold text-emerald-700">
                  {asText(change.newValue)}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const listCount = <T,>(diff?: ListDiff<T>) =>
  (diff?.added?.length ?? 0)
  + (diff?.removed?.length ?? 0)
  + (diff?.modified?.length ?? 0)

type DiffSectionProps<T extends { changes?: Record<string, FieldDiff> }> = {
  title: string
  diff?: ListDiff<T>
  getTitle: (item: T) => ReactNode
  getDescription?: (item: T) => ReactNode
}

const DiffSection = <T extends { changes?: Record<string, FieldDiff> }>({
  title,
  diff,
  getTitle,
  getDescription,
}: DiffSectionProps<T>) => {
  const added = diff?.added ?? []
  const removed = diff?.removed ?? []
  const modified = diff?.modified ?? []
  const total = added.length + removed.length + modified.length

  if (total === 0) return null

  const renderItemSummary = (item: T) => (
    <>
      <div className="font-semibold text-slate-800">{getTitle(item)}</div>
      {getDescription && (
        <div className="mt-1 text-xs leading-5 text-slate-500">
          {getDescription(item)}
        </div>
      )}
    </>
  )

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-2">
        <h2 className="text-lg font-bold text-slate-900">{title}</h2>
        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">
          {total} changes
        </span>
      </div>

      {added.length > 0 && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50/60 p-4">
          <h3 className="mb-3 text-sm font-bold text-emerald-700">
            Added ({added.length})
          </h3>
          <div className="space-y-2">
            {added.map((item, index) => (
              <div
                key={`${title}-added-${index}`}
                className="rounded-lg border border-emerald-100 bg-white p-3"
              >
                {renderItemSummary(item)}
              </div>
            ))}
          </div>
        </div>
      )}

      {removed.length > 0 && (
        <div className="rounded-xl border border-red-200 bg-red-50/60 p-4">
          <h3 className="mb-3 text-sm font-bold text-red-700">
            Removed ({removed.length})
          </h3>
          <div className="space-y-2">
            {removed.map((item, index) => (
              <div
                key={`${title}-removed-${index}`}
                className="rounded-lg border border-red-100 bg-white p-3 text-red-700 line-through"
              >
                {renderItemSummary(item)}
              </div>
            ))}
          </div>
        </div>
      )}

      {modified.length > 0 && (
        <div className="rounded-xl border border-blue-200 bg-blue-50/50 p-4">
          <h3 className="mb-3 text-sm font-bold text-blue-700">
            Modified ({modified.length})
          </h3>
          <div className="space-y-4">
            {modified.map((item, index) => (
              <div
                key={`${title}-modified-${index}`}
                className="rounded-lg border border-blue-100 bg-white p-4 shadow-sm"
              >
                <div className="mb-3">{renderItemSummary(item)}</div>
                {renderChanges(item.changes)}
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}

export default function SyllabusDiffDetails({
  diff,
}: {
  diff: SyllabusDiffResponse
}) {
  const totalChanges = Object.keys(diff.generalInfoDiff ?? {}).length
    + listCount(diff.cloDiff)
    + listCount(diff.cloPloDiff)
    + listCount(diff.topicDiff)
    + listCount(diff.topicCloDiff)
    + listCount(diff.assessmentDiff)
    + listCount(diff.assessmentCloDiff)
    + listCount(diff.readingListDiff)

  const hasChanges = diff.hasChanges ?? totalChanges > 0

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Card className="border-slate-200 bg-slate-50 p-4">
          <p className="text-xs font-bold uppercase tracking-widest text-slate-500">
            Previous Version
          </p>
          <p className="mt-1 text-xl font-black text-slate-900">
            {diff.oldVersionLabel}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            Syllabus ID: {diff.oldSyllabusId}
          </p>
        </Card>

        <Card className="border-slate-200 bg-slate-50 p-4">
          <p className="text-xs font-bold uppercase tracking-widest text-slate-500">
            New Version
          </p>
          <p className="mt-1 text-xl font-black text-slate-900">
            {diff.newVersionLabel}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            Syllabus ID: {diff.newSyllabusId}
          </p>
        </Card>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
        <div>
          <p className="font-bold text-slate-900">Comparison Results</p>
          <p className="text-sm text-slate-500">
            Includes General Information, CLOs, Topics, Assessments, Reading List, and all mappings.
          </p>
        </div>
        <span className="rounded-full bg-brand-50 px-4 py-2 text-sm font-black text-brand-700">
          {totalChanges} changes
        </span>
      </div>

      {!hasChanges ? (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-6 text-center">
          <p className="font-bold text-emerald-800">
            The two versions have no content differences.
          </p>
        </div>
      ) : (
        <div className="space-y-10">
          {diff.generalInfoDiff
            && Object.keys(diff.generalInfoDiff).length > 0 && (
              <section className="space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-200 pb-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    General Information
                  </h2>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">
                    {Object.keys(diff.generalInfoDiff).length} changes
                  </span>
                </div>
                <div className="rounded-xl border border-slate-200 bg-white p-4">
                  {renderChanges(diff.generalInfoDiff)}
                </div>
              </section>
            )}

          <DiffSection
            title="Course Learning Outcomes (CLO)"
            diff={diff.cloDiff}
            getTitle={(item) => `${asText(item.code)}: ${asText(item.description)}`}
            getDescription={(item) => [
              item.bloomLevel && `Bloom: ${item.bloomLevel}`,
              item.competencyLevel && `Competency: ${item.competencyLevel}`,
              item.plos?.length ? `PLO: ${item.plos.join(", ")}` : undefined,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="CLO–PLO Matrix"
            diff={diff.cloPloDiff}
            getTitle={(item) => `${asText(item.cloCode)} → ${asText(item.ploCode)}`}
            getDescription={(item) => [
              item.level && `Level: ${item.level}`,
              item.contributionWeight !== undefined
                ? `Weight: ${item.contributionWeight}`
                : undefined,
              item.notes,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="Topic / Topics"
            diff={diff.topicDiff}
            getTitle={(item) => asText(item.name)}
            getDescription={(item) => [
              item.weekNumber !== undefined ? `Week ${item.weekNumber}` : undefined,
              item.orderInWeek !== undefined
                ? `Order ${item.orderInWeek}`
                : undefined,
              item.nameVn,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="Topic–CLO Matrix"
            diff={diff.topicCloDiff}
            getTitle={(item) => `${asText(item.topicName)} → ${asText(item.cloCode)}`}
            getDescription={(item) => [
              item.weekNumber !== undefined ? `Week ${item.weekNumber}` : undefined,
              item.orderInWeek !== undefined
                ? `Order ${item.orderInWeek}`
                : undefined,
              item.teachingLevel && `Level: ${item.teachingLevel}`,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="Assessment Component"
            diff={diff.assessmentDiff}
            getTitle={(item) => asText(item.name)}
            getDescription={(item) => [
              item.weightPercent !== undefined
                ? `Weight: ${item.weightPercent}%`
                : undefined,
              item.orderIndex !== undefined
                ? `Order ${item.orderIndex}`
                : undefined,
              item.nameVn,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="Assessment–CLO Matrix"
            diff={diff.assessmentCloDiff}
            getTitle={(item) => `${asText(item.assessmentName)} → ${asText(item.cloCode)}`}
            getDescription={(item) => [
              item.contributionPercent !== undefined
                ? `Contribution: ${item.contributionPercent}%`
                : undefined,
              item.orderIndex !== undefined
                ? `Order ${item.orderIndex}`
                : undefined,
            ].filter(Boolean).join(" · ") || "-"}
          />

          <DiffSection
            title="Reading List"
            diff={diff.readingListDiff}
            getTitle={(item) => asText(item.title)}
            getDescription={(item) => [
              item.author,
              item.year,
              item.publisher,
              item.usageType && `Usage: ${item.usageType}`,
              item.isbn && `ISBN: ${item.isbn}`,
            ].filter(Boolean).join(" · ") || "-"}
          />
        </div>
      )}
    </div>
  )
}
