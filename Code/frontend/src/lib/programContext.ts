import type { Cohort, Major, Program } from "@/types/admin"

export const clearProgramContext = (
  params: URLSearchParams
) => {
  params.delete("programId")
  params.delete("programCode")
  params.delete("cohortId")

  // Tham số cũ
  params.delete("cohort")
  params.delete("academicYear")
}

export const clearMajorContext = (
  params: URLSearchParams
) => {
  params.delete("majorId")
  params.delete("majorCode")

  // Tham số cũ
  params.delete("major")
}

export const setMajorContext = (
  params: URLSearchParams,
  major: Major
) => {
  params.set("majorId", String(major.id))
  params.set("majorCode", major.code)

  params.delete("major")
}

export const setProgramContext = (
  params: URLSearchParams,
  program: Program,
  cohort?: Cohort,
) => {
  params.set("programId", String(program.id))
  params.set("programCode", program.code)

  params.set("majorId", String(program.majorId))
  params.set("majorCode", program.majorCode)

  if (cohort) {
    params.set("cohortId", String(cohort.id))
  } else {
    params.delete("cohortId")
  }

  // Xóa tham số cũ để tránh lọc lẫn
  params.delete("cohort")
  params.delete("academicYear")
  params.delete("major")
}

export const getSyllabusBasePath = (
  pathname: string
) => {
  if (pathname.startsWith("/dept-head")) {
    return "/dept-head/syllabus"
  }

  if (pathname.startsWith("/dean")) {
    return "/dean/syllabus"
  }

  if (pathname.startsWith("/coordinator")) {
    return "/coordinator/syllabus"
  }

  if (pathname.startsWith("/instructor")) {
    return "/instructor/syllabus"
  }

  return "/admin/syllabus"
}
