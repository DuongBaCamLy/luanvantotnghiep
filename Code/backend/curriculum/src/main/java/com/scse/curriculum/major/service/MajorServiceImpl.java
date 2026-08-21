package com.scse.curriculum.major.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.major.dto.CreateMajorRequest;
import com.scse.curriculum.major.dto.MajorResponse;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MajorServiceImpl
        implements MajorService {

    private final MajorRepository repository;

    @Override
    public MajorResponse create(
            CreateMajorRequest request) {

        if (repository.existsByCode(request.getCode())) {
            throw new RuntimeException(
                    "Major code already exists");
        }

        Major major = Major.builder()
                .code(request.getCode())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .build();

        return map(
                repository.save(major));
    }

    @Override
    public List<MajorResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public MajorResponse getById(Integer id) {

        Major major = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Major not found"));

        return map(major);
    }

    @Override
    public MajorResponse getByCode(String code) {

        Major major = repository.findByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Major not found"));

        return map(major);
    }

    private MajorResponse map(
            Major major) {

        return MajorResponse.builder()
                .id(major.getId())
                .code(major.getCode())
                .name(major.getName())
                .nameVn(major.getNameVn())
                .build();
    }
}