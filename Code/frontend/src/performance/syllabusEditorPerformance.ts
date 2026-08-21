/**
 * NFR-01.2
 * Syllabus Editor Performance Monitoring
 *
 * Target:
 * Syllabus Editor load time < 1500ms
 */


export interface SyllabusEditorPerformance {

  syllabusId: number

  /**
   * Total loading/rendering duration
   * milliseconds
   */
  durationMs: number


  /**
   * Performance target
   */
  targetMs: number


  /**
   * true if duration <= target
   */
  passed: boolean


  /**
   * Measurement timestamp
   */
  measuredAt: string
}



/**
 * Measure syllabus editor loading time
 *
 * @param syllabusId syllabus identifier
 * @param start performance.now() when opening editor
 */
export function measureSyllabusEditorLoad(
  syllabusId: number,
  start: number
): SyllabusEditorPerformance {


  const end =
    performance.now()


  const duration =
    end - start



  const result: SyllabusEditorPerformance = {

    syllabusId,


    durationMs:
      Number(
        duration.toFixed(2)
      ),


    targetMs:
      1500,


    passed:
      duration <= 1500,


    measuredAt:
      new Date().toISOString()

  }



  /**
   * Store result globally
   * Used for:
   * - DevTools checking
   * - NFR acceptance testing
   */
  window.__LVTN_SYLLABUS_EDITOR_PERF__
    = result



  /**
   * Console log for QA/performance verification
   */
  console.info(
    "[NFR-01.2] Syllabus Editor Performance",
    {
      syllabusId:
        result.syllabusId,

      durationMs:
        result.durationMs,

      targetMs:
        result.targetMs,

      status:
        result.passed
          ? "PASS"
          : "FAIL"
    }
  )



  return result
}



/**
 * Start timer when opening editor
 *
 * Usage:
 *
 * const start = startSyllabusEditorTimer()
 *
 * measureSyllabusEditorLoad(id,start)
 */
export function startSyllabusEditorTimer(): number {

  return performance.now()

}




declare global {

  interface Window {


    /**
     * Latest NFR-01.2 measurement
     */
    __LVTN_SYLLABUS_EDITOR_PERF__?:
      SyllabusEditorPerformance


  }

}