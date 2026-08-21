package com.scse.curriculum.performance;


import org.springframework.context.annotation.Configuration;


/**
 * NFR-01.5
 *
 * Concurrent User Performance Requirement
 *
 * Mục tiêu:
 * - Hỗ trợ tối thiểu 50 người dùng đồng thời
 * - Response time ổn định
 * - Error rate thấp
 *
 */
@Configuration
public class ConcurrentUserPerformanceConfig {


    /**
     * Số user đồng thời yêu cầu
     */
    public static final int REQUIRED_CONCURRENT_USERS = 50;


    /**
     * Ngưỡng response time P95
     */
    public static final long MAX_RESPONSE_TIME_MS = 3000;


    /**
     * Tỷ lệ lỗi cho phép
     */
    public static final double MAX_ERROR_RATE = 0.01;


}