package com.scse.curriculum.assessment.service;

import com.scse.curriculum.assessment.dto.*;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentComponentServiceImpl
        implements AssessmentComponentService {

    private final AssessmentComponentRepository repository;

    private final SyllabusRepository syllabusRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public AssessmentComponentResponse create(
            CreateAssessmentComponentRequest request) {

        Syllabus syllabus =
                syllabusRepository.findById(
                                request.getSyllabusId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found"));

        syllabusContentGuard.assertMutable(syllabus.getId());

        AssessmentComponent component =
                AssessmentComponent.builder()
                        .syllabus(syllabus)
                        .name(request.getName())
                        .nameVn(request.getNameVn())
                        .assessmentType(
                                request.getAssessmentType())
                        .weightPercent(
                                request.getWeightPercent())
                        .minScore(
                                request.getMinScore())
                        .maxScore(
                                request.getMaxScore())
                        .orderIndex(
                                request.getOrderIndex())
                        .build();

        return map(
                repository.save(component));
    }

    @Override
    public List<AssessmentComponentResponse> getAll() {

        return repository.findAll()
                .stream()
                .filter(component -> syllabusContentGuard.canView(
                        component.getSyllabus().getId()))
                .map(this::map)
                .toList();
    }

    @Override
    public AssessmentComponentResponse getById(
            Integer id) {

        AssessmentComponent component =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assessment Component not found"));

        syllabusContentGuard.assertCanView(component.getSyllabus().getId());
        return map(component);
    }

    @Override
    public List<AssessmentComponentResponse> getBySyllabus(
            Integer syllabusId) {

        syllabusContentGuard.assertCanView(syllabusId);
        return repository.findBySyllabusId(
                        syllabusId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
public AssessmentComponentResponse update(
        Integer id,
        CreateAssessmentComponentRequest request) {

    AssessmentComponent component =
            repository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Assessment Component not found"));

    syllabusContentGuard.assertMutable(component.getSyllabus().getId());

    if (request.getSyllabusId() != null) {
        Syllabus syllabus =
                syllabusRepository.findById(
                                request.getSyllabusId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found"));

        syllabusContentGuard.assertMutable(syllabus.getId());
        component.setSyllabus(syllabus);
    }

    component.setName(request.getName());
    component.setNameVn(request.getNameVn());
    component.setAssessmentType(request.getAssessmentType());
    component.setWeightPercent(request.getWeightPercent());
    component.setMinScore(request.getMinScore());
    component.setMaxScore(request.getMaxScore());
    component.setOrderIndex(request.getOrderIndex());

    return map(repository.save(component));
}

@Override
public void delete(
        Integer id) {

    AssessmentComponent component =
            repository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Assessment Component not found"));

    syllabusContentGuard.assertMutable(component.getSyllabus().getId());
    repository.delete(component);
}
    private AssessmentComponentResponse map(
            AssessmentComponent component) {

        return AssessmentComponentResponse.builder()
                .id(component.getId())

                .syllabusId(
                        component.getSyllabus().getId())

                .courseCode(
                        component.getSyllabus()
                                .getCourse()
                                .getCourseCode())

                .name(
                        component.getName())

                .nameVn(
                        component.getNameVn())

                .assessmentType(
                        component.getAssessmentType())

                .weightPercent(
                        component.getWeightPercent())

                .minScore(
                        component.getMinScore())

                .maxScore(
                        component.getMaxScore())

                .orderIndex(
                        component.getOrderIndex())

                .build();
    }
}