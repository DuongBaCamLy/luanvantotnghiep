package com.scse.curriculum.cloplomapping.service;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.cloplomapping.dto.CreateCloPloMappingRequest;
import com.scse.curriculum.cloplomapping.dto.CloPloMappingResponse;
import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CloPloMappingServiceImpl
        implements CloPloMappingService {

    private final CloPloMappingRepository repository;

    private final CloRepository cloRepository;

    private final PloRepository ploRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public CloPloMappingResponse create(
            CreateCloPloMappingRequest request) {

        if (repository.existsByCloIdAndPloId(
                request.getCloId(),
                request.getPloId())) {

            throw new RuntimeException(
                    "Mapping already exists");
        }

        Clo clo =
                cloRepository.findById(
                                request.getCloId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "CLO not found"));

        syllabusContentGuard.assertMutable(clo.getSyllabus().getId());

        Plo plo =
                ploRepository.findById(
                                request.getPloId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "PLO not found"));

        Float contributionWeight = request.getContributionWeight();
        if (contributionWeight == null) {
            contributionWeight = 1.0f;
        }

        CloPloMapping mapping =
                CloPloMapping.builder()
                        .clo(clo)
                        .plo(plo)
                        .level(
                                request.getLevel())
                        .contributionWeight(contributionWeight)
                        .notes(
                                request.getNotes())
                        .build();

        return map(
                repository.save(mapping));
    }

    @Override
    public List<CloPloMappingResponse> getAll() {

        return repository.findAll()
                .stream()
                .filter(mapping -> syllabusContentGuard.canView(
                        mapping.getClo().getSyllabus().getId()))
                .map(this::map)
                .toList();
    }

    @Override
    public CloPloMappingResponse getById(
            Integer id) {

        CloPloMapping mapping = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mapping not found"));
        syllabusContentGuard.assertCanView(
                mapping.getClo().getSyllabus().getId());
        return map(mapping);
    }

    @Override
    public List<CloPloMappingResponse> getByClo(
            Integer cloId) {

        Clo clo = cloRepository.findById(cloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLO not found"));
        syllabusContentGuard.assertCanView(clo.getSyllabus().getId());

        return repository.findByCloId(cloId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<CloPloMappingResponse> getByPlo(
            Integer ploId) {

        return repository.findByPloId(ploId)
                .stream()
                .filter(mapping -> syllabusContentGuard.canView(
                        mapping.getClo().getSyllabus().getId()))
                .map(this::map)
                .toList();
    }

    @Override
    public void delete(
            Integer id) {

        CloPloMapping mapping = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mapping not found"));

        syllabusContentGuard.assertMutable(
                mapping.getClo().getSyllabus().getId());
        repository.delete(mapping);
    }

    private CloPloMappingResponse map(
            CloPloMapping mapping) {

        return CloPloMappingResponse.builder()
                .id(mapping.getId())

                .cloId(
                        mapping.getClo().getId())

                .cloCode(
                        mapping.getClo().getCode())

                .ploId(
                        mapping.getPlo().getId())

                .ploCode(
                        mapping.getPlo().getCode())

                .level(
                        mapping.getLevel())

                .contributionWeight(
                        mapping.getContributionWeight())

                .notes(
                        mapping.getNotes())

                .build();
    }
}