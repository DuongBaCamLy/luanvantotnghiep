package com.scse.curriculum.clo.service;

import com.scse.curriculum.clo.dto.CloResponse;
import com.scse.curriculum.clo.dto.CreateCloRequest;
import com.scse.curriculum.clo.entity.BloomLevel;
import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.entity.CompetencyLevel;
import com.scse.curriculum.clo.repository.CloRepository;
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
public class CloServiceImpl implements CloService {

    private final CloRepository repository;

    private final SyllabusRepository syllabusRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public CloResponse create(
            CreateCloRequest request) {

        if (repository.existsBySyllabusIdAndCode(
                request.getSyllabusId(),
                request.getCode())) {

            throw new RuntimeException(
                    "CLO already exists in syllabus");
        }

        Syllabus syllabus =
                syllabusRepository.findById(
                                request.getSyllabusId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found"));

        syllabusContentGuard.assertMutable(syllabus.getId());

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            orderIndex = 1;
        }

        Clo clo = Clo.builder()
                .syllabus(syllabus)
                .code(
                        request.getCode())
                .description(
                        request.getDescription())
                .descriptionVn(
                        request.getDescriptionVn())
                .competencyLevel(
                        request.getCompetencyLevel() == null
                                ? null
                                : CompetencyLevel.valueOf(
                                        request.getCompetencyLevel()
                                                .toUpperCase()))
                .bloomLevel(
                        request.getBloomLevel() == null
                                ? null
                                : BloomLevel.valueOf(
                                        request.getBloomLevel()
                                                .toUpperCase()))
                .orderIndex(orderIndex)
                .build();

        return map(
                repository.save(clo));
    }

    @Override
    public List<CloResponse> getAll() {

        return repository.findAll()
                .stream()
                .filter(clo -> syllabusContentGuard.canView(
                        clo.getSyllabus().getId()))
                .map(this::map)
                .toList();
    }

    @Override
    public CloResponse getById(
            Integer id) {

        Clo clo = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "CLO not found"));

        syllabusContentGuard.assertCanView(clo.getSyllabus().getId());
        return map(clo);
    }

    @Override
    public List<CloResponse> getBySyllabus(
            Integer syllabusId) {

        syllabusContentGuard.assertCanView(syllabusId);
        return repository.findBySyllabusId(
                        syllabusId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
public CloResponse update(
        Integer id,
        CreateCloRequest request) {

    Clo clo = repository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "CLO not found"));

    syllabusContentGuard.assertMutable(clo.getSyllabus().getId());

    if (request.getSyllabusId() != null
            && (clo.getSyllabus() == null
            || !request.getSyllabusId().equals(clo.getSyllabus().getId()))) {

        Syllabus syllabus =
                syllabusRepository.findById(
                                request.getSyllabusId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Syllabus not found"));

        syllabusContentGuard.assertMutable(syllabus.getId());
        clo.setSyllabus(syllabus);
    }

    clo.setCode(request.getCode());
    clo.setDescription(request.getDescription());
    clo.setDescriptionVn(request.getDescriptionVn());

    clo.setCompetencyLevel(
            request.getCompetencyLevel() == null
                    ? null
                    : CompetencyLevel.valueOf(
                            request.getCompetencyLevel().toUpperCase()));

    clo.setBloomLevel(
            request.getBloomLevel() == null
                    ? null
                    : BloomLevel.valueOf(
                            request.getBloomLevel().toUpperCase()));

    clo.setOrderIndex(
            request.getOrderIndex() == null
                    ? 1
                    : request.getOrderIndex());

    return map(repository.save(clo));
}

@Override
public void delete(
        Integer id) {

    Clo clo = repository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "CLO not found"));

    syllabusContentGuard.assertMutable(clo.getSyllabus().getId());
    repository.delete(clo);
}
    private CloResponse map(
            Clo clo) {

        return CloResponse.builder()
                .id(
                        clo.getId())

                .syllabusId(
                        clo.getSyllabus().getId())

                .code(
                        clo.getCode())

                .description(
                        clo.getDescription())

                .descriptionVn(
                        clo.getDescriptionVn())

                .competencyLevel(
                        clo.getCompetencyLevel() == null
                                ? null
                                : clo.getCompetencyLevel().name())

                .bloomLevel(
                        clo.getBloomLevel() == null
                                ? null
                                : clo.getBloomLevel().name())

                .orderIndex(
                        clo.getOrderIndex())

                .build();
    }
}