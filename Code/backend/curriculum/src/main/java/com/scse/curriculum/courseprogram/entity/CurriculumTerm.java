package com.scse.curriculum.courseprogram.entity;

public enum CurriculumTerm {
    HK1,
    HK2,
    HK3,
    HK4,
    HK5,
    HK6,
    SUMMER,
    HK7,
    HK8,
    ELECTIVE;

    public boolean isRegularSemester() {
        return name().matches("HK[1-8]");
    }

    public Integer getSemesterNumber() {
        if (!isRegularSemester()) {
            return null;
        }

        return Integer.parseInt(
                name().substring(2)
        );
    }

    public static CurriculumTerm fromValue(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Học kỳ không được để trống"
            );
        }

        String normalized = value
                .trim()
                .toUpperCase()
                .replace("SEMESTER", "HK")
                .replace("HỌC KỲ", "HK")
                .replace(" ", "");

        if (normalized.matches("[1-8]")) {
            normalized = "HK" + normalized;
        }

        return CurriculumTerm.valueOf(normalized);
    }
}