package com.scse.curriculum.dashboard.controller;

import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.common.security.XssInputValidator;
import com.scse.curriculum.dashboard.service.CreditDistributionQueryService;
import com.scse.curriculum.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DashboardControllerAuthorizationTest.MethodSecurity.class)
class DashboardControllerAuthorizationTest {
    // Exercise real @PreAuthorize interception; mock only the query services.
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurity {}

    @Autowired private MockMvc mvc;
    @MockBean private DashboardService dashboardService;
    @MockBean private CreditDistributionQueryService creditDistributionQueryService;
    @MockBean private JwtService jwtService;
    @MockBean private XssInputValidator xssInputValidator;

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructorCannotReadAdminAggregatesOrTerms() throws Exception {
        assertAdminForbidden();
    }

    @Test
    @WithMockUser(roles = "DEAN")
    void deanCannotReadAdminAggregatesOrTerms() throws Exception {
        assertAdminForbidden();
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    void departmentHeadCannotReadAdminAggregatesOrTerms() throws Exception {
        assertAdminForbidden();
    }

    private void assertAdminForbidden() throws Exception {
        mvc.perform(get("/api/dashboard/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard/admin/terms")).andExpect(status().isForbidden());
        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanReadAdminAndDeanDashboards() throws Exception {
        mvc.perform(get("/api/dashboard/admin")).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/admin/terms")).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/dean")).andExpect(status().isOk());
        verify(dashboardService)
        .getAdminDashboard(
                null,
                null,
                null,
                null,
                null);
        verify(dashboardService).getAdminTermOptions();
        verify(dashboardService).getDeanDashboard(null, null, null);
    }

    @Test
    @WithMockUser(roles = "DEAN")
    void deanCanReadFilteredDeanDashboard() throws Exception {
        mvc.perform(get("/api/dashboard/dean").param("majorId", "1")
                .param("cohortId", "10").param("semester", "2"))
                .andExpect(status().isOk());
        verify(dashboardService).getDeanDashboard(1, 10, 2);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructorUsesSelfDashboardAndCannotReadDeanOrHead() throws Exception {
        mvc.perform(get("/api/dashboard/faculty/me")).andExpect(status().isOk());
        verify(dashboardService).getMyFacultyDashboard();
        mvc.perform(get("/api/dashboard/dean")).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard/dept-head/10")).andExpect(status().isForbidden());
    }
}
