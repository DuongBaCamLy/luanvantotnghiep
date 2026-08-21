import {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react"
import { useMutation, useQuery } from "@tanstack/react-query"
import {
  BookOpen,
  Info,
  LoaderCircle,
  Plus,
  Save,
  ShieldCheck,
} from "lucide-react"

import { courseApi } from "@/api/courseApi"
import { departmentApi } from "@/api/departmentApi"
import { syllabusApi } from "@/api/syllabusApi"
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import type { Course } from "@/types/course"
import type {
  CreateSyllabusRequest,
} from "@/types/syllabus"

interface Props {
  initialData?:
    CreateSyllabusRequest
  onSubmit: (
    values:
      CreateSyllabusRequest,
  ) => void
  loading?: boolean
  lockProgramContext?: boolean
  lockAssignmentContext?: boolean
  /** Automatically load the latest syllabus/template when a course is selected. */
  autoPrefillExisting?: boolean
  formId?: string
  submitLabel?: string
  allowCreateCourse?: boolean
}

const COURSE_TYPES = [
  "Compulsory",
  "Elective",
  "General",
] as const

const defaultAcademicYear = () => {
  const now = new Date()
  const startYear = now.getMonth() >= 7
    ? now.getFullYear()
    : now.getFullYear() - 1

  return `${startYear}-${startYear + 1}`
}

const SEMESTERS = [
  {
    value: "HK1",
    label: "Semester 1",
  },
  {
    value: "HK2",
    label: "Semester 2",
  },
  {
    value: "HK3",
    label: "Semester 3",
  },
  {
    value: "HK4",
    label: "Semester 4",
  },
  {
    value: "HK5",
    label: "Semester 5",
  },
  {
    value: "HK6",
    label: "Semester 6",
  },
  {
    value: "HK7",
    label: "Semester 7",
  },
  {
    value: "HK8",
    label: "Semester 8",
  },
  {
    value: "SUMMER",
    label: "Summer Semester",
  },
] as const

const parseStringArray = (
  value: unknown,
): string[] => {
  if (Array.isArray(value)) {
    return value
      .map(String)
      .filter(Boolean)
  }

  if (
    typeof value
    !== "string"
    || !value.trim()
  ) {
    return []
  }

  try {
    const parsed:
      unknown =
        JSON.parse(value)

    return Array.isArray(
      parsed,
    )
      ? parsed
          .map(String)
          .filter(Boolean)
      : []
  } catch {
    return value
      .split(",")
      .map(
        (item) =>
          item.trim(),
      )
      .filter(Boolean)
  }
}

const normalizeSemester = (
  value: unknown,
) => {
  const text =
    String(value ?? "")
      .trim()
      .toUpperCase()

  if (!text) {
    return ""
  }

  if (
    text === "SUMMER"
  ) {
    return "SUMMER"
  }

  const matched =
    text.match(
      /(?:HK|SEMESTER)?\s*([1-8])/,
    )

  return matched
    ? `HK${matched[1]}`
    : text
}

const courseLabel = (
  course: Course,
) =>
  `${course.courseCode} — ${course.name}`

export default function SyllabusForm({
  initialData,
  onSubmit,
  loading = false,
  lockProgramContext = false,
  lockAssignmentContext = false,
  autoPrefillExisting = true,
  formId,
  submitLabel = "Save Draft Metadata",
  allowCreateCourse = false,
}: Props) {
  const [
    courses,
    setCourses,
  ] =
    useState<Course[]>([])

  const [
    coursesLoading,
    setCoursesLoading,
  ] =
    useState(true)

  const [
    coursesError,
    setCoursesError,
  ] =
    useState(false)

  const [newCourseOpen, setNewCourseOpen] = useState(false)
  const [newCourseCode, setNewCourseCode] = useState("")
  const [newCourseName, setNewCourseName] = useState("")
  const [newCourseNameVn, setNewCourseNameVn] = useState("")
  const [newCourseDepartmentId, setNewCourseDepartmentId] = useState("")
  const [newCourseTheory, setNewCourseTheory] = useState("3")
  const [newCourseLab, setNewCourseLab] = useState("0")
  const [newCourseLevel, setNewCourseLevel] = useState("INTRODUCTORY")

  const { data: departments = [] } = useQuery({
    queryKey: ["departments", "syllabus-new-course"],
    queryFn: departmentApi.getAll,
    enabled: allowCreateCourse && newCourseOpen,
  })

  const createCourseMutation = useMutation({
    mutationFn: courseApi.create,
    onSuccess: (created) => {
      setCourses((current) => [...current, created])
      setCourseId(String(created.id))
      setNewCourseOpen(false)
    },
  })

  const [
    courseId,
    setCourseId,
  ] =
    useState(
      initialData?.courseId
        ? String(
            initialData
              .courseId,
          )
        : "",
    )

  const [
    versionLabel,
    setVersionLabel,
  ] =
    useState(
      initialData
        ?.versionLabel
      || "v1.0",
    )

  const [
    academicYear,
    setAcademicYear,
  ] =
    useState(
      initialData
        ?.academicYear
      || defaultAcademicYear(),
    )

  const [
    semester,
    setSemester,
  ] =
    useState(
      normalizeSemester(initialData?.semester)
      || "HK1",
    )

  const [
    major,
    setMajor,
  ] =
    useState(
      initialData?.major
      || "",
    )

  const [
    courseDesignation,
    setCourseDesignation,
  ] =
    useState(
      initialData
        ?.courseDesignation
      || "",
    )

  const [
    courseTypes,
    setCourseTypes,
  ] =
    useState<string[]>(
      parseStringArray(
        initialData
          ?.courseTypes,
      ),
    )

  const [
    language,
    setLanguage,
  ] =
    useState(
      initialData?.language
      || "",
    )

  const [
    relation,
    setRelation,
  ] =
    useState(
      initialData?.relation
      || "",
    )

  const [
    teachingMethods,
    setTeachingMethods,
  ] =
    useState(
      initialData
        ?.teachingMethods
      || "",
    )

  const [
    workloadTotal,
    setWorkloadTotal,
  ] =
    useState(
      initialData
        ?.workloadTotal
      || "",
    )

  const [
    workloadContact,
    setWorkloadContact,
  ] =
    useState(
      initialData
        ?.workloadContact
      || "",
    )

  const [
    workloadPrivate,
    setWorkloadPrivate,
  ] =
    useState(
      initialData
        ?.workloadPrivate
      || "",
    )

  const [
    prerequisites,
    setPrerequisites,
  ] =
    useState(
      initialData
        ?.prerequisites
      || "",
    )

  const [
    objectives,
    setObjectives,
  ] =
    useState(
      initialData
        ?.objectives
      || "",
    )

  const [
    examForms,
    setExamForms,
  ] =
    useState(
      initialData
        ?.examForms
      || "",
    )

  const [
    examRequirements,
    setExamRequirements,
  ] =
    useState(
      initialData
        ?.examRequirements
      || "",
    )

  const [
    changeSummary,
    setChangeSummary,
  ] =
    useState(
      initialData
        ?.changeSummary
      || "",
    )

  const [
    formError,
    setFormError,
  ] =
    useState<string | null>(
      null,
    )

  const [prefilledContent, setPrefilledContent] = useState({
    clos: initialData?.clos,
    topics: initialData?.topics,
    assessments: initialData?.assessments,
  })

  useEffect(() => {
    setPrefilledContent({
      clos: initialData?.clos,
      topics: initialData?.topics,
      assessments: initialData?.assessments,
    })
  }, [initialData])

  useEffect(() => {
    let cancelled = false

    setCoursesLoading(true)
    setCoursesError(false)

    courseApi
      .getAll()
      .then((data) => {
        if (!cancelled) {
          setCourses(data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCoursesError(true)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setCoursesLoading(
            false,
          )
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const selectedCourse =
    useMemo(
      () =>
        courses.find(
          (course) =>
            course.id
            === Number(
              courseId,
            ),
        ),
      [
        courseId,
        courses,
      ],
    )

  const prefetchedCourseIds = useRef<Set<number>>(new Set())

  useEffect(() => {
    const id = Number(courseId)
    if (!autoPrefillExisting || !id || !Number.isFinite(id)) return
    if (prefetchedCourseIds.current.has(id)) return

    // If the caller already supplied a real syllabus/template, keep it authoritative.
    const hasExistingTemplate = Boolean(
      initialData && (
        initialData.language
        || initialData.relation
        || initialData.teachingMethods
        || initialData.objectives
        || initialData.prerequisites
        || (initialData.clos?.length ?? 0) > 0
        || (initialData.topics?.length ?? 0) > 0
        || (initialData.assessments?.length ?? 0) > 0
      )
    )
    if (hasExistingTemplate) {
      prefetchedCourseIds.current.add(id)
      return
    }

    prefetchedCourseIds.current.add(id)
    let cancelled = false

    syllabusApi.getCreateContext(id)
      .then((context) => {
        if (cancelled) return
        const template = context.latestApprovedSyllabus ?? context.latestSyllabus
        if (!template) return

        setCourseDesignation(template.courseDesignation ?? "")
        setCourseTypes(parseStringArray(template.courseTypes))
        setSemester(normalizeSemester(template.semester))
        setMajor(template.major ?? "")
        setLanguage(template.language ?? "")
        setRelation(template.relation ?? "")
        setTeachingMethods(template.teachingMethods ?? "")
        setWorkloadTotal(template.workloadTotal ?? "")
        setWorkloadContact(template.workloadContact ?? "")
        setWorkloadPrivate(template.workloadPrivate ?? "")
        setPrerequisites(template.prerequisites ?? "")
        setObjectives(template.objectives ?? "")
        setExamForms(template.examForms ?? "")
        setExamRequirements(template.examRequirements ?? "")
        setPrefilledContent({
          clos: template.clos ?? [],
          topics: template.topics ?? [],
          assessments: template.assessments ?? [],
        })
        setChangeSummary(`Created from ${template.versionLabel || `v${template.versionNumber}`} template`)
      })
      .catch(() => {
        // Prefill is an enhancement; the form remains usable when no previous syllabus exists.
      })

    return () => { cancelled = true }
  }, [autoPrefillExisting, courseId, initialData])

  const assignmentLocked =
    lockAssignmentContext

  const courseLocked =
    lockProgramContext
    || assignmentLocked

  const contextLocked =
    assignmentLocked

  const toggleCourseType =
    (
      value: string,
    ) => {
      setCourseTypes(
        (current) =>
          current.includes(value)
            ? current.filter(
                (item) =>
                  item !== value,
              )
            : [
                ...current,
                value,
              ],
      )
    }

  const handleSubmit =
    (
      event:
        React.FormEvent,
    ) => {
      event.preventDefault()

      if (!courseId) {
        setFormError(
          "Select a course before saving the Draft metadata.",
        )
        return
      }

      if (
        !academicYear.trim()
      ) {
        setFormError(
          "Academic Year is required. Do not enter Curriculum Cohort in this field.",
        )
        return
      }

      if (
        !semester.trim()
      ) {
        setFormError(
          "Semester is required.",
        )
        return
      }

      setFormError(null)

      const payload:
        CreateSyllabusRequest = {
          ...initialData,
          courseId:
            Number(courseId),
          versionNumber:
            Number(
              initialData
                ?.versionNumber
              || 1,
            ),
          versionLabel:
            versionLabel.trim()
            || "v1.0",
          academicYear:
            academicYear.trim(),
          semester:
            semester.trim(),
          major:
            major.trim(),
          courseDesignation:
            courseDesignation
              .trim(),
          courseTypes:
            JSON.stringify(
              courseTypes,
            ),
          language:
            language.trim(),
          relation:
            relation.trim(),
          teachingMethods:
            teachingMethods
              .trim(),
          workloadTotal:
            workloadTotal
              .trim(),
          workloadContact:
            workloadContact
              .trim(),
          workloadPrivate:
            workloadPrivate
              .trim(),
          prerequisites:
            prerequisites
              .trim(),
          objectives:
            objectives.trim(),
          examForms:
            examForms.trim(),
          examRequirements:
            examRequirements
              .trim(),
          changeSummary:
            changeSummary
              .trim(),
          notes:
            initialData?.notes
            ?? "",

          // Structured academic content is edited in SyllabusEditorPage.
          // Preserve existing collections instead of replacing them with
          // legacy SLO / CLO1-CLO3 matrices.
          clos:
            prefilledContent.clos,
          topics:
            prefilledContent.topics,
          assessments:
            prefilledContent.assessments,
          rubrics:
            initialData?.rubrics
            ?? "",
        }

      onSubmit(payload)
    }

  const handleCreateCourse = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!newCourseCode.trim() || !newCourseName.trim() || !newCourseNameVn.trim() || !newCourseDepartmentId) {
      return
    }

    createCourseMutation.mutate({
      courseCode: newCourseCode.trim(),
      name: newCourseName.trim(),
      nameVn: newCourseNameVn.trim(),
      departmentId: Number(newCourseDepartmentId),
      creditTheory: Number(newCourseTheory || 0),
      creditLab: Number(newCourseLab || 0),
      courseLevel: newCourseLevel,
    })
  }

  return (
    <>
    <form
      id={formId}
      onSubmit={
        handleSubmit
      }
      className="space-y-6"
    >
      <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-5">
          <div className="flex items-start gap-3">
            <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-[#eaf7f7] text-[#007d84]">
              <BookOpen className="size-5" />
            </span>

            <div>
              <h2 className="font-bold text-[#17343d]">
                Draft Metadata
              </h2>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                Save the syllabus shell and general metadata here. CLOs, CLO–PLO mapping, teaching topics, assessments, and reading list belong to the structured Syllabus Editor.
              </p>
            </div>
          </div>
        </div>

        <div className="space-y-6 p-6">
          <div className="rounded-xl border border-blue-100 bg-blue-50/50 px-4 py-3">
            <div className="flex items-start gap-2">
              <Info className="mt-0.5 size-4 shrink-0 text-blue-700" />

              <p className="text-xs leading-5 text-blue-800">
                <strong>Academic Year and Curriculum Cohort are different concepts.</strong>{" "}
                Enter the teaching academic year here, for example 2024-2025. Program/Cohort and PLO scope are handled by curriculum data and the structured editor.
              </p>
            </div>
          </div>

          {formError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
              {formError}
            </div>
          )}

          <div className="grid gap-5 md:grid-cols-2">
            <FieldGroup
              label="Course"
              required
            >
              {coursesLoading ? (
                <div className="flex h-10 items-center rounded-md border border-slate-200 bg-slate-50 px-3 text-sm text-slate-500">
                  <LoaderCircle className="mr-2 size-4 animate-spin" />
                  Loading courses...
                </div>
              ) : coursesError ? (
                <div className="flex h-10 items-center rounded-md border border-rose-200 bg-rose-50 px-3 text-sm text-rose-700">
                  Unable to load course list.
                </div>
              ) : (
                <div>
                <Select
                  value={
                    courseId
                  }
                  onValueChange={
                    setCourseId
                  }
                  disabled={
                    courseLocked
                  }
                >
                  <SelectTrigger className="bg-white">
                    <SelectValue placeholder="Select course" />
                  </SelectTrigger>

                  <SelectContent>
                    {courses.map(
                      (course) => (
                        <SelectItem
                          key={course.id}
                          value={String(
                            course.id,
                          )}
                        >
                          {courseLabel(
                            course,
                          )}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>

                {allowCreateCourse && !courseLocked && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="mt-2"
                    onClick={() => setNewCourseOpen(true)}
                  >
                    <Plus className="size-3.5" />
                    Create a new course
                  </Button>
                )}
                </div>
              )}
            </FieldGroup>

            <FieldGroup
              label="Version"
            >
              <Input
                value={
                  versionLabel
                }
                onChange={(
                  event,
                ) =>
                  setVersionLabel(
                    event.target
                      .value,
                  )
                }
                disabled={
                  assignmentLocked
                }
                placeholder="v1.0"
              />
            </FieldGroup>

            <FieldGroup
              label="Academic Year"
              required
            >
              <Input
                value={
                  academicYear
                }
                onChange={(
                  event,
                ) =>
                  setAcademicYear(
                    event.target
                      .value,
                  )
                }
                disabled={
                  contextLocked
                }
                placeholder="2024-2025"
              />
            </FieldGroup>

            <FieldGroup
              label="Semester"
              required
            >
              <Select
                value={
                  semester
                }
                onValueChange={
                  setSemester
                }
                disabled={
                  contextLocked
                }
              >
                <SelectTrigger className="bg-white">
                  <SelectValue placeholder="Select semester" />
                </SelectTrigger>

                <SelectContent>
                  {SEMESTERS.map(
                    (option) => (
                      <SelectItem
                        key={option.value}
                        value={option.value}
                      >
                        {option.label}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>
            </FieldGroup>
          </div>

          {selectedCourse && (
            <div className="grid gap-3 rounded-xl border border-slate-100 bg-slate-50/70 p-4 sm:grid-cols-2 xl:grid-cols-4">
              <ReadOnlyValue
                label="Course Code"
                value={
                  selectedCourse
                    .courseCode
                }
              />

              <ReadOnlyValue
                label="English Name"
                value={
                  selectedCourse
                    .name
                }
              />

              <ReadOnlyValue
                label="Theory Credits"
                value={String(
                  selectedCourse
                    .creditTheory
                  ?? 0,
                )}
              />

              <ReadOnlyValue
                label="Lab / Practice Credits"
                value={String(
                  selectedCourse
                    .creditLab
                  ?? 0,
                )}
              />
            </div>
          )}

          <div className="grid gap-5 md:grid-cols-2">
            <FieldGroup
              label="Major / Academic Area"
            >
              <Input
                value={major}
                onChange={(
                  event,
                ) =>
                  setMajor(
                    event.target
                      .value,
                  )
                }
                placeholder="Optional metadata"
              />
            </FieldGroup>

            <FieldGroup
              label="Language of Instruction"
            >
              <Input
                value={language}
                onChange={(
                  event,
                ) =>
                  setLanguage(
                    event.target
                      .value,
                  )
                }
                placeholder="e.g. English"
              />
            </FieldGroup>
          </div>

          <FieldGroup
            label="Course Designation"
          >
            <TextArea
              value={
                courseDesignation
              }
              onChange={
                setCourseDesignation
              }
              placeholder="Course designation or brief classification."
            />
          </FieldGroup>

          <FieldGroup
            label="Course Type"
          >
            <div className="flex flex-wrap gap-3">
              {COURSE_TYPES.map(
                (type) => (
                  <label
                    key={type}
                    className={
                      courseTypes
                        .includes(type)
                        ? "flex cursor-pointer items-center gap-2 rounded-lg border border-[#7bc5c8] bg-[#f1fbfb] px-3 py-2 text-sm font-medium text-[#006f75]"
                        : "flex cursor-pointer items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600"
                    }
                  >
                    <input
                      type="checkbox"
                      checked={
                        courseTypes
                          .includes(
                            type,
                          )
                      }
                      onChange={() =>
                        toggleCourseType(
                          type,
                        )
                      }
                    />
                    {type}
                  </label>
                ),
              )}
            </div>
          </FieldGroup>

          <div className="grid gap-5 md:grid-cols-2">
            <FieldGroup
              label="Relation to Curriculum"
            >
              <TextArea
                value={relation}
                onChange={
                  setRelation
                }
                placeholder="How the course contributes to the curriculum."
              />
            </FieldGroup>

            <FieldGroup
              label="Teaching Methods"
            >
              <TextArea
                value={
                  teachingMethods
                }
                onChange={
                  setTeachingMethods
                }
                placeholder="Lecture, laboratory, project, discussion..."
              />
            </FieldGroup>
          </div>

          <section className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">
            <h3 className="text-sm font-semibold text-slate-800">
              Workload
            </h3>

            <div className="mt-4 grid gap-4 md:grid-cols-3">
              <FieldGroup
                label="Total Workload"
              >
                <Input
                  value={
                    workloadTotal
                  }
                  onChange={(
                    event,
                  ) =>
                    setWorkloadTotal(
                      event.target
                        .value,
                    )
                  }
                />
              </FieldGroup>

              <FieldGroup
                label="Contact Hours"
              >
                <Input
                  value={
                    workloadContact
                  }
                  onChange={(
                    event,
                  ) =>
                    setWorkloadContact(
                      event.target
                        .value,
                    )
                  }
                />
              </FieldGroup>

              <FieldGroup
                label="Private Study"
              >
                <Input
                  value={
                    workloadPrivate
                  }
                  onChange={(
                    event,
                  ) =>
                    setWorkloadPrivate(
                      event.target
                        .value,
                    )
                  }
                />
              </FieldGroup>
            </div>
          </section>

          <FieldGroup
            label="Prerequisites / Related Courses"
          >
            <TextArea
              value={
                prerequisites
              }
              onChange={
                setPrerequisites
              }
              placeholder="Describe prerequisite, corequisite, or recommended course requirements."
            />
          </FieldGroup>

          <FieldGroup
            label="Course Objectives"
          >
            <TextArea
              value={objectives}
              onChange={
                setObjectives
              }
              placeholder="High-level course objectives. Detailed measurable outcomes belong in the CLO section of the editor."
            />
          </FieldGroup>

          <div className="grid gap-5 md:grid-cols-2">
            <FieldGroup
              label="Examination Forms"
            >
              <TextArea
                value={examForms}
                onChange={
                  setExamForms
                }
                placeholder="Written exam, project, oral presentation..."
              />
            </FieldGroup>

            <FieldGroup
              label="Study & Examination Requirements"
            >
              <TextArea
                value={
                  examRequirements
                }
                onChange={
                  setExamRequirements
                }
                placeholder="Attendance, eligibility, minimum requirements..."
              />
            </FieldGroup>
          </div>

          <FieldGroup
            label="Change Summary"
          >
            <Input
              value={
                changeSummary
              }
              onChange={(
                event,
              ) =>
                setChangeSummary(
                  event.target
                    .value,
                )
              }
              placeholder="e.g. Initial Draft or metadata update"
            />
          </FieldGroup>
        </div>
      </section>

      <section className="rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

          <div>
            <p className="font-semibold text-[#17343d]">
              Submission validation happens later
            </p>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              Draft metadata may be saved while incomplete. Before submission, the Syllabus Editor and backend validation must check required General Information, CLOs, CLO–PLO mapping, teaching topics, assessment plan, and reading list.
            </p>
          </div>
        </div>
      </section>

      <div className="flex justify-end">
        <Button
          type="submit"
          size="lg"
          disabled={
            loading
            || coursesLoading
          }
          className="bg-[#007d84] text-white hover:bg-[#006d73]"
        >
          {loading ? (
            <>
              <LoaderCircle className="size-4 animate-spin" />
              Saving Draft...
            </>
          ) : (
            <>
              <Save className="size-4" />
              {submitLabel}
            </>
          )}
        </Button>
      </div>
    </form>

    <Dialog open={newCourseOpen} onOpenChange={setNewCourseOpen}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Create New Course</DialogTitle>
          <DialogDescription>
            Create the course record first, then it will be selected for this new syllabus.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleCreateCourse} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Input placeholder="Course code" value={newCourseCode} onChange={(event) => setNewCourseCode(event.target.value)} required />
            <Select value={newCourseDepartmentId} onValueChange={setNewCourseDepartmentId}>
              <SelectTrigger><SelectValue placeholder="Department" /></SelectTrigger>
              <SelectContent>
                {departments.map((department) => (
                  <SelectItem key={department.id} value={String(department.id)}>
                    {department.code} - {department.nameVn || department.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Input placeholder="English course name" value={newCourseName} onChange={(event) => setNewCourseName(event.target.value)} required />
          <Input placeholder="Vietnamese course name" value={newCourseNameVn} onChange={(event) => setNewCourseNameVn(event.target.value)} required />
          <div className="grid gap-4 sm:grid-cols-3">
            <Input type="number" min={0} placeholder="Theory credits" value={newCourseTheory} onChange={(event) => setNewCourseTheory(event.target.value)} />
            <Input type="number" min={0} placeholder="Lab credits" value={newCourseLab} onChange={(event) => setNewCourseLab(event.target.value)} />
            <Select value={newCourseLevel} onValueChange={setNewCourseLevel}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="INTRODUCTORY">Introductory</SelectItem>
                <SelectItem value="INTERMEDIATE">Intermediate</SelectItem>
                <SelectItem value="ADVANCED">Advanced</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {createCourseMutation.isError && (
            <p className="text-sm text-rose-600">
              {(createCourseMutation.error as { response?: { data?: { message?: string } } })?.response?.data?.message || "Unable to create the course."}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setNewCourseOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={createCourseMutation.isPending || departments.length === 0}>
              {createCourseMutation.isPending ? "Creating..." : "Create Course"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
    </>
  )
}

function FieldGroup({
  label,
  required = false,
  children,
}: {
  label: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <div>
      <label className="mb-1.5 block text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
        {label}
        {required && (
          <span className="ml-1 text-rose-600">
            *
          </span>
        )}
      </label>

      {children}
    </div>
  )
}

function ReadOnlyValue({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-slate-400">
        {label}
      </p>

      <p className="mt-1 text-sm font-semibold text-slate-800">
        {value || "Not recorded"}
      </p>
    </div>
  )
}

function TextArea({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (
    value: string,
  ) => void
  placeholder?: string
}) {
  return (
    <textarea
      value={value}
      onChange={(
        event,
      ) =>
        onChange(
          event.target.value,
        )
      }
      placeholder={
        placeholder
      }
      className="min-h-[92px] w-full resize-y rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 outline-none transition focus:border-[#49aeb2] focus:ring-2 focus:ring-[#49aeb2]/15"
    />
  )
}