package com.scse.curriculum.courserelationship.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courserelationship.dto.CourseRelationshipResponse;
import com.scse.curriculum.courserelationship.dto.CreateCourseRelationshipRequest;
import com.scse.curriculum.courserelationship.entity.CourseRelationship;
import com.scse.curriculum.courserelationship.entity.RelationType;
import com.scse.curriculum.courserelationship.repository.CourseRelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseRelationshipServiceImpl implements CourseRelationshipService {

    private final CourseRelationshipRepository repository;
    private final CourseRepository courseRepository;

    @Override
    public CourseRelationshipResponse create(CreateCourseRelationshipRequest request) {
        validateRelationshipRequest(request, null);

        if (repository.existsByCourse_IdAndRelatedCourse_IdAndRelationType(
                request.getCourseId(), request.getRelatedCourseId(), request.getRelationType())) {
            throw new RuntimeException("Course relationship already exists");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Course relatedCourse = courseRepository.findById(request.getRelatedCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Related course not found"));

        CourseRelationship relationship = CourseRelationship.builder()
                .course(course)
                .relatedCourse(relatedCourse)
                .relationType(request.getRelationType())
                .build();

        return map(repository.save(relationship));
    }

    @Override
    public CourseRelationshipResponse update(Integer id, CreateCourseRelationshipRequest request) {
        CourseRelationship relationship = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course relationship not found"));

        validateRelationshipRequest(request, id);

        if (repository.existsByCourse_IdAndRelatedCourse_IdAndRelationType(
                request.getCourseId(), request.getRelatedCourseId(), request.getRelationType())
                && !(relationship.getCourse().getId().equals(request.getCourseId())
                && relationship.getRelatedCourse().getId().equals(request.getRelatedCourseId())
                && relationship.getRelationType() == request.getRelationType())) {
            throw new RuntimeException("Course relationship already exists");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Course relatedCourse = courseRepository.findById(request.getRelatedCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Related course not found"));

        relationship.setCourse(course);
        relationship.setRelatedCourse(relatedCourse);
        relationship.setRelationType(request.getRelationType());

        return map(repository.save(relationship));
    }

    @Override
    public void delete(Integer id) {
        CourseRelationship relationship = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course relationship not found"));
        repository.delete(relationship);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRelationshipResponse> getAll() {
        return repository.findAllWithCourses()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRelationshipResponse getById(Integer id) {
        return map(repository.findByIdWithCourses(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course relationship not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRelationshipResponse> getByCourseId(Integer courseId) {
        return repository.findByCourseIdWithCourses(courseId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRelationshipResponse> search(String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        return repository
                .findByCourse_CourseCodeContainingIgnoreCaseOrCourse_NameContainingIgnoreCaseOrRelatedCourse_CourseCodeContainingIgnoreCaseOrRelatedCourse_NameContainingIgnoreCase(q, q, q, q)
                .stream().map(this::map).toList();
    }

    private void validateRelationshipRequest(CreateCourseRelationshipRequest request, Integer ignoredRelationshipId) {
        if (Objects.equals(request.getCourseId(), request.getRelatedCourseId())) {
            throw new IllegalArgumentException("Không thể tạo quan hệ học phần với chính nó.");
        }

        if (request.getRelationType() == RelationType.PREREQUISITE
                && wouldCreatePrerequisiteCycle(request.getCourseId(), request.getRelatedCourseId(), ignoredRelationshipId)) {
            throw new IllegalArgumentException(
                    "Quan hệ tiên quyết này tạo vòng lặp prerequisite. Vui lòng kiểm tra lại chuỗi môn học trước khi lưu.");
        }
    }

    /**
     * Quy ước dữ liệu hiện tại: course_id là môn đang học; related_course_id là môn tiên quyết.
     * Ví dụ A cần học trước B => lưu course_id=A, related_course_id=B.
     * Khi thêm A -> B, nếu B đã đi được ngược về A trong graph prerequisite thì sẽ tạo vòng lặp.
     */
    private boolean wouldCreatePrerequisiteCycle(Integer courseId, Integer relatedCourseId, Integer ignoredRelationshipId) {
        Map<Integer, List<Integer>> graph = new HashMap<>();

        for (CourseRelationship relationship : repository.findByRelationType(RelationType.PREREQUISITE)) {
            if (ignoredRelationshipId != null && Objects.equals(relationship.getId(), ignoredRelationshipId)) {
                continue;
            }

            Integer from = relationship.getCourse().getId();
            Integer to = relationship.getRelatedCourse().getId();
            graph.computeIfAbsent(from, key -> new ArrayList<>()).add(to);
        }

        graph.computeIfAbsent(courseId, key -> new ArrayList<>()).add(relatedCourseId);

        ArrayDeque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        stack.push(relatedCourseId);

        while (!stack.isEmpty()) {
            Integer current = stack.pop();
            if (Objects.equals(current, courseId)) {
                return true;
            }

            if (!visited.add(current)) {
                continue;
            }

            for (Integer next : graph.getOrDefault(current, List.of())) {
                stack.push(next);
            }
        }

        return false;
    }

    private CourseRelationshipResponse map(CourseRelationship relationship) {
        return CourseRelationshipResponse.builder()
                .id(relationship.getId())
                .courseId(relationship.getCourse().getId())
                .courseCode(relationship.getCourse().getCourseCode())
                .courseName(relationship.getCourse().getName())
                .relatedCourseId(relationship.getRelatedCourse().getId())
                .relatedCourseCode(relationship.getRelatedCourse().getCourseCode())
                .relatedCourseName(relationship.getRelatedCourse().getName())
                .relationType(relationship.getRelationType())
                .build();
    }
}
