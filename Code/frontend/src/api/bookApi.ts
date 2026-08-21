import { api } from "./axios"
import type {
  Book,
  SyllabusBook,
  CreateSyllabusBookRequest,
  CreateBookRequest,
  Plo,
  PloRequest,
} from "@/types/book"

export const bookApi = {
  getAll: async (): Promise<Book[]> => {
    const res = await api.get<Book[]>("/api/books")
    return res.data
  },

  create: async (data: CreateBookRequest): Promise<Book> => {
    const res = await api.post<Book>("/api/books", data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/books/${id}`)
  },
}

export const syllabusBookApi = {
  getBySyllabus: async (syllabusId: number): Promise<SyllabusBook[]> => {
    const res = await api.get<SyllabusBook[]>(`/api/syllabus-books/syllabus/${syllabusId}`)
    return res.data
  },

  create: async (data: CreateSyllabusBookRequest): Promise<SyllabusBook> => {
    const res = await api.post<SyllabusBook>("/api/syllabus-books", data)
    return res.data
  },

  delete: async (syllabusId: number, bookId: number): Promise<void> => {
    await api.delete(`/api/syllabus-books/${syllabusId}/${bookId}`)
  },
}

export const ploApi = {
  getAll: async (): Promise<Plo[]> => {
    const res = await api.get<Plo[]>("/api/plos")
    return res.data
  },

  getByProgram: async (programId: number): Promise<Plo[]> => {
    const res = await api.get<Plo[]>(`/api/plos/program/${programId}`)
    return res.data
  },

  create: async (data: PloRequest): Promise<Plo> => {
    const res = await api.post<Plo>("/api/plos", data)
    return res.data
  },

  update: async (id: number, data: PloRequest): Promise<Plo> => {
    const res = await api.put<Plo>(`/api/plos/${id}`, data)
    return res.data
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/api/plos/${id}`)
  },
}
