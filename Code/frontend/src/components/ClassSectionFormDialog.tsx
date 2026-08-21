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
import { Input } from "@/components/ui/input"
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

const isValidAcademicYear = (
  value: string,
) =>
  /^\d{4}-\d{4}$/.test(value)

export default function ClassSectionFormDialog({
  open,
  onOpenChange,
  initialData,
  onSubmit,
  isLoading = false,
}: ClassSectionFormDialogProps) {
  const [courseId, setCourseId] = useState("")
  const [instructorId, setInstructorId] = useState("")
  const [semester, setSemester] = useState("1")
  const [academicYear, setAcademicYear] = useState(
    getDefaultAcademicYear(),
  )
  const [groupNumber, setGroupNumber] = useState("1")
  const [sectionType, setSectionType] =
    useState<"THEORY" | "LAB" | "COMBINED">("THEORY")
  const [isActive, setIsActive] = useState(true)
  const [validationError, setValidationError] = useState("")

  const {
    data: courses = [],
  } = useQuery({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
    enabled: open,
  })

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

  const academicYearOptions =
    useMemo(() => {
      const now = new Date()
      const baseYear =
        now.getMonth() >= 7
          ? now.getFullYear()
          : now.getFullYear() - 1

      const values = new Set<string>()

      for (
        let offset = -2;
        offset <= 2;
        offset++
      ) {
        const start = baseYear + offset
        values.add(`${start}-${start + 1}`)
      }

      if (initialData?.academicYear) {
        values.add(initialData.academicYear)
      }

      return Array.from(values).sort(
        (left, right) =>
          right.localeCompare(
            left,
            "en",
            {
              numeric: true,
            },
          ),
      )
    }, [
      initialData?.academicYear,
    ])

  const semesterOptions =
    useMemo(() => {
      const values = new Set<number>([
        1,
        2,
        3,
      ])

      if (initialData?.semester) {
        values.add(initialData.semester)
      }

      return Array.from(values).sort(
        (left, right) =>
          left - right,
      )
    }, [
      initialData?.semester,
    ])

  const hasLinkedSyllabus =
    Boolean(
      initialData?.syllabusId,
    )

  useEffect(() => {
    if (!open) {
      return
    }

    if (initialData) {
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
      || !instructorId
      || !semester
      || !academicYear.trim()
      || !groupNumber
    ) {
      setValidationError(
        "Course, Instructor, Semester, Academic Year, and Group are required.",
      )
      return
    }

    if (
      !isValidAcademicYear(
        academicYear.trim(),
      )
    ) {
      setValidationError(
        "Academic Year must use the format YYYY-YYYY, for example 2026-2027.",
      )
      return
    }

    const startYear =
      Number(
        academicYear.slice(
          0,
          4,
        ),
      )

    const endYear =
      Number(
        academicYear.slice(
          5,
          9,
        ),
      )

    if (
      endYear
      !== startYear + 1
    ) {
      setValidationError(
        "Academic Year must represent two consecutive years, for example 2026-2027.",
      )
      return
    }

    const parsedGroup =
      Number(groupNumber)

    if (
      !Number.isInteger(parsedGroup)
      || parsedGroup < 1
    ) {
      setValidationError(
        "Group must be a positive whole number.",
      )
      return
    }

    setValidationError("")

    onSubmit({
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
        parsedGroup,

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

      sectionType,
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
                ? "Update this preserved teaching responsibility. If a syllabus is already linked, its authorization scope is locked."
                : "Assign an instructor first. The assignment starts without a syllabus; the assigned Faculty creates the Draft later."}
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
                    Course, instructor, semester, and academic year are locked because the linked syllabus relies on this assignment scope. Deactivate the assignment instead of deleting its history.
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
                    This assignment is the authorization source for FR-03.1. After saving it, the assigned Faculty can create a Draft for the same Course + Academic Year + Semester.
                  </p>
                </div>
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
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

              <div className="space-y-1.5">
                <Label>
                  Academic Year *
                </Label>

                <Select
                  value={academicYear}
                  onValueChange={(value) => {
                    setAcademicYear(value)
                    setValidationError("")
                  }}
                  disabled={
                    isLoading
                    || hasLinkedSyllabus
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    {academicYearOptions.map(
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
                  onValueChange={(value) => {
                    setSemester(value)
                    setValidationError("")
                  }}
                  disabled={
                    isLoading
                    || hasLinkedSyllabus
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    {semesterOptions.map(
                      (value) => (
                        <SelectItem
                          key={value}
                          value={String(value)}
                        >
                          Semester {value}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="section-group">
                  Group *
                </Label>

                <Input
                  id="section-group"
                  type="number"
                  min={1}
                  step={1}
                  value={groupNumber}
                  onChange={(event) => {
                    setGroupNumber(event.target.value)
                    setValidationError("")
                  }}
                  disabled={isLoading}
                />
              </div>

              <div className="space-y-1.5">
                <Label>
                  Class Type *
                </Label>

                <Select
                  value={sectionType}
                  onValueChange={(value) =>
                    setSectionType(
                      value as
                        | "THEORY"
                        | "LAB"
                        | "COMBINED",
                    )
                  }
                  disabled={isLoading}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value="THEORY">
                      Theory
                    </SelectItem>

                    <SelectItem value="LAB">
                      Lab
                    </SelectItem>

                    <SelectItem value="COMBINED">
                      Combined
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5 sm:col-span-2">
                <Label>
                  Assignment Status
                </Label>

                <Select
                  value={
                    isActive
                      ? "true"
                      : "false"
                  }
                  onValueChange={(value) =>
                    setIsActive(
                      value === "true",
                    )
                  }
                  disabled={isLoading}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value="true">
                      Active
                    </SelectItem>

                    <SelectItem value="false">
                      Inactive
                    </SelectItem>
                  </SelectContent>
                </Select>

                <p className="text-xs text-slate-500">
                  Inactive assignments remain in history but cannot authorize new syllabus creation.
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
