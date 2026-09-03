package com.scse.curriculum.syllabus.service;


import com.scse.curriculum.course.entity.Course;

import com.scse.curriculum.syllabus.dto.*;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;



@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyllabusManagementServiceImpl
        implements SyllabusManagementService {



    private final SyllabusRepository syllabusRepository;

    private final SyllabusAccessService syllabusAccessService;



    /*
     =====================================================
     ADMIN LIST CONTEXT
     =====================================================
     */


    @Override
    public SyllabusListContextResponse getListContext(){


        UserAccount currentUser = syllabusAccessService.currentUser();
        List<Syllabus> scopedSource = syllabusRepository.findAdminList();
        List<SyllabusListItemResponse> syllabusList =

                scopedSource
                        .stream()
                        .filter(syllabusAccessService::canView)
                        .map(this::mapToListItem)
                        .toList();



        SyllabusFilterResponse filters =

                SyllabusFilterResponse.builder()

                        .courses(
                                syllabusRepository.findCourseFilters()
                        )

                        .programs(
                                syllabusRepository.findProgramFilters()
                        )

                        .semesters(
                                syllabusRepository.findSemesterFilters()
                        )

                        .statuses(

                                syllabusRepository
                                        .findStatusFilters()
                                        .stream()
                                        .map(Enum::name)
                                        .toList()

                        )

                        .users(
                                syllabusRepository.findUserFilters()
                        )

                        .build();



        return SyllabusListContextResponse.builder()

                .syllabuses(syllabusList)

                .filters(filters)

                .build();

    }





    /*
     =====================================================
     SYLLABUS CATALOG
     
     GET /api/syllabuses/catalog
     
     Dùng cho trang Catalog
     
     =====================================================
     */


    @Override
    public SyllabusCatalogResponse getCatalog(){
        UserAccount currentUser = syllabusAccessService.currentUser();
        List<Syllabus> catalogSource = syllabusRepository.findAdminList();

        List<SyllabusCatalogResponse.SyllabusCatalogItem> items =
                catalogSource

                        .stream()

                        .filter(syllabusAccessService::canView)

                        .map(this::mapToCatalogItem)

                        .toList();



        return SyllabusCatalogResponse.builder()

                .syllabuses(items)

                .build();

    }





    private SyllabusCatalogResponse.SyllabusCatalogItem
    mapToCatalogItem(
            Syllabus syllabus
    ){


        return SyllabusCatalogResponse
                .SyllabusCatalogItem
                .builder()


                .id(
                        syllabus.getId()
                )


                .courseCode(
                        syllabus.getCourseCodeSnapshot()
                )


                .courseName(
                        syllabus.getCourseNameSnapshot()
                )


                .version(
                        syllabus.getVersionLabel()
                )


                .program(
                        syllabus.getProgram()
                )


                .createdImportedBy(

                        syllabus.getCreatedBy()!=null
                                ?
                                syllabus.getCreatedBy()
                                        .getUsername()
                                :
                                null

                )


                .status(

                        syllabus.getStatus()!=null
                                ?
                                syllabus.getStatus()
                                        .name()
                                :
                                null

                )


                .finalApprovalDate(
                        syllabus.getFinalApprovalDate()
                )


                .actions(

                        List.of(
                                "VIEW",
                                "EXPORT",
                                "DELETE"
                        )

                )


                .build();

    }







    /*
     =====================================================
     CREATE CONTEXT
     =====================================================
     */


    @Override
    public SyllabusCreateContextResponse getCreateContext(

            Integer courseId,

            String academicYear,

            String semester

    ){



        Syllabus latestApproved =

                syllabusRepository
                        .findLatestApproved(courseId)
                        .orElse(null);



        Syllabus latestVersion =

                syllabusRepository
                        .findLatestVersion(courseId)
                        .orElse(null);




        Syllabus source =

                latestApproved != null
                        ?
                        latestApproved
                        :
                        latestVersion;




        Course course =

                source != null
                        ?
                        source.getCourse()
                        :
                        null;




        return SyllabusCreateContextResponse.builder()

                .course(
                        buildCourseContext(course)
                )

                .latestSyllabus(
                        mapToResponse(latestVersion)
                )


                .latestApprovedSyllabus(
                        mapToResponse(latestApproved)
                )


                .build();


    }







    private SyllabusListItemResponse mapToListItem(
            Syllabus syllabus
    ){


        return SyllabusListItemResponse.builder()


                .id(
                        syllabus.getId()
                )


                .courseCode(
                        syllabus.getCourseCodeSnapshot()
                )


                .courseName(
                        syllabus.getCourseNameSnapshot()
                )


                .version(
                        syllabus.getVersionLabel()
                )


                .program(
                        syllabus.getProgram()
                )


                .semester(
                        syllabus.getSemester()
                )


                .createdImportedBy(

                        syllabus.getCreatedBy()!=null
                                ?
                                syllabus.getCreatedBy()
                                        .getUsername()
                                :
                                null

                )


                .status(

                        syllabus.getStatus()!=null
                                ?
                                syllabus.getStatus()
                                        .name()
                                :
                                null

                )


                .finalApprovalDate(
                        syllabus.getFinalApprovalDate()
                )


                .build();

    }






    private SyllabusResponse mapToResponse(
            Syllabus syllabus
    ){


        if(syllabus==null)
            return null;



        return SyllabusResponse.builder()

                .id(
                        syllabus.getId()
                )


                .courseId(

                        syllabus.getCourse()!=null
                                ?
                                syllabus.getCourse().getId()
                                :
                                null

                )


                .courseCode(
                        syllabus.getCourseCodeSnapshot()
                )


                .courseName(
                        syllabus.getCourseNameSnapshot()
                )


                .versionNumber(
                        syllabus.getVersionNumber()
                )


                .versionLabel(
                        syllabus.getVersionLabel()
                )


                .academicYear(
                        syllabus.getAcademicYear()
                )


                .program(
                        syllabus.getProgram()
                )


                .semester(
                        syllabus.getSemester()
                )


                .sourceType(

                        syllabus.getSourceType()!=null
                                ?
                                syllabus.getSourceType().name()
                                :
                                null

                )


                .importStatus(

                        syllabus.getImportStatus()!=null
                                ?
                                syllabus.getImportStatus().name()
                                :
                                null

                )


                .status(

                        syllabus.getStatus()!=null
                                ?
                                syllabus.getStatus().name()
                                :
                                null

                )


                .createdAt(
                        syllabus.getCreatedAt()
                )


                .updatedAt(
                        syllabus.getUpdatedAt()
                )


                .build();

    }







    private SyllabusCreateContextResponse.CourseContext
    buildCourseContext(
            Course course
    ){


        if(course==null)
            return null;



        return SyllabusCreateContextResponse
                .CourseContext
                .builder()


                .id(
                        course.getId()
                )


                .courseCode(
                        course.getCourseCode()
                )


                .name(
                        course.getName()
                )


                .nameVn(
                        course.getNameVn()
                )


                .creditTheory(
                        course.getCreditTheory()
                )


                .creditLab(
                        course.getCreditLab()
                )


                .description(
                        course.getDescription()
                )


                .departmentName(

                        course.getDepartment()!=null
                                ?
                                course.getDepartment()
                                        .getName()
                                :
                                null

                )


                .build();

    }


}
