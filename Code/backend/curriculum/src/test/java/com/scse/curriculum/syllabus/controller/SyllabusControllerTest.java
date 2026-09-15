package com.scse.curriculum.syllabus.controller;


import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.common.security.XssInputValidator;

import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusListContextResponse;

import com.scse.curriculum.syllabus.service.SyllabusManagementService;
import com.scse.curriculum.syllabus.service.SyllabusService;

import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;
import com.scse.curriculum.syllabus.word.SyllabusWordService;

import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.syllabus.service.SyllabusServiceImpl;

import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;
import com.scse.curriculum.syllabus.comparison.model.SemanticAnalysisStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@WebMvcTest(SyllabusController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyllabusControllerTest {

    @Test
    void deleteApproved_shouldReturn403WithArchiveInstruction() throws Exception {
        doThrow(new ForbiddenOperationException(
                "Approved syllabus cannot be deleted. Archive it instead."))
                .when(syllabusService).delete(100);

        mockMvc.perform(delete("/api/syllabuses/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Approved syllabus cannot be deleted. Archive it instead."));
    }

    @Test
    void deleteAll_isRemovedAndOldUrlCannotInvokeDeletion() throws Exception {
        assertThat(SyllabusService.class.getMethods()).extracting("name").doesNotContain("deleteAll");
        assertThat(SyllabusServiceImpl.class.getMethods()).extracting("name").doesNotContain("deleteAll");
        assertThat(SyllabusController.class.getMethods()).extracting("name").doesNotContain("deleteAll");

        mockMvc.perform(delete("/api/syllabuses/all"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Allow"))
                        .contains("GET", "PUT").doesNotContain("DELETE"));

        verifyNoInteractions(syllabusService, syllabusManagementService);
    }



    @Autowired
    private MockMvc mockMvc;



    /*
     * Security dependencies
     */

    @MockBean
    private JwtService jwtService;


    @MockBean
    private XssInputValidator xssInputValidator;



    /*
     * Controller dependencies
     */

    @MockBean
    private SyllabusService syllabusService;


    @MockBean
    private SyllabusManagementService syllabusManagementService;


    @MockBean
    private SyllabusPdfService syllabusPdfService;


@MockBean
private SyllabusWordService syllabusWordService;


    @Test
    void listContext_shouldReturn200()
            throws Exception {


        when(
                syllabusManagementService.getListContext()
        )
        .thenReturn(
                SyllabusListContextResponse.builder()
                        .build()
        );


        mockMvc.perform(
                get("/api/syllabuses/list-context")
        )
        .andExpect(
                status().isOk()
        );

    }






    @Test
    void createContext_shouldReturn200()
            throws Exception {


        when(
                syllabusService.getCreateContext(anyInt())
        )
        .thenReturn(
                SyllabusCreateContextResponse.builder()
                        .build()
        );


        mockMvc.perform(
                get("/api/syllabuses/create-context")
                        .param(
                                "courseId",
                                "1"
                        )
                        .param(
                                "academicYear",
                                "2026"
                        )
                        .param(
                                "semester",
                                "Semester 1"
                        )
        )
        .andExpect(
                status().isOk()
        );

        verify(syllabusService).getCreateContext(1);

    }

@Test
void semanticDiff_shouldPreserveOldNewDirection()
        throws Exception {

    SemanticSyllabusDiffResponse response =
            SemanticSyllabusDiffResponse.builder()
                    .oldSyllabusId(100)
                    .newSyllabusId(200)
                    .courseId(10)
                    .courseCode("IT116IU")
                    .courseName("C/C++ Programming")
                    .status(
                            SemanticAnalysisStatus.SUCCESS
                    )
                    .hasMeaningfulChanges(true)
                    .hasUnresolvedItems(false)
                    .summary(
                            "Semantic comparison completed."
                    )
                    .build();

    when(
            syllabusService.getSemanticDiff(
                    100,
                    200
            )
    ).thenReturn(
            response
    );

    mockMvc.perform(
            post(
                    "/api/syllabuses/200/diff/semantic"
            )
                    .param(
                            "compareWith",
                            "100"
                    )
    )
    .andExpect(
            status().isOk()
    );

    /*
     * Critical:
     *
     * compareWith must remain OLD.
     * path id must remain NEW.
     */
    verify(
            syllabusService
    ).getSemanticDiff(
            100,
            200
    );
}
}
