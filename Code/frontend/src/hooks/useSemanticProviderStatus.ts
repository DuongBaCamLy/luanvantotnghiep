import { useQuery } from "@tanstack/react-query"

import { semanticProviderApi } from "@/api/semanticProviderApi"

export const useSemanticProviderStatus = (
  enabled = true,
) =>
  useQuery({
    queryKey: [
      "syllabus-semantic-provider-status",
    ],
    queryFn:
      semanticProviderApi.getStatus,
    enabled,
    staleTime:
      1000 * 60 * 5,
    retry: 1,
  })
