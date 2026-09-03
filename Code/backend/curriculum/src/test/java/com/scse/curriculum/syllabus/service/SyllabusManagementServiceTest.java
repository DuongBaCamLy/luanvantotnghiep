package com.scse.curriculum.syllabus.service;


import com.scse.curriculum.syllabus.dto.SyllabusCatalogResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SyllabusListContextResponse;

import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;

import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import org.mockito.InjectMocks;
import org.mockito.Mock;


import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class SyllabusManagementServiceTest {




    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private SyllabusAccessService syllabusAccessService;




    @InjectMocks
    private SyllabusManagementServiceImpl service;








    /**
     * =====================================================
     *
     * TEST ADMIN LIST CONTEXT
     *
     * =====================================================
     */
    @Test
    void getListContext_shouldReturnContext(){



        when(
                syllabusRepository.findAdminList()
        )
        .thenReturn(
                List.of()
        );



        when(
                syllabusRepository.findCourseFilters()
        )
        .thenReturn(
                List.of()
        );



        when(
                syllabusRepository.findProgramFilters()
        )
        .thenReturn(
                List.of()
        );



        when(
                syllabusRepository.findSemesterFilters()
        )
        .thenReturn(
                List.of()
        );



        when(
                syllabusRepository.findStatusFilters()
        )
        .thenReturn(
                List.of()
        );



        when(
                syllabusRepository.findUserFilters()
        )
        .thenReturn(
                List.of()
        );





        SyllabusListContextResponse response =

                service.getListContext();





        assertNotNull(response);



        assertNotNull(
                response.getSyllabuses()
        );



        assertNotNull(
                response.getFilters()
        );



        verify(
                syllabusRepository
        )
        .findAdminList();

    }










    /**
     * =====================================================
     *
     * TEST SYLLABUS CATALOG
     *
     * Catalog columns:
     *
     * Course
     * Version
     * Program
     * Created / Imported By
     * Status
     * Final Approval Date
     * Actions
     *
     * =====================================================
     */
    @Test
    void getCatalog_shouldReturnCatalog(){
        when(syllabusAccessService.currentUser())
                .thenReturn(UserAccount.builder().role(UserRole.ADMIN).build());

        when(
                syllabusRepository.findAdminList()
        )
        .thenReturn(
                List.of()
        );





        SyllabusCatalogResponse response =

                service.getCatalog();





        assertNotNull(response);



        assertNotNull(
                response.getSyllabuses()
        );





        verify(
                syllabusRepository
        )
        .findAdminList();


    }

    @Test
    void getCatalog_shouldExcludeSyllabusOutsideCurrentUserScope() {
        Syllabus inaccessible = new Syllabus();

        when(syllabusAccessService.currentUser())
                .thenReturn(UserAccount.builder().role(UserRole.ADMIN).build());

        when(syllabusRepository.findAdminList())
                .thenReturn(List.of(inaccessible));
        when(syllabusAccessService.canView(inaccessible))
                .thenReturn(false);

        SyllabusCatalogResponse response = service.getCatalog();

        assertTrue(response.getSyllabuses().isEmpty());
        verify(syllabusAccessService).canView(inaccessible);
    }

    @Test
    void getCatalog_shouldApplyMajorScopeThroughAccessServiceForDeptHead() {
        UserAccount deptHead = UserAccount.builder()
                .role(UserRole.DEPT_HEAD)
                .build();
        Syllabus allowed = Syllabus.builder().id(1).build();
        Syllabus denied = Syllabus.builder().id(2).build();

        when(syllabusAccessService.currentUser()).thenReturn(deptHead);
        when(syllabusRepository.findAdminList()).thenReturn(List.of(allowed, denied));
        when(syllabusAccessService.canView(allowed)).thenReturn(true);
        when(syllabusAccessService.canView(denied)).thenReturn(false);

        SyllabusCatalogResponse response = service.getCatalog();

        assertNotNull(response);
        assertEquals(1, response.getSyllabuses().size());
        verify(syllabusRepository).findAdminList();
        verify(syllabusAccessService).canView(allowed);
        verify(syllabusAccessService).canView(denied);
    }









    /**
     * =====================================================
     *
     * TEST CREATE CONTEXT
     *
     * Priority:
     *
     * Latest Approved
     *
     *       |
     *
     *       v
     *
     * Latest Version
     *
     * =====================================================
     */
    @Test
    void getCreateContext_shouldUseLatestApprovedFirst(){





        Syllabus approved =

                Syllabus.builder()

                        .id(1)

                        .status(
                                SyllabusStatus.APPROVED
                        )

                        .build();






        Syllabus latest =

                Syllabus.builder()

                        .id(2)

                        .build();







        when(
                syllabusRepository.findLatestApproved(1)
        )
        .thenReturn(
                Optional.of(approved)
        );







        when(
                syllabusRepository.findLatestVersion(1)
        )
        .thenReturn(
                Optional.of(latest)
        );









        SyllabusCreateContextResponse response =

                service.getCreateContext(

                        1,

                        "2026",

                        "Semester 1"

                );









        assertNotNull(response);








        verify(
                syllabusRepository
        )
        .findLatestApproved(1);








        verify(
                syllabusRepository
        )
        .findLatestVersion(1);



    }





}
