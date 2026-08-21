import { useEffect, useMemo, useState } from "react"
import axios from "axios"
import {
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import {
  AlertTriangle,
  ArrowLeft,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Download,
  Edit3,
  FileSearch2,
  GitCompareArrows,
  GraduationCap,
  History,
  Layers3,
  ListChecks,
  LoaderCircle,
  MessageSquareText,
  ShieldCheck,
  UserRound,
} from "lucide-react"

import { syllabusApi } from "@/api/syllabusApi"
import { syllabusPdfApi } from "@/api/syllabusPdfApi"
import ApprovalHistoryTable from "@/components/approval/ApprovalHistoryTable"
import SyllabusPdfPreviewDialog from "@/components/syllabus/SyllabusPdfPreviewDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { useSyllabus } from "@/hooks/useSyllabus"
import { getSyllabusBasePath } from "@/lib/programContext"
import { useAuthStore } from "@/store/authStore"
import type { Syllabus } from "@/types/syllabus"

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "Draft",
  SUBMITTED: "Submitted",
  UNDER_REVIEW: "Under Review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  REVISION_REQUESTED: "Revision Requested",
  ARCHIVED: "Archived",
}

const normalizeRole = (
  value: unknown,
) =>
  String(value ?? "")
    .replace(/^ROLE_/i, "")
    .toUpperCase()
    .trim()

const normalizeStatus = (
  value: unknown,
) =>
  String(value ?? "")
    .toUpperCase()
    .trim()

const formatStatusLabel = (
  value: unknown,
) => {
  const status =
    normalizeStatus(value)

  return (
    STATUS_LABELS[status]
    || status
    || "Unknown"
  )
}

const formatStatusLabelForRole = (
  value: unknown,
  role: string,
) => {
  const status =
    normalizeStatus(value)

  if (
    role === "DEPT_HEAD"
  ) {
    if (
      status === "SUBMITTED"
    ) {
      return "Pending Department Review"
    }

    if (
      status === "UNDER_REVIEW"
    ) {
      return "Forwarded to Dean"
    }
  }

  if (
    role === "DEAN"
    && status === "UNDER_REVIEW"
  ) {
    return "Pending Final Review"
  }

  return formatStatusLabel(
    value,
  )
}

const workflowPositionLabel = (
  value: unknown,
  role: string,
) => {
  const status =
    normalizeStatus(value)

  if (
    status === "DRAFT"
  ) {
    return "Instructor preparation"
  }

  if (
    status === "SUBMITTED"
  ) {
    return role === "DEPT_HEAD"
      ? "Department decision required"
      : "Awaiting department review"
  }

  if (
    status === "UNDER_REVIEW"
  ) {
    return role === "DEAN"
      ? "Dean final decision required"
      : "Department review completed · awaiting Dean"
  }

  if (
    status === "APPROVED"
  ) {
    return "Approval workflow completed"
  }

  if (
    status === "REJECTED"
    || status
      === "REVISION_REQUESTED"
  ) {
    return "Returned to instructor for revision"
  }

  if (
    status === "ARCHIVED"
  ) {
    return "Historical / archived version"
  }

  return formatStatusLabel(
    value,
  )
}

const getStatusClass = (
  value: unknown,
  role = "",
) => {
  const status =
    normalizeStatus(value)

  if (status === "APPROVED") {
    return "border-emerald-200 bg-emerald-50 text-emerald-700"
  }

  if (
    role === "DEPT_HEAD"
    && status === "SUBMITTED"
  ) {
    return "border-amber-200 bg-amber-50 text-amber-700"
  }

  if (
    status === "SUBMITTED"
    || status === "UNDER_REVIEW"
  ) {
    return "border-blue-200 bg-blue-50 text-blue-700"
  }

  if (
    status === "REJECTED"
    || status === "REVISION_REQUESTED"
  ) {
    return "border-rose-200 bg-rose-50 text-rose-700"
  }

  if (status === "ARCHIVED") {
    return "border-slate-300 bg-slate-100 text-slate-600"
  }

  return "border-amber-200 bg-amber-50 text-amber-700"
}

const formatDate = (
  value: string | null | undefined,
) => {
  if (!value) {
    return "—"
  }

  const date =
    new Date(value)

  if (
    Number.isNaN(
      date.getTime(),
    )
  ) {
    return value
  }

  return new Intl.DateTimeFormat(
    "en-GB",
    {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    },
  ).format(date)
}

const formatSemester = (
  value: string | null | undefined,
) => {
  const semester =
    String(value ?? "")
      .trim()

  if (!semester) {
    return "Not specified"
  }

  const matched =
    semester.match(
      /semester\s*(\d+)/i,
    )

  const normalized =
    matched?.[1]
    ?? semester

  return /^\d+$/.test(normalized)
    ? `Semester ${normalized}`
    : normalized
}

const normalizeDisplayText = (
  value: string | null | undefined,
) => {
  const text =
    String(value ?? "")
      .trim()

  if (!text) {
    return null
  }

  const legacyPlaceholders = new Set([
    "Course Code",
    "Course Name",
    "TBD",
    "N/A",
    "NA",
    "-",
  ])

  return legacyPlaceholders.has(text)
    ? null
    : text
}

const formatNotesForDisplay = (
  value: string | null | undefined,
) => {
  const text =
    String(value ?? "")
      .trim()

  if (!text) {
    return null
  }

  if (
    text.startsWith("{")
    || text.startsWith("[")
  ) {
    try {
      JSON.parse(text)
      return "Technical metadata is stored with this syllabus version and is not displayed in this view."
    } catch {
      // Keep non-JSON notes visible.
    }
  }

  return text
}

const versionLabel = (
  syllabus: Syllabus,
) =>
  syllabus.versionLabel
  || `v${syllabus.versionNumber}`

const compareVersionAscending = (
  left: Syllabus,
  right: Syllabus,
) => {
  const leftVersion =
    Number(
      left.versionNumber
      ?? 0,
    )

  const rightVersion =
    Number(
      right.versionNumber
      ?? 0,
    )

  if (
    leftVersion
    !== rightVersion
  ) {
    return (
      leftVersion
      - rightVersion
    )
  }

  return left.id - right.id
}

const safeFilePart = (
  value: unknown,
  fallback = "NA",
) => {
  const text =
    String(value ?? "")
      .trim()

  if (!text) {
    return fallback
  }

  return (
    text
      .normalize("NFD")
      .replace(
        /[\u0300-\u036f]/g,
        "",
      )
      .replace(
        /[^a-zA-Z0-9._-]+/g,
        "-",
      )
      .replace(
        /^-+|-+$/g,
        "",
      )
    || fallback
  )
}

const getPdfErrorMessage =
  async (
    error: unknown,
  ) => {
    if (
      axios.isAxiosError(
        error,
      )
    ) {
      const responseData =
        error.response?.data

      if (
        responseData
        instanceof Blob
      ) {
        try {
          const text =
            await responseData.text()

          if (!text) {
            return undefined
          }

          try {
            const parsed =
              JSON.parse(text) as {
                message?: string
                error?: string
              }

            return (
              parsed.message
              || parsed.error
              || text
            )
          } catch {
            return text
          }
        } catch {
          return undefined
        }
      }

      if (
        typeof responseData
        === "object"
        && responseData !== null
      ) {
        const candidate =
          responseData as {
            message?: string
            error?: string
          }

        return (
          candidate.message
          || candidate.error
        )
      }

      if (
        typeof responseData
        === "string"
      ) {
        return responseData
      }

      return error.message
    }

    if (
      error instanceof Error
    ) {
      return error.message
    }

    return undefined
  }

export default function SyllabusDetailPage() {
  const { id } =
    useParams()

  const navigate =
    useNavigate()

  const location =
    useLocation()

  const user =
    useAuthStore(
      (state) => state.user,
    )

  const syllabusBasePath =
    getSyllabusBasePath(
      location.pathname,
    )

  const syllabusId =
    Number(id)

  const [
    previewOpen,
    setPreviewOpen,
  ] = useState(false)

  const [
    downloading,
    setDownloading,
  ] = useState(false)

  const {
    data: syllabus,
    isLoading,
    isError,
  } = useSyllabus(
    Number.isFinite(
      syllabusId,
    )
      ? syllabusId
      : null,
  )

  const {
    data: courseVersions = [],
    isLoading: versionsLoading,
  } = useQuery({
    queryKey: [
      "syllabuses-course",
      syllabus?.courseId,
    ],

    queryFn: () =>
      syllabusApi.getByCourse(
        syllabus!.courseId,
      ),

    enabled:
      Boolean(
        syllabus?.courseId,
      ),
  })

  const sortedVersions =
    useMemo(
      () =>
        courseVersions
          .slice()
          .sort(
            compareVersionAscending,
          ),
      [courseVersions],
    )

  const currentVersionIndex =
    useMemo(
      () =>
        sortedVersions
          .findIndex(
            (version) =>
              version.id
              === syllabus?.id,
          ),
      [
        sortedVersions,
        syllabus?.id,
      ],
    )

  const previousVersion =
    currentVersionIndex > 0
      ? sortedVersions[
          currentVersionIndex - 1
        ]
      : undefined

  const versionsNewestFirst =
    useMemo(
      () =>
        sortedVersions
          .slice()
          .reverse(),
      [sortedVersions],
    )

  const role =
    normalizeRole(
      user?.role,
    )

  const isInstructor =
    role === "INSTRUCTOR"

  const isAdmin =
    role === "ADMIN"

  const isDean =
    role === "DEAN"

  const isDeptHead =
    role === "DEPT_HEAD"

  const isOwner =
    Boolean(
      isInstructor
      && syllabus
      && user?.userId
        === syllabus.createdById,
    )

  const isOversightRole =
    [
      "ADMIN",
      "DEAN",
      "DEPT_HEAD",
    ].includes(role)

  const canViewApprovalHistory =
    Boolean(
      syllabus && user,
    )

  const canEditDraft =
    Boolean(
      syllabus
      && isOwner
      && normalizeStatus(
        syllabus.status,
      ) === "DRAFT",
    )

  const currentStatus =
    normalizeStatus(
      syllabus?.status,
    )

  const isApproved =
    currentStatus
      === "APPROVED"

  const requiresCurrentReviewerAction =
    (
      isDeptHead
      && currentStatus
        === "SUBMITTED"
    )
    || (
      isDean
      && currentStatus
        === "UNDER_REVIEW"
    )

  const isRevisionState =
    [
      "REJECTED",
      "REVISION_REQUESTED",
    ].includes(
      currentStatus,
    )

  const scrollToApprovalHistory =
    () => {
      document
        .getElementById(
          "approval-history",
        )
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        })
    }

  const scrollToVersionHistory =
    () => {
      document
        .getElementById(
          "version-history",
        )
        ?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        })
    }

  useEffect(() => {
    if (
      !syllabus
      || location.hash
        !== "#approval-history"
    ) {
      return
    }

    const timeoutId =
      window.setTimeout(
        () => {
          scrollToApprovalHistory()
        },
        100,
      )

    return () =>
      window.clearTimeout(
        timeoutId,
      )
  }, [
    location.hash,
    syllabus,
  ])

  if (
    !id
    || Number.isNaN(
      syllabusId,
    )
  ) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-rose-700">
        Invalid syllabus ID.
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center text-slate-500">
        <LoaderCircle className="mr-2 size-5 animate-spin" />
        Loading syllabus...
      </div>
    )
  }

  if (
    isError
    || !syllabus
  ) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-rose-700">
        The syllabus was not found or you do not have permission to access it.
      </div>
    )
  }

  const handleDownload =
    async () => {
      if (!isApproved) {
        alert(
          "Only APPROVED syllabuses can be exported as official PDFs.",
        )
        return
      }

      try {
        setDownloading(true)

        await syllabusPdfApi.download(
          syllabus.id,
          [
            "Syllabus",
            safeFilePart(
              syllabus.courseCode,
            ),
            safeFilePart(
              versionLabel(
                syllabus,
              ),
            ),
            "Official.pdf",
          ].join("_"),
        )
      } catch (error) {
        const message =
          await getPdfErrorMessage(
            error,
          )

        alert(
          message
          ?? "Unable to export the official syllabus PDF.",
        )
      } finally {
        setDownloading(
          false,
        )
      }
    }

  const pageEyebrow =
    isDeptHead
      ? "SCSE / Department Syllabus Review"
      : isDean
        ? "SCSE / Final Syllabus Review"
        : isAdmin
          ? "SCSE / Syllabus Administration"
          : "SCSE / Syllabus Detail"

  const workflowPosition =
    workflowPositionLabel(
      syllabus.status,
      role,
    )

  const approvedByDisplay =
    isApproved
      ? (
          syllabus
            .approvedByUsername
          || "Not recorded"
        )
      : "Not approved"

  const approvedAtDisplay =
    isApproved
      ? (
          syllabus.approvedAt
            ? formatDate(
                syllabus
                  .approvedAt,
              )
            : "Not recorded"
        )
      : "Not approved"

  const goToPreviousComparison =
    () => {
      if (!previousVersion) {
        return
      }

      navigate(
        `${syllabusBasePath}/${syllabus.id}/diff`
        + `?compareWith=${previousVersion.id}`,
      )
    }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                {pageEyebrow}
              </p>

              <Badge
                variant="outline"
                className={
                  getStatusClass(
                    syllabus.status,
                    role,
                  )
                }
              >
                {formatStatusLabelForRole(
                  syllabus.status,
                  role,
                )}
              </Badge>

              {syllabus.isCurrent && (
                <Badge
                  variant="outline"
                  className="border-blue-200 bg-blue-50 text-blue-700"
                >
                  Current Version
                </Badge>
              )}
            </div>

            <h1 className="mt-2 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              {syllabus.courseCode}
              {" — "}
              {syllabus.courseName}
            </h1>

            <p className="mt-1 text-sm text-[#687f89]">
              {versionLabel(
                syllabus,
              )}
              {" · "}
              {formatSemester(
                syllabus.semester,
              )}
              {" · "}
              {syllabus.major
                || "Major not specified"}
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                navigate(
                  syllabusBasePath,
                )
              }
            >
              <ArrowLeft className="size-4" />
              Back to Catalog
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() =>
                setPreviewOpen(true)
              }
            >
              <FileSearch2 className="size-4" />
              Preview PDF
            </Button>

            {isApproved && (
              <Button
                type="button"
                variant="outline"
                disabled={downloading}
                className="border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
                onClick={() =>
                  void handleDownload()
                }
              >
                {downloading ? (
                  <LoaderCircle className="size-4 animate-spin" />
                ) : (
                  <Download className="size-4" />
                )}
                Official PDF
              </Button>
            )}

            {previousVersion && (
              <Button
                type="button"
                variant="outline"
                onClick={
                  goToPreviousComparison
                }
              >
                <GitCompareArrows className="size-4" />
                Compare Previous
              </Button>
            )}

            {requiresCurrentReviewerAction && (
              <Button
                type="button"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                onClick={() =>
                  navigate(
                    isDean
                      ? "/dean/approvals"
                      : "/dept-head/approvals",
                  )
                }
              >
                <ShieldCheck className="size-4" />
                {isDean
                  ? "Open Final Review Queue"
                  : "Open Department Review Queue"}
              </Button>
            )}

            {canEditDraft && (
              <Button
                type="button"
                className="bg-[#007d84] text-white hover:bg-[#006d73]"
                onClick={() =>
                  navigate(
                    `${syllabusBasePath}/${syllabus.id}/editor`,
                  )
                }
              >
                <Edit3 className="size-4" />
                Edit Draft
              </Button>
            )}
          </div>
        </div>

        <div className="grid border-t border-slate-200 sm:grid-cols-2 xl:grid-cols-4">
          <SummaryItem
            icon={GraduationCap}
            label="Major"
            value={
              syllabus.major
              || "—"
            }
          />

          <SummaryItem
            icon={CalendarDays}
            label="Semester"
            value={
              formatSemester(
                syllabus.semester,
              )
            }
          />

          <SummaryItem
            icon={UserRound}
            label="Created / Imported By"
            value={
              syllabus
                .createdByUsername
              || "—"
            }
          />

          <SummaryItem
            icon={ShieldCheck}
            label="Approved By"
            value={
              approvedByDisplay
            }
          />
        </div>
      </section>

      {isOversightRole && (
        <section className="rounded-xl border border-[#cfe1e4] bg-[#f7fbfb] px-5 py-4 shadow-sm">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-start gap-3">
              <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#007d84]">
                  Workflow Position
                </p>

                <p className="mt-1 font-semibold text-[#17343d]">
                  {workflowPosition}
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  {isDeptHead
                    ? "This view is read-only. Department review decisions are performed from the Department Review Queue; all decisions and comments remain in the approval history."
                    : isDean
                      ? "This view is read-only. Final decisions are performed from the Dean Approval Queue; all decisions and comments remain in the approval history."
                      : "Administrative oversight view. Academic reviewer decisions remain with the Department Head and Dean."}
                </p>
              </div>
            </div>

            {requiresCurrentReviewerAction && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="shrink-0 bg-white"
                onClick={() =>
                  navigate(
                    isDean
                      ? "/dean/approvals"
                      : "/dept-head/approvals",
                  )
                }
              >
                <ShieldCheck className="size-3.5" />
                Open Pending Review
              </Button>
            )}
          </div>
        </section>
      )}

      {isRevisionState && (
        <section className="rounded-2xl border border-rose-200 bg-rose-50 p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex items-start gap-3">
              <AlertTriangle className="mt-0.5 size-5 shrink-0 text-rose-700" />

              <div>
                <h2 className="font-semibold text-rose-900">
                  Reviewer feedback requires revision
                </h2>

                <p className="mt-1 text-sm leading-6 text-rose-700">
                  This syllabus was returned by a reviewer. Open Review & Comment History below to read the feedback and required changes.
                </p>
              </div>
            </div>

            {canViewApprovalHistory && (
              <Button
                type="button"
                variant="outline"
                className="shrink-0 border-rose-300 bg-white text-rose-700 hover:bg-rose-100"
                onClick={
                  scrollToApprovalHistory
                }
              >
                <MessageSquareText className="size-4" />
                View Reviewer Comment
              </Button>
            )}
          </div>
        </section>
      )}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={History}
          label="Saved Versions"
          value={
            versionsLoading
              ? "..."
              : String(
                  sortedVersions.length,
                )
          }
          description="Preserved version history"
        />

        <MetricCard
          icon={ListChecks}
          label="CLOs"
          value={String(
            syllabus.clos?.length
            ?? 0,
          )}
          description="Course learning outcomes"
        />

        <MetricCard
          icon={Layers3}
          label="Topics"
          value={String(
            syllabus.topics?.length
            ?? 0,
          )}
          description="Teaching content entries"
        />

        <MetricCard
          icon={CheckCircle2}
          label="Assessments"
          value={String(
            syllabus.assessments
              ?.length
            ?? 0,
          )}
          description="Assessment components"
        />
      </section>

      <div className="grid gap-5 xl:grid-cols-[1.15fr_0.85fr]">
        <Card className="border-slate-200 p-6 shadow-sm">
          <div className="mb-5 flex items-center gap-2">
            <BookOpen className="size-5 text-[#007d84]" />

            <div>
              <h2 className="text-lg font-bold text-slate-900">
                Course Information
              </h2>

              <p className="mt-0.5 text-xs text-slate-500">
                Academic information recorded for this syllabus version.
              </p>
            </div>
          </div>

          <dl className="grid gap-5 sm:grid-cols-2">
            <Info
              label="Course Designation"
              value={
                syllabus
                  .courseDesignation
              }
            />

            <Info
              label="Course Type"
              value={
                syllabus.courseTypes
              }
            />

            <Info
              label="Language"
              value={
                syllabus.language
              }
            />

            <Info
              label="Curriculum Relationship"
              value={
                syllabus.relation
              }
            />

            <Info
              label="Teaching Methods"
              value={
                syllabus
                  .teachingMethods
              }
            />

            <Info
              label="Prerequisites"
              value={
                syllabus
                  .prerequisites
              }
            />

            <Info
              label="Workload Total"
              value={
                syllabus
                  .workloadTotal
              }
            />

            <Info
              label="Contact Workload"
              value={
                syllabus
                  .workloadContact
              }
            />

            <Info
              label="Private Study Workload"
              value={
                syllabus
                  .workloadPrivate
              }
            />

            <Info
              label="Exam Forms"
              value={
                syllabus.examForms
              }
            />
          </dl>

          <div className="mt-6 space-y-5 border-t border-slate-100 pt-5">
            <Info
              label="Objectives"
              value={
                normalizeDisplayText(
                  syllabus.objectives,
                )
              }
            />

            <Info
              label="Exam Requirements"
              value={
                normalizeDisplayText(
                  syllabus.examRequirements,
                )
              }
            />

            <Info
              label="Rubrics"
              value={
                normalizeDisplayText(
                  syllabus.rubrics,
                )
              }
            />
          </div>
        </Card>

        <Card className="border-slate-200 p-6 shadow-sm">
          <div className="mb-5">
            <h2 className="text-lg font-bold text-slate-900">
              Version & Workflow
            </h2>

            <p className="mt-0.5 text-xs text-slate-500">
              Status and approval lifecycle information for this syllabus version.
            </p>
          </div>

          <dl className="space-y-4">
            <Info
              label="Version"
              value={
                versionLabel(
                  syllabus,
                )
              }
            />

            <Info
              label="Current Version"
              value={
                syllabus.isCurrent
                  ? "Yes"
                  : "No"
              }
            />

            <Info
              label="Status"
              value={
                formatStatusLabelForRole(
                  syllabus.status,
                  role,
                )
              }
            />

            <Info
              label="Submitted At"
              value={
                formatDate(
                  syllabus
                    .submittedAt,
                )
              }
            />

            <Info
              label="Final Approval Date"
              value={
                approvedAtDisplay
              }
            />

            <Info
              label="Updated At"
              value={
                formatDate(
                  syllabus
                    .updatedAt,
                )
              }
            />

            <Info
              label="Change Summary"
              value={
                syllabus
                  .changeSummary
              }
            />

            <Info
              label="Notes"
              value={
                formatNotesForDisplay(
                  syllabus.notes,
                )
              }
            />
          </dl>
        </Card>
      </div>

      <section
        id="version-history"
        className="scroll-mt-24 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
      >
        <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <History className="size-5 text-[#007d84]" />

              <h2 className="font-semibold text-slate-900">
                Version History
              </h2>

            </div>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              Every preserved version for {syllabus.courseCode}. Older versions remain read-only and can be reopened or compared.
            </p>
          </div>

          {sortedVersions.length > 1 && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={
                scrollToVersionHistory
              }
              className="hidden"
            >
              History
            </Button>
          )}
        </div>

        {versionsLoading ? (
          <div className="flex items-center justify-center gap-2 px-5 py-10 text-sm text-slate-500">
            <LoaderCircle className="size-4 animate-spin" />
            Loading version history...
          </div>
        ) : versionsNewestFirst.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">
            No version history is available.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-sm">
              <thead className="bg-slate-50 text-[10px] font-semibold uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 text-left">
                    Version
                  </th>

                  <th className="px-5 py-3 text-left">
                    Status
                  </th>

                  <th className="px-5 py-3 text-left">
                    Semester
                  </th>

                  <th className="px-5 py-3 text-left">
                    Created / Imported By
                  </th>

                  <th className="px-5 py-3 text-left">
                    Submitted
                  </th>

                  <th className="px-5 py-3 text-right">
                    Actions
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-slate-100">
                {versionsNewestFirst.map(
                  (version) => {
                    const isThisVersion =
                      version.id
                      === syllabus.id

                    return (
                      <tr
                        key={
                          version.id
                        }
                        className={
                          isThisVersion
                            ? "bg-[#f5fbfb]"
                            : "hover:bg-slate-50/70"
                        }
                      >
                        <td className="px-5 py-4">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-semibold text-slate-800">
                              {versionLabel(
                                version,
                              )}
                            </span>

                            {version.isCurrent && (
                              <Badge
                                variant="outline"
                                className="border-blue-200 bg-blue-50 text-blue-700"
                              >
                                Current
                              </Badge>
                            )}

                            {isThisVersion && (
                              <Badge
                                variant="outline"
                                className="border-[#b9dfe1] bg-[#f2fafa] text-[#007d84]"
                              >
                                Viewing
                              </Badge>
                            )}
                          </div>
                        </td>

                        <td className="px-5 py-4">
                          <Badge
                            variant="outline"
                            className={
                              getStatusClass(
                                version.status,
                                role,
                              )
                            }
                          >
                            {formatStatusLabelForRole(
                              version.status,
                              role,
                            )}
                          </Badge>
                        </td>

                        <td className="px-5 py-4 text-slate-600">
                          {formatSemester(
                            version.semester,
                          )}
                        </td>

                        <td className="px-5 py-4 text-slate-600">
                          {version
                            .createdByUsername
                          || "—"}
                        </td>

                        <td className="px-5 py-4 text-xs text-slate-500">
                          {formatDate(
                            version
                              .submittedAt,
                          )}
                        </td>

                        <td className="px-5 py-4">
                          <div className="flex justify-end gap-2">
                            {!isThisVersion && (
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() =>
                                  navigate(
                                    `${syllabusBasePath}/${version.id}`,
                                  )
                                }
                              >
                                View
                              </Button>
                            )}

                            {!isThisVersion && (
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() =>
                                  navigate(
                                    `${syllabusBasePath}/${syllabus.id}/diff`
                                    + `?compareWith=${version.id}`,
                                  )
                                }
                              >
                                <GitCompareArrows className="size-3.5" />
                                Compare
                              </Button>
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

      {canViewApprovalHistory && (
        <section
          id="approval-history"
          className="scroll-mt-24"
        >
          <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 className="mt-1 text-xl font-bold text-[#17343d]">
                Review & Comment History
              </h2>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Every Department Head and Dean review decision is preserved with reviewer comments, timestamps, and workflow history so the complete approval trail can be verified.
              </p>
            </div>
          </div>

          <ApprovalHistoryTable
            syllabusId={
              syllabus.id
            }
          />
        </section>
      )}

      <SyllabusPdfPreviewDialog
        open={previewOpen}
        onOpenChange={
          setPreviewOpen
        }
        syllabusId={
          syllabus.id
        }
        courseCode={
          syllabus.courseCode
        }
        courseName={
          syllabus.courseName
        }
        status={
          formatStatusLabelForRole(
            syllabus.status,
            role,
          )
        }
      />
    </div>
  )
}

function SummaryItem({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ElementType
  label: string
  value: string
}) {
  return (
    <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4 last:border-b-0 sm:[&:nth-child(odd)]:border-r xl:border-b-0 xl:border-r xl:last:border-r-0">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
        <Icon className="size-5" />
      </span>

      <div className="min-w-0">
        <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
          {label}
        </p>

        <p className="mt-1 truncate text-sm font-semibold text-slate-800">
          {value}
        </p>
      </div>
    </div>
  )
}

function MetricCard({
  icon: Icon,
  label,
  value,
  description,
}: {
  icon: React.ElementType
  label: string
  value: string
  description: string
}) {
  return (
    <Card className="border-slate-200 p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            {label}
          </p>

          <p className="mt-2 text-2xl font-bold text-slate-900">
            {value}
          </p>

          <p className="mt-1 text-xs text-slate-500">
            {description}
          </p>
        </div>

        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
          <Icon className="size-5" />
        </span>
      </div>
    </Card>
  )
}

function Info({
  label,
  value,
}: {
  label: string
  value?: string | null
}) {
  return (
    <div>
      <dt className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
        {label}
      </dt>

      <dd className="mt-1 whitespace-pre-line text-sm leading-6 text-slate-700">
        {value || "Not provided"}
      </dd>
    </div>
  )
}