import { api } from "./axios"
import type { AuditLog } from "@/types/admin"

export interface AuditLogPage {
  content: AuditLog[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}

export const auditLogApi = {
  getPage: async (params: { q: string; action?: string; page: number; size: number }, signal?: AbortSignal): Promise<AuditLogPage> => {
    const response = await api.get<AuditLogPage>("/api/audit-logs/page", {
      params, signal, timeout: 15_000,
    })
    return response.data
  },
  getAll: async (): Promise<AuditLog[]> => {
    const response = await api.get<AuditLog[]>("/api/audit-logs")
    return response.data
  },

  getRecent: async (limit = 8): Promise<AuditLog[]> => {
    const response = await api.get<AuditLog[]>("/api/audit-logs/recent", {
      params: { limit },
    })
    return response.data
  },

  search: async (q: string): Promise<AuditLog[]> => {
    const response = await api.get<AuditLog[]>("/api/audit-logs/search", { params: { q } })
    return response.data
  }
}
