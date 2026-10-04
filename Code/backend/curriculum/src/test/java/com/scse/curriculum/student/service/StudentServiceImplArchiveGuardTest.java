package com.scse.curriculum.student.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.student.dto.CreateStudentRequest;
import com.scse.curriculum.student.dto.StudentResponse;
import com.scse.curriculum.student.entity.Student;
import com.scse.curriculum.student.repository.StudentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplArchiveGuardTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortOperationalGuard cohortOperationalGuard;

    @InjectMocks
    private StudentServiceImpl service;

    @Test
    void cannotCreateStudentInArchivedCohort() {

        Cohort archived = cohort(
                100,
                "CS2022",
                false);

        CreateStudentRequest request =
                request("ITITIU001", 100);

        when(cohortRepository.findById(100))
                .thenReturn(Optional.of(archived));

        doThrow(archivedException())
                .when(cohortOperationalGuard)
                .assertActive(archived);

        assertThatThrownBy(() ->
                service.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archived);

        verify(repository, never())
                .save(any(Student.class));
    }

    @Test
    void cannotUpdateStudentAlreadyBelongingToArchivedCohort() {

        Cohort archived = cohort(
                100,
                "CS2022",
                false);

        Student student = student(
                1,
                "ITITIU001",
                archived);

        CreateStudentRequest request =
                request("ITITIU001", 100);

        when(repository.findById(1))
                .thenReturn(Optional.of(student));

        doThrow(archivedException())
                .when(cohortOperationalGuard)
                .assertActive(archived);

        assertThatThrownBy(() ->
                service.update(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archived);

        verify(repository, never())
                .findByStudentCode(anyString());

        verifyNoInteractions(cohortRepository);

        verify(repository, never())
                .save(any(Student.class));
    }

    @Test
    void cannotMoveStudentIntoArchivedCohort() {

        Cohort active = cohort(
                101,
                "CS2026",
                true);

        Cohort archived = cohort(
                100,
                "CS2022",
                false);

        Student student = student(
                1,
                "ITITIU001",
                active);

        CreateStudentRequest request =
                request("ITITIU001", 100);

        when(repository.findById(1))
                .thenReturn(Optional.of(student));

        when(repository.findByStudentCode(
                "ITITIU001"))
                .thenReturn(Optional.of(student));

        when(cohortRepository.findById(100))
                .thenReturn(Optional.of(archived));

        org.mockito.Mockito.doNothing()
                .when(cohortOperationalGuard)
                .assertActive(active);

        doThrow(archivedException())
                .when(cohortOperationalGuard)
                .assertActive(archived);

        assertThatThrownBy(() ->
                service.update(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(active);

        verify(cohortOperationalGuard)
                .assertActive(archived);

        verify(repository, never())
                .save(any(Student.class));
    }

    @Test
    void cannotDeleteStudentBelongingToArchivedCohort() {

        Cohort archived = cohort(
                100,
                "CS2022",
                false);

        Student student = student(
                1,
                "ITITIU001",
                archived);

        when(repository.findById(1))
                .thenReturn(Optional.of(student));

        doThrow(archivedException())
                .when(cohortOperationalGuard)
                .assertActive(archived);

        assertThatThrownBy(() ->
                service.delete(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        verify(cohortOperationalGuard)
                .assertActive(archived);

        verify(repository, never())
                .delete(any(Student.class));
    }

    @Test
    void historicalStudentReadRemainsAvailableForArchivedCohort() {

        Cohort archived = cohort(
                100,
                "CS2022",
                false);

        Student student = student(
                1,
                "ITITIU001",
                archived);

        when(repository.findById(1))
                .thenReturn(Optional.of(student));

        StudentResponse result =
                service.getById(1);

        assertThat(result.getId())
                .isEqualTo(1);

        assertThat(result.getStudentCode())
                .isEqualTo("ITITIU001");

        assertThat(result.getCohortId())
                .isEqualTo(100);

        assertThat(result.getCohortName())
                .isEqualTo("CS2022");

        verifyNoInteractions(
                cohortOperationalGuard);
    }

    private CreateStudentRequest request(
            String studentCode,
            Integer cohortId) {

        CreateStudentRequest request =
                new CreateStudentRequest();

        request.setStudentCode(studentCode);
        request.setFullName("Test Student");
        request.setEmail("student@example.edu");
        request.setCohortId(cohortId);
        request.setIsActive(true);

        return request;
    }

    private Cohort cohort(
            Integer id,
            String name,
            boolean active) {

        return Cohort.builder()
                .id(id)
                .name(name)
                .isActive(active)
                .build();
    }

    private Student student(
            Integer id,
            String studentCode,
            Cohort cohort) {

        return Student.builder()
                .id(id)
                .studentCode(studentCode)
                .fullName("Test Student")
                .email("student@example.edu")
                .cohort(cohort)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private IllegalStateException archivedException() {

        return new IllegalStateException(
                "Archived Cohort is read-only. Restore it before making operational changes.");
    }
}
