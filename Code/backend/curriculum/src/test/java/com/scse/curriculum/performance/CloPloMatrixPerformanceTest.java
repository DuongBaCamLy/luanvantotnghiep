package com.scse.curriculum.performance;


import com.scse.curriculum.dashboard.dto.DashboardHeatmapResponse;
import com.scse.curriculum.dashboard.service.DashboardService;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;


import java.util.ArrayList;



import static org.mockito.Mockito.*;



class CloPloMatrixPerformanceTest {



@Test
void generate100CoursesMatrixShouldBeUnder5Seconds(){


    DashboardService dashboardService =
            mock(DashboardService.class);



    DashboardHeatmapResponse response =
            new DashboardHeatmapResponse();



    var courses =
            new ArrayList<
                    DashboardHeatmapResponse.CourseCoverage
            >();



    for(
        int i=1;
        i<=100;
        i++
    ){

        DashboardHeatmapResponse.CourseCoverage course =
                new DashboardHeatmapResponse.CourseCoverage();


        course.setCourseId(i);

        course.setCourseCode(
                "IT"+i
        );

        course.setCourseNameVn(
                "Course "+i
        );


        courses.add(course);

    }



    response.setCourseCoverages(
            courses
    );



    when(
        dashboardService.getHeatmapCoverage(
                1L,
                1,
                "2026-2027",
                "1",
                3
        )
    )
    .thenReturn(response);



    long start =
            System.currentTimeMillis();



    DashboardHeatmapResponse result =
            dashboardService.getHeatmapCoverage(
                    1L,
                    1,
                    "2026-2027",
                    "1",
                    3
            );



    long duration =
            System.currentTimeMillis()
                    - start;



    Assertions.assertEquals(
            100,
            result.getCourseCoverages()
                    .size()
    );


    Assertions.assertTrue(
            duration < 5000,
            "CLO-PLO matrix generation exceeded 5 seconds: "
                    + duration
                    + "ms"
    );

}

}