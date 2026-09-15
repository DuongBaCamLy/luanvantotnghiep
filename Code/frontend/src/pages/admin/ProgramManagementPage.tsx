import { useMemo, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import {
  Archive,
  Copy,
  Eye,
  Filter,
  History,
  Map as MapIcon,
  Pencil,
  Plus,
  RotateCcw,
  Search,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { departmentApi } from "@/api/departmentApi"
import { programApi as progApi } from "@/api/programApi"
import { useAuthStore } from "@/store/authStore"
import { prefixFor } from "@/config/navConfig"
import type { Cohort, Program, ProgramArchiveValidation, ProgramCreditValidation, UpdateProgramRequest } from "@/types/admin"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  getSyllabusBasePath,
  setProgramContext,
} from "@/lib/programContext"
function getErrorMessage(error: unknown, fallback: string): string {
  if (
    typeof error === "object"
    && error !== null
    && "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: { message?: string }
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

export default function ProgramManagementPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()

  const user = useAuthStore((state) => state.user)
  const role = user?.role
  const roleBase = role ? prefixFor(role) : ""

  const isAdmin = role === "ADMIN"
  const isDean = role === "DEAN"
  const [open, setOpen] = useState(false)
  const [cohortFilter, setCohortFilter] = useState("all")
  const [majorFilter, setMajorFilter] = useState("all")
  const [statusFilter, setStatusFilter] = useState("all")
  const [searchTerm, setSearchTerm] = useState("")
  const [syllabiProgram, setSyllabiProgram] = useState<Program | null>(null)
  const [syllabiCohortId, setSyllabiCohortId] = useState("")
  const [cohortAction, setCohortAction] = useState<"syllabi" | "map" | "archive">("syllabi")
  const [cohortProgram, setCohortProgram] = useState<Program | null>(null)
  const [cohortEntryYear, setCohortEntryYear] = useState(String(new Date().getFullYear()))

  const [selectedProgram, setSelectedProgram] = useState<Program | null>(null)
  const [creditDialogOpen, setCreditDialogOpen] = useState(false)
  const [creditCohortId, setCreditCohortId] = useState("")
  const [creditResult, setCreditResult] = useState<ProgramCreditValidation | null>(null)

  const [cloneDialogOpen, setCloneDialogOpen] = useState(false)
  const [sourceCohortId, setSourceCohortId] = useState("")
  const [targetCohortId, setTargetCohortId] = useState("")
  const [overwriteExisting, setOverwriteExisting] = useState(false)
  const [editProgram, setEditProgram] = useState<Program | null>(null)
  const [statusProgram, setStatusProgram] = useState<Program | null>(null)
  const [archiveValidation, setArchiveValidation] = useState<ProgramArchiveValidation | null>(null)
  const [metadata, setMetadata] = useState<UpdateProgramRequest | null>(null)
  const [notice, setNotice] = useState<{ type: "success" | "error"; message: string } | null>(null)

  // Form Fields
  const [code, setCode] = useState("")
  const [name, setName] = useState("")
  const [nameVn, setNameVn] = useState("")
  const [majorId, setMajorId] = useState("")
  const [programTypeId, setProgramTypeId] = useState("")
  const [departmentId, setDepartmentId] = useState("")
  const [accreditationBody, setAccreditationBody] = useState("")
  const [totalCredits, setTotalCredits] = useState("135")
  const [durationYears, setDurationYears] = useState("4")
  const [validFrom, setValidFrom] = useState("2021-09-01")

  const { data: programs, isLoading, isError } = useQuery({
    queryKey: ["programs"],
    queryFn: progApi.getAll,
  })

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: departmentApi.getAll,
    enabled: isAdmin,
  })

  const { data: majors } = useQuery({
    queryKey: ["majors"],
    queryFn: progApi.getMajors,
  })

  const { data: programTypes } = useQuery({
    queryKey: ["program-types"],
    queryFn: progApi.getProgramTypes,
    enabled: isAdmin,
  })
  const { data: cohorts = [] } = useQuery<Cohort[]>({ queryKey: ["cohorts"], queryFn: cohortApi.getAll })

  const cohortsForSelectedProgram = useMemo(() => {
    if (!selectedProgram) return []
    return cohorts.filter((c) => c.programId === selectedProgram.id)
  }, [cohorts, selectedProgram])

  const cohortFilterOptions = useMemo(() => {
    return cohorts
      .slice()
      .sort((a, b) => {
        if (a.entryYear !== b.entryYear) {
          return b.entryYear - a.entryYear
        }

        return a.name.localeCompare(b.name, "en", { numeric: true })
      })
  }, [cohorts])

  const cohortsByProgramId = useMemo(() => {
    const map = new Map<number, Cohort[]>()

    cohorts.forEach((cohort) => {
      const current = map.get(cohort.programId) ?? []
      current.push(cohort)
      map.set(cohort.programId, current)
    })

    map.forEach((items) => {
      items.sort((a, b) => b.entryYear - a.entryYear)
    })

    return map
  }, [cohorts])

  const majorFilterOptions = useMemo(() => {
    return (majors ?? [])
      .slice()
      .sort((a, b) =>
        a.code.localeCompare(b.code, "en", { numeric: true })
      )
  }, [majors])

  const filteredPrograms = useMemo(() => {
    const normalizedSearch = searchTerm.trim().toLowerCase()

    return (programs ?? []).filter((program) => {
      const matchesMajor =
        majorFilter === "all"
        || String(program.majorId) === majorFilter

      const matchesCohort =
        cohortFilter === "all"
        || (cohortsByProgramId.get(program.id) ?? []).some(
          (cohort) => String(cohort.id) === cohortFilter
        )

      const matchesStatus =
        statusFilter === "all"
        || (statusFilter === "active" && program.isActive)
        || (statusFilter === "archived" && !program.isActive)

      const searchableText = [
        program.code,
        program.name,
        program.nameVn,
        program.majorCode,
        program.departmentCode,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()

      const matchesSearch =
        !normalizedSearch
        || searchableText.includes(normalizedSearch)

      return (
        matchesMajor
        && matchesCohort
        && matchesStatus
        && matchesSearch
      )
    })
  }, [
    programs,
    cohortFilter,
    majorFilter,
    statusFilter,
    searchTerm,
    cohortsByProgramId,
  ])

  const displayProgramCode = (program: Program) => {
    const stripped = String(program.code ?? "")
      .trim()
      .replace(/[-_\s]?20\d{2}$/i, "")
      .replace(/[-_\s]+$/g, "")
      .trim()

    return stripped || program.code
  }

  const resetFilters = () => {
    setSearchTerm("")
    setMajorFilter("all")
    setCohortFilter("all")
    setStatusFilter("all")
  }

  const goToSyllabusCatalog = (program: Program, cohort: Cohort) => {
    const params = new URLSearchParams()

    setProgramContext(params, program, cohort)

    navigate({
      pathname: getSyllabusBasePath(location.pathname),
      search: params.toString(),
    })
  }

  const openSyllabusCatalog = (program: Program) => {
    const programCohorts = cohortsByProgramId.get(program.id) ?? []
    if (programCohorts.length === 1) {
      goToSyllabusCatalog(program, programCohorts[0])
      return
    }

    setCohortAction("syllabi")
    setSyllabiProgram(program)
    setSyllabiCohortId("")
  }

  const goToCurriculumMap = (program: Program, cohort: Cohort) => {
    const params = new URLSearchParams()
    setProgramContext(params, program, cohort)
    const roleBase = `/${location.pathname.split("/")[1]}`
    navigate({
      pathname: `${roleBase}/syllabus/curriculum-map`,
      search: params.toString(),
    })
  }

  const openCurriculumMap = (program: Program) => {
    const programCohorts = (cohortsByProgramId.get(program.id) ?? [])
      .filter((cohort) => cohort.isActive !== false)
    if (programCohorts.length === 1) {
      goToCurriculumMap(program, programCohorts[0])
      return
    }
    setCohortAction("map")
    setSyllabiProgram(program)
    setSyllabiCohortId("")
  }

  const openCohortAction = (
    program: Program,
    action: "archive",
  ) => {
    setCohortAction(action)
    setSyllabiProgram(program)
    setSyllabiCohortId("")
  }

  const openProgramEditor = (program: Program) => {
    setEditProgram(program)
    setMetadata({
      name: program.name,
      nameVn: program.nameVn,
      majorId: program.majorId,
      programTypeId: program.programTypeId,
      departmentId: program.departmentId,
      accreditationBody: program.accreditationBody,
      totalCredits: program.totalCredits,
      durationYears: program.durationYears,
      validFrom: program.validFrom,
      validTo: program.validTo,
    })
  }

  const createMutation = useMutation({
    mutationFn: progApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["programs"] })
      setOpen(false)
      setCode("")
      setName("")
      setNameVn("")
      setMajorId("")
      setProgramTypeId("")
      setDepartmentId("")
      setAccreditationBody("")
      setTotalCredits("135")
      setDurationYears("4")
      setValidFrom("2021-09-01")
    },
    onError: (error: unknown) => {
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to create the curriculum program.",
        ),
      })
    },
  })

  const validateCreditsMutation = useMutation({
    mutationFn: ({ programId, cohortId }: { programId: number; cohortId: number }) =>
      progApi.validateCredits(programId, cohortId),
    onSuccess: (data) => setCreditResult(data),
    onError: (error: unknown) => {
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to validate curriculum credits.",
        ),
      })
    },
  })

  const cloneMutation = useMutation({
    mutationFn: ({ programId }: { programId: number }) =>
      progApi.cloneToCohort(programId, {
        sourceCohortId: Number(sourceCohortId),
        targetCohortId: Number(targetCohortId),
        overwriteExisting,
      }),
    onSuccess: (data) => {
      alert(data.message)
      setCloneDialogOpen(false)
      setSourceCohortId("")
      setTargetCohortId("")
      setOverwriteExisting(false)
      queryClient.invalidateQueries({ queryKey: ["course-programs"] })
    },
    onError: (error: unknown) => {
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to clone the curriculum to the target cohort.",
        ),
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProgramRequest }) => progApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["programs"] })
      setEditProgram(null)
      setMetadata(null)
      setNotice({ type: "success", message: "Curriculum program metadata has been updated." })
    },
    onError: (error: unknown) =>
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to update curriculum program metadata.",
        ),
      }),
  })

  const archiveMutation = useMutation({
    mutationFn: (id: number) => progApi.archive(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["programs"] })
      setStatusProgram(null)
      setArchiveValidation(null)
      setNotice({ type: "success", message: "The curriculum program has been archived. No related data was deleted." })
    },
    onError: (error: unknown) =>
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to archive the curriculum program.",
        ),
      }),
  })

  const reactivateMutation = useMutation({
    mutationFn: (id: number) => progApi.reactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["programs"] })
      setStatusProgram(null)
      setNotice({ type: "success", message: "The curriculum program has been reactivated. All existing data has been preserved." })
    },
    onError: (error: unknown) =>
      setNotice({
        type: "error",
        message: getErrorMessage(
          error,
          "Unable to reactivate the curriculum program.",
        ),
      }),
  })

  const cohortStatusMutation = useMutation({
    mutationFn: (cohort: Cohort) =>
      cohort.isActive ? cohortApi.archive(cohort.id) : cohortApi.reactivate(cohort.id),
    onSuccess: (cohort) => {
      queryClient.invalidateQueries({ queryKey: ["cohorts"] })
      queryClient.invalidateQueries({ queryKey: ["course-programs"] })
      setSyllabiProgram(null)
      setSyllabiCohortId("")
      setNotice({
        type: "success",
        message: `${cohort.name} has been ${cohort.isActive ? "reactivated" : "archived"}. The Program and curriculum data were preserved.`,
      })
    },
    onError: (error: unknown) => setNotice({
      type: "error",
      message: getErrorMessage(error, "Unable to update the selected cohort status."),
    }),
  })

  const createCohortMutation = useMutation({
    mutationFn: ({ programId, entryYear }: { programId: number; entryYear: number }) =>
      cohortApi.create({ programId, entryYear }),
    onSuccess: (cohort) => {
      queryClient.invalidateQueries({ queryKey: ["cohorts"] })
      setCohortProgram(null)
      setNotice({ type: "success", message: `${cohort.name} has been added.` })
    },
    onError: (error: unknown) => setNotice({
      type: "error",
      message: getErrorMessage(error, "Unable to add the cohort. Check that the entry year is not duplicated."),
    }),
  })

  const openCloneDialog = (program: Program) => {
    setSelectedProgram(program)
    setSourceCohortId("")
    setTargetCohortId("")
    setOverwriteExisting(false)
    setCloneDialogOpen(true)
  }

  const goToProgramDiff = (program: Program) => {
    const basePath = location.pathname.includes("/programs")
      ? location.pathname.slice(0, location.pathname.indexOf("/programs"))
      : "/admin"

    navigate(`${basePath}/programs/${program.id}/diff`)
  }
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (
      !code ||
      !name ||
      !nameVn ||
      !majorId ||
      !programTypeId ||
      !departmentId ||
      !totalCredits ||
      !durationYears ||
      !validFrom
    ) {
      alert("Please complete all required fields")
      return
    }

    createMutation.mutate({
      code,
      name,
      nameVn,
      majorId: Number(majorId),
      programTypeId: Number(programTypeId),
      departmentId: Number(departmentId),
      accreditationBody: accreditationBody || "ASIIN",
      totalCredits: Number(totalCredits),
      durationYears: Number(durationYears),
      validFrom,
    })
  }

  return (
    <div data-admin-page="ProgramManagementPage" className="bg-[#FDFDF9] min-h-screen -m-6 p-6 md:-m-10 md:p-10 space-y-6">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-[0_10px_28px_rgba(0,86,94,0.07)]">
        <div className="absolute inset-x-0 top-0 h-[4px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="pointer-events-none absolute -right-20 -top-24 h-56 w-56 rounded-full bg-[#eaf6f6]" />
        <div className="pointer-events-none absolute right-28 top-16 h-20 w-20 rounded-full bg-[#fff4df]" />

        <div className="relative flex flex-col gap-5 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 items-start gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#007d84] shadow-sm">
              <span className="text-xl font-bold text-white">
                CP
              </span>
            </div>

            <div data-admin-page-header="ProgramManagementPage" className="min-w-0">
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <span className="rounded-full border border-[#cbe1e3] bg-[#eef8f8] px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.9px] text-[#007d84]">
                  SCSE
                </span>

                <span className="text-[10px] font-semibold uppercase tracking-[0.8px] text-slate-400">
                  {isDean
                    ? "Curriculum Oversight"
                    : "Curriculum Program Management"}
                </span>
              </div>

              <h1 className="font-heading text-[28px] font-bold tracking-[-0.6px] text-[#17343d] md:text-[31px]">
                Curriculum Programs
              </h1>

              <p className="mt-1.5 max-w-3xl text-[13px] leading-5 text-[#657b85]">
                {isDean
                  ? "Review curriculum metadata, cohort versions, credit requirements, semester plans, curriculum comparisons, and change history across the School."
                  : "Create and manage curriculum programs, maintain curriculum structures, validate credits, clone cohort curricula, and review curriculum changes."}
              </p>

              <div className="mt-4 flex flex-wrap items-center gap-2">
                <span className="rounded-md bg-[#edf7f7] px-2.5 py-1 text-[11px] font-medium text-[#006f76]">
                  Program & Cohort
                </span>

                <span className="rounded-md bg-[#f5f7f8] px-2.5 py-1 text-[11px] font-medium text-[#536b75]">
                  Credit Validation
                </span>

                <span className="rounded-md bg-[#fff5e7] px-2.5 py-1 text-[11px] font-medium text-[#b56b00]">
                  Curriculum Comparison
                </span>
              </div>
            </div>
          </div>

          {isAdmin && (
            <div className="flex shrink-0 lg:self-start">
              <Button
                className="h-10 gap-2 rounded-md bg-[#007d84] px-4 text-[12px] font-semibold text-white shadow-sm hover:bg-[#006c72]"
                onClick={() =>
                  navigate("/admin/curricula/create")
                }
              >
                <Plus className="size-4" />
                Add Curriculum Program
              </Button>
            </div>
          )}
        </div>
      </section>

      {notice && (
        <div role="status" className={`fixed right-5 top-5 z-50 max-w-md rounded-lg px-4 py-3 text-sm shadow-lg ${notice.type === "success" ? "bg-emerald-600 text-white" : "bg-rose-600 text-white"}`}>
          <div className="flex items-center gap-3"><span>{notice.message}</span><button type="button" className="font-bold" onClick={() => setNotice(null)} aria-label="Close notification">×</button></div>
        </div>
      )}

      <div className="rounded-xl border border-slate-200/80 bg-white p-5 shadow-sm">
        <div className="mb-4 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-orange-600">
          <Filter className="size-4" />
          Filters
        </div>

        <div data-admin-filter className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-[minmax(260px,1.4fr)_1fr_1fr_1fr_auto] xl:items-end">
          <div className="space-y-1.5">
            <Label htmlFor="program-search">Search</Label>
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
              <Input
                id="program-search"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Program code or name..."
                className="h-10 pl-9"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>Major</Label>
            <Select value={majorFilter} onValueChange={setMajorFilter}>
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="All Majors" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Majors</SelectItem>
                {majorFilterOptions.map((major) => (
                  <SelectItem key={major.id} value={String(major.id)}>
                    {major.code} — {major.name || major.nameVn}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>Cohort</Label>
            <Select value={cohortFilter} onValueChange={setCohortFilter}>
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="All Cohorts" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Cohorts</SelectItem>
                {cohortFilterOptions.map((cohort) => (
                  <SelectItem key={cohort.id} value={String(cohort.id)}>
                    {cohort.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label>Status</Label>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                <SelectItem value="active">Active</SelectItem>
                <SelectItem value="archived">Archived</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            variant="outline"
            className="h-10"
            onClick={resetFilters}
            disabled={
              !searchTerm
              && majorFilter === "all"
              && cohortFilter === "all"
              && statusFilter === "all"
            }
          >
            <RotateCcw className="size-4" />
            Clear
          </Button>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          Showing <span className="font-semibold text-slate-700">{filteredPrograms.length}</span>
          {" "}of{" "}
          <span className="font-semibold text-slate-700">{programs?.length ?? 0}</span>
          {" "}curriculum programs.
        </p>
      </div>

      <div className="overflow-hidden rounded-xl border border-slate-200/80 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <Table className="min-w-[1180px]">
            <TableHeader>
              <TableRow className="bg-slate-50/80">
                <TableHead className="min-w-[300px] font-semibold text-slate-600">
                  Program
                </TableHead>

                <TableHead className="w-[90px] font-semibold text-slate-600">
                  Major
                </TableHead>

                <TableHead className="w-[100px] font-semibold text-slate-600">
                  Department
                </TableHead>

                <TableHead className="min-w-[180px] font-semibold text-slate-600">
                  Cohort
                </TableHead>

                <TableHead className="w-[110px] font-semibold text-slate-600">
                  Accreditation
                </TableHead>

                <TableHead className="w-[90px] text-center font-semibold text-slate-600">
                  Credits
                </TableHead>

                <TableHead className="w-[90px] text-center font-semibold text-slate-600">
                  Duration
                </TableHead>

                <TableHead className="w-[110px] font-semibold text-slate-600">
                  Status
                </TableHead>

                <TableHead className="min-w-[420px] font-semibold text-slate-600">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="py-10 text-center text-slate-400"
                  >
                    Loading...
                  </TableCell>
                </TableRow>
              ) : isError ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="py-10 text-center text-rose-500"
                  >
                    Unable to load curriculum program data.
                  </TableCell>
                </TableRow>
              ) : filteredPrograms.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="py-10 text-center text-slate-400"
                  >
                    No curriculum programs match the selected filters.
                  </TableCell>
                </TableRow>
              ) : (
                filteredPrograms.map((prog) => {
                  const programCohorts =
                    cohortsByProgramId.get(prog.id) ?? []

                  const visibleCohorts =
                    programCohorts.slice(0, 2)

                  const remainingCohorts =
                    programCohorts.length - visibleCohorts.length

                  return (
                    <TableRow
                      key={prog.id}
                      className="align-top hover:bg-slate-50/70"
                    >
                      <TableCell>
                        <div className="flex flex-col">
                          <span className="font-mono text-sm font-bold text-[#007d84]">
                            {displayProgramCode(prog)}
                          </span>

                          <span className="mt-1 font-semibold text-slate-900">
                            {prog.nameVn || prog.name}
                          </span>

                          <span className="mt-0.5 text-xs leading-5 text-slate-500">
                            {prog.name}
                          </span>

                        </div>
                      </TableCell>

                      <TableCell className="font-semibold text-slate-700">
                        {prog.majorCode}
                      </TableCell>

                      <TableCell className="text-slate-600">
                        {prog.departmentCode}
                      </TableCell>

                      <TableCell>
                        <div className="flex flex-wrap items-center gap-1">
                          {programCohorts.length === 0 ? (
                            <span className="text-xs text-slate-400">No cohorts</span>
                          ) : (
                            <>
                            {visibleCohorts.map((cohort) => (
                              <Badge
                                key={cohort.id}
                                variant="outline"
                                className="border-slate-200 bg-slate-50 text-slate-600"
                              >
                                {cohort.name}
                              </Badge>
                            ))}

                            {remainingCohorts > 0 && (
                              <Badge
                                variant="outline"
                                className="border-slate-200 bg-white text-slate-500"
                              >
                                +{remainingCohorts}
                              </Badge>
                            )}
                            </>
                          )}
                          {isAdmin && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="h-6 rounded-full px-2 text-[11px] font-semibold text-[#007d84] hover:bg-[#eaf6f6]"
                              onClick={() => {
                                setCohortProgram(prog)
                                setCohortEntryYear(String(new Date().getFullYear()))
                              }}
                            >
                              <Plus className="size-3" /> Cohort
                            </Button>
                          )}
                        </div>
                      </TableCell>

                      <TableCell>
                        {prog.accreditationBody ? (
                          <Badge
                            variant="outline"
                            className="border-indigo-200 bg-indigo-50/30 text-indigo-600"
                          >
                            {prog.accreditationBody}
                          </Badge>
                        ) : (
                          <span className="text-xs text-slate-400">
                            Not specified
                          </span>
                        )}
                      </TableCell>

                      <TableCell className="text-center font-bold text-slate-800">
                        {prog.totalCredits}
                      </TableCell>

                      <TableCell className="text-center text-slate-600">
                        {prog.durationYears} years
                      </TableCell>

                      <TableCell>
                        {prog.isActive ? (
                          <Badge className="border border-emerald-200 bg-emerald-50 text-emerald-700">
                            Active
                          </Badge>
                        ) : (
                          <Badge className="border border-slate-200 bg-slate-100 text-slate-500">
                            Archived
                          </Badge>
                        )}
                      </TableCell>

                      <TableCell>
                        <div className="flex flex-wrap gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              openSyllabusCatalog(prog)
                            }
                          >
                            <Eye className="size-3.5" />
                            View Syllabi
                          </Button>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`${roleBase}/programs/${prog.id}/curriculum`)}
                          >
                            <MapIcon className="size-3.5" />
                            {isAdmin ? "Manage Curriculum" : "View Curriculum"}
                          </Button>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => openCurriculumMap(prog)}
                          >
                            <MapIcon className="size-3.5" />
                            Curriculum Map
                          </Button>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              goToProgramDiff(prog)
                            }
                          >
                            <History className="size-3.5" />
                            Compare Program
                          </Button>

                          {isAdmin && (
                            <>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => openProgramEditor(prog)}
                              >
                                <Pencil className="size-3.5" />
                                Edit
                              </Button>

                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() =>
                                  openCloneDialog(prog)
                                }
                              >
                                <Copy className="size-3.5" />
                                Clone
                              </Button>

                              <Button
                                type="button"
                                variant={
                                  prog.isActive
                                    ? "outline"
                                    : "default"
                                }
                                size="sm"
                                onClick={() =>
                                  openCohortAction(prog, "archive")
                                }
                                className="border-amber-300 text-amber-700 hover:bg-amber-50"
                              >
                                <Archive className="size-3.5" />
                                Archive
                              </Button>
                            </>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog open={creditDialogOpen} onOpenChange={setCreditDialogOpen}>
        <DialogContent className="sm:max-w-3xl bg-white max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Validate Curriculum Credits</DialogTitle>
            <DialogDescription>
              Select a cohort to calculate the curriculum credits and compare them with the required total credits.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid grid-cols-[1fr_auto] gap-3 items-end">
              <div className="flex flex-col gap-1.5">
                <Label>Cohort</Label>
                <Select value={creditCohortId} onValueChange={setCreditCohortId}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select a cohort..." />
                  </SelectTrigger>
                  <SelectContent>
                    {cohortsForSelectedProgram.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name} — {c.entryYear}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <Button
                disabled={!selectedProgram || !creditCohortId || validateCreditsMutation.isPending}
                onClick={() => selectedProgram && validateCreditsMutation.mutate({
                  programId: selectedProgram.id,
                  cohortId: Number(creditCohortId),
                })}
              >
                {validateCreditsMutation.isPending ? "Validating..." : "Validate"}
              </Button>
            </div>

            {cohortsForSelectedProgram.length === 0 && (
              <Alert>
                <AlertTitle>No Cohorts Available</AlertTitle>
                <AlertDescription>
                  {isDean
                    ? "No cohort is available for this curriculum program. Please contact an Administrator to configure the cohort."
                    : "Create a cohort before validating the total credits."}
                </AlertDescription>
              </Alert>
            )}

            {creditResult && (
              <div className="space-y-4">
                <Alert className={creditResult.valid ? "border-emerald-200 bg-emerald-50" : "border-amber-200 bg-amber-50"}>
                  <AlertTitle>{creditResult.valid ? "Credits Valid" : "Credit Requirement Not Met"}</AlertTitle>
                  <AlertDescription>{creditResult.message}</AlertDescription>
                </Alert>

                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
                  <div className="rounded-lg border p-3">
                    <p className="text-xs text-slate-500">Required</p>
                    <p className="text-xl font-bold">{creditResult.expectedTotalCredits ?? 0} credits</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-xs text-slate-500">Current</p>
                    <p className="text-xl font-bold">{creditResult.actualTotalCredits} credits</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-xs text-slate-500">Required Courses</p>
                    <p className="text-xl font-bold">{creditResult.requiredCredits} credits</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-xs text-slate-500">Elective / Other</p>
                    <p className="text-xl font-bold">{creditResult.electiveCredits} credits</p>
                  </div>
                </div>

                <div className="rounded-lg border overflow-hidden">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Course Code</TableHead>
                        <TableHead>Course Name</TableHead>
                        <TableHead>Type</TableHead>
                        <TableHead className="text-center">Semester</TableHead>
                        <TableHead className="text-center">Theory</TableHead>
                        <TableHead className="text-center">Lab</TableHead>
                        <TableHead className="text-center">Total</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {creditResult.courses.map((course) => (
                        <TableRow key={course.courseProgramId}>
                          <TableCell className="font-mono font-medium">{course.courseCode}</TableCell>
                          <TableCell>{course.courseName}</TableCell>
                          <TableCell>{course.courseTypeName ?? "-"}</TableCell>
                          <TableCell className="text-center">{course.semesterSuggest ?? "-"}</TableCell>
                          <TableCell className="text-center">{course.creditTheory ?? 0}</TableCell>
                          <TableCell className="text-center">{course.creditLab ?? 0}</TableCell>
                          <TableCell className="text-center font-bold">{course.totalCredits}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {isAdmin && (
      <Dialog open={cloneDialogOpen} onOpenChange={setCloneDialogOpen}>
        <DialogContent className="sm:max-w-xl bg-white">
          <DialogHeader>
            <DialogTitle className="text-xl text-[#006f76]">Clone Cohort</DialogTitle>
            <DialogDescription>{selectedProgram?.code}</DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label>From</Label>
                <Select value={sourceCohortId} onValueChange={setSourceCohortId}>
                  <SelectTrigger><SelectValue placeholder="Select source..." /></SelectTrigger>
                  <SelectContent>
                    {cohortsForSelectedProgram.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.name} — {c.entryYear}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label>To</Label>
                <Select value={targetCohortId} onValueChange={setTargetCohortId}>
                  <SelectTrigger><SelectValue placeholder="Select target..." /></SelectTrigger>
                  <SelectContent>
                    {cohortsForSelectedProgram.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.name} — {c.entryYear}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <label className="flex items-start gap-2 rounded-lg border p-3 text-sm">
              <input
                type="checkbox"
                checked={overwriteExisting}
                onChange={(e) => setOverwriteExisting(e.target.checked)}
                className="mt-1"
              />
              <span>
                <span className="block font-semibold">Replace existing curriculum</span>
                <span className="text-slate-500">Clear the target before cloning.</span>
              </span>
            </label>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setCloneDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={
                !selectedProgram ||
                !sourceCohortId ||
                !targetCohortId ||
                sourceCohortId === targetCohortId ||
                cloneMutation.isPending
              }
              onClick={() => selectedProgram && cloneMutation.mutate({ programId: selectedProgram.id })}
            >
              {cloneMutation.isPending ? "Cloning..." : "Clone"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      )}

      {isAdmin && (
      <Dialog open={Boolean(editProgram)} onOpenChange={(open) => !open && setEditProgram(null)}>
        <DialogContent className="sm:max-w-2xl bg-white max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Curriculum Program Metadata</DialogTitle>
            <DialogDescription>The program code cannot be changed. Other information will be updated after saving.</DialogDescription>
          </DialogHeader>
          {editProgram && metadata && (
            <form onSubmit={(event) => { event.preventDefault(); updateMutation.mutate({ id: editProgram.id, data: metadata }) }} className="space-y-4">
              <div className="rounded-md bg-slate-50 px-3 py-2 text-sm"><span className="text-slate-500">Program Code: </span><span className="font-mono font-semibold">{editProgram.code}</span></div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-1.5"><Label>Vietnamese Name</Label><Input value={metadata.nameVn} onChange={(e) => setMetadata({ ...metadata, nameVn: e.target.value })} /></div>
                <div className="space-y-1.5"><Label>English Name</Label><Input required value={metadata.name} onChange={(e) => setMetadata({ ...metadata, name: e.target.value })} /></div>
                <div className="space-y-1.5"><Label>Major</Label><Select value={String(metadata.majorId)} onValueChange={(value) => setMetadata({ ...metadata, majorId: Number(value) })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>{majors?.map((major) => <SelectItem key={major.id} value={String(major.id)}>{major.code} - {major.nameVn}</SelectItem>)}</SelectContent></Select></div>
                <div className="space-y-1.5"><Label>Program Type</Label><Select value={String(metadata.programTypeId)} onValueChange={(value) => setMetadata({ ...metadata, programTypeId: Number(value) })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>{programTypes?.map((type) => <SelectItem key={type.id} value={String(type.id)}>{type.code} - {type.name}</SelectItem>)}</SelectContent></Select></div>
                <div className="space-y-1.5"><Label>Responsible Department</Label><Select value={String(metadata.departmentId)} onValueChange={(value) => setMetadata({ ...metadata, departmentId: Number(value) })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>{departments?.map((department) => <SelectItem key={department.id} value={String(department.id)}>{department.code} - {department.nameVn}</SelectItem>)}</SelectContent></Select></div>
                <div className="space-y-1.5"><Label>Total Credits</Label><Input required min={1} type="number" value={metadata.totalCredits} onChange={(e) => setMetadata({ ...metadata, totalCredits: Number(e.target.value) })} /></div>
                <div className="space-y-1.5"><Label>Duration (Years)</Label><Input required min={1} type="number" value={metadata.durationYears} onChange={(e) => setMetadata({ ...metadata, durationYears: Number(e.target.value) })} /></div>
                <div className="space-y-1.5"><Label>Accreditation Standard</Label><Input value={metadata.accreditationBody || ""} onChange={(e) => setMetadata({ ...metadata, accreditationBody: e.target.value || null })} /></div>
                <div className="space-y-1.5"><Label>Effective Date</Label><Input required type="date" value={metadata.validFrom} onChange={(e) => setMetadata({ ...metadata, validFrom: e.target.value })} /></div>
                <div className="space-y-1.5"><Label>Expiration Date</Label><Input type="date" value={metadata.validTo || ""} onChange={(e) => setMetadata({ ...metadata, validTo: e.target.value || null })} /></div>
              </div>
              <DialogFooter><Button type="button" variant="outline" onClick={() => setEditProgram(null)}>Cancel</Button><Button type="submit" disabled={updateMutation.isPending}>{updateMutation.isPending ? "Saving..." : "Save Metadata"}</Button></DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      )}

      {isAdmin && (
      <Dialog open={Boolean(statusProgram)} onOpenChange={(open) => !open && setStatusProgram(null)}>
        <DialogContent className="sm:max-w-lg bg-white">
          <DialogHeader>
            <DialogTitle>{statusProgram?.isActive ? "Archive Curriculum Program" : "Reactivate Curriculum Program"}</DialogTitle>
            <DialogDescription>{statusProgram?.isActive ? "This action only archives the program. Cohorts, courses, syllabi, and related data will not be deleted." : "The curriculum program and all existing data will be reactivated without data loss."}</DialogDescription>
          </DialogHeader>
          {statusProgram?.isActive && !archiveValidation && <p className="text-sm text-slate-500">Checking archive conditions...</p>}
          {statusProgram?.isActive && archiveValidation && (
            archiveValidation.canArchive ? <Alert className="border-emerald-200 bg-emerald-50"><AlertTitle>Ready to Archive</AlertTitle><AlertDescription>Program metadata and cohort credit requirements are valid.</AlertDescription></Alert> :
              <Alert className="border-rose-200 bg-rose-50"><AlertTitle>Cannot Archive</AlertTitle><AlertDescription><ul className="mt-2 list-disc pl-5">{archiveValidation.violations.map((violation) => <li key={violation}>{violation}</li>)}</ul></AlertDescription></Alert>
          )}
          <DialogFooter><Button type="button" variant="outline" onClick={() => setStatusProgram(null)}>Cancel</Button>{statusProgram && (statusProgram.isActive ? <Button className="bg-amber-600 hover:bg-amber-700" disabled={!archiveValidation?.canArchive || archiveMutation.isPending} onClick={() => archiveMutation.mutate(statusProgram.id)}>{archiveMutation.isPending ? "Archiving..." : "Confirm Archive"}</Button> : <Button disabled={reactivateMutation.isPending} onClick={() => reactivateMutation.mutate(statusProgram.id)}>{reactivateMutation.isPending ? "Reactivating..." : "Confirm Reactivation"}</Button>)}</DialogFooter>
        </DialogContent>
      </Dialog>

      )}

      {isAdmin && (
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-xl bg-white max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-primary font-heading">Add New Curriculum Program</DialogTitle>
            <DialogDescription>
              Create a new curriculum framework for an academic major.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 pt-2">
            <div className="grid grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prog-code">Program Code</Label>
                <Input
                  id="prog-code"
                  placeholder="e.g. CS-2021, IT-2021..."
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prog-year">Effective Date</Label>
                <Input
                  id="prog-year"
                  type="date"
                  value={validFrom}
                  onChange={(e) => setValidFrom(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="prog-name-vn">Curriculum Program Name (Vietnamese)</Label>
              <Input
                id="prog-name-vn"
                placeholder="Example: Bachelor program name in Vietnamese..."
                value={nameVn}
                onChange={(e) => setNameVn(e.target.value)}
                required
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="prog-name-en">English Name</Label>
              <Input
                id="prog-name-en"
                placeholder="e.g. Bachelor of Science in Computer Science..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label>Major</Label>
                <Select value={majorId} onValueChange={setMajorId}>
                  <SelectTrigger className="bg-white border-slate-200">
                    <SelectValue placeholder="Select major..." />
                  </SelectTrigger>
                  <SelectContent>
                    {majors?.map((m) => (
                      <SelectItem key={m.id} value={String(m.id)}>
                        {m.code} - {m.nameVn}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label>Program Type</Label>
                <Select value={programTypeId} onValueChange={setProgramTypeId}>
                  <SelectTrigger className="bg-white border-slate-200">
                    <SelectValue placeholder="Select program type..." />
                  </SelectTrigger>
                  <SelectContent>
                    {programTypes?.map((pt) => (
                      <SelectItem key={pt.id} value={String(pt.id)}>
                        {pt.code} - {pt.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="flex flex-col gap-1.5">
                <Label>Responsible Department</Label>
                <Select value={departmentId} onValueChange={setDepartmentId}>
                  <SelectTrigger className="bg-white border-slate-200">
                    <SelectValue placeholder="Select department..." />
                  </SelectTrigger>
                  <SelectContent>
                    {departments?.map((d) => (
                      <SelectItem key={d.id} value={String(d.id)}>
                        {d.code} - {d.nameVn}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prog-credits">Total Credits</Label>
                <Input
                  id="prog-credits"
                  type="number"
                  value={totalCredits}
                  onChange={(e) => setTotalCredits(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prog-duration">Duration (Years)</Label>
                <Input
                  id="prog-duration"
                  type="number"
                  value={durationYears}
                  onChange={(e) => setDurationYears(e.target.value)}
                  required
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <Label htmlFor="prog-accreditation">Accreditation Standard</Label>
                <Input
                  id="prog-accreditation"
                  placeholder="e.g. ASIIN, ABET..."
                  value={accreditationBody}
                  onChange={(e) => setAccreditationBody(e.target.value)}
                />
              </div>
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" className="bg-primary text-white hover:bg-primary/90" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Saving..." : "Create Program"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>      )}

      <Dialog
        open={cohortProgram !== null}
        onOpenChange={(isOpen) => !isOpen && setCohortProgram(null)}
      >
        <DialogContent className="overflow-hidden border-[#cfe2e4] bg-white p-0 sm:max-w-sm">
          <div className="h-1 bg-gradient-to-r from-[#007d84] via-[#20a0a5] to-[#f0a72f]" />
          <div className="space-y-5 px-6 pb-2 pt-5">
            <DialogHeader className="space-y-1 text-left">
              <DialogTitle className="text-xl font-bold text-[#006f76]">Add Cohort</DialogTitle>
              <DialogDescription>
                {cohortProgram ? `${displayProgramCode(cohortProgram)} · ${cohortProgram.name}` : ""}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-2">
              <Label htmlFor="cohort-entry-year" className="text-[11px] font-bold uppercase tracking-[0.08em] text-slate-500">
                Entry year
              </Label>
              <Input
                id="cohort-entry-year"
                type="number"
                min={2000}
                max={2100}
                value={cohortEntryYear}
                onChange={(event) => setCohortEntryYear(event.target.value)}
                className="h-11 bg-[#f8fbfb]"
              />
              {cohortProgram && Number(cohortEntryYear) >= 2000 && Number(cohortEntryYear) <= 2100 && (
                <p className="text-xs text-slate-500">
                  Code: <span className="font-mono font-bold text-[#007d84]">{displayProgramCode(cohortProgram)}{cohortEntryYear}</span>
                </p>
              )}
            </div>
          </div>
          <DialogFooter className="border-t border-[#e2ecee] bg-[#f7fafb] px-6 py-4">
            <Button variant="ghost" onClick={() => setCohortProgram(null)}>Cancel</Button>
            <Button
              className="bg-[#007d84] text-white hover:bg-[#006b71]"
              disabled={
                !cohortProgram
                || Number(cohortEntryYear) < 2000
                || Number(cohortEntryYear) > 2100
                || (cohortsByProgramId.get(cohortProgram.id) ?? []).some((cohort) => cohort.entryYear === Number(cohortEntryYear))
                || createCohortMutation.isPending
              }
              onClick={() => cohortProgram && createCohortMutation.mutate({
                programId: cohortProgram.id,
                entryYear: Number(cohortEntryYear),
              })}
            >
              {createCohortMutation.isPending ? "Adding..." : "Add"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={syllabiProgram !== null}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            setSyllabiProgram(null)
            setSyllabiCohortId("")
          }
        }}
      >
        <DialogContent className="overflow-hidden border-[#cfe2e4] bg-white p-0 sm:max-w-md">
          <div className="h-1 bg-gradient-to-r from-[#007d84] via-[#20a0a5] to-[#f0a72f]" />
          <div className="space-y-5 px-6 pb-2 pt-5">
          <DialogHeader className="space-y-1 text-left">
            <DialogTitle className="text-xl font-bold tracking-tight text-[#006f76]">
              {cohortAction === "syllabi" && "View Syllabi"}
              {cohortAction === "map" && "View Curriculum Map"}
              {cohortAction === "archive" && "Cohort Status"}
            </DialogTitle>
            <DialogDescription className="text-sm">
              {syllabiProgram ? `${displayProgramCode(syllabiProgram)} · ${syllabiProgram.name}` : ""}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
          <Label className="text-[11px] font-bold uppercase tracking-[0.08em] text-slate-500">Cohort</Label>
          <Select value={syllabiCohortId} onValueChange={setSyllabiCohortId}>
            <SelectTrigger className="h-11 rounded-lg border-[#cbdde0] bg-[#f8fbfb] shadow-none focus:ring-[#007d84]">
              <SelectValue placeholder="Choose a cohort" />
            </SelectTrigger>
            <SelectContent>
              {(syllabiProgram ? cohortsByProgramId.get(syllabiProgram.id) ?? [] : [])
                .filter((cohort) => cohortAction !== "map" || cohort.isActive !== false)
                .map((cohort) => (
                <SelectItem key={cohort.id} value={String(cohort.id)}>
                  {cohort.name}{cohort.isActive === false ? " — Archived" : ""}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          </div>
          </div>
          <DialogFooter className="border-t border-[#e2ecee] bg-[#f7fafb] px-6 py-4 sm:justify-end">
            <Button className="h-9" variant="ghost" onClick={() => setSyllabiProgram(null)}>Cancel</Button>
            <Button
              className={cohortAction === "archive"
                ? "h-9 bg-amber-600 text-white hover:bg-amber-700"
                : "h-9 bg-[#007d84] text-white hover:bg-[#006b71]"}
              disabled={!syllabiProgram || !syllabiCohortId || cohortStatusMutation.isPending}
              onClick={() => {
                const cohort = (syllabiProgram ? cohortsByProgramId.get(syllabiProgram.id) ?? [] : []).find((item) => item.id === Number(syllabiCohortId))
                if (!syllabiProgram || !cohort) return
                if (cohortAction === "syllabi") {
                  goToSyllabusCatalog(syllabiProgram, cohort)
                } else if (cohortAction === "map") {
                  goToCurriculumMap(syllabiProgram, cohort)
                } else {
                  cohortStatusMutation.mutate(cohort)
                }
              }}
            >
              {cohortAction === "syllabi" && "View Syllabi"}
              {cohortAction === "map" && "View Map"}
              {cohortAction === "archive" && (() => {
                const cohort = (syllabiProgram ? cohortsByProgramId.get(syllabiProgram.id) ?? [] : []).find((item) => item.id === Number(syllabiCohortId))
                return cohort?.isActive ? "Archive" : "Reactivate"
              })()}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  )
}
