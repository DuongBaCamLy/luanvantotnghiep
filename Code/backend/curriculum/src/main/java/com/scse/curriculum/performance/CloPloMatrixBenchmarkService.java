package com.scse.curriculum.performance;


import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.dashboard.service.DashboardService;

import org.springframework.stereotype.Service;


@Service
public class CloPloMatrixBenchmarkService {


    private final DashboardService dashboardService;



    public CloPloMatrixBenchmarkService(
            DashboardService dashboardService
    ){
        this.dashboardService = dashboardService;
    }




    public CloPloMatrixPerformance benchmark(

            Long programId,

            Integer cohortId,

            String academicYear,

            String semester,

            Integer courseTypeId

    ){


        long start =
                System.currentTimeMillis();



        DashboardHeatmapResponse result =
                dashboardService.getHeatmapCoverage(

                        programId,

                        cohortId,

                        academicYear,

                        semester,

                        courseTypeId

                );



        long duration =
                System.currentTimeMillis()
                        - start;



        long courses =
                result.getCourseCoverages()
                        == null
                        ?
                        0
                        :
                        result.getCourseCoverages()
                                .size();



        return CloPloMatrixPerformance.measure(
                courses,
                duration
        );


    }

}