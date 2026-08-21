package com.scse.curriculum.student.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.student.dto.CreateStudentRequest;
import com.scse.curriculum.student.dto.StudentResponse;
import com.scse.curriculum.student.entity.Student;
import com.scse.curriculum.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private final StudentRepository repository;
    private final CohortRepository cohortRepository;

    @Override
    public StudentResponse create(CreateStudentRequest request) {
        if (repository.existsByStudentCode(request.getStudentCode())) {
            throw new RuntimeException("Student code already exists");
        }

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));

        Student student = Student.builder()
                .studentCode(request.getStudentCode())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .cohort(cohort)
            .userId(request.getUserId())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdAt(LocalDateTime.now())
                .build();

        return map(repository.save(student));
    }

    @Override
    public StudentResponse update(Integer id, CreateStudentRequest request) {
        Student student = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        repository.findByStudentCode(request.getStudentCode())
                .ifPresent(item -> {
                    if (!item.getId().equals(id)) {
                        throw new RuntimeException("Student code already exists");
                    }
                });

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));

        student.setStudentCode(request.getStudentCode());
        student.setFullName(request.getFullName());
        student.setEmail(request.getEmail());
        student.setCohort(cohort);
        student.setUserId(request.getUserId());
        student.setIsActive(request.getIsActive() == null || request.getIsActive());

        return map(repository.save(student));
    }

    @Override
    public void delete(Integer id) {
        Student student = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        repository.delete(student);
    }

    @Override
    public List<StudentResponse> getAll() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public StudentResponse getById(Integer id) {
        return map(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
    }

    @Override
    public StudentResponse getByStudentCode(String studentCode) {
        return map(repository.findByStudentCode(studentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
    }

    @Override
    public List<StudentResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return repository.findByStudentCodeContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, q)
                .stream().map(this::map).toList();
    }

    private StudentResponse map(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .email(student.getEmail())
                .cohortId(student.getCohort().getId())
                .cohortName(student.getCohort().getName())
                .userId(student.getUserId())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }
}