import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import { syllabusApi } from "@/api/syllabusApi";

export const useDeleteSyllabus =
  () => {
    const queryClient =
      useQueryClient();

    return useMutation({
      mutationFn: (id: number) =>
        syllabusApi.delete(id),

      onSuccess: () => {
        queryClient.invalidateQueries({
          queryKey: ["syllabuses"],
        });
      },
    });
  };