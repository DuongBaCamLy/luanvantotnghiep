import { useQuery } from "@tanstack/react-query"
import { syllabusApi } from "@/api/syllabusApi"
import type { Syllabus } from "@/types/syllabus"

export const useSyllabuses = () => {
  return useQuery<Syllabus[], Error>({
    queryKey: ["syllabuses"],
    queryFn: syllabusApi.getAll,
    staleTime: 1000 * 60 * 5,
  })
}