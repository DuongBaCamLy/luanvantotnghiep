export type UsageType = "REQUIRED" | "RECOMMENDED" | "SUPPLEMENTARY"
export type BookType = "TEXTBOOK" | "REFERENCE" | "SUPPLEMENTARY"

export interface Book {
  id: number
  title: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  bookType?: BookType
}

export interface SyllabusBook {
  syllabusId: number
  bookId: number
  bookTitle: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  usageType: UsageType
  orderIndex?: number
}

export interface CreateSyllabusBookRequest {
  syllabusId: number
  bookId: number
  usageType: UsageType
  orderIndex?: number
}

export interface CreateBookRequest {
  title: string
  author?: string
  publisher?: string
  year?: number
  edition?: string
  isbn?: string
  url?: string
  bookType?: BookType
}

export interface Plo {
  id: number
  programId: number
  programCode?: string
  programName?: string
  code: string
  description?: string
  descriptionVn?: string
  category?: string
  versionNumber?: number
  isActive?: boolean
  createdAt?: string
}

export interface PloRequest {
  programId: number
  code: string
  description: string
  descriptionVn?: string
  category?: string
  versionNumber?: number
}
