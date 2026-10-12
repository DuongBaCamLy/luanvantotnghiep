package com.scse.curriculum.auditlog.controller;

import com.scse.curriculum.auditlog.service.AuditLogService;
import com.scse.curriculum.auditlog.dto.AuditLogPageResponse;
import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.common.security.XssInputValidator;
import com.scse.curriculum.config.PhaseRoleGuard;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AuditLogControllerTest.MethodSecurity.class, PhaseRoleGuard.class})
class AuditLogControllerTest {
    @TestConfiguration @EnableMethodSecurity static class MethodSecurity { }
    @Autowired MockMvc mvc;
    @MockBean AuditLogService service;
    @MockBean UserAccountRepository users;
    @MockBean JwtService jwt;
    @MockBean XssInputValidator xss;

    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCanReadPageAndOriginalRecentEndpoint() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(UserAccount.builder().role(UserRole.ADMIN).isActive(true).build()));
        when(service.getPage("", null, 0, 25)).thenReturn(new AuditLogPageResponse(List.of(), 0, 25, 0, 0));
        mvc.perform(get("/api/audit-logs/page")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray()).andExpect(jsonPath("$.size").value(25));
        when(service.getRecent(8)).thenReturn(List.of());
        mvc.perform(get("/api/audit-logs/recent")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test @WithMockUser(username = "dean", roles = "ADMIN")
    void persistedDeanIsDeniedEvenWithAdminAuthority() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("dean", "dean"))
                .thenReturn(Optional.of(UserAccount.builder().role(UserRole.DEAN).isActive(true).build()));
        mvc.perform(get("/api/audit-logs/page")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(username = "admin", roles = "ADMIN")
    void inactiveAdminIsDenied() throws Exception {
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(UserAccount.builder().role(UserRole.ADMIN).isActive(false).build()));
        mvc.perform(get("/api/audit-logs/page")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void invalidActionIsRejectedBeforeQuery() throws Exception {
        mvc.perform(get("/api/audit-logs/page").param("action", "NOT_A_REAL_ACTION"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
