import { api } from "./axios"

export type SnapshotRow = Record<string, unknown>
export interface RevisionSnapshot {
  id: number
  versionNumber: number
  versionLabel: string
  eventType: string
  capturedAt: string
  actor: string | null
  content: Record<string, SnapshotRow | SnapshotRow[]>
}

export const syllabusHistoryApi = {
  history: async (id: number) => (await api.get<RevisionSnapshot[]>(`/api/syllabuses/${id}/revision-history`)).data,
  startRevision: async (id: number) => (await api.post(`/api/syllabuses/${id}/revision`)).data,
}
