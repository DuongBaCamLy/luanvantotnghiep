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
      queryClient.invalidateQueries({ queryKey: ["syllabuses"] });
    },
    onError: (error: any) => {
      console.error("Lỗi nhân bản đề cương:", error);
    },
  });
};
