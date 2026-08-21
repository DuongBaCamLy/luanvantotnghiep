package com.scse.curriculum.performance;


import lombok.Builder;
import lombok.Data;


/**
 * Kết quả benchmark NFR-01.5
 */
@Data
@Builder
public class LoadTestResult {


    private int concurrentUsers;


    private long averageResponseMs;


    private long p95ResponseMs;


    private double errorRate;


    private boolean passed;


}