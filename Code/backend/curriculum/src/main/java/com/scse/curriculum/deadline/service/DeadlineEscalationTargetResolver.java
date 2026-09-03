package com.scse.curriculum.deadline.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.classsection.entity.ClassSection;
import com.scse.curriculum.classsection.repository.ClassSectionRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.instructor.entity.Instructor;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.user.repository.UserAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeadlineEscalationTargetResolver {

    private static final Set<SyllabusStatus> SUBMITTED_STATES = Set.of(
            SyllabusStatus.SUBMITTED,
            SyllabusStatus.UNDER_REVIEW,
            SyllabusStatus.APPROVED);

    private final ClassSectionRepository classSectionRepository;
    private final UserAccountRepository userAccountRepository;

    public Resolution resolve(String academicYear, Integer semester) {
        List<ClassSection> assignments = classSectionRepository
                .findActiveForDeadline(academicYear, semester);

        List<DeadlineEscalationTarget.OverdueInstructor> overdue =
                resolveOverdueInstructors(assignments);

        if (overdue.isEmpty()) {
            return new Resolution(
                    List.of(),
                    List.of(),
                    0,
                    false,
                    0,
                    0);
        }

        Map<Integer, List<DeadlineEscalationTarget.OverdueInstructor>> byMajor =
                overdue.stream()
                        .filter(item -> item.departmentId() != null)
                        .collect(Collectors.groupingBy(
                                DeadlineEscalationTarget.OverdueInstructor::departmentId,
                                LinkedHashMap::new,
                                Collectors.toList()));

        List<DepartmentResolution> departments = new ArrayList<>();
        List<DeadlineEscalationTarget> recipients = new ArrayList<>();
        int departmentsWithoutHead = 0;

        for (Map.Entry<Integer, List<DeadlineEscalationTarget.OverdueInstructor>> entry
                : byMajor.entrySet()) {
            Integer majorId = entry.getKey();
            List<DeadlineEscalationTarget.OverdueInstructor> items = sortInstructors(entry.getValue());
            DeadlineEscalationTarget.OverdueInstructor sample = items.getFirst();

            List<UserAccount> heads = userAccountRepository
                    .findByRoleAndManagedMajor_IdAndIsActiveTrue(
                            UserRole.DEPT_HEAD, majorId);
            heads = uniqueActiveUsers(heads);

            if (heads.isEmpty()) {
                departmentsWithoutHead++;
            }

            departments.add(new DepartmentResolution(
                    majorId,
                    sample.departmentCode(),
                    sample.departmentName(),
                    items,
                    heads));

            for (UserAccount head : heads) {
                recipients.add(new DeadlineEscalationTarget(
                        head,
                        UserRole.DEPT_HEAD,
                        "MAJOR:" + majorId,
                        majorId,
                        sample.departmentCode(),
                        sample.departmentName(),
                        items));
            }
        }

        List<UserAccount> deans = uniqueActiveUsers(
                userAccountRepository.findByRoleAndIsActiveTrue(UserRole.DEAN));
        boolean missingDean = deans.isEmpty();

        List<DeadlineEscalationTarget.OverdueInstructor> allItems = sortInstructors(overdue);
        for (UserAccount dean : deans) {
            recipients.add(new DeadlineEscalationTarget(
                    dean,
                    UserRole.DEAN,
                    "FACULTY",
                    null,
                    null,
                    "School of Computer Science and Engineering",
                    allItems));
        }

        recipients.sort(Comparator
                .comparing((DeadlineEscalationTarget target) ->
                        target.recipientRole() == UserRole.DEPT_HEAD ? 0 : 1)
                .thenComparing(target -> target.departmentCode() == null
                        ? ""
                        : target.departmentCode(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(target -> target.recipient().getUsername(),
                        String.CASE_INSENSITIVE_ORDER));

        departments.sort(Comparator.comparing(
                item -> item.departmentCode() == null ? "" : item.departmentCode(),
                String.CASE_INSENSITIVE_ORDER));

        int missingCourseCount = allItems.stream()
                .mapToInt(item -> item.missingCourses().size())
                .sum();

        return new Resolution(
                List.copyOf(recipients),
                List.copyOf(departments),
                departmentsWithoutHead,
                missingDean,
                allItems.size(),
                missingCourseCount);
    }

    private List<DeadlineEscalationTarget.OverdueInstructor> resolveOverdueInstructors(
            List<ClassSection> assignments) {
        Map<InstructorMajorKey, List<ClassSection>> grouped = assignments.stream()
                .filter(section -> section.getInstructor() != null)
                .filter(section -> section.getCourse() != null)
                .collect(Collectors.groupingBy(
                        section -> new InstructorMajorKey(
                                section.getInstructor().getId(),
                                majorId(section)),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<DeadlineEscalationTarget.OverdueInstructor> result = new ArrayList<>();

        for (List<ClassSection> instructorDepartmentAssignments : grouped.values()) {
            ClassSection sampleSection = instructorDepartmentAssignments.getFirst();
            Instructor instructor = sampleSection.getInstructor();
            Major major = sampleSection.getProgram() == null
                    ? null : sampleSection.getProgram().getMajor();

            Map<Integer, List<ClassSection>> byCourse = instructorDepartmentAssignments.stream()
                    .collect(Collectors.groupingBy(
                            section -> section.getCourse().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<DeadlineEscalationTarget.MissingCourse> missingCourses = byCourse.values()
                    .stream()
                    .filter(this::courseStillMissing)
                    .map(this::toMissingCourse)
                    .sorted(Comparator.comparing(
                            DeadlineEscalationTarget.MissingCourse::courseCode,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (missingCourses.isEmpty()) {
                continue;
            }

            result.add(new DeadlineEscalationTarget.OverdueInstructor(
                    instructor.getId(),
                    instructor.getFullName(),
                    instructor.getEmail(),
                    major == null ? null : major.getId(),
                    major == null ? "UNASSIGNED" : major.getCode(),
                    major == null ? "Chưa gán Major" : major.getName(),
                    missingCourses));
        }

        return sortInstructors(result);
    }

    private boolean courseStillMissing(List<ClassSection> courseAssignments) {
        return courseAssignments.stream()
                .map(ClassSection::getSyllabus)
                .noneMatch(this::hasBeenSubmitted);
    }

    private boolean hasBeenSubmitted(Syllabus syllabus) {
        return syllabus != null
                && syllabus.getStatus() != null
                && SUBMITTED_STATES.contains(syllabus.getStatus());
    }

    private DeadlineEscalationTarget.MissingCourse toMissingCourse(
            List<ClassSection> courseAssignments) {
        Course course = courseAssignments.getFirst().getCourse();
        return new DeadlineEscalationTarget.MissingCourse(
                course.getId(),
                course.getCourseCode(),
                course.getName());
    }

    private Integer majorId(ClassSection section) {
        return section.getProgram() == null || section.getProgram().getMajor() == null
                ? null : section.getProgram().getMajor().getId();
    }

    private List<DeadlineEscalationTarget.OverdueInstructor> sortInstructors(
            List<DeadlineEscalationTarget.OverdueInstructor> items) {
        return items.stream()
                .sorted(Comparator
                        .comparing((DeadlineEscalationTarget.OverdueInstructor item) ->
                                item.departmentCode() == null ? "" : item.departmentCode(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> item.instructorName() == null
                                ? ""
                                : item.instructorName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<UserAccount> uniqueActiveUsers(List<UserAccount> users) {
        Map<String, UserAccount> unique = new LinkedHashMap<>();
        if (users == null) {
            return List.of();
        }
        for (UserAccount user : users) {
            if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
                continue;
            }
            String key = user.getEmail() == null || user.getEmail().isBlank()
                    ? "id:" + user.getId()
                    : "email:" + user.getEmail().trim().toLowerCase();
            unique.putIfAbsent(key, user);
        }
        return List.copyOf(unique.values());
    }

    private record InstructorMajorKey(
            Integer instructorId,
            Integer majorId) {
    }

    public record DepartmentResolution(
            Integer departmentId,
            String departmentCode,
            String departmentName,
            List<DeadlineEscalationTarget.OverdueInstructor> overdueInstructors,
            List<UserAccount> departmentHeads) {

        public int missingCourseCount() {
            return overdueInstructors.stream()
                    .mapToInt(item -> item.missingCourses().size())
                    .sum();
        }
    }

    public record Resolution(
            List<DeadlineEscalationTarget> recipients,
            List<DepartmentResolution> departments,
            int departmentsWithoutHead,
            boolean missingDean,
            int overdueInstructorCount,
            int missingCourseCount) {
    }
}
