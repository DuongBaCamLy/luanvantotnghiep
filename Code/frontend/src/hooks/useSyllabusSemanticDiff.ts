import { useMutation } from "@tanstack/react-query"

import { syllabusApi } from "@/api/syllabusApi"

import type {
  SemanticSyllabusDiffResponse,
} from "@/types/syllabusDiff"

export const useSyllabusSemanticDiff = (
  id: number,
  compareWith: number
) => {
  return useMutation<
    SemanticSyllabusDiffResponse,
    Error,
    void
  >({
    mutationFn: () =>
      syllabusApi.getSemanticDiff(
        id,
        compareWith
      ),
  })
}