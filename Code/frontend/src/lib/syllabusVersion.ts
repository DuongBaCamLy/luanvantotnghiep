/** Prefer the backend label; numeric workflow revisions have no minor component. */
export function formatVersionLabel(
  versionNumber?: number | null,
  versionLabel?: string | null,
): string {
  const label = versionLabel?.trim()
  if (label && /^v[1-9]\d*\.0$/.test(label)
      && (versionNumber == null || label === `v${versionNumber}.0`)) {
    return label
  }
  if (versionNumber != null && Number.isSafeInteger(versionNumber) && versionNumber > 0) {
    return `v${versionNumber}.0`
  }
  const legacy = label?.match(/^(?:Version\s+|v)?([1-9]\d*)(?:\.0)?$/i)
  return legacy ? `v${legacy[1]}.0` : "—"
}

/** Numeric ordering, independent of display labels and cohort comparison rules. */
export function compareSyllabusVersions(
  left: { versionNumber?: number | null },
  right: { versionNumber?: number | null },
): number {
  return (left.versionNumber ?? 0) - (right.versionNumber ?? 0)
}
