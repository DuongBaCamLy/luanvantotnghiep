import {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react"
import { useMutation, useQuery } from "@tanstack/react-query"
import {
  FileJson,
  Info,
  LoaderCircle,
  Plus,
  RotateCcw,
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
import StandardSyllabusFormSections, {
  parseStandardSyllabusNotes,
  serializeStandardSyllabusNotes,
} from "@/components/syllabus/StandardSyllabusFormSections"
import { CoursePrerequisitePicker } from "@/components/curriculum/CoursePrerequisitePicker"
import type { TopicImportData } from "@/types/syllabusImport"

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
  /** Render the same canonical form as a non-interactive syllabus snapshot. */
  readOnly?: boolean
  /** Identity extracted from an unmatched template while using a temporary DB course context. */
  previewCourseIdentity?: { code?: string; name?: string }
  /** Curriculum scope selected before this form was opened. Program remains internal. */
  curriculumContext?: { program: string; major: string; cohort: string }
  allowedCourseIds?: number[]
  /** Raw content rows from the active file preview, used to hydrate Weight/Level directly. */
  importedContentTopics?: TopicImportData[]
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
    value: "Semester 1",
    label: "Semester 1",
  },
  {
    value: "Semester 2",
    label: "Semester 2",
  },
  {
    value: "Semester 3",
    label: "Semester 3",
  },
  {
    value: "Semester 4",
    label: "Semester 4",
  },
  {
    value: "Semester 5",
    label: "Semester 5",
  },
  {
    value: "Semester 6",
    label: "Semester 6",
  },
  {
    value: "Semester 7",
    label: "Semester 7",
  },
  {
    value: "Semester 8",
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
      /^(?:HK|SEMESTER)?\s*([1-8])$/,
    )

  return matched
    ? `Semester ${matched[1]}`
    : ""
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
  submitLabel = "Lưu syllabus",
  allowCreateCourse = false,
  readOnly = false,
  previewCourseIdentity,
  curriculumContext,
  allowedCourseIds,
  importedContentTopics,
}: Props) {
  const coursesQuery = useQuery({
    queryKey: ["courses", "syllabus-form"],
    queryFn: courseApi.getAll,
  })

  const [createdCourses, setCreatedCourses] = useState<Course[]>([])

  const courses = useMemo(() => {
    const byId = new Map<number, Course>()
    const allowed = allowedCourseIds ? new Set(allowedCourseIds) : null
    for (const course of coursesQuery.data ?? []) {
      if (!allowed || allowed.has(course.id)) byId.set(course.id, course)
    }
    for (const course of createdCourses) byId.set(course.id, course)
    return Array.from(byId.values())
  }, [coursesQuery.data, createdCourses, allowedCourseIds])

  const coursesLoading = coursesQuery.isLoading
  const coursesError = coursesQuery.isError

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
      setCreatedCourses((current) => [...current, created])
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
      || "Semester 1",
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
    clos: initialData?.clos ?? [],
    topics: initialData?.topics ?? [],
    assessments: initialData?.assessments ?? [],
  })

  const [supplemental, setSupplemental] = useState(
    parseStandardSyllabusNotes(initialData?.notes),
  )

  useEffect(() => {
    if (!importedContentTopics?.length) return
    setPrefilledContent((current) => ({
      ...current,
      topics: current.topics.map((topic, index) => {
        const imported = importedContentTopics[index]
        if (!imported) return topic
        return {
          ...topic,
          contentWeight: imported.contentWeight ?? imported.teachingHours,
          contentLevel: imported.contentLevel ?? imported.teachingLevel,
          teachingLevel: imported.teachingLevel ?? imported.contentLevel,
        }
      }),
    }))
    setSupplemental((current) => ({
      ...current,
      topicDetails: {
        ...current.topicDetails,
        ...Object.fromEntries(importedContentTopics.map((topic, index) => {
          const existing = current.topicDetails[String(index)]
          return [String(index), {
            clo: existing?.clo ?? "",
            assessments: existing?.assessments ?? "",
            resources: existing?.resources ?? "",
            weight: existing?.weight || String(topic.contentWeight ?? topic.teachingHours ?? ""),
            level: existing?.level || String(topic.teachingLevel ?? topic.contentLevel ?? ""),
          }]
        })),
      },
    }))
  }, [importedContentTopics])

  const [creditFieldsEdited, setCreditFieldsEdited] = useState({
    creditPoints: false,
    lectureCredits: false,
    laboratoryCredits: false,
  })

  // The create context and an import preview can finish loading in different
  // renders. React state initializers only run on the first render, so hydrate
  // every imported field once whenever the actual initial payload changes.
  // The serialized signature prevents ordinary parent re-renders from
  // overwriting edits the user has already made in the form.
  const initialDataSignature = JSON.stringify(initialData ?? null)
  const hydratedInitialDataSignature = useRef(initialDataSignature)

  useEffect(() => {
    if (hydratedInitialDataSignature.current === initialDataSignature) return
    hydratedInitialDataSignature.current = initialDataSignature

    setCourseId(initialData?.courseId ? String(initialData.courseId) : "")
    setVersionLabel(initialData?.versionLabel || "v1.0")
    setAcademicYear(initialData?.academicYear || defaultAcademicYear())
    setSemester(normalizeSemester(initialData?.semester) || "Semester 1")
    setMajor(initialData?.major || "")
    setCourseDesignation(initialData?.courseDesignation || "")
    setCourseTypes(parseStringArray(initialData?.courseTypes))
    setLanguage(initialData?.language || "")
    setRelation(initialData?.relation || "")
    setTeachingMethods(initialData?.teachingMethods || "")
    setWorkloadTotal(initialData?.workloadTotal || "")
    setWorkloadContact(initialData?.workloadContact || "")
    setWorkloadPrivate(initialData?.workloadPrivate || "")
    setPrerequisites(initialData?.prerequisites || "")
    setObjectives(initialData?.objectives || "")
    setExamForms(initialData?.examForms || "")
    setExamRequirements(initialData?.examRequirements || "")
    setChangeSummary(initialData?.changeSummary || "")
    setPrefilledContent({
      clos: initialData?.clos ?? [],
      topics: initialData?.topics ?? [],
      assessments: initialData?.assessments ?? [],
    })
    setSupplemental(parseStandardSyllabusNotes(initialData?.notes))
    setCreditFieldsEdited({
      creditPoints: false,
      lectureCredits: false,
      laboratoryCredits: false,
    })
    setFormError(null)
  }, [initialDataSignature])

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

  const defaultCreditPoints = selectedCourse
    ? String((selectedCourse.creditTheory ?? 0) + (selectedCourse.creditLab ?? 0))
    : ""
  const defaultLectureCredits = selectedCourse ? String(selectedCourse.creditTheory ?? 0) : ""
  const defaultLaboratoryCredits = selectedCourse ? String(selectedCourse.creditLab ?? 0) : ""
  const effectiveCreditPoints = supplemental.creditPoints
    || (!creditFieldsEdited.creditPoints ? defaultCreditPoints : "")
  const effectiveLectureCredits = supplemental.lectureCredits
    || (!creditFieldsEdited.lectureCredits ? defaultLectureCredits : "")
  const effectiveLaboratoryCredits = supplemental.laboratoryCredits
    || (!creditFieldsEdited.laboratoryCredits ? defaultLaboratoryCredits : "")

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
        setSupplemental(parseStandardSyllabusNotes(template.notes))
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

  const buildPayload = (): CreateSyllabusRequest => ({
    ...initialData,
    courseId: Number(courseId),
    versionNumber: Number(initialData?.versionNumber || 1),
    versionLabel: versionLabel.trim() || "v1.0",
    academicYear: academicYear.trim(),
    semester: semester.trim(),
    major: major.trim(),
    courseDesignation: courseDesignation.trim(),
    courseTypes: JSON.stringify(courseTypes),
    language: language.trim(),
    relation: relation.trim(),
    teachingMethods: teachingMethods.trim(),
    workloadTotal: workloadTotal.trim(),
    workloadContact: workloadContact.trim(),
    workloadPrivate: workloadPrivate.trim(),
    prerequisites: prerequisites.trim(),
    objectives: objectives.trim(),
    examForms: examForms.trim(),
    examRequirements: examRequirements.trim(),
    changeSummary: changeSummary.trim(),
    notes: serializeStandardSyllabusNotes({
      ...supplemental,
      creditPoints: effectiveCreditPoints,
      lectureCredits: effectiveLectureCredits,
      laboratoryCredits: effectiveLaboratoryCredits,
    }),
    clos: prefilledContent.clos,
    topics: prefilledContent.topics.map((topic) => {
      // orderInWeek is internal ordering metadata, not a user-authored field.
      // Normalize legacy/imported zero-based rows when the Draft is saved.
      const persisted = {
        ...topic,
        orderInWeek: Math.max(1, Number(topic.orderInWeek) || 1),
      }
      delete persisted.contentWeight
      delete persisted.contentLevel
      delete persisted.teachingLevel
      return persisted
    }),
    assessments: prefilledContent.assessments,
  })

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()

    if (readOnly) return

    if (!courseId) {
      setFormError("Select a course before saving the syllabus.")
      return
    }

    if (!academicYear.trim()) {
      setFormError("Academic Year is required. Do not enter Curriculum Cohort in this field.")
      return
    }

    if (!semester.trim()) {
      setFormError("Semester is required.")
      return
    }

    setFormError(null)
    onSubmit(buildPayload())
  }

  const handleReset = () => {
    setCourseId(initialData?.courseId ? String(initialData.courseId) : "")
    setVersionLabel(initialData?.versionLabel || "v1.0")
    setAcademicYear(initialData?.academicYear || defaultAcademicYear())
    setSemester(normalizeSemester(initialData?.semester) || "Semester 1")
    setMajor(initialData?.major || "")
    setCourseDesignation(initialData?.courseDesignation || "")
    setCourseTypes(parseStringArray(initialData?.courseTypes))
    setLanguage(initialData?.language || "")
    setRelation(initialData?.relation || "")
    setTeachingMethods(initialData?.teachingMethods || "")
    setWorkloadTotal(initialData?.workloadTotal || "")
    setWorkloadContact(initialData?.workloadContact || "")
    setWorkloadPrivate(initialData?.workloadPrivate || "")
    setPrerequisites(initialData?.prerequisites || "")
    setObjectives(initialData?.objectives || "")
    setExamForms(initialData?.examForms || "")
    setExamRequirements(initialData?.examRequirements || "")
    setChangeSummary(initialData?.changeSummary || "")
    setPrefilledContent({
      clos: initialData?.clos ?? [],
      topics: initialData?.topics ?? [],
      assessments: initialData?.assessments ?? [],
    })
    setSupplemental(parseStandardSyllabusNotes(initialData?.notes))
    setCreditFieldsEdited({
      creditPoints: false,
      lectureCredits: false,
      laboratoryCredits: false,
    })
    setFormError(null)
  }

  const handleExportJson = () => {
    const json = JSON.stringify(buildPayload(), null, 2)
    const url = URL.createObjectURL(new Blob([json], { type: "application/json" }))
    const anchor = document.createElement("a")
    anchor.href = url
    anchor.download = `${selectedCourse?.courseCode || "syllabus"}-${versionLabel || "draft"}.json`
    anchor.click()
    URL.revokeObjectURL(url)
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
      <fieldset disabled={readOnly} className="min-w-0 space-y-6 border-0 p-0">
      <section className="overflow-hidden rounded-xl border border-[#cfdee1] border-t-4 border-t-[#007d84] bg-white shadow-sm">
        <div className="flex flex-col gap-5 px-5 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#56727b]">
                SCSE / Syllabus Administration
              </p>
              <span className="rounded border border-[#7eb8bc] bg-[#f2fbfb] px-2 py-0.5 text-[10px] font-semibold text-[#006f75]">
                {readOnly ? "Read-only" : "Draft"}
              </span>
            </div>
            <h1 className="mt-2 truncate text-2xl font-bold tracking-[-0.02em] text-[#006f75]">
              {previewCourseIdentity?.code || previewCourseIdentity?.name
                ? [previewCourseIdentity.code, previewCourseIdentity.name].filter(Boolean).join(" — ")
                : selectedCourse ? `${selectedCourse.courseCode} — ${selectedCourse.name}` : "New Course Syllabus"}
            </h1>
            <p className="mt-1 text-xs text-slate-500">
              {previewCourseIdentity ? "Imported template preview" : selectedCourse?.departmentName || "School of Computer Science and Engineering"}
              {semester ? ` · ${semester}` : ""}
              {!previewCourseIdentity && selectedCourse ? ` · ${(selectedCourse.creditTheory ?? 0) + (selectedCourse.creditLab ?? 0)} credits` : ""}
            </p>
          </div>

          {!readOnly && <div className="flex flex-wrap gap-2">
            <Button type="button" variant="outline" size="sm" onClick={handleReset} disabled={loading}>
              <RotateCcw className="size-4" />
              Làm mới
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={handleExportJson} disabled={!courseId}>
              <FileJson className="size-4" />
              Xuất JSON
            </Button>
            <Button type="submit" size="sm" disabled={loading || coursesLoading} className="bg-[#007d84] text-white hover:bg-[#006d73]">
              {loading ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />}
              {loading ? "Đang lưu..." : submitLabel}
            </Button>
          </div>}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-5">
          <div className="flex items-start gap-3">
            <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-[#dcf3f2] text-sm font-semibold text-[#006f75]">
              1
            </span>

            <div>
              <h2 className="font-bold text-[#17343d]">
                General information
              </h2>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                {readOnly
                  ? "Thông tin syllabus được hiển thị theo đúng cấu trúc form chuẩn và không thể chỉnh sửa trong chế độ View."
                  : "Thông tin chung của môn học. Tất cả dữ liệu syllabus có thể kiểm tra và chỉnh sửa trước khi lưu."}
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
                Enter the teaching academic year here, for example 2024-2025. Cohort and PLO scope come from the curriculum context selected in the Add New Syllabus flow.
              </p>
            </div>
          </div>

          {curriculumContext && (
            <div className="rounded-xl border border-teal-200 bg-teal-50/60 px-4 py-3">
              <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-teal-700">Curriculum context (locked)</p>
              <div className="mt-2 grid gap-3 text-sm sm:grid-cols-2">
                <div><span className="text-xs text-slate-500">Major</span><p className="font-medium text-slate-800">{curriculumContext.major}</p></div>
                <div><span className="text-xs text-slate-500">Cohort</span><p className="font-medium text-slate-800">{curriculumContext.cohort}</p></div>
              </div>
            </div>
          )}

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
              {previewCourseIdentity ? (
                <ReadOnlyValue
                  label="Imported Course"
                  value={[previewCourseIdentity.code, previewCourseIdentity.name].filter(Boolean).join(" — ")}
                />
              ) : coursesLoading ? (
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
                  onValueChange={(value) => {
                    setCourseId(value)
                    setCreditFieldsEdited({
                      creditPoints: false,
                      lectureCredits: false,
                      laboratoryCredits: false,
                    })
                  }}
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
                  || lockProgramContext
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

          </div>

          {(selectedCourse || previewCourseIdentity) && (
            <div className="grid gap-4 md:grid-cols-2">
              <ReadOnlyValue
                label="Course Name"
                value={
                  previewCourseIdentity?.name || selectedCourse?.name || ""
                }
              />

              <ReadOnlyValue
                label="Course Code"
                value={previewCourseIdentity?.code || selectedCourse?.courseCode || ""}
              />
            </div>
          )}

          <FieldGroup label="Course Designation">
            <Input
              value={courseDesignation}
              onChange={(event) => setCourseDesignation(event.target.value)}
              placeholder="Course designation or brief classification."
            />
          </FieldGroup>

          <FieldGroup label="Semester(s) in which the course is taught" required>
            <Select value={semester} onValueChange={setSemester} disabled={contextLocked}>
              <SelectTrigger className="bg-white">
                <SelectValue placeholder="Select semester" />
              </SelectTrigger>
              <SelectContent>
                {SEMESTERS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FieldGroup>

          <div className="grid gap-5 md:grid-cols-2">
            <FieldGroup
              label="Person Responsible for the Course"
            >
              <Input
                value={supplemental.personResponsible}
                onChange={(event) =>
                  setSupplemental((current) => ({
                    ...current,
                    personResponsible: event.target.value,
                  }))
                }
                placeholder="Instructor or course coordinator"
              />
            </FieldGroup>

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
              <Input
                value={relation}
                onChange={(event) => setRelation(event.target.value)}
                placeholder="How the course contributes to the curriculum."
              />
            </FieldGroup>

            <FieldGroup
              label="Teaching Methods"
            >
              <Input
                value={teachingMethods}
                onChange={(event) => setTeachingMethods(event.target.value)}
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
                label="(Estimated) Total Workload"
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
                label="Contact Hours (lecture, exercise, laboratory session, etc.)"
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
                label="Private Study Including Examination Preparation"
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

            <div className="mt-4">
              <FieldGroup label="Student Responsibility">
                <TextArea
                  value={supplemental.workloadStudentResponsibility}
                  onChange={(value) => setSupplemental((current) => ({
                    ...current,
                    workloadStudentResponsibility: value,
                  }))}
                  placeholder="Student self-study responsibility stated in the source syllabus."
                />
              </FieldGroup>
            </div>

            <div className="mt-4 grid gap-4 md:grid-cols-3">
              <FieldGroup label="Credit Points — Total">
                <Input
                  value={effectiveCreditPoints}
                  onChange={(event) => {
                    setCreditFieldsEdited((current) => ({ ...current, creditPoints: true }))
                    setSupplemental((current) => ({ ...current, creditPoints: event.target.value }))
                  }}
                />
              </FieldGroup>
              <FieldGroup label="Credits — Lecture">
                <Input
                  value={effectiveLectureCredits}
                  onChange={(event) => {
                    setCreditFieldsEdited((current) => ({ ...current, lectureCredits: true }))
                    setSupplemental((current) => ({ ...current, lectureCredits: event.target.value }))
                  }}
                />
              </FieldGroup>
              <FieldGroup label="Credits — Laboratory">
                <Input
                  value={effectiveLaboratoryCredits}
                  onChange={(event) => {
                    setCreditFieldsEdited((current) => ({ ...current, laboratoryCredits: true }))
                    setSupplemental((current) => ({ ...current, laboratoryCredits: event.target.value }))
                  }}
                />
              </FieldGroup>
            </div>
          </section>

          <FieldGroup
            label="Required and Recommended Prerequisites for Joining the Course"
          >
            <div className="space-y-3">
              <Input
                value={prerequisites}
                onChange={(event) => setPrerequisites(event.target.value)}
                placeholder="Text shown in the syllabus document, e.g. IT116IU or None."
              />
              {courseId && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4">
                  <div className="mb-3">
                    <p className="text-xs font-semibold text-slate-700">Structured course relationships</p>
                    <p className="mt-1 text-[11px] leading-5 text-slate-500">
                      These relationships draw the arrows on the Curriculum Map. Prerequisite cycles are rejected automatically.
                    </p>
                  </div>
                  <CoursePrerequisitePicker courseId={Number(courseId)} disabled={readOnly} />
                </div>
              )}
            </div>
          </FieldGroup>

          <FieldGroup
            label="Course Objectives"
          >
            <TextArea
              value={objectives}
              onChange={setObjectives}
              placeholder="High-level course objectives. Detailed measurable outcomes belong in the CLO section of the editor."
            />
          </FieldGroup>

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

      <StandardSyllabusFormSections
        readOnly={readOnly}
        importedContentTopics={importedContentTopics}
        clos={prefilledContent.clos}
        onClosChange={(clos) => setPrefilledContent((current) => ({ ...current, clos }))}
        topics={prefilledContent.topics}
        onTopicsChange={(topics) => setPrefilledContent((current) => ({ ...current, topics }))}
        assessments={prefilledContent.assessments}
        onAssessmentsChange={(assessments) => setPrefilledContent((current) => ({ ...current, assessments }))}
        examForms={examForms}
        onExamFormsChange={setExamForms}
        examRequirements={examRequirements}
        onExamRequirementsChange={setExamRequirements}
        supplemental={supplemental}
        onSupplementalChange={setSupplemental}
      />

      <section className="rounded-xl border border-[#cfe1e4] bg-[#f6fbfb] px-5 py-4">
        <div className="flex items-start gap-3">
          <ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#007d84]" />

          <div>
            <p className="font-semibold text-[#17343d]">
              {readOnly ? "Complete syllabus form" : "Full form is saved as one syllabus Draft"}
            </p>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              {readOnly
                ? "General Information, CLOs, matrices, content, weekly activities, assessments, readings, and revision information are shown from this saved syllabus version."
                : "General Information, CLOs, Content, weekly activities, assessments, readings, and revision notes are submitted in one Draft. After the Draft receives its database ID, CLO–PLO, Topic–CLO, Assessment–CLO, and reading links are saved through their existing backend APIs."}
            </p>
          </div>
        </div>
      </section>

      {!readOnly && <div className="flex flex-wrap justify-end gap-2">
        <Button type="button" variant="outline" size="lg" onClick={handleExportJson} disabled={!courseId}>
          <FileJson className="size-4" />
          Xuất JSON
        </Button>
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
              Đang lưu...
            </>
          ) : (
            <>
              <Save className="size-4" />
              {submitLabel}
            </>
          )}
        </Button>
      </div>}
      </fieldset>
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
      <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-slate-500">
        {label}
      </p>

      <p className="mt-1 flex h-10 items-center rounded-md border border-[#cedde1] bg-white px-3 text-sm font-medium text-slate-800 shadow-sm">
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
