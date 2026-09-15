import { api } from "./axios"

export type SemanticProviderStatus =
  | "DISABLED"
  | "NOT_CONFIGURED"
  | "READY"
  | "MODEL_UNAVAILABLE"
  | "UNREACHABLE"

export type SemanticProviderStatusResponse = {
  provider: string
  status: SemanticProviderStatus
  enabled: boolean
  configured: boolean
  reachable?: boolean | null
  modelAvailable?: boolean | null
  model?: string | null
  baseUrl?: string | null
  message?: string | null
}

export const semanticProviderApi = {
  getStatus:
    async (): Promise<SemanticProviderStatusResponse> => {
      const response =
        await api.get<SemanticProviderStatusResponse>(
          "/api/syllabuses/diff/semantic/provider-status",
        )

      return response.data
    },
}
