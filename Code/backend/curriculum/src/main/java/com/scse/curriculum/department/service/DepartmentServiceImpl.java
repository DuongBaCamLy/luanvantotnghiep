package com.scse.curriculum.department.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.department.dto.CreateDepartmentRequest;
import com.scse.curriculum.department.dto.DepartmentResponse;
import com.scse.curriculum.department.dto.DepartmentHeadCandidateResponse;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.instructor.repository.InstructorRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;
    private final UserAccountRepository userAccountRepository;
    private final InstructorRepository instructorRepository;

    @Override
    public DepartmentResponse create(
            CreateDepartmentRequest request) {

        if (repository.existsByCode(request.getCode())) {
            throw new ResourceNotFoundException("Department code already exists");
        }

        Department department = Department.builder()
                .code(request.getCode())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        department = repository.save(department);

        return map(department);
    }

    @Override
    public List<DepartmentResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public DepartmentResponse getById(Integer id) {

        Department department = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        return map(department);
    }

    @Override
    public DepartmentResponse getByCode(String code) {

        Department department = repository.findByCode(code)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Department not found"));

        return map(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentHeadCandidateResponse> getActiveHeadCandidates() {
        return userAccountRepository.findByRoleAndIsActiveTrue(UserRole.DEPT_HEAD)
                .stream()
                .map(this::mapCandidate)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse assignHead(Integer departmentId, Integer userAccountId) {
        Department department = repository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        if (!Boolean.TRUE.equals(department.getIsActive())) {
            throw new IllegalStateException("An inactive department cannot receive a Head of Department.");
        }

        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (account.getRole() != UserRole.DEPT_HEAD || !Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalArgumentException(
                    "Only an active DEPT_HEAD account may be assigned as Head of Department.");
        }
        if (account.getInstructorId() == null) {
            throw new IllegalArgumentException(
                    "The selected DEPT_HEAD account must be linked to an instructor profile.");
        }

        Instructor instructor = instructorRepository.findById(account.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Linked instructor profile not found"));
        if (!Boolean.TRUE.equals(instructor.getIsActive())) {
            throw new IllegalArgumentException(
                    "The selected DEPT_HEAD account is linked to an inactive instructor profile.");
        }

        instructor.setDepartment(department);
        instructorRepository.save(instructor);
        return map(department);
    }

    private DepartmentResponse map(Department department) {

        UserAccount head = userAccountRepository
                .findActiveByRoleAndInstructorDepartmentId(UserRole.DEPT_HEAD, department.getId())
                .stream()
                .findFirst()
                .orElse(null);
        Instructor headProfile = head == null || head.getInstructorId() == null
                ? null
                : instructorRepository.findById(head.getInstructorId()).orElse(null);

        return DepartmentResponse.builder()
                .id(department.getId())
                .code(department.getCode())
                .name(department.getName())
                .nameVn(department.getNameVn())
                .isActive(department.getIsActive())
                .headUserId(head == null ? null : head.getId())
                .headUsername(head == null ? null : head.getUsername())
                .headInstructorId(headProfile == null ? null : headProfile.getId())
                .headFullName(headProfile == null ? null : headProfile.getFullName())
                .createdAt(department.getCreatedAt())
                .build();
    }

    private DepartmentHeadCandidateResponse mapCandidate(UserAccount account) {
        Instructor instructor = account.getInstructorId() == null
                ? null
                : instructorRepository.findById(account.getInstructorId()).orElse(null);
        return DepartmentHeadCandidateResponse.builder()
                .userAccountId(account.getId())
                .username(account.getUsername())
                .instructorId(instructor == null ? null : instructor.getId())
                .staffCode(instructor == null ? null : instructor.getStaffCode())
                .fullName(instructor == null ? null : instructor.getFullName())
                .departmentId(instructor == null || instructor.getDepartment() == null
                        ? null : instructor.getDepartment().getId())
                .departmentName(instructor == null || instructor.getDepartment() == null
                        ? null : instructor.getDepartment().getName())
                .build();
    }
}
