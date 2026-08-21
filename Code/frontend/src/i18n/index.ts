import { en } from "@/i18n/en"

export const APP_LOCALE = "en-US"

export type TranslationKey = keyof typeof en
export type TranslationValues = Record<string, string | number>

export function t(
  key: TranslationKey,
  values: TranslationValues = {}
): string {
  let result: string = en[key]

  for (const [name, value] of Object.entries(values)) {
    result = result.replaceAll(`{${name}}`, String(value))
  }

  return result
}

export function formatDateTime(
  value: string | null | undefined,
  options: Intl.DateTimeFormatOptions = {
    dateStyle: "short",
    timeStyle: "short",
  }
): string {
  if (!value) return "-"

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(APP_LOCALE, options).format(date)
}