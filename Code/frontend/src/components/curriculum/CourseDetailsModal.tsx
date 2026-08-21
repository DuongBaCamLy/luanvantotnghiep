import { useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { courseApi } from "@/api/courseApi";
import { syllabusApi } from "@/api/syllabusApi";

interface CourseDetailsModalProps {
  courseId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CourseDetailsModal({
  courseId,
  open,
  onOpenChange,
}: CourseDetailsModalProps) {
  // Fetch course data
  const { data: course, isLoading: isCourseLoading } = useQuery({
    queryKey: ["course", courseId],
    queryFn: () => courseApi.getById(courseId!),
    enabled: !!courseId && open,
  });

  // Fetch syllabuses for this course to find the current one
  const { data: syllabuses, isLoading: isSyllabusLoading } = useQuery({
    queryKey: ["syllabuses", "course", courseId],
    queryFn: () => syllabusApi.getByCourse(courseId!),
    enabled: !!courseId && open,
  });

  const isLoading = isCourseLoading || isSyllabusLoading;
  const currentSyllabus = syllabuses?.find((s) => s.isCurrent) || syllabuses?.[0];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl border-b pb-2">
            Course Syllabus: {course?.name || "Loading..."}
          </DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex justify-center p-8">Loading course details...</div>
        ) : !course ? (
          <div className="p-4 text-red-500">Course not found.</div>
        ) : (
          <div className="space-y-6">
            <div className="text-center">
              <h2 className="text-xl font-bold">COURSE SYLLABUS</h2>
              <h3 className="text-lg font-semibold">Course Name: {course.name}</h3>
              <p className="text-md">Course Code: <strong>{course.courseCode}</strong></p>
            </div>

            <section>
              <h4 className="font-bold text-lg mb-2">1. General information</h4>
              <table className="w-full border-collapse border border-slate-300 text-sm">
                <tbody>
                  <tr>
                    <td className="border border-slate-300 p-2 font-semibold w-1/3 bg-slate-50">
                      Course designation
                    </td>
                    <td className="border border-slate-300 p-2 italic text-slate-700">
                      {course.description || "No description provided."}
                    </td>
                  </tr>
                  <tr>
                    <td className="border border-slate-300 p-2 font-semibold bg-slate-50">
                      Semester(s) in which the course is taught
                    </td>
                    <td className="border border-slate-300 p-2">
                      Depends on curriculum mapping
                    </td>
                  </tr>
                  <tr>
                    <td className="border border-slate-300 p-2 font-semibold bg-slate-50">
                      Person responsible for the course
                    </td>
                    <td className="border border-slate-300 p-2">
                      {currentSyllabus?.createdByUsername || "N/A"}
                    </td>
                  </tr>
                  <tr>
                    <td className="border border-slate-300 p-2 font-semibold bg-slate-50">
                      Credit points
                    </td>
                    <td className="border border-slate-300 p-2">
                      {course.totalCredits} (Theory: {course.creditTheory}, Lab: {course.creditLab})
                    </td>
                  </tr>
                  <tr>
                    <td className="border border-slate-300 p-2 font-semibold bg-slate-50">
                      Relation to curriculum
                    </td>
                    <td className="border border-slate-300 p-2">
                      {course.departmentName}
                    </td>
                  </tr>
                </tbody>
              </table>
            </section>

            {currentSyllabus ? (
              <div className="text-sm text-slate-500 text-right">
                Syllabus Version: {currentSyllabus.versionLabel} | Status: {currentSyllabus.status}
              </div>
            ) : (
              <div className="text-sm text-yellow-600 bg-yellow-50 p-3 rounded">
                No syllabus document has been created for this course yet.
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
