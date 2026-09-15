package com.scse.curriculum.syllabus.comparison.service;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.course.entity.Course;

import com.scse.curriculum.syllabus.comparison.config.OpenAiSemanticProperties;
import com.scse.curriculum.syllabus.comparison.dto.SemanticComparisonRequest;
import com.scse.curriculum.syllabus.comparison.dto.SemanticDiffResult;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;
import com.scse.curriculum.syllabus.comparison.exception.SemanticAiException;
import com.scse.curriculum.syllabus.comparison.model.SemanticAnalysisStatus;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeNature;
import com.scse.curriculum.syllabus.comparison.model.SemanticChangeType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSectionType;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;

import com.scse.curriculum.syllabus.entity.Syllabus;

import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class SyllabusSemanticComparisonServiceTest {

    private CloRepository cloRepository;

    private TopicRepository topicRepository;

    private SemanticComparisonService semanticComparisonService;

    private BasicSemanticComparisonService basicComparisonService;

    private OpenAiSemanticProperties properties;

    private SyllabusSemanticComparisonService service;

    private Course course;

    private Syllabus oldSyllabus;

    private Syllabus newSyllabus;

    @BeforeEach
    void setUp() {

        cloRepository =
                mock(CloRepository.class);

        topicRepository =
                mock(TopicRepository.class);

        semanticComparisonService =
                mock(SemanticComparisonService.class);

        basicComparisonService =
                new BasicSemanticComparisonService();

        properties =
                new OpenAiSemanticProperties();

        /*
         * Default:
         * AI enabled/configured.
         */
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setMaxBatchSize(20);

        service =
                new SyllabusSemanticComparisonService(
                        cloRepository,
                        topicRepository,
                        semanticComparisonService,
                        basicComparisonService,
                        properties
                );

        course =
                Course.builder()
                        .id(100)
                        .courseCode("IT116IU")
                        .name("C/C++ Programming")
                        .build();

        oldSyllabus =
                Syllabus.builder()
                        .id(1000)
                        .course(course)
                        .versionNumber(1)
                        .versionLabel("v1.0")
                        .academicYear("2021")
                        .objectives(
                                "Understand fundamental programming concepts."
                        )
                        .teachingMethods(
                                "Lecture and laboratory practice."
                        )
                        .examRequirements(
                                "Students must pass the final examination."
                        )
                        .build();

        newSyllabus =
                Syllabus.builder()
                        .id(2000)
                        .course(course)
                        .versionNumber(2)
                        .versionLabel("v2.0")
                        .academicYear("2026")
                        .objectives(
                                "Apply programming concepts to design and implement software."
                        )
                        .teachingMethods(
                                "Lecture, laboratory practice and project-based learning."
                        )
                        .examRequirements(
                                "Students must pass the final examination."
                        )
                        .build();

        when(
                cloRepository.findBySyllabusId(1000)
        ).thenReturn(
                List.of()
        );

        when(
                cloRepository.findBySyllabusId(2000)
        ).thenReturn(
                List.of()
        );

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                1000
                        )
        ).thenReturn(
                List.of()
        );

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                2000
                        )
        ).thenReturn(
                List.of()
        );
    }

    @Test
    void shouldCollectGeneralAcademicFields() {

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(this::sameMeaningResult)
                    .toList();
        });

        service.compare(
                oldSyllabus,
                newSyllabus
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                semanticComparisonService
        ).compare(
                captor.capture()
        );

        List<SemanticComparisonRequest> requests =
                captor.getValue();

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "general.objectives"
                                        )
                        )
        );

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "general.teachingMethods"
                                        )
                        )
        );

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "general.examRequirements"
                                        )
                        )
        );
    }

    @Test
    void sameCloCodeShouldBePairedForSemanticComparison() {

        Clo oldClo =
                Clo.builder()
                        .id(1)
                        .syllabus(oldSyllabus)
                        .code("CLO1")
                        .description(
                                "Understand basic programming concepts."
                        )
                        .orderIndex(1)
                        .build();

        Clo newClo =
                Clo.builder()
                        .id(2)
                        .syllabus(newSyllabus)
                        .code("CLO1")
                        .description(
                                "Apply programming concepts to solve practical problems."
                        )
                        .orderIndex(1)
                        .build();

        when(
                cloRepository.findBySyllabusId(1000)
        ).thenReturn(
                List.of(oldClo)
        );

        when(
                cloRepository.findBySyllabusId(2000)
        ).thenReturn(
                List.of(newClo)
        );

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(this::sameMeaningResult)
                    .toList();
        });

        service.compare(
                oldSyllabus,
                newSyllabus
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                semanticComparisonService
        ).compare(
                captor.capture()
        );

        List<SemanticComparisonRequest> requests =
                captor.getValue();

        SemanticComparisonRequest cloRequest =
                requests.stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "clo.clo1.description"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                oldClo.getDescription(),
                cloRequest.getOldText()
        );

        assertEquals(
                newClo.getDescription(),
                cloRequest.getNewText()
        );

        assertEquals(
                SemanticSectionType.CLO,
                cloRequest.getSectionType()
        );
    }

    @Test
    void addedCloShouldBeRepresentedAsAddedSemanticContent() {

        Clo newClo =
                Clo.builder()
                        .id(2)
                        .syllabus(newSyllabus)
                        .code("CLO2")
                        .description(
                                "Design modular C++ applications."
                        )
                        .orderIndex(2)
                        .build();

        when(
                cloRepository.findBySyllabusId(2000)
        ).thenReturn(
                List.of(newClo)
        );

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(request -> {

                        if (request.getItemId().equals(
                                "clo.clo2.description"
                        )) {

                            return SemanticDiffResult.builder()
                                    .itemId(request.getItemId())
                                    .sectionType(request.getSectionType())
                                    .fieldName(request.getFieldName())
                                    .oldText(request.getOldText())
                                    .newText(request.getNewText())
                                    .classification(
                                            SemanticChangeType.MEANINGFUL_CHANGE
                                    )
                                    .changeNature(
                                            SemanticChangeNature.ADDED
                                    )
                                    .significance(
                                            SemanticSignificance.MEDIUM
                                    )
                                    .requiresAi(false)
                                    .summary(
                                            "A new CLO was added."
                                    )
                                    .build();
                        }

                        return sameMeaningResult(
                                request
                        );
                    })
                    .toList();
        });

        SemanticSyllabusDiffResponse response =
                service.compare(
                        oldSyllabus,
                        newSyllabus
                );

        SemanticDiffResult added =
                response.getItems()
                        .stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "clo.clo2.description"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertNull(
                added.getOldText()
        );

        assertEquals(
                "Design modular C++ applications.",
                added.getNewText()
        );

        assertEquals(
                SemanticChangeNature.ADDED,
                added.getChangeNature()
        );
    }

    @Test
    void topicWithSameWeekAndOrderShouldBePaired() {

        Topic oldTopic =
                Topic.builder()
                        .id(10)
                        .syllabus(oldSyllabus)
                        .weekNumber(3)
                        .orderInWeek(1)
                        .name(
                                "Object-Oriented Programming"
                        )
                        .teachingMethod(
                                "Lecture"
                        )
                        .learningActivity(
                                "Coding exercises"
                        )
                        .build();

        Topic newTopic =
                Topic.builder()
                        .id(20)
                        .syllabus(newSyllabus)
                        .weekNumber(3)
                        .orderInWeek(1)
                        .name(
                                "Object-Oriented Programming with C++"
                        )
                        .teachingMethod(
                                "Lecture and live coding"
                        )
                        .learningActivity(
                                "Coding exercises and mini project"
                        )
                        .build();

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                1000
                        )
        ).thenReturn(
                List.of(oldTopic)
        );

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                2000
                        )
        ).thenReturn(
                List.of(newTopic)
        );

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(this::sameMeaningResult)
                    .toList();
        });

        service.compare(
                oldSyllabus,
                newSyllabus
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                semanticComparisonService
        ).compare(
                captor.capture()
        );

        List<SemanticComparisonRequest> requests =
                captor.getValue();

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "topic.week-3-order-1.name"
                                        )
                        )
        );

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "topic.week-3-order-1.teachingMethod"
                                        )
                        )
        );

        assertTrue(
                requests.stream()
                        .anyMatch(
                                item ->
                                        item.getItemId().equals(
                                                "topic.week-3-order-1.learningActivity"
                                        )
                        )
        );
    }

    @Test
    void movedTopicShouldFallbackToNameMatching() {

        /*
         * Same academic topic, but moved to another week.
         *
         * Primary identity:
         * week/order -> different
         *
         * Fallback identity:
         * topic name -> same
         */
        Topic oldTopic =
                Topic.builder()
                        .id(10)
                        .syllabus(oldSyllabus)
                        .weekNumber(3)
                        .orderInWeek(1)
                        .name(
                                "Pointers and Memory Management"
                        )
                        .teachingMethod(
                                "Lecture"
                        )
                        .learningActivity(
                                "Exercises"
                        )
                        .build();

        Topic newTopic =
                Topic.builder()
                        .id(20)
                        .syllabus(newSyllabus)
                        .weekNumber(5)
                        .orderInWeek(1)
                        .name(
                                "Pointers and Memory Management"
                        )
                        .teachingMethod(
                                "Lecture and laboratory"
                        )
                        .learningActivity(
                                "Laboratory exercises"
                        )
                        .build();

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                1000
                        )
        ).thenReturn(
                List.of(oldTopic)
        );

        when(
                topicRepository
                        .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                                2000
                        )
        ).thenReturn(
                List.of(newTopic)
        );

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(this::sameMeaningResult)
                    .toList();
        });

        service.compare(
                oldSyllabus,
                newSyllabus
        );

        ArgumentCaptor<List<SemanticComparisonRequest>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(
                semanticComparisonService
        ).compare(
                captor.capture()
        );

        List<SemanticComparisonRequest> requests =
                captor.getValue();

        /*
         * The pair uses the NEW topic position as display identity.
         */
        SemanticComparisonRequest teachingMethod =
                requests.stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "topic.week-5-order-1.teachingMethod"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "Lecture",
                teachingMethod.getOldText()
        );

        assertEquals(
                "Lecture and laboratory",
                teachingMethod.getNewText()
        );
    }

    @Test
    void differentCourseShouldBeRejected() {

        Course otherCourse =
                Course.builder()
                        .id(999)
                        .courseCode(
                                "IT013IU"
                        )
                        .name(
                                "Algorithms and Data Structures"
                        )
                        .build();

        newSyllabus.setCourse(
                otherCourse
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.compare(
                                oldSyllabus,
                                newSyllabus
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "same course"
                )
        );

        verifyNoInteractions(
                semanticComparisonService
        );
    }

    @Test
    void disabledAiShouldReturnDeterministicResultsAndUnresolvedItems() {

        properties.setEnabled(false);
        properties.setApiKey(null);

        oldSyllabus.setObjectives(
                "Understand basic database concepts."
        );

        newSyllabus.setObjectives(
                "Design and implement relational databases."
        );

        SemanticSyllabusDiffResponse response =
                service.compare(
                        oldSyllabus,
                        newSyllabus
                );

        assertEquals(
                SemanticAnalysisStatus.DISABLED,
                response.getStatus()
        );

        assertTrue(
                response.isHasUnresolvedItems()
        );

        SemanticDiffResult objective =
                response.getItems()
                        .stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "general.objectives"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertTrue(
                objective.isRequiresAi()
        );

        assertNull(
                objective.getClassification()
        );

        verifyNoInteractions(
                semanticComparisonService
        );
    }

    @Test
    void aiFailureShouldReturnPartialInsteadOfNoChange() {

        when(
                semanticComparisonService.compare(anyList())
        ).thenThrow(
                new SemanticAiException(
                        "OpenAI timeout"
                )
        );

        SemanticSyllabusDiffResponse response =
                service.compare(
                        oldSyllabus,
                        newSyllabus
                );

        assertEquals(
                SemanticAnalysisStatus.PARTIAL,
                response.getStatus()
        );

        assertTrue(
                response.isHasUnresolvedItems()
        );

        SemanticDiffResult objective =
                response.getItems()
                        .stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "general.objectives"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        /*
         * Critical:
         *
         * timeout must NOT become:
         * NO_MEANINGFUL_CHANGE.
         */
        assertNull(
                objective.getClassification()
        );

        assertTrue(
                objective.isRequiresAi()
        );
    }

    @Test
    void punctuationOnlyDifferenceShouldRemainDeterministic() {

        oldSyllabus.setObjectives(
                "Understand programming concepts."
        );

        newSyllabus.setObjectives(
                "Understand programming concepts"
        );

        oldSyllabus.setTeachingMethods(null);
        newSyllabus.setTeachingMethods(null);

        oldSyllabus.setExamRequirements(null);
        newSyllabus.setExamRequirements(null);

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            /*
             * Real AiSemanticComparisonService would resolve this
             * without OpenAI.
             *
             * For this aggregator test we return deterministic
             * result explicitly.
             */
            return requests.stream()
                    .map(request ->
                            basicComparisonService
                                    .resolveWithoutAi(request)
                                    .orElseThrow()
                    )
                    .toList();
        });

        SemanticSyllabusDiffResponse response =
                service.compare(
                        oldSyllabus,
                        newSyllabus
                );

        SemanticDiffResult objective =
                response.getItems()
                        .stream()
                        .filter(
                                item ->
                                        item.getItemId().equals(
                                                "general.objectives"
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                SemanticChangeType.NO_MEANINGFUL_CHANGE,
                objective.getClassification()
        );

        assertEquals(
                SemanticChangeNature.SAME_MEANING,
                objective.getChangeNature()
        );

        assertFalse(
                objective.isRequiresAi()
        );
    }

    @Test
    void overallSignificanceShouldUseHighestResultSignificance() {

        when(
                semanticComparisonService.compare(anyList())
        ).thenAnswer(invocation -> {

            List<SemanticComparisonRequest> requests =
                    invocation.getArgument(0);

            return requests.stream()
                    .map(request -> {

                        SemanticSignificance significance =
                                request.getItemId()
                                        .equals(
                                                "general.objectives"
                                        )
                                        ? SemanticSignificance.HIGH
                                        : SemanticSignificance.LOW;

                        SemanticChangeType type =
                                request.getItemId()
                                        .equals(
                                                "general.objectives"
                                        )
                                        ? SemanticChangeType.MEANINGFUL_CHANGE
                                        : SemanticChangeType.NO_MEANINGFUL_CHANGE;

                        return SemanticDiffResult.builder()
                                .itemId(
                                        request.getItemId()
                                )
                                .sectionType(
                                        request.getSectionType()
                                )
                                .fieldName(
                                        request.getFieldName()
                                )
                                .oldText(
                                        request.getOldText()
                                )
                                .newText(
                                        request.getNewText()
                                )
                                .classification(
                                        type
                                )
                                .changeNature(
                                        type
                                                == SemanticChangeType.MEANINGFUL_CHANGE
                                                ? SemanticChangeNature.MODIFIED
                                                : SemanticChangeNature.SAME_MEANING
                                )
                                .significance(
                                        significance
                                )
                                .requiresAi(false)
                                .summary(
                                        "Test result"
                                )
                                .build();
                    })
                    .toList();
        });

        SemanticSyllabusDiffResponse response =
                service.compare(
                        oldSyllabus,
                        newSyllabus
                );

        assertEquals(
                SemanticSignificance.HIGH,
                response.getOverallSignificance()
        );

        assertTrue(
                response.isHasMeaningfulChanges()
        );
    }

    private SemanticDiffResult sameMeaningResult(
            SemanticComparisonRequest request) {

        return SemanticDiffResult.builder()
                .itemId(
                        request.getItemId()
                )
                .sectionType(
                        request.getSectionType()
                )
                .fieldName(
                        request.getFieldName()
                )
                .oldText(
                        request.getOldText()
                )
                .newText(
                        request.getNewText()
                )
                .classification(
                        SemanticChangeType.NO_MEANINGFUL_CHANGE
                )
                .changeNature(
                        SemanticChangeNature.SAME_MEANING
                )
                .significance(
                        SemanticSignificance.LOW
                )
                .requiresAi(false)
                .oldMeaning(
                        "Old meaning"
                )
                .newMeaning(
                        "New meaning"
                )
                .summary(
                        "Academic meaning remains equivalent."
                )
                .build();
    }
}