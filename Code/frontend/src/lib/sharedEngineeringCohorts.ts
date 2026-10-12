/** Keep NE/CE curriculum versions selectable; each cohort still owns its exact Program. */
export function engineeringCohortProgramIds(
  programs: { id: number; majorCode?: string }[], majorCode: string,
): Set<number> | undefined {
  const key = majorCode.trim().toUpperCase()
  if (key !== "NE" && key !== "CE") return undefined
  return new Set(programs.filter(program => program.majorCode?.trim().toUpperCase() === key).map(program => program.id))
}
