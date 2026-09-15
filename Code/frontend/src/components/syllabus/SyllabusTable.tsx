
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Link } from "react-router-dom"
import { Eye, Pencil, Trash2, Check } from "lucide-react"
import axios from "axios"
import { useDeleteSyllabus } from "@/hooks/useDeleteSyllabus"
import { useSubmitSyllabus } from "@/hooks/useSubmitSyllabus"
import { getSyllabusMetadata } from "@/lib/syllabusHelper"

import type { Syllabus } from "@/types/syllabus"

interface Props {
  data: Syllabus[]
}

const normalizeSemesterValue = (value: unknown) => {
  const text = String(value ?? "").trim()

  const matched = text.match(/semester\s*(\d+)/i)
  if (matched) return matched[1]

  return text
}

const formatSemesterLabel = (value: unknown) => {
  const semester = normalizeSemesterValue(value)

  if (!semester) return "—"

  return /^\d+$/.test(semester)
    ? `Semester ${semester}`
    : semester
}

export default function SyllabusTable({
  data,
}: Props) {
  const deleteMutation = useDeleteSyllabus()
  const submitMutation = useSubmitSyllabus()

  

  const handleDelete = (id: number) => {
    if (window.confirm("Are you sure you want to delete this syllabus?")) {
      deleteMutation.mutate(id)
    }
  }

  const handleSubmit = (
  id: number,
) => {
  if (
    !window.confirm(
      "Submit this syllabus for Department review?",
    )
  ) {
    return
  }

  submitMutation.mutate(id, {
    onSuccess: () => {
      alert(
        "Syllabus submitted successfully!",
      )
    },

    onError: (error: unknown) => {
      const message =
        axios.isAxiosError<{
          message?: string
        }>(error)
          ? error.response?.data?.message
          : undefined

      alert(
        message
          || "Unable to submit the syllabus.",
      )
    },
  })
}



  const getStatusBadge = (status: string) => {
    switch (status.toUpperCase()) {
      case "APPROVED":
        return (
          <Badge className="bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-50 dark:bg-emerald-950/30 dark:text-emerald-400 dark:border-emerald-800">
            Approved
          </Badge>
        )
      case "SUBMITTED":
        return (
          <Badge className="bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-50 dark:bg-blue-950/30 dark:text-blue-400 dark:border-blue-800">
            Submitted
          </Badge>
        )
      case "REJECTED":
        return (
          <Badge className="bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-50 dark:bg-rose-950/30 dark:text-rose-400 dark:border-rose-800">
            Rejected
          </Badge>
        )
      case "REVISION_REQUESTED":
        return (
          <Badge className="bg-amber-50 text-amber-700 border border-amber-200">
            Revision Requested
          </Badge>
        )
      case "ARCHIVED":
        return (
          <Badge className="bg-slate-100 text-slate-500 border border-slate-200">
            Archived
          </Badge>
        )
      case "DRAFT":
      default:
        return (
          <Badge className="bg-slate-100 text-slate-700 border border-slate-200 hover:bg-slate-100 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700">
            Draft
          </Badge>
        )
    }
  }

  return (
    <>
    <Table>
      <TableHeader>
        <TableRow className="bg-slate-50/50 dark:bg-slate-800/50">
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[120px]">Course Code</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300">Syllabus Title</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300">Major</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[130px]">Semester</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[100px]">Version</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[120px]">Academic Year</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[120px]">Status</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[100px]">Current</TableHead>
          <TableHead className="font-semibold text-slate-700 dark:text-slate-300 w-[160px] text-right">Actions</TableHead>
        </TableRow>
      </TableHeader>

      <TableBody>
        {data.length === 0 ? (
          <TableRow>
            <TableCell colSpan={9} className="text-center py-8 text-slate-400 dark:text-slate-600">
              No matching syllabus data.
            </TableCell>
          </TableRow>
        ) : (
          data.map((item) => {
            const metadata = getSyllabusMetadata(item.courseCode, item.courseName)
            return (
              <TableRow key={item.id} className="hover:bg-slate-50/40 dark:hover:bg-slate-800/20">
                <TableCell className="font-mono text-xs font-semibold text-slate-600 dark:text-slate-400">
                  {item.courseCode}
                </TableCell>

                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-semibold text-slate-900 dark:text-slate-200">
                      {item.courseName}
                    </span>
                    <span className="text-xs text-slate-400 font-normal">
                      Major: {metadata.major}
                    </span>
                  </div>
                </TableCell>

                <TableCell className="text-slate-600 dark:text-slate-400 text-sm">
                  {item.major || metadata.specialization || "—"}
                </TableCell>

                <TableCell className="text-slate-600 dark:text-slate-400 text-sm">
                  {formatSemesterLabel(item.semester)}
                </TableCell>

                <TableCell className="font-medium text-sm text-slate-700 dark:text-slate-300">
                  {item.versionLabel}
                </TableCell>

                <TableCell className="text-slate-600 dark:text-slate-400 text-sm">
                  {item.academicYear}
                </TableCell>

                <TableCell>
                  {getStatusBadge(item.status)}
                </TableCell>

                <TableCell>
                  {item.isCurrent ? (
                    <div className="flex items-center gap-1.5 text-emerald-600 font-medium text-sm">
                      <span className="size-2 rounded-full bg-emerald-500 animate-pulse shrink-0" />
                      <span>Yes</span>
                    </div>
                  ) : (
                    <span className="text-slate-400 text-sm">No</span>
                  )}
                </TableCell>

                <TableCell>
                  <div className="flex items-center justify-end gap-1">
                    <Link to={`/dept-head/syllabus/${item.id}`} title="View Details">
                      <Button
                        size="sm"
                        variant="ghost"
                        className="h-8 w-8 p-0 text-slate-500 hover:text-slate-900 hover:bg-slate-100 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                      >
                        <Eye className="size-4" />
                      </Button>
                    </Link>

                    {item.status === "DRAFT" && (
                      <Link to={`/dept-head/syllabus/${item.id}/edit`} title="Edit Draft">
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-8 w-8 p-0 text-slate-500 hover:text-primary hover:bg-primary/5 dark:hover:bg-primary/10 dark:hover:text-sky-400"
                        >
                          <Pencil className="size-4" />
                        </Button>
                      </Link>
                    )}

                    {item.status === "DRAFT" && (
                      <Button
                        size="sm"
                        variant="ghost"
                        title="Submit Syllabus"
                        onClick={() => handleSubmit(item.id)}
                        disabled={submitMutation.isPending}
                        className="h-8 w-8 p-0 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/20 dark:hover:text-emerald-400"
                      >
                        <Check className="size-4" />
                      </Button>
                    )}

                    {item.status === "DRAFT" && (
                      <Button
                        size="sm"
                        variant="ghost"
                        title="Delete Draft"
                        onClick={() => handleDelete(item.id)}
                        className="h-8 w-8 p-0 text-slate-500 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/20 dark:hover:text-rose-400"
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            )
          })
        )}
      </TableBody>
    </Table>
    </>
  )
}