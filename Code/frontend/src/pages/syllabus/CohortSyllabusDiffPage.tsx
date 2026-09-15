import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useEffect, useMemo, useState } from "react"
import {
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router-dom"
import {
  ArrowLeft,
  ArrowRight,
  ArrowRightLeft,
  GitCompareArrows,
  History,
  Loader2,
  RefreshCw,
} from "lucide-react"

import { courseApi } from "@/api/courseApi"
import { syllabusApi } from "@/api/syllabusApi"
import SyllabusDiffDetails from "@/components/syllabus/SyllabusDiffDetails"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { getSyllabusBasePath } from "@/lib/programContext"

import type { Course } from "@/types/course"
import type { Syllabus } from "@/types/syllabus"
import type { SyllabusDiffResponse } from "@/types/syllabusDiff"

const toArray = <T,>(payload: unknown): T[] => {
  if (Array.isArray(payload)) return payload as T[]

  const data = payload as {
    data?: unknown
    content?: unknown
    items?: unknown
  }

  if (Array.isArray(data?.data)) return data.data as T[]
  if (Array.isArray(data?.content)) return data.content as T[]
  if (Array.isArray(data?.items)) return data.items as T[]

  return []
}

const normalize = (value: unknown) => String(value ?? "").trim()

const formatStatusLabel = (value: unknown) => {
  const status = normalize(value).toUpperCase()

  if (!status) return "Unknown"

  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ")
}

const getStatusClass = (value: unknown) => {
  const status = normalize(value).toUpperCase()

  if (status === "APPROVED") {
    return "border-emerald-200 bg-emerald-50 text-emerald-700"
  }

  if (status === "SUBMITTED" || status === "UNDER_REVIEW") {
    return "border-blue-200 bg-blue-50 text-blue-700"
  }

  if (status === "REJECTED" || status === "REVISION_REQUESTED") {
    return "border-rose-200 bg-rose-50 text-rose-700"
  }

  if (status === "ARCHIVED") {
    return "border-slate-300 bg-slate-100 text-slate-600"
  }

  return "border-amber-200 bg-amber-50 text-amber-700"
}

const normalizeSemester = (value: unknown) => {
  const text = normalize(value)
  if (!text) return ""

  const match = text.match(/semester\s*(\d+)/i)
  return match?.[1] ?? text
}

const getCourseLabel = (course: Course) => {
  const name = normalize(course.name)
  return name ? `${course.courseCode} — ${name}` : course.courseCode
}

const getVersionBaseLabel = (syllabus: Syllabus) =>
  formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel)

const getVersionLabel = (syllabus: Syllabus) => {
  const parts = [
    getVersionBaseLabel(syllabus),
    formatStatusLabel(syllabus.status),
  ]

  const semester = normalizeSemester(syllabus.semester)

  if (semester) {
    parts.push(/^\d+$/.test(semester) ? `Semester ${semester}` : semester)
  }

  if (syllabus.isCurrent) {
    parts.push("Current")
  }

  return parts.join(" · ")
}

const compareVersionNumber = (left: Syllabus, right: Syllabus) => {
  const leftVersion = Number(left.versionNumber ?? 0)
  const rightVersion = Number(right.versionNumber ?? 0)

  if (leftVersion !== rightVersion) {
    return leftVersion - rightVersion
  }

  return left.id - right.id
}

const getDefaultVersionPair = (
  versions: Syllabus[],
) => {
  if (versions.length === 0) {
    return {
      sourceId: "",
      targetId: "",
    }
  }

  if (versions.length === 1) {
    return {
      sourceId: "",
      targetId: String(
        versions[0]!.id,
      ),
    }
  }

  const currentIndex =
    versions.findIndex(
      (version) =>
        version.isCurrent,
    )

  const targetIndex =
    currentIndex >= 0
      ? currentIndex
      : versions.length - 1

  const target:
    Syllabus | undefined =
      versions[targetIndex]

  if (!target) {
    return {
      sourceId: "",
      targetId: "",
    }
  }

  let source:
    Syllabus | undefined =
      versions[targetIndex - 1]

  if (!source) {
    source =
      versions.find(
        (version) =>
          version.id
          !== target.id,
      )
  }

  return {
    sourceId:
      source
        ? String(source.id)
        : "",
    targetId:
      String(target.id),
  }
}

const getErrorMessage = (error: unknown) => {
  const candidate = error as {
    response?: {
      data?: {
        message?: string
        error?: string
      }
    }
    message?: string
  }

  return (
    candidate.response?.data?.message
    || candidate.response?.data?.error
    || candidate.message
    || "Unable to load syllabus comparison data."
  )
}

export default function CohortSyllabusDiffPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const basePath = getSyllabusBasePath(location.pathname)

  const [initialSelection] = useState(() => ({
    courseId: searchParams.get("courseId"),
    oldSyllabusId: searchParams.get("oldSyllabusId"),
    newSyllabusId: searchParams.get("newSyllabusId"),
  }))

  const [courses, setCourses] = useState<Course[]>([])
  const [syllabusVersions, setSyllabusVersions] = useState<Syllabus[]>([])

  const [selectedCourseId, setSelectedCourseId] = useState(
    searchParams.get("courseId") ?? ""
  )
  const [sourceVersionId, setSourceVersionId] = useState(
    searchParams.get("oldSyllabusId") ?? ""
  )
  const [targetVersionId, setTargetVersionId] = useState(
    searchParams.get("newSyllabusId") ?? ""
  )

  const [syllabusDiff, setSyllabusDiff] = useState<SyllabusDiffResponse | null>(null)
  const [isLoadingCourses, setIsLoadingCourses] = useState(true)
  const [isLoadingVersions, setIsLoadingVersions] = useState(Boolean(selectedCourseId))
  const [isComparing, setIsComparing] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")

  useEffect(() => {
    let isMounted = true

    courseApi
      .getAll()
      .then((payload) => {
        if (!isMounted) return

        const nextCourses = toArray<Course>(payload)
          .slice()
          .sort((a, b) =>
            a.courseCode.localeCompare(b.courseCode, "en", { numeric: true })
          )

        setCourses(nextCourses)

        const queryCourseId = initialSelection.courseId
        const queryCourseExists = Boolean(
          queryCourseId
          && nextCourses.some((course) => String(course.id) === queryCourseId)
        )

        setSelectedCourseId((current) => nextCourses.some((course) => String(course.id) === current)
          ? current : queryCourseExists ? String(queryCourseId) : nextCourses[0] ? String(nextCourses[0].id) : "")
      })
      .catch((error) => {
        if (isMounted) {
          setCourses([])
          setErrorMessage(getErrorMessage(error))
        }
      })
      .finally(() => {
        if (isMounted) setIsLoadingCourses(false)
      })

    return () => {
      isMounted = false
    }
  }, [initialSelection])

  const [loadedCourseId, setLoadedCourseId] = useState(selectedCourseId)
  if (loadedCourseId !== selectedCourseId) {
    setLoadedCourseId(selectedCourseId)
    setSyllabusVersions([])
    setSourceVersionId("")
    setTargetVersionId("")
    setSyllabusDiff(null)
    setIsLoadingVersions(Boolean(selectedCourseId))
    setErrorMessage("")
  }

  useEffect(() => {
    if (!selectedCourseId) return
    let isMounted = true

    syllabusApi
      .getByCourse(Number(selectedCourseId))
      .then((payload) => {
        if (!isMounted) return

        const versions = toArray<Syllabus>(payload)
          .slice()
          .sort(compareVersionNumber)

        setSyllabusVersions(versions)

        const requestedSourceId = initialSelection.oldSyllabusId
        const requestedTargetId = initialSelection.newSyllabusId

        const validRequestedSource = Boolean(
          requestedSourceId
          && versions.some((version) => String(version.id) === requestedSourceId)
        )

        const validRequestedTarget = Boolean(
          requestedTargetId
          && versions.some((version) => String(version.id) === requestedTargetId)
        )

        if (
          validRequestedSource
          && validRequestedTarget
          && requestedSourceId !== requestedTargetId
        ) {
          setSourceVersionId(String(requestedSourceId))
          setTargetVersionId(String(requestedTargetId))
          return
        }

        const defaultPair = getDefaultVersionPair(versions)
        setSourceVersionId(defaultPair.sourceId)
        setTargetVersionId(defaultPair.targetId)
      })
      .catch((error) => {
        if (isMounted) {
          setSyllabusVersions([])
          setSourceVersionId("")
          setTargetVersionId("")
          setErrorMessage(getErrorMessage(error))
        }
      })
      .finally(() => {
        if (isMounted) setIsLoadingVersions(false)
      })

    return () => {
      isMounted = false
    }
  }, [selectedCourseId, initialSelection])

  useEffect(() => {
    const params = new URLSearchParams()

    if (selectedCourseId) {
      params.set("courseId", selectedCourseId)
    }

    if (sourceVersionId) {
      params.set("oldSyllabusId", sourceVersionId)
    }

    if (targetVersionId) {
      params.set("newSyllabusId", targetVersionId)
    }

    setSearchParams(params, { replace: true })
  }, [
    selectedCourseId,
    sourceVersionId,
    targetVersionId,
    setSearchParams,
  ])

  const selectedCourse = useMemo(
    () => courses.find((course) => String(course.id) === selectedCourseId),
    [courses, selectedCourseId]
  )

  const sourceVersion = useMemo(
    () => syllabusVersions.find((version) => String(version.id) === sourceVersionId),
    [sourceVersionId, syllabusVersions]
  )

  const targetVersion = useMemo(
    () => syllabusVersions.find((version) => String(version.id) === targetVersionId),
    [syllabusVersions, targetVersionId]
  )

  const canCompare = Boolean(
    selectedCourseId
    && sourceVersionId
    && targetVersionId
    && sourceVersionId !== targetVersionId
    && syllabusVersions.length >= 2
  )

  const handleCourseChange = (value: string) => {
    setSelectedCourseId(value)
    setSourceVersionId("")
    setTargetVersionId("")
    setSyllabusDiff(null)
    setErrorMessage("")
  }

  const handleSourceChange = (value: string) => {
    setSourceVersionId(value)
    setSyllabusDiff(null)
    setErrorMessage("")

    if (value === targetVersionId) {
      const alternative = syllabusVersions.find(
        (version) => String(version.id) !== value
      )

      setTargetVersionId(alternative ? String(alternative.id) : "")
    }
  }

  const handleTargetChange = (value: string) => {
    setTargetVersionId(value)
    setSyllabusDiff(null)
    setErrorMessage("")

    if (value === sourceVersionId) {
      const targetIndex = syllabusVersions.findIndex(
        (version) => String(version.id) === value
      )

      const alternative = syllabusVersions[targetIndex - 1]
        || syllabusVersions.find((version) => String(version.id) !== value)

      setSourceVersionId(alternative ? String(alternative.id) : "")
    }
  }

  const swapVersions = () => {
    if (!sourceVersionId && !targetVersionId) return

    setSourceVersionId(targetVersionId)
    setTargetVersionId(sourceVersionId)
    setSyllabusDiff(null)
    setErrorMessage("")
  }

  const resetToPreviousCurrent = () => {
    const pair = getDefaultVersionPair(syllabusVersions)

    setSourceVersionId(pair.sourceId)
    setTargetVersionId(pair.targetId)
    setSyllabusDiff(null)
    setErrorMessage("")
  }

  const handleCompare = async () => {
    if (!canCompare) {
      setErrorMessage("Select one course and two different syllabus versions.")
      return
    }

    setIsComparing(true)
    setErrorMessage("")
    setSyllabusDiff(null)

    try {
      // Backend contract: getDiff(target/current, source/previous)
      const diff = await syllabusApi.getDiff(
        Number(targetVersionId),
        Number(sourceVersionId)
      )

      setSyllabusDiff(diff)
    } catch (error) {
      setErrorMessage(getErrorMessage(error))
    } finally {
      setIsComparing(false)
    }
  }

  return (
    <div data-admin-page="CohortSyllabusDiffPage" className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="relative overflow-hidden rounded-2xl border border-[#d7e5e8] bg-white shadow-sm">
        <div className="absolute inset-x-0 top-0 h-[3px] bg-gradient-to-r from-[#007d84] via-[#15949a] to-[#f0a72f]" />

        <div className="flex flex-col gap-5 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-start gap-4">
            <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-[#eef8f8] text-[#007d84]">
              <GitCompareArrows className="size-6" />
            </div>

            <div data-admin-page-header="CohortSyllabusDiffPage">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
                FR-03.7 · Syllabus Version Comparison
              </p>

              <h1 className="mt-1 text-[28px] font-bold tracking-[-0.5px] text-[#17343d]">
                Syllabus Version Comparison
              </h1>

              <p className="mt-1 max-w-3xl text-sm leading-6 text-[#687f89]">
                Compare two preserved versions of the same course syllabus and highlight exactly what changed. By default, the page selects the current/latest version and its immediate previous version.
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(basePath)}
          >
            <ArrowLeft className="size-4" />
            Back to Syllabus Catalog
          </Button>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="font-semibold text-[#17343d]">
              Select Versions
            </h2>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              FR-03.5–03.7 · Version history is preserved. Select a course, then compare a previous/source version with a target/current version.
            </p>
          </div>

          {syllabusVersions.length >= 2 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="w-fit text-[#007d84]"
              onClick={resetToPreviousCurrent}
            >
              <RefreshCw className="size-4" />
              Previous → Current
            </Button>
          )}
        </div>

        <div data-admin-filter className="grid gap-4 xl:grid-cols-[1.35fr_1fr_auto_1fr_auto] xl:items-end">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Course
            </label>

            <Select
              value={selectedCourseId}
              onValueChange={handleCourseChange}
              disabled={isLoadingCourses || courses.length === 0}
            >
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="Select course..." />
              </SelectTrigger>

              <SelectContent>
                {courses.map((course) => (
                  <SelectItem key={course.id} value={String(course.id)}>
                    {getCourseLabel(course)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Source / Previous Version
            </label>

            <Select
              value={sourceVersionId}
              onValueChange={handleSourceChange}
              disabled={isLoadingVersions || syllabusVersions.length < 2}
            >
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="Select source version..." />
              </SelectTrigger>

              <SelectContent>
                {syllabusVersions.map((version) => (
                  <SelectItem
                    key={version.id}
                    value={String(version.id)}
                    disabled={String(version.id) === targetVersionId}
                  >
                    {getVersionLabel(version)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            variant="outline"
            size="icon"
            className="hidden xl:inline-flex"
            disabled={syllabusVersions.length < 2}
            title="Swap source and target versions"
            aria-label="Swap source and target versions"
            onClick={swapVersions}
          >
            <ArrowRightLeft className="size-4" />
          </Button>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Target / Current Version
            </label>

            <Select
              value={targetVersionId}
              onValueChange={handleTargetChange}
              disabled={isLoadingVersions || syllabusVersions.length < 2}
            >
              <SelectTrigger className="h-10 bg-white">
                <SelectValue placeholder="Select target version..." />
              </SelectTrigger>

              <SelectContent>
                {syllabusVersions.map((version) => (
                  <SelectItem
                    key={version.id}
                    value={String(version.id)}
                    disabled={String(version.id) === sourceVersionId}
                  >
                    {getVersionLabel(version)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button
            type="button"
            className="h-10 bg-[#007d84] px-5 text-white hover:bg-[#006d73]"
            disabled={!canCompare || isComparing || isLoadingVersions}
            onClick={handleCompare}
          >
            {isComparing ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Comparing...
              </>
            ) : (
              <>
                <GitCompareArrows className="size-4" />
                Compare
              </>
            )}
          </Button>
        </div>

        {isLoadingVersions && (
          <div className="mt-4 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-700">
            Loading syllabus versions for the selected course...
          </div>
        )}

        {!isLoadingVersions && selectedCourseId && syllabusVersions.length === 0 && (
          <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            This course does not have any saved syllabus versions yet.
          </div>
        )}

        {!isLoadingVersions && syllabusVersions.length === 1 && (
          <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            This course currently has only one syllabus version. FR-03.7 comparison becomes available after another version is created and preserved in version history.
          </div>
        )}

        {errorMessage && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {errorMessage}
          </div>
        )}
      </section>

      {selectedCourse && (
        <section className="rounded-2xl border border-[#cfe1e4] bg-[#f7fbfb] px-5 py-4 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.12em] text-[#708894]">
                Selected Course
              </p>

              <p className="mt-1 font-semibold text-[#17343d]">
                {selectedCourse.courseCode} · {selectedCourse.name}
              </p>

              <p className="mt-1 text-xs text-slate-500">
                {syllabusVersions.length} saved syllabus{" "}
                {syllabusVersions.length === 1 ? "version" : "versions"}
              </p>
            </div>

            {sourceVersion && targetVersion && (
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                <VersionSummary title="Source" version={sourceVersion} />

                <ArrowRight className="mx-auto size-4 shrink-0 text-slate-400 sm:mx-0" />

                <VersionSummary title="Target" version={targetVersion} />
              </div>
            )}
          </div>
        </section>
      )}

      {!syllabusDiff ? (
        <section className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center shadow-sm">
          <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-[#eef8f8] text-[#007d84]">
            <History className="size-6" />
          </div>

          <h2 className="mt-4 font-semibold text-[#17343d]">
            {syllabusVersions.length >= 2
              ? "Ready to compare syllabus versions"
              : "Version comparison is not available yet"}
          </h2>

          <p className="mx-auto mt-2 max-w-2xl text-sm leading-6 text-slate-500">
            {syllabusVersions.length >= 2
              ? "Choose the source and target versions above, then select Compare. The changed fields and collections will be highlighted below."
              : "Select a course with at least two preserved syllabus versions. Curriculum-program comparison is intentionally handled in Curriculum Programs, not on this syllabus page."}
          </p>
        </section>
      ) : (
        <section className="space-y-4">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-[#708894]">
              Comparison Result
            </p>

            <h2 className="mt-1 text-xl font-bold text-[#17343d]">
              Highlighted Syllabus Changes
            </h2>

            <p className="mt-1 text-xs leading-5 text-slate-500">
              Red represents the previous value or removed content; green represents the new value or added content. Unchanged content is not emphasized.
            </p>
          </div>

          <Card className="overflow-hidden border-slate-200 bg-white p-5 shadow-sm">
            <SyllabusDiffDetails diff={syllabusDiff} />
          </Card>
        </section>
      )}
    </div>
  )
}

function VersionSummary({
  title,
  version,
}: {
  title: string
  version: Syllabus
}) {
  return (
    <div className="min-w-[210px] rounded-xl border border-slate-200 bg-white px-4 py-3">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-slate-400">
        {title}
      </p>

      <div className="mt-1 flex flex-wrap items-center gap-2">
        <p className="font-semibold text-slate-800">
          {getVersionBaseLabel(version)}
        </p>

        {version.isCurrent && (
          <span className="rounded-full border border-blue-200 bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
            Current
          </span>
        )}
      </div>

      <span
        className={`mt-2 inline-flex rounded-full border px-2 py-0.5 text-[10px] font-semibold ${getStatusClass(version.status)}`}
      >
        {formatStatusLabel(version.status)}
      </span>
    </div>
  )
}