package com.scse.curriculum.enrollment.service;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.enrollment.dto.CreateEnrollmentRequest;
import com.scse.curriculum.enrollment.dto.EnrollmentResponse;
import com.scse.curriculum.enrollment.entity.Enrollment;
import com.scse.curriculum.enrollment.entity.EnrollmentStatus;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.student.entity.Student;
import com.scse.curriculum.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository repository;
    private final StudentRepository studentRepository;
    private final ClassSectionRepository classSectionRepository;

    @Override
    public EnrollmentResponse create(CreateEnrollmentRequest request) {
        if (repository.existsByStudent_IdAndClassSection_Id(request.getStudentId(), request.getClassSectionId())) {
            throw new RuntimeException("Enrollment already exists");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .classSection(classSection)
                .enrolledAt(request.getEnrolledAt() == null ? LocalDate.now() : request.getEnrolledAt())
                .status(request.getStatus() == null ? EnrollmentStatus.ENROLLED : request.getStatus())
                .build();

        return map(repository.save(enrollment));
    }

    @Override
    public EnrollmentResponse update(Integer id, CreateEnrollmentRequest request) {
        Enrollment enrollment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (repository.existsByStudent_IdAndClassSection_Id(request.getStudentId(), request.getClassSectionId())
                && !(enrollment.getStudent().getId().equals(request.getStudentId())
                && enrollment.getClassSection().getId().equals(request.getClassSectionId()))) {
            throw new RuntimeException("Enrollment already exists");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        ClassSection classSection = classSectionRepository.findById(request.getClassSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class section not found"));

        enrollment.setStudent(student);
        enrollment.setClassSection(classSection);
        enrollment.setEnrolledAt(request.getEnrolledAt() == null ? enrollment.getEnrolledAt() : request.getEnrolledAt());
        enrollment.setStatus(request.getStatus() == null ? enrollment.getStatus() : request.getStatus());

        return map(repository.save(enrollment));
    }

    @Override
    public void delete(Integer id) {
        Enrollment enrollment = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        repository.delete(enrollment);
    }

    @Override
    public List<EnrollmentResponse> getAll() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public EnrollmentResponse getById(Integer id) {
        return map(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found")));
    }

    @Override
    public List<EnrollmentResponse> getByStudentId(Integer studentId) {
        return repository.findByStudent_Id(studentId).stream().map(this::map).toList();
    }

    @Override
    public List<EnrollmentResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return repository.findByStudent_StudentCodeContainingIgnoreCaseOrStudent_FullNameContainingIgnoreCaseOrClassSection_Course_CourseCodeContainingIgnoreCaseOrClassSection_Course_NameContainingIgnoreCase(q, q, q, q)
                .stream().map(this::map).toList();
    }

    private EnrollmentResponse map(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentCode(enrollment.getStudent().getStudentCode())
                .studentName(enrollment.getStudent().getFullName())
                .classSectionId(enrollment.getClassSection().getId())
                .courseCode(enrollment.getClassSection().getCourse().getCourseCode())
                .courseName(enrollment.getClassSection().getCourse().getName())
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus())
                .build();
    }
}