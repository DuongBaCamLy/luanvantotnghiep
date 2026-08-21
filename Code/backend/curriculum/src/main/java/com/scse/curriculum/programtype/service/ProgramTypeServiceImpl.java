package com.scse.curriculum.programtype.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.programtype.dto.CreateProgramTypeRequest;
import com.scse.curriculum.programtype.dto.ProgramTypeResponse;
import com.scse.curriculum.programtype.entity.ProgramType;
import com.scse.curriculum.programtype.repository.ProgramTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramTypeServiceImpl
        implements ProgramTypeService {

    private final ProgramTypeRepository repository;

    @Override
    public ProgramTypeResponse create(
            CreateProgramTypeRequest request) {

        if (repository.existsByCode(request.getCode())) {
            throw new RuntimeException(
                    "Program type code already exists");
        }

        ProgramType programType =
                ProgramType.builder()
                        .code(request.getCode())
                        .name(request.getName())
                        .build();

        return map(repository.save(programType));
    }

    @Override
    public List<ProgramTypeResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public ProgramTypeResponse getById(Integer id) {

        ProgramType programType =
                repository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Program type not found"));

        return map(programType);
    }

    @Override
    public ProgramTypeResponse getByCode(String code) {

        ProgramType programType =
                repository.findByCode(code)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Program type not found"));

        return map(programType);
    }

    private ProgramTypeResponse map(
            ProgramType programType) {

        return ProgramTypeResponse.builder()
                .id(programType.getId())
                .code(programType.getCode())
                .name(programType.getName())
                .build();
    }
}