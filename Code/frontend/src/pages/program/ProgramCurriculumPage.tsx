import { useEffect, useMemo, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useNavigate, useParams } from "react-router-dom"
import {
  ArrowLeft,
  BookOpen,
  Link2,
  Pencil,
  Plus,
  Search,
  Trash2,
  X,
} from "lucide-react"

import { cohortApi } from "@/api/cohortApi"
import { courseApi } from "@/api/courseApi"
import {
  courseProgramApi,
  type CourseProgramItem,
  type CurriculumTerm,
} from "@/api/courseProgramApi"
import {
  courseRelationshipApi,
  type RelationType,
} from "@/api/courseRelationshipApi"
import { courseTypeApi } from "@/api/courseTypeApi"
import { programApi } from "@/api/programApi"
import { Badge } from "@/components/ui/badge"
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

const ALL_GROUPS = "__all_groups__"
const ALL_SEMESTERS = "__all_semesters__"
const SRS_COURSE_TYPE_CODES = new Set([
  "COMPULSORY",
  "ELECTIVE",
  "GENERAL",
])
const PREREQUISITE_RELATION: RelationType =
  "PREREQUISITE"

type Notice =
  | {
      type: "success" | "error"
      message: string
    }
  | null

function getErrorMessage(
  error: unknown,
  fallback: string,
): string {
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
          }
        }
      }
    ).response

    if (response?.data?.message) {
      return response.data.message
    }
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

function toTermCode(
  semester: number,
): CurriculumTerm {
  return `HK${semester}` as CurriculumTerm
}

function getYearSuggest(
  semester: number,
): number {
  return Math.ceil(semester / 2)
}

export default function ProgramCurriculumPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const programId = Number(id)
  const validProgramId =
    Number.isInteger(programId)
    && programId > 0

  const [selectedCohortId, setSelectedCohortId] =
    useState("")
  const [search, setSearch] = useState("")
  const [groupFilter, setGroupFilter] =
    useState(ALL_GROUPS)
  const [semesterFilter, setSemesterFilter] =
    useState(ALL_SEMESTERS)

  const [notice, setNotice] =
    useState<Notice>(null)

  const [addOpen, setAddOpen] =
    useState(false)
  const [addCourseId, setAddCourseId] =
    useState("")
  const [addCourseTypeId, setAddCourseTypeId] =
    useState("")
  const [addSemester, setAddSemester] =
    useState("1")

  const [editingItem, setEditingItem] =
    useState<CourseProgramItem | null>(null)
  const [editCourseTypeId, setEditCourseTypeId] =
    useState("")
  const [editSemester, setEditSemester] =
    useState("1")
  const [prerequisiteCourseId, setPrerequisiteCourseId] =
    useState("")

  const {
    data: program,
    isLoading: isProgramLoading,
  } = useQuery({
    queryKey: [
      "program",
      programId,
    ],
    queryFn: () =>
      programApi.getById(programId),
    enabled: validProgramId,
  })

  const {
    data: cohorts = [],
    isLoading: isCohortsLoading,
  } = useQuery({
    queryKey: [
      "cohorts",
      "program",
      programId,
    ],
    queryFn: () =>
      cohortApi.getByProgram(programId),
    enabled: validProgramId,
  })

  const {
    data: coursePrograms = [],
    isLoading: isCourseProgramsLoading,
  } = useQuery({
    queryKey: ["course-programs"],
    queryFn: courseProgramApi.getAll,
    enabled: validProgramId,
  })

  const {
    data: courses = [],
    isLoading: isCoursesLoading,
  } = useQuery({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
  })

  const {
    data: courseTypes = [],
    isLoading: isCourseTypesLoading,
  } = useQuery({
    queryKey: ["course-types"],
    queryFn: courseTypeApi.getAll,
  })

  const editingCourseId =
    editingItem?.courseId ?? null

  const {
    data: relationships = [],
    isLoading: isRelationshipsLoading,
  } = useQuery({
    queryKey: [
      "course-relationships",
      editingCourseId,
    ],
    queryFn: () =>
      courseRelationshipApi.getByCourse(
        editingCourseId!,
      ),
    enabled:
      editingCourseId != null
      && editingCourseId > 0,
  })

  const activeCohorts = useMemo(
    () =>
      cohorts
        .filter(
          (cohort) =>
            cohort.isActive !== false,
        )
        .slice()
        .sort(
          (a, b) =>
            Number(b.entryYear ?? 0)
            - Number(a.entryYear ?? 0),
        ),
    [cohorts],
  )

  useEffect(() => {
    if (
      selectedCohortId
      || activeCohorts.length === 0
    ) {
      return
    }

    setSelectedCohortId(
      String(activeCohorts[0].id),
    )
  }, [
    activeCohorts,
    selectedCohortId,
  ])

  const selectedCohort =
    activeCohorts.find(
      (cohort) =>
        String(cohort.id)
        === selectedCohortId,
    )

  const srsCourseTypes = useMemo(
    () =>
      courseTypes.filter((courseType) =>
        SRS_COURSE_TYPE_CODES.has(
          courseType.code,
        ),
      ),
    [courseTypes],
  )

  const courseTypeById = useMemo(
    () =>
      new Map(
        courseTypes.map(
          (courseType) => [
            courseType.id,
            courseType,
          ],
        ),
      ),
    [courseTypes],
  )

  /*
   * course_program có dữ liệu baseline với cohort_id = NULL.
   * Khi chọn cohort, dòng riêng của cohort phải ghi đè
   * dòng baseline của cùng course.
   *
   * Ta dùng getAll() + resolve ở frontend để không phụ thuộc
   * vào một endpoint "curriculum" riêng chưa chắc có trong
   * phiên bản backend hiện tại.
   */
  const effectiveCurriculum =
    useMemo(() => {
      if (!selectedCohortId) {
        return []
      }

      const cohortId =
        Number(selectedCohortId)

      const relevantRows =
        coursePrograms
          .filter(
            (item) =>
              item.programId === programId
              && (
                item.cohortId == null
                || item.cohortId
                  === cohortId
              ),
          )
          .slice()
          .sort((a, b) => {
            const aSpecific =
              a.cohortId == null ? 0 : 1
            const bSpecific =
              b.cohortId == null ? 0 : 1

            if (
              aSpecific !== bSpecific
            ) {
              return aSpecific - bSpecific
            }

            return a.id - b.id
          })

      const byCourse =
        new Map<
          number,
          CourseProgramItem
        >()

      for (const item of relevantRows) {
        byCourse.set(
          item.courseId,
          item,
        )
      }

      return Array.from(
        byCourse.values(),
      ).sort((a, b) =>
        a.courseCode.localeCompare(
          b.courseCode,
          "en",
          {
            numeric: true,
          },
        ),
      )
    }, [
      coursePrograms,
      programId,
      selectedCohortId,
    ])

  const sharedCourseIds = useMemo(
    () =>
      new Set(
        coursePrograms
          .filter(
            (item) =>
              item.programId === programId
              && item.cohortId == null,
          )
          .map(
            (item) => item.courseId,
          ),
      ),
    [
      coursePrograms,
      programId,
    ],
  )

  const effectiveCourseIds =
    useMemo(
      () =>
        new Set(
          effectiveCurriculum.map(
            (item) => item.courseId,
          ),
        ),
      [effectiveCurriculum],
    )

  const availableCourses = useMemo(
    () =>
      courses
        .filter(
          (course) =>
            course.isActive !== false
            && !effectiveCourseIds.has(
              course.id,
            ),
        )
        .slice()
        .sort((a, b) =>
          a.courseCode.localeCompare(
            b.courseCode,
            "en",
            {
              numeric: true,
            },
          ),
        ),
    [
      courses,
      effectiveCourseIds,
    ],
  )

  const filteredCurriculum =
    useMemo(() => {
      const keyword =
        search.trim().toLowerCase()

      return effectiveCurriculum.filter(
        (item) => {
          const matchesSearch =
            !keyword
            || item.courseCode
              .toLowerCase()
              .includes(keyword)
            || item.courseName
              .toLowerCase()
              .includes(keyword)

          const matchesGroup =
            groupFilter
              === ALL_GROUPS
            || String(
              item.courseTypeId ?? "",
            ) === groupFilter

          const matchesSemester =
            semesterFilter
              === ALL_SEMESTERS
            || String(
              item.semesterSuggest
              ?? "",
            ) === semesterFilter

          return (
            matchesSearch
            && matchesGroup
            && matchesSemester
          )
        },
      )
    }, [
      effectiveCurriculum,
      search,
      groupFilter,
      semesterFilter,
    ])

  const totalCredits = useMemo(
    () =>
      effectiveCurriculum.reduce(
        (sum, item) =>
          sum
          + Number(
            item.totalCredits ?? 0,
          ),
        0,
      ),
    [effectiveCurriculum],
  )

  const groupCounts = useMemo(() => {
    const result: Record<
      string,
      number
    > = {
      COMPULSORY: 0,
      ELECTIVE: 0,
      GENERAL: 0,
    }

    for (
      const item
      of effectiveCurriculum
    ) {
      const courseType =
        item.courseTypeId == null
          ? undefined
          : courseTypeById.get(
              item.courseTypeId,
            )

      if (
        courseType
        && courseType.code in result
      ) {
        result[courseType.code] += 1
      }
    }

    return result
  }, [
    effectiveCurriculum,
    courseTypeById,
  ])

  const prerequisites =
    useMemo(
      () =>
        relationships.filter(
          (relationship) =>
            relationship.relationType
            === "PREREQUISITE",
        ),
      [relationships],
    )

  const prerequisiteIds =
    useMemo(
      () =>
        new Set(
          prerequisites.map(
            (relationship) =>
              relationship
                .relatedCourseId,
          ),
        ),
      [prerequisites],
    )

  const prerequisiteCandidates =
    useMemo(
      () =>
        effectiveCurriculum
          .filter(
            (item) =>
              item.courseId
                !== editingCourseId
              && !prerequisiteIds.has(
                item.courseId,
              ),
          )
          .slice()
          .sort((a, b) =>
            a.courseCode.localeCompare(
              b.courseCode,
              "en",
              {
                numeric: true,
              },
            ),
          ),
      [
        effectiveCurriculum,
        editingCourseId,
        prerequisiteIds,
      ],
    )

  const resetAddForm = () => {
    setAddCourseId("")
    setAddCourseTypeId(
      srsCourseTypes[0]
        ? String(
            srsCourseTypes[0].id,
          )
        : "",
    )
    setAddSemester("1")
  }

  const openAddDialog = () => {
    resetAddForm()
    setNotice(null)
    setAddOpen(true)
  }

  const openEditDialog = (
    item: CourseProgramItem,
  ) => {
    setEditingItem(item)
    setEditCourseTypeId(
      item.courseTypeId
        ? String(item.courseTypeId)
        : "",
    )
    setEditSemester(
      String(
        item.semesterSuggest ?? 1,
      ),
    )
    setPrerequisiteCourseId("")
    setNotice(null)
  }

  const invalidateCurriculum =
    async () => {
      await queryClient.invalidateQueries({
        queryKey: [
          "course-programs",
        ],
      })

      await queryClient.invalidateQueries({
        queryKey: [
          "programs",
        ],
      })
    }

  const addMutation =
    useMutation({
      mutationFn: async () => {
        if (
          !selectedCohort
          || !addCourseId
          || !addCourseTypeId
        ) {
          throw new Error(
            "Select a cohort, course, and course group.",
          )
        }

        const semester =
          Number(addSemester)

        const courseType =
          courseTypeById.get(
            Number(
              addCourseTypeId,
            ),
          )

        return courseProgramApi.create({
          courseId:
            Number(addCourseId),
          programId,
          cohortId:
            selectedCohort.id,
          courseTypeId:
            Number(
              addCourseTypeId,
            ),
          termCode:
            toTermCode(semester),
          semesterSuggest:
            semester,
          yearSuggest:
            getYearSuggest(
              semester,
            ),
          required:
            courseType?.code
            !== "ELECTIVE",
        })
      },
      onSuccess: async () => {
        await invalidateCurriculum()
        setAddOpen(false)
        resetAddForm()
        setNotice({
          type: "success",
          message:
            "Course added to the selected cohort curriculum.",
        })
      },
      onError: (error) => {
        setNotice({
          type: "error",
          message: getErrorMessage(
            error,
            "Unable to add the course to the curriculum.",
          ),
        })
      },
    })

  const editMutation =
    useMutation({
      mutationFn: async () => {
        if (
          !editingItem
          || !selectedCohort
          || !editCourseTypeId
        ) {
          throw new Error(
            "Missing curriculum data.",
          )
        }

        const semester =
          Number(editSemester)

        const courseType =
          courseTypeById.get(
            Number(
              editCourseTypeId,
            ),
          )

        /*
         * Nếu đang sửa baseline (cohort_id = NULL),
         * không sửa trực tiếp baseline vì sẽ ảnh hưởng
         * tất cả cohort. Tạo override riêng cho cohort.
         */
        if (
          editingItem.cohortId == null
        ) {
          return courseProgramApi.create({
            courseId:
              editingItem.courseId,
            programId,
            cohortId:
              selectedCohort.id,
            courseTypeId:
              Number(
                editCourseTypeId,
              ),
            termCode:
              toTermCode(semester),
            semesterSuggest:
              semester,
            yearSuggest:
              getYearSuggest(
                semester,
              ),
            required:
              courseType?.code
              !== "ELECTIVE",
          })
        }

        return courseProgramApi.update(
          editingItem.id,
          {
            courseTypeId:
              Number(
                editCourseTypeId,
              ),
            termCode:
              toTermCode(semester),
            semesterSuggest:
              semester,
            isRequired:
              courseType?.code
              !== "ELECTIVE",
          },
        )
      },
      onSuccess: async () => {
        await invalidateCurriculum()
        setEditingItem(null)
        setNotice({
          type: "success",
          message:
            "Curriculum course updated successfully.",
        })
      },
      onError: (error) => {
        setNotice({
          type: "error",
          message: getErrorMessage(
            error,
            "Unable to update the curriculum course.",
          ),
        })
      },
    })

  const deleteMutation =
    useMutation({
      mutationFn: async (
        item: CourseProgramItem,
      ) => {
        await courseProgramApi.delete(
          item.id,
        )
      },
      onSuccess: async () => {
        await invalidateCurriculum()
        setNotice({
          type: "success",
          message:
            "Curriculum course row removed successfully.",
        })
      },
      onError: (error) => {
        setNotice({
          type: "error",
          message: getErrorMessage(
            error,
            "Unable to remove the curriculum course.",
          ),
        })
      },
    })

  const addPrerequisiteMutation =
    useMutation({
      mutationFn: async () => {
        if (
          !editingCourseId
          || !prerequisiteCourseId
        ) {
          throw new Error(
            "Select a prerequisite course.",
          )
        }

        return courseRelationshipApi.create({
  courseId: editingCourseId,
  relatedCourseId: Number(
    prerequisiteCourseId
  ),
  relationType: PREREQUISITE_RELATION,
})
      },
      onSuccess: async () => {
        setPrerequisiteCourseId("")

        await queryClient.invalidateQueries({
          queryKey: [
            "course-relationships",
            editingCourseId,
          ],
        })

        setNotice({
          type: "success",
          message:
            "Prerequisite added successfully.",
        })
      },
      onError: (error) => {
        setNotice({
          type: "error",
          message: getErrorMessage(
            error,
            "Unable to add the prerequisite. Check for a prerequisite cycle.",
          ),
        })
      },
    })

  const removePrerequisiteMutation =
    useMutation({
      mutationFn: (
        relationshipId: number,
      ) =>
        courseRelationshipApi.delete(
          relationshipId,
        ),
      onSuccess: async () => {
        await queryClient.invalidateQueries({
          queryKey: [
            "course-relationships",
            editingCourseId,
          ],
        })

        setNotice({
          type: "success",
          message:
            "Prerequisite removed successfully.",
        })
      },
      onError: (error) => {
        setNotice({
          type: "error",
          message: getErrorMessage(
            error,
            "Unable to remove the prerequisite.",
          ),
        })
      },
    })

  const confirmDelete = (
    item: CourseProgramItem,
  ) => {
    const isShared =
      item.cohortId == null

    const hasSharedUnderOverride =
      item.cohortId != null
      && sharedCourseIds.has(
        item.courseId,
      )

    const message = isShared
      ? (
        "This is a shared baseline course. Removing it will affect every cohort that inherits this baseline. Continue?"
      )
      : hasSharedUnderOverride
        ? (
          "This is a cohort override. Removing it will reveal the shared baseline course again. Continue?"
        )
        : (
          `Remove ${item.courseCode} from ${selectedCohort?.name ?? "the selected cohort"}?`
        )

    if (
      window.confirm(message)
    ) {
      deleteMutation.mutate(item)
    }
  }

  const loading =
    isProgramLoading
    || isCohortsLoading
    || isCourseProgramsLoading
    || isCoursesLoading
    || isCourseTypesLoading

  if (!validProgramId) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700">
        Invalid curriculum program ID.
      </div>
    )
  }

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-6 pb-10">
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:flex-row lg:items-start lg:justify-between">
        <div className="flex items-start gap-4">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-orange-50 text-orange-600">
            <BookOpen className="size-5" />
          </div>

          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-slate-400">
              FR-02.3 · FR-02.4 / Curriculum Courses
            </p>

            <h1 className="mt-1 text-2xl font-bold text-slate-900">
              Manage Curriculum
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              {program
                ? `${program.code} · ${program.name}`
                : "Loading curriculum program..."}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() =>
              navigate("/admin/programs")
            }
          >
            <ArrowLeft className="size-4" />
            Back
          </Button>

          <Button
            type="button"
            disabled={
              !selectedCohortId
              || loading
            }
            onClick={openAddDialog}
          >
            <Plus className="size-4" />
            Add Course
          </Button>
        </div>
      </div>

      {notice && (
        <div
          role={
            notice.type === "error"
              ? "alert"
              : "status"
          }
          className={`rounded-xl border px-4 py-3 text-sm ${
            notice.type
              === "success"
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-800"
          }`}
        >
          {notice.message}
        </div>
      )}

      <div className="grid gap-4 xl:grid-cols-[minmax(260px,1.1fr)_repeat(5,minmax(130px,0.7fr))]">
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <Label className="text-[11px] uppercase tracking-wide text-slate-500">
            Cohort
          </Label>

          <Select
            value={selectedCohortId}
            onValueChange={
              setSelectedCohortId
            }
            disabled={
              activeCohorts.length
              === 0
            }
          >
            <SelectTrigger className="mt-2 bg-white">
              <SelectValue placeholder="Select cohort..." />
            </SelectTrigger>

            <SelectContent>
              {activeCohorts.map(
                (cohort) => (
                  <SelectItem
                    key={cohort.id}
                    value={String(
                      cohort.id,
                    )}
                  >
                    {cohort.name}
                    {" · "}
                    {cohort.entryYear}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>
        </div>

        <SummaryCard
          label="Total Courses"
          value={
            effectiveCurriculum.length
          }
        />

        <SummaryCard
          label="Total Credits"
          value={totalCredits}
        />

        <SummaryCard
          label="Compulsory"
          value={
            groupCounts.COMPULSORY
          }
        />

        <SummaryCard
          label="Elective"
          value={
            groupCounts.ELECTIVE
          }
        />

        <SummaryCard
          label="General"
          value={
            groupCounts.GENERAL
          }
        />
      </div>

      {activeCohorts.length === 0 && !isCohortsLoading && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          This program has no active cohort. Create a cohort before adding cohort-specific curriculum courses.
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="grid gap-3 border-b border-slate-100 p-4 md:grid-cols-[minmax(260px,1fr)_220px_180px]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-400" />

            <Input
              className="pl-9"
              value={search}
              onChange={(event) =>
                setSearch(
                  event.target.value,
                )
              }
              placeholder="Search course code or name..."
            />
          </div>

          <Select
            value={groupFilter}
            onValueChange={
              setGroupFilter
            }
          >
            <SelectTrigger>
              <SelectValue placeholder="All Course Groups" />
            </SelectTrigger>

            <SelectContent>
              <SelectItem
                value={ALL_GROUPS}
              >
                All Course Groups
              </SelectItem>

              {srsCourseTypes.map(
                (courseType) => (
                  <SelectItem
                    key={
                      courseType.id
                    }
                    value={String(
                      courseType.id,
                    )}
                  >
                    {courseType.name}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>

          <Select
            value={semesterFilter}
            onValueChange={
              setSemesterFilter
            }
          >
            <SelectTrigger>
              <SelectValue placeholder="All Semesters" />
            </SelectTrigger>

            <SelectContent>
              <SelectItem
                value={
                  ALL_SEMESTERS
                }
              >
                All Semesters
              </SelectItem>

              {Array.from(
                {
                  length: 8,
                },
                (_, index) =>
                  index + 1,
              ).map(
                (semester) => (
                  <SelectItem
                    key={semester}
                    value={String(
                      semester,
                    )}
                  >
                    Semester{" "}
                    {semester}
                  </SelectItem>
                ),
              )}
            </SelectContent>
          </Select>
        </div>

        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50/70">
                <TableHead className="min-w-[115px]">
                  Course Code
                </TableHead>
                <TableHead className="min-w-[260px]">
                  Course Name
                </TableHead>
                <TableHead className="min-w-[145px]">
                  Course Group
                </TableHead>
                <TableHead className="text-center">
                  Theory
                </TableHead>
                <TableHead className="text-center">
                  Lab
                </TableHead>
                <TableHead className="text-center">
                  Total
                </TableHead>
                <TableHead className="min-w-[115px] text-center">
                  Semester
                </TableHead>
                <TableHead className="min-w-[150px]">
                  Prerequisites
                </TableHead>
                <TableHead className="min-w-[130px]">
                  Scope
                </TableHead>
                <TableHead className="min-w-[190px] text-right">
                  Actions
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell
                    colSpan={10}
                    className="py-12 text-center text-sm text-slate-500"
                  >
                    Loading curriculum...
                  </TableCell>
                </TableRow>
              ) : !selectedCohortId ? (
                <TableRow>
                  <TableCell
                    colSpan={10}
                    className="py-12 text-center text-sm text-slate-500"
                  >
                    Select a cohort to view its curriculum.
                  </TableCell>
                </TableRow>
              ) : filteredCurriculum.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={10}
                    className="py-12 text-center text-sm text-slate-500"
                  >
                    No curriculum courses match the current filters.
                  </TableCell>
                </TableRow>
              ) : (
                filteredCurriculum.map(
                  (item) => {
                    const courseType =
                      item.courseTypeId == null
                        ? undefined
                        : courseTypeById.get(
                            item.courseTypeId,
                          )

                    const isShared =
                      item.cohortId == null

                    const hasSharedBaseline =
                      !isShared
                      && sharedCourseIds.has(
                        item.courseId,
                      )

                    return (
                      <TableRow
                        key={item.id}
                      >
                        <TableCell className="font-mono font-semibold text-slate-800">
                          {item.courseCode}
                        </TableCell>

                        <TableCell>
                          <p className="font-medium text-slate-800">
                            {item.courseName}
                          </p>
                        </TableCell>

                        <TableCell>
                          <CourseTypeBadge
                            code={
                              courseType?.code
                              ?? ""
                            }
                            label={
                              courseType?.name
                              ?? item.courseTypeName
                              ?? "Unclassified"
                            }
                          />
                        </TableCell>

                        <TableCell className="text-center">
                          {item.creditTheory ?? 0}
                        </TableCell>

                        <TableCell className="text-center">
                          {item.creditLab ?? 0}
                        </TableCell>

                        <TableCell className="text-center font-semibold">
                          {item.totalCredits ?? 0}
                        </TableCell>

                        <TableCell className="text-center">
                          {item.semesterSuggest
                            ? `Semester ${item.semesterSuggest}`
                            : item.termCode ?? "—"}
                        </TableCell>

                        <TableCell>
                          <span className="text-xs text-slate-500">
                            Manage in Edit
                          </span>
                        </TableCell>

                        <TableCell>
                          {isShared ? (
                            <Badge
                              variant="outline"
                              className="border-slate-200 bg-slate-50 text-slate-600"
                            >
                              Shared baseline
                            </Badge>
                          ) : (
                            <div className="space-y-1">
                              <Badge
                                variant="outline"
                                className="border-blue-200 bg-blue-50 text-blue-700"
                              >
                                {
                                  selectedCohort?.name
                                }
                              </Badge>

                              {hasSharedBaseline && (
                                <p className="text-[10px] text-slate-400">
                                  overrides baseline
                                </p>
                              )}
                            </div>
                          )}
                        </TableCell>

                        <TableCell>
                          <div className="flex justify-end gap-2">
                            <Button
                              type="button"
                              size="sm"
                              variant="outline"
                              onClick={() =>
                                openEditDialog(
                                  item,
                                )
                              }
                            >
                              <Pencil className="size-3.5" />
                              Edit
                            </Button>

                            <Button
                              type="button"
                              size="sm"
                              variant="outline"
                              className="border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
                              disabled={
                                deleteMutation.isPending
                              }
                              onClick={() =>
                                confirmDelete(
                                  item,
                                )
                              }
                            >
                              <Trash2 className="size-3.5" />
                              Remove
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    )
                  },
                )
              )}
            </TableBody>
          </Table>
        </div>

        <div className="border-t border-slate-100 px-4 py-3 text-xs text-slate-500">
          Showing {filteredCurriculum.length} of{" "}
          {effectiveCurriculum.length} effective curriculum courses for{" "}
          {selectedCohort?.name ?? "the selected cohort"}.
          Shared baseline rows apply to every cohort unless a cohort-specific override exists.
        </div>
      </div>

      <Dialog
        open={addOpen}
        onOpenChange={(open) => {
          setAddOpen(open)

          if (!open) {
            resetAddForm()
          }
        }}
      >
        <DialogContent className="bg-white sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>
              Add Course to Curriculum
            </DialogTitle>

            <DialogDescription>
              Add a course specifically to{" "}
              {selectedCohort?.name ?? "the selected cohort"}.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Course</Label>

              <Select
                value={addCourseId}
                onValueChange={
                  setAddCourseId
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a course..." />
                </SelectTrigger>

                <SelectContent>
                  {availableCourses.map(
                    (course) => (
                      <SelectItem
                        key={course.id}
                        value={String(
                          course.id,
                        )}
                      >
                        {course.courseCode}
                        {" · "}
                        {course.name}
                        {" · "}
                        {Number(
                          course.creditTheory
                          ?? 0,
                        )
                        + Number(
                          course.creditLab
                          ?? 0,
                        )}
                        {" credits"}
                      </SelectItem>
                    ),
                  )}
                </SelectContent>
              </Select>

              {availableCourses.length === 0 && (
                <p className="text-xs text-slate-500">
                  Every active course is already present in this effective curriculum.
                </p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <Label>
                  Course Group
                </Label>

                <Select
                  value={addCourseTypeId}
                  onValueChange={
                    setAddCourseTypeId
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select group..." />
                  </SelectTrigger>

                  <SelectContent>
                    {srsCourseTypes.map(
                      (courseType) => (
                        <SelectItem
                          key={
                            courseType.id
                          }
                          value={String(
                            courseType.id,
                          )}
                        >
                          {courseType.name}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>
                  Recommended Semester
                </Label>

                <Select
                  value={addSemester}
                  onValueChange={
                    setAddSemester
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    {Array.from(
                      {
                        length: 8,
                      },
                      (_, index) =>
                        index + 1,
                    ).map(
                      (semester) => (
                        <SelectItem
                          key={semester}
                          value={String(
                            semester,
                          )}
                        >
                          Semester{" "}
                          {semester}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                setAddOpen(false)
              }
            >
              Cancel
            </Button>

            <Button
              type="button"
              disabled={
                !addCourseId
                || !addCourseTypeId
                || addMutation.isPending
              }
              onClick={() =>
                addMutation.mutate()
              }
            >
              {addMutation.isPending
                ? "Adding..."
                : "Add Course"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(editingItem)}
        onOpenChange={(open) => {
          if (!open) {
            setEditingItem(null)
            setPrerequisiteCourseId("")
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-y-auto bg-white sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              Edit Curriculum Course
            </DialogTitle>

            <DialogDescription>
              {editingItem
                ? `${editingItem.courseCode} · ${editingItem.courseName}`
                : ""}
            </DialogDescription>
          </DialogHeader>

          {editingItem && (
            <div className="space-y-6">
              {editingItem.cohortId == null && (
                <div className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-xs text-blue-800">
                  This row is inherited from the shared baseline. Saving changes will create a cohort-specific override instead of changing every cohort.
                </div>
              )}

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label>
                    Course Group
                  </Label>

                  <Select
                    value={editCourseTypeId}
                    onValueChange={
                      setEditCourseTypeId
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select group..." />
                    </SelectTrigger>

                    <SelectContent>
                      {srsCourseTypes.map(
                        (courseType) => (
                          <SelectItem
                            key={
                              courseType.id
                            }
                            value={String(
                              courseType.id,
                            )}
                          >
                            {courseType.name}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label>
                    Recommended Semester
                  </Label>

                  <Select
                    value={editSemester}
                    onValueChange={
                      setEditSemester
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                      {Array.from(
                        {
                          length: 8,
                        },
                        (_, index) =>
                          index + 1,
                      ).map(
                        (semester) => (
                          <SelectItem
                            key={semester}
                            value={String(
                              semester,
                            )}
                          >
                            Semester{" "}
                            {semester}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="rounded-xl border border-slate-200">
                <div className="border-b border-slate-100 px-4 py-3">
                  <div className="flex items-center gap-2">
                    <Link2 className="size-4 text-orange-600" />
                    <p className="font-semibold text-slate-800">
                      Prerequisites
                    </p>
                  </div>

                  <p className="mt-1 text-xs text-slate-500">
                    Course prerequisites are stored at course level. Backend cycle detection will reject invalid loops.
                  </p>
                </div>

                <div className="space-y-4 p-4">
                  {isRelationshipsLoading ? (
                    <p className="text-sm text-slate-500">
                      Loading prerequisites...
                    </p>
                  ) : prerequisites.length === 0 ? (
                    <p className="text-sm text-slate-500">
                      No prerequisite configured.
                    </p>
                  ) : (
                    <div className="flex flex-wrap gap-2">
                      {prerequisites.map(
                        (
                          relationship,
                        ) => (
                          <Badge
                            key={
                              relationship.id
                            }
                            variant="outline"
                            className="gap-1.5 border-slate-200 bg-slate-50 py-1"
                          >
                            {
                              relationship.relatedCourseCode
                            }
                            {" · "}
                            {
                              relationship.relatedCourseName
                            }

                            <button
                              type="button"
                              className="ml-1 rounded p-0.5 hover:bg-slate-200"
                              aria-label={`Remove prerequisite ${relationship.relatedCourseCode}`}
                              disabled={
                                removePrerequisiteMutation.isPending
                              }
                              onClick={() =>
                                removePrerequisiteMutation.mutate(
                                  relationship.id,
                                )
                              }
                            >
                              <X className="size-3" />
                            </button>
                          </Badge>
                        ),
                      )}
                    </div>
                  )}

                  <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                    <Select
                      value={prerequisiteCourseId}
                      onValueChange={
                        setPrerequisiteCourseId
                      }
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select prerequisite course..." />
                      </SelectTrigger>

                      <SelectContent>
                        {prerequisiteCandidates.map(
                          (item) => (
                            <SelectItem
                              key={
                                item.courseId
                              }
                              value={String(
                                item.courseId,
                              )}
                            >
                              {
                                item.courseCode
                              }
                              {" · "}
                              {
                                item.courseName
                              }
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>

                    <Button
                      type="button"
                      variant="outline"
                      disabled={
                        !prerequisiteCourseId
                        || addPrerequisiteMutation.isPending
                      }
                      onClick={() =>
                        addPrerequisiteMutation.mutate()
                      }
                    >
                      <Plus className="size-4" />
                      Add Prerequisite
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() =>
                setEditingItem(null)
              }
            >
              Cancel
            </Button>

            <Button
              type="button"
              disabled={
                !editCourseTypeId
                || editMutation.isPending
              }
              onClick={() =>
                editMutation.mutate()
              }
            >
              {editMutation.isPending
                ? "Saving..."
                : "Save Changes"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function SummaryCard({
  label,
  value,
}: {
  label: string
  value: number
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4">
      <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>

      <p className="mt-2 text-2xl font-bold text-slate-900">
        {value}
      </p>
    </div>
  )
}

function CourseTypeBadge({
  code,
  label,
}: {
  code: string
  label: string
}) {
  const className =
    code === "COMPULSORY"
      ? "border-blue-200 bg-blue-50 text-blue-700"
      : code === "ELECTIVE"
        ? "border-amber-200 bg-amber-50 text-amber-700"
        : code === "GENERAL"
          ? "border-emerald-200 bg-emerald-50 text-emerald-700"
          : "border-slate-200 bg-slate-50 text-slate-600"

  return (
    <Badge
      variant="outline"
      className={className}
    >
      {label}
    </Badge>
  )
}