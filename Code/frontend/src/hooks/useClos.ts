import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { cloApi, cloPloMappingApi } from "@/api/cloApi"
import type { CreateCloRequest, CreateCloPloMappingRequest } from "@/types/clo"

export function useClos(syllabusId: number) {
  return useQuery({
    queryKey: ["clos", syllabusId],
    queryFn: () => cloApi.getBySyllabus(syllabusId),
    enabled: !!syllabusId,
  })
}

export function useCreateClo() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCloRequest) => cloApi.create(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["clos", vars.syllabusId] })
    },
  })
}

export function useDeleteClo(syllabusId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cloApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["clos", syllabusId] })
    },
  })
}

export function useCloPloMappings(cloId: number) {
  return useQuery({
    queryKey: ["clo-plo-mappings", cloId],
    queryFn: () => cloPloMappingApi.getByClo(cloId),
    enabled: !!cloId,
  })
}

export function useCreateCloPloMapping() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCloPloMappingRequest) => cloPloMappingApi.create(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["clo-plo-mappings", vars.cloId] })
    },
  })
}

export function useDeleteCloPloMapping(cloId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => cloPloMappingApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["clo-plo-mappings", cloId] })
    },
  })
}
