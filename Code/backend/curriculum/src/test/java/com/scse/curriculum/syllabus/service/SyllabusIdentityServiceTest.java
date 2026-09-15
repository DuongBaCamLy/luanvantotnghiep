package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyllabusIdentityServiceTest {
    @Mock CourseRepository courses;
    @Mock SyllabusRepository syllabuses;
    @InjectMocks SyllabusIdentityService service;
    Course course = Course.builder().id(53).courseCode("IT116IU").build();

    @Test void rejectsDuplicateEvenWithDifferentRevisionNumber() {
        when(courses.lockIdentity(53)).thenReturn(Optional.of(course));
        when(syllabuses.existsLogicalIdentity(53, "CS-2021", "CS2021", "Semester 2", null)).thenReturn(true);
        assertThatThrownBy(() -> service.assertAvailable(course, "CS-2021", "CS2021", "Semester 2", null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("already exists");
    }
    @Test void allowsDifferentCohortAndSemester() {
        when(courses.lockIdentity(53)).thenReturn(Optional.of(course));
        service.assertAvailable(course, "CS-2021", "CS2026", "Semester 2", null);
        service.assertAvailable(course, "CS-2021", "CS2026", "Semester 3", null);
        verify(syllabuses).existsLogicalIdentity(53, "CS-2021", "CS2026", "Semester 2", null);
        verify(syllabuses).existsLogicalIdentity(53, "CS-2021", "CS2026", "Semester 3", null);
    }
    @Test void excludesSameRowDuringWorkflow() {
        when(courses.lockIdentity(53)).thenReturn(Optional.of(course));
        service.assertAvailable(course, "CS-2021", "CS2026", "Semester 2", 3013);
        verify(syllabuses).existsLogicalIdentity(53, "CS-2021", "CS2026", "Semester 2", 3013);
    }
    @Test void rejectsLegacyIdentityText() {
        assertThatThrownBy(() -> service.assertAvailable(course,"CS-2021","2026-2027","HK1",null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(courses, syllabuses);
    }
    @Test void unscheduledElectiveStillCannotHaveDuplicate() {
        when(courses.lockIdentity(53)).thenReturn(Optional.of(course));
        when(syllabuses.existsLogicalIdentity(53, "CS-2021", "CS2026", null, null)).thenReturn(true);
        assertThatThrownBy(() -> service.assertAvailable(course,"CS-2021","CS2026",null,null))
                .isInstanceOf(IllegalStateException.class);
    }
}
