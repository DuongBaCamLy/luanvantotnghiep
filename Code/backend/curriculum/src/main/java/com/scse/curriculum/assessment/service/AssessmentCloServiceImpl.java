package com.scse.curriculum.assessment.service;

import com.scse.curriculum.assessment.dto.AssessmentCloRequest;
import com.scse.curriculum.assessment.dto.AssessmentCloResponse;
import com.scse.curriculum.assessment.entity.AssessmentClo;
import com.scse.curriculum.assessment.entity.AssessmentCloId;
import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentCloRepository;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentCloServiceImpl
        implements AssessmentCloService {

    private final AssessmentCloRepository assessmentCloRepository;

    private final AssessmentComponentRepository assessmentComponentRepository;

    private final CloRepository cloRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public AssessmentCloResponse create(
            AssessmentCloRequest request) {

        AssessmentComponent component =
                assessmentComponentRepository.findById(
                        request.getAssessmentComponentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assessment Component not found with id: "
                                                + request.getAssessmentComponentId()));

        Clo clo =
                cloRepository.findById(
                        request.getCloId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CLO not found with id: "
                                                + request.getCloId()));

        Integer assessmentSyllabusId = component.getSyllabus().getId();
        Integer cloSyllabusId = clo.getSyllabus().getId();
        syllabusContentGuard.assertMutable(assessmentSyllabusId);
        syllabusContentGuard.assertSameSyllabus(
                assessmentSyllabusId,
                cloSyllabusId,
                "Assessment-CLO mapping");

        AssessmentCloId id =
                new AssessmentCloId(
                        component.getId(),
                        clo.getId());

        if (assessmentCloRepository.existsById(id)) {
            throw new RuntimeException(
                    "Assessment-CLO mapping already exists");
        }

        AssessmentClo assessmentClo =
                AssessmentClo.builder()
                        .id(id)
                        .assessmentComponent(component)
                        .clo(clo)
                        .contributionPercent(
                                request.getContributionPercent())
                        .build();

        AssessmentClo saved =
                assessmentCloRepository.save(assessmentClo);

        return mapToResponse(saved);
    }

    @Override
    public List<AssessmentCloResponse> getByAssessmentComponent(
            Integer assessmentComponentId) {

        AssessmentComponent component = assessmentComponentRepository
                .findById(assessmentComponentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assessment Component not found"));
        syllabusContentGuard.assertCanView(component.getSyllabus().getId());

        return assessmentCloRepository
                .findByAssessmentComponent_Id(
                        assessmentComponentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AssessmentCloResponse> getByClo(
            Integer cloId) {

        Clo clo = cloRepository.findById(cloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLO not found"));
        syllabusContentGuard.assertCanView(clo.getSyllabus().getId());

        return assessmentCloRepository
                .findByClo_Id(cloId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(
            Integer assessmentComponentId,
            Integer cloId) {

        AssessmentCloId id =
                new AssessmentCloId(
                        assessmentComponentId,
                        cloId);

        AssessmentClo mapping =
                assessmentCloRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assessment-CLO mapping not found"));

        syllabusContentGuard.assertMutable(
                mapping.getAssessmentComponent()
                        .getSyllabus()
                        .getId());
        assessmentCloRepository.delete(mapping);
    }

    private AssessmentCloResponse mapToResponse(
            AssessmentClo entity) {

        return AssessmentCloResponse.builder()
                .assessmentComponentId(
                        entity.getAssessmentComponent().getId())
                .assessmentName(
                        entity.getAssessmentComponent().getName())
                .cloId(
                        entity.getClo().getId())
                .cloCode(
                        entity.getClo().getCode())
                .contributionPercent(
                        entity.getContributionPercent())
                .build();
    }
}