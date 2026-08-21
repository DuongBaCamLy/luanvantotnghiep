import { useQuery } from "@tanstack/react-query"
import { courseApi } from "@/api/courseApi"
import type { Course } from "@/types/course"

export const useCourses = () => {
  return useQuery<Course[], Error>({
    queryKey: ["courses"],
    queryFn: courseApi.getAll,
    staleTime: 1000 * 60 * 10, // 10 minutes cache
  })
}
