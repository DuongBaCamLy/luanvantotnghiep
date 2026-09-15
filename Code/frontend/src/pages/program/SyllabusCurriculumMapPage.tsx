import { useMemo } from "react"
import { useQuery } from "@tanstack/react-query"
import { ArrowLeft, GitBranch, Loader2, Printer, RefreshCw } from "lucide-react"
import { useNavigate, useSearchParams } from "react-router-dom"

import { cohortApi } from "@/api/cohortApi"
import { curriculumMapApi, type CurriculumMapCourse } from "@/api/curriculumMapApi"
import { programApi } from "@/api/programApi"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { cn } from "@/lib/utils"
import {
  formatSyllabusFilterStatus,
  readSyllabusFilter,
  SYLLABUS_CANONICAL_STATUSES,
  SYLLABUS_FILTER_ALL,
  SYLLABUS_SEMESTER_OPTIONS,
} from "@/lib/syllabusCatalogFilters"

const COLUMN_WIDTH = 190
const COLUMN_GAP = 32
const COLUMN_PITCH = COLUMN_WIDTH + COLUMN_GAP
const NODE_WIDTH = 178
const NODE_HEIGHT = 84
const NODE_TOP = 72
const NODE_PITCH = 108

function baseProgramCode(code?: string | null) {
  return String(code ?? "").trim().replace(/[-_\s]?20\d{2}$/i, "").replace(/[-_\s]+$/g, "")
}

const nodeId = (courseId: number) => `curriculum-course-${courseId}`

function relationLabel(type: string) {
  if (type === "COREQUISITE") return "Co-requisite"
  if (type === "RECOMMENDED") return "Previous / recommended"
  return "Prerequisite"
}

export default function SyllabusCurriculumMapPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const filter = useMemo(() => readSyllabusFilter(searchParams), [searchParams])
  const requestedProgramId = filter.programId
  const requestedCohortId = filter.cohortId

  const programsQuery = useQuery({ queryKey: ["programs"], queryFn: programApi.getAll })
  const majorsQuery = useQuery({ queryKey: ["majors"], queryFn: programApi.getMajors })
  const majors = useMemo(
    () => [...(majorsQuery.data ?? [])].sort((left, right) => left.code.localeCompare(right.code, "en", { numeric: true })),
    [majorsQuery.data],
  )
  const programs = useMemo(
    () => (programsQuery.data ?? []).filter((program) => program.isActive !== false),
    [programsQuery.data],
  )
  const selectedProgram = useMemo(
    () => programs.find((program) => program.id === requestedProgramId),
    [programs, requestedProgramId],
  )
  const selectedMajorCode = searchParams.get("majorCode")
    ?? selectedProgram?.majorCode
    ?? SYLLABUS_FILTER_ALL
  const eligiblePrograms = useMemo(
    () => selectedMajorCode === SYLLABUS_FILTER_ALL
      ? programs
      : programs.filter((program) => program.majorCode === selectedMajorCode),
    [programs, selectedMajorCode],
  )

  const cohortsQuery = useQuery({
    queryKey: ["cohorts"],
    queryFn: cohortApi.getAll,
  })
  const cohorts = useMemo(
    () => [...(cohortsQuery.data ?? [])]
      .filter((cohort) => cohort.isActive !== false && eligiblePrograms.some((program) => program.id === cohort.programId))
      .sort((a, b) => a.entryYear - b.entryYear),
    [cohortsQuery.data, eligiblePrograms],
  )
  const selectedCohort = useMemo(
    () => cohorts.find((cohort) => cohort.id === requestedCohortId),
    [cohorts, requestedCohortId],
  )

  const mapQuery = useQuery({
    queryKey: ["curriculum-map", filter.programId, filter.cohortId, filter.semester, filter.status],
    queryFn: () => curriculumMapApi.get(filter),
    enabled: Boolean(selectedProgram && selectedCohort),
  })

  const semesterMap = useMemo(() => {
    const result = new Map<string, CurriculumMapCourse[]>()
    for (const group of mapQuery.data?.semesters ?? []) result.set(group.semester, group.courses)
    return result
  }, [mapQuery.data])
  const visibleGroups = useMemo(
    () => [...semesterMap.keys()],
    [semesterMap],
  )
  const courseCount = useMemo(
    () => [...semesterMap.values()].reduce((total, courses) => total + courses.length, 0),
    [semesterMap],
  )
  const graphLayout = useMemo(() => {
    const positions = new Map<number, { x: number; y: number }>()
    let maximumRows = 1
    visibleGroups.forEach((semester, columnIndex) => {
      const courses = semesterMap.get(semester) ?? []
      maximumRows = Math.max(maximumRows, courses.length)
      courses.forEach((course, rowIndex) => positions.set(course.courseId, {
        x: columnIndex * COLUMN_PITCH + COLUMN_WIDTH / 2,
        y: NODE_TOP + rowIndex * NODE_PITCH + NODE_HEIGHT / 2,
      }))
    })
    return {
      positions,
      width: Math.max(COLUMN_WIDTH, visibleGroups.length * COLUMN_PITCH - COLUMN_GAP),
      height: NODE_TOP + maximumRows * NODE_PITCH + 40,
    }
  }, [semesterMap, visibleGroups])

  const setMajor = (value: string) => {
    const next = new URLSearchParams(searchParams)
    next.delete("programId")
    next.delete("programCode")
    next.delete("cohortId")
    if (value === SYLLABUS_FILTER_ALL) {
      next.delete("majorId")
      next.delete("majorCode")
    } else {
      const major = majors.find((item) => item.code === value)
      next.set("majorCode", value)
      if (major) next.set("majorId", String(major.id))
      const matchingPrograms = programs.filter((program) => program.majorCode === value)
      if (matchingPrograms.length === 1) {
        next.set("programId", String(matchingPrograms[0].id))
        next.set("programCode", matchingPrograms[0].code)
      }
    }
    setSearchParams(next)
  }
  const setCohort = (value: string) => {
    const next = new URLSearchParams(searchParams)
    next.set("cohortId", value)
    const cohort = cohorts.find((item) => item.id === Number(value))
    const program = cohort ? programs.find((item) => item.id === cohort.programId) : undefined
    if (program) {
      next.set("programId", String(program.id))
      next.set("programCode", program.code)
      next.set("majorId", String(program.majorId))
      next.set("majorCode", program.majorCode)
    }
    setSearchParams(next)
  }
  const setFilterValue = (key: "semester" | "status", value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value === SYLLABUS_FILTER_ALL) next.delete(key)
    else next.set(key, value)
    setSearchParams(next)
  }

  const isLoading = programsQuery.isLoading || majorsQuery.isLoading || cohortsQuery.isLoading || mapQuery.isLoading
  const hasError = programsQuery.isError || majorsQuery.isError || cohortsQuery.isError || mapQuery.isError

  return (
    <div data-admin-page="SyllabusCurriculumMapPage" className="space-y-5 pb-8">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white px-6 py-5 shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />
        <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex items-start gap-3">
            <Button type="button" variant="ghost" size="icon" onClick={() => navigate(-1)} title="Back"><ArrowLeft className="size-4" /></Button>
            <div data-admin-page-header="SyllabusCurriculumMapPage">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">SCSE / Curriculum</p>
              <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#006d73]">Curriculum Map</h1>
              <p className="mt-1 text-sm text-slate-500">Cohort determines the curriculum; semester places each course; structured relationships draw the arrows.</p>
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <MapSelect label="Major" value={selectedMajorCode} placeholder="Major" width="w-[280px]" onChange={setMajor}>
              <SelectItem value={SYLLABUS_FILTER_ALL}>All Majors</SelectItem>
              {majors.map((major) => <SelectItem key={major.id} value={major.code}>
                {major.code}{major.name ? ` — ${major.name}` : major.nameVn ? ` — ${major.nameVn}` : ""}
              </SelectItem>)}
            </MapSelect>
            <MapSelect label="Cohort" value={selectedCohort ? String(selectedCohort.id) : ""} placeholder="Select cohort" width="w-[130px]" onChange={setCohort} disabled={selectedMajorCode === SYLLABUS_FILTER_ALL || cohorts.length === 0}>
              {cohorts.map((cohort) => <SelectItem key={cohort.id} value={String(cohort.id)}>{cohort.name}</SelectItem>)}
            </MapSelect>
            <MapSelect label="Semester" value={filter.semester ?? SYLLABUS_FILTER_ALL} placeholder="All semesters" width="w-[150px]" onChange={(value) => setFilterValue("semester", value)}>
              <SelectItem value={SYLLABUS_FILTER_ALL}>All Semesters</SelectItem>
              {SYLLABUS_SEMESTER_OPTIONS.map((semester) => <SelectItem key={semester} value={semester}>Semester {semester}</SelectItem>)}
            </MapSelect>
            <MapSelect label="Status" value={filter.status ?? SYLLABUS_FILTER_ALL} placeholder="All statuses" width="w-[180px]" onChange={(value) => setFilterValue("status", value)}>
              <SelectItem value={SYLLABUS_FILTER_ALL}>All Statuses</SelectItem>
              {SYLLABUS_CANONICAL_STATUSES.map((status) => <SelectItem key={status} value={status}>{formatSyllabusFilterStatus(status)}</SelectItem>)}
            </MapSelect>
            <Button type="button" variant="outline" onClick={() => mapQuery.refetch()} disabled={!selectedCohort || mapQuery.isFetching}><RefreshCw className={cn("size-4", mapQuery.isFetching && "animate-spin")} />Refresh</Button>
            <Button type="button" variant="outline" onClick={() => window.print()} disabled={!mapQuery.data}><Printer className="size-4" />Print / PDF</Button>
          </div>
        </div>
      </section>

      <section className="rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="font-semibold text-[#17343d]">{selectedProgram ? `${baseProgramCode(selectedProgram.code)} — ${selectedProgram.nameVn || selectedProgram.name}` : "Select a program"}</h2>
            <p className="mt-1 text-xs text-slate-500">{selectedCohort?.name ?? "Select a Cohort"} · {courseCount} courses · {mapQuery.data?.relations.length ?? 0} relationships</p>
          </div>
          <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-600">
            <LegendNode className="border-sky-300 bg-sky-100" label="Major course" />
            <LegendNode className="border-slate-400 bg-white" label="General course" />
            <LegendLine label="Prerequisite" />
            <LegendLine dashed label="Previous / recommended" />
            <LegendLine dotted label="Co-requisite" />
          </div>
        </div>

        {isLoading ? <Status><Loader2 className="size-5 animate-spin text-[#007d84]" />Loading curriculum map...</Status>
          : hasError ? <Status><GitBranch className="size-8 text-rose-400" /><span className="text-rose-700">Unable to load curriculum data. Verify the Program and Cohort.</span></Status>
          : !selectedCohort ? <Status>Select a Cohort to generate its map.</Status>
          : courseCount === 0 ? <Status>No curriculum map can be generated because there are no syllabus records matching the selected filters.</Status>
          : (
            <div className="overflow-x-auto p-5">
              <div className="relative" style={{ width: graphLayout.width, height: graphLayout.height }}>
                <svg className="pointer-events-none absolute inset-0 z-0 overflow-visible" width={graphLayout.width} height={graphLayout.height} aria-hidden="true">
                  <defs>
                    <marker id="curriculum-arrow" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><path d="M0,0 L7,3.5 L0,7 Z" fill="#475569" /></marker>
                    <marker id="curriculum-arrow-teal" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><path d="M0,0 L7,3.5 L0,7 Z" fill="#0f766e" /></marker>
                  </defs>
                  {(mapQuery.data?.relations ?? []).map((relation, index) => {
                    const start = graphLayout.positions.get(relation.fromCourseId)
                    const end = graphLayout.positions.get(relation.toCourseId)
                    if (!start || !end) return null
                    const direction = end.x >= start.x ? 1 : -1
                    const startX = start.x + direction * (NODE_WIDTH / 2)
                    const endX = end.x - direction * (NODE_WIDTH / 2 + 5)
                    const bend = Math.max(38, Math.abs(endX - startX) * 0.42)
                    const color = relation.relationType === "COREQUISITE" ? "#0f766e" : "#475569"
                    const dash = relation.relationType === "RECOMMENDED" ? "7 5" : relation.relationType === "COREQUISITE" ? "2 3" : undefined
                    return <g key={`${relation.fromCourseId}-${relation.toCourseId}-${relation.relationType}-${index}`}>
                      <title>{`${relation.fromCourseCode} → ${relation.toCourseCode}: ${relationLabel(relation.relationType)}`}</title>
                      <path d={`M ${startX} ${start.y} C ${startX + direction * bend} ${start.y}, ${endX - direction * bend} ${end.y}, ${endX} ${end.y}`} fill="none" stroke={color} strokeWidth="1.35" strokeDasharray={dash} markerEnd={relation.relationType === "COREQUISITE" ? "url(#curriculum-arrow-teal)" : "url(#curriculum-arrow)"} />
                    </g>
                  })}
                </svg>
                {visibleGroups.map((semester, columnIndex) => {
                  const courses = semesterMap.get(semester) ?? []
                  const left = columnIndex * COLUMN_PITCH
                  return <div key={semester}>
                    <div className="absolute top-0 z-20 border-b-2 border-[#007d84] bg-white pb-2 text-center" style={{ left, width: COLUMN_WIDTH }}>
                      <h3 className="text-sm font-bold text-[#17343d]">{semester}</h3><span className="text-[11px] text-slate-400">{courses.length} courses</span>
                    </div>
                    {courses.map((course, rowIndex) => <div key={course.courseId} className="absolute z-10" style={{ left: left + (COLUMN_WIDTH - NODE_WIDTH) / 2, top: NODE_TOP + rowIndex * NODE_PITCH }}><CourseMapNode course={course} /></div>)}
                    {courses.length === 0 && <div className="absolute flex h-16 items-center justify-center rounded-lg border border-dashed border-slate-200 text-xs text-slate-300" style={{ left, top: NODE_TOP, width: COLUMN_WIDTH }}>No courses</div>}
                  </div>
                })}
              </div>
            </div>
          )}
      </section>
    </div>
  )
}

function MapSelect({ label, value, placeholder, width, onChange, disabled, children }: { label: string; value: string; placeholder: string; width: string; onChange: (value: string) => void; disabled?: boolean; children: React.ReactNode }) {
  return <label className="space-y-1 text-[10px] font-semibold uppercase tracking-wider text-slate-500"><span>{label}</span><Select value={value} onValueChange={onChange} disabled={disabled}><SelectTrigger className={cn(width, "bg-white normal-case tracking-normal")}><SelectValue placeholder={placeholder} /></SelectTrigger><SelectContent>{children}</SelectContent></Select></label>
}

function CourseMapNode({ course }: { course: CurriculumMapCourse }) {
  const isGeneral = /general|đại cương/i.test(course.courseTypes ?? "")
  return <div id={nodeId(course.courseId)} className={cn("relative z-10 flex h-[84px] w-[178px] flex-col justify-center rounded-md border px-3 py-2 text-center shadow-sm transition hover:-translate-y-0.5 hover:shadow-md", isGeneral ? "border-slate-400 bg-white" : "border-sky-300 bg-sky-100")} title={`${course.courseCode} · ${course.courseName}\n${course.courseTypes || "Course type not assigned"}`}>
    <div className="font-mono text-[11px] font-bold text-[#006d73]">{course.courseCode} ({course.creditTheory ?? 0},{course.creditLab ?? 0})</div>
    <div className="mt-1 text-[11px] font-medium leading-4 text-slate-800">{course.courseName}</div>
    {course.syllabusVersion && <div className="mt-1 text-[9px] text-slate-400">{course.syllabusVersion}</div>}
  </div>
}

function Status({ children }: { children: React.ReactNode }) { return <div className="flex min-h-[460px] items-center justify-center gap-2 px-6 text-center text-sm text-slate-500">{children}</div> }
function LegendNode({ className, label }: { className: string; label: string }) { return <span className="inline-flex items-center gap-1.5"><i className={cn("h-4 w-6 rounded-sm border", className)} />{label}</span> }
function LegendLine({ label, dashed, dotted }: { label: string; dashed?: boolean; dotted?: boolean }) { return <span className="inline-flex items-center gap-1.5"><i className={cn("block w-7 border-t-2 border-slate-500", dashed && "border-dashed", dotted && "border-dotted")} />{label}</span> }
