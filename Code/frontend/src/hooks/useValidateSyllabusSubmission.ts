import { useMutation } from "@tanstack/react-query"

import { syllabusApi } from "@/api/syllabusApi"

export const useValidateSyllabusSubmission = () =>
  useMutation({
    mutationFn: (id: number) => syllabusApi.validateForSubmit(id),
  })
