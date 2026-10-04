package com.scse.curriculum.deadline.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeadlineReminderTargetResolver {

    private static final Set<SyllabusStatus> SUBMITTED_STATES =
            Set.of(
                    SyllabusStatus.SUBMITTED,
                    SyllabusStatus.UNDER_REVIEW,
                    SyllabusStatus.APPROVED);

    private final ClassSectionRepository classSectionRepository;

    public Resolution resolve(
            String academicYear,
            Integer semester) {

        List<ClassSection> assignments =
                classSectionRepository
                        .findActiveForDeadline(
                                academicYear,
                                semester);

        /*
         * Teaching Assignment now points directly to UserAccount.
         *
         * Key = UserAccount.id
         */
        Map<Integer, List<ClassSection>> byInstructorUser =
                assignments
                        .stream()
                        .filter(section ->
                                section.getInstructorUser()
                                        != null)
                        .collect(
                                Collectors.groupingBy(
                                        section ->
                                                section
                                                        .getInstructorUser()
                                                        .getId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        if (byInstructorUser.isEmpty()) {
            return new Resolution(
                    List.of(),
                    0);
        }

        List<DeadlineReminderTarget> targets =
                new ArrayList<>();

        int skippedWithoutUserAccount = 0;

        for (Map.Entry<Integer, List<ClassSection>> entry :
                byInstructorUser.entrySet()) {

            Integer instructorUserId =
                    entry.getKey();

            List<ClassSection> instructorAssignments =
                    entry.getValue();

            UserAccount user =
                    instructorAssignments
                            .get(0)
                            .getInstructorUser();

            /*
             * Defensive validation.
             *
             * Assignment recipients must remain active
             * Instructor accounts.
             */
            if (user == null
                    || user.getId() == null
                    || user.getRole()
                            != UserRole.INSTRUCTOR
                    || !Boolean.TRUE.equals(
                            user.getIsActive())) {

                skippedWithoutUserAccount++;
                continue;
            }

            Map<Integer, List<ClassSection>> byCourse =
                    instructorAssignments
                            .stream()
                            .filter(section ->
                                    section.getCourse()
                                            != null)
                            .collect(
                                    Collectors.groupingBy(
                                            section ->
                                                    section
                                                            .getCourse()
                                                            .getId(),
                                            LinkedHashMap::new,
                                            Collectors.toList()));

            List<DeadlineReminderTarget.MissingCourse>
                    missingCourses =
                    byCourse
                            .values()
                            .stream()
                            .filter(
                                    this::courseStillMissing)
                            .map(
                                    this::toMissingCourse)
                            .sorted(
                                    Comparator.comparing(
                                            DeadlineReminderTarget
                                                    .MissingCourse
                                                    ::courseCode,
                                            String.CASE_INSENSITIVE_ORDER))
                            .toList();

            if (missingCourses.isEmpty()) {
                continue;
            }

            String instructorName =
                    user.getFullName();

            /*
             * Temporary compatibility:
             * DeadlineReminderTarget still calls this field
             * instructorId.
             *
             * It now receives UserAccount.id.
             * We will rename that record field next.
             */
            targets.add(
                    new DeadlineReminderTarget(
                            user,
                            instructorUserId,
                            instructorName,
                            missingCourses));
        }

        targets.sort(
                Comparator.comparing(
                        target ->
                                target.instructorName() == null
                                        || target.instructorName()
                                                .isBlank()
                                        ? target.user()
                                                .getUsername()
                                        : target.instructorName(),
                        String.CASE_INSENSITIVE_ORDER));

        return new Resolution(
                List.copyOf(targets),
                skippedWithoutUserAccount);
    }

    private boolean courseStillMissing(
            Collection<ClassSection> courseAssignments) {

        return courseAssignments
                .stream()
                .map(ClassSection::getSyllabus)
                .noneMatch(this::hasBeenSubmitted);
    }

    private boolean hasBeenSubmitted(
            Syllabus syllabus) {

        return syllabus != null
                && syllabus.getStatus() != null
                && SUBMITTED_STATES.contains(
                        syllabus.getStatus());
    }

    private DeadlineReminderTarget.MissingCourse
    toMissingCourse(
            List<ClassSection> courseAssignments) {

        ClassSection sample =
                courseAssignments.get(0);

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