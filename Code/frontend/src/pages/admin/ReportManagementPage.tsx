import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useLocation, useNavigate } from "react-router-dom"
import axios from "axios"
import {
  ArrowRightLeft,
  Download,
  Eye,
  FileCheck2,
  FileText,
  Grid3X3,
  LoaderCircle,
} from "lucide-react"

import {
  curriculumChangeReportApi,
  reportApi,
} from "@/api/reportApi"
import { cohortApi } from "@/api/cohortApi"
import { programApi } from "@/api/programApi"
import { syllabusPdfApi } from "@/api/syllabusPdfApi"
import SyllabusPdfPreviewDialog from "@/components/syllabus/SyllabusPdfPreviewDialog"
import PloCoverageReportCard from "@/components/report/PloCoverageReportCard"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useSyllabuses } from "@/hooks/useSyllabuses"
import type { Cohort, Program } from "@/types/admin"

const ALL = "__all__"

const currentAcademicYear = () => {
  const now = new Date()
  const start =
    now.getMonth() >= 7
      ? now.getFullYear()
      : now.getFullYear() - 1

  return `${start}-${start + 1}`
}

const buildAcademicYearOptions = (
  syllabusYears: Array<string | null | undefined>,
) => {
  const years = new Set<string>()
  const current = currentAcademicYear()
  const start = Number(current.slice(0, 4))

  for (let offset = -3; offset <= 1; offset++) {
    const year = start + offset
    years.add(`${year}-${year + 1}`)
  }

  syllabusYears.forEach((value) => {
    const normalized = String(value ?? "").trim()

    if (/^\d{4}-\d{4}$/.test(normalized)) {
      years.add(normalized)
    }
  })

  return Array.from(years).sort((left, right) =>
    right.localeCompare(left, "en", {
      numeric: true,
    }),
  )
}

const downloadBlobResponse = (
  data: BlobPart,
  filename: string,
) => {
  const url =
    window.URL.createObjectURL(
      new Blob([data]),
    )

  const link =
    document.createElement("a")

  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()

  window.setTimeout(
    () =>
      window.URL.revokeObjectURL(
        url,
      ),
    1_000,
  )
}

const apiMessage = (
  error: unknown,
  fallback: string,
) => {
  if (
    axios.isAxiosError(error)
  ) {
    const data =
      error.response?.data as
        | {
            message?: string
          }
        | undefined

    return (
      data?.message
      ?? fallback
    )
  }

  return error instanceof Error
    ? error.message
    : fallback
}

const baseProgramCode = (
  value: string,
) => {
  const normalized = String(value ?? "").trim()

  return (
    normalized
      .replace(/[-_\\s]?20\\d{2}$/i, "")
      .replace(/[-_\\s]+$/g, "")
      .trim()
    || normalized
  )
}

const programLabel = (
  program: Program,
) =>
  `${baseProgramCode(program.code)} — ${program.name || program.nameVn}`

const cohortLabel = (
  cohort: Cohort,
) =>
  cohort.name

export default function ReportManagementPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const roleBase =
    location.pathname.startsWith("/dean")
      ? "/dean"
      : location.pathname.startsWith("/dept-head")
        ? "/dept-head"
        : "/admin"

  const {
    data: syllabuses = [],
    isLoading:
      loadingSyllabuses,
  } = useSyllabuses()

  const {
    data: programs = [],
  } = useQuery({
    queryKey: [
      "programs",
      "reports",
    ],
    queryFn: programApi.getAll,
  })

  const activePrograms =
    useMemo(
      () =>
        programs
          .filter(
            (program) =>
              program.isActive,
          )
          .slice()
          .sort((left, right) =>
            left.code.localeCompare(
              right.code,
              "en",
              {
                numeric: true,
              },
            ),
          ),
      [programs],
    )

  const academicYearOptions =
    useMemo(
      () =>
        buildAcademicYearOptions(
          syllabuses.map(
            (item) =>
              item.academicYear,
          ),
        ),
      [syllabuses],
    )

  const [
    matrixProgramId,
    setMatrixProgramId,
  ] = useState("")

  const [
    matrixCohortId,
    setMatrixCohortId,
  ] = useState("")

  const [
    matrixAcademicYear,
    setMatrixAcademicYear,
  ] = useState(
    currentAcademicYear(),
  )

  const [
    matrixSemester,
    setMatrixSemester,
  ] = useState("1")

  const {
    data: matrixCohorts = [],
  } = useQuery({
    queryKey: [
      "cohorts",
      "reports",
      "matrix",
      matrixProgramId,
    ],
    queryFn: () =>
      cohortApi.getByProgram(
        Number(matrixProgramId),
      ),
    enabled:
      Boolean(matrixProgramId),
  })

  const sortedMatrixCohorts =
    useMemo(
      () =>
        [...matrixCohorts].sort(
          (left, right) =>
            Number(right.entryYear ?? 0)
            - Number(left.entryYear ?? 0),
        ),
      [matrixCohorts],
    )

  const effectiveMatrixCohortId =
    matrixCohortId
    || (
      sortedMatrixCohorts[0]
        ? String(sortedMatrixCohorts[0].id)
        : ""
    )

  const [
    listProgramId,
    setListProgramId,
  ] = useState(ALL)

  const [
    listCohortId,
    setListCohortId,
  ] = useState(ALL)

  const [
    academicYear,
    setAcademicYear,
  ] = useState(
    currentAcademicYear(),
  )

  const [
    semester,
    setSemester,
  ] = useState("1")

  const [
    syllabusStatus,
    setSyllabusStatus,
  ] = useState(ALL)

  const {
    data: listCohorts = [],
  } = useQuery({
    queryKey: [
      "cohorts",
      "reports",
      "syllabus-list",
      listProgramId,
    ],
    queryFn: () =>
      cohortApi.getByProgram(
        Number(listProgramId),
      ),
    enabled:
      listProgramId !== ALL,
  })

  const [
    selectedSyllabusId,
    setSelectedSyllabusId,
  ] = useState("")

  const [
    previewOpen,
    setPreviewOpen,
  ] = useState(false)

  const [
    syllabusSearch,
    setSyllabusSearch,
  ] = useState("")

  const [
    changeProgramId,
    setChangeProgramId,
  ] = useState("")

  const [
    oldCohortId,
    setOldCohortId,
  ] = useState("")

  const [
    newCohortId,
    setNewCohortId,
  ] = useState("")

  const {
    data: changeCohorts = [],
  } = useQuery({
    queryKey: [
      "cohorts",
      "reports",
      "curriculum-change",
      changeProgramId,
    ],
    queryFn: () =>
      cohortApi.getByProgram(
        Number(changeProgramId),
      ),
    enabled:
      Boolean(changeProgramId),
  })

  const sortedChangeCohorts =
    useMemo(
      () =>
        [...changeCohorts].sort(
          (left, right) =>
            Number(right.entryYear ?? 0)
            - Number(left.entryYear ?? 0),
        ),
      [changeCohorts],
    )

  const effectiveNewCohortId =
    newCohortId
    || (
      sortedChangeCohorts[0]
        ? String(sortedChangeCohorts[0].id)
        : ""
    )

  const effectiveOldCohortId =
    oldCohortId
    || (
      sortedChangeCohorts[1]
        ? String(sortedChangeCohorts[1].id)
        : ""
    )

  const [
    isExporting,
    setIsExporting,
  ] =
    useState<string | null>(
      null,
    )

  const selectedSyllabus =
    useMemo(
      () =>
        syllabuses.find(
          (item) =>
            item.id
            === Number(
              selectedSyllabusId,
            ),
        ),
      [
        selectedSyllabusId,
        syllabuses,
      ],
    )

  const sortedSyllabuses =
    useMemo(
      () =>
        [...syllabuses].sort(
          (left, right) =>
            `${left.courseCode}-${left.academicYear}-${left.versionNumber}`
              .localeCompare(
                `${right.courseCode}-${right.academicYear}-${right.versionNumber}`,
                "en",
                {
                  numeric: true,
                },
              ),
        ),
      [syllabuses],
    )

  const filteredSyllabuses =
    useMemo(
      () => {
        const query =
          syllabusSearch.trim().toLowerCase()

        if (!query) {
          return sortedSyllabuses
        }

        return sortedSyllabuses.filter(
          (item) =>
            [
              item.courseCode,
              item.courseName,
              item.academicYear,
              item.versionLabel,
              item.status,
            ]
              .filter(Boolean)
              .join(" ")
              .toLowerCase()
              .includes(query),
        )
      },
      [
        sortedSyllabuses,
        syllabusSearch,
      ],
    )

  const handleMatrixExport =
    async (
      format:
        | "EXCEL"
        | "PDF",
    ) => {
      if (
        !matrixProgramId
        || !effectiveMatrixCohortId
      ) {
        alert(
          "Select a Program and Cohort before exporting the CLO–PLO Matrix.",
        )
        return
      }

      const operation =
        `CLO_PLO_${format}`

      try {
        setIsExporting(
          operation,
        )

        const scope = {
          cohortId:
            Number(
              effectiveMatrixCohortId,
            ),
          academicYear:
            matrixAcademicYear,
          semester:
            matrixSemester,
        }

        const response =
          format === "EXCEL"
            ? await reportApi
                .exportCloPloMatrixExcel(
                  Number(
                    matrixProgramId,
                  ),
                  scope,
                )
            : await reportApi
                .exportCloPloMatrixPdf(
                  Number(
                    matrixProgramId,
                  ),
                  scope,
                )

        downloadBlobResponse(
          response.data,
          `CLO_PLO_Matrix_${matrixAcademicYear}_S${matrixSemester}.${format === "EXCEL" ? "xlsx" : "pdf"}`,
        )
      } catch (
        error: unknown
      ) {
        alert(
          apiMessage(
            error,
            "Unable to export the CLO–PLO Matrix.",
          ),
        )
      } finally {
        setIsExporting(
          null,
        )
      }
    }

  const handleSyllabusListExport =
    async (
      format:
        | "EXCEL"
        | "PDF",
    ) => {
      const operation =
        `SYLLABUS_LIST_${format}`

      try {
        setIsExporting(
          operation,
        )

        const scope = {
          academicYear,
          semester,
          programId:
            listProgramId
              === ALL
              ? undefined
              : Number(
                  listProgramId,
                ),
          cohortId:
            listCohortId
              === ALL
              ? undefined
              : Number(
                  listCohortId,
                ),
          status:
            syllabusStatus
              === ALL
              ? "ALL"
              : syllabusStatus,
        }

        const response =
          format === "EXCEL"
            ? await reportApi
                .exportSyllabusListExcel(
                  scope,
                )
            : await reportApi
                .exportSyllabusListPdf(
                  scope,
                )

        downloadBlobResponse(
          response.data,
          `Syllabus_List_${academicYear}_S${semester}.${format === "EXCEL" ? "xlsx" : "pdf"}`,
        )
      } catch (
        error: unknown
      ) {
        alert(
          apiMessage(
            error,
            "Unable to export the syllabus submission report.",
          ),
        )
      } finally {
        setIsExporting(
          null,
        )
      }
    }

  const handleSyllabusExport =
    async () => {
      if (
        !selectedSyllabus
      ) {
        return
      }

      if (
        selectedSyllabus.status
        !== "APPROVED"
      ) {
        alert(
          "Official PDF export is available only for an APPROVED syllabus. Use Preview for non-approved versions.",
        )
        return
      }

      try {
        setIsExporting(
          "SINGLE_SYLLABUS",
        )

        await syllabusPdfApi
          .downloadFromReports(
            selectedSyllabus.id,
            `Syllabus_${selectedSyllabus.courseCode}_${selectedSyllabus.versionLabel}.pdf`,
          )
      } catch (
        error: unknown
      ) {
        alert(
          apiMessage(
            error,
            "Unable to export the official syllabus PDF.",
          ),
        )
      } finally {
        setIsExporting(
          null,
        )
      }
    }

  const handleCurriculumChangeExport =
    async (
      format:
        | "EXCEL"
        | "PDF",
    ) => {
      if (
        !changeProgramId
        || !effectiveOldCohortId
        || !effectiveNewCohortId
      ) {
        alert(
          "Select Program, From Cohort, and To Cohort.",
        )
        return
      }

      if (
        effectiveOldCohortId
        === effectiveNewCohortId
      ) {
        alert(
          "From Cohort and To Cohort must be different.",
        )
        return
      }

      const operation =
        `CURRICULUM_CHANGE_${format}`

      try {
        setIsExporting(
          operation,
        )

        const scope = {
          programId:
            Number(
              changeProgramId,
            ),
          oldCohortId:
            Number(
              effectiveOldCohortId,
            ),
          newCohortId:
            Number(
              effectiveNewCohortId,
            ),
        }

        const response =
          format === "EXCEL"
            ? await curriculumChangeReportApi
                .exportExcel(
                  scope,
                )
            : await curriculumChangeReportApi
                .exportPdf(
                  scope,
                )

        downloadBlobResponse(
          response.data,
          `Curriculum_Change_P${changeProgramId}_C${effectiveOldCohortId}_to_C${effectiveNewCohortId}.${format === "EXCEL" ? "xlsx" : "pdf"}`,
        )
      } catch (
        error: unknown
      ) {
        alert(
          apiMessage(
            error,
            "Unable to export the curriculum change report.",
          ),
        )
      } finally {
        setIsExporting(
          null,
        )
      }
    }

  const viewCurriculumChange =
    () => {
      if (
        !changeProgramId
        || !effectiveOldCohortId
        || !effectiveNewCohortId
      ) {
        alert(
          "Select Program, From Cohort, and To Cohort.",
        )
        return
      }

      if (
        effectiveOldCohortId
        === effectiveNewCohortId
      ) {
        alert(
          "From Cohort and To Cohort must be different.",
        )
        return
      }

      const params =
        new URLSearchParams({
          mode: "curriculum",
          programId:
            changeProgramId,
          oldCohortId:
            effectiveOldCohortId,
          newCohortId:
            effectiveNewCohortId,
        })

      navigate(
        `${roleBase}/syllabus/diff?${params.toString()}`,
      )
    }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="px-6 py-5">
          <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
            Quality Assurance Reporting
          </p>

          <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
            Reports & Data Export
          </h1>

          <p className="mt-1 max-w-4xl text-sm leading-6 text-[#687f89]">
            Generate accreditation-ready reports from live curriculum, syllabus, CLO–PLO and approval data. Academic Year and Cohort are handled as separate reporting dimensions.
          </p>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card className="border border-slate-200 shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Grid3X3 className="size-5 text-[#007d84]" />
              CLO–PLO Matrix
            </CardTitle>

            <CardDescription>
              Export the whole-program CLO–PLO matrix for one curriculum scope.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            <Select
              value={matrixProgramId}
              onValueChange={(value) => {
                setMatrixProgramId(value)
                setMatrixCohortId("")
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select Program" />
              </SelectTrigger>

              <SelectContent>
                {activePrograms.map((program) => (
                  <SelectItem
                    key={program.id}
                    value={String(program.id)}
                  >
                    {programLabel(program)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Select
              value={effectiveMatrixCohortId}
              onValueChange={setMatrixCohortId}
              disabled={!matrixProgramId}
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={
                    matrixProgramId
                      ? "Select Cohort"
                      : "Select Program First"
                  }
                />
              </SelectTrigger>

              <SelectContent>
                {sortedMatrixCohorts.map((cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(cohort.id)}
                  >
                    {cohortLabel(cohort)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="grid grid-cols-2 gap-3">
              <FieldLabel label="Academic Year">
                <Select
                  value={matrixAcademicYear}
                  onValueChange={setMatrixAcademicYear}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    {academicYearOptions.map((year) => (
                      <SelectItem
                        key={year}
                        value={year}
                      >
                        {year}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FieldLabel>

              <FieldLabel label="Semester">
                <SemesterSelect
                  value={matrixSemester}
                  onChange={setMatrixSemester}
                />
              </FieldLabel>
            </div>

            <div className="flex gap-3 pt-1">
              <ExportButton
                label="Excel"
                pending={
                  isExporting
                  === "CLO_PLO_EXCEL"
                }
                onClick={() =>
                  handleMatrixExport(
                    "EXCEL",
                  )
                }
                tone="emerald"
                disabled={
                  !matrixProgramId
                  || !effectiveMatrixCohortId
                }
              />

              <ExportButton
                label="PDF"
                pending={
                  isExporting
                  === "CLO_PLO_PDF"
                }
                onClick={() =>
                  handleMatrixExport(
                    "PDF",
                  )
                }
                tone="rose"
                disabled={
                  !matrixProgramId
                  || !effectiveMatrixCohortId
                }
              />
            </div>
          </CardContent>
        </Card>

        <Card className="border border-slate-200 shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <FileText className="size-5 text-amber-600" />
              Syllabus Submission Report
            </CardTitle>

            <CardDescription>
              Review submission status, responsible instructor and approval date by academic term.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <FieldLabel label="Academic Year">
                <Select
                  value={academicYear}
                  onValueChange={setAcademicYear}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    {academicYearOptions.map((year) => (
                      <SelectItem
                        key={year}
                        value={year}
                      >
                        {year}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FieldLabel>

              <FieldLabel label="Semester">
                <SemesterSelect
                  value={semester}
                  onChange={setSemester}
                />
              </FieldLabel>
            </div>

            <Select
              value={listProgramId}
              onValueChange={(value) => {
                setListProgramId(value)
                setListCohortId(ALL)
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="All Programs" />
              </SelectTrigger>

              <SelectContent>
                <SelectItem value={ALL}>
                  All Programs
                </SelectItem>

                {activePrograms.map((program) => (
                  <SelectItem
                    key={program.id}
                    value={String(program.id)}
                  >
                    {programLabel(program)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="grid grid-cols-2 gap-3">
              <Select
                value={listCohortId}
                onValueChange={setListCohortId}
                disabled={listProgramId === ALL}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={
                      listProgramId === ALL
                        ? "Program First"
                        : "All Cohorts"
                    }
                  />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All Cohorts
                  </SelectItem>

                  {listCohorts.map((cohort) => (
                    <SelectItem
                      key={cohort.id}
                      value={String(cohort.id)}
                    >
                      {cohortLabel(cohort)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select
                value={syllabusStatus}
                onValueChange={setSyllabusStatus}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>

                <SelectContent>
                  <SelectItem value={ALL}>
                    All Statuses
                  </SelectItem>
                  <SelectItem value="DRAFT">
                    Draft
                  </SelectItem>
                  <SelectItem value="SUBMITTED">
                    Submitted
                  </SelectItem>
                  <SelectItem value="UNDER_REVIEW">
                    Under Review
                  </SelectItem>
                  <SelectItem value="APPROVED">
                    Approved
                  </SelectItem>
                  <SelectItem value="REJECTED">
                    Rejected
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex gap-3 pt-1">
              <ExportButton
                label="Excel"
                pending={
                  isExporting
                  === "SYLLABUS_LIST_EXCEL"
                }
                onClick={() =>
                  handleSyllabusListExport(
                    "EXCEL",
                  )
                }
                tone="emerald"
              />

              <ExportButton
                label="PDF"
                pending={
                  isExporting
                  === "SYLLABUS_LIST_PDF"
                }
                onClick={() =>
                  handleSyllabusListExport(
                    "PDF",
                  )
                }
                tone="rose"
              />
            </div>
          </CardContent>
        </Card>

        <Card className="border border-blue-200 bg-gradient-to-b from-white to-blue-50/40 shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <FileCheck2 className="size-5 text-blue-700" />
              Official Syllabus PDF
            </CardTitle>

            <CardDescription>
              Preview any visible version and export the official document only after approval.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            <FieldLabel label="Find Syllabus">
              <Input
                value={syllabusSearch}
                onChange={(event) =>
                  setSyllabusSearch(event.target.value)
                }
                placeholder="Search by course, year, version or status..."
              />
            </FieldLabel>

            <FieldLabel label="Syllabus Version">
              <Select
                value={selectedSyllabusId}
                onValueChange={setSelectedSyllabusId}
                disabled={loadingSyllabuses}
              >
                <SelectTrigger>
                  <SelectValue
                    placeholder={
                      loadingSyllabuses
                        ? "Loading..."
                        : "Select course and version"
                    }
                  />
                </SelectTrigger>

                <SelectContent className="max-h-80">
                  {filteredSyllabuses.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={String(item.id)}
                    >
                      {item.courseCode}
                      {" · "}
                      {item.academicYear}
                      {" · "}
                      {item.versionLabel}
                      {" · "}
                      {item.status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </FieldLabel>

            {selectedSyllabus && (
              <div className="rounded-xl border border-blue-100 bg-white p-3 text-sm">
                <p className="font-semibold text-slate-900">
                  {selectedSyllabus.courseName}
                </p>

                <p className="mt-1 text-xs text-slate-500">
                  Semester {selectedSyllabus.semester || "—"}
                  {" · "}
                  Created by {selectedSyllabus.createdByUsername}
                </p>

                {selectedSyllabus.status !== "APPROVED" && (
                  <p className="mt-2 text-xs font-medium text-amber-700">
                    Preview is available, but official export remains disabled until this version is APPROVED.
                  </p>
                )}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 pt-1">
              <Button
                variant="outline"
                onClick={() =>
                  setPreviewOpen(
                    true,
                  )
                }
                disabled={
                  !selectedSyllabus
                }
                className="gap-2 border-blue-200 bg-white text-blue-800 hover:bg-blue-50"
              >
                <Eye className="size-4" />
                Preview
              </Button>

              <Button
                onClick={handleSyllabusExport}
                disabled={
                  !selectedSyllabus
                  || selectedSyllabus.status !== "APPROVED"
                  || isExporting
                    === "SINGLE_SYLLABUS"
                }
                className="gap-2 bg-[#007d84] text-white hover:bg-[#006d73]"
              >
                {isExporting
                  === "SINGLE_SYLLABUS"
                  ? (
                    <LoaderCircle className="size-4 animate-spin" />
                  )
                  : (
                    <Download className="size-4" />
                  )}
                Export Official
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <PloCoverageReportCard />

      <Card className="border border-violet-200 shadow-sm">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <ArrowRightLeft className="size-5 text-violet-700" />
            Curriculum Change Report
          </CardTitle>

          <CardDescription>
            Compare course and curriculum metadata changes between two cohorts of the same program.
          </CardDescription>
        </CardHeader>

        <CardContent className="grid gap-4 xl:grid-cols-[1.2fr_1fr_1fr_auto_auto_auto] xl:items-end">
          <FieldLabel label="Program">
            <Select
              value={changeProgramId}
              onValueChange={(value) => {
                setChangeProgramId(value)
                setOldCohortId("")
                setNewCohortId("")
              }}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select Program" />
              </SelectTrigger>

              <SelectContent>
                {activePrograms.map((program) => (
                  <SelectItem
                    key={program.id}
                    value={String(program.id)}
                  >
                    {programLabel(program)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldLabel>

          <FieldLabel label="From Cohort">
            <Select
              value={effectiveOldCohortId}
              onValueChange={setOldCohortId}
              disabled={!changeProgramId}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select Cohort" />
              </SelectTrigger>

              <SelectContent>
                {sortedChangeCohorts.map((cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(cohort.id)}
                  >
                    {cohortLabel(cohort)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldLabel>

          <FieldLabel label="To Cohort">
            <Select
              value={effectiveNewCohortId}
              onValueChange={setNewCohortId}
              disabled={!changeProgramId}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select Cohort" />
              </SelectTrigger>

              <SelectContent>
                {sortedChangeCohorts.map((cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(cohort.id)}
                  >
                    {cohortLabel(cohort)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldLabel>

          <Button
            type="button"
            className="bg-violet-700 text-white hover:bg-violet-800"
            onClick={viewCurriculumChange}
            disabled={
              !changeProgramId
              || !effectiveOldCohortId
              || !effectiveNewCohortId
              || effectiveOldCohortId === effectiveNewCohortId
            }
          >
            View Report
          </Button>

          <ExportButton
            label="Excel"
            pending={
              isExporting
              === "CURRICULUM_CHANGE_EXCEL"
            }
            onClick={() =>
              handleCurriculumChangeExport(
                "EXCEL",
              )
            }
            tone="emerald"
            disabled={
              !changeProgramId
              || !effectiveOldCohortId
              || !effectiveNewCohortId
              || effectiveOldCohortId === effectiveNewCohortId
            }
          />

          <ExportButton
            label="PDF"
            pending={
              isExporting
              === "CURRICULUM_CHANGE_PDF"
            }
            onClick={() =>
              handleCurriculumChangeExport(
                "PDF",
              )
            }
            tone="rose"
            disabled={
              !changeProgramId
              || !effectiveOldCohortId
              || !effectiveNewCohortId
              || effectiveOldCohortId === effectiveNewCohortId
            }
          />
        </CardContent>
      </Card>

      {selectedSyllabus && (
        <SyllabusPdfPreviewDialog
          open={previewOpen}
          onOpenChange={setPreviewOpen}
          syllabusId={selectedSyllabus.id}
          courseCode={selectedSyllabus.courseCode}
          courseName={selectedSyllabus.courseName}
          status={selectedSyllabus.status}
        />
      )}
    </div>
  )
}

function FieldLabel({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <Label>
        {label}
      </Label>
      {children}
    </div>
  )
}

function SemesterSelect({
  value,
  onChange,
}: {
  value: string
  onChange:
    (value: string) => void
}) {
  return (
    <Select
      value={value}
      onValueChange={onChange}
    >
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>

      <SelectContent>
        <SelectItem value="1">
          Semester 1
        </SelectItem>
        <SelectItem value="2">
          Semester 2
        </SelectItem>
        <SelectItem value="3">
          Semester 3
        </SelectItem>
      </SelectContent>
    </Select>
  )
}

function ExportButton({
  label,
  pending,
  onClick,
  tone,
  disabled = false,
}: {
  label: string
  pending: boolean
  onClick: () => void
  tone:
    | "emerald"
    | "rose"
  disabled?: boolean
}) {
  const className =
    tone === "emerald"
      ? "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 hover:text-emerald-800"
      : "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 hover:text-rose-800"

  return (
    <Button
      variant="outline"
      className={`gap-2 ${className}`}
      onClick={onClick}
      disabled={
        pending
        || disabled
      }
    >
      {pending
        ? (
          <LoaderCircle className="size-4 animate-spin" />
        )
        : (
          <Download className="size-4" />
        )}

      {label}
    </Button>
  )
}
