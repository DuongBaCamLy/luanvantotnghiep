import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { syllabusBookApi, bookApi, ploApi } from "@/api/bookApi"
import type { CreateSyllabusBookRequest, CreateBookRequest } from "@/types/book"

export function useSyllabusBooks(syllabusId: number) {
  return useQuery({
    queryKey: ["syllabus-books", syllabusId],
    queryFn: () => syllabusBookApi.getBySyllabus(syllabusId),
    enabled: !!syllabusId,
  })
}

export function useAllBooks() {
  return useQuery({
    queryKey: ["books"],
    queryFn: bookApi.getAll,
  })
}

export function useCreateBook() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateBookRequest) => bookApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["books"] })
    },
  })
}

export function useAddSyllabusBook() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateSyllabusBookRequest) => syllabusBookApi.create(data),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ["syllabus-books", vars.syllabusId] })
    },
  })
}

export function useRemoveSyllabusBook(syllabusId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (bookId: number) => syllabusBookApi.delete(syllabusId, bookId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["syllabus-books", syllabusId] })
    },
  })
}

export function useAllPlos() {
  return useQuery({
    queryKey: ["plos"],
    queryFn: ploApi.getAll,
  })
}

export function usePlosByProgram(programId?: number) {
  return useQuery({
    queryKey: ["plos", "program", programId],
    queryFn: () => ploApi.getByProgram(programId!),
    enabled: !!programId,
  })
}
