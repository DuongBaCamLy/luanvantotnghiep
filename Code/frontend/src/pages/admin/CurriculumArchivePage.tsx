import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { useLocation, useNavigate } from "react-router-dom"
import {
  Archive,
  Eye,
  RotateCcw,
  Search,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { programApi } from "@/api/programApi"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { formatDateTime } from "@/i18n"
import { getSyllabusBasePath, setProgramContext } from "@/lib/programContext"
import type { Cohort } from "@/types/admin"

type Notice =
  | {
      type: "success" | "error"
      message: string
    }
  | null

function getErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: string
          }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

function archivedByLabel(
  cohort: Cohort,
): string {
  const fullName =
    cohort.archivedByFullName?.trim()

  const username =
    cohort.archivedByUsername?.trim()

  if (fullName && username) {
    return `${fullName} — ${username}`
  }

  return fullName || username || "—"
}

export default function CurriculumArchivePage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()

  const [search, setSearch] =
    useState("")
  const [notice, setNotice] =
    useState<Notice>(null)

  const {
    data: archivedCohorts = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: [
      "cohorts",
      "archived",
    ],
    queryFn:
      cohortApi.getArchived,
  })

  const {
    data: programs = [],
  } = useQuery({
    queryKey: ["programs"],
    queryFn: programApi.getAll,
  })

  const programById = useMemo(
    () =>
      new Map(
        programs.map(
          (program) => [
            program.id,
            program,
          ],
        ),
      ),
    [programs],
  )

  const restoreMutation =
    useMutation({
      mutationFn:
        (cohort: Cohort) =>
          cohortApi.reactivate(
            cohort.id,
          ),

      onSuccess:
        async (cohort) => {
          await Promise.all([
            queryClient.invalidateQueries({
              queryKey: ["cohorts"],
            }),
            queryClient.invalidateQueries({
              queryKey: [
                "cohorts",
                "archived",
              ],
            }),
            queryClient.invalidateQueries({
              queryKey: [
                "course-programs",
              ],
            }),
          ])

          setNotice({
            type: "success",
            message:
              `${cohort.name} has been restored to active curricula.`,
          })
        },

      onError:
        (mutationError) => {
          setNotice({
            type: "error",
            message: getErrorMessage(
              mutationError,
              "Unable to restore the selected cohort.",
            ),
          })
        },
    })

  const filtered =
    useMemo(() => {
      const keyword =
        search
          .trim()
          .toLowerCase()

      return archivedCohorts
        .filter(
          (cohort) =>
            cohort.isActive === false,
        )
        .filter((cohort) => {
          if (!keyword) {
            return true
          }

          return [
            cohort.name,
            cohort.programCode,
            cohort.programName,
            cohort.archivedByFullName,
            cohort.archivedByUsername,
          ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase()
            .includes(keyword)
        })
    }, [
      archivedCohorts,
      search,
    ])

  const viewSyllabuses = (
    cohort: Cohort,
  ) => {
    const program =
      programById.get(
        cohort.programId,
      )

    if (!program) {
      setNotice({
        type: "error",
        message:
          "Unable to resolve the Program for this archived Cohort.",
      })
      return
    }

    const params =
      new URLSearchParams()

    /*
     * Reuse the exact canonical context contract used by
     * Curriculum Programs -> View Syllabi.
     *
     * This carries:
     * programId, programCode, majorId, majorCode, cohortId.
     */
    setProgramContext(
      params,
      program,
      cohort,
    )

    navigate({
      pathname:
        getSyllabusBasePath(
          location.pathname,
        ),
      search:
        params.toString(),
    })
  }

  return (
    <div
      data-admin-page="CurriculumArchivePage"
      className="min-h-screen space-y-6 bg-[#FDFDF9] -m-6 p-6 md:-m-10 md:p-10"
    >
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white p-6 shadow-[0_10px_28px_rgba(0,86,94,0.07)]">
        <div className="absolute inset-x-0 top-0 h-1 bg-[#007d84]" />

        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-start gap-3">
            <div className="flex size-11 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <Archive className="size-5" />
            </div>

            <div>
              <h1 className="text-2xl font-bold text-slate-900">
                Curriculum Archive
              </h1>

              <p className="mt-1 text-sm text-slate-500">
                Archived cohorts are preserved for historical reference and excluded from active curriculum operations.
              </p>
            </div>
          </div>

          <div className="relative w-full md:w-80">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

            <Input
              value={search}
              onChange={(event) =>
                setSearch(
                  event.target.value,
                )
              }
              placeholder="Search cohort or program..."
              className="pl-9"
            />
          </div>
        </div>
      </section>

      {notice && (
        <div
          className={
            notice.type === "success"
              ? "rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
              : "rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
          }
        >
          {notice.message}
        </div>
      )}

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isLoading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            Loading archived curricula...
          </div>
        ) : isError ? (
          <div className="p-8 text-center text-sm text-rose-600">
            {getErrorMessage(
              error,
              "Unable to load Curriculum Archive.",
            )}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  Cohort
                </TableHead>

                <TableHead>
                  Program
                </TableHead>

                <TableHead>
                  Archived At
                </TableHead>

                <TableHead>
                  Archived By
                </TableHead>

                <TableHead className="text-right">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-28 text-center text-slate-500"
                  >
                    No archived curricula found.
                  </TableCell>
                </TableRow>
              ) : (
                filtered.map(
                  (cohort) => (
                    <TableRow
                      key={cohort.id}
                    >
                      <TableCell>
                        <div className="font-semibold text-slate-900">
                          {cohort.name}
                        </div>

                        <div className="text-xs text-slate-500">
                          Entry year {cohort.entryYear}
                        </div>
                      </TableCell>

                      <TableCell>
                        <div className="font-medium text-slate-800">
                          {cohort.programName}
                        </div>

                        <div className="text-xs text-slate-500">
                          {cohort.programCode}
                        </div>
                      </TableCell>

                      <TableCell>
                        {cohort.archivedAt
                          ? formatDateTime(cohort.archivedAt)
                          : "—"}
                      </TableCell>

                      <TableCell>
                        {archivedByLabel(
                          cohort,
                        )}
                      </TableCell>

                      <TableCell>
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              viewSyllabuses(
                                cohort,
                              )
                            }
                          >
                            <Eye className="mr-2 size-4" />
                            View
                          </Button>

                          <Button
                            type="button"
                            size="sm"
                            disabled={
                              restoreMutation.isPending
                            }
                            onClick={() =>
                              restoreMutation.mutate(
                                cohort,
                              )
                            }
                          >
                            <RotateCcw className="mr-2 size-4" />
                            Restore
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ),
                )
              )}
            </TableBody>
          </Table>
        )}
      </section>
    </div>
  )
}
