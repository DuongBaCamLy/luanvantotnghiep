import { api } from "@/api/axios"
import type {
  CreateUserRequest,
  UpdateUserRequest,
  UserAccountResponse,
} from "@/types/user"

const BASE = "/api/admin/users"

export async function getUsers(): Promise<UserAccountResponse[]> {
  const { data } = await api.get<UserAccountResponse[]>(BASE)
  return data
}

export async function createUser(
  payload: CreateUserRequest
): Promise<UserAccountResponse> {
  const { data } = await api.post<UserAccountResponse>(BASE, payload)
  return data
}

export async function updateUser(
  id: number,
  payload: UpdateUserRequest
): Promise<UserAccountResponse> {
  const { data } = await api.put<UserAccountResponse>(`${BASE}/${id}`, payload)
  return data
}

export async function toggleUserActive(
  id: number
): Promise<UserAccountResponse> {
  const { data } = await api.patch<UserAccountResponse>(
    `${BASE}/${id}/toggle-active`
  )
  return data
}

export async function deleteUser(id: number): Promise<void> {
  await api.delete(`${BASE}/${id}`)
}