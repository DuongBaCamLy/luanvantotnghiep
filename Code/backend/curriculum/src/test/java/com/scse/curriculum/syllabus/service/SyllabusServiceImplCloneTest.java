package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.entity.AssessmentType;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.book.entity.Book;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.entity.SectionType;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
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
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceImplCloneTest {

    @Mock private CourseProgramRepository courseProgramRepository;
    @Mock private SyllabusRepository repository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserAccountRepository userRepository;
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

    private Course course;
    private UserAccount faculty;
    private Syllabus source;
    private ClassSection targetAssignment;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(20)
                .courseCode("IT013IU")
                .name("Algorithms and Data Structures")
                .build();

        faculty = UserAccount.builder()
                .id(10)
                .username("faculty1")
                .role(UserRole.INSTRUCTOR)
                .instructorId(3)
                .isActive(true)
                .build();

        UserAccount oldCreator = UserAccount.builder()
                .id(11)
                .username("old-faculty")
                .role(UserRole.INSTRUCTOR)
                .build();

        source = Syllabus.builder()
                .id(50)
                .course(course)
                .versionNumber(4)
                .versionLabel("v4.0")
                .academicYear("2025-2026")
                .semester("HK1")
                .courseDesignation("Required")
                .courseTypes("[\"Core\"]")
                .language("English")
                .relation("Prerequisite relation")
                .teachingMethods("Lecture, Lab")
                .workloadTotal("135")
                .workloadContact("45")
                .workloadPrivate("90")
                .prerequisites("Programming Fundamentals")
                .objectives("Understand algorithms")
                .examForms("Midterm, Final")
                .examRequirements("Minimum 50%")
                .rubrics("Standard rubric")
                .major("CS")
                .status(SyllabusStatus.APPROVED)
                .isCurrent(true)
                .createdBy(oldCreator)
                .notes("Source notes")
                .build();

        Instructor instructor = Instructor.builder()
                .id(3)
                .staffCode("GV003")
                .fullName("Faculty One")
                .build();

        targetAssignment = ClassSection.builder()
                .id(99)
                .course(course)
                .instructor(instructor)
                .semester(2)
                .academicYear("2026-2027")
                .groupNumber(1)
                .sectionType(SectionType.THEORY)
                .isActive(true)
                .syllabus(null)
                .build();
    }

    @Test
    void cloneCopiesAllDetailsAndUsesTargetAssignmentContext() {
        CloneSyllabusRequest request = new CloneSyllabusRequest(
                99,
                null,
                null,
                "Update for semester 2");

        SyllabusAccessService.CloneAuthorization authorization =
                new SyllabusAccessService.CloneAuthorization(
                        faculty,
                        course,
                        "2026-2027",
                        "HK2",
                        List.of(targetAssignment));

        Clo sourceClo = Clo.builder()
                .id(1)
                .syllabus(source)
                .code("CLO1")
                .description("Analyze algorithms")
                .descriptionVn("Phân tích thuật toán")
                .competencyLevel(CompetencyLevel.KNOWLEDGE)
                .bloomLevel(BloomLevel.ANALYZE)
                .orderIndex(1)
                .build();

        Plo plo = Plo.builder()
                .id(7)
                .code("PLO2")
                .description("Problem solving")
                .build();

        CloPloMapping sourceCloPlo = CloPloMapping.builder()
                .id(2)
                .clo(sourceClo)
                .plo(plo)
                .level(ContributionLevel.D)
                .contributionWeight(0.75f)
                .notes("Mapping note")
                .build();

        Topic sourceTopic = Topic.builder()
                .id(3)
                .syllabus(source)
                .weekNumber(1)
                .orderInWeek(1)
                .name("Sorting")
                .nameVn("Sắp xếp")
                .teachingHours(3)
                .labHours(2)
                .selfStudyHours(5)
                .topicType(TopicType.LECTURE)
                .teachingMethod("Lecture")
                .learningActivity("Practice")
                .notes("Topic note")
                .build();

        TopicClo sourceTopicClo = TopicClo.builder()
                .id(new TopicCloId(3, 1))
                .topic(sourceTopic)
                .clo(sourceClo)
                .teachingLevel(TeachingLevel.D)
                .build();

        Book book = Book.builder()
                .id(8)
                .title("Introduction to Algorithms")
                .build();

        SyllabusBook sourceBook = SyllabusBook.builder()
                .id(new SyllabusBookId(50, 8))
                .syllabus(source)
                .book(book)
                .usageType(UsageType.REQUIRED)
                .orderIndex(1)
                .build();

        AssessmentComponent sourceAssessment = AssessmentComponent.builder()
                .id(4)
                .syllabus(source)
                .name("Final Exam")
                .nameVn("Thi cuối kỳ")
                .assessmentType(AssessmentType.FINAL_EXAM)
                .weightPercent(50f)
                .minScore(0f)
                .maxScore(100f)
                .orderIndex(2)
                .build();

        AssessmentClo sourceAssessmentClo = AssessmentClo.builder()
                .id(new AssessmentCloId(4, 1))
                .assessmentComponent(sourceAssessment)
                .clo(sourceClo)
                .contributionPercent(50f)
                .build();

        when(repository.findByIdWithRelations(50))
                .thenReturn(Optional.of(source));
        when(syllabusAccessService.authorizeClone(
                source, 99, null, null))
                .thenReturn(authorization);
        when(repository.findMaxVersionNumberByCourseId(20))
                .thenReturn(4);
        when(repository.saveAndFlush(any(Syllabus.class)))
                .thenAnswer(invocation -> {
                    Syllabus draft = invocation.getArgument(0);
                    draft.setId(60);
                    return draft;
                });

        when(cloRepository.findBySyllabusId(50))
                .thenReturn(List.of(sourceClo));
        when(cloRepository.save(any(Clo.class)))
                .thenAnswer(invocation -> {
                    Clo target = invocation.getArgument(0);
                    target.setId(101);
                    return target;
                });
        when(cloPloMappingRepository.findByCloId(1))
                .thenReturn(List.of(sourceCloPlo));

        when(topicRepository.findBySyllabusId(50))
                .thenReturn(List.of(sourceTopic));
        when(topicRepository.save(any(Topic.class)))
                .thenAnswer(invocation -> {
                    Topic target = invocation.getArgument(0);
                    target.setId(201);
                    return target;
                });
        when(topicCloRepository.findByIdTopicId(3))
                .thenReturn(List.of(sourceTopicClo));

        when(syllabusBookRepository.findBySyllabus_Id(50))
                .thenReturn(List.of(sourceBook));

        when(assessmentComponentRepository.findBySyllabusId(50))
                .thenReturn(List.of(sourceAssessment));
        when(assessmentComponentRepository.save(any(AssessmentComponent.class)))
                .thenAnswer(invocation -> {
                    AssessmentComponent target = invocation.getArgument(0);
                    target.setId(301);
                    return target;
                });
        when(assessmentCloRepository.findByAssessmentComponent_Id(4))
                .thenReturn(List.of(sourceAssessmentClo));

        when(repository.findByIdWithRelations(60))
                .thenAnswer(invocation -> Optional.of(targetAssignment.getSyllabus()));

        SyllabusResponse response = service.clone(50, request);

        ArgumentCaptor<Syllabus> syllabusCaptor =
                ArgumentCaptor.forClass(Syllabus.class);
        verify(repository).saveAndFlush(syllabusCaptor.capture());
        Syllabus cloned = syllabusCaptor.getValue();

        assertThat(cloned.getId()).isEqualTo(60);
        assertThat(cloned.getVersionNumber()).isEqualTo(5);
        assertThat(cloned.getVersionLabel()).isEqualTo("v5.0");
        assertThat(cloned.getAcademicYear()).isEqualTo("2026-2027");
        assertThat(cloned.getSemester()).isEqualTo("HK2");
        assertThat(cloned.getCreatedBy()).isSameAs(faculty);
        assertThat(cloned.getStatus()).isEqualTo(SyllabusStatus.DRAFT);
        assertThat(cloned.getCourseDesignation()).isEqualTo(source.getCourseDesignation());
        assertThat(cloned.getNotes()).isEqualTo("Source notes");
        assertThat(cloned.getChangeSummary()).isEqualTo("Update for semester 2");

        assertThat(targetAssignment.getSyllabus()).isSameAs(cloned);
        verify(classSectionRepository).saveAll(List.of(targetAssignment));

        ArgumentCaptor<CloPloMapping> cloPloCaptor =
                ArgumentCaptor.forClass(CloPloMapping.class);
        verify(cloPloMappingRepository).save(cloPloCaptor.capture());
        assertThat(cloPloCaptor.getValue().getClo().getId()).isEqualTo(101);
        assertThat(cloPloCaptor.getValue().getPlo()).isSameAs(plo);
        assertThat(cloPloCaptor.getValue().getContributionWeight()).isEqualTo(0.75f);

        ArgumentCaptor<TopicClo> topicCloCaptor =
                ArgumentCaptor.forClass(TopicClo.class);
        verify(topicCloRepository).save(topicCloCaptor.capture());
        assertThat(topicCloCaptor.getValue().getTopic().getId()).isEqualTo(201);
        assertThat(topicCloCaptor.getValue().getClo().getId()).isEqualTo(101);
        assertThat(topicCloCaptor.getValue().getTeachingLevel()).isEqualTo(TeachingLevel.D);

        ArgumentCaptor<SyllabusBook> bookCaptor =
                ArgumentCaptor.forClass(SyllabusBook.class);
        verify(syllabusBookRepository).save(bookCaptor.capture());
        assertThat(bookCaptor.getValue().getSyllabus().getId()).isEqualTo(60);
        assertThat(bookCaptor.getValue().getBook()).isSameAs(book);
        assertThat(bookCaptor.getValue().getUsageType()).isEqualTo(UsageType.REQUIRED);

        ArgumentCaptor<AssessmentClo> assessmentCloCaptor =
                ArgumentCaptor.forClass(AssessmentClo.class);
        verify(assessmentCloRepository).save(assessmentCloCaptor.capture());
        assertThat(assessmentCloCaptor.getValue()
                .getAssessmentComponent().getId()).isEqualTo(301);
        assertThat(assessmentCloCaptor.getValue().getClo().getId()).isEqualTo(101);
        assertThat(assessmentCloCaptor.getValue()
                .getContributionPercent()).isEqualTo(50f);

        assertThat(response.getId()).isEqualTo(60);
        assertThat(response.getAcademicYear()).isEqualTo("2026-2027");
        assertThat(response.getSemester()).isEqualTo("HK2");
        assertThat(response.getCreatedById()).isEqualTo(10);
    }
}
