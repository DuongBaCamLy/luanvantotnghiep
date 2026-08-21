package com.scse.curriculum.performance;


import java.time.Instant;


public final class ReportExportTimer {


    private ReportExportTimer(){}


    public static ReportExportResult finish(
            String report,
            String format,
            long start
    ){

        long duration =
                System.currentTimeMillis()
                - start;


        return new ReportExportResult(
                report,
                format,
                duration,
                10000,
                duration <= 10000,
                Instant.now().toString()
        );

    }

}