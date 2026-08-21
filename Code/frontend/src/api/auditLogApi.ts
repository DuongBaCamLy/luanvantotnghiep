import { api } from "./axios"
import type { AuditLog } from "@/types/admin"

export const auditLogApi = {
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
