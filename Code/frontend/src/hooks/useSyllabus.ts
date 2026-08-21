import { useQuery } from "@tanstack/react-query";
import { syllabusApi } from "@/api/syllabusApi";

export const useSyllabus = (
  id: number | null
) => {
  return useQuery({
    queryKey: ["syllabus", id],
    queryFn: () =>
      syllabusApi.getById(id!),
    enabled: !!id,
  });
};