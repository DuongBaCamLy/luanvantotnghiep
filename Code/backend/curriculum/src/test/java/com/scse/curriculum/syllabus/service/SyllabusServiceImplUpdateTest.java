package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.entity.CurriculumTerm;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.studentscore.entity.StudentScore;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.entity.TopicType;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceImplUpdateTest {

    @Mock private CourseProgramRepository courseProgramRepository;
    @Mock private PloRepository ploRepository;
    @Mock private SyllabusRepository repository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CloRepository cloRepository;
    @Mock private CloPloMappingRepository cloPloMappingRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private TopicCloRepository topicCloRepository;
    @Mock private SyllabusBookRepository syllabusBookRepository;
    @Mock private AssessmentComponentRepository assessmentComponentRepository;
    @Mock private AssessmentCloRepository assessmentCloRepository;
    @Mock private StudentScoreRepository studentScoreRepository;
    @Mock private ApprovalRequestRepository approvalRequestRepository;
    @Mock private ClassSectionRepository classSectionRepository;
    @Mock private WorkflowNotificationService workflowNotificationService;
    @Mock private SyllabusAccessService syllabusAccessService;
    @Mock private SyllabusSubmissionValidationService submissionValidationService;
    @Mock private SyllabusDiffService syllabusDiffService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private SyllabusServiceImpl service;

    private Syllabus syllabus;
    private Clo clo;
    private Topic topic;
    private AssessmentComponent assessment;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .id(53)
                .courseCode("IT116IU")
                .name("C/C++ Programming")
                .creditTheory(3)
                .creditLab(1)
                .build();

        UserAccount creator = UserAccount.builder()
                .id(1)
                .username("admin")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        syllabus = Syllabus.builder()
                .id(100)
                .course(course)
                .versionNumber(2)
                .versionLabel("v2.0")
                .academicYear("2026-2027")
                .semester("HK2")
                .status(SyllabusStatus.DRAFT)
                .isCurrent(false)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .clos(new ArrayList<>())
                .topics(new ArrayList<>())
                .assessments(new ArrayList<>())
                .build();

        clo = Clo.builder()
                .id(11)
                .syllabus(syllabus)
                .code("CLO 1")
                .description("Old CLO description")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.UNDERSTAND)
                .orderIndex(1)
                .build();
        topic = Topic.builder()
                .id(21)
                .syllabus(syllabus)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Old topic")
                .teachingHours(3)
                .labHours(0)
                .selfStudyHours(6)
                .topicType(TopicType.LECTURE)
                .build();
        assessment = AssessmentComponent.builder()
                .id(31)
                .syllabus(syllabus)
                .name("Old quiz")
                .assessmentType("QUIZ")
                .weightPercent(10f)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(1)
                .build();

        syllabus.getClos().add(clo);
        syllabus.getTopics().add(topic);
        syllabus.getAssessments().add(assessment);

        when(repository.findByIdWithRelations(100)).thenReturn(Optional.of(syllabus));
    }

    @Test
    void fullDraftUpdateKeepsChildIdsMappingsReadingsAndScores() {
        when(repository.save(syllabus)).thenReturn(syllabus);

        CreateSyllabusRequest request = new CreateSyllabusRequest();

        CreateSyllabusRequest.CloDTO updatedClo = new CreateSyllabusRequest.CloDTO();
        updatedClo.setId(11);
        updatedClo.setCode("CLO 1 renamed");
        updatedClo.setDescription("Updated CLO description");
        updatedClo.setCompetencyLevel("SKILL");
        updatedClo.setBloomLevel("APPLY");
        updatedClo.setOrderIndex(2);

        CreateSyllabusRequest.TopicDTO updatedTopic = new CreateSyllabusRequest.TopicDTO();
        updatedTopic.setId(21);
        updatedTopic.setWeekNumber(2);
        updatedTopic.setOrderInWeek(3);
        updatedTopic.setName("Updated topic");
        updatedTopic.setTeachingHours(4);
        updatedTopic.setLabHours(1);
        updatedTopic.setSelfStudyHours(7);
        updatedTopic.setTopicType("LAB");
        updatedTopic.setAssessments("Quiz, Lab");
        updatedTopic.setResources("1");

        CreateSyllabusRequest.AssessmentDTO updatedAssessment =
                new CreateSyllabusRequest.AssessmentDTO();
        updatedAssessment.setId(31);
        updatedAssessment.setName("Updated quiz");
        updatedAssessment.setAssessmentType("ASSIGNMENT");
        updatedAssessment.setWeightPercent(20f);
        updatedAssessment.setMinScore(5f);
        updatedAssessment.setMaxScore(10f);
        updatedAssessment.setOrderIndex(2);

        request.setClos(List.of(updatedClo));
        request.setTopics(List.of(updatedTopic));
        request.setAssessments(List.of(updatedAssessment));

        SyllabusResponse response = service.update(100, request);

        assertThat(syllabus.getClos()).containsExactly(clo);
        assertThat(clo.getId()).isEqualTo(11);
        assertThat(clo.getCode()).isEqualTo("CLO 1 renamed");
        assertThat(clo.getCompetencyLevel()).isEqualTo(CompetencyLevel.SKILL);

        assertThat(syllabus.getTopics()).containsExactly(topic);
        assertThat(topic.getId()).isEqualTo(21);
        assertThat(topic.getWeekNumber()).isEqualTo(2);
        assertThat(topic.getLabHours()).isEqualTo(1);

        assertThat(syllabus.getAssessments()).containsExactly(assessment);
        assertThat(assessment.getId()).isEqualTo(31);
        assertThat(assessment.getName()).isEqualTo("Updated quiz");
        assertThat(assessment.getWeightPercent()).isEqualTo(20f);

        assertThat(response.getClos().get(0).getId()).isEqualTo(11);
        assertThat(response.getTopics().get(0).getId()).isEqualTo(21);
        assertThat(response.getAssessments().get(0).getId()).isEqualTo(31);

        verify(assessmentCloRepository, never()).deleteAll(any());
        verify(topicCloRepository, never()).deleteAll(any());
        verify(cloPloMappingRepository, never()).deleteAll(any());
        verify(studentScoreRepository, never()).deleteAll(any());
        verify(studentScoreRepository, never()).findByAssessmentComponent_Id(any());
        verify(syllabusBookRepository, never()).deleteAll(any());
    }

    @Test
    void legacyPayloadWithoutIdsUsesNaturalKeysAndPreservesRelations() {
        when(repository.save(syllabus)).thenReturn(syllabus);

        CreateSyllabusRequest request = new CreateSyllabusRequest();
        request.setClos(List.of(new CreateSyllabusRequest.CloDTO(
                "clo 1", "Legacy client update", null,
                "KNOWLEDGE", "UNDERSTAND", 1)));
        request.setTopics(List.of(new CreateSyllabusRequest.TopicDTO(
                1, 1, "Topic edited by legacy client", null,
                3, 0, 6, "LECTURE", "Lecture", null,
                null, null, null)));
        request.setAssessments(List.of(new CreateSyllabusRequest.AssessmentDTO(
                "old quiz", null, "QUIZ", 15f, 0f, 100f, 1)));

        service.update(100, request);

        assertThat(syllabus.getClos()).containsExactly(clo);
        assertThat(syllabus.getTopics()).containsExactly(topic);
        assertThat(syllabus.getAssessments()).containsExactly(assessment);
        assertThat(clo.getDescription()).isEqualTo("Legacy client update");
        assertThat(topic.getName()).isEqualTo("Topic edited by legacy client");
        assertThat(assessment.getWeightPercent()).isEqualTo(15f);

        verify(assessmentCloRepository, never()).deleteAll(any());
        verify(topicCloRepository, never()).deleteAll(any());
        verify(cloPloMappingRepository, never()).deleteAll(any());
        verify(studentScoreRepository, never()).deleteAll(any());
    }

    @Test
    void semesterUpdateSynchronizesSyllabusAndLinkedCurriculumMapping() {
        CourseProgram mapping = CourseProgram.builder()
                .id(501)
                .syllabus(syllabus)
                .semesterSuggest(2)
                .termCode(CurriculumTerm.HK2)
                .build();
        when(courseProgramRepository.findBySyllabus_Id(100))
                .thenReturn(List.of(mapping));
        when(repository.save(syllabus)).thenReturn(syllabus);

        CreateSyllabusRequest request = new CreateSyllabusRequest();
        request.setSemester("Semester 1");

        SyllabusResponse response = service.update(100, request);

        assertThat(syllabus.getSemester()).isEqualTo("Semester 1");
        assertThat(mapping.getSemesterSuggest()).isEqualTo(1);
        assertThat(mapping.getTermCode()).isEqualTo(CurriculumTerm.HK1);
        assertThat(response.getSemester()).isEqualTo("Semester 1");
        verify(courseProgramRepository).saveAll(List.of(mapping));
        verify(repository).flush();
    }

    @Test
    void removingAssessmentWithScoresIsRejectedInsteadOfDeletingScores() {
        CreateSyllabusRequest request = new CreateSyllabusRequest();
        request.setAssessments(List.of());

        when(studentScoreRepository.findByAssessmentComponent_Id(31))
                .thenReturn(List.of(StudentScore.builder().id(900).build()));

        assertThatThrownBy(() -> service.update(100, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("student scores exist");

        assertThat(syllabus.getAssessments()).containsExactly(assessment);
        verify(studentScoreRepository, never()).deleteAll(any());
        verify(assessmentCloRepository, never()).deleteAll(any());
        verify(repository, never()).save(any(Syllabus.class));
    }
}
