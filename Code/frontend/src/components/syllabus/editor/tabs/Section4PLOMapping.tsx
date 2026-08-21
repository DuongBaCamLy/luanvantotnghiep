import {
  useMemo,
  useState,
} from "react"
import {
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Info,
  Layers,
  Loader2,
  RefreshCw,
  ShieldCheck,
  Target,
} from "lucide-react"

import { cloPloMappingApi } from "@/api/cloApi"
import { courseProgramApi } from "@/api/courseProgramApi"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useClos } from "@/hooks/useClos"
import { useAllPlos } from "@/hooks/useSyllabusBooks"
import { useSyllabus } from "@/hooks/useSyllabus"
import type { Plo } from "@/types/book"
import type {
  Clo,
  CloPloMapping,
  ContributionLevel,
} from "@/types/clo"

interface Props {
  syllabusId: number
  readOnly?: boolean
}

interface CourseProgramRef {
  courseId?: number | null
  courseCode?: string | null
  programId?: number | null
  programCode?: string | null
  programName?: string | null
  programNameVn?: string | null
  cohortId?: number | null
  cohortName?: string | null
  course?: {
    id?: number | null
    courseId?: number | null
    code?: string | null
    courseCode?: string | null
  } | null
  program?: {
    id?: number | null
    programId?: number | null
    code?: string | null
    name?: string | null
    nameVn?: string | null
  } | null
}

interface ProgramOption {
  id: number
  code: string
  name: string
}

interface LevelConfig {
  value: ContributionLevel | ""
  label: string
  longLabel: string
  bg: string
  text: string
  border: string
}

const LEVELS: LevelConfig[] = [
  {
    value: "",
    label: "—",
    longLabel: "No mapping",
    bg: "bg-white",
    text: "text-slate-400",
    border: "border-slate-200",
  },
  {
    value: "I",
    label: "I",
    longLabel: "Introduce",
    bg: "bg-sky-50",
    text: "text-sky-700",
    border: "border-sky-200",
  },
  {
    value: "D",
    label: "D",
    longLabel: "Develop",
    bg: "bg-amber-50",
    text: "text-amber-700",
    border: "border-amber-200",
  },
  {
    value: "A",
    label: "A",
    longLabel: "Achieve",
    bg: "bg-emerald-50",
    text: "text-emerald-700",
    border: "border-emerald-200",
  },
]

const normalize = (
  value: unknown,
) =>
  String(value ?? "")
    .trim()

const normalizeCode = (
  value: unknown,
) =>
  normalize(value)
    .toUpperCase()

const toNumber = (
  value: unknown,
): number | undefined => {
  if (
    value === null
    || value === undefined
    || normalize(value) === ""
  ) {
    return undefined
  }

  const numberValue =
    Number(value)

  return Number.isFinite(
    numberValue,
  )
    ? numberValue
    : undefined
}

const toArray = <T,>(
  payload: unknown,
): T[] => {
  if (
    Array.isArray(payload)
  ) {
    return payload as T[]
  }

  if (
    typeof payload
      !== "object"
    || payload === null
  ) {
    return []
  }

  const record =
    payload as Record<
      string,
      unknown
    >

  for (
    const key
    of [
      "data",
      "content",
      "items",
      "result",
    ]
  ) {
    if (
      Array.isArray(
        record[key],
      )
    ) {
      return record[key] as T[]
    }
  }

  return []
}

const getCourseProgramCourseId = (
  item: CourseProgramRef,
) =>
  toNumber(
    item.courseId
    ?? item.course?.id
    ?? item.course
      ?.courseId,
  )

const getCourseProgramProgramId = (
  item: CourseProgramRef,
) =>
  toNumber(
    item.programId
    ?? item.program?.id
    ?? item.program
      ?.programId,
  )

const getCourseProgramProgramCode = (
  item: CourseProgramRef,
) =>
  normalize(
    item.programCode
    ?? item.program?.code,
  )

const getCourseProgramProgramName = (
  item: CourseProgramRef,
) =>
  normalize(
    item.programName
    ?? item.programNameVn
    ?? item.program?.name
    ?? item.program?.nameVn,
  )

const getLevelConfig = (
  value:
    ContributionLevel
    | "",
) =>
  LEVELS.find(
    (level) =>
      level.value === value,
  )
  ?? LEVELS[0]

const getMapping = (
  mappings:
    Record<
      number,
      CloPloMapping[]
    >,
  cloId: number,
  ploId: number,
) =>
  mappings[cloId]?.find(
    (mapping) =>
      mapping.ploId
      === ploId,
  )

const apiErrorMessage = (
  error: unknown,
  fallback: string,
) => {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response =
      (
        error as {
          response?: {
            data?: {
              message?: string
            }
          }
        }
      ).response

    if (
      response
        ?.data
        ?.message
    ) {
      return response
        .data
        .message
    }
  }

  if (
    error instanceof Error
    && error.message
  ) {
    return error.message
  }

  return fallback
}

const latestActivePlos = (
  plos: Plo[],
  programId: number,
) => {
  const active =
    plos.filter(
      (plo) =>
        plo.programId
          === programId
        && plo.isActive
          !== false,
    )

  const latestByCode =
    new Map<string, Plo>()

  active.forEach(
    (plo) => {
      const key =
        normalizeCode(
          plo.code,
        )

      const current =
        latestByCode.get(
          key,
        )

      if (
        !current
        || Number(
          plo.versionNumber
          ?? 1,
        )
          > Number(
            current
              .versionNumber
            ?? 1,
          )
      ) {
        latestByCode.set(
          key,
          plo,
        )
      }
    },
  )

  return Array.from(
    latestByCode.values(),
  ).sort(
    (left, right) =>
      normalizeCode(
        left.code,
      ).localeCompare(
        normalizeCode(
          right.code,
        ),
        "en",
        {
          numeric: true,
        },
      ),
  )
}

export default function Section4PLOMapping({
  syllabusId,
  readOnly = false,
}: Props) {
  const queryClient =
    useQueryClient()

  const [
    selectedProgramId,
    setSelectedProgramId,
  ] = useState("")

  const [
    savingCell,
    setSavingCell,
  ] =
    useState<string | null>(
      null,
    )

  const [
    actionError,
    setActionError,
  ] =
    useState<string | null>(
      null,
    )

  const {
    data: syllabus,
    isLoading:
      loadingSyllabus,
    isError:
      syllabusError,
  } =
    useSyllabus(
      syllabusId,
    )

  const {
    data: clos = [],
    isLoading:
      loadingClos,
    isError:
      closError,
    refetch:
      refetchClos,
  } =
    useClos(
      syllabusId,
    )

  const {
    data:
      rawPlos = [],
    isLoading:
      loadingPlos,
    isError:
      plosError,
  } =
    useAllPlos()

  const {
    data:
      coursePrograms = [],
    isLoading:
      loadingCoursePrograms,
    isError:
      courseProgramsError,
    refetch:
      refetchCoursePrograms,
  } =
    useQuery({
      queryKey: [
        "course-programs",
        "clo-plo-scope",
        syllabus?.courseId
          ?? null,
      ],

      queryFn:
        async () =>
          toArray<CourseProgramRef>(
            await courseProgramApi
              .getAll(),
          ),

      enabled:
        Boolean(
          syllabus?.courseId,
        ),
      staleTime:
        60_000,
    })

  const mappingQueries =
    useQueries({
      queries:
        clos.map(
          (clo) => ({
            queryKey: [
              "clo-plo-mappings",
              clo.id,
            ],
            queryFn: () =>
              cloPloMappingApi
                .getByClo(
                  clo.id,
                ),
            enabled:
              clo.id > 0,
            staleTime:
              15_000,
          }),
        ),
    })

  const sortedClos =
    useMemo(
      () =>
        [...clos].sort(
          (
            left,
            right,
          ) => {
            const orderDiff =
              Number(
                left.orderIndex
                ?? 0,
              )
              - Number(
                right.orderIndex
                ?? 0,
              )

            if (orderDiff) {
              return orderDiff
            }

            return normalizeCode(
              left.code,
            ).localeCompare(
              normalizeCode(
                right.code,
              ),
              "en",
              {
                numeric: true,
              },
            )
          },
        ),
      [clos],
    )

  const allMappings =
    useMemo(() => {
      const result:
        Record<
          number,
          CloPloMapping[]
        > = {}

      sortedClos.forEach(
        (
          clo,
          index,
        ) => {
          result[clo.id] =
            mappingQueries[index]
              ?.data
            ?? []
        },
      )

      return result
    }, [
      sortedClos,
      mappingQueries,
    ])

  const programOptions =
    useMemo<
      ProgramOption[]
    >(
      () => {
        if (
          !syllabus
            ?.courseId
        ) {
          return []
        }

        const map =
          new Map<
            number,
            ProgramOption
          >()

        coursePrograms
          .filter(
            (item) =>
              getCourseProgramCourseId(
                item,
              )
              === syllabus
                .courseId,
          )
          .forEach(
            (item) => {
              const id =
                getCourseProgramProgramId(
                  item,
                )

              if (!id) {
                return
              }

              const existing =
                map.get(id)

              const code =
                getCourseProgramProgramCode(
                  item,
                )

              const name =
                getCourseProgramProgramName(
                  item,
                )

              map.set(
                id,
                {
                  id,
                  code:
                    code
                    || existing
                      ?.code
                    || `Program ${id}`,
                  name:
                    name
                    || existing
                      ?.name
                    || "",
                },
              )
            },
          )

        return Array.from(
          map.values(),
        ).sort(
          (left, right) =>
            left.code
              .localeCompare(
                right.code,
                "en",
                {
                  numeric: true,
                },
              ),
        )
      },
      [
        coursePrograms,
        syllabus,
      ],
    )

  const mappedProgramIds =
    useMemo(
      () => {
        const ploById =
          new Map(
            (
              rawPlos as Plo[]
            ).map(
              (plo) => [
                plo.id,
                plo,
              ],
            ),
          )

        const result =
          new Set<number>()

        Object.values(
          allMappings,
        )
          .flat()
          .forEach(
            (mapping) => {
              const plo =
                ploById.get(
                  mapping.ploId,
                )

              if (
                plo?.programId
              ) {
                result.add(
                  plo.programId,
                )
              }
            },
          )

        return Array.from(
          result,
        )
      },
      [
        rawPlos,
        allMappings,
      ],
    )

  const inferredProgramId =
    useMemo(
      () => {
        if (
          mappedProgramIds
            .length === 1
          && programOptions
            .some(
              (program) =>
                program.id
                === mappedProgramIds[0],
            )
        ) {
          return mappedProgramIds[0]
        }

        if (
          programOptions
            .length === 1
        ) {
          return programOptions[0]
            .id
        }

        return undefined
      },
      [
        mappedProgramIds,
        programOptions,
      ],
    )

  const effectiveProgramId =
    toNumber(
      selectedProgramId,
    )
    ?? inferredProgramId

  const selectedProgram =
    programOptions.find(
      (program) =>
        program.id
        === effectiveProgramId,
    )

  const scopedPlos =
    useMemo(
      () =>
        effectiveProgramId
          ? latestActivePlos(
              rawPlos as Plo[],
              effectiveProgramId,
            )
          : [],
      [
        rawPlos,
        effectiveProgramId,
      ],
    )

  const scopedPloIds =
    useMemo(
      () =>
        new Set(
          scopedPlos.map(
            (plo) => plo.id,
          ),
        ),
      [scopedPlos],
    )

  const scopedMappings =
    useMemo(() => {
      const result:
        Record<
          number,
          CloPloMapping[]
        > = {}

      sortedClos.forEach(
        (clo) => {
          result[clo.id] =
            (
              allMappings[
                clo.id
              ]
              ?? []
            ).filter(
              (mapping) =>
                scopedPloIds.has(
                  mapping.ploId,
                ),
            )
        },
      )

      return result
    }, [
      sortedClos,
      allMappings,
      scopedPloIds,
    ])

  const outOfScopeMappings =
    useMemo(
      () =>
        Object.values(
          allMappings,
        )
          .flat()
          .filter(
            (mapping) =>
              effectiveProgramId
              && !scopedPloIds.has(
                mapping.ploId,
              ),
          ),
      [
        allMappings,
        effectiveProgramId,
        scopedPloIds,
      ],
    )

  const unmappedClos =
    useMemo(
      () =>
        sortedClos.filter(
          (clo) =>
            (
              scopedMappings[
                clo.id
              ]
              ?? []
            ).length === 0,
        ),
      [
        sortedClos,
        scopedMappings,
      ],
    )

  const coveredPloIds =
    useMemo(
      () =>
        new Set(
          Object.values(
            scopedMappings,
          )
            .flat()
            .map(
              (mapping) =>
                mapping.ploId,
            ),
        ),
      [scopedMappings],
    )

  const uncoveredPlos =
    useMemo(
      () =>
        scopedPlos.filter(
          (plo) =>
            !coveredPloIds
              .has(plo.id),
        ),
      [
        scopedPlos,
        coveredPloIds,
      ],
    )

  const mappingError =
    mappingQueries.some(
      (query) =>
        query.isError,
    )

  const loading =
    loadingSyllabus
    || loadingClos
    || loadingPlos
    || loadingCoursePrograms
    || mappingQueries.some(
      (query) =>
        query.isLoading,
    )

  const retryAll =
    async () => {
      await Promise.all([
        refetchClos(),
        refetchCoursePrograms(),
        ...mappingQueries.map(
          (query) =>
            query.refetch(),
        ),
      ])
    }

  const getCurrentLevel = (
    cloId: number,
    ploId: number,
  ):
    ContributionLevel
    | "" =>
    getMapping(
      scopedMappings,
      cloId,
      ploId,
    )?.level
    ?? ""

  const handleCellClick =
    async (
      cloId: number,
      ploId: number,
    ) => {
      if (
        readOnly
        || savingCell
      ) {
        return
      }

      const key =
        `${cloId}-${ploId}`

      setSavingCell(key)
      setActionError(null)

      const current =
        getCurrentLevel(
          cloId,
          ploId,
        )

      const currentIndex =
        LEVELS.findIndex(
          (level) =>
            level.value
            === current,
        )

      const next =
        LEVELS[
          (
            currentIndex + 1
          )
          % LEVELS.length
        ].value

      const existing =
        getMapping(
          scopedMappings,
          cloId,
          ploId,
        )

      try {
        if (
          existing
          && next === ""
        ) {
          await cloPloMappingApi
            .delete(
              existing.id,
            )
        } else if (
          !existing
          && next !== ""
        ) {
          await cloPloMappingApi
            .create({
              cloId,
              ploId,
              level: next,
            })
        } else if (
          existing
          && next !== ""
        ) {
          /*
           * The current API exposes create/delete but no update.
           * Change the level transactionally as far as the client can:
           * delete old -> create new -> recreate old on failure.
           */
          await cloPloMappingApi
            .delete(
              existing.id,
            )

          try {
            await cloPloMappingApi
              .create({
                cloId,
                ploId,
                level: next,
              })
          } catch (
            createError
          ) {
            if (current !== "") {
              try {
                await cloPloMappingApi
                  .create({
                    cloId,
                    ploId,
                    level: current,
                  })
              } catch {
                // Backend/server logs remain the source of truth if rollback also fails.
              }
            }

            throw createError
          }
        }

        await queryClient
          .invalidateQueries({
            queryKey: [
              "clo-plo-mappings",
              cloId,
            ],
          })
      } catch (error) {
        setActionError(
          apiErrorMessage(
            error,
            `Unable to update the ${getLevelConfig(next).longLabel.toLowerCase()} mapping.`,
          ),
        )
      } finally {
        setSavingCell(null)
      }
    }

  if (loading) {
    return (
      <div className="flex h-52 items-center justify-center gap-2 text-slate-400">
        <Loader2 className="size-5 animate-spin" />
        Loading CLO–PLO mapping scope...
      </div>
    )
  }

  if (
    syllabusError
    || closError
    || plosError
    || courseProgramsError
    || mappingError
  ) {
    return (
      <Card className="border border-rose-200 bg-rose-50/60 shadow-sm">
        <CardContent className="p-6">
          <div className="flex items-start gap-3">
            <AlertTriangle className="mt-0.5 size-5 shrink-0 text-rose-600" />

            <div>
              <p className="font-semibold text-rose-800">
                Unable to load CLO–PLO mapping data
              </p>

              <p className="mt-1 text-sm leading-6 text-rose-700">
                The mapping matrix requires the syllabus, CLOs, curriculum program links, PLOs, and existing mappings to load successfully.
              </p>

              <Button
                type="button"
                variant="outline"
                size="sm"
                className="mt-3 gap-2 bg-white"
                onClick={() =>
                  void retryAll()
                }
              >
                <RefreshCw className="size-4" />
                Try Again
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (
    sortedClos.length === 0
  ) {
    return (
      <Card className="border border-amber-200 bg-amber-50/50 shadow-sm">
        <CardContent className="px-6 py-10 text-center">
          <AlertCircle className="mx-auto mb-3 size-10 text-amber-400" />

          <p className="text-sm font-semibold text-amber-900">
            Course Learning Outcomes are required first
          </p>

          <p className="mx-auto mt-2 max-w-xl text-xs leading-5 text-amber-700">
            Add at least one CLO in the Course Learning Outcomes tab before creating program-outcome mappings.
          </p>
        </CardContent>
      </Card>
    )
  }

  if (
    programOptions.length === 0
  ) {
    return (
      <Card className="border border-rose-200 bg-rose-50/50 shadow-sm">
        <CardContent className="px-6 py-8">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 size-5 shrink-0 text-rose-600" />

            <div>
              <p className="font-semibold text-rose-900">
                No curriculum program scope is available for this course
              </p>

              <p className="mt-2 text-sm leading-6 text-rose-700">
                CLOs must map to PLOs belonging to a curriculum program that actually contains this course. Ask an Administrator to add the course to the appropriate Program/Cohort curriculum before mapping outcomes.
              </p>

              <p className="mt-2 text-xs text-rose-600">
                The editor intentionally does not expose every PLO in the database because that could create invalid cross-program mappings.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-5">
      <Card className="border border-cyan-200 bg-cyan-50/40 shadow-sm">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <Target className="mt-0.5 size-5 shrink-0 text-primary" />

            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-slate-800">
                Program-scoped outcome mapping
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-600">
                Only active PLOs from the selected curriculum program are shown. If multiple PLO versions share the same code, the latest active version is used.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="border-b border-slate-100 pb-3">
          <CardTitle className="flex items-center gap-2 text-sm font-bold text-slate-700">
            <Layers className="size-4 text-primary" />
            Mapping Scope
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-4 pt-4">
          <div className="grid gap-4 lg:grid-cols-[minmax(280px,440px)_1fr]">
            <div>
              <p className="mb-1.5 text-[10px] font-bold uppercase tracking-[0.1em] text-slate-500">
                Curriculum Program
              </p>

              {programOptions.length === 1 ? (
                <div className="flex min-h-10 items-center rounded-lg border border-slate-200 bg-slate-50 px-3 text-sm font-semibold text-slate-700">
                  {programOptions[0].code}
                  {programOptions[0].name
                    ? ` — ${programOptions[0].name}`
                    : ""}
                </div>
              ) : (
                <Select
                  value={
                    effectiveProgramId
                      ? String(
                          effectiveProgramId,
                        )
                      : undefined
                  }
                  onValueChange={(
                    value,
                  ) => {
                    setSelectedProgramId(
                      value,
                    )
                    setActionError(
                      null,
                    )
                  }}
                  disabled={
                    savingCell !== null
                  }
                >
                  <SelectTrigger className="bg-white">
                    <SelectValue placeholder="Select the program whose PLOs this syllabus contributes to" />
                  </SelectTrigger>

                  <SelectContent>
                    {programOptions.map(
                      (program) => (
                        <SelectItem
                          key={program.id}
                          value={String(
                            program.id,
                          )}
                        >
                          {program.code}
                          {program.name
                            ? ` — ${program.name}`
                            : ""}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Metric
                label="CLOs"
                value={
                  sortedClos.length
                }
              />

              <Metric
                label="Program PLOs"
                value={
                  scopedPlos.length
                }
              />

              <Metric
                label="Mapped CLOs"
                value={
                  sortedClos.length
                  - unmappedClos.length
                }
              />

              <Metric
                label="Mapped Cells"
                value={
                  Object.values(
                    scopedMappings,
                  )
                    .flat()
                    .length
                }
              />
            </div>
          </div>

          {selectedProgram && (
            <p className="text-xs text-slate-500">
              Current scope:{" "}
              <strong className="text-slate-700">
                {selectedProgram.code}
                {selectedProgram.name
                  ? ` — ${selectedProgram.name}`
                  : ""}
              </strong>
            </p>
          )}
        </CardContent>
      </Card>

      {!effectiveProgramId && (
        <Card className="border border-amber-200 bg-amber-50/50 shadow-sm">
          <CardContent className="p-5">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 size-5 shrink-0 text-amber-600" />

              <div>
                <p className="font-semibold text-amber-900">
                  Select a curriculum program
                </p>

                <p className="mt-1 text-sm leading-6 text-amber-700">
                  This course belongs to more than one curriculum program. Select the intended program before creating CLO–PLO mappings so the syllabus cannot accidentally map to an unrelated PLO set.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {effectiveProgramId
        && scopedPlos.length === 0 && (
        <Card className="border border-amber-200 bg-amber-50/50 shadow-sm">
          <CardContent className="p-5">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 size-5 shrink-0 text-amber-600" />

              <div>
                <p className="font-semibold text-amber-900">
                  No active PLOs are available in this program
                </p>

                <p className="mt-1 text-sm leading-6 text-amber-700">
                  An Administrator or Dean must define active Program Learning Outcomes before the Instructor can complete this section.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {effectiveProgramId
        && scopedPlos.length > 0 && (
        <>
          <Card className="border border-slate-200 shadow-sm">
            <CardContent className="p-4">
              <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
                <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-slate-500">
                  Contribution Level
                </p>

                {LEVELS.slice(1).map(
                  (level) => (
                    <div
                      key={level.value}
                      className="flex items-center gap-2"
                    >
                      <span
                        className={`flex size-7 items-center justify-center rounded-lg border text-sm font-bold ${level.bg} ${level.text} ${level.border}`}
                      >
                        {level.label}
                      </span>

                      <span className="text-xs text-slate-600">
                        {level.longLabel}
                      </span>
                    </div>
                  ),
                )}

                {!readOnly && (
                  <p className="ml-auto text-xs italic text-slate-400">
                    Click a cell to cycle: Empty → I → D → A → Empty
                  </p>
                )}
              </div>
            </CardContent>
          </Card>

          {actionError && (
            <Card className="border border-rose-200 bg-rose-50/50 shadow-sm">
              <CardContent className="p-4">
                <div className="flex items-start gap-2">
                  <AlertTriangle className="mt-0.5 size-4 shrink-0 text-rose-600" />

                  <p className="text-xs leading-5 text-rose-700">
                    {actionError}
                  </p>
                </div>
              </CardContent>
            </Card>
          )}

          {unmappedClos.length > 0 ? (
            <Card className="border border-rose-200 bg-rose-50/40 shadow-sm">
              <CardContent className="p-4">
                <div className="flex items-start gap-2">
                  <AlertCircle className="mt-0.5 size-4 shrink-0 text-rose-500" />

                  <div>
                    <p className="text-xs font-semibold text-rose-800">
                      Submission blocker
                    </p>

                    <p className="mt-1 text-xs leading-5 text-rose-700">
                      {unmappedClos
                        .map(
                          (clo) =>
                            clo.code,
                        )
                        .join(", ")}{" "}
                      {unmappedClos.length === 1
                        ? "is"
                        : "are"}{" "}
                      not mapped to any PLO in the selected program. Every CLO must map to at least one PLO before submission.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card className="border border-emerald-200 bg-emerald-50/40 shadow-sm">
              <CardContent className="p-4">
                <div className="flex items-start gap-2">
                  <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" />

                  <p className="text-xs leading-5 text-emerald-800">
                    Every CLO is mapped to at least one PLO in the selected program.
                  </p>
                </div>
              </CardContent>
            </Card>
          )}

          {outOfScopeMappings.length > 0 && (
            <Card className="border border-amber-200 bg-amber-50/40 shadow-sm">
              <CardContent className="p-4">
                <div className="flex items-start gap-2">
                  <ShieldCheck className="mt-0.5 size-4 shrink-0 text-amber-600" />

                  <div>
                    <p className="text-xs font-semibold text-amber-900">
                      Data integrity notice
                    </p>

                    <p className="mt-1 text-xs leading-5 text-amber-700">
                      {outOfScopeMappings.length} existing mapping
                      {outOfScopeMappings.length === 1 ? "" : "s"} point to PLOs outside the currently selected program. They are excluded from readiness counts. Review legacy data before final submission if these mappings were not intentional.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          <Card className="border border-slate-200 shadow-sm">
            <CardHeader className="border-b border-slate-100 pb-3">
              <CardTitle className="flex flex-wrap items-center gap-2 text-sm font-bold text-slate-700">
                <Layers className="size-4 text-primary" />
                CLO–PLO Matrix

                {unmappedClos.length === 0 && (
                  <Badge
                    variant="outline"
                    className="border-emerald-200 bg-emerald-50 text-emerald-700"
                  >
                    <CheckCircle2 className="mr-1 size-3" />
                    All CLOs mapped
                  </Badge>
                )}
              </CardTitle>
            </CardHeader>

            <CardContent className="overflow-auto p-0">
              <table className="w-full min-w-max border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 bg-slate-50">
                    <th className="sticky left-0 z-10 min-w-[220px] border-r border-slate-200 bg-slate-50 px-4 py-3 text-left text-xs font-bold uppercase tracking-wider text-slate-600">
                      CLO / PLO
                    </th>

                    {scopedPlos.map(
                      (plo) => (
                        <th
                          key={plo.id}
                          className="min-w-[112px] px-3 py-3 text-center"
                        >
                          <div className="flex flex-col items-center gap-1">
                            <span className="text-xs font-bold text-primary">
                              {plo.code}
                            </span>

                            <span className="text-[10px] font-normal text-slate-400">
                              v{plo.versionNumber ?? 1}
                            </span>
                          </div>
                        </th>
                      ),
                    )}

                    <th className="min-w-[95px] px-3 py-3 text-center text-xs font-bold uppercase tracking-wider text-slate-600">
                      Mapped
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {sortedClos.map(
                    (
                      clo: Clo,
                    ) => {
                      const rowMappings =
                        scopedMappings[
                          clo.id
                        ]
                        ?? []

                      return (
                        <tr
                          key={clo.id}
                          className="transition-colors hover:bg-slate-50/40"
                        >
                          <td className="sticky left-0 z-10 border-r border-slate-200 bg-white px-4 py-3">
                            <div className="max-w-[205px]">
                              <p className="font-mono text-xs font-bold text-primary">
                                {clo.code}
                              </p>

                              <p
                                className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500"
                                title={
                                  clo.description
                                }
                              >
                                {clo.description}
                              </p>
                            </div>
                          </td>

                          {scopedPlos.map(
                            (plo) => {
                              const level =
                                getCurrentLevel(
                                  clo.id,
                                  plo.id,
                                )

                              const levelCfg =
                                getLevelConfig(
                                  level,
                                )

                              const key =
                                `${clo.id}-${plo.id}`

                              const isSaving =
                                savingCell
                                === key

                              return (
                                <td
                                  key={plo.id}
                                  className="px-3 py-3 text-center"
                                >
                                  <button
                                    type="button"
                                    aria-label={`${clo.code} to ${plo.code}: ${levelCfg.longLabel}`}
                                    onClick={() =>
                                      void handleCellClick(
                                        clo.id,
                                        plo.id,
                                      )
                                    }
                                    disabled={
                                      readOnly
                                      || savingCell
                                        !== null
                                    }
                                    className={`mx-auto flex size-10 items-center justify-center rounded-lg border text-sm font-bold transition ${
                                      levelCfg.bg
                                    } ${
                                      levelCfg.text
                                    } ${
                                      levelCfg.border
                                    } ${
                                      !readOnly
                                        && savingCell === null
                                        ? "cursor-pointer hover:-translate-y-0.5 hover:shadow-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                                        : "cursor-default"
                                    } ${
                                      isSaving
                                        ? "animate-pulse opacity-60"
                                        : ""
                                    }`}
                                    title={`${clo.code} → ${plo.code}: ${levelCfg.longLabel}`}
                                  >
                                    {isSaving
                                      ? "…"
                                      : levelCfg.label}
                                  </button>
                                </td>
                              )
                            },
                          )}

                          <td className="px-3 py-3 text-center">
                            <Badge
                              variant="outline"
                              className={
                                rowMappings.length === 0
                                  ? "border-rose-200 bg-rose-50 text-rose-700"
                                  : "border-emerald-200 bg-emerald-50 text-emerald-700"
                              }
                            >
                              {rowMappings.length}
                            </Badge>
                          </td>
                        </tr>
                      )
                    },
                  )}
                </tbody>

                <tfoot>
                  <tr className="border-t-2 border-slate-200 bg-slate-50">
                    <td className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50 px-4 py-3">
                      <div>
                        <p className="text-xs font-bold text-slate-600">
                          Course contribution
                        </p>

                        <p className="mt-0.5 text-[10px] text-slate-400">
                          Informational only — one course is not required to cover every PLO.
                        </p>
                      </div>
                    </td>

                    {scopedPlos.map(
                      (plo) => {
                        const count =
                          sortedClos.filter(
                            (clo) =>
                              getCurrentLevel(
                                clo.id,
                                plo.id,
                              ) !== "",
                          ).length

                        return (
                          <td
                            key={plo.id}
                            className="px-3 py-3 text-center"
                          >
                            <Badge
                              variant="outline"
                              className={
                                count === 0
                                  ? "border-slate-200 bg-white text-slate-500"
                                  : "border-cyan-200 bg-cyan-50 text-primary"
                              }
                            >
                              {count}
                            </Badge>
                          </td>
                        )
                      },
                    )}

                    <td />
                  </tr>
                </tfoot>
              </table>
            </CardContent>
          </Card>

          {uncoveredPlos.length > 0 && (
            <Card className="border border-blue-100 bg-blue-50/30 shadow-sm">
              <CardContent className="p-4">
                <div className="flex items-start gap-2">
                  <Info className="mt-0.5 size-4 shrink-0 text-blue-600" />

                  <p className="text-xs leading-5 text-blue-700">
                    This course does not contribute to{" "}
                    {uncoveredPlos
                      .map(
                        (plo) =>
                          plo.code,
                      )
                      .join(", ")}. This is informational at syllabus level; program-wide PLO coverage is evaluated in the CLO–PLO Heatmap across approved courses.
                  </p>
                </div>
              </CardContent>
            </Card>
          )}

          <Card className="border border-slate-200 shadow-sm">
            <CardHeader className="border-b border-slate-100 pb-3">
              <CardTitle className="text-sm font-bold text-slate-700">
                Program Learning Outcome Reference
              </CardTitle>
            </CardHeader>

            <CardContent className="grid gap-3 p-4 md:grid-cols-2">
              {scopedPlos.map(
                (plo) => (
                  <div
                    key={plo.id}
                    className="rounded-xl border border-slate-200 bg-slate-50/60 p-4"
                  >
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-sm font-bold text-primary">
                        {plo.code}
                      </span>

                      {plo.category && (
                        <Badge
                          variant="outline"
                          className="border-slate-200 bg-white text-slate-600"
                        >
                          {plo.category}
                        </Badge>
                      )}

                      <span className="text-[10px] text-slate-400">
                        Version {plo.versionNumber ?? 1}
                      </span>
                    </div>

                    <p className="mt-2 text-xs leading-5 text-slate-700">
                      {plo.description
                        || "No English description recorded."}
                    </p>

                    {plo.descriptionVn && (
                      <p className="mt-2 text-xs leading-5 text-slate-500">
                        {plo.descriptionVn}
                      </p>
                    )}
                  </div>
                ),
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}

function Metric({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50/70 px-3 py-3 text-center">
      <p className="text-xl font-bold text-slate-800">
        {value}
      </p>

      <p className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-slate-400">
        {label}
      </p>
    </div>
  )
}