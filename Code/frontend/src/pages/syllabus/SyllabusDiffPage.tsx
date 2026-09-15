import type { ReactNode } from "react"
import { useParams, useSearchParams } from "react-router-dom"

import SyllabusComparisonView from "@/components/syllabus/comparison/SyllabusComparisonView"
import SemanticProviderStatusCard from "@/components/syllabus/comparison/SemanticProviderStatusCard"
import SyllabusToolbar from "@/components/syllabus/SyllabusToolbar"
import { Card } from "@/components/ui/card"
import { useSyllabus } from "@/hooks/useSyllabus"
import { useSyllabusDiff } from "@/hooks/useSyllabusDiff"
import { useSyllabusSemanticDiff } from "@/hooks/useSyllabusSemanticDiff"
import type { SyllabusDiffResponse } from "@/types/syllabusDiff"

const normalizeStatus = (value: unknown) =>
  String(value ?? "")
    .trim()
    .toUpperCase()

export default function SyllabusDiffPage() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()

  const compareWith =
    searchParams.get("compareWith")

  const newId =
    Number(id)

  const oldId =
    Number(compareWith)

  const validIds =
    Number.isInteger(newId)
    && newId > 0
    && Number.isInteger(oldId)
    && oldId > 0
    && oldId !== newId

  /*
   * Route convention:
   *
   *   id          = NEW / target cohort syllabus
   *   compareWith = OLD / baseline cohort syllabus
   *
   * Load both logical syllabus snapshots FIRST. Structural comparison is
   * enabled only after the browser can confirm the same public comparison
   * rules that the backend enforces authoritatively.
   */
  const oldSyllabusQuery =
    useSyllabus(
      validIds
        ? oldId
        : null,
    )

  const newSyllabusQuery =
    useSyllabus(
      validIds
        ? newId
        : null,
    )

  const oldSyllabus =
    oldSyllabusQuery.data

  const newSyllabus =
    newSyllabusQuery.data

  const bothLoaded =
    Boolean(
      oldSyllabus
      && newSyllabus,
    )

  const bothApproved =
    bothLoaded
    && normalizeStatus(
      oldSyllabus?.status,
    ) === "APPROVED"
    && normalizeStatus(
      newSyllabus?.status,
    ) === "APPROVED"

  const sameCourse =
    bothLoaded
    && oldSyllabus?.courseId != null
    && newSyllabus?.courseId != null
    && oldSyllabus.courseId
      === newSyllabus.courseId

  const sameProgram =
    bothLoaded
    && oldSyllabus?.programId != null
    && newSyllabus?.programId != null
    && oldSyllabus.programId
      === newSyllabus.programId

  const differentCohorts =
    bothLoaded
    && oldSyllabus?.cohortId != null
    && newSyllabus?.cohortId != null
    && oldSyllabus.cohortId
      !== newSyllabus.cohortId

  const comparisonEligible =
    validIds
    && bothApproved
    && sameCourse
    && sameProgram
    && differentCohorts

  /*
   * The structural API is intentionally NOT called for Draft, Submitted,
   * Under Review, Rejected, same-cohort, different-course, or
   * different-program pairs.
   *
   * The backend repeats all of these checks; this browser guard is only for
   * correct UX and never replaces backend authorization/business validation.
   */
  const structuralQuery =
    useSyllabusDiff(
      validIds ? newId : 0,
      validIds ? oldId : 0,
      comparisonEligible,
    )

  /*
   * Semantic comparison is on-demand. Because its Analyze button exists only
   * after comparisonEligible succeeds, a non-approved pair cannot trigger the
   * AI endpoint from this screen. The backend enforces the same rule again.
   */
  const semanticMutation =
    useSyllabusSemanticDiff(
      validIds ? newId : 0,
      validIds ? oldId : 0,
    )

  if (!compareWith || !validIds) {
    return (
      <MessageCard>
        Select two different cohort syllabuses to compare.
      </MessageCard>
    )
  }

  if (
    oldSyllabusQuery.isLoading
    || newSyllabusQuery.isLoading
  ) {
    return (
      <MessageCard>
        Loading syllabus comparison context...
      </MessageCard>
    )
  }

  if (
    oldSyllabusQuery.isError
    || newSyllabusQuery.isError
    || !oldSyllabus
    || !newSyllabus
  ) {
    return (
      <MessageCard tone="error">
        Unable to load both syllabus cohorts. Verify that both records exist
        and that you have permission to view them.
      </MessageCard>
    )
  }

  if (!bothApproved) {
    return (
      <MessageCard tone="error">
        Cohort comparison is available only when both syllabuses are
        APPROVED. Draft, submitted, under-review, rejected, or archived
        syllabuses cannot be compared.
      </MessageCard>
    )
  }

  if (!sameCourse) {
    return (
      <MessageCard tone="error">
        These syllabuses belong to different courses. Comparison requires the
        same course in two different cohorts.
      </MessageCard>
    )
  }

  if (!sameProgram) {
    return (
      <MessageCard tone="error">
        These syllabuses belong to different programs. Comparison requires
        the same course and the same program.
      </MessageCard>
    )
  }

  if (!differentCohorts) {
    return (
      <MessageCard tone="error">
        Comparison requires two different cohorts. Revisions inside the same
        cohort are not visible cohort comparisons.
      </MessageCard>
    )
  }

  if (structuralQuery.isLoading) {
    return (
      <MessageCard>
        Loading approved cohort comparison...
      </MessageCard>
    )
  }

  if (
    structuralQuery.isError
    || !structuralQuery.data
  ) {
    return (
      <MessageCard tone="error">
        Unable to load the structural comparison. The server rejected this
        pair or the comparison data is unavailable.
      </MessageCard>
    )
  }

  const structuralDiff =
    structuralQuery.data as SyllabusDiffResponse

  const semanticError =
    semanticMutation.isError
      ? semanticMutation.error instanceof Error
        ? semanticMutation.error.message
        : "Unable to run semantic analysis."
      : null

  return (
    <div
      data-admin-page="SyllabusDiffPage"
      className="space-y-6"
    >
      <SyllabusToolbar />

      <SemanticProviderStatusCard
        enabled={comparisonEligible}
      />

      <SyllabusComparisonView
        oldSyllabus={oldSyllabus}
        newSyllabus={newSyllabus}
        structuralDiff={structuralDiff}
        semanticDiff={semanticMutation.data}
        semanticLoading={semanticMutation.isPending}
        semanticError={semanticError}
        onAnalyzeSemantic={() =>
          semanticMutation.mutate()
        }
      />
    </div>
  )
}

function MessageCard({
  children,
  tone = "normal",
}: {
  children: ReactNode
  tone?: "normal" | "error"
}) {
  return (
    <div className="space-y-6">
      <SyllabusToolbar />

      <Card
        className={
          tone === "error"
            ? "p-6 text-center text-rose-600"
            : "p-6 text-center text-slate-500"
        }
      >
        {children}
      </Card>
    </div>
  )
}
