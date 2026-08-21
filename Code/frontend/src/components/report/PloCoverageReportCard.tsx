import { useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import axios from "axios"
import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Download,
  LoaderCircle,
  RefreshCw,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { programApi } from "@/api/programApi"
import {
  reportApi,
  type PloCoverageReportData,
  type PloCoverageReportScope,
} from "@/api/reportApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const currentAcademicYear = () => {
  const now = new Date()
  const start =
    now.getMonth() >= 7
      ? now.getFullYear()
      : now.getFullYear() - 1

  return `${start}-${start + 1}`
}

const academicYearOptions = () => {
  const current = currentAcademicYear()
  const start = Number(current.slice(0, 4))

  return Array.from(
    { length: 5 },
    (_, index) => {
      const year =
        start + 1 - index

      return `${year}-${year + 1}`
    },
  )
}

const downloadBlob = (
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

const errorMessage = (
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

const looksCorrupted = (
  value:
    | string
    | null
    | undefined,
) => {
  const text =
    String(value ?? "")

  if (!text.trim()) {
    return false
  }

  return (
    text.includes("???")
    || text.includes("�")
    || /[ÃÂÆÐ]/.test(text)
    || /[\u0000-\u0008\u000B\u000C\u000E-\u001F]/.test(text)
  )
}

const cleanDescription = (
  english:
    | string
    | null
    | undefined,
  vietnamese:
    | string
    | null
    | undefined,
) => {
  if (
    english?.trim()
    && !looksCorrupted(
      english,
    )
  ) {
    return english.trim()
  }

  if (
    vietnamese?.trim()
    && !looksCorrupted(
      vietnamese,
    )
  ) {
    return vietnamese.trim()
  }

  return "Description unavailable — repair the PLO master-data encoding."
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

export default function PloCoverageReportCard() {
  const [
    programId,
    setProgramId,
  ] = useState("")

  const [
    cohortId,
    setCohortId,
  ] = useState("")

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
    report,
    setReport,
  ] =
    useState<PloCoverageReportData | null>(
      null,
    )

  const [
    loadedScopeKey,
    setLoadedScopeKey,
  ] = useState("")

  const [
    loadingReport,
    setLoadingReport,
  ] = useState(false)

  const [
    exporting,
    setExporting,
  ] =
    useState<
      | "EXCEL"
      | "PDF"
      | null
    >(null)

  const [
    error,
    setError,
  ] = useState("")

  const {
    data: programs = [],
    isLoading:
      loadingPrograms,
  } = useQuery({
    queryKey: [
      "programs",
      "plo-coverage-report",
    ],
    queryFn: programApi.getAll,
  })

  const {
    data: cohorts = [],
    isLoading:
      loadingCohorts,
  } = useQuery({
    queryKey: [
      "cohorts",
      "plo-coverage-report",
      programId,
    ],
    queryFn: () =>
      cohortApi.getByProgram(
        Number(programId),
      ),
    enabled:
      Boolean(programId),
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
          .sort(
            (left, right) =>
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

  const sortedCohorts =
    useMemo(
      () =>
        [...cohorts].sort(
          (left, right) =>
            Number(
              right.entryYear
              ?? 0,
            )
            - Number(
              left.entryYear
              ?? 0,
            ),
        ),
      [cohorts],
    )

  const effectiveCohortId =
    cohortId
    || (
      sortedCohorts[0]
        ? String(sortedCohorts[0].id)
        : ""
    )

  const currentScopeKey =
    `${programId}|${effectiveCohortId}|${academicYear}|${semester}`

  const visibleReport =
    report
    && loadedScopeKey
      === currentScopeKey
      ? report
      : null

  const suspiciousPloCount =
    visibleReport
      ? visibleReport.plos.filter(
          (plo) =>
            looksCorrupted(
              plo.description,
            )
            && looksCorrupted(
              plo.descriptionVn,
            ),
        ).length
      : 0

  const reportReadiness =
    !visibleReport
      ? "NOT_GENERATED"
      : visibleReport.summary.totalPlos === 0
        ? "NO_PLO"
        : visibleReport.summary.totalClos === 0
          || visibleReport.summary.contributingCourses === 0
          ? "NO_EVIDENCE"
          : visibleReport.summary.mappedClos === 0
            ? "NO_MAPPING"
            : "READY"

  const canExportOfficialCoverage =
    reportReadiness === "READY"

  const buildScope =
    (): PloCoverageReportScope | null => {
      if (
        !programId
        || !effectiveCohortId
      ) {
        setError(
          "Select a Program and Cohort before generating the PLO Coverage Report.",
        )
        return null
      }

      return {
        programId:
          Number(programId),
        cohortId:
          Number(effectiveCohortId),
        academicYear,
        semester,
      }
    }

  const loadReport =
    async () => {
      const scope =
        buildScope()

      if (!scope) {
        return
      }

      try {
        setLoadingReport(true)
        setError("")

        const data =
          await reportApi
            .getPloCoverageReport(
              scope,
            )

        setReport(data)
        setLoadedScopeKey(
          currentScopeKey,
        )
      } catch (
        reason: unknown
      ) {
        setReport(null)
        setLoadedScopeKey("")
        setError(
          errorMessage(
            reason,
            "Unable to generate the PLO Coverage Report.",
          ),
        )
      } finally {
        setLoadingReport(false)
      }
    }

  const exportReport =
    async (
      format:
        | "EXCEL"
        | "PDF",
    ) => {
      const scope =
        buildScope()

      if (!scope) {
        return
      }

      try {
        setExporting(
          format,
        )
        setError("")

        const response =
          format === "EXCEL"
            ? await reportApi
                .exportPloCoverageExcel(
                  scope,
                )
            : await reportApi
                .exportPloCoveragePdf(
                  scope,
                )

        downloadBlob(
          response.data,
          `PLO_Coverage_P${programId}_C${effectiveCohortId}_${academicYear}_S${semester}.${format === "EXCEL" ? "xlsx" : "pdf"}`,
        )
      } catch (
        reason: unknown
      ) {
        setError(
          errorMessage(
            reason,
            "Unable to export the PLO Coverage Report.",
          ),
        )
      } finally {
        setExporting(null)
      }
    }

  return (
    <Card className="border border-violet-200 shadow-sm">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-lg">
          <BarChart3 className="size-5 text-violet-700" />
          PLO Coverage Report
        </CardTitle>

        <CardDescription>
          Review whole-curriculum PLO coverage, contributing courses/CLOs and I/D/A distribution.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-5">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="space-y-1.5">
            <Label>
              Program *
            </Label>

            <Select
              value={programId}
              onValueChange={(value) => {
                setProgramId(value)
                setCohortId("")
                setError("")
              }}
              disabled={loadingPrograms}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select Program" />
              </SelectTrigger>

              <SelectContent>
                {activePrograms.map(
                  (program) => (
                    <SelectItem
                      key={program.id}
                      value={String(
                        program.id,
                      )}
                    >
                      {baseProgramCode(program.code)}
                      {" — "}
                      {program.name
                        || program.nameVn}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>
              Cohort *
            </Label>

            <Select
              value={effectiveCohortId}
              onValueChange={(value) => {
                setCohortId(value)
                setError("")
              }}
              disabled={
                !programId
                || loadingCohorts
              }
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={
                    !programId
                      ? "Select Program First"
                      : "Select Cohort"
                  }
                />
              </SelectTrigger>

              <SelectContent>
                {sortedCohorts.map(
                  (cohort) => (
                    <SelectItem
                      key={cohort.id}
                      value={String(
                        cohort.id,
                      )}
                    >
                      {cohort.name}
                      {" — Entry "}
                      {cohort.entryYear}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>
              Academic Year *
            </Label>

            <Select
              value={academicYear}
              onValueChange={setAcademicYear}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>

              <SelectContent>
                {academicYearOptions().map(
                  (year) => (
                    <SelectItem
                      key={year}
                      value={year}
                    >
                      {year}
                    </SelectItem>
                  ),
                )}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>
              Semester *
            </Label>

            <Select
              value={semester}
              onValueChange={setSemester}
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
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            className="bg-violet-700 text-white hover:bg-violet-800"
            disabled={
              loadingReport
              || !programId
              || !effectiveCohortId
            }
            onClick={loadReport}
          >
            {loadingReport ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <RefreshCw className="size-4" />
            )}
            Generate Report
          </Button>

          <Button
            type="button"
            variant="outline"
            className="border-emerald-200 text-emerald-700"
            disabled={
              exporting === "EXCEL"
              || !canExportOfficialCoverage
            }
            onClick={() =>
              exportReport(
                "EXCEL",
              )
            }
          >
            {exporting
              === "EXCEL" ? (
                <LoaderCircle className="size-4 animate-spin" />
              ) : (
                <Download className="size-4" />
              )}
            Excel
          </Button>

          <Button
            type="button"
            variant="outline"
            className="border-rose-200 text-rose-700"
            disabled={
              exporting === "PDF"
              || !canExportOfficialCoverage
            }
            onClick={() =>
              exportReport(
                "PDF",
              )
            }
          >
            {exporting
              === "PDF" ? (
                <LoaderCircle className="size-4 animate-spin" />
              ) : (
                <Download className="size-4" />
              )}
            PDF
          </Button>
        </div>

        {error && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {visibleReport && (
          <>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <SummaryCard
                label="PLO Covered"
                value={
                  reportReadiness === "READY"
                    ? `${visibleReport.summary.coveredPlos}/${visibleReport.summary.totalPlos}`
                    : "—"
                }
              />

              <SummaryCard
                label="Coverage Rate"
                value={
                  reportReadiness === "READY"
                    ? `${visibleReport.summary.coveragePercentage}%`
                    : "—"
                }
              />

              <SummaryCard
                label="Contributing Courses"
                value={String(
                  visibleReport.summary.contributingCourses,
                )}
              />

              <SummaryCard
                label="Mapped CLOs"
                value={`${visibleReport.summary.mappedClos}/${visibleReport.summary.totalClos}`}
              />
            </div>

            {reportReadiness !== "READY" && (
              <div className="flex gap-3 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm text-blue-800">
                <AlertTriangle className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    Coverage is not ready for official interpretation.
                  </p>

                  <p className="mt-1 leading-6">
                    {reportReadiness === "NO_PLO"
                      ? "No active PLO is available for the selected program."
                      : reportReadiness === "NO_EVIDENCE"
                        ? "The selected scope has no contributing CLO evidence. Do not interpret this state as 0% PLO coverage; review approved syllabus data or choose another academic period."
                        : "CLOs are available, but no CLO–PLO mapping contributes to this scope yet. Complete the mappings before accepting the coverage result as official."}
                  </p>
                </div>
              </div>
            )}

            {reportReadiness === "READY"
            && visibleReport.summary.uncoveredPlos > 0 && (
              <div className="flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                <AlertTriangle className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    {visibleReport.summary.uncoveredPlos} PLO(s) are not covered in the selected scope.
                  </p>

                  <p className="mt-1">
                    {visibleReport.plos
                      .filter(
                        (plo) =>
                          !plo.covered,
                      )
                      .map(
                        (plo) =>
                          plo.ploCode,
                      )
                      .join(", ")}
                  </p>
                </div>
              </div>
            )}

            {suspiciousPloCount > 0 && (
              <div className="flex gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
                <AlertTriangle className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    PLO master-data encoding issue detected.
                  </p>

                  <p className="mt-1">
                    {suspiciousPloCount} PLO description(s) contain corrupted characters. Repair the source PLO records before accepting Excel/PDF exports as official.
                  </p>
                </div>
              </div>
            )}

            <div className="overflow-hidden rounded-xl border border-slate-200">
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1050px] text-sm">
                  <thead className="bg-slate-50 text-left text-[11px] uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-4 py-3">
                        PLO
                      </th>

                      <th className="px-4 py-3">
                        Description
                      </th>

                      <th className="px-4 py-3 text-center">
                        Status
                      </th>

                      <th className="px-4 py-3 text-center">
                        Courses
                      </th>

                      <th className="px-4 py-3 text-center">
                        CLOs
                      </th>

                      <th className="px-4 py-3 text-center">
                        I / D / A
                      </th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-slate-100 bg-white">
                    {visibleReport.plos.map(
                      (plo) => (
                        <tr
                          key={plo.ploId}
                          className="align-top"
                        >
                          <td className="px-4 py-3 font-bold text-slate-900">
                            {plo.ploCode}
                          </td>

                          <td className="max-w-[680px] px-4 py-3 leading-6 text-slate-600">
                            {cleanDescription(
                              plo.description,
                              plo.descriptionVn,
                            )}
                          </td>

                          <td className="px-4 py-3 text-center">
                            {reportReadiness !== "READY" ? (
                              <Badge
                                variant="outline"
                                className="border-blue-200 bg-blue-50 text-blue-700"
                              >
                                Pending data
                              </Badge>
                            ) : plo.covered ? (
                              <Badge
                                variant="outline"
                                className="border-emerald-200 bg-emerald-50 text-emerald-700"
                              >
                                <CheckCircle2 className="mr-1 size-3" />
                                Covered
                              </Badge>
                            ) : (
                              <Badge
                                variant="outline"
                                className="border-amber-200 bg-amber-50 text-amber-700"
                              >
                                Uncovered
                              </Badge>
                            )}
                          </td>

                          <td className="px-4 py-3 text-center font-medium text-slate-700">
                            {plo.contributingCourseCount}
                          </td>

                          <td className="px-4 py-3 text-center font-medium text-slate-700">
                            {plo.contributingCloCount}
                          </td>

                          <td className="px-4 py-3 text-center font-medium text-slate-700">
                            {plo.introductionCount}
                            {" / "}
                            {plo.developmentCount}
                            {" / "}
                            {plo.achievementCount}
                          </td>
                        </tr>
                      ),
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

function SummaryCard({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-2xl font-bold text-slate-900">
        {value}
      </p>
    </div>
  )
}