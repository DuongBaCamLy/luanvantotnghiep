export type BloomLevel =
  | "REMEMBER"
  | "UNDERSTAND"
  | "APPLY"
  | "ANALYZE"
  | "EVALUATE"
  | "CREATE"

export type CompetencyLevel = "KNOWLEDGE" | "SKILL" | "ATTITUDE"

export type ContributionLevel = "I" | "D" | "A"

export interface Clo {
  id: number
  syllabusId: number
  code: string
  description: string
  descriptionVn?: string
  competencyLevel?: CompetencyLevel | string
  bloomLevel?: BloomLevel | string
  orderIndex?: number
}

export interface CreateCloRequest {
  syllabusId: number
  code: string
  description: string
  descriptionVn?: string
  competencyLevel?: string
  bloomLevel?: string
  orderIndex?: number
}

export interface CloPloMapping {
  id: number
  cloId: number
  cloCode: string
  ploId: number
  ploCode: string
  level: ContributionLevel
  contributionWeight?: number
  notes?: string
}

export interface CreateCloPloMappingRequest {
  cloId: number
  ploId: number
  level: ContributionLevel
  contributionWeight?: number
  notes?: string
}

export const BLOOM_LEVELS: { value: BloomLevel; label: string; color: string }[] = [
  { value: "REMEMBER", label: "Remember – Nhớ", color: "bg-slate-100 text-slate-700" },
  { value: "UNDERSTAND", label: "Understand – Hiểu", color: "bg-blue-100 text-blue-700" },
  { value: "APPLY", label: "Apply – Áp dụng", color: "bg-green-100 text-green-700" },
  { value: "ANALYZE", label: "Analyze – Phân tích", color: "bg-yellow-100 text-yellow-700" },
  { value: "EVALUATE", label: "Evaluate – Đánh giá", color: "bg-orange-100 text-orange-700" },
  { value: "CREATE", label: "Create – Sáng tạo", color: "bg-purple-100 text-purple-700" },
]

export const COMPETENCY_LEVELS: { value: CompetencyLevel; label: string }[] = [
  { value: "KNOWLEDGE", label: "Kiến thức (Knowledge)" },
  { value: "SKILL", label: "Kỹ năng (Skill)" },
  { value: "ATTITUDE", label: "Thái độ (Attitude)" },
]

export const CONTRIBUTION_LEVELS: { value: ContributionLevel; label: string; desc: string; color: string }[] = [
  { value: "I", label: "I", desc: "Introduce", color: "bg-sky-100 text-sky-700 border-sky-300" },
  { value: "D", label: "D", desc: "Develop", color: "bg-amber-100 text-amber-700 border-amber-300" },
  { value: "A", label: "A", desc: "Achieve", color: "bg-emerald-100 text-emerald-700 border-emerald-300" },
]
