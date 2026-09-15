import { formatVersionLabel } from "@/lib/syllabusVersion"
import { useQuery } from "@tanstack/react-query"
import { courseRelationshipApi } from "@/api/courseRelationshipApi"
import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { FileText, Info, Save, Link2, ShieldCheck } from "lucide-react"
import type { Syllabus } from "@/types/syllabus"
import { useUpdateSyllabus } from "@/hooks/useUpdateSyllabus"

const parseCourseTypes = (raw?: string | null): string[] => {
  if (!raw?.trim()) return []
  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.map(String).filter(Boolean)
    }
  } catch {
    // Backward-compatible fallback for legacy comma-separated values.
  }
  return raw.split(",").map(value => value.trim()).filter(Boolean)
}


interface Props {
  syllabus: Syllabus
  readOnly?: boolean
}

export default function Section1GeneralInfo({ syllabus, readOnly = false }: Props) {
  const updateMutation = useUpdateSyllabus()

  const { data: relationships = [], isLoading: relationshipsLoading } = useQuery({
    queryKey: ["course-relationships", syllabus.courseId],
    queryFn: () => courseRelationshipApi.getByCourse(syllabus.courseId),
    enabled: !!syllabus.courseId,
  })

  const [form, setForm] = useState({
  // Các field đang được render trong UI
  versionLabel: formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel),
  academicYear: syllabus.academicYear ?? "",
  changeSummary: syllabus.changeSummary ?? "",
  notes: syllabus.notes ?? "",

  // Các field không render ở tab này nhưng backend update() sẽ ghi đè.
  // Giữ lại giá trị cũ để tránh mất dữ liệu khi lưu "Thông tin chung".
  courseDesignation: syllabus.courseDesignation ?? "",
  courseTypes: syllabus.courseTypes ?? "",
  semester: syllabus.semester ?? "",
  language: syllabus.language ?? "",
  relation: syllabus.relation ?? "",
  teachingMethods: syllabus.teachingMethods ?? "",
  workloadTotal: syllabus.workloadTotal ?? "",
  workloadContact: syllabus.workloadContact ?? "",
  workloadPrivate: syllabus.workloadPrivate ?? "",
  prerequisites: syllabus.prerequisites ?? "",
  objectives: syllabus.objectives ?? "",
  examForms: syllabus.examForms ?? "",
  examRequirements: syllabus.examRequirements ?? "",
  major: syllabus.major ?? "",
})
  const selectedCourseTypes = parseCourseTypes(syllabus.courseTypes)

  const handleSave = () => {
    updateMutation.mutate(
      {
        id: syllabus.id,
        data: {
          ...form,
          courseTypes: syllabus.courseTypes ?? JSON.stringify(selectedCourseTypes),
          major: syllabus.major ?? form.major,
          academicYear: syllabus.academicYear ?? form.academicYear,
          semester: syllabus.semester ?? form.semester,
          courseId: syllabus.courseId,
          createdBy: syllabus.createdById,
        },
      },
      {
        onSuccess: () => {
          alert("General information saved successfully!")
        },
      }
    )
  }

  const fieldClass = "h-10 bg-white border-slate-200 focus:border-primary/60 text-sm"
  const readOnlyClass = "bg-slate-50 text-slate-700 cursor-default"

  return (
    <div className="space-y-5">
      <div className="rounded-xl border border-cyan-200 bg-cyan-50/60 px-4 py-3">
        <div className="flex items-start gap-2">
          <ShieldCheck className="mt-0.5 size-4 shrink-0 text-primary" />
          <p className="text-xs leading-5 text-slate-600">
            Course identity, Academic Year, Semester, Course Type, Major, and structured prerequisite relationships are controlled by teaching-assignment or curriculum data. Instructors can review them here but cannot change them from the syllabus editor.
          </p>
        </div>
      </div>
      {/* Course identity — read only from system */}
      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="pb-3 border-b border-slate-100">
          <CardTitle className="text-sm font-bold text-slate-700 flex items-center gap-2">
            <Info className="size-4 text-primary" />
            Course Information (System Data)
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Course Code</Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50 text-sm font-mono font-semibold text-slate-700">
                {syllabus.courseCode}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Course Name</Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50 text-sm text-slate-700">
                {syllabus.courseName}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Credit Points</Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50 text-sm text-slate-700">
                {syllabus.creditTheory != null || syllabus.creditLab != null
                  ? `Total: ${(syllabus.creditTheory ?? 0) + (syllabus.creditLab ?? 0)} (Lecture: ${syllabus.creditTheory ?? 0}, Laboratory: ${syllabus.creditLab ?? 0})`
                  : "Not configured"}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Person Responsible for the Course
              </Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50 text-sm text-slate-700">
                {syllabus.responsibleInstructors || "Not assigned"}
              </div>
              <p className="text-[11px] text-slate-400">
                From the linked teaching assignment. Shown on the exported PDF exactly as displayed here.
              </p>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Prepared By</Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50 text-sm text-slate-700">
                {syllabus.createdByUsername}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Status</Label>
              <div className="h-10 px-3 flex items-center rounded-lg border border-slate-200 bg-slate-50">
                <Badge className={`text-xs font-semibold ${syllabus.status === "APPROVED" ? "bg-emerald-100 text-emerald-700" :
                    syllabus.status === "SUBMITTED" ? "bg-blue-100 text-blue-700" :
                      syllabus.status === "REJECTED" ? "bg-rose-100 text-rose-700" :
                        "bg-slate-100 text-slate-600"
                  }`}>
                  {syllabus.status === "APPROVED" ? "Approved" :
                    syllabus.status === "SUBMITTED" ? "Submitted" :
                      syllabus.status === "REJECTED" ? "Rejected" : "Draft"}
                </Badge>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
      {/* Curriculum-owned course relationships — read only for Instructor */}
      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="pb-3 border-b border-slate-100">
          <CardTitle className="text-sm font-bold text-slate-700 flex items-center gap-2">
            <Link2 className="size-4 text-primary" />
            Prerequisite / Related Courses
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4 space-y-3">
          <p className="text-xs text-slate-500">
            Structured relationships drive the Curriculum Map and are managed by authorized curriculum administrators. They are read-only in the Instructor workspace.
          </p>

          {relationshipsLoading ? (
            <span className="text-xs text-slate-400">Loading course relationships...</span>
          ) : (
            <div className="flex flex-wrap gap-2">
              {relationships.map((rel) => (
                <span
                  key={rel.id}
                  className="inline-flex items-center gap-1.5 rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700"
                >
                  <span className="font-mono font-semibold">{rel.relatedCourseCode}</span>
                  <span className="text-slate-400">·</span>
                  <span className="text-slate-500">
                    {rel.relationType === "PREREQUISITE" && "Prerequisite"}
                    {rel.relationType === "COREQUISITE" && "Corequisite"}
                    {rel.relationType === "RECOMMENDED" && "Recommended"}
                    {rel.relationType === "EQUIVALENT" && "Equivalent"}
                  </span>
                </span>
              ))}

              {relationships.length === 0 && (
                <span className="text-xs text-slate-400 italic">
                  No structured course relationships are configured.
                </span>
              )}
            </div>
          )}
        </CardContent>
      </Card>
      {/* Editable metadata */}
      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="pb-3 border-b border-slate-100">
          <CardTitle className="text-sm font-bold text-slate-700 flex items-center gap-2">
            <FileText className="size-4 text-primary" />
            Syllabus Version Information
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Applicable Academic Year <span className="text-rose-500">*</span>
              </Label>
              <div className={`h-10 px-3 flex items-center rounded-lg border border-slate-200 text-sm ${readOnlyClass}`}>
                {form.academicYear || "—"}
              </div>
              {!readOnly && (
                <p className="text-[11px] text-slate-400">Locked to the teaching assignment.</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Version Label <span className="text-rose-500">*</span>
              </Label>
              <Input
                className={`${fieldClass} ${readOnly ? readOnlyClass : ""}`}
                readOnly
                placeholder="v1.0"
                value={form.versionLabel}
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Version Number
              </Label>
              <div className={`h-10 px-3 flex items-center rounded-lg border border-slate-200 text-sm ${readOnlyClass}`}>
                {formatVersionLabel(syllabus.versionNumber, syllabus.versionLabel)}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Change Summary from Previous Version
              </Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none transition-colors placeholder:text-slate-400
                  ${readOnly ? readOnlyClass + " resize-none" : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                placeholder="Describe the main changes from the previous version..."
                value={form.changeSummary}
                onChange={(e) => setForm({ ...form, changeSummary: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                Internal Notes
              </Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none transition-colors placeholder:text-slate-400
                  ${readOnly ? readOnlyClass + " resize-none" : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                placeholder="Additional notes for the Head of Department / review committee..."
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
              />
            </div>
          </div>

          {!readOnly && (
            <div className="flex justify-end pt-2 border-t border-slate-100">
              <Button
                onClick={handleSave}
                disabled={updateMutation.isPending}
                className="bg-primary text-white hover:bg-primary/90 gap-2 h-9 text-sm shadow-sm"
              >
                <Save className="size-4" />
                {updateMutation.isPending ? "Saving..." : "Save General Information"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Full academic information required before submission */}
      <Card className="border border-slate-200 shadow-sm">
        <CardHeader className="pb-3 border-b border-slate-100">
          <CardTitle className="text-sm font-bold text-slate-700 flex items-center gap-2">
            <Info className="size-4 text-primary" />
            Required Information Before Submission
          </CardTitle>
        </CardHeader>
        <CardContent className="pt-4 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5 md:col-span-2">
              <Label className="text-xs font-semibold text-slate-600">Course Designation <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[80px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.courseDesignation}
                onChange={(e) => setForm({ ...form, courseDesignation: e.target.value })}
                placeholder="Describe the role of this course in the curriculum..."
              />
            </div>

            <div className="space-y-2">
              <Label className="text-xs font-semibold text-slate-600">
                Course Type <span className="text-rose-500">*</span>
              </Label>
              <div className="min-h-[86px] rounded-lg border border-slate-200 bg-slate-50/70 p-3">
                <div className="flex flex-wrap gap-2">
                  {selectedCourseTypes.length > 0 ? (
                    selectedCourseTypes.map((option) => (
                      <Badge
                        key={option}
                        variant="outline"
                        className="border-cyan-200 bg-white text-primary"
                      >
                        {option}
                      </Badge>
                    ))
                  ) : (
                    <span className="text-sm text-slate-400">Not configured</span>
                  )}
                </div>
                <p className="mt-2 text-[11px] text-slate-400">
                  Read-only curriculum classification.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-4 content-start">
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-slate-600">Semester <span className="text-rose-500">*</span></Label>
                <div className={`h-10 px-3 flex items-center rounded-lg border border-slate-200 text-sm ${readOnlyClass}`}>
                  {form.semester || "—"}
                </div>
                {!readOnly && <p className="text-[11px] text-slate-400">Locked to the teaching assignment.</p>}
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-slate-600">
                  Major / Academic Area <span className="text-rose-500">*</span>
                </Label>
                <div className={`min-h-10 px-3 py-2 flex items-center rounded-lg border border-slate-200 text-sm ${readOnlyClass}`}>
                  {syllabus.major || "Not configured"}
                </div>
                <p className="text-[11px] text-slate-400">
                  Read-only curriculum context. Program/Cohort and PLO scope are managed outside the Instructor editor.
                </p>
              </div>
              <div className="space-y-1.5">
                <Label className="text-xs font-semibold text-slate-600">Language of Instruction <span className="text-rose-500">*</span></Label>
                <Input
                  className={`${fieldClass} ${readOnly ? readOnlyClass : ""}`}
                  readOnly={readOnly}
                  value={form.language}
                  onChange={(e) => setForm({ ...form, language: e.target.value })}
                  placeholder="English / Vietnamese"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Curriculum Relationship Notes <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[80px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.relation}
                onChange={(e) => setForm({ ...form, relation: e.target.value })}
                placeholder="Enter 'None' if not applicable."
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Teaching Methods <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[80px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.teachingMethods}
                onChange={(e) => setForm({ ...form, teachingMethods: e.target.value })}
                placeholder="Lecture, lab, project-based learning..."
              />
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50/50 p-4">
            <div className="mb-3">
              <p className="text-sm font-bold text-slate-800">Workload <span className="text-rose-500">*</span></p>
              <p className="text-xs text-slate-500 mt-0.5">Total = Contact + Private Study and must match the total hours in Topics.</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[
                ["workloadTotal", "Total Workload"],
                ["workloadContact", "Contact Hours"],
                ["workloadPrivate", "Private Study"],
              ].map(([field, label]) => (
                <div key={field} className="space-y-1.5">
                  <Label className="text-xs font-semibold text-slate-600">{label}</Label>
                  <Input
                    type="number"
                    min={0}
                    step="0.5"
                    className={`${fieldClass} ${readOnly ? readOnlyClass : ""}`}
                    readOnly={readOnly}
                    value={form[field as keyof typeof form] as string}
                    onChange={(e) => setForm({ ...form, [field]: e.target.value })}
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Prerequisite Notes <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.prerequisites}
                onChange={(e) => setForm({ ...form, prerequisites: e.target.value })}
                placeholder="Enter 'None' if not applicable."
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Course Objectives <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.objectives}
                onChange={(e) => setForm({ ...form, objectives: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Assessment / Examination Forms <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.examForms}
                onChange={(e) => setForm({ ...form, examForms: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <Label className="text-xs font-semibold text-slate-600">Learning and Examination Requirements <span className="text-rose-500">*</span></Label>
              <textarea
                className={`w-full min-h-[90px] px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none ${readOnly ? readOnlyClass : "bg-white focus:border-primary/60"}`}
                readOnly={readOnly}
                value={form.examRequirements}
                onChange={(e) => setForm({ ...form, examRequirements: e.target.value })}
              />
            </div>
          </div>

          {!readOnly && (
            <div className="flex justify-end pt-2 border-t border-slate-100">
              <Button
                onClick={handleSave}
                disabled={updateMutation.isPending}
                className="bg-primary text-white hover:bg-primary/90 gap-2 h-9 text-sm shadow-sm"
              >
                <Save className="size-4" />
                {updateMutation.isPending ? "Saving..." : "Save All Required Information"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Approval info if applicable */}
      {(syllabus.approvedByUsername || syllabus.approvedAt) && (
        <Card className="border border-emerald-200 bg-emerald-50/40 shadow-sm">
          <CardContent className="pt-4 pb-4">
            <div className="flex items-start gap-3">
              <div className="size-9 rounded-full bg-emerald-100 flex items-center justify-center shrink-0">
                <span className="text-emerald-700 text-lg">✓</span>
              </div>
              <div>
                <p className="text-sm font-semibold text-emerald-800">Syllabus Approved</p>
                <p className="text-xs text-emerald-600 mt-0.5">
                  Approved by <strong>{syllabus.approvedByUsername}</strong>
                  {syllabus.approvedAt && (
                    <> · Date {new Date(syllabus.approvedAt).toLocaleDateString("en-US")}</>
                  )}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}
