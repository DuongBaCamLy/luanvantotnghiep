package com.scse.curriculum.performance;


import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;



@Component
public class ReportPerformanceFilter
        implements Filter {



@Override
public void doFilter(

        ServletRequest request,

        ServletResponse response,

        FilterChain chain

)
throws IOException, ServletException {


    long start =
            System.currentTimeMillis();


    chain.doFilter(
            request,
            response
    );


    long duration =
            System.currentTimeMillis()
            - start;



    HttpServletResponse http =
            (HttpServletResponse) response;



    http.setHeader(
            "X-LVTN-Report-Export-Time",
            duration + "ms"
    );


}

}