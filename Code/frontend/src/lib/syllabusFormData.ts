import type {
  CreateSyllabusRequest,
  Syllabus,
} from "@/types/syllabus"

export type SyllabusFormContext = {
  assignmentId?: number
  courseProgramId?: number
  programId?: number
}

/**
 * Maps the complete persisted syllabus snapshot to the canonical form model.
 * View and Edit share this conversion so supplemental matrices/readings stored
 * in notes and the normalized child collections are interpreted identically.
 */
export const toSyllabusFormData = (
  syllabus: Syllabus,
  context: SyllabusFormContext = {},
): CreateSyllabusRequest => ({
  assignmentId: context.assignmentId,
  programId: context.programId,
  courseProgramId: context.courseProgramId,
  cohortId: syllabus.cohortId,
  courseId: syllabus.courseId,
  versionNumber: syllabus.versionNumber,
  versionLabel: syllabus.versionLabel,
  academicYear: syllabus.academicYear,
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
  createdBy: syllabus.createdById,
  changeSummary: syllabus.changeSummary ?? "",
  notes: syllabus.notes ?? "",
  sourceType: syllabus.sourceType ?? undefined,
  originalFileName: syllabus.originalFileName ?? undefined,
  originalFileType: syllabus.originalFileType ?? undefined,
  clos: (syllabus.clos ?? []).map((clo) => ({ ...clo })),
  topics: (syllabus.topics ?? []).map((topic) => ({ ...topic })),
  assessments: (syllabus.assessments ?? []).map((assessment) => ({ ...assessment })),
})
