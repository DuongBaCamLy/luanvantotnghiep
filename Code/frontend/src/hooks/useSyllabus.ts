import { useQuery } from "@tanstack/react-query";
import { syllabusApi } from "@/api/syllabusApi";

export const useSyllabus = (
  id: number | null,
  forEdit = false,
) => {
  return useQuery({
    queryKey: ["syllabus", id, forEdit ? "edit" : "view"],
    queryFn: () =>
      forEdit ? syllabusApi.getForEdit(id!) : syllabusApi.getById(id!),
    enabled: !!id,
  });
};
