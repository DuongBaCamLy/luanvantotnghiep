import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import { syllabusApi } from "@/api/syllabusApi";

import type {
  CreateSyllabusRequest,
} from "@/types/syllabus";

export const useUpdateSyllabus =
  () => {
    const queryClient =
      useQueryClient();

    return useMutation({
      mutationFn: ({
        id,
        data,
      }: {
        id: number;
        data: CreateSyllabusRequest;
      }) =>
        syllabusApi.update(
          id,
          data
        ),

      onSuccess: (_data, variables) => {
      for (const key of ["faculty-dashboard", "depthead-courses", "my-active-assignments", "course-programs", "program-timeline"]) {
        void queryClient.invalidateQueries({ queryKey: [key] })
      }
        queryClient.invalidateQueries({
          queryKey: ["syllabuses"],
        });

        queryClient.invalidateQueries({
        queryKey: ["syllabus", variables.id],
      })

      queryClient.invalidateQueries({
        queryKey: ["syllabuses-course"],
      })

      queryClient.invalidateQueries({
        queryKey: ["clo-plo-heatmap"],
      })
      },
    });
  };
