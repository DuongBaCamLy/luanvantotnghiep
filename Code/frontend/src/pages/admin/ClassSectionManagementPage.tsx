import { useMemo, useState } from "react"
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import {
  BookOpenCheck,
  BookOpenText,
  CheckCircle2,
  Edit3,
  Plus,
  Power,
  Search,
  Users,
} from "lucide-react"

import {
  createClassSection,
  getClassSections,
  updateClassSection,
  type ClassSectionResponse,
  type CreateClassSectionRequest,
} from "@/api/classSectionApi"
import ClassSectionFormDialog from "@/components/ClassSectionFormDialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

const ALL = "__all__"

const getErrorMessage = (error: unknown) => {
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
            error?: string
          } | string
        }
      }
    ).response

    if (typeof response?.data === "string" && response.data) {
      return response.data
    }

    if (response?.data && typeof response.data === "object") {
      if (response.data.message) {
        return response.data.message
      }

      if (response.data.error) {
        return response.data.error
      }
    }
  }

  if (error instanceof Error) {
    return error.message
  }

  return "Unable to update the teaching assignment."
}

const formatSyllabusStatus = (
  value: string | null,
) => {
  if (!value) {
    return ""
  }

  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) => character.toUpperCase())
}

export default function ClassSectionManagementPage() {
  const queryClient = useQueryClient()

  const [formOpen, setFormOpen] = useState(false)
  const [editingSection, setEditingSection] =
    useState<ClassSectionResponse | null>(null)

  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState(ALL)
  const [syllabusFilter, setSyllabusFilter] = useState(ALL)

  const [notice, setNotice] =
    useState<
      | {
          type: "success" | "error"
          message: string
        }
      | null
    >(null)

  const {
    data: sections = [],
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["class-sections"],
    queryFn: getClassSections,
  })

  const refreshData = async () => {
    await Promise.all([
      queryClient.invalidateQueries({
        queryKey: ["class-sections"],
      }),
      queryClient.invalidateQueries({
        queryKey: ["instructors"],
      }),
      queryClient.invalidateQueries({
        queryKey: ["syllabuses"],
      }),
    ])
  }

  const createMutation = useMutation({
    mutationFn: createClassSection,
    onSuccess: async () => {
      await refreshData()
      setFormOpen(false)
      setEditingSection(null)

      setNotice({
        type: "success",
        message:
          "Teaching assignment created. The assigned instructor can now create a Draft syllabus when no syllabus is linked.",
      })
    },
    onError: (mutationError) => {
      setNotice({
        type: "error",
        message: getErrorMessage(mutationError),
      })
    },
  })

  const updateMutation = useMutation({
    mutationFn: updateClassSection,
    onSuccess: async (updated) => {
      await refreshData()
      setFormOpen(false)
      setEditingSection(null)

      setNotice({
        type: "success",
        message: `Teaching assignment for ${updated.courseCode} was updated.`,
      })
    },
    onError: (mutationError) => {
      setNotice({
        type: "error",
        message: getErrorMessage(mutationError),
      })
    },
  })

  const filteredSections = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()

    return sections.filter((section) => {
      if (
        statusFilter === "ACTIVE"
        && !section.isActive
      ) {
        return false
      }

      if (
        statusFilter === "INACTIVE"
        && section.isActive
      ) {
        return false
      }

      if (
        syllabusFilter === "MISSING"
        && section.syllabusId !== null
      ) {
        return false
      }

      if (
        syllabusFilter === "LINKED"
        && section.syllabusId === null
      ) {
        return false
      }

      if (!keyword) {
        return true
      }

      const searchable = [
        section.courseCode,
        section.courseName,
        section.instructorName,
      ]
        .join(" ")
        .toLowerCase()

      return searchable.includes(keyword)
    })
  }, [
    searchTerm,
    sections,
    statusFilter,
    syllabusFilter,
  ])

  const stats = useMemo(() => {
    const active = sections.filter((section) => section.isActive).length
    const missing = sections.filter((section) => section.syllabusId === null).length
    const linked = sections.length - missing
    const distinctInstructors = new Set(
      sections.map((section) => section.instructorId),
    ).size

    return {
      total: sections.length,
      active,
      missing,
      linked,
      distinctInstructors,
    }
  }, [sections])

  const handleOpenCreate = () => {
    setEditingSection(null)
    setNotice(null)
    setFormOpen(true)
  }

  const handleOpenEdit = (section: ClassSectionResponse) => {
    setEditingSection(section)
    setNotice(null)
    setFormOpen(true)
  }

  const handleSubmit = (data: CreateClassSectionRequest) => {
    setNotice(null)

    if (editingSection) {
      updateMutation.mutate({
        id: editingSection.id,
        req: data,
      })
      return
    }

    createMutation.mutate(data)
  }

  const handleToggleStatus = (
    section: ClassSectionResponse,
  ) => {
    const nextActive = !section.isActive

    const confirmed = window.confirm(
      nextActive
        ? `Activate the teaching assignment for ${section.courseCode} — ${section.instructorName}?`
        : `Deactivate the teaching assignment for ${section.courseCode} — ${section.instructorName}?\n\nThe record will be preserved for history. A linked syllabus will not be deleted.`,
    )

    if (!confirmed) {
      return
    }

    setNotice(null)

    if (!section.programId || !section.cohortId) {
      setNotice({
        type: "error",
        message: "This legacy assignment has no Program/Cohort context. Edit it and select the curriculum before changing its status.",
      })
      return
    }

    updateMutation.mutate({
      id: section.id,
      req: {
        courseId: section.courseId,
        programId: section.programId,
        cohortId: section.cohortId,
        syllabusId: section.syllabusId,
        instructorId: section.instructorId,
        semester: section.semester,
        academicYear: section.academicYear,
        groupNumber: section.groupNumber,
        labGroup: section.labGroup ?? undefined,
        maxStudents: section.maxStudents ?? undefined,
        room: section.room ?? undefined,
        schedule: section.schedule ?? undefined,
        sectionType: section.sectionType,
        isActive: nextActive,
      },
    })
  }

  const clearFilters = () => {
    setSearchTerm("")
    setStatusFilter(ALL)
    setSyllabusFilter(ALL)
  }

  const isSaving =
    createMutation.isPending || updateMutation.isPending

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              FR-01.4 · Teaching Responsibility
            </p>

            <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
              Teaching Assignments
            </h1>

            <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
              Assign active instructors to courses. Curriculum scope and semester are resolved from the curriculum when Faculty creates a syllabus.
            </p>
          </div>

          <Button
            type="button"
            className="bg-[#007d84] text-white hover:bg-[#006d73]"
            onClick={handleOpenCreate}
          >
            <Plus className="size-4" />
            Add Assignment
          </Button>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Assignments"
          value={stats.total}
          description={`${stats.distinctInstructors} assigned instructors`}
          icon={<Users className="size-5" />}
        />
        <MetricCard
          label="Active"
          value={stats.active}
          description="Currently usable assignments"
          icon={<CheckCircle2 className="size-5" />}
        />
        <MetricCard
          label="Waiting for Draft"
          value={stats.missing}
          description="No syllabus linked yet"
          icon={<BookOpenText className="size-5" />}
        />
        <MetricCard
          label="Syllabus Linked"
          value={stats.linked}
          description="Draft or preserved version attached"
          icon={<BookOpenCheck className="size-5" />}
        />
      </section>

      {notice && (
        <section
          className={
            notice.type === "success"
              ? "rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
              : "rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800"
          }
        >
          {notice.message}
        </section>
      )}

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(280px,1fr)_180px_190px_auto]">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <Input
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Search course or instructor..."
              className="pl-9"
            />
          </div>

          <Select
            value={syllabusFilter}
            onValueChange={setSyllabusFilter}
          >
            <SelectTrigger>
              <SelectValue placeholder="All Syllabus States" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All Syllabus States</SelectItem>
              <SelectItem value="MISSING">Waiting for Draft</SelectItem>
              <SelectItem value="LINKED">Syllabus Linked</SelectItem>
            </SelectContent>
          </Select>

          <Select
            value={statusFilter}
            onValueChange={setStatusFilter}
          >
            <SelectTrigger>
              <SelectValue placeholder="All Statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All Statuses</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="INACTIVE">Inactive</SelectItem>
            </SelectContent>
          </Select>

          <Button
            type="button"
            variant="outline"
            onClick={clearFilters}
          >
            Clear
          </Button>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          Showing {filteredSections.length} of {sections.length} assignments.
        </p>
      </section>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        {isError && (
          <div className="m-5 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
            {getErrorMessage(error)}
          </div>
        )}

        <div className="overflow-x-auto">
          <Table className="min-w-[850px]">
            <TableHeader className="bg-slate-50">
              <TableRow>
                <TableHead>Course</TableHead>
                <TableHead>Instructor</TableHead>
                <TableHead>Syllabus</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-32 text-center text-slate-500"
                  >
                    Loading teaching assignments...
                  </TableCell>
                </TableRow>
              ) : filteredSections.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-40 text-center"
                  >
                    <p className="font-semibold text-slate-700">
                      No teaching assignments match the selected filters.
                    </p>
                    <p className="mt-1 text-sm text-slate-500">
                      Add a new assignment or adjust the filters.
                    </p>
                  </TableCell>
                </TableRow>
              ) : (
                filteredSections.map((section) => (
                  <TableRow
                    key={section.id}
                    className="hover:bg-[#f8fbfb]"
                  >
                    <TableCell>
                      <p className="font-mono text-xs font-bold text-[#007d84]">
                        {section.courseCode}
                      </p>
                      <p className="mt-1 max-w-[260px] font-semibold text-slate-800">
                        {section.courseName}
                      </p>
                    </TableCell>

                    <TableCell className="font-medium text-slate-700">
                      {section.instructorName}
                    </TableCell>

                    <TableCell>
                      {section.syllabusId ? (
                        <div>
                          <Badge
                            variant="outline"
                            className="border-blue-200 bg-blue-50 text-blue-700"
                          >
                            v{section.syllabusVersionNumber}
                          </Badge>
                          <p className="mt-1 text-xs text-slate-500">
                            {formatSyllabusStatus(section.syllabusStatus)}
                          </p>
                        </div>
                      ) : (
                        <Badge
                          variant="outline"
                          className="border-amber-200 bg-amber-50 text-amber-700"
                        >
                          Waiting for Faculty Draft
                        </Badge>
                      )}
                    </TableCell>

                    <TableCell>
                      {section.isActive ? (
                        <Badge
                          variant="outline"
                          className="border-emerald-200 bg-emerald-50 text-emerald-700"
                        >
                          Active
                        </Badge>
                      ) : (
                        <Badge
                          variant="outline"
                          className="border-slate-300 bg-slate-100 text-slate-600"
                        >
                          Inactive
                        </Badge>
                      )}
                    </TableCell>

                    <TableCell>
                      <div className="flex justify-end gap-2">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenEdit(section)}
                        >
                          <Edit3 className="size-3.5" />
                          Edit
                        </Button>

                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={updateMutation.isPending}
                          className={
                            section.isActive
                              ? "border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                              : "border-emerald-200 text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800"
                          }
                          onClick={() => handleToggleStatus(section)}
                        >
                          <Power className="size-3.5" />
                          {section.isActive ? "Deactivate" : "Activate"}
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </section>

      <ClassSectionFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          if (!isSaving) {
            setFormOpen(open)
          }
        }}
        initialData={editingSection}
        onSubmit={handleSubmit}
        isLoading={isSaving}
      />
    </div>
  )
}

function MetricCard({
  label,
  value,
  description,
  icon,
}: {
  label: string
  value: number
  description: string
  icon: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
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
          {icon}
        </span>
      </div>
    </div>
  )
}
