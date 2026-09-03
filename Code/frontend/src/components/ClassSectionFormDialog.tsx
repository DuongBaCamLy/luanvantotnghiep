import {
  useEffect,
  useMemo,
  useState,
} from "react"
import { useQuery } from "@tanstack/react-query"
import {
  BookOpenText,
  LockKeyhole,
} from "lucide-react"

import {
  type ClassSectionResponse,
  type CreateClassSectionRequest,
} from "@/api/classSectionApi"
import { courseApi } from "@/api/courseApi"
import { programApi } from "@/api/programApi"
import { cohortApi } from "@/api/cohortApi"
import { courseProgramApi } from "@/api/courseProgramApi"
import { instructorApi } from "@/api/instructorApi"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

interface ClassSectionFormDialogProps {
  open: boolean
  onOpenChange:
    (open: boolean) => void
  initialData?:
    ClassSectionResponse | null
  onSubmit:
    (
      data:
        CreateClassSectionRequest,
    ) => void
  isLoading?: boolean
}

const getDefaultAcademicYear = () => {
  const now = new Date()
  const year = now.getFullYear()
  const startYear =
    now.getMonth() >= 7
      ? year
      : year - 1

  return `${startYear}-${startYear + 1}`
}

export default function ClassSectionFormDialog({
  open,
  onOpenChange,
  initialData,
  onSubmit,
  isLoading = false,
}: ClassSectionFormDialogProps) {
  const [courseId, setCourseId] = useState("")
  const [programId, setProgramId] = useState("")
  const [cohortId, setCohortId] = useState("")
  const [instructorId, setInstructorId] = useState("")
  const [semester, setSemester] = useState("1")
  const [academicYear, setAcademicYear] = useState(
    getDefaultAcademicYear(),
  )
  const [, setGroupNumber] = useState("1")
  const [sectionType, setSectionType] =
    useState<"THEORY" | "LAB" | "COMBINED">("THEORY")
  const [isActive, setIsActive] = useState(true)
  const [validationError, setValidationError] = useState("")

  const {
    data: allCourses = [],
  } = useQuery({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
    enabled: open,
  })

  const { data: programs = [] } = useQuery({
    queryKey: ["programs", "teaching-assignment"],
    queryFn: programApi.getAll,
    enabled: open,
  })

  const { data: cohorts = [] } = useQuery({
    queryKey: ["cohorts", "teaching-assignment"],
    queryFn: cohortApi.getAll,
    enabled: open,
  })

  const { data: curriculumItems = [] } = useQuery({
    queryKey: ["course-programs", "curriculum", programId, cohortId],
    queryFn: () => courseProgramApi.getCurriculum(Number(programId), Number(cohortId)),
    enabled: open && Boolean(programId && cohortId),
  })

  const availableCohorts = cohorts.filter(
    (cohort) => String(cohort.programId) === programId,
  )
  const curriculumCourseIds = new Set(curriculumItems.map((item) => item.courseId))
  const courses = allCourses.filter((course) => curriculumCourseIds.has(course.id))

  const {
    data: instructors = [],
  } = useQuery({
    queryKey: ["instructors"],
    queryFn: instructorApi.getAll,
    enabled: open,
  })

  const activeInstructors =
    useMemo(
      () =>
        instructors
          .filter(
            (instructor) =>
              instructor.isActive
              || instructor.id === initialData?.instructorId,
          )
          .slice()
          .sort(
            (left, right) =>
              left.fullName.localeCompare(
                right.fullName,
                "vi",
              ),
          ),
      [
        initialData?.instructorId,
        instructors,
      ],
    )

  const hasLinkedSyllabus =
    Boolean(
      initialData?.syllabusId,
    )

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialData) {
      setProgramId(initialData.programId ? String(initialData.programId) : "")
      setCohortId(initialData.cohortId ? String(initialData.cohortId) : "")
      setCourseId(
        String(initialData.courseId),
      )
      setInstructorId(
        String(initialData.instructorId),
      )
      setSemester(
        String(initialData.semester),
      )
      setAcademicYear(
        initialData.academicYear,
      )
      setGroupNumber(
        String(initialData.groupNumber),
      )
      setSectionType(
        initialData.sectionType,
      )
      setIsActive(
        initialData.isActive,
      )
    } else {
      setProgramId("")
      setCohortId("")
      setCourseId("")
      setInstructorId("")
      setSemester("1")
      setAcademicYear(
        getDefaultAcademicYear(),
      )
      setGroupNumber("1")
      setSectionType("THEORY")
      setIsActive(true)
    }

    setValidationError("")
  }, [
    initialData,
    open,
  ])

  const handleSubmit = (
    event: React.FormEvent,
  ) => {
    event.preventDefault()

    if (
      !courseId
      || !programId
      || !cohortId
      || !instructorId
    ) {
      setValidationError(
        "Program, Cohort, Course, and Instructor are required.",
      )
      return
    }

    setValidationError("")

    onSubmit({
      programId: Number(programId),
      cohortId: Number(cohortId),
      courseId:
        Number(courseId),

      syllabusId:
        initialData?.syllabusId
        ?? null,

      instructorId:
        Number(instructorId),

      semester:
        Number(semester),

      academicYear:
        academicYear.trim(),

      groupNumber:
        initialData?.groupNumber ?? 1,

      labGroup:
        initialData?.labGroup
        ?? undefined,

      maxStudents:
        initialData?.maxStudents
        ?? undefined,

      room:
        initialData?.room
        ?? undefined,

      schedule:
        initialData?.schedule
        ?? undefined,

      sectionType:
        sectionType,
      isActive:
        isActive,
    })
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(nextOpen) => {
        if (!isLoading) {
          onOpenChange(nextOpen)
        }
      }}
    >
      <DialogContent className="overflow-hidden p-0 sm:max-w-[760px]">
        <div className="border-b border-[#d7e5e8] bg-[#f7fbfb] px-6 py-5">
          <DialogHeader className="text-left">
            <DialogTitle className="text-xl text-[#17343d]">
              {initialData
                ? "Edit Teaching Assignment"
                : "Add Teaching Assignment"}
            </DialogTitle>

            <DialogDescription>
              {initialData
                ? "Update the Program, Cohort, Course, and Instructor assignment."
                : "Select the curriculum context and assign an Instructor to its Course."}
            </DialogDescription>
          </DialogHeader>
        </div>

        <form
          onSubmit={handleSubmit}
        >
          <div className="space-y-5 px-6 py-5">
            {hasLinkedSyllabus ? (
              <div className="flex gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800">
                <LockKeyhole className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    Syllabus already linked
                  </p>

                  <p className="mt-1 leading-6">
                    This assignment is already linked to a syllabus. Its Course and Instructor relationship is preserved for workflow history.
                  </p>
                </div>
              </div>
            ) : (
              <div className="flex gap-3 rounded-xl border border-amber-100 bg-amber-50 p-4 text-sm text-amber-800">
                <BookOpenText className="mt-0.5 size-5 shrink-0" />

                <div>
                  <p className="font-semibold">
                    Syllabus will be created by Faculty
                  </p>

                  <p className="mt-1 leading-6">
                    This assignment authorizes the selected Instructor for exactly one Program, Cohort, and Course context.
                  </p>
                </div>
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>Program *</Label>
                <Select
                  value={programId}
                  onValueChange={(value) => {
                    setProgramId(value)
                    setCohortId("")
                    setCourseId("")
                    setValidationError("")
                  }}
                  disabled={isLoading || hasLinkedSyllabus}
                >
                  <SelectTrigger><SelectValue placeholder="Select Program" /></SelectTrigger>
                  <SelectContent>
                    {programs.filter((program) => program.isActive || program.id === initialData?.programId).map((program) => (
                      <SelectItem key={program.id} value={String(program.id)}>
                        {program.code} — {program.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>Cohort *</Label>
                <Select
                  value={cohortId}
                  onValueChange={(value) => {
                    setCohortId(value)
                    setCourseId("")
                    setValidationError("")
                  }}
                  disabled={isLoading || hasLinkedSyllabus || !programId}
                >
                  <SelectTrigger><SelectValue placeholder={programId ? "Select Cohort" : "Select Program first"} /></SelectTrigger>
                  <SelectContent>
                    {availableCohorts.filter((cohort) => cohort.isActive || cohort.id === initialData?.cohortId).map((cohort) => (
                      <SelectItem key={cohort.id} value={String(cohort.id)}>{cohort.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>
                  Course *
                </Label>

                <Select
                  value={courseId}
                  onValueChange={(value) => {
                    setCourseId(value)
                    setValidationError("")
                  }}
                  disabled={
                    isLoading
                    || hasLinkedSyllabus
                    || !cohortId
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select Course" />
                  </SelectTrigger>

                  <SelectContent>
                    {courses.map(
                      (course: any) => (
                        <SelectItem
                          key={course.id}
                          value={String(course.id)}
                        >
                          {course.courseCode}
                          {" — "}
                          {course.name}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>
                  Instructor *
                </Label>

                <Select
                  value={instructorId}
                  onValueChange={(value) => {
                    setInstructorId(value)
                    setValidationError("")
                  }}
                  disabled={
                    isLoading
                    || hasLinkedSyllabus
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select Instructor" />
                  </SelectTrigger>

                  <SelectContent>
                    {activeInstructors.map(
                      (instructor) => (
                        <SelectItem
                          key={instructor.id}
                          value={String(instructor.id)}
                        >
                          {instructor.staffCode}
                          {" — "}
                          {instructor.fullName}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>

                <p className="text-xs text-slate-500">
                  Only active instructor profiles are available for new assignments.
                </p>
              </div>

            </div>

            {validationError && (
              <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {validationError}
              </div>
            )}
          </div>

          <DialogFooter className="border-t bg-slate-50 px-6 py-4">
            <Button
              type="button"
              variant="outline"
              disabled={isLoading}
              onClick={() =>
                onOpenChange(false)
              }
            >
              Cancel
            </Button>

            <Button
              type="submit"
              className="bg-[#007d84] text-white hover:bg-[#006d73]"
              disabled={isLoading}
            >
              {isLoading
                ? "Saving..."
                : initialData
                  ? "Save Changes"
                  : "Create Assignment"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
