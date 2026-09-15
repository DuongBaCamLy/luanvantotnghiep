import { useQuery } from "@tanstack/react-query"
import { useLocation, useNavigate } from "react-router-dom"

import { syllabusApi } from "@/api/syllabusApi"
import { useSyllabus } from "@/hooks/useSyllabus"
import { getSyllabusBasePath } from "@/lib/programContext"
import { formatVersionLabel } from "@/lib/syllabusVersion"
import type { Syllabus } from "@/types/syllabus"

const normalizeStatus = (value: unknown) =>
  String(value ?? "")
    .trim()
    .toUpperCase()

const statusLabel = (value: unknown) => {
  const status = normalizeStatus(value)

  const labels: Record<string, string> = {
    DRAFT: "Draft",
    SUBMITTED: "Submitted",
    UNDER_REVIEW: "Under Review",
    APPROVED: "Approved",
    REJECTED: "Rejected",
    REVISION_REQUESTED: "Revision Requested",
    ARCHIVED: "Archived",
  }

  return labels[status] ?? status ?? "Unknown"
}

const statusClass = (value: unknown) => {
  switch (normalizeStatus(value)) {
    case "APPROVED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700"

    case "DRAFT":
      return "border-amber-200 bg-amber-50 text-amber-700"

    case "SUBMITTED":
    case "UNDER_REVIEW":
      return "border-blue-200 bg-blue-50 text-blue-700"

    case "REJECTED":
    case "REVISION_REQUESTED":
      return "border-rose-200 bg-rose-50 text-rose-700"

    default:
      return "border-slate-200 bg-slate-50 text-slate-600"
  }
}

const formatDate = (value?: string | null) => {
  if (!value) return "—"

  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return "—"
  }

  return parsed.toLocaleString()
}

const cohortName = (syllabus: Syllabus) =>
  String(
    syllabus.cohortName
      ?? syllabus.academicYear
      ?? "",
  ).trim()

const cohortYear = (syllabus: Syllabus) => {
  const value = cohortName(syllabus)
  const match = value.match(/(\d{4})(?!.*\d)/)

  return match ? Number(match[1]) : 0
}

/**
 * Syllabuses may belong to the same Course but different Programs.
 * Cohort history must never mix those programs.
 */
const sameProgram = (
  left: Syllabus,
  right: Syllabus,
) => {
  if (
    left.programId != null
    && right.programId != null
  ) {
    return left.programId === right.programId
  }

  const leftCode =
    String(left.programCode ?? "")
      .trim()
      .toUpperCase()

  const rightCode =
    String(right.programCode ?? "")
      .trim()
      .toUpperCase()

  return Boolean(leftCode)
    && leftCode === rightCode
}

export default function SyllabusRevisionHistory({
  syllabusId,
}: {
  syllabusId: number
}) {
  const navigate = useNavigate()
  const location = useLocation()

  const syllabusBasePath =
    getSyllabusBasePath(location.pathname)

  /*
   * Current logical syllabus.
   *
   * IMPORTANT:
   * We are NOT loading syllabus_revision_snapshot here.
   * Snapshots are internal revision/audit history and must not
   * appear as separate syllabus rows in this UI.
   */
  const currentQuery =
    useSyllabus(syllabusId)

  const current =
    currentQuery.data

  /*
   * Load the actual logical syllabus rows for this Course.
   *
   * Example:
   *
   * IT116IU
   *   CS2021 -> syllabus 3055
   *   CS2026 -> syllabus 3013
   *
   * Those are the two rows that this table must show.
   */
  const cohortQuery = useQuery({
    queryKey: [
      "syllabuses",
      "course",
      current?.courseId,
    ],

    queryFn: () =>
      syllabusApi.getByCourse(
        current!.courseId,
      ),

    enabled:
      Boolean(current?.courseId),
  })

  const cohortSyllabuses =
    (cohortQuery.data ?? [])
      .filter(
        (item) =>
          current != null
          && sameProgram(item, current),
      )
      /*
       * Old cohort -> new cohort:
       * CS2021
       * CS2026
       */
      .sort((a, b) => {
        const yearDiff =
          cohortYear(a) - cohortYear(b)

        if (yearDiff !== 0) {
          return yearDiff
        }

        return a.id - b.id
      })

  /*
   * Comparison business rule:
   *
   * - SAME course
   * - SAME program
   * - DIFFERENT cohorts
   * - BOTH APPROVED
   * - use nearest older approved cohort
   */
  const previousApprovedFor = (
    syllabus: Syllabus,
  ) => {
    if (
      normalizeStatus(syllabus.status)
      !== "APPROVED"
    ) {
      return null
    }

    const currentYear =
      cohortYear(syllabus)

    return (
      cohortSyllabuses
        .filter(
          (candidate) =>
            candidate.id !== syllabus.id
            && normalizeStatus(
              candidate.status,
            ) === "APPROVED"
            && cohortYear(candidate)
              < currentYear,
        )
        .sort(
          (a, b) =>
            cohortYear(b)
            - cohortYear(a),
        )[0]
      ?? null
    )
  }

  const loading =
    currentQuery.isLoading
    || (
      Boolean(current)
      && cohortQuery.isLoading
    )

  const error =
    currentQuery.isError
    || cohortQuery.isError

  return (
    <section
      id="approval-history"
      className="scroll-mt-24 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
    >
      <div className="border-b border-slate-200 px-5 py-4">
        <h2 className="text-lg font-semibold text-[#17343d]">
          Syllabus History
        </h2>

        <p className="mt-1 text-xs text-slate-500">
          One syllabus per cohort for this course and program.
          Comparison is available only between approved cohorts.
        </p>
      </div>

      {loading && (
        <p className="p-5 text-sm text-slate-500">
          Loading syllabus history…
        </p>
      )}

      {error && (
        <p
          role="alert"
          className="p-5 text-sm text-rose-700"
        >
          Unable to load syllabus history.
        </p>
      )}

      {!loading
        && !error
        && cohortSyllabuses.length === 0 && (
          <p className="p-5 text-sm text-slate-500">
            No syllabus records found for this course.
          </p>
        )}

      {!loading
        && !error
        && cohortSyllabuses.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[950px] text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50">
                <tr className="text-[11px] font-bold uppercase tracking-wide text-slate-500">
                  <th className="px-5 py-3">
                    Version
                  </th>

                  <th className="px-5 py-3">
                    Cohort
                  </th>

                  <th className="px-5 py-3">
                    Semester
                  </th>

                  <th className="px-5 py-3">
                    Status
                  </th>

                  <th className="px-5 py-3">
                    Created / Imported By
                  </th>

                  <th className="px-5 py-3">
                    Final Approval Date
                  </th>

                  <th className="px-5 py-3 text-right">
                    Actions
                  </th>
                </tr>
              </thead>

              <tbody>
                {cohortSyllabuses.map(
                  (item) => {
                    const previousApproved =
                      previousApprovedFor(item)

                    const approved =
                      normalizeStatus(
                        item.status,
                      ) === "APPROVED"

                    return (
                      <tr
                        key={item.id}
                        className={
                          `border-b border-slate-100 last:border-0 ${
                            item.id === syllabusId
                              ? "bg-teal-50/30"
                              : ""
                          }`
                        }
                      >
                        <td className="px-5 py-4">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-slate-800">
                              {formatVersionLabel(
                                item.versionNumber,
                              )}
                            </span>

                            {item.id
                              === syllabusId && (
                              <span className="rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                                Viewing
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-4 font-medium text-slate-700">
                          {cohortName(item)
                            || "—"}
                        </td>

                        <td className="px-5 py-4 text-slate-600">
                          {item.semester
                            || "—"}
                        </td>

                        <td className="px-5 py-4">
                          <span
                            className={`inline-flex rounded-md border px-2 py-1 text-xs font-semibold ${statusClass(
                              item.status,
                            )}`}
                          >
                            {statusLabel(
                              item.status,
                            )}
                          </span>
                        </td>

                        <td className="px-5 py-4 text-slate-600">
                          {item.createdByUsername
                            || "—"}
                        </td>

                        <td className="px-5 py-4 text-xs text-slate-500">
                          {formatDate(
                            item.finalApprovalDate
                            ?? item.approvedAt,
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex justify-end gap-2">
                            {item.id
                              !== syllabusId && (
                              <button
                                type="button"
                                onClick={() =>
                                  navigate(
                                    `${syllabusBasePath}/${item.id}`,
                                  )
                                }
                                className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-50"
                              >
                                View
                              </button>
                            )}

                            {approved
                              && previousApproved && (
                              <button
                                type="button"
                                onClick={() =>
                                  navigate(
                                    `${syllabusBasePath}/${item.id}/diff?compareWith=${previousApproved.id}`,
                                  )
                                }
                                className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-semibold text-blue-700 transition hover:bg-blue-100"
                              >
                                Compare
                              </button>
                            )}

                            {!approved && (
                              <span className="self-center text-xs text-slate-400">
                                Approval required
                              </span>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  },
                )}
              </tbody>
            </table>
          </div>
        )}
    </section>
  )
}