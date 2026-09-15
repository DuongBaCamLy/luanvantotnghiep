import {
  useQuery,
} from "@tanstack/react-query"
import {
  CircleAlert,
  LoaderCircle,
  RefreshCw,
} from "lucide-react"

import {
  getMyActiveAssignments,
} from "@/api/classSectionApi"
import InstructorCourseWorklist from "@/components/instructor/InstructorCourseWorklist"
import {
  Button,
} from "@/components/ui/button"
import {
  Card,
  CardContent,
} from "@/components/ui/card"

export default function InstructorAssignmentsPage() {
  const assignmentQuery =
    useQuery({
      queryKey: [
        "my-active-assignments",
      ],
      queryFn:
        getMyActiveAssignments,
      staleTime: 30_000,
      refetchInterval: 60_000,
      refetchOnMount: "always",
    })

  if (
    assignmentQuery.isLoading
  ) {
    return (
      <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
        <div className="h-28 animate-pulse rounded-xl border border-slate-200 bg-white" />

        <div className="h-[460px] animate-pulse rounded-xl border border-slate-200 bg-white" />
      </div>
    )
  }

  if (
    assignmentQuery.isError
  ) {
    return (
      <div className="mx-auto flex min-h-[55vh] max-w-2xl items-center justify-center p-6">
        <Card className="w-full border-rose-200 bg-rose-50/70 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-start gap-4">
              <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-rose-100 text-rose-700">
                <CircleAlert className="size-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="font-semibold text-slate-900">
                  Unable to load assigned courses
                </p>

                <p className="mt-1 text-sm leading-6 text-slate-600">
                  The system could not load the active courses assigned to this Instructor account.
                </p>

                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="mt-4 bg-white"
                  onClick={() =>
                    void assignmentQuery.refetch()
                  }
                >
                  <RefreshCw className="size-4" />
                  Try Again
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  const assignments =
    assignmentQuery.data
    ?? []

  return (
    <div className="mx-auto w-full max-w-[1500px] space-y-5 pb-10">
      <section className="rounded-xl border border-slate-200 bg-white px-6 py-5 shadow-sm">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-[#708894]">
              Instructor
            </p>

            <h1 className="mt-1 text-2xl font-bold text-[#17343d]">
              My Courses
            </h1>

            <p className="mt-1 text-sm leading-6 text-slate-500">
              Review your assigned courses and open the correct syllabus action for each course.
            </p>
          </div>

          <Button
            type="button"
            variant="outline"
            disabled={
              assignmentQuery.isFetching
            }
            onClick={() =>
              void assignmentQuery.refetch()
            }
          >
            {assignmentQuery.isFetching ? (
              <LoaderCircle className="size-4 animate-spin" />
            ) : (
              <RefreshCw className="size-4" />
            )}

            Refresh
          </Button>
        </div>
      </section>

      <InstructorCourseWorklist
        assignments={
          assignments
        }
        title="Assigned Courses"
        description="Filter your assigned courses by Major, Cohort, Semester, and syllabus Status, then open the required syllabus action."
      />
    </div>
  )
}