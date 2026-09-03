import { api } from "./axios"

export interface CloPloMatrixExportScope {
  cohortId?: number
  search?: string
  semester?: string
  status?: string
}

export interface SyllabusListExportScope {
  academicYear: string
  semester: string
  programId?: number
  cohortId?: number
  status?: string
}

export interface PloCoverageReportScope {
  programId: number
  cohortId: number
  academicYear: string
  semester: string
  courseTypeId?: number
}

export interface CurriculumChangeReportScope {
  programId: number
  oldCohortId: number
  newCohortId: number
}

export type ContributionLevel = "I" | "D" | "A"

export interface PloCoverageContribution {
  courseId: number
  courseCode: string
  courseName: string
  courseNameVn?: string | null

  courseTypeCode?: string | null
  courseTypeName?: string | null
  courseTypeNameVn?: string | null

  level?: ContributionLevel | null
  mappingCount: number
  cloCodes: string[]
}

export interface PloCoverageItem {
  ploId: number
  ploCode: string

  description?: string | null
  descriptionVn?: string | null
  category?: string | null

  covered: boolean

  contributingCourseCount: number
  contributingCloCount: number

  introductionCount: number
  developmentCount: number
  achievementCount: number

  contributions: PloCoverageContribution[]
}

export interface PloCoverageSummary {
  totalPlos: number
  coveredPlos: number
  uncoveredPlos: number
  coveragePercentage: number

  totalCourses: number
  contributingCourses: number

  totalClos: number
  mappedClos: number
  unmappedClos: number
}

export interface PloCoverageReportData {
  programId: number
  programCode: string
  programName?: string | null
  programNameVn?: string | null

  cohortId: number
  cohortName: string
  cohortEntryYear?: number | null

  academicYear: string
  semester: string

  courseTypeId?: number | null
  courseTypeCode?: string | null
  courseTypeName?: string | null
  courseTypeNameVn?: string | null

  scopeKey: string
  dataSource?: string | null

  summary: PloCoverageSummary
  plos: PloCoverageItem[]
  warnings: string[]
}

export interface CurriculumChangeSummary {
  oldCourseCount: number
  newCourseCount: number
  addedCourseCount: number
  removedCourseCount: number
  modifiedCourseCount: number
  unchangedCourseCount: number
  oldTotalCredits: number
  newTotalCredits: number
  creditDifference: number
  metadataChangeCount: number
}

export interface CurriculumChangeReportData {
  programId: number
  programCode: string
  programName?: string | null
  programNameVn?: string | null

  oldCohortId: number
  oldCohortName: string
  oldCohortEntryYear?: number | null

  newCohortId: number
  newCohortName: string
  newCohortEntryYear?: number | null

  scopeKey: string
  summary: CurriculumChangeSummary

  courseChanges: unknown[]
  metadataChanges: unknown[]
}

export const reportApi = {
  exportCloPloMatrixExcel: (
    programId: number,
    scope: CloPloMatrixExportScope,
  ) =>
    api.get("/api/reports/clo-plo-matrix/excel", {
      params: {
        programId,
        ...scope,
      },
      responseType: "blob",
    }),

  exportCloPloMatrixPdf: (
    programId: number,
    scope: CloPloMatrixExportScope,
  ) =>
    api.get("/api/reports/clo-plo-matrix/pdf", {
      params: {
        programId,
        ...scope,
      },
      responseType: "blob",
    }),

  exportSyllabusListExcel: (
    scope: SyllabusListExportScope,
  ) =>
    api.get("/api/reports/syllabus-list/excel", {
      params: scope,
      responseType: "blob",
    }),

  exportSyllabusListPdf: (
    scope: SyllabusListExportScope,
  ) =>
    api.get("/api/reports/syllabus-list/pdf", {
      params: scope,
      responseType: "blob",
    }),

  getPloCoverageReport: (
    scope: PloCoverageReportScope,
  ): Promise<PloCoverageReportData> =>
    api
      .get<PloCoverageReportData>(
        "/api/reports/plo-coverage",
        {
          params: scope,
        },
      )
      .then((response) => response.data),

  exportPloCoverageExcel: (
    scope: PloCoverageReportScope,
  ) =>
    api.get("/api/reports/plo-coverage/excel", {
      params: scope,
      responseType: "blob",
    }),

  exportPloCoveragePdf: (
    scope: PloCoverageReportScope,
  ) =>
    api.get("/api/reports/plo-coverage/pdf", {
      params: scope,
      responseType: "blob",
    }),
}

export const curriculumChangeReportApi = {
  get: (
    scope: CurriculumChangeReportScope,
  ): Promise<CurriculumChangeReportData> =>
    api
      .get<CurriculumChangeReportData>(
        "/api/reports/curriculum-change",
        {
          params: scope,
        },
      )
      .then((response) => response.data),

  exportExcel: (
    scope: CurriculumChangeReportScope,
  ) =>
    api.get(
      "/api/reports/curriculum-change/excel",
      {
        params: scope,
        responseType: "blob",
      },
    ),

  exportPdf: (
    scope: CurriculumChangeReportScope,
  ) =>
    api.get(
      "/api/reports/curriculum-change/pdf",
      {
        params: scope,
        responseType: "blob",
      },
    ),
}
