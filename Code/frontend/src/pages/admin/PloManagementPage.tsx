import { useMemo, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  BookOpenCheck,
  Edit3,
  Plus,
  Search,
  Target,
  Trash2,
} from "lucide-react"

import { ploApi } from "@/api/bookApi"
import { programApi } from "@/api/programApi"
import type { Plo, PloRequest } from "@/types/book"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

const CATEGORY_OPTIONS = [
  { value: "KNOWLEDGE", label: "Knowledge" },
  { value: "SKILL", label: "Skill" },
  { value: "ATTITUDE", label: "Attitude" },
  { value: "OTHER", label: "Other" },
] as const

const EMPTY_FORM = {
  programId: "",
  code: "",
  description: "",
  descriptionVn: "",
  category: "KNOWLEDGE",
  versionNumber: "1",
}

type PloFormState = typeof EMPTY_FORM

function getBaseProgramCode(value: unknown) {
  const code = String(value ?? "").trim()

  if (!code) return "—"

  return (
    code
      .replace(/[-_\s]?20\d{2}$/i, "")
      .replace(/[-_\s]+$/g, "")
      .trim()
    || code
  )
}

function getProgramDisplay(program: {
  code?: string | null
  name?: string | null
  nameVn?: string | null
}) {
  const code = getBaseProgramCode(program.code)
  const name = program.name || program.nameVn || "Unnamed program"
  return `${code} — ${name}`
}

function hasLegacyEncodingIssue(value?: string | null) {
  return Boolean(value && value.includes("???"))
}

function getErrorMessage(error: unknown, fallback: string) {
  const responseError = error as {
    response?: { data?: { message?: string }; status?: number }
  }

  if (responseError.response?.status === 409) {
    return "This PLO cannot be deleted because it is referenced by CLO–PLO mappings. Remove the related mappings first."
  }

  return responseError.response?.data?.message || fallback
}

function categoryLabel(category?: string) {
  return (
    CATEGORY_OPTIONS.find((option) => option.value === category)?.label ||
    category ||
    "Chưa phân nhóm"
  )
}

function categoryBadgeClass(category?: string) {
  switch (category) {
    case "KNOWLEDGE":
      return "border-blue-200 bg-blue-50 text-blue-700"
    case "SKILL":
      return "border-violet-200 bg-violet-50 text-violet-700"
    case "ATTITUDE":
      return "border-amber-200 bg-amber-50 text-amber-700"
    default:
      return "border-slate-200 bg-slate-50 text-slate-600"
  }
}

export default function PloManagementPage() {
  const queryClient = useQueryClient()
  const [programFilter, setProgramFilter] = useState("all")
  const [categoryFilter, setCategoryFilter] = useState("all")
  const [searchQuery, setSearchQuery] = useState("")
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingPlo, setEditingPlo] = useState<Plo | null>(null)
  const [form, setForm] = useState<PloFormState>(EMPTY_FORM)

  const {
    data: programs = [],
    isLoading: isLoadingPrograms,
    isError: isProgramError,
  } = useQuery({
    queryKey: ["programs"],
    queryFn: programApi.getAll,
  })

  const {
    data: plos = [],
    isLoading: isLoadingPlos,
    isError: isPloError,
  } = useQuery({
    queryKey: ["plos"],
    queryFn: ploApi.getAll,
  })

  const closeDialog = () => {
    setDialogOpen(false)
    setEditingPlo(null)
    setForm(EMPTY_FORM)
  }

  const createMutation = useMutation({
    mutationFn: ploApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plos"] })
      closeDialog()
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, "Unable to create PLO."))
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: PloRequest }) =>
      ploApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plos"] })
      closeDialog()
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, "Unable to update PLO."))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: ploApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["plos"] })
    },
    onError: (error) => {
      window.alert(getErrorMessage(error, "Unable to delete PLO."))
    },
  })

  const filteredPlos = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase()

    return [...plos]
      .filter((plo) => {
        const matchesProgram =
          programFilter === "all" || plo.programId.toString() === programFilter
        const matchesCategory =
          categoryFilter === "all" || plo.category === categoryFilter
        const matchesKeyword =
          !keyword ||
          plo.code.toLowerCase().includes(keyword) ||
          (plo.programCode || "").toLowerCase().includes(keyword) ||
          (plo.programName || "").toLowerCase().includes(keyword) ||
          (plo.description || "").toLowerCase().includes(keyword) ||
          (plo.descriptionVn || "").toLowerCase().includes(keyword)

        return matchesProgram && matchesCategory && matchesKeyword
      })
      .sort((left, right) => {
        const programCompare = (left.programCode || "").localeCompare(
          right.programCode || "",
          "vi",
          { numeric: true }
        )
        if (programCompare !== 0) return programCompare

        const versionCompare =
          (left.versionNumber || 1) - (right.versionNumber || 1)
        if (versionCompare !== 0) return versionCompare

        return left.code.localeCompare(right.code, "vi", { numeric: true })
      })
  }, [categoryFilter, plos, programFilter, searchQuery])

  const groupedPlos = useMemo(() => {
    const totalByProgram = new Map<number, number>()
    plos.forEach((plo) => {
      totalByProgram.set(
        plo.programId,
        (totalByProgram.get(plo.programId) || 0) + 1,
      )
    })

    const groups = new Map<number, {
      programId: number
      programCode: string
      programName: string
      totalCount: number
      plos: Plo[]
    }>()

    filteredPlos.forEach((plo) => {
      const program = programs.find((item) => item.id === plo.programId)
      const existing = groups.get(plo.programId)

      if (existing) {
        existing.plos.push(plo)
        return
      }

      groups.set(plo.programId, {
        programId: plo.programId,
        programCode: getBaseProgramCode(plo.programCode || program?.code),
        programName:
          plo.programName
          || program?.name
          || program?.nameVn
          || "Unnamed program",
        totalCount: totalByProgram.get(plo.programId) || 0,
        plos: [plo],
      })
    })

    return Array.from(groups.values())
  }, [filteredPlos, plos, programs])

  const stats = useMemo(() => {
    const activePlos = plos.filter((plo) => plo.isActive !== false)
    return {
      total: plos.length,
      programs: new Set(plos.map((plo) => plo.programId)).size,
      active: activePlos.length,
      filtered: filteredPlos.length,
    }
  }, [filteredPlos.length, plos])

  const openCreateDialog = () => {
    const defaultProgramId =
      programFilter !== "all"
        ? programFilter
        : programs[0]?.id
          ? programs[0].id.toString()
          : ""

    setEditingPlo(null)
    setForm({ ...EMPTY_FORM, programId: defaultProgramId })
    setDialogOpen(true)
  }

  const openEditDialog = (plo: Plo) => {
    setEditingPlo(plo)
    setForm({
      programId: plo.programId.toString(),
      code: plo.code,
      description: plo.description || "",
      descriptionVn: plo.descriptionVn || "",
      category: plo.category || "KNOWLEDGE",
      versionNumber: (plo.versionNumber || 1).toString(),
    })
    setDialogOpen(true)
  }

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()

    const code = form.code.trim().toUpperCase()
    const description = form.description.trim()
    const versionNumber = Number(form.versionNumber)

    if (!form.programId || !code || !description) {
      window.alert("Select a curriculum program and enter the PLO code and English description.")
      return
    }

    if (!Number.isInteger(versionNumber) || versionNumber < 1) {
      window.alert("Version PLO phải là số nguyên lớn hơn hoặc bằng 1.")
      return
    }

    const payload: PloRequest = {
      programId: Number(form.programId),
      code,
      description,
      descriptionVn: form.descriptionVn.trim() || undefined,
      category: form.category,
      versionNumber,
    }

    if (editingPlo) {
      updateMutation.mutate({ id: editingPlo.id, data: payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  const handleDelete = (plo: Plo) => {
    const confirmed = window.confirm(
      `Delete ${plo.code} from ${getBaseProgramCode(plo.programCode) || plo.programId}?\n\nA PLO that is referenced by CLO mappings cannot be deleted.`
    )

    if (confirmed) deleteMutation.mutate(plo.id)
  }

  const isSaving = createMutation.isPending || updateMutation.isPending
  const isLoading = isLoadingPrograms || isLoadingPlos
  const hasError = isProgramError || isPloError

  return (
    <div className="space-y-6">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-gradient-to-br from-white via-[#f8fbfb] to-[#eef7f7] px-6 py-5 shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-[#708894]">
              Academic Outcomes
            </p>
            <h1 className="mt-1 font-heading text-2xl font-bold text-primary">
              Program Learning Outcomes (PLO)
            </h1>
            <p className="mt-1 max-w-3xl text-sm leading-6 text-slate-500">
              Define and maintain the PLO/ILO list for each curriculum program. CLO mapping, coverage diagnostics, and matrix export are managed in the CLO–PLO Heatmap.
            </p>
          </div>
        <Button
          onClick={openCreateDialog}
          disabled={isLoadingPrograms || programs.length === 0}
          className="gap-2 bg-primary text-white hover:bg-primary/90"
        >
          <Plus className="size-4" />
          Add PLO
        </Button>
        </div>
      </section>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Total PLOs
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {isLoading ? "..." : stats.total}
              </p>
            </div>
            <div className="rounded-xl border border-blue-100 bg-blue-50 p-3 text-blue-600">
              <Target className="size-5" />
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Programs with PLOs
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {isLoading ? "..." : stats.programs}
              </p>
            </div>
            <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-3 text-emerald-600">
              <BookOpenCheck className="size-5" />
            </div>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Active
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {isLoading ? "..." : stats.active}
              </p>
            </div>
            <Badge className="border border-emerald-200 bg-emerald-50 text-emerald-700">
              Active
            </Badge>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Filtered Results
              </p>
              <p className="mt-2 text-3xl font-extrabold text-slate-900">
                {isLoading ? "..." : stats.filtered}
              </p>
            </div>
            <div className="rounded-xl border border-violet-100 bg-violet-50 p-3 text-violet-600">
              <Target className="size-5" />
            </div>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(260px,1fr)_240px_220px]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />
            <Input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search PLO code, program, or description..."
              className="pl-9"
            />
          </div>

          <Select value={programFilter} onValueChange={setProgramFilter}>
            <SelectTrigger>
              <SelectValue placeholder="All Programs" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Programs</SelectItem>
              {programs.map((program) => (
                <SelectItem key={program.id} value={program.id.toString()}>
                  {getProgramDisplay(program)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select value={categoryFilter} onValueChange={setCategoryFilter}>
            <SelectTrigger>
              <SelectValue placeholder="All Categories" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Categories</SelectItem>
              {CATEGORY_OPTIONS.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Showing <span className="font-semibold text-slate-700">{filteredPlos.length}</span> of{" "}
          <span className="font-semibold text-slate-700">{plos.length}</span> PLOs.
        </p>
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50/80">
                <TableHead className="min-w-[190px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Program
                </TableHead>
                <TableHead className="w-[105px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  PLO Code
                </TableHead>
                <TableHead className="w-[130px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Category
                </TableHead>
                <TableHead className="min-w-[300px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  English Description
                </TableHead>
                <TableHead className="min-w-[280px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Vietnamese Description
                </TableHead>
                <TableHead className="w-[90px] text-center text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Version
                </TableHead>
                <TableHead className="w-[120px] text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Status
                </TableHead>
                <TableHead className="w-[105px] text-right text-[10px] font-bold uppercase tracking-[0.12em] text-slate-600">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={8} className="py-12 text-center text-slate-400">
                    Loading PLOs...
                  </TableCell>
                </TableRow>
              ) : hasError ? (
                <TableRow>
                  <TableCell colSpan={8} className="py-12 text-center text-rose-600">
                    Unable to load PLO or curriculum program data.
                  </TableCell>
                </TableRow>
              ) : filteredPlos.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="py-12 text-center text-slate-400">
                    {plos.length === 0
                      ? "No PLO has been defined yet. Add the first PLO for a curriculum program."
                      : "No PLO matches the current filters."}
                  </TableCell>
                </TableRow>
              ) : (
                groupedPlos.flatMap((group) =>
                  group.plos.map((plo, index) => (
                  <TableRow
                    key={plo.id}
                    className={`align-top hover:bg-slate-50/50 ${index === 0 ? "border-t border-[#9fc8cb]" : ""}`}
                  >
                    {index === 0 && (
                      <TableCell
                        rowSpan={group.plos.length}
                        className="border-r border-[#c9dfe1] bg-[#f1f8f8] px-5 py-5 align-top"
                      >
                        <p className="text-base font-extrabold text-[#006f76]">
                          {group.programCode || `#${group.programId}`}
                        </p>
                        <p className="mt-1 max-w-[170px] whitespace-normal text-xs leading-5 text-slate-600">
                          {group.programName}
                        </p>
                        <Badge className="mt-3 border border-[#cce7e8] bg-[#e5f4f4] text-[10px] font-bold text-[#006f76]">
                          {group.plos.length === group.totalCount
                            ? `${group.totalCount} PLO${group.totalCount === 1 ? "" : "s"}`
                            : `${group.plos.length} of ${group.totalCount} PLOs`}
                        </Badge>
                      </TableCell>
                    )}
                    <TableCell>
                      <span className="font-mono text-sm font-bold text-[#007d84]">
                        {plo.code}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Badge
                        className={`border ${categoryBadgeClass(plo.category)}`}
                      >
                        {categoryLabel(plo.category)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <p className="line-clamp-4 whitespace-normal text-sm leading-6 text-slate-700">
                        {plo.description || "—"}
                      </p>
                      {hasLegacyEncodingIssue(plo.description) && (
                        <p className="mt-1 text-[10px] font-medium text-amber-700">
                          Legacy text requires data cleanup
                        </p>
                      )}
                    </TableCell>
                    <TableCell>
                      <p className="line-clamp-4 whitespace-normal text-sm leading-6 text-slate-600">
                        {plo.descriptionVn || "Not provided"}
                      </p>
                      {hasLegacyEncodingIssue(plo.descriptionVn) && (
                        <p className="mt-1 text-[10px] font-medium text-amber-700">
                          Legacy text requires data cleanup
                        </p>
                      )}
                    </TableCell>
                    <TableCell className="text-center font-semibold text-slate-700">
                      v{plo.versionNumber || 1}
                    </TableCell>
                    <TableCell>
                      {plo.isActive === false ? (
                        <Badge className="border border-slate-200 bg-slate-100 text-slate-600">
                          Inactive
                        </Badge>
                      ) : (
                        <Badge className="border border-emerald-200 bg-emerald-50 text-emerald-700">
                          Active
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-1">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          title={`Chỉnh sửa ${plo.code}`}
                          onClick={() => openEditDialog(plo)}
                        >
                          <Edit3 className="size-4 text-blue-600" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          title={`Xóa ${plo.code}`}
                          disabled={deleteMutation.isPending}
                          onClick={() => handleDelete(plo)}
                        >
                          <Trash2 className="size-4 text-rose-600" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                  )),
                )
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog
        open={dialogOpen}
        onOpenChange={(open) => {
          if (!open && !isSaving) closeDialog()
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto bg-white sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading text-primary">
              {editingPlo ? `Chỉnh sửa ${editingPlo.code}` : "Add PLO mới"}
            </DialogTitle>
            <DialogDescription>
              Each PLO code must be unique within the same curriculum program and version.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4 pt-2">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="plo-program">Program đào tạo *</Label>
                <Select
                  value={form.programId}
                  onValueChange={(value) =>
                    setForm((current) => ({ ...current, programId: value }))
                  }
                >
                  <SelectTrigger id="plo-program">
                    <SelectValue placeholder="Select a curriculum program" />
                  </SelectTrigger>
                  <SelectContent>
                    {programs.map((program) => (
                      <SelectItem key={program.id} value={program.id.toString()}>
                        {getProgramDisplay(program)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="plo-code">PLO Code *</Label>
                <Input
                  id="plo-code"
                  value={form.code}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      code: event.target.value.toUpperCase(),
                    }))
                  }
                  maxLength={20}
                  placeholder="e.g. PLO1"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="plo-version">Version *</Label>
                <Input
                  id="plo-version"
                  type="number"
                  min={1}
                  step={1}
                  value={form.versionNumber}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      versionNumber: event.target.value,
                    }))
                  }
                  required
                />
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="plo-category">Category PLO</Label>
                <Select
                  value={form.category}
                  onValueChange={(value) =>
                    setForm((current) => ({ ...current, category: value }))
                  }
                >
                  <SelectTrigger id="plo-category">
                    <SelectValue placeholder="Select a category" />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORY_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="plo-description-en">English Description *</Label>
                <textarea
                  id="plo-description-en"
                  value={form.description}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      description: event.target.value,
                    }))
                  }
                  rows={5}
                  required
                  placeholder="Enter the program learning outcome in English..."
                  className="flex min-h-28 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/15"
                />
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="plo-description-vn">Vietnamese Description</Label>
                <textarea
                  id="plo-description-vn"
                  value={form.descriptionVn}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      descriptionVn: event.target.value,
                    }))
                  }
                  rows={5}
                  placeholder="Enter the Vietnamese translation..."
                  className="flex min-h-28 w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm outline-none transition-colors placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/15"
                />
              </div>
            </div>

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={closeDialog} disabled={isSaving}>
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={isSaving || programs.length === 0}
                className="bg-primary text-white hover:bg-primary/90"
              >
                {isSaving
                  ? "Saving..."
                  : editingPlo
                    ? "Save Changes"
                    : "Create PLO"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
