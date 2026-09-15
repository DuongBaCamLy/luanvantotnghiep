package com.scse.curriculum.syllabus.comparison.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;

class SyllabusSemanticNarrativeScopeTest {

    @Test
    void aiCandidatesContainNarrativeFieldsOnly() {
        CloRepository cloRepository =
                mock(CloRepository.class);

        TopicRepository topicRepository =
                mock(TopicRepository.class);

        SemanticComparisonService semanticComparisonService =
                mock(SemanticComparisonService.class);

        BasicSemanticComparisonService basicComparisonService =
                new BasicSemanticComparisonService();

        OpenAiSemanticProperties properties =
                new OpenAiSemanticProperties();

        SyllabusSemanticComparisonService service =
                new SyllabusSemanticComparisonService(
                        cloRepository,
                        topicRepository,
                        semanticComparisonService,
                        basicComparisonService,
                        properties);

        Course course =
                Course.builder()
                        .id(10)
                        .courseCode("IT116IU")
                        .name("C/C++ Programming")
                        .creditTheory(3)
                        .creditLab(1)
                        .build();

        Syllabus oldSyllabus =
                Syllabus.builder()
                        .id(100)
                        .course(course)
                        .versionNumber(1)
                        .versionLabel("v1.0")
                        .academicYear("CS2021")
                        .program("CS")
                        .semester("Semester 2")
                        .courseDesignation("Core")
                        .courseTypes("COMPULSORY")
                        .language("English")
                        .workloadTotal("150")
                        .workloadContact("45")
                        .workloadPrivate("105")
                        .prerequisites("IT101IU")
                        .objectives(
                                "Understand programming fundamentals.")
                        .teachingMethods(
                                "Lecture and laboratory practice.")
                        .examForms(
                                "Written examination")
                        .examRequirements(
                                "Students must pass the final examination.")
                        .build();

        Syllabus newSyllabus =
                Syllabus.builder()
                        .id(200)
                        .course(course)
                        .versionNumber(1)
                        .versionLabel("v1.0")
                        .academicYear("CS2026")
                        .program("CS")
                        .semester("Semester 2")
                        .courseDesignation("Core")
                        .courseTypes("COMPULSORY")
                        .language("English")
                        .workloadTotal("160")
                        .workloadContact("48")
                        .workloadPrivate("112")
                        .prerequisites("IT101IU")
                        .objectives(
                                "Apply programming fundamentals to solve practical problems.")
                        .teachingMethods(
                                "Lecture, laboratory practice and project work.")
                        .examForms(
                                "Written examination")
                        .examRequirements(
                                "Students must pass the final examination and project.")
                        .build();

        Clo oldClo =
                Clo.builder()
                        .id(1)
                        .syllabus(oldSyllabus)
                        .code("CLO1")
                        .description(
                                "Understand basic programming concepts.")
                        .orderIndex(1)
                        .build();

        Clo newClo =
                Clo.builder()
                        .id(2)
                        .syllabus(newSyllabus)
                        .code("CLO1")
                        .description(
                                "Apply programming concepts to practical problems.")
                        .orderIndex(1)
                        .build();

        Topic oldTopic =
                Topic.builder()
                        .id(11)
                        .syllabus(oldSyllabus)
                        .weekNumber(1)
                        .orderInWeek(1)
                        .name(
                                "Programming fundamentals")
                        .teachingMethod(
                                "Lecture")
                        .learningActivity(
                                "Guided exercises")
                        .build();

        Topic newTopic =
                Topic.builder()
                        .id(12)
                        .syllabus(newSyllabus)
                        .weekNumber(1)
                        .orderInWeek(1)
                        .name(
                                "Programming fundamentals with practice")
                        .teachingMethod(
                                "Lecture and live coding")
                        .learningActivity(
                                "Guided exercises and coding practice")
                        .build();

        when(cloRepository.findBySyllabusId(100))
                .thenReturn(
                        List.of(oldClo));

        when(cloRepository.findBySyllabusId(200))
                .thenReturn(
                        List.of(newClo));

        when(topicRepository
                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(100))
                .thenReturn(
                        List.of(oldTopic));

        when(topicRepository
                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(200))
                .thenReturn(
                        List.of(newTopic));

        List<SemanticComparisonRequest> requests =
                service.collectCandidates(
                        oldSyllabus,
                        newSyllabus);

        Set<String> allowedFields =
                Set.of(
                        "objectives",
                        "teachingMethods",
                        "examRequirements",
                        "description",
                        "name",
                        "teachingMethod",
                        "learningActivity");

        assertThat(requests)
                .isNotEmpty();

        assertThat(requests)
                .allSatisfy(request ->
                        assertThat(
                                allowedFields)
                                .contains(
                                        request.getFieldName()));

        assertThat(requests)
                .extracting(
                        SemanticComparisonRequest::getFieldName)
                .doesNotContain(
                        "courseDesignation",
                        "courseTypes",
                        "semester",
                        "language",
                        "workloadTotal",
                        "workloadContact",
                        "workloadPrivate",
                        "prerequisites",
                        "examForms",
                        "weightPercent",
                        "minScore",
                        "maxScore",
                        "orderIndex",
                        "weekNumber",
                        "teachingHours",
                        "labHours",
                        "selfStudyHours");
    }
}
