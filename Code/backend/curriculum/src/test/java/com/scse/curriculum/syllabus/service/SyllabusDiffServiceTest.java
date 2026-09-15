package com.scse.curriculum.syllabus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.book.entity.BookType;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.SyllabusBookId;
import com.scse.curriculum.syllabusbook.entity.UsageType;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.entity.TopicType;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TeachingLevel;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;

@ExtendWith(MockitoExtension.class)
class SyllabusDiffServiceTest {

    @Mock
    private CloRepository cloRepository;

    @Mock
    private CloPloMappingRepository cloPloMappingRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicCloRepository topicCloRepository;

    @Mock
    private AssessmentComponentRepository assessmentComponentRepository;

    @Mock
    private AssessmentCloRepository assessmentCloRepository;

    @Mock
    private SyllabusBookRepository syllabusBookRepository;

    @InjectMocks
    private SyllabusDiffService service;

    private Syllabus oldSyllabus;
    private Syllabus newSyllabus;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .id(10)
                .courseCode("IT064IU")
                .name("Introduction to Computing")
                .nameVn("Giới thiệu về máy tính")
                .creditTheory(3)
                .creditLab(1)
                .build();

        oldSyllabus = Syllabus.builder()
                .id(100)
                .course(course)
                .versionLabel("v1.0")
                .academicYear("CS2021")
                .semester("Semester 1")
                .language("English")
                .objectives("Old objective")
                .notes("Old note")
                .build();

        newSyllabus = Syllabus.builder()
                .id(101)
                .course(course)
                .versionLabel("v2.0")
                .academicYear("CS2022")
                .semester("Semester 2")
                .language("English")
                .objectives("New objective")
                .notes("New note")
                .build();
    }

    @Test
    void compareIncludesReadingsAndAllMappings() {
        Clo oldClo = Clo.builder()
                .id(1)
                .syllabus(oldSyllabus)
                .code("CLO1")
                .description("Explain computing concepts")
                .descriptionVn("Giải thích khái niệm máy tính")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.UNDERSTAND)
                .orderIndex(1)
                .build();

        Clo newClo = Clo.builder()
                .id(11)
                .syllabus(newSyllabus)
                .code("CLO1")
                .description("Analyze computing concepts")
                .descriptionVn("Phân tích khái niệm máy tính")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.ANALYZE)
                .orderIndex(1)
                .build();

        Plo oldPlo = Plo.builder()
                .id(7)
                .code("PLO2")
                .description("Problem solving")
                .build();

        Plo newPlo = Plo.builder()
                .id(7)
                .code("PLO2")
                .description("Problem solving")
                .build();

        CloPloMapping oldCloPlo = CloPloMapping.builder()
                .id(20)
                .clo(oldClo)
                .plo(oldPlo)
                .level(ContributionLevel.I)
                .contributionWeight(0.25f)
                .notes("Introduced")
                .build();

        CloPloMapping newCloPlo = CloPloMapping.builder()
                .id(21)
                .clo(newClo)
                .plo(newPlo)
                .level(ContributionLevel.D)
                .contributionWeight(0.75f)
                .notes("Developed")
                .build();

        Topic oldTopic = Topic.builder()
                .id(2)
                .syllabus(oldSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Computer basics")
                .nameVn("Cơ bản máy tính")
                .teachingHours(3)
                .labHours(1)
                .selfStudyHours(4)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Lecture")
                .learningActivity("Discussion")
                .build();

        Topic newTopic = Topic.builder()
                .id(12)
                .syllabus(newSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Computer basics")
                .nameVn("Nền tảng máy tính")
                .teachingHours(2)
                .labHours(2)
                .selfStudyHours(4)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Flipped classroom")
                .learningActivity("Discussion")
                .build();

        TopicClo oldTopicClo = TopicClo.builder()
                .id(new TopicCloId(2, 1))
                .topic(oldTopic)
                .clo(oldClo)
                .teachingLevel(TeachingLevel.I)
                .build();

        TopicClo newTopicClo = TopicClo.builder()
                .id(new TopicCloId(12, 11))
                .topic(newTopic)
                .clo(newClo)
                .teachingLevel(TeachingLevel.D)
                .build();

        AssessmentComponent oldAssessment = AssessmentComponent.builder()
                .id(3)
                .syllabus(oldSyllabus)
                .name("Final exam")
                .nameVn("Thi cuối kỳ")
                .assessmentType("FINAL_EXAM")
                .weightPercent(50f)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(1)
                .build();

        AssessmentComponent newAssessment = AssessmentComponent.builder()
                .id(13)
                .syllabus(newSyllabus)
                .name("Final exam")
                .nameVn("Thi cuối kỳ")
                .assessmentType("FINAL_EXAM")
                .weightPercent(60f)
                .minScore(10f)
                .maxScore(100f)
                .orderIndex(1)
                .build();

        AssessmentClo oldAssessmentClo = AssessmentClo.builder()
                .id(new AssessmentCloId(3, 1))
                .assessmentComponent(oldAssessment)
                .clo(oldClo)
                .contributionPercent(40f)
                .build();

        AssessmentClo newAssessmentClo = AssessmentClo.builder()
                .id(new AssessmentCloId(13, 11))
                .assessmentComponent(newAssessment)
                .clo(newClo)
                .contributionPercent(60f)
                .build();

        Book oldBook = Book.builder()
                .id(8)
                .title("Computer Science: An Overview")
                .author("J. Glenn Brookshear")
                .publisher("Pearson")
                .year(2019)
                .edition("13")
                .isbn("9780134875460")
                .bookType(BookType.TEXTBOOK)
                .build();

        Book newBook = Book.builder()
                .id(8)
                .title("Computer Science: An Overview")
                .author("J. Glenn Brookshear, Dennis Brylow")
                .publisher("Pearson")
                .year(2019)
                .edition("13")
                .isbn("9780134875460")
                .bookType(BookType.TEXTBOOK)
                .build();

        SyllabusBook oldReading = SyllabusBook.builder()
                .id(new SyllabusBookId(100, 8))
                .syllabus(oldSyllabus)
                .book(oldBook)
                .usageType(UsageType.REQUIRED)
                .orderIndex(1)
                .build();

        SyllabusBook newReading = SyllabusBook.builder()
                .id(new SyllabusBookId(101, 8))
                .syllabus(newSyllabus)
                .book(newBook)
                .usageType(UsageType.RECOMMENDED)
                .orderIndex(2)
                .build();

        when(cloRepository.findBySyllabusId(100)).thenReturn(List.of(oldClo));
        when(cloRepository.findBySyllabusId(101)).thenReturn(List.of(newClo));

        when(cloPloMappingRepository.findByClo_Syllabus_Id(100))
                .thenReturn(List.of(oldCloPlo));
        when(cloPloMappingRepository.findByClo_Syllabus_Id(101))
                .thenReturn(List.of(newCloPlo));

        when(topicRepository.findBySyllabusId(100)).thenReturn(List.of(oldTopic));
        when(topicRepository.findBySyllabusId(101)).thenReturn(List.of(newTopic));

        when(topicCloRepository.findByTopic_Syllabus_Id(100))
                .thenReturn(List.of(oldTopicClo));
        when(topicCloRepository.findByTopic_Syllabus_Id(101))
                .thenReturn(List.of(newTopicClo));

        when(assessmentComponentRepository.findBySyllabusId(100))
                .thenReturn(List.of(oldAssessment));
        when(assessmentComponentRepository.findBySyllabusId(101))
                .thenReturn(List.of(newAssessment));

        when(assessmentCloRepository
                .findByAssessmentComponent_Syllabus_Id(100))
                .thenReturn(List.of(oldAssessmentClo));
        when(assessmentCloRepository
                .findByAssessmentComponent_Syllabus_Id(101))
                .thenReturn(List.of(newAssessmentClo));

        when(syllabusBookRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(oldReading));
        when(syllabusBookRepository.findBySyllabus_Id(101))
                .thenReturn(List.of(newReading));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges()).isTrue();

        /*
         * Task 2 moves Course Objectives out of General Information and into
         * the dedicated Requirements section. Raw Syllabus.notes is never
         * exposed as one field.
         */
        assertThat(result.getGeneralInfoDiff())
                .containsKeys("academicYear", "semester")
                .doesNotContainKeys("objectives", "notes");

        assertThat(result.getRequirementsDiff())
                .containsKey("objectives");

        /*
         * Old note / New note are malformed legacy plain-text notes, so the
         * canonical parser exposes them only as Revision -> Internal Notes.
         */
        assertThat(result.getRevisionInfoDiff())
                .containsKey("internalNotes");

        assertThat(result.getCloDiff().getModified()).hasSize(1);
        assertThat(result.getCloDiff().getModified().get(0).getChanges())
                .containsKeys(
                        "description",
                        "descriptionVn",
                        "bloomLevel");

        assertThat(result.getCloPloDiff().getModified()).hasSize(1);
        assertThat(result.getCloPloDiff().getModified().get(0).getChanges())
                .containsKeys(
                        "level",
                        "contributionWeight",
                        "notes");

        assertThat(result.getTopicDiff().getModified()).hasSize(1);
        assertThat(result.getTopicDiff().getModified().get(0).getChanges())
                .containsKeys(
                        "nameVn",
                        "teachingHours",
                        "labHours",
                        "teachingMethod");

        assertThat(result.getTopicCloDiff().getModified()).hasSize(1);
        assertThat(result.getTopicCloDiff().getModified().get(0).getChanges())
                .containsKey("teachingLevel");

        assertThat(result.getAssessmentDiff().getModified()).hasSize(1);
        SyllabusDiffResponse.AssessmentDiff assessmentDiff =
                result.getAssessmentDiff().getModified().get(0);

        assertThat(assessmentDiff.getAssessmentType())
                .isEqualTo("FINAL_EXAM");
        assertThat(assessmentDiff.getWeightPercent())
                .isEqualTo(60.0d);
        assertThat(assessmentDiff.getMinScore())
                .isEqualTo(10.0d);
        assertThat(assessmentDiff.getMaxScore())
                .isEqualTo(100.0d);
        assertThat(assessmentDiff.getChanges())
                .containsKeys("weightPercent", "minScore");

        assertThat(result.getAssessmentCloDiff().getModified()).hasSize(1);
        assertThat(result.getAssessmentCloDiff().getModified().get(0).getChanges())
                .containsKey("contributionPercent");

        assertThat(result.getReadingsDiff().getModified()).hasSize(1);
        assertThat(result.getReadingsDiff().getModified().get(0).getChanges())
                .containsKeys("author", "usageType", "orderIndex");
    }

    @Test
    void compareReturnsNoChangesForEquivalentVersions() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges()).isFalse();

        assertThat(result.getGeneralInfoDiff()).isEmpty();
        assertThat(result.getWorkloadCreditDiff()).isEmpty();
        assertThat(result.getRequirementsDiff()).isEmpty();
        assertThat(result.getContentDiff()).isEmpty();
        assertThat(result.getAssessmentInfoDiff()).isEmpty();
        assertThat(result.getExaminationDiff()).isEmpty();
        assertThat(result.getRevisionInfoDiff()).isEmpty();

        assertThat(result.getCloDiff().getAdded()).isEmpty();
        assertThat(result.getCloDiff().getRemoved()).isEmpty();
        assertThat(result.getCloDiff().getModified()).isEmpty();

        assertThat(result.getPlannedActivityDiff().getAdded()).isEmpty();
        assertThat(result.getPlannedActivityDiff().getRemoved()).isEmpty();
        assertThat(result.getPlannedActivityDiff().getModified()).isEmpty();

        assertThat(result.getReadingsDiff().getModified()).isEmpty();
    }

    /*
     * ============================================================
     * TASK 1 REGRESSION TESTS
     * ============================================================
     */

    @Test
    void compareAssessmentsDoesNotMatchDifferentComponentsByOrder() {
        stubEmptyRepositories();

        AssessmentComponent oldQuiz = assessment(
                1,
                oldSyllabus,
                "Quiz / Assignment",
                "ASSIGNMENT",
                10f,
                1);

        AssessmentComponent oldLabs = assessment(
                2,
                oldSyllabus,
                "Labs",
                "LAB_REPORT",
                20f,
                2);

        AssessmentComponent oldMidterm = assessment(
                3,
                oldSyllabus,
                "Midterm examination",
                "MIDTERM_EXAM",
                30f,
                3);

        AssessmentComponent oldFinal = assessment(
                4,
                oldSyllabus,
                "Final examination",
                "FINAL_EXAM",
                40f,
                4);

        AssessmentComponent newMidterm = assessment(
                13,
                newSyllabus,
                "Midterm examination",
                "MIDTERM_EXAM",
                30f,
                1);

        AssessmentComponent newFinal = assessment(
                14,
                newSyllabus,
                "Final examination",
                "FINAL_EXAM",
                40f,
                2);

        when(assessmentComponentRepository.findBySyllabusId(100))
                .thenReturn(List.of(
                        oldQuiz,
                        oldLabs,
                        oldMidterm,
                        oldFinal));

        when(assessmentComponentRepository.findBySyllabusId(101))
                .thenReturn(List.of(
                        newMidterm,
                        newFinal));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getAssessmentDiff().getAdded())
                .isEmpty();

        assertThat(result.getAssessmentDiff().getRemoved())
                .extracting(SyllabusDiffResponse.AssessmentDiff::getName)
                .containsExactlyInAnyOrder(
                        "Quiz / Assignment",
                        "Labs");

        assertThat(result.getAssessmentDiff().getModified())
                .hasSize(2)
                .extracting(SyllabusDiffResponse.AssessmentDiff::getName)
                .containsExactlyInAnyOrder(
                        "Midterm examination",
                        "Final examination");

        assertThat(result.getAssessmentDiff().getModified())
                .allSatisfy(item -> {
                    assertThat(item.getChanges())
                            .containsKey("orderIndex")
                            .doesNotContainKeys(
                                    "name",
                                    "assessmentType");
                });
    }

    @Test
    void compareAssessmentsKeepsIdentityWhenOrderChanges() {
        stubEmptyRepositories();

        AssessmentComponent oldQuiz = assessment(
                1,
                oldSyllabus,
                "Quiz",
                "QUIZ",
                20f,
                1);

        AssessmentComponent oldMidterm = assessment(
                2,
                oldSyllabus,
                "Midterm examination",
                "MIDTERM_EXAM",
                30f,
                2);

        AssessmentComponent newMidterm = assessment(
                12,
                newSyllabus,
                "Midterm examination",
                "MIDTERM_EXAM",
                30f,
                1);

        AssessmentComponent newQuiz = assessment(
                11,
                newSyllabus,
                "Quiz",
                "QUIZ",
                20f,
                2);

        when(assessmentComponentRepository.findBySyllabusId(100))
                .thenReturn(List.of(
                        oldQuiz,
                        oldMidterm));

        when(assessmentComponentRepository.findBySyllabusId(101))
                .thenReturn(List.of(
                        newMidterm,
                        newQuiz));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getAssessmentDiff().getAdded())
                .isEmpty();

        assertThat(result.getAssessmentDiff().getRemoved())
                .isEmpty();

        assertThat(result.getAssessmentDiff().getModified())
                .hasSize(2)
                .extracting(SyllabusDiffResponse.AssessmentDiff::getName)
                .containsExactlyInAnyOrder(
                        "Quiz",
                        "Midterm examination");

        assertThat(result.getAssessmentDiff().getModified())
                .allSatisfy(item ->
                        assertThat(item.getChanges())
                                .containsOnlyKeys("orderIndex"));
    }

    @Test
    void compareTopicMovedToAnotherWeekKeepsSameTopicIdentity() {
        stubEmptyRepositories();

        Topic oldTopic = topic(
                1,
                oldSyllabus,
                "Pointers",
                3,
                1);

        Topic newTopic = topic(
                11,
                newSyllabus,
                "Pointers",
                4,
                2);

        when(topicRepository.findBySyllabusId(100))
                .thenReturn(List.of(oldTopic));

        when(topicRepository.findBySyllabusId(101))
                .thenReturn(List.of(newTopic));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getTopicDiff().getAdded())
                .isEmpty();

        assertThat(result.getTopicDiff().getRemoved())
                .isEmpty();

        assertThat(result.getTopicDiff().getModified())
                .hasSize(1);

        SyllabusDiffResponse.TopicDiff modified =
                result.getTopicDiff()
                        .getModified()
                        .get(0);

        assertThat(modified.getName())
                .isEqualTo("Pointers");

        assertThat(modified.getChanges())
                .containsKeys(
                        "weekNumber",
                        "orderInWeek")
                .doesNotContainKey("name");
    }

    @Test
    void compareUnrelatedNamedTopicsAtSamePositionDoesNotPairThem() {
        stubEmptyRepositories();

        Topic oldTopic = topic(
                1,
                oldSyllabus,
                "Arrays",
                3,
                1);

        Topic newTopic = topic(
                11,
                newSyllabus,
                "Pointers",
                3,
                1);

        when(topicRepository.findBySyllabusId(100))
                .thenReturn(List.of(oldTopic));

        when(topicRepository.findBySyllabusId(101))
                .thenReturn(List.of(newTopic));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getTopicDiff().getModified())
                .isEmpty();

        assertThat(result.getTopicDiff().getRemoved())
                .hasSize(1);

        assertThat(
                result.getTopicDiff()
                        .getRemoved()
                        .get(0)
                        .getName())
                .isEqualTo("Arrays");

        assertThat(result.getTopicDiff().getAdded())
                .hasSize(1);

        assertThat(
                result.getTopicDiff()
                        .getAdded()
                        .get(0)
                        .getName())
                .isEqualTo("Pointers");
    }

    @Test
    void compareSupplementalPersonResponsibleWithoutRawNotesField() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "personResponsible": "Old lecturer"
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "personResponsible": "New lecturer"
                }
                """);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges())
                .isTrue();

        assertThat(result.getGeneralInfoDiff())
                .containsOnlyKeys("personResponsible")
                .doesNotContainKey("notes");

        assertThat(
                result.getGeneralInfoDiff()
                        .get("personResponsible")
                        .getOldValue())
                .isEqualTo("Old lecturer");

        assertThat(
                result.getGeneralInfoDiff()
                        .get("personResponsible")
                        .getNewValue())
                .isEqualTo("New lecturer");

        assertThat(result.getRevisionInfoDiff())
                .doesNotContainKey("notes");
    }

    /*
     * ============================================================
     * TASK 2 CANONICAL FIELD-BY-FIELD REGRESSION TESTS
     * ============================================================
     */

    @Test
    void compareCanonicalSupplementalScalarFieldsBySection() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setChangeSummary("Old revision summary");
        newSyllabus.setChangeSummary("New revision summary");

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "personResponsible": "Dr. Old",
                  "dateRevised": "2025-08-01",
                  "creditPoints": "4",
                  "lectureCredits": "3",
                  "laboratoryCredits": "1",
                  "workloadStudentResponsibility": "Prepare before class",
                  "assessmentPassNote": "Old pass requirement",
                  "contentNote": "Old content note",
                  "internalNotes": "Old internal note"
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "personResponsible": "Dr. New",
                  "dateRevised": "2026-08-01",
                  "creditPoints": "5",
                  "lectureCredits": "4",
                  "laboratoryCredits": "1",
                  "workloadStudentResponsibility": "Prepare and review after class",
                  "assessmentPassNote": "New pass requirement",
                  "contentNote": "New content note",
                  "internalNotes": "New internal note"
                }
                """);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges())
                .isTrue();

        assertThat(result.getGeneralInfoDiff())
                .containsOnlyKeys("personResponsible");

        assertThat(result.getWorkloadCreditDiff())
                .containsKeys(
                        "workloadStudentResponsibility",
                        "creditPoints",
                        "lectureCredits")
                .doesNotContainKey("laboratoryCredits");

        assertThat(result.getContentDiff())
                .containsOnlyKeys("contentNote");

        assertThat(result.getAssessmentInfoDiff())
                .containsOnlyKeys("assessmentPassNote");

        assertThat(result.getRevisionInfoDiff())
                .containsKeys(
                        "dateRevised",
                        "internalNotes",
                        "changeSummary");

        assertThat(result.getRequirementsDiff())
                .isEmpty();

        assertThat(result.getExaminationDiff())
                .isEmpty();

        assertThat(result.getGeneralInfoDiff())
                .doesNotContainKey("notes");
    }

    @Test
    void compareLegacySupplementalAliasesEqualCanonicalFields() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "instructor": "Dr. Nguyen",
                  "ects": "4",
                  "creditsTheory": "3",
                  "creditsPractice": "1"
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "personResponsible": "Dr. Nguyen",
                  "creditPoints": "4",
                  "lectureCredits": "3",
                  "laboratoryCredits": "1"
                }
                """);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges())
                .isFalse();

        assertThat(result.getGeneralInfoDiff())
                .isEmpty();

        assertThat(result.getWorkloadCreditDiff())
                .isEmpty();

        assertThat(result.getRevisionInfoDiff())
                .isEmpty();
    }

    @Test
    void compareTopicSupplementalFieldsInCanonicalTopicDiff() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        Topic oldTopic = Topic.builder()
                .id(1)
                .syllabus(oldSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Arrays")
                .teachingHours(3)
                .labHours(0)
                .selfStudyHours(6)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Lecture")
                .learningActivity("Exercises")
                .build();

        Topic newTopic = Topic.builder()
                .id(11)
                .syllabus(newSyllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Arrays")
                .teachingHours(3)
                .labHours(0)
                .selfStudyHours(6)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Lecture")
                .learningActivity("Exercises")
                .build();

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "topicDetails": {
                    "0": {
                      "clo": "CLO1",
                      "assessments": "Quiz",
                      "resources": "Slides",
                      "level": "I",
                      "weight": "3"
                    }
                  }
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "topicDetails": {
                    "0": {
                      "clo": "CLO1",
                      "assessments": "Quiz, Lab",
                      "resources": "Slides, Lab guide",
                      "level": "I, T",
                      "weight": "4"
                    }
                  }
                }
                """);

        when(topicRepository.findBySyllabusId(100))
                .thenReturn(List.of(oldTopic));

        when(topicRepository.findBySyllabusId(101))
                .thenReturn(List.of(newTopic));

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getTopicDiff().getAdded())
                .isEmpty();

        assertThat(result.getTopicDiff().getRemoved())
                .isEmpty();

        assertThat(result.getTopicDiff().getModified())
                .hasSize(1);

        SyllabusDiffResponse.TopicDiff modified =
                result.getTopicDiff().getModified().get(0);

        assertThat(modified.getName())
                .isEqualTo("Arrays");

        assertThat(modified.getAssessments())
                .isEqualTo("Quiz, Lab");

        assertThat(modified.getResources())
                .isEqualTo("Slides, Lab guide");

        assertThat(modified.getContentWeight())
                .isEqualTo("4");

        assertThat(modified.getContentLevel())
                .isEqualTo("I, T");

        assertThat(modified.getChanges())
                .containsOnlyKeys(
                        "assessments",
                        "resources",
                        "contentWeight",
                        "contentLevel")
                .doesNotContainKey("notes");
    }

    @Test
    void comparePlannedActivitiesFieldByField() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "plannedActivities": [
                    {
                      "week": 1,
                      "topic": "Arrays",
                      "clo": "CLO1",
                      "assessments": "Quiz",
                      "learningActivities": "Lecture",
                      "resources": "Slides"
                    }
                  ]
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "plannedActivities": [
                    {
                      "week": 2,
                      "topic": "Arrays",
                      "clo": "CLO1, CLO2",
                      "assessments": "Quiz, Lab",
                      "learningActivities": "Lecture, Exercises",
                      "resources": "Slides, Lab guide"
                    }
                  ]
                }
                """);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.getPlannedActivityDiff().getAdded())
                .isEmpty();

        assertThat(result.getPlannedActivityDiff().getRemoved())
                .isEmpty();

        assertThat(result.getPlannedActivityDiff().getModified())
                .hasSize(1);

        SyllabusDiffResponse.PlannedActivityDiff modified =
                result.getPlannedActivityDiff().getModified().get(0);

        assertThat(modified.getTopic())
                .isEqualTo("Arrays");

        assertThat(modified.getWeek())
                .isEqualTo(2);

        assertThat(modified.getChanges())
                .containsOnlyKeys(
                        "week",
                        "clo",
                        "assessments",
                        "learningActivities",
                        "resources")
                .doesNotContainKey("topic");
    }

    @Test
    void compareIgnoresJsonMirrorsForRelationBackedMatricesAndReadings() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "ploCodes": ["PLO1"],
                  "cloPloMatrix": {
                    "CLO1": {
                      "PLO1": "x"
                    }
                  },
                  "assessmentCloMatrix": {
                    "0": {
                      "CLO1": "25"
                    }
                  },
                  "readings": [
                    {
                      "title": "Old mirrored book",
                      "author": "Old author",
                      "publisher": "Old publisher",
                      "year": "2020"
                    }
                  ]
                }
                """);

        newSyllabus.setNotes("""
                {
                  "schemaVersion": 1,
                  "ploCodes": ["PLO1", "PLO2"],
                  "cloPloMatrix": {
                    "CLO1": {
                      "PLO1": "xxx",
                      "PLO2": "xx"
                    }
                  },
                  "assessmentCloMatrix": {
                    "0": {
                      "CLO1": "75"
                    }
                  },
                  "readings": [
                    {
                      "title": "New mirrored book",
                      "author": "New author",
                      "publisher": "New publisher",
                      "year": "2026"
                    }
                  ]
                }
                """);

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        /*
         * These JSON structures are mirrors/supporting form state.
         * Canonical comparison is performed through CLO-PLO,
         * Assessment-CLO and SyllabusBook relation repositories.
         */
        assertThat(result.isHasChanges())
                .isFalse();

        assertThat(result.getCloPloDiff().getAdded())
                .isEmpty();

        assertThat(result.getCloPloDiff().getRemoved())
                .isEmpty();

        assertThat(result.getCloPloDiff().getModified())
                .isEmpty();

        assertThat(result.getAssessmentCloDiff().getAdded())
                .isEmpty();

        assertThat(result.getAssessmentCloDiff().getRemoved())
                .isEmpty();

        assertThat(result.getAssessmentCloDiff().getModified())
                .isEmpty();

        assertThat(result.getReadingsDiff().getAdded())
                .isEmpty();

        assertThat(result.getReadingsDiff().getRemoved())
                .isEmpty();

        assertThat(result.getReadingsDiff().getModified())
                .isEmpty();
    }

    @Test
    void compareMalformedLegacyNotesAsInternalNotesWithoutCrashing() {
        stubEmptyRepositories();
        makeBaseEquivalent();

        oldSyllabus.setNotes("legacy note: old unstructured content");
        newSyllabus.setNotes("legacy note: new unstructured content");

        SyllabusDiffResponse result =
                service.compare(oldSyllabus, newSyllabus);

        assertThat(result.isHasChanges())
                .isTrue();

        assertThat(result.getGeneralInfoDiff())
                .doesNotContainKey("notes");

        assertThat(result.getRevisionInfoDiff())
                .containsOnlyKeys("internalNotes");

        SyllabusDiffResponse.FieldDiff diff =
                result.getRevisionInfoDiff().get("internalNotes");

        assertThat(diff.getOldValue())
                .isEqualTo("legacy note: old unstructured content");

        assertThat(diff.getNewValue())
                .isEqualTo("legacy note: new unstructured content");
    }

    @Test
void compareIgnoresUnicodeAndWhitespaceOnlyFormattingDifferences() {

    stubEmptyRepositories();
    makeBaseEquivalent();

    oldSyllabus.setCourseDesignation(
            "Core\u00A0course");
    newSyllabus.setCourseDesignation(
            "Core course");

    oldSyllabus.setTeachingMethods(
            "Lecture\r\nand\tlaboratory");
    newSyllabus.setTeachingMethods(
            "Lecture and laboratory");

    oldSyllabus.setPrerequisites(
            "Data Structures\u00A0and Algorithms");
    newSyllabus.setPrerequisites(
            "Data Structures and Algorithms");

    oldSyllabus.setExamForms(
            "Assignments,\nFinal exam");
    newSyllabus.setExamForms(
            "Assignments, Final exam");

    SyllabusDiffResponse result =
            service.compare(
                    oldSyllabus,
                    newSyllabus);

    assertThat(result.getGeneralInfoDiff())
            .doesNotContainKeys(
                    "courseDesignation",
                    "teachingMethods");

    assertThat(result.getRequirementsDiff())
            .doesNotContainKey(
                    "prerequisites");

    assertThat(result.getExaminationDiff())
            .doesNotContainKey(
                    "examForms");

    assertThat(result.isHasChanges())
            .isFalse();
}
@Test
void compareIgnoresEquivalentStructuredFieldRepresentations() {

    stubEmptyRepositories();
    makeBaseEquivalent();

    oldSyllabus.setSemester("Semester 4");
    newSyllabus.setSemester("4");

    oldSyllabus.setCourseTypes(
            "Core, Required");
    newSyllabus.setCourseTypes(
            "required; core");

    oldSyllabus.setNotes("""
            {
              "schemaVersion": 1,
              "creditPoints": "4",
              "lectureCredits": "3.0",
              "laboratoryCredits": "1"
            }
            """);

    newSyllabus.setNotes("""
            {
              "schemaVersion": 1,
              "creditPoints": "4.0",
              "lectureCredits": "3",
              "laboratoryCredits": "1.00"
            }
            """);

    SyllabusDiffResponse result =
            service.compare(
                    oldSyllabus,
                    newSyllabus);

    assertThat(result.getGeneralInfoDiff())
            .doesNotContainKeys(
                    "semester",
                    "courseTypes");

    assertThat(result.getWorkloadCreditDiff())
            .doesNotContainKeys(
                    "creditPoints",
                    "lectureCredits",
                    "laboratoryCredits");

    assertThat(result.isHasChanges())
            .isFalse();
}

@Test
void compareIgnoresEquivalentWorkloadRepresentations() {

    stubEmptyRepositories();
    makeBaseEquivalent();

    oldSyllabus.setWorkloadTotal(
            "195 hours");
    newSyllabus.setWorkloadTotal(
            "195.0");

    oldSyllabus.setWorkloadContact(
            "45 (lecture) + 30 (laboratory)");
    newSyllabus.setWorkloadContact(
            "45 lecture + 30 laboratory");

    oldSyllabus.setWorkloadPrivate(
            "120.00 hours");
    newSyllabus.setWorkloadPrivate(
            "120");

    SyllabusDiffResponse result =
            service.compare(
                    oldSyllabus,
                    newSyllabus);

    assertThat(result.getWorkloadCreditDiff())
            .doesNotContainKeys(
                    "workloadTotal",
                    "workloadContact",
                    "workloadPrivate");

    assertThat(result.isHasChanges())
            .isFalse();
}

@Test
void compareIgnoresEquivalentPercentageAndCategoricalRepresentations() {

    stubEmptyRepositories();
    makeBaseEquivalent();

    Topic oldTopic =
            topic(
                    1,
                    oldSyllabus,
                    "Data Structures",
                    1,
                    1);

    Topic newTopic =
            topic(
                    2,
                    newSyllabus,
                    "Data Structures",
                    1,
                    1);


    oldTopic.setNotes("""
            {
              "schemaVersion": 1,
              "contentWeight": "10%",
              "contentLevel": "I, T"
            }
            """);

    newTopic.setNotes("""
            {
              "schemaVersion": 1,
              "contentWeight": "10.0",
              "contentLevel": "t; i"
            }
            """);

    when(topicRepository.findBySyllabusId(100))
            .thenReturn(List.of(oldTopic));

    when(topicRepository.findBySyllabusId(101))
            .thenReturn(List.of(newTopic));

    AssessmentComponent oldAssessment =
            assessment(
                    1,
                    oldSyllabus,
                    "Final Exam",
                    "FINAL_EXAM",
                    40f,
                    1);

    AssessmentComponent newAssessment =
            assessment(
                    2,
                    newSyllabus,
                    "Final Exam",
                    "final_exam",
                    40f,
                    1);

    when(assessmentComponentRepository.findBySyllabusId(100))
            .thenReturn(List.of(oldAssessment));

    when(assessmentComponentRepository.findBySyllabusId(101))
            .thenReturn(List.of(newAssessment));

    SyllabusDiffResponse result =
            service.compare(
                    oldSyllabus,
                    newSyllabus);

    assertThat(result.getTopicDiff().getModified())
            .isEmpty();

    assertThat(result.getAssessmentDiff().getModified())
            .isEmpty();

    assertThat(result.isHasChanges())
            .isFalse();
}
    /*
     * ============================================================
     * TEST HELPERS
     * ============================================================
     */

    private void makeBaseEquivalent() {
        oldSyllabus.setAcademicYear(newSyllabus.getAcademicYear());
        oldSyllabus.setSemester(newSyllabus.getSemester());
        oldSyllabus.setObjectives(newSyllabus.getObjectives());
        oldSyllabus.setNotes(newSyllabus.getNotes());
        oldSyllabus.setChangeSummary(newSyllabus.getChangeSummary());
    }

    private void stubEmptyRepositories() {
        lenient().when(cloRepository.findBySyllabusId(100))
                .thenReturn(List.of());
        lenient().when(cloRepository.findBySyllabusId(101))
                .thenReturn(List.of());

        lenient().when(cloPloMappingRepository.findByClo_Syllabus_Id(100))
                .thenReturn(List.of());
        lenient().when(cloPloMappingRepository.findByClo_Syllabus_Id(101))
                .thenReturn(List.of());

        lenient().when(topicRepository.findBySyllabusId(100))
                .thenReturn(List.of());
        lenient().when(topicRepository.findBySyllabusId(101))
                .thenReturn(List.of());

        lenient().when(topicCloRepository.findByTopic_Syllabus_Id(100))
                .thenReturn(List.of());
        lenient().when(topicCloRepository.findByTopic_Syllabus_Id(101))
                .thenReturn(List.of());

        lenient().when(assessmentComponentRepository.findBySyllabusId(100))
                .thenReturn(List.of());
        lenient().when(assessmentComponentRepository.findBySyllabusId(101))
                .thenReturn(List.of());

        lenient().when(assessmentCloRepository
                        .findByAssessmentComponent_Syllabus_Id(100))
                .thenReturn(List.of());
        lenient().when(assessmentCloRepository
                        .findByAssessmentComponent_Syllabus_Id(101))
                .thenReturn(List.of());

        lenient().when(syllabusBookRepository.findBySyllabus_Id(100))
                .thenReturn(List.of());
        lenient().when(syllabusBookRepository.findBySyllabus_Id(101))
                .thenReturn(List.of());
    }

    private AssessmentComponent assessment(
            int id,
            Syllabus syllabus,
            String name,
            String assessmentType,
            float weight,
            int orderIndex) {

        return AssessmentComponent.builder()
                .id(id)
                .syllabus(syllabus)
                .name(name)
                .assessmentType(assessmentType)
                .weightPercent(weight)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(orderIndex)
                .build();
    }

    private Topic topic(
            int id,
            Syllabus syllabus,
            String name,
            int weekNumber,
            int orderInWeek) {

        return Topic.builder()
                .id(id)
                .syllabus(syllabus)
                .name(name)
                .weekNumber(weekNumber)
                .orderInWeek(orderInWeek)
                .build();
    }
}
