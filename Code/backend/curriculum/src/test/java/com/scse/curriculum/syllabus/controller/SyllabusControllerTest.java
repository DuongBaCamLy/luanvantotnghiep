package com.scse.curriculum.syllabus.controller;


import com.scse.curriculum.auth.security.JwtService;
import com.scse.curriculum.common.security.XssInputValidator;

import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusListContextResponse;

import com.scse.curriculum.syllabus.service.SyllabusManagementService;
import com.scse.curriculum.syllabus.service.SyllabusService;

import com.scse.curriculum.syllabus.pdf.SyllabusPdfService;


import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@WebMvcTest(SyllabusController.class)
@AutoConfigureMockMvc(addFilters = false)
class SyllabusControllerTest {



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


}
