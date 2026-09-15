import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query"

import { syllabusApi } from "@/api/syllabusApi"

export const useSubmitSyllabus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) =>
      syllabusApi.submit(id),

    onSuccess: (submitted, sourceSyllabusId) => {
  for (const key of ["my-active-assignments", "program-timeline"]) {
    void queryClient.invalidateQueries({ queryKey: [key] })
  }
  queryClient.invalidateQueries({ queryKey: ["course-programs"] })
  queryClient.invalidateQueries({
    queryKey: ["syllabuses"],
  })

  // Submission keeps the same logical syllabus and revision.
  queryClient.invalidateQueries({
    queryKey: [
      "syllabus",
      sourceSyllabusId,
    ],
  })

  // Refresh the captured submission event.
  queryClient.invalidateQueries({
    queryKey: [
      "syllabus-revision-history",
      submitted.id,
    ],
  })

  queryClient.invalidateQueries({
    queryKey: ["approval-requests"],
  })
  queryClient.invalidateQueries({ queryKey: ["approval-history", submitted.id] })

  queryClient.invalidateQueries({
    queryKey: ["faculty-dashboard"],
  })

  queryClient.invalidateQueries({
    queryKey: ["dept-head-dashboard"],
  })

  queryClient.invalidateQueries({
    queryKey: ["depthead-courses"],
  })

  queryClient.invalidateQueries({
    queryKey: ["curriculum-map"],
  })

  queryClient.invalidateQueries({
    queryKey: ["clo-plo-heatmap"],
  })

  queryClient.invalidateQueries({
    queryKey: ["notifications"],
  })
},
  })
}
