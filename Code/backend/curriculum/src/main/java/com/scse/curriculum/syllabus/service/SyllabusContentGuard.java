package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyllabusContentGuard {

    private final SyllabusRepository syllabusRepository;
    private final SyllabusAccessService syllabusAccessService;

    public void assertCanView(Integer syllabusId) {
        Syllabus syllabus = getSyllabus(syllabusId);
        syllabusAccessService.assertCanView(syllabus);
    }

    public boolean canView(Integer syllabusId) {
        Syllabus syllabus = getSyllabus(syllabusId);
        return syllabusAccessService.canView(syllabus);
    }

    public void assertMutable(Integer syllabusId) {
        Syllabus syllabus = getSyllabus(syllabusId);

        syllabusAccessService.assertCanModify(syllabus);

        if (syllabus.getStatus() != SyllabusStatus.DRAFT) {
            throw new IllegalStateException(
                    "Version " + syllabus.getVersionLabel()
                            + " has already been submitted and is immutable. "
                            + "Clone it to create a new DRAFT.");
        }
    }

    public void assertSameSyllabus(
            Integer firstSyllabusId,
            Integer secondSyllabusId,
            String relationName) {

        if (!firstSyllabusId.equals(secondSyllabusId)) {
            throw new IllegalArgumentException(
                    relationName
                            + " may only be created between components "
                            + "belonging to the same syllabus.");
        }
    }

    private Syllabus getSyllabus(Integer syllabusId) {
        return syllabusRepository.findByIdWithRelations(syllabusId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Syllabus not found with id: " + syllabusId));
    }
}
