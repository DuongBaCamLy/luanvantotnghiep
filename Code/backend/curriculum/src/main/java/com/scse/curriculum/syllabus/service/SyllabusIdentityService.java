package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyllabusIdentityService {
    private final CourseRepository courses;
    private final SyllabusRepository syllabuses;

    @Transactional
    public void assertAvailable(Course course, String program, String cohort, String semester, Integer excludeId) {
        if (program == null || program.isBlank() || cohort == null || !cohort.matches("[A-Za-z]+[0-9]{4}")
                || (semester != null && !semester.matches("Semester [1-8]"))) {
            throw new IllegalArgumentException("Select a canonical Program, Cohort and curriculum semester.");
        }
        // Serializes creates/imports/clones and identity changes for this course.
        courses.lockIdentity(course.getId()).orElseThrow();
        if (syllabuses.existsLogicalIdentity(course.getId(), program, cohort, semester, excludeId)) {
            throw new IllegalStateException("A syllabus already exists for this course, program, cohort and semester. Open that syllabus instead.");
        }
    }
}
