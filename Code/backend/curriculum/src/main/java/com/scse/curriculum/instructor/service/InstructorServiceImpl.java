package com.scse.curriculum.instructor.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.instructor.dto.InstructorRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.mapper.InstructorMapper;
import com.scse.curriculum.instructor.repository.InstructorRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorMapper instructorMapper;

    /**
     * Instructor Profile is now independent from UserAccount and
     * Teaching Assignment identity.
     *
     * These response fields are kept temporarily only for DTO compatibility.
     * There is no longer any account/profile linkage.
     */
    private void populateCompatibilityFields(
            InstructorResponse response) {

        response.setUserAccountId(null);
        response.setUsername(null);
        response.setAccountActive(null);
        response.setRole(null);

        /*
         * Course assignments are now owned by UserAccount through
         * ClassSection.instructorUser. They cannot be counted safely from
         * an independent Instructor Profile.
         */
        response.setCourseCount(0);
    }

    @Override
    public InstructorResponse createInstructor(
            InstructorRequest request) {

        if (instructorRepository.existsByStaffCode(
                request.getStaffCode())) {

            throw new IllegalArgumentException(
                    "Staff code already exists.");
        }

        if (instructorRepository.existsByEmail(
                request.getEmail())) {

            throw new IllegalArgumentException(
                    "Email already exists.");
        }

        Instructor instructor =
                instructorMapper.toEntity(request);

        instructor =
                instructorRepository.save(instructor);

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateCompatibilityFields(response);

        return response;
    }

    @Override
    public InstructorResponse updateInstructor(
            Integer id,
            InstructorRequest request) {

        Instructor instructor =
                instructorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Instructor not found with id: "
                                                + id));

        instructorRepository
                .findByStaffCode(
                        request.getStaffCode())
                .ifPresent(item -> {

                    if (!item.getId().equals(id)) {
                        throw new IllegalArgumentException(
                                "Staff code already exists.");
                    }
                });

        instructorRepository
                .findByEmail(
                        request.getEmail())
                .ifPresent(item -> {

                    if (!item.getId().equals(id)) {
                        throw new IllegalArgumentException(
                                "Email already exists.");
                    }
                });

        instructorMapper.updateEntity(
                instructor,
                request);

        instructor =
                instructorRepository.save(instructor);

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateCompatibilityFields(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorResponse> getAllInstructors() {

        return instructorRepository
                .findAll()
                .stream()
                .map(instructor -> {

                    InstructorResponse response =
                            instructorMapper
                                    .toResponse(instructor);

                    populateCompatibilityFields(response);

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteInstructor(
            Integer id) {

        Instructor instructor =
                instructorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Instructor not found with id: "
                                                + id));

        /*
         * No UserAccount unlinking is performed here.
         * Instructor Profile and UserAccount are independent.
         */
        instructorRepository.delete(instructor);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponse getInstructorById(
            Integer id) {

        Instructor instructor =
                instructorRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Instructor not found with id: "
                                                + id));

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateCompatibilityFields(response);

        return response;
    }
}
