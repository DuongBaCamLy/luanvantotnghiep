package com.scse.curriculum.performance;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/performance")
public class ReportPerformanceController {


private final ReportPerformanceService service;



public ReportPerformanceController(
        ReportPerformanceService service
){

    this.service = service;

}



@GetMapping("/report-export")
public ReportExportPerformance test(

@RequestParam String reportType,

@RequestParam String format,

@RequestParam long durationMs

){


return service.measure(
        reportType,
        format,
        durationMs
);


}


}