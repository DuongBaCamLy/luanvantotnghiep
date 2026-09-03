import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { FilePenLine, Files, FileUp } from "lucide-react"

import { courseApi } from "@/api/courseApi"
import { courseProgramApi, type CourseProgramItem } from "@/api/courseProgramApi"
import ImportSyllabusDialog from "@/components/syllabus/ImportSyllabusDialog"
import BulkImportSyllabusDialog from "@/components/syllabus/BulkImportSyllabusDialog"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import type { BulkSyllabusImportPreviewResponse, SyllabusImportPreviewResponse } from "@/types/syllabusImport"

type Props = {
  open: boolean
  onOpenChange: (open: boolean) => void
  scopeProgramId?: number
  scopeCohortId?: number
  allowedCourseIds?: number[]
  contextCourseId?: number
  assignmentId?: number
  allowedAssignments?: Array<{ id: number; courseId: number }>
  onCreateRequested: (options: { courseId?: number; courseProgramId?: number; assignmentId?: number; importPreview?: SyllabusImportPreviewResponse; previewOnly?: boolean }) => void
  onBulkImportRequested?: (response: BulkSyllabusImportPreviewResponse) => void
}

export default function AddSyllabusDialog({ open, onOpenChange, scopeProgramId, scopeCohortId, allowedCourseIds, contextCourseId, assignmentId, allowedAssignments, onCreateRequested, onBulkImportRequested }: Props) {
  const [importOpen, setImportOpen] = useState(false)
  const [bulkImportOpen, setBulkImportOpen] = useState(false)

  const { data: courses = [], isLoading: coursesLoading } = useQuery({
    queryKey: ["courses", "add-syllabus-dialog"],
    queryFn: courseApi.getAll,
    enabled: open,
  })
  const hasCurriculumScope = Boolean(scopeProgramId && scopeCohortId)
  const { data: scopedCoursePrograms = [], isLoading: scopedCoursesLoading } = useQuery<CourseProgramItem[]>({
    queryKey: ["course-programs", "curriculum", scopeProgramId, scopeCohortId],
    queryFn: () => courseProgramApi.getCurriculum(scopeProgramId!, scopeCohortId!),
    enabled: open && hasCurriculumScope,
  })

  const close = () => {
    setImportOpen(false)
    setBulkImportOpen(false)
    onOpenChange(false)
  }

  const scopedCourseIds = new Set(scopedCoursePrograms.map((item) => item.courseId))
  const curriculumCourses = hasCurriculumScope
    ? courses.filter((course) => scopedCourseIds.has(course.id))
    : courses
  const assignmentScopedCourses = allowedCourseIds !== undefined
    ? curriculumCourses.filter((course) => allowedCourseIds.includes(course.id))
    : curriculumCourses
  const availableCourses = contextCourseId
    ? assignmentScopedCourses.filter((course) => course.id === contextCourseId)
    : assignmentScopedCourses
  const normalizeCourseCode = (value: unknown) => String(value ?? "").replace(/[^A-Z0-9]/gi, "").toUpperCase()

  return (
    <>
      <Dialog open={open} onOpenChange={(value) => value ? onOpenChange(true) : close()}>
        <DialogContent className="sm:max-w-[620px]">
          <DialogHeader>
            <DialogTitle>Add New Syllabus</DialogTitle>
            <DialogDescription>
              Add a DOCX/PDF template or create the syllabus manually. Imported content is reviewed in the editable form before saving.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {hasCurriculumScope && availableCourses.length === 0 && !scopedCoursesLoading && (
              <p className="text-xs text-amber-700">No course is configured for the selected Major and Cohort. Add the course to Curriculum Programs first.</p>
            )}

            <div className={`grid gap-3 ${hasCurriculumScope ? "sm:grid-cols-3" : "sm:grid-cols-2"}`}>
              <button type="button" disabled={coursesLoading || scopedCoursesLoading || availableCourses.length === 0} onClick={() => setImportOpen(true)} className="rounded-xl border border-slate-200 p-4 text-left transition hover:border-[#007d84] hover:bg-[#f2f8f9] disabled:cursor-not-allowed disabled:opacity-50">
                <FileUp className="mb-3 size-5 text-[#007d84]" />
                <p className="text-sm font-semibold text-slate-900">Add Template</p>
                <p className="mt-1 text-xs leading-5 text-slate-500">Upload DOCX (recommended) or PDF, then extract and auto-fill the form.</p>
              </button>
              {hasCurriculumScope && onBulkImportRequested && (
                <button type="button" disabled={coursesLoading || scopedCoursesLoading || availableCourses.length === 0} onClick={() => setBulkImportOpen(true)} className="rounded-xl border border-slate-200 p-4 text-left transition hover:border-[#007d84] hover:bg-[#f2f8f9] disabled:cursor-not-allowed disabled:opacity-50">
                  <Files className="mb-3 size-5 text-[#007d84]" />
                  <p className="text-sm font-semibold text-slate-900">Import Program Document</p>
                  <p className="mt-1 text-xs leading-5 text-slate-500">Extract multiple syllabuses from one file.</p>
                </button>
              )}
              <button type="button" disabled={coursesLoading || scopedCoursesLoading || availableCourses.length === 0} onClick={() => {
                close()
                const selectedCourse = contextCourseId
                  ? availableCourses.find((course) => course.id === contextCourseId)
                  : availableCourses.length === 1 ? availableCourses[0] : undefined
                const selectedCourseProgram = selectedCourse
                  ? scopedCoursePrograms.find((item) => item.courseId === selectedCourse.id)
                  : undefined
                onCreateRequested({
                  courseId: selectedCourse?.id,
                  courseProgramId: selectedCourseProgram?.id,
                  assignmentId,
                })
              }} className="rounded-xl border border-slate-200 p-4 text-left transition hover:border-[#007d84] hover:bg-[#f2f8f9] disabled:cursor-not-allowed disabled:opacity-50">
                <FilePenLine className="mb-3 size-5 text-[#007d84]" />
                <p className="text-sm font-semibold text-slate-900">Create Manually</p>
                <p className="mt-1 text-xs leading-5 text-slate-500">Open an empty form and enter the information manually.</p>
              </button>
            </div>
          </div>

          <DialogFooter><Button type="button" variant="outline" onClick={close}>Cancel</Button></DialogFooter>
        </DialogContent>
      </Dialog>

      <ImportSyllabusDialog
        open={importOpen}
        onClose={() => setImportOpen(false)}
        onPreviewConfirmed={(importPreview) => {
          if (!importPreview.data) {
            alert("The imported file does not contain syllabus data.")
            return
          }
          const sourceCode = normalizeCourseCode(importPreview.data.sourceCourseCode)
          const selectedCourse = availableCourses.find((course) => {
            const targetCode = normalizeCourseCode(course.courseCode)
            return targetCode === sourceCode || targetCode.replace(/IU$/, "") === sourceCode.replace(/IU$/, "")
          })
          if (!selectedCourse) {
            if (allowedCourseIds) {
              alert("You are not assigned to this course and cannot create or import its syllabus.")
              return
            }
            const previewCourse = availableCourses[0]
            if (!previewCourse) {
              alert("No curriculum course is available as a temporary preview context.")
              return
            }
            const previewCourseProgram = scopedCoursePrograms.find((item) => item.courseId === previewCourse.id)
            setImportOpen(false)
            close()
            onCreateRequested({
              courseId: previewCourse.id,
              courseProgramId: previewCourseProgram?.id,
              assignmentId,
              importPreview,
              previewOnly: true,
            })
            return
          }
          const selectedCourseProgram = scopedCoursePrograms.find((item) => item.courseId === selectedCourse.id)
          const matchedAssignmentId = allowedAssignments?.find((assignment) => assignment.courseId === selectedCourse.id)?.id
          setImportOpen(false)
          close()
          onCreateRequested({ courseId: selectedCourse.id, courseProgramId: selectedCourseProgram?.id, assignmentId: matchedAssignmentId ?? assignmentId, importPreview })
        }}
      />
      {scopeProgramId && scopeCohortId && onBulkImportRequested && (
        <BulkImportSyllabusDialog
          open={bulkImportOpen}
          onClose={() => setBulkImportOpen(false)}
          programId={scopeProgramId}
          cohortId={scopeCohortId}
          cohortName={scopedCoursePrograms.find((item) => item.cohortId === scopeCohortId)?.cohortName ?? `Cohort ${scopeCohortId}`}
          allowedCourseCodes={allowedCourseIds ? availableCourses.map((course) => course.courseCode) : undefined}
          onStart={onBulkImportRequested}
        />
      )}
    </>
  )
}
