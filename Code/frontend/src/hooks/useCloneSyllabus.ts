import { useMutation, useQueryClient } from "@tanstack/react-query";
import { syllabusApi } from "@/api/syllabusApi";
import type { CloneSyllabusRequest } from "@/types/syllabus";

export const useCloneSyllabus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      request,
    }: {
      id: number;
      request: CloneSyllabusRequest;
    }) => syllabusApi.clone(id, request),
    onSuccess: () => {
      for (const key of ["faculty-dashboard", "depthead-courses", "my-active-assignments", "course-programs", "program-timeline"]) {
        void queryClient.invalidateQueries({ queryKey: [key] })
      }
      queryClient.invalidateQueries({ queryKey: ["syllabuses"] });
    },
  });
};
