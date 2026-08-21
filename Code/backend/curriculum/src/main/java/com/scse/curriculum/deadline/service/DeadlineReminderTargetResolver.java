package com.scse.curriculum.deadline.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeadlineReminderTargetResolver {

    private static final Set<SyllabusStatus> SUBMITTED_STATES = Set.of(
            SyllabusStatus.SUBMITTED,
            SyllabusStatus.UNDER_REVIEW,
            SyllabusStatus.APPROVED);

    private final ClassSectionRepository classSectionRepository;
    private final UserAccountRepository userAccountRepository;

    public Resolution resolve(String academicYear, Integer semester) {
        List<ClassSection> assignments = classSectionRepository
                .findActiveForDeadline(academicYear, semester);

        Map<Integer, List<ClassSection>> byInstructor = assignments.stream()
                .filter(section -> section.getInstructor() != null)
                .collect(Collectors.groupingBy(
                        section -> section.getInstructor().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        if (byInstructor.isEmpty()) {
            return new Resolution(List.of(), 0);
        }

        Map<Integer, UserAccount> usersByInstructor = userAccountRepository
                .findByInstructorIdIn(byInstructor.keySet())
                .stream()
                .filter(user -> user.getInstructorId() != null)
                .collect(Collectors.toMap(
                        UserAccount::getInstructorId,
                        Function.identity(),
                        (first, ignored) -> first));

        List<DeadlineReminderTarget> targets = new ArrayList<>();
        int skippedWithoutUserAccount = 0;

        for (Map.Entry<Integer, List<ClassSection>> entry : byInstructor.entrySet()) {
            Integer instructorId = entry.getKey();
            List<ClassSection> instructorAssignments = entry.getValue();
            UserAccount user = usersByInstructor.get(instructorId);

            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                skippedWithoutUserAccount++;
                continue;
            }

            Map<Integer, List<ClassSection>> byCourse = instructorAssignments.stream()
                    .filter(section -> section.getCourse() != null)
                    .collect(Collectors.groupingBy(
                            section -> section.getCourse().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<DeadlineReminderTarget.MissingCourse> missingCourses = byCourse.values()
                    .stream()
                    .filter(this::courseStillMissing)
                    .map(this::toMissingCourse)
                    .sorted(Comparator.comparing(
                            DeadlineReminderTarget.MissingCourse::courseCode,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (missingCourses.isEmpty()) {
                continue;
            }

            ClassSection sample = instructorAssignments.get(0);
            String instructorName = sample.getInstructor().getFullName();

            targets.add(new DeadlineReminderTarget(
                    user,
                    instructorId,
                    instructorName,
                    missingCourses));
        }

        targets.sort(Comparator.comparing(
                target -> target.instructorName() == null
                        ? target.user().getUsername()
                        : target.instructorName(),
                String.CASE_INSENSITIVE_ORDER));

        return new Resolution(List.copyOf(targets), skippedWithoutUserAccount);
    }

    private boolean courseStillMissing(Collection<ClassSection> courseAssignments) {
        return courseAssignments.stream()
                .map(ClassSection::getSyllabus)
                .noneMatch(this::hasBeenSubmitted);
    }

    private boolean hasBeenSubmitted(Syllabus syllabus) {
        return syllabus != null
                && syllabus.getStatus() != null
                && SUBMITTED_STATES.contains(syllabus.getStatus());
    }

    private DeadlineReminderTarget.MissingCourse toMissingCourse(
            List<ClassSection> courseAssignments) {
        ClassSection sample = courseAssignments.get(0);
        return new DeadlineReminderTarget.MissingCourse(
                sample.getCourse().getId(),
                sample.getCourse().getCourseCode(),
                sample.getCourse().getName());
    }

    public record Resolution(
            List<DeadlineReminderTarget> targets,
            int skippedWithoutUserAccount) {
    }
}
