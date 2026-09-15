package com.scse.curriculum.syllabus.entity;

/** Workflow revisions are whole numbers; there is no minor-revision domain. */
public final class SyllabusVersion {
    private SyllabusVersion() {}

    public static String format(Integer versionNumber) {
        if (versionNumber == null || versionNumber < 1) {
            throw new IllegalArgumentException("Syllabus version number must be a positive integer.");
        }
        return "v" + versionNumber + ".0";
    }

    /** Read legacy labels without mutating historical entities or timestamps. */
    public static String display(Integer versionNumber, String legacyLabel) {
        if (versionNumber != null) {
            return format(versionNumber);
        }
        if (legacyLabel == null || legacyLabel.isBlank()) {
            return null;
        }
        var match = java.util.regex.Pattern.compile("(?i)^(?:Version\\s+|v)?([1-9][0-9]*)(?:\\.0)?$")
                .matcher(legacyLabel.trim());
        if (!match.matches()) {
            throw new IllegalArgumentException("Unsupported syllabus version label: " + legacyLabel);
        }
        return format(Integer.valueOf(match.group(1)));
    }

    public static void requireCanonical(Integer versionNumber, String label) {
        if (!format(versionNumber).equals(label)) {
            throw new IllegalArgumentException("Syllabus version label must be " + format(versionNumber)
                    + "; workflow version labels are system-controlled.");
        }
    }
}
