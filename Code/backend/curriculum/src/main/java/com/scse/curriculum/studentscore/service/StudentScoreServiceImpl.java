package com.scse.curriculum.studentscore.service;

import com.scse.curriculum.assessment.entity.AssessmentComponent;
import com.scse.curriculum.assessment.repository.AssessmentComponentRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.enrollment.entity.Enrollment;
import com.scse.curriculum.enrollment.repository.EnrollmentRepository;
import com.scse.curriculum.studentscore.dto.CreateStudentScoreRequest;
import com.scse.curriculum.studentscore.dto.StudentScoreResponse;
import com.scse.curriculum.studentscore.entity.StudentScore;
import com.scse.curriculum.studentscore.repository.StudentScoreRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentScoreServiceImpl implements StudentScoreService {

    private final StudentScoreRepository repository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public StudentScoreResponse create(CreateStudentScoreRequest request) {
        if (repository.existsByEnrollment_IdAndAssessmentComponent_Id(request.getEnrollmentId(), request.getAssessmentComponentId())) {
            throw new RuntimeException("Student score already exists");
        }

        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        AssessmentComponent component = assessmentComponentRepository.findById(request.getAssessmentComponentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment component not found"));

        UserAccount recordedBy = null;
        if (request.getRecordedById() != null) {
            recordedBy = userAccountRepository.findById(request.getRecordedById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        StudentScore score = StudentScore.builder()
                .enrollment(enrollment)
                .assessmentComponent(component)
                .rawScore(request.getRawScore())
                .finalScore(request.getFinalScore())
                .isAbsent(request.getIsAbsent() != null && request.getIsAbsent())
                .remark(request.getRemark())
                .recordedAt(LocalDateTime.now())
                .recordedBy(recordedBy)
                .build();

        return map(repository.save(score));
    }

    @Override
    public StudentScoreResponse update(Integer id, CreateStudentScoreRequest request) {
        StudentScore score = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student score not found"));

        if (repository.existsByEnrollment_IdAndAssessmentComponent_Id(request.getEnrollmentId(), request.getAssessmentComponentId())
                && !(score.getEnrollment().getId().equals(request.getEnrollmentId())
                && score.getAssessmentComponent().getId().equals(request.getAssessmentComponentId()))) {
            throw new RuntimeException("Student score already exists");
        }

        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        AssessmentComponent component = assessmentComponentRepository.findById(request.getAssessmentComponentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment component not found"));

        UserAccount recordedBy = null;
        if (request.getRecordedById() != null) {
            recordedBy = userAccountRepository.findById(request.getRecordedById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        score.setEnrollment(enrollment);
        score.setAssessmentComponent(component);
        score.setRawScore(request.getRawScore());
        score.setFinalScore(request.getFinalScore());
        score.setIsAbsent(request.getIsAbsent() != null && request.getIsAbsent());
        score.setRemark(request.getRemark());
        score.setRecordedBy(recordedBy);

        return map(repository.save(score));
    }

    @Override
    public void delete(Integer id) {
        StudentScore score = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student score not found"));
        repository.delete(score);
    }

    @Override
    public List<StudentScoreResponse> getAll() {
        return repository.findAll().stream().map(this::map).toList();
    }

    @Override
    public StudentScoreResponse getById(Integer id) {
        return map(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student score not found")));
    }

    @Override
    public List<StudentScoreResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return repository.findByEnrollment_Student_StudentCodeContainingIgnoreCaseOrEnrollment_Student_FullNameContainingIgnoreCaseOrAssessmentComponent_NameContainingIgnoreCase(q, q, q)
                .stream().map(this::map).toList();
    }

    private StudentScoreResponse map(StudentScore score) {
        return StudentScoreResponse.builder()
                .id(score.getId())
                .enrollmentId(score.getEnrollment().getId())
                .assessmentComponentId(score.getAssessmentComponent().getId())
                .assessmentComponentName(score.getAssessmentComponent().getName())
                .rawScore(score.getRawScore())
                .finalScore(score.getFinalScore())
                .isAbsent(score.getIsAbsent())
                .remark(score.getRemark())
                .recordedAt(score.getRecordedAt())
                .recordedById(score.getRecordedBy() == null ? null : score.getRecordedBy().getId())
                .build();
    }
}