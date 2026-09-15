package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.approval.repository.ApprovalRequestRepository;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.email.WorkflowNotificationService;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabusbook.repository.SyllabusBookRepository;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.common.exception.ForbiddenOperationException;
import com.scse.curriculum.syllabus.importer.repository.SyllabusImportHistoryRepository;
import jakarta.persistence.Query;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SyllabusServiceImplDeleteTest {
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

    @Mock private com.scse.curriculum.syllabus.service.SyllabusIdentityService syllabusIdentityService;
    @Mock private com.scse.curriculum.syllabus.history.SyllabusHistoryService syllabusHistoryService;
    @InjectMocks
    private SyllabusServiceImpl service;


    @Mock private SyllabusImportHistoryRepository syllabusImportHistoryRepository;
    @Mock private Query snapshotDelete;
    @Mock private Query snapshotCount;

    @Test
    void approvedCannotBeDeletedOrUnlinked() {
        Syllabus syllabus = Syllabus.builder().id(100).status(SyllabusStatus.APPROVED).build();
        Clo clo = Clo.builder().id(11).syllabus(syllabus).build();
        syllabus.getClos().add(clo);
        CourseProgram courseProgram = new CourseProgram();
        courseProgram.setSyllabus(syllabus);
        ClassSection classSection = new ClassSection();
        classSection.setSyllabus(syllabus);
        when(repository.findByIdWithRelations(100)).thenReturn(Optional.of(syllabus));

        assertThatThrownBy(() -> service.delete(100))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Approved syllabus cannot be deleted. Archive it instead.");

        assertThat(syllabus.getStatus()).isEqualTo(SyllabusStatus.APPROVED);
        assertThat(syllabus.getClos()).containsExactly(clo);
        assertThat(courseProgram.getSyllabus()).isSameAs(syllabus);
        assertThat(classSection.getSyllabus()).isSameAs(syllabus);
        verify(repository).findByIdWithRelations(100);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(courseProgramRepository, classSectionRepository, entityManager,
                cloRepository, cloPloMappingRepository, topicRepository, topicCloRepository,
                assessmentComponentRepository, assessmentCloRepository, studentScoreRepository,
                syllabusBookRepository, approvalRequestRepository, syllabusImportHistoryRepository);
    }

    @Test
    void draftSingleDeletePreservesCurriculumAndAssignmentRecords() {
        Syllabus syllabus = Syllabus.builder().id(100).status(SyllabusStatus.DRAFT).build();
        CourseProgram courseProgram = new CourseProgram();
        courseProgram.setSyllabus(syllabus);
        ClassSection classSection = new ClassSection();
        classSection.setSyllabus(syllabus);
        Clo clo = Clo.builder().id(11).syllabus(syllabus).build();
        when(repository.findByIdWithRelations(100)).thenReturn(Optional.of(syllabus));
        when(courseProgramRepository.findBySyllabus_Id(100)).thenReturn(List.of(courseProgram));
        when(classSectionRepository.findBySyllabusId(100)).thenReturn(List.of(classSection));
        when(cloRepository.findBySyllabusId(100)).thenReturn(List.of(clo));
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0, String.class).stripLeading().startsWith("DELETE")
                        ? snapshotDelete : snapshotCount);
        when(snapshotDelete.setParameter("syllabusId", 100)).thenReturn(snapshotDelete);
        when(snapshotCount.setParameter("syllabusId", 100)).thenReturn(snapshotCount);
        when(snapshotCount.getSingleResult()).thenReturn(0L);

        service.delete(100);

        verify(syllabusAccessService).assertCanModify(syllabus);
        assertThat(courseProgram.getSyllabus()).isNull();
        assertThat(classSection.getSyllabus()).isNull();
        verify(courseProgramRepository).saveAll(List.of(courseProgram));
        verify(classSectionRepository).saveAll(List.of(classSection));
        verify(courseProgramRepository, never()).delete(any());
        verify(courseProgramRepository, never()).deleteAll(any());
        verify(classSectionRepository, never()).delete(any());
        verify(classSectionRepository, never()).deleteAll(any());
        verify(snapshotDelete).executeUpdate();
        verify(cloPloMappingRepository).findByCloId(11);
        verify(cloRepository).deleteAll(List.of(clo));
        verify(approvalRequestRepository).deleteAll(List.of());
        verify(syllabusImportHistoryRepository).deleteAll(List.of());
        verify(repository).deleteById(100);
        verify(repository).flush();
    }
}
