package com.scse.curriculum.performance;


import java.time.Instant;


public record ReportExportPerformance(

        String reportType,

        String format,

        long durationMs,

        long targetMs,

        boolean passed,

        String measuredAt

) {


    public static ReportExportPerformance create(
            String reportType,
            String format,
            long durationMs
    ){

        long target = 10000;


        return new ReportExportPerformance(

                reportType,

                format,

                durationMs,

                target,

                durationMs <= target,

                Instant.now().toString()

        );

    }

}