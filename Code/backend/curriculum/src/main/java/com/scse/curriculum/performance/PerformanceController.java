package com.scse.curriculum.performance;


import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/performance")
public class PerformanceController {


private final CloPloMatrixBenchmarkService service;



public PerformanceController(
        CloPloMatrixBenchmarkService service
){
    this.service = service;
}



@GetMapping("/clo-plo-matrix")
public CloPloMatrixPerformance benchmark(

@RequestParam Long programId,

@RequestParam Integer cohortId,

@RequestParam String academicYear,

@RequestParam String semester,

@RequestParam Integer courseTypeId

){

return service.benchmark(
        programId,
        cohortId,
        academicYear,
        semester,
        courseTypeId
);

}

}