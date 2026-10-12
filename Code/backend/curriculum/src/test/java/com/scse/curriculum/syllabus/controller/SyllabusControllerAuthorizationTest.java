
package com.scse.curriculum.syllabus.controller;

import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.common.security.XssInputValidator;
import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;
import com.scse.curriculum.syllabus.word.SyllabusWordService;
import com.scse.curriculum.syllabus.service.SyllabusService;
import com.scse.curriculum.syllabus.service.SyllabusManagementService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(SyllabusController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SyllabusControllerAuthorizationTest.MethodSecurity.class)
class SyllabusControllerAuthorizationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurity {
    }

    @Autowired
    private SyllabusController controller;

    @MockBean
    private SyllabusService syllabusService;

    @MockBean
    private SyllabusManagementService syllabusManagementService;

    @MockBean
    private SyllabusPdfService syllabusPdfService;

    @MockBean
    private SyllabusWordService syllabusWordService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private XssInputValidator xssInputValidator;

    @Test
    @WithMockUser(roles = "DEAN")
    void deanCanUseFiveDraftEndpointsButCannotDeleteOrClone() {
        assertWriteEndpointsAllowed();

        assertThatThrownBy(() -> controller.delete(1))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.clone(1, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructorRetainsExistingControllerPermissions() {
        assertWriteEndpointsAllowed();

        assertThatCode(() -> controller.delete(1))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.clone(1, null))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRetainsExistingControllerPermissions() {
        assertWriteEndpointsAllowed();

        assertThatCode(() -> controller.delete(1))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.clone(1, null))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(roles = "DEPT_HEAD")
    void departmentHeadCannotUseDraftWriteEndpoints() {
        assertWriteEndpointsDenied();
    }

    @Test
    @WithMockUser(roles = "DEAN_SECRETARY")
    void deanSecretaryCannotUseDraftWriteEndpoints() {
        assertWriteEndpointsDenied();
    }

    private void assertWriteEndpointsAllowed() {
        assertThatCode(() -> controller.createContext(1, null, null))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.create(null))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.update(1, null))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.validateForSubmit(1))
                .doesNotThrowAnyException();

        assertThatCode(() -> controller.submit(1))
                .doesNotThrowAnyException();
    }

    private void assertWriteEndpointsDenied() {
        assertThatThrownBy(() -> controller.createContext(1, null, null))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.create(null))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.update(1, null))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.validateForSubmit(1))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> controller.submit(1))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(syllabusService);
    }
}
