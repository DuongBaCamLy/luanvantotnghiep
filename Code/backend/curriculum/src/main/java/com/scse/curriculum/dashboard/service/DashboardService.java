package com.scse.curriculum.dashboard.service;
import java.util.List;
import com.scse.curriculum.dashboard.dto.*;

public interface DashboardService {
    DashboardDeanResponse getDeanDashboard(
            Integer majorId,
            Integer cohortId,
            Integer semester);
    List<DashboardAdminResponse.TermOption> getAdminTermOptions();
    DashboardDeptHeadResponse getDeptHeadDashboard(long deptHeadUserId);
    DashboardFacultyResponse getFacultyDashboard(long facultyUserId);
    DashboardFacultyResponse getMyFacultyDashboard();
    DashboardAdminResponse getAdminDashboard(
        String academicYear,
        Integer semester,
        Integer majorId,
        Integer programId,
        Integer cohortId);
    DashboardHeatmapResponse getHeatmapCoverage(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);

    DashboardHeatmapResponse getHeatmapCoverage(long programId, Integer cohortId, String academicYear,
                                                 String semester, Integer courseTypeId,
                                                 String search, String status);
}
