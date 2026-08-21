import { useLocation, useNavigate, useParams } from "react-router-dom"

import SyllabusForm from "@/components/syllabus/SyllabusForm"

import { useSyllabus } from "@/hooks/useSyllabus"
import { useUpdateSyllabus } from "@/hooks/useUpdateSyllabus"

import type { CreateSyllabusRequest } from "@/types/syllabus"
import { useAuthStore } from "@/store/authStore"
import { getSyllabusBasePath } from "@/lib/programContext"

export default function SyllabusEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((state) => state.user)

  const syllabusId = Number(id)

  const { data, isLoading } = useSyllabus(syllabusId)

  const updateMutation = useUpdateSyllabus()

  if (isLoading) {
    return <div>Loading...</div>
  }

  if (!data) {
    return <div>Syllabus not found</div>
  }

  if (data.status !== "DRAFT") {
    return (
      <div className="mx-auto max-w-3xl space-y-4 rounded-xl border border-amber-200 bg-amber-50 p-6 text-amber-950">
        <h1 className="text-xl font-bold">Read-only Version</h1>
        <p>{data.versionLabel || `v${data.versionNumber}.0`} is currently {data.status}. Only Draft versions can be edited.</p>
        <button className="text-sm font-semibold underline" onClick={() => navigate(getSyllabusBasePath(location.pathname))}>Back to Version List</button>
      </div>
    )
  }

  const initialData: CreateSyllabusRequest = {
  courseId: data.courseId,
  versionNumber: data.versionNumber,
  versionLabel: data.versionLabel,
  academicYear: data.academicYear,

  courseDesignation: data.courseDesignation ?? "",
  courseTypes: data.courseTypes ?? "",
  semester: data.semester ?? "",
  language: data.language ?? "",
  relation: data.relation ?? "",
  teachingMethods: data.teachingMethods ?? "",
  workloadTotal: data.workloadTotal ?? "",
  workloadContact: data.workloadContact ?? "",
  workloadPrivate: data.workloadPrivate ?? "",
  prerequisites: data.prerequisites ?? "",
  objectives: data.objectives ?? "",
  examForms: data.examForms ?? "",
  examRequirements: data.examRequirements ?? "",
  rubrics: data.rubrics ?? "",
  major: data.major ?? "",

  createdBy: data.createdById,
  changeSummary: data.changeSummary ?? "",
  notes: data.notes ?? "",
  clos: data.clos ?? [],
  topics: data.topics ?? [],
  assessments: data.assessments ?? [],
}

  const handleUpdate = (values: CreateSyllabusRequest) => {
    updateMutation.mutate(
      {
        id: syllabusId,
        data: values,
      },
      {
        onSuccess: () => {
          alert("Syllabus updated successfully!")
          navigate(getSyllabusBasePath(location.pathname))
        },
        onError: (error: any) => {
          console.error("Update syllabus error:", error)
          alert(
            "Unable to update the syllabus: " +
              (error?.response?.data?.message || error.message)
          )
        },
      }
    )
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-20">
      <div className="flex items-start justify-between border-b border-slate-200 pb-4">
        <div>
          <p className="text-xs font-bold text-slate-500 uppercase tracking-widest mb-2">
            {data.courseCode} • {data.academicYear} •{" "}
            {data.versionLabel || "v1.0"}
          </p>

          <h1 className="text-3xl font-black text-slate-900 tracking-tight">
            Course Syllabus Form
          </h1>
        </div>

        <button
          onClick={() => navigate(-1)}
          className="text-xs font-bold text-slate-500 hover:text-slate-900 uppercase tracking-widest flex items-center gap-2 transition-colors"
        >
          ← BACK TO LIST
        </button>
      </div>

      <SyllabusForm
        initialData={initialData}
        onSubmit={handleUpdate}
        loading={updateMutation.isPending}
        lockAssignmentContext={user?.role === "INSTRUCTOR"}
      />
    </div>
  )
}
