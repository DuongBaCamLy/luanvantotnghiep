package com.scse.curriculum.performance;


import java.time.Instant;


public record CloPloMatrixPerformance(

        long courseCount,

        long durationMs,

        long targetMs,

        boolean passed,

        String measuredAt

) {


    public static CloPloMatrixPerformance measure(
            long courseCount,
            long durationMs
    ){

        long target = 5000;


        return new CloPloMatrixPerformance(

                courseCount,

                durationMs,

                target,

                durationMs <= target,

                Instant.now().toString()

        );
    }

}