package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.cohort.service.CohortOperationalGuard;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyllabusContentGuardTest {

    @Mock
    private SyllabusRepository syllabusRepository;

    @Mock
    private SyllabusAccessService syllabusAccessService;

    @Mock
    private CohortOperationalGuard cohortOperationalGuard;

    @InjectMocks
    private SyllabusContentGuard guard;

    private Syllabus draft;

    @BeforeEach
    void setUp() {

        draft = Syllabus.builder()
                .id(100)
                .status(SyllabusStatus.DRAFT)
                .versionLabel("v1.0")
                .build();
    }

    @Test
    void archivedCohortBlocksMutableSyllabusContentAfterAuthorization() {

        when(syllabusRepository.findByIdWithRelations(100))
                .thenReturn(Optional.of(draft));

        doThrow(new IllegalStateException(
                "Archived Cohort is read-only. Restore it before making operational changes."))
                .when(cohortOperationalGuard)
                .assertSyllabusNotArchived(100);

        assertThatThrownBy(() ->
                guard.assertMutable(100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Archived Cohort");

        InOrder order =
                inOrder(
                        syllabusAccessService,
                        cohortOperationalGuard);

        order.verify(syllabusAccessService)
                .assertCanModify(draft);

        order.verify(cohortOperationalGuard)
                .assertSyllabusNotArchived(100);
    }

    @Test
    void activeDraftRemainsMutable() {

        when(syllabusRepository.findByIdWithRelations(100))
                .thenReturn(Optional.of(draft));

        assertThatCode(() ->
                guard.assertMutable(100))
                .doesNotThrowAnyException();

        verify(syllabusAccessService)
                .assertCanModify(draft);

        verify(cohortOperationalGuard)
                .assertSyllabusNotArchived(100);
    }

    @Test
    void workflowStatusStillBlocksMutationAfterArchiveCheck() {

        Syllabus approved = Syllabus.builder()
                .id(101)
                .status(SyllabusStatus.APPROVED)
                .versionLabel("v2.0")
                .build();

        when(syllabusRepository.findByIdWithRelations(101))
                .thenReturn(Optional.of(approved));

        assertThatThrownBy(() ->
                guard.assertMutable(101))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "not editable in its current workflow status");

        verify(syllabusAccessService)
                .assertCanModify(approved);

        verify(cohortOperationalGuard)
                .assertSyllabusNotArchived(101);
    }

    @Test
    void historicalReadDoesNotApplyArchiveMutationGuard() {

        Syllabus historical = Syllabus.builder()
                .id(102)
                .status(SyllabusStatus.APPROVED)
                .versionLabel("v3.0")
                .build();

        when(syllabusRepository.findByIdWithRelations(102))
                .thenReturn(Optional.of(historical));

        assertThatCode(() ->
                guard.assertCanView(102))
                .doesNotThrowAnyException();

        verify(syllabusAccessService)
                .assertCanView(historical);

        verifyNoInteractions(cohortOperationalGuard);
    }
}