package com.scse.curriculum.dashboard.controller;

import com.scse.curriculum.dashboard.dto.*;
import com.scse.curriculum.dashboard.service.DashboardService;
import com.scse.curriculum.dashboard.service.CreditDistributionQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CreditDistributionQueryService creditDistributionQueryService;

    public DashboardController(
            DashboardService dashboardService,
            CreditDistributionQueryService creditDistributionQueryService) {
        this.dashboardService = dashboardService;
        this.creditDistributionQueryService = creditDistributionQueryService;
    }

    @GetMapping("/dean")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<DashboardDeanResponse> getDeanDashboard(
            @RequestParam(required = false) Integer majorId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(dashboardService.getDeanDashboard(
                majorId, cohortId, semester));
    }

    @GetMapping("/dept-head/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPT_HEAD') or hasRole('ADMIN')")
    public ResponseEntity<DashboardDeptHeadResponse> getDeptHeadDashboard(@PathVariable long userId) {
        return ResponseEntity.ok(dashboardService.getDeptHeadDashboard(userId));
    }

    @GetMapping("/faculty/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<DashboardFacultyResponse> getMyFacultyDashboard() {
        return ResponseEntity.ok(dashboardService.getMyFacultyDashboard());
    }

    @GetMapping("/faculty/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR') or hasRole('ADMIN')")
    public ResponseEntity<DashboardFacultyResponse> getFacultyDashboard(@PathVariable long userId) {
        return ResponseEntity.ok(dashboardService.getFacultyDashboard(userId));
    }

    @GetMapping("/admin")
// TEMPORARY: Instructor reuses the Admin dashboard for UI testing.
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public ResponseEntity<DashboardAdminResponse> getAdminDashboard(
        @RequestParam(required = false) String academicYear,
        @RequestParam(required = false) Integer semester,
        @RequestParam(required = false) Integer programId,
        @RequestParam(required = false) Integer cohortId) {

    return ResponseEntity.ok(
            dashboardService.getAdminDashboard(
                    academicYear,
                    semester,
                    programId,
                    cohortId));
}
@GetMapping("/admin/terms")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public ResponseEntity<List<DashboardAdminResponse.TermOption>>
getAdminTermOptions() {

    return ResponseEntity.ok(
            dashboardService.getAdminTermOptions()
    );
}


    @GetMapping("/credit-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public ResponseEntity<CreditDistributionResponse> getCreditDistribution(
            @RequestParam Integer programId,
            @RequestParam Integer cohortId) {
        return ResponseEntity.ok(
                creditDistributionQueryService.getDistribution(programId, cohortId));
    }

    @GetMapping("/heatmap/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public ResponseEntity<DashboardHeatmapResponse> getHeatmapCoverage(
            @PathVariable long programId,
            @RequestParam(required = false) Integer cohortId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) Integer courseTypeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(dashboardService.getHeatmapCoverage(
                programId, cohortId, academicYear, semester, courseTypeId,
                search, status));
    }
}
