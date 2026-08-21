export interface DashboardPerformanceResult {
  durationMs: number
  measuredAt: string
  targetMs: number
  passed: boolean
  academicYear?: string | null
  semester?: number | null
}

declare global {
  interface Window {
    __LVTN_DASHBOARD_PERF__?: DashboardPerformanceResult
  }
}

export function saveDashboardPerformance(
  result: DashboardPerformanceResult,
) {
  window.__LVTN_DASHBOARD_PERF__ = result
}