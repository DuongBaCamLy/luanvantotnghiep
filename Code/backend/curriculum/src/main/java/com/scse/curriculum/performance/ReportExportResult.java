package com.scse.curriculum.performance;


public record ReportExportResult(

        String report,

        String format,

        long durationMs,

        long targetMs,

        boolean passed,

        String measuredAt

) {}