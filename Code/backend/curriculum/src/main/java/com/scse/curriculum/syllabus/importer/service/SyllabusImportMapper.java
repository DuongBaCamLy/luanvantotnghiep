package com.scse.curriculum.syllabus.importer.service;


import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.importer.dto.SyllabusImportData;

import org.springframework.stereotype.Component;


import java.util.stream.Collectors;


@Component
public class SyllabusImportMapper {


    public CreateSyllabusRequest toCreateRequest(
            SyllabusImportData data,
            Integer courseId
    ){


        CreateSyllabusRequest request =
                new CreateSyllabusRequest();


        request.setCourseId(courseId);



        /*
         * General information
         */

        request.setCourseDesignation(
                data.getCourseDesignation()
        );


        request.setCourseTypes(
                data.getCourseTypes()
        );


        request.setSemester(
                data.getSemester()
        );


        request.setLanguage(
                data.getLanguage()
        );


        request.setRelation(
                data.getRelation()
        );


        request.setTeachingMethods(
                data.getTeachingMethods()
        );



        /*
         * Workload
         */

        request.setWorkloadTotal(
                data.getWorkloadTotal()
        );


        request.setWorkloadContact(
                data.getWorkloadContact()
        );


        request.setWorkloadPrivate(
                data.getWorkloadPrivate()
        );



        /*
         * Requirements
         */


        request.setPrerequisites(
                data.getPrerequisites()
        );


        request.setObjectives(
                data.getObjectives()
        );


        request.setExamForms(
                data.getExamForms()
        );


        request.setExamRequirements(
                data.getExamRequirements()
        );


        request.setMajor(
                data.getMajor()
        );



        /*
         * CLO
         */


        if(data.getClos()!=null){

            request.setClos(

                data.getClos()
                .stream()
                .map(clo ->
                    new CreateSyllabusRequest.CloDTO(
                            clo.getCode(),
                            clo.getDescription(),
                            clo.getDescriptionVn(),
                            clo.getCompetencyLevel(),
                            clo.getBloomLevel(),
                            clo.getOrderIndex()
                    )
                )
                .collect(Collectors.toList())

            );
        }



        /*
         * Topic
         */

        if(data.getTopics()!=null){

            request.setTopics(

    data.getTopics()
    .stream()
    .map(topic ->

        new CreateSyllabusRequest.TopicDTO(

            topic.getWeekNumber(),

            topic.getOrderInWeek(),

            topic.getName(),

            topic.getNameVn(),

            topic.getTeachingHours(),

            topic.getLabHours(),

            topic.getSelfStudyHours(),

            topic.getTopicType(),

            topic.getTeachingMethod(),

            topic.getLearningActivity(),

            null,

            topic.getResources(),

            topic.getNotes()

        )

    )
    .collect(Collectors.toList())

);
        }



        /*
         * Assessment
         */


        if(data.getAssessments()!=null){

            request.setAssessments(

                data.getAssessments()
                .stream()
                .map(a ->
                    new CreateSyllabusRequest.AssessmentDTO(
                            a.getName(),
                            a.getNameVn(),
                            a.getAssessmentType(),
                            a.getWeightPercent(),
                            a.getMinScore(),
                            a.getMaxScore(),
                            a.getOrderIndex()
                    )
                )
                .collect(Collectors.toList())

            );

        }


        return request;

    }

}