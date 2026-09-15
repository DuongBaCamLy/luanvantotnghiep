import { useQuery } from "@tanstack/react-query";
import { syllabusApi } from "@/api/syllabusApi";

export const useSyllabusDiff = (
  id: number,
  compareWith: number,
  enabled = true,
) => {
  return useQuery({
    queryKey: ["syllabusDiff", id, compareWith],
    queryFn: () => syllabusApi.getDiff(id, compareWith),
    enabled: enabled && !!id && !!compareWith,
  });
};
