package com.scse.curriculum.plo.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.plo.dto.CreatePloRequest;
import com.scse.curriculum.plo.dto.PloResponse;
import com.scse.curriculum.plo.entity.Plo;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PloServiceImpl implements PloService {

    private final PloRepository repository;
    private final ProgramRepository programRepository;

    @Override
@Transactional
public PloResponse create(
        CreatePloRequest request) {

        Integer version =
                request.getVersionNumber() == null
                        ? 1
                        : request.getVersionNumber();

        if (repository.existsByProgramIdAndCodeAndVersionNumber(
                request.getProgramId(),
                request.getCode(),
                version)) {

            throw new RuntimeException(
                    "PLO already exists");
        }

        Program program =
                programRepository.findById(
                        request.getProgramId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Program not found"));

        Plo plo = Plo.builder()
                .program(program)
                .code(request.getCode())
                .description(request.getDescription())
                .descriptionVn(request.getDescriptionVn())
                .category(request.getCategory())
                .versionNumber(version)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return map(repository.save(plo));
    }

    @Override
    public List<PloResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public PloResponse getById(
            Integer id) {

        Plo plo = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "PLO not found"));

        return map(plo);
    }

    @Override
    public List<PloResponse> getByCode(
            String code) {

        List<Plo> plos =
                repository.findAllByCode(code);

        if (plos.isEmpty()) {
            throw new ResourceNotFoundException(
                    "PLO not found");
        }

        return plos.stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<PloResponse> getByProgram(
            Integer programId) {

        return repository.findByProgramId(programId)
                .stream()
                .map(this::map)
                .toList();
    }

   @Override
@Transactional
public PloResponse update(
        Integer id,
        CreatePloRequest request) {
        Plo plo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PLO not found"));

        if (!plo.getProgram().getId().equals(request.getProgramId())) {
            Program program = programRepository.findById(request.getProgramId())
                    .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
            plo.setProgram(program);
        }

        plo.setCode(request.getCode());
        plo.setDescription(request.getDescription());
        plo.setDescriptionVn(request.getDescriptionVn());
        plo.setCategory(request.getCategory());
        if (request.getVersionNumber() != null) {
            plo.setVersionNumber(request.getVersionNumber());
        }

        return map(repository.save(plo));
    }

    @Override
@Transactional
public void delete(Integer id) {
        Plo plo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PLO not found"));
        repository.delete(plo);
    }

    private PloResponse map(
            Plo plo) {

        return PloResponse.builder()
                .id(plo.getId())

                .programId(
                        plo.getProgram().getId())

                .programCode(
                        plo.getProgram().getCode())

                .programName(
                        plo.getProgram().getName())

                .code(
                        plo.getCode())

                .description(
                        plo.getDescription())

                .descriptionVn(
                        plo.getDescriptionVn())

                .category(
                        plo.getCategory())

                .versionNumber(
                        plo.getVersionNumber())

                .isActive(
                        plo.getIsActive())

                .createdAt(
                        plo.getCreatedAt())

                .build();
    }
}