package com.scse.curriculum.instructor.service;

import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.instructor.dto.InstructorRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.mapper.InstructorMapper;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final InstructorMapper instructorMapper;
    private final UserAccountRepository userAccountRepository;
    private final ClassSectionRepository classSectionRepository;

    /**
     * Instructor Management owns staff profiles.
     * User Management owns login accounts and SRS roles.
     *
     * We still expose linked-account metadata here so Admin can immediately
     * see whether an instructor profile is connected to a system account.
     */
    private void populateExtraFields(
            InstructorResponse response,
            Integer instructorId) {

        Optional<UserAccount> userOpt =
                userAccountRepository.findByInstructorId(instructorId);

        if (userOpt.isEmpty() && response.getEmail() != null) {
            userOpt = userAccountRepository.findByEmail(response.getEmail());
        }

        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();

            response.setUserAccountId(user.getId());
            response.setUsername(user.getUsername());
            response.setAccountActive(user.getIsActive());
            response.setRole(user.getRole());
        } else {
            response.setUserAccountId(null);
            response.setUsername(null);
            response.setAccountActive(null);
            response.setRole(null);
        }

        Integer count =
                classSectionRepository
                        .countDistinctCoursesByInstructorId(instructorId);

        response.setCourseCount(
                count != null ? count : 0);
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

        /*
         * IMPORTANT:
         * Do not auto-create a login account here.
         *
         * FR-01 account/role administration belongs to User Management.
         * After creating the profile, Admin can create/link the account from
         * User Management. This also removes the previous insecure default
         * password ("admin123") and prevents hidden role/account changes.
         */

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateExtraFields(
                response,
                instructor.getId());

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

        /*
         * Do not silently change the linked login account here.
         * Role, account email, password and login activation are managed from
         * User Management. The two statuses are intentionally independent.
         */

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateExtraFields(
                response,
                id);

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

                    populateExtraFields(
                            response,
                            instructor.getId());

                    return response;
                })
                .collect(
                        Collectors.toList());
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
         * Legacy hard-delete endpoint kept for backward compatibility.
         * The Admin UI uses profile activation/deactivation instead.
         */
        userAccountRepository
                .findByInstructorId(id)
                .ifPresent(user -> {
                    user.setInstructorId(null);
                    userAccountRepository.save(user);
                });

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

        populateExtraFields(
                response,
                id);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponse getInstructorByUserId(
            Integer userId) {

        UserAccount userAccount =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id: "
                                                + userId));

        Integer instructorId =
                userAccount.getInstructorId();

        if (instructorId == null) {
            throw new ResourceNotFoundException(
                    "User is not linked to an instructor");
        }

        Instructor instructor =
                instructorRepository
                        .findById(instructorId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Instructor not found with id: "
                                                + instructorId));

        InstructorResponse response =
                instructorMapper.toResponse(instructor);

        populateExtraFields(
                response,
                instructorId);

        return response;
    }
}