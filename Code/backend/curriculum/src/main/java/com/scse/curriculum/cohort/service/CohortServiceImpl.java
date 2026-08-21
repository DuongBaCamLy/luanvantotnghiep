package com.scse.curriculum.cohort.service;

import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.cohort.dto.CohortResponse;
import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortServiceImpl
        implements CohortService {

    private final CohortRepository repository;

    private final ProgramRepository programRepository;

    @Override
    public CohortResponse create(
            CreateCohortRequest request) {

        if (repository.existsByProgramIdAndEntryYear(
                request.getProgramId(),
                request.getEntryYear())) {

            throw new RuntimeException(
                    "Cohort already exists");
        }

        Program program
                = programRepository.findById(
                        request.getProgramId())
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Program not found"));

        Cohort cohort = Cohort.builder()
                .program(program)
                .entryYear(request.getEntryYear())
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .build();

        return map(
                repository.save(cohort));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CohortResponse getById(
            Integer id) {

        Cohort cohort
                = repository.findById(id)
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Cohort not found"));

        return map(cohort);
    }

    @Override
    @Transactional(readOnly = true)
    public CohortResponse getByName(
            String name) {

        Cohort cohort
                = repository.findByName(name)
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Cohort not found"));

        return map(cohort);
    }

    private CohortResponse map(
            Cohort cohort) {

        return CohortResponse.builder()
                .id(cohort.getId())
                .programId(
                        cohort.getProgram().getId())
                .programCode(
                        cohort.getProgram().getCode())
                .programName(
                        cohort.getProgram().getName())
                .entryYear(
                        cohort.getEntryYear())
                .name(
                        cohort.getName())
                .description(
                        cohort.getDescription())
                .isActive(
                        cohort.getIsActive())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CohortResponse> getByProgram(
            Integer programId) {

        return repository
                .findByProgram_IdOrderByEntryYearDesc(programId)
                .stream()
                .map(this::map)
                .toList();
    }
}
