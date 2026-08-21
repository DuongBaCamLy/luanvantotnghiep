import {
  lazy,
  Suspense,
  useEffect,
  useMemo,
  useState,
} from "react"

import axios from "axios"
import { useNavigate, useSearchParams } from "react-router-dom"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"

import {
  ArrowLeft,
  CheckCircle2,
  ClipboardCheck,
  ClipboardList,
  Eye,
  FileSearch2,
  FileText,
  Layers,
  LibraryBig,
  ListChecks,
  Send,
  ShieldCheck,
  Upload,
} from "lucide-react"

import type {
  Syllabus,
  SubmissionValidationResponse,
} from "@/types/syllabus"

import { useSubmitSyllabus } from "@/hooks/useSubmitSyllabus"
import { useAuthStore } from "@/store/authStore"
import { useValidateSyllabusSubmission } from "@/hooks/useValidateSyllabusSubmission"

import SubmissionValidationDialog from "@/components/syllabus/SubmissionValidationDialog"
import SyllabusPdfPreviewDialog from "@/components/syllabus/SyllabusPdfPreviewDialog"
import ImportSyllabusDialog from "@/components/syllabus/ImportSyllabusDialog"

const Section1GeneralInfo = lazy(
  () => import("./tabs/Section1GeneralInfo"),
)

const Section3CLOs = lazy(
  () => import("./tabs/Section3CLOs"),
)

const Section4PLOMapping = lazy(
  () => import("./tabs/Section4PLOMapping"),
)

const Section5Topics = lazy(
  () => import("./tabs/Section5Topics"),
)

const Section6Assessment = lazy(
  () => import("./tabs/Section6Assessment"),
)

const Section7ReadingList = lazy(
  () => import("./tabs/Section7ReadingList"),
)

interface TabDefinition {
  id: number
  label: string
  shortLabel: string
  description: string
  icon: React.ElementType
}

const TABS: TabDefinition[] = [
  {
    id: 1,
    label: "General Information",
    shortLabel: "General",
    description:
      "Review course context and complete the syllabus-level academic information.",
    icon: FileText,
  },
  {
    id: 3,
    label: "Course Learning Outcomes",
    shortLabel: "CLO",
    description:
      "Define measurable course learning outcomes using appropriate Bloom levels.",
    icon: ListChecks,
  },
  {
    id: 4,
    label: "CLO–PLO Mapping",
    shortLabel: "CLO–PLO",
    description:
      "Map each CLO to program learning outcomes using Introduce, Develop, or Achieve.",
    icon: Layers,
  },
  {
    id: 5,
    label: "Teaching Content",
    shortLabel: "Topics",
    description:
      "Plan the teaching content, learning activities, and study workload by topic.",
    icon: ClipboardList,
  },
  {
    id: 6,
    label: "Assessment Plan",
    shortLabel: "Assessment",
    description:
      "Define assessment components, weights, and their relationship to course outcomes.",
    icon: CheckCircle2,
  },
  {
    id: 7,
    label: "Reading List",
    shortLabel: "Reading",
    description:
      "Maintain textbooks and supplementary learning resources for the course.",
    icon: LibraryBig,
  },
  {
    id: 8,
    label: "Review & Submit",
    shortLabel: "Review",
    description:
      "Run the mandatory submission check, preview the syllabus, and send the Draft for review.",
    icon: ClipboardCheck,
  },
]

interface StatusDisplay {
  label: string
  className: string
  helpText: string
}

const STATUS_CONFIG: Record<string, StatusDisplay> = {
  DRAFT: {
    label: "Draft",
    className:
      "border-slate-200 bg-slate-100 text-slate-700",
    helpText:
      "Editable. Complete the required syllabus sections before submission.",
  },
  SUBMITTED: {
    label: "Pending Department Review",
    className:
      "border-blue-200 bg-blue-50 text-blue-700",
    helpText:
      "Submitted to the Head of Department. This version is read-only.",
  },
  UNDER_REVIEW: {
    label: "Pending Dean Review",
    className:
      "border-violet-200 bg-violet-50 text-violet-700",
    helpText:
      "Department review is complete and the syllabus is awaiting the Dean's decision.",
  },
  APPROVED: {
    label: "Approved",
    className:
      "border-emerald-200 bg-emerald-50 text-emerald-700",
    helpText:
      "This syllabus has completed the academic approval workflow.",
  },
  REJECTED: {
    label: "Returned for Revision",
    className:
      "border-rose-200 bg-rose-50 text-rose-700",
    helpText:
      "A reviewer returned this version. Use the revision Draft created by the workflow.",
  },
  REVISION_REQUESTED: {
    label: "Revision Required",
    className:
      "border-amber-200 bg-amber-50 text-amber-700",
    helpText:
      "Reviewer feedback requires changes before the syllabus can be submitted again.",
  },
  ARCHIVED: {
    label: "Archived",
    className:
      "border-slate-200 bg-slate-50 text-slate-500",
    helpText:
      "This preserved syllabus version is read-only.",
  },
}

const normalizeStatus = (
  value: unknown,
): string =>
  String(value ?? "")
    .trim()
    .toUpperCase()

const formatSemester = (
  value: unknown,
): string => {
  const text = String(value ?? "").trim()

  if (!text) return "Semester not set"

  const matched = text.match(
    /(?:semester|hk)\s*(\d+)/i,
  )

  const normalized =
    matched?.[1] ?? text

  return /^\d+$/.test(normalized)
    ? `Semester ${normalized}`
    : normalized
}

const apiMessage = (
  error: unknown,
  fallback: string,
): string => {
  if (axios.isAxiosError(error)) {
    const data =
      error.response?.data as
        | { message?: string }
        | undefined

    return data?.message ?? fallback
  }

  return error instanceof Error
    ? error.message
    : fallback
}

interface Props {
  syllabus: Syllabus
  readOnly?: boolean
}

export default function SyllabusEditorShell({
  syllabus,
  readOnly = false,
}: Props) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const currentUser = useAuthStore((state) => state.user)
  const isAdmin = String(currentUser?.role ?? "").replace(/^ROLE_/i, "").toUpperCase() === "ADMIN"

  const [activeTab, setActiveTab] =
    useState<number>(1)

  const [
    validation,
    setValidation,
  ] =
    useState<SubmissionValidationResponse | null>(
      null,
    )

  const [
    validationOpen,
    setValidationOpen,
  ] = useState(false)

  const [
    pdfPreviewOpen,
    setPdfPreviewOpen,
  ] = useState(false)

  const [
    importOpen,
    setImportOpen,
  ] = useState(false)

  const submitMutation =
    useSubmitSyllabus()

  const validationMutation =
    useValidateSyllabusSubmission()

  const normalizedStatus =
    normalizeStatus(syllabus.status)

  const statusCfg =
    STATUS_CONFIG[normalizedStatus]
    ?? STATUS_CONFIG.DRAFT

  const effectiveReadOnly =
    readOnly
    || (!isAdmin && normalizedStatus !== "DRAFT")

  const canEdit =
    !effectiveReadOnly

  useEffect(() => {
    if (searchParams.get("import") === "1" && canEdit) {
      setImportOpen(true)
    }
  }, [searchParams, canEdit])

  const canSubmit =
    normalizedStatus === "DRAFT"
    && !readOnly

  const activeTabDefinition =
    useMemo(
      () =>
        TABS.find(
          (tab) =>
            tab.id === activeTab,
        )
        ?? TABS[0],
      [activeTab],
    )

  const openReviewTab = () => {
    setActiveTab(8)

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    })
  }

  const runSubmissionCheck = () => {
    if (!canSubmit) {
      return
    }

    validationMutation.mutate(
      syllabus.id,
      {
        onSuccess: (result) => {
          setValidation(result)
          setValidationOpen(true)
        },

        onError: (error: unknown) => {
          alert(
            apiMessage(
              error,
              "Unable to validate the syllabus submission requirements.",
            ),
          )
        },
      },
    )
  }

  const handleConfirmSubmit = () => {
    if (!canSubmit) {
      return
    }

    submitMutation.mutate(
      syllabus.id,
      {
        onSuccess: () => {
          setValidationOpen(false)

          alert(
            "Syllabus submitted successfully. It is now pending Department review.",
          )

          navigate(
            `/instructor/syllabus/${syllabus.id}`,
          )
        },

        onError: (error: unknown) => {
          const response =
            axios.isAxiosError(error)
              ? (
                  error.response
                    ?.data as
                    | SubmissionValidationResponse
                    | undefined
                )
              : undefined

          if (response?.issues) {
            setValidation(response)
            setValidationOpen(true)
            return
          }

          alert(
            apiMessage(
              error,
              "Unable to submit the syllabus.",
            ),
          )
        },
      },
    )
  }

  const handleGoToSection = (
    tabId: number,
  ) => {
    setActiveTab(tabId)
    setValidationOpen(false)

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    })
  }

  const renderSection = () => {
    switch (activeTab) {
      case 1:
        return (
          <Section1GeneralInfo
            syllabus={syllabus}
            readOnly={effectiveReadOnly}
          />
        )

      case 3:
        return (
          <Section3CLOs
            syllabusId={syllabus.id}
            readOnly={effectiveReadOnly}
          />
        )

      case 4:
        return (
          <Section4PLOMapping
            syllabusId={syllabus.id}
            readOnly={effectiveReadOnly}
          />
        )

      case 5:
        return (
          <Section5Topics
            syllabusId={syllabus.id}
            readOnly={effectiveReadOnly}
          />
        )

      case 6:
        return (
          <Section6Assessment
            syllabusId={syllabus.id}
            readOnly={effectiveReadOnly}
          />
        )

      case 7:
        return (
          <Section7ReadingList
            syllabusId={syllabus.id}
            readOnly={effectiveReadOnly}
          />
        )

      case 8:
        return (
          <ReviewAndSubmitSection
            syllabus={syllabus}
            statusCfg={statusCfg}
            canSubmit={canSubmit}
            validating={
              validationMutation.isPending
            }
            submitting={
              submitMutation.isPending
            }
            onPreview={() =>
              setPdfPreviewOpen(true)
            }
            onRunCheck={
              runSubmissionCheck
            }
            onGoToSection={
              setActiveTab
            }
          />
        )

      default:
        return null
    }
  }

  return (
    <div className="min-h-screen bg-[#f7faf9]">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 shadow-sm backdrop-blur">
        <div className="mx-auto max-w-[1500px] px-4 py-3 sm:px-6">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <div className="flex min-w-0 items-start gap-3">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                aria-label="Back"
                onClick={() =>
                  navigate(-1)
                }
                className="mt-0.5 size-9 shrink-0 p-0 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
              >
                <ArrowLeft className="size-5" />
              </Button>

              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#007d84]">
                    Instructor · Syllabus Editor
                  </span>

                  {syllabus.isCurrent && (
                    <Badge
                      variant="outline"
                      className="border-[#b9dfe1] bg-[#f1fafa] text-[#007d84]"
                    >
                      Current Version
                    </Badge>
                  )}
                </div>

                <div className="mt-1 flex flex-wrap items-center gap-2">
                  <h1 className="min-w-0 text-lg font-bold text-[#17343d] sm:text-xl">
                    {syllabus.courseCode}
                    <span className="mx-2 text-slate-300">
                      —
                    </span>
                    {syllabus.courseName}
                  </h1>

                  <Badge
                    variant="outline"
                    className={
                      statusCfg.className
                    }
                  >
                    {statusCfg.label}
                  </Badge>
                </div>

                <p className="mt-1 text-xs text-slate-500">
                  {syllabus.academicYear
                    || "Academic year not set"}
                  {" · "}
                  {formatSemester(
                    syllabus.semester,
                  )}
                  {" · "}
                  {syllabus.versionLabel
                    || `v${syllabus.versionNumber ?? 1}`}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2 xl:justify-end">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() =>
                  setPdfPreviewOpen(true)
                }
                className="gap-1.5 border-slate-300 bg-white"
              >
                <FileSearch2 className="size-4" />
                Preview PDF
              </Button>

              {canEdit && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setImportOpen(true)
                  }
                  className="gap-1.5 border-slate-300 bg-white"
                >
                  <Upload className="size-4" />
                  Import Word/Excel
                </Button>
              )}

              {canSubmit && (
                <Button
                  type="button"
                  size="sm"
                  onClick={openReviewTab}
                  className="gap-1.5 bg-[#007d84] text-white hover:bg-[#006c72]"
                >
                  <ClipboardCheck className="size-4" />
                  Review & Submit
                </Button>
              )}
            </div>
          </div>
        </div>

        <div className="border-t border-slate-100">
          <div className="mx-auto max-w-[1500px] overflow-x-auto px-4 sm:px-6">
            <nav
              className="flex min-w-max items-center"
              aria-label="Syllabus sections"
            >
              {TABS.map(
                (tab, index) => {
                  const Icon =
                    tab.icon

                  const isActive =
                    activeTab === tab.id

                  return (
                    <button
                      type="button"
                      key={tab.id}
                      aria-current={
                        isActive
                          ? "page"
                          : undefined
                      }
                      onClick={() =>
                        setActiveTab(
                          tab.id,
                        )
                      }
                      className={`group flex items-center gap-2 border-b-2 px-3 py-3 text-sm font-medium transition-colors ${
                        isActive
                          ? "border-[#007d84] bg-[#f1fafa] text-[#006c72]"
                          : "border-transparent text-slate-500 hover:border-slate-300 hover:bg-slate-50 hover:text-slate-800"
                      }`}
                    >
                      <span
                        className={`flex size-5 items-center justify-center rounded-full text-[10px] font-bold ${
                          isActive
                            ? "bg-[#007d84] text-white"
                            : "bg-slate-100 text-slate-500 group-hover:bg-slate-200"
                        }`}
                      >
                        {index + 1}
                      </span>

                      <Icon className="size-4" />

                      <span>
                        {tab.shortLabel}
                      </span>
                    </button>
                  )
                },
              )}
            </nav>
          </div>
        </div>
      </header>

      <div className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-[1500px] flex-col gap-2 px-4 py-3 sm:px-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-sm font-semibold text-[#17343d]">
              {
                activeTabDefinition.label
              }
            </p>

            <p className="mt-0.5 text-xs leading-5 text-slate-500">
              {
                activeTabDefinition.description
              }
            </p>
          </div>

          <div className="flex items-center gap-2 text-xs">
            {effectiveReadOnly ? (
              <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1 font-medium text-slate-600">
                <Eye className="size-3.5" />
                Read-only version
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 font-medium text-emerald-700">
                <ShieldCheck className="size-3.5" />
                Draft is editable
              </span>
            )}
          </div>
        </div>
      </div>

      <main className="mx-auto w-full max-w-[1500px] px-4 py-6 sm:px-6">
        <Suspense
          fallback={
            <div className="flex min-h-72 items-center justify-center rounded-2xl border border-slate-200 bg-white text-sm text-slate-500 shadow-sm">
              Loading syllabus section...
            </div>
          }
        >
          {renderSection()}
        </Suspense>
      </main>

      <SyllabusPdfPreviewDialog
        open={pdfPreviewOpen}
        onOpenChange={
          setPdfPreviewOpen
        }
        syllabusId={syllabus.id}
        courseCode={
          syllabus.courseCode
        }
        courseName={
          syllabus.courseName
        }
        status={statusCfg.label}
      />

      <SubmissionValidationDialog
        open={validationOpen}
        validation={validation}
        submitting={
          submitMutation.isPending
        }
        onOpenChange={
          setValidationOpen
        }
        onSubmit={
          handleConfirmSubmit
        }
        onGoToSection={
          handleGoToSection
        }
      />

      <ImportSyllabusDialog
        syllabusId={syllabus.id}
        open={importOpen}
        onClose={() =>
          setImportOpen(false)
        }
        onImported={() =>
          window.location.reload()
        }
      />
    </div>
  )
}

interface ReviewAndSubmitSectionProps {
  syllabus: Syllabus
  statusCfg: StatusDisplay
  canSubmit: boolean
  validating: boolean
  submitting: boolean
  onPreview: () => void
  onRunCheck: () => void
  onGoToSection: (tabId: number) => void
}

function ReviewAndSubmitSection({
  syllabus,
  statusCfg,
  canSubmit,
  validating,
  submitting,
  onPreview,
  onRunCheck,
  onGoToSection,
}: ReviewAndSubmitSectionProps) {
  const reviewSections = [
    {
      tabId: 1,
      number: "01",
      title:
        "General Information",
      description:
        "Course context, syllabus metadata, academic information, workload, objectives and requirements.",
    },
    {
      tabId: 3,
      number: "02",
      title:
        "Course Learning Outcomes",
      description:
        "Measurable CLO statements and Bloom taxonomy levels.",
    },
    {
      tabId: 4,
      number: "03",
      title:
        "CLO–PLO Mapping",
      description:
        "Program-outcome alignment with Introduce, Develop and Achieve levels.",
    },
    {
      tabId: 5,
      number: "04",
      title:
        "Teaching Content",
      description:
        "Topics, learning activities and workload allocation.",
    },
    {
      tabId: 6,
      number: "05",
      title:
        "Assessment Plan",
      description:
        "Assessment components, weights and outcome mappings.",
    },
    {
      tabId: 7,
      number: "06",
      title:
        "Reading List",
      description:
        "Textbooks and supplementary academic resources.",
    },
  ]

  return (
    <div className="space-y-5">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 bg-gradient-to-r from-[#f1fafa] via-white to-white px-5 py-5 sm:px-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <div className="flex items-center gap-2">
                <div className="flex size-10 items-center justify-center rounded-xl bg-[#007d84] text-white">
                  <ClipboardCheck className="size-5" />
                </div>

                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-[#007d84]">
                    Submission Readiness
                  </p>

                  <h2 className="text-xl font-bold text-[#17343d]">
                    Review the Draft before submission
                  </h2>
                </div>
              </div>

              <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
                The system validates the syllabus before it enters the academic review workflow. A successful check opens the final confirmation dialog; unresolved mandatory items block submission.
              </p>
            </div>

            <Badge
              variant="outline"
              className={`w-fit ${statusCfg.className}`}
            >
              {statusCfg.label}
            </Badge>
          </div>
        </div>

        <div className="grid gap-4 p-5 sm:grid-cols-3 sm:p-6">
          <ReviewMetric
            label="Course"
            value={
              syllabus.courseCode
              || "—"
            }
          />

          <ReviewMetric
            label="Academic Term"
            value={`${syllabus.academicYear || "—"} · ${formatSemester(syllabus.semester)}`}
          />

          <ReviewMetric
            label="Version"
            value={
              syllabus.versionLabel
              || `v${syllabus.versionNumber ?? 1}`
            }
          />
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-[1fr_360px]">
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 px-5 py-4">
            <h3 className="font-semibold text-[#17343d]">
              Syllabus Sections
            </h3>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              Open any section to review or correct its content before running the submission check.
            </p>
          </div>

          <div className="divide-y divide-slate-100">
            {reviewSections.map(
              (section) => (
                <button
                  type="button"
                  key={section.tabId}
                  onClick={() =>
                    onGoToSection(
                      section.tabId,
                    )
                  }
                  className="flex w-full items-start gap-4 px-5 py-4 text-left transition-colors hover:bg-slate-50"
                >
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-[#f1fafa] text-xs font-bold text-[#007d84]">
                    {
                      section.number
                    }
                  </span>

                  <span className="min-w-0">
                    <span className="block text-sm font-semibold text-slate-800">
                      {
                        section.title
                      }
                    </span>

                    <span className="mt-1 block text-xs leading-5 text-slate-500">
                      {
                        section.description
                      }
                    </span>
                  </span>
                </button>
              ),
            )}
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-2xl border border-[#b9dfe1] bg-[#f4fbfb] p-5 shadow-sm">
            <div className="flex items-start gap-3">
              <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

              <div>
                <h3 className="font-semibold text-[#17343d]">
                  Mandatory submission check
                </h3>

                <p className="mt-2 text-xs leading-5 text-slate-600">
                  The check verifies required syllabus information and the submission rules enforced by the backend. Submission is only available after those rules pass.
                </p>
              </div>
            </div>

            <Button
              type="button"
              onClick={onRunCheck}
              disabled={
                !canSubmit
                || validating
                || submitting
              }
              className="mt-5 w-full gap-2 bg-[#007d84] text-white hover:bg-[#006c72]"
            >
              <Send className="size-4" />

              {validating
                ? "Running Check..."
                : submitting
                  ? "Submitting..."
                  : "Run Submission Check"}
            </Button>

            {!canSubmit && (
              <p className="mt-3 text-xs leading-5 text-slate-500">
                This preserved version is not an editable Draft, so it cannot be submitted from the editor.
              </p>
            )}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="font-semibold text-[#17343d]">
              Preview first
            </h3>

            <p className="mt-2 text-xs leading-5 text-slate-500">
              Review the generated PDF before submission to confirm the final academic presentation.
            </p>

            <Button
              type="button"
              variant="outline"
              onClick={onPreview}
              className="mt-4 w-full gap-2"
            >
              <FileSearch2 className="size-4" />
              Preview PDF
            </Button>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 text-xs leading-5 text-slate-500 shadow-sm">
            <p className="font-semibold text-slate-700">
              What happens after submission?
            </p>

            <p className="mt-2">
              The submitted version is preserved and becomes read-only while the Head of Department reviews it. If approved at department level, it continues to the Dean for final academic approval.
            </p>
          </div>
        </aside>
      </section>
    </div>
  )
}

function ReviewMetric({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/70 px-4 py-3">
      <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-sm font-semibold text-slate-800">
        {value}
      </p>
    </div>
  )
}