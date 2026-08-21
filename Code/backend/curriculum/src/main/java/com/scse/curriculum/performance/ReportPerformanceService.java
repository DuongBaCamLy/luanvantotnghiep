package com.scse.curriculum.performance;


import org.springframework.stereotype.Service;


@Service
public class ReportPerformanceService {


    public ReportExportPerformance measure(

            String reportType,

            String format,

            long start

    ){


        long duration =
                System.currentTimeMillis()
                - start;


        ReportExportPerformance result =
                ReportExportPerformance.create(
                        reportType,
                        format,
                        duration
                );


        return result;

    }


}