package com.scse.curriculum.syllabus.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabusbook.entity.SyllabusBook;
import com.scse.curriculum.syllabusbook.entity.UsageType;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.entity.TopicType;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.entity.TeachingLevel;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.plo.entity.Plo;
@ExtendWith(MockitoExtension.class)
class SyllabusSubmissionValidationServiceTest {

    @Mock
    private CloRepository cloRepository;
    @Mock
    private CloPloMappingRepository cloPloMappingRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
private TopicCloRepository topicCloRepository;
    @Mock
    private AssessmentComponentRepository assessmentRepository;
@Mock
private AssessmentCloRepository assessmentCloRepository;
    @Mock
    private SyllabusBookRepository syllabusBookRepository;

    @InjectMocks
    private SyllabusSubmissionValidationService service;

    @Test
    void invalidDraftReturnsGroupedMandatoryErrors() {
        Syllabus syllabus = Syllabus.builder()
                .id(10)
                .course(Course.builder()
                        .courseCode("TEST")
                        .name("Test Course")
                        .nameVn("Môn kiểm thử")
                        .creditTheory(3)
                        .creditLab(0)
                        .build())
                .createdBy(UserAccount.builder().id(6).build())
                .academicYear("CS2021")
                .semester("HK1")
                .versionLabel("v1.0")
                .build();

        when(cloRepository.findBySyllabusId(10)).thenReturn(List.of());
        when(topicRepository.findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(10))
                .thenReturn(List.of());
        when(assessmentRepository.findBySyllabusId(10)).thenReturn(List.of());
        when(syllabusBookRepository.findBySyllabus_Id(10)).thenReturn(List.of());
when(topicCloRepository.findByTopic_Syllabus_Id(10))
        .thenReturn(List.of());

when(assessmentCloRepository
        .findByAssessmentComponent_Syllabus_Id(10))
        .thenReturn(List.of());
        SubmissionValidationResponse result = service.validate(syllabus);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCount()).isEqualTo(result.getIssues().size());
        assertThat(result.getIssues())
                .extracting(issue -> issue.getSection())
                .contains(
                        "General Information",
                        "Course Learning Outcomes (CLO)",
                        "Teaching Content",
                        "Assessment Plan",
                        "Reading List");
    }

    @Test
    void completeDraftPassesValidation() {
        Course course = Course.builder()
                .courseCode("TEST")
                .name("Test Course")
                .nameVn("Môn kiểm thử")
                .creditTheory(3)
                .creditLab(0)
                .build();

        Syllabus syllabus = Syllabus.builder()
                .id(11)
                .course(course)
                .createdBy(UserAccount.builder().id(6).build())
                .academicYear("CS2021")
                .semester("HK1")
                .versionLabel("v1.0")
                .courseDesignation("Fundamental course")
                .courseTypes("[\"Fundamental\"]")
                .language("English")
                .relation("None")
                .teachingMethods("Lecture and laboratory")
                .workloadTotal("10")
                .workloadContact("3 (lecture) + 1 (laboratory)")
                .workloadPrivate("6")
                .prerequisites("None")
                .objectives("Understand the fundamentals")
                .examForms("Assignments and final exam")
                .examRequirements("Complete all mandatory assessments")
                .build();

        Clo clo = Clo.builder()
                .id(100)
                .syllabus(syllabus)
                .code("CLO1")
                .description("Apply the core concepts")
                .bloomLevel(BloomLevel.APPLY)
                .competencyLevel(CompetencyLevel.SKILL)
                .build();

        Topic topic = Topic.builder()
                .id(200)
                .syllabus(syllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Introduction")
                .teachingHours(3)
                .labHours(1)
                .selfStudyHours(6)
                .topicType(TopicType.LECTURE)
                .build();
TopicClo topicClo = TopicClo.builder()
        .topic(topic)
        .clo(clo)
        .teachingLevel(TeachingLevel.I)
        .build();
        AssessmentComponent assessment = AssessmentComponent.builder()
                .id(300)
                .syllabus(syllabus)
                .name("Final exam")
                .assessmentType("FINAL_EXAM")
                .weightPercent(100f)
                .minScore(0f)
                .maxScore(10f)
                .build();
AssessmentClo assessmentClo =
        AssessmentClo.builder()
                .assessmentComponent(assessment)
                .clo(clo)
                .contributionPercent(100f)
                .build();
    when(topicCloRepository.findByTopic_Syllabus_Id(11))
        .thenReturn(List.of(topicClo));
        when(assessmentCloRepository
        .findByAssessmentComponent_Syllabus_Id(11))
        .thenReturn(List.of(assessmentClo));
                SyllabusBook book = SyllabusBook.builder()
        .syllabus(syllabus)
        .book(
                Book.builder()
                        .title("Required textbook")
                        .author("John Smith")
                        .year(2024)
                        .build()
        )
        .usageType(UsageType.REQUIRED)
        .orderIndex(1)
        .build();
        when(cloRepository.findBySyllabusId(11)).thenReturn(List.of(clo));
        when(cloPloMappingRepository.findByCloId(100))
                .thenReturn(List.of(validCloPloMapping(clo)));
        when(topicRepository.findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(11))
                .thenReturn(List.of(topic));
        when(assessmentRepository.findBySyllabusId(11))
                .thenReturn(List.of(assessment));
        when(syllabusBookRepository.findBySyllabus_Id(11))
                .thenReturn(List.of(book));

        SubmissionValidationResponse result = service.validate(syllabus);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getAssessmentTotalWeight()).isEqualTo(100d);
        assertThat(result.getWorkloadTotal()).isEqualTo(10d);
        assertThat(result.getWorkloadContact()).isEqualTo(4d);
        assertThat(result.getTopicContactHours()).isEqualTo(4d);
        assertThat(result.getTopicPrivateHours()).isEqualTo(6d);
    }
    @Test
void validContentWithoutTopicAndAssessmentMappingsIsRejected() {
    Course course = Course.builder()
            .courseCode("TEST")
            .name("Test Course")
            .nameVn("Môn kiểm thử")
            .creditTheory(3)
            .creditLab(0)
            .build();

    Syllabus syllabus = Syllabus.builder()
            .id(12)
            .course(course)
            .createdBy(UserAccount.builder().id(6).build())
            .academicYear("CS2021")
            .semester("HK1")
            .versionLabel("v1.0")
            .major("Computer Science")
            .courseDesignation("Fundamental course")
            .courseTypes("[\"Fundamental\"]")
            .language("English")
            .relation("None")
            .teachingMethods("Lecture")
            .workloadTotal("10")
            .workloadContact("4")
            .workloadPrivate("6")
            .prerequisites("None")
            .objectives("Understand fundamentals")
            .examForms("Final exam")
            .examRequirements("Complete examination")
            .build();

    Clo clo = Clo.builder()
            .id(101)
            .syllabus(syllabus)
            .code("CLO1")
            .description("Apply concepts")
            .bloomLevel(BloomLevel.APPLY)
            .competencyLevel(CompetencyLevel.SKILL)
            .build();

    Topic topic = Topic.builder()
            .id(201)
            .syllabus(syllabus)
            .weekNumber(1)
            .orderInWeek(1)
            .name("Introduction")
            .teachingHours(3)
            .labHours(1)
            .selfStudyHours(6)
            .topicType(TopicType.LECTURE)
            .build();

    AssessmentComponent assessment =
            AssessmentComponent.builder()
                    .id(301)
                    .syllabus(syllabus)
                    .name("Final exam")
                    .assessmentType("FINAL_EXAM")
                    .weightPercent(100f)
                    .minScore(0f)
                    .maxScore(10f)
                    .build();

    SyllabusBook book = SyllabusBook.builder()
        .syllabus(syllabus)
        .book(Book.builder()
                .title("Required textbook")
                .author("John Smith")
                .year(2024)
                .build())
        .usageType(UsageType.REQUIRED)
        .orderIndex(1)
        .build();

    when(cloRepository.findBySyllabusId(12))
            .thenReturn(List.of(clo));

    when(cloPloMappingRepository.findByCloId(101))
            .thenReturn(List.of(
                    validCloPloMapping(clo)));

    when(topicRepository
            .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                    12))
            .thenReturn(List.of(topic));

    when(topicCloRepository.findByTopic_Syllabus_Id(12))
            .thenReturn(List.of());

    when(assessmentRepository.findBySyllabusId(12))
            .thenReturn(List.of(assessment));

    when(assessmentCloRepository
            .findByAssessmentComponent_Syllabus_Id(12))
            .thenReturn(List.of());

    when(syllabusBookRepository.findBySyllabus_Id(12))
            .thenReturn(List.of(book));

    SubmissionValidationResponse result =
            service.validate(syllabus);

    assertThat(result.isValid()).isFalse();

    assertThat(result.getIssues())
            .extracting(issue -> issue.getCode())
            .contains(
                    "TOPIC_CLO_MAPPING_REQUIRED",
                    "ASSESSMENT_CLO_MAPPING_REQUIRED");
}

private CloPloMapping validCloPloMapping(
        Clo clo) {

    Plo plo = Plo.builder()
            .id(9001)
            .code("PLO1")
            .description("Apply professional knowledge")
            .isActive(true)
            .build();

    return CloPloMapping.builder()
            .id(9002)
            .clo(clo)
            .plo(plo)
            .level(ContributionLevel.I)
            .contributionWeight(null)
            .build();
}
@Test
void missingCloPloMetadataAndReadingMetadataAreRejected() {
    Syllabus syllabus = Syllabus.builder()
            .id(13)
            .build();

    Clo clo = Clo.builder()
            .id(1301)
            .syllabus(syllabus)
            .code("CLO1")
            .description("Apply fundamental concepts")
            .bloomLevel(BloomLevel.APPLY)
            .competencyLevel(
                    CompetencyLevel.SKILL)
            .build();

    Plo plo = Plo.builder()
            .id(1302)
            .code("PLO1")
            .description("Program learning outcome")
            .isActive(true)
            .build();

    CloPloMapping missingTarget =
            CloPloMapping.builder()
                    .id(1303)
                    .clo(clo)
                    .plo(null)
                    .level(ContributionLevel.I)
                    .build();

    CloPloMapping missingLevel =
            CloPloMapping.builder()
                    .id(1304)
                    .clo(clo)
                    .plo(plo)
                    .level(null)
                    .build();

    Book incompleteBook = Book.builder()
            .id(1305)
            .title("Incomplete reference")
            .author(null)
            .year(null)
            .build();

    SyllabusBook linkedBook =
            SyllabusBook.builder()
                    .syllabus(syllabus)
                    .book(incompleteBook)
                    .usageType(UsageType.REQUIRED)
                    .orderIndex(1)
                    .build();

    when(cloRepository.findBySyllabusId(13))
            .thenReturn(List.of(clo));

    when(cloPloMappingRepository
            .findByCloId(1301))
            .thenReturn(List.of(
                    missingTarget,
                    missingLevel));

    when(topicRepository
            .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                    13))
            .thenReturn(List.of());

    when(topicCloRepository
            .findByTopic_Syllabus_Id(13))
            .thenReturn(List.of());

    when(assessmentRepository
            .findBySyllabusId(13))
            .thenReturn(List.of());

    when(assessmentCloRepository
            .findByAssessmentComponent_Syllabus_Id(
                    13))
            .thenReturn(List.of());

    when(syllabusBookRepository
            .findBySyllabus_Id(13))
            .thenReturn(List.of(linkedBook));

    SubmissionValidationResponse result =
            service.validate(syllabus);

    assertThat(result.isValid()).isFalse();

    assertThat(result.getIssues())
            .extracting(issue -> issue.getCode())
            .contains(
                    "CLO_PLO_TARGET_REQUIRED",
                    "CLO_PLO_LEVEL_REQUIRED",
                    "READING_BOOK_AUTHOR_REQUIRED",
                    "READING_BOOK_YEAR_REQUIRED");
}
}
