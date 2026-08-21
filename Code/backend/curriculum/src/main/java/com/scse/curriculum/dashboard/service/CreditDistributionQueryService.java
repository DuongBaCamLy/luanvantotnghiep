package com.scse.curriculum.dashboard.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.dashboard.dto.CreditDistributionResponse;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditDistributionQueryService {

    private static final String UNCLASSIFIED_CODE = "UNCLASSIFIED";

    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final CourseProgramRepository courseProgramRepository;

    public CreditDistributionResponse getDistribution(Integer programId, Integer cohortId) {
        if (programId == null) {
            throw new IllegalArgumentException("programId is required");
        }
        if (cohortId == null) {
            throw new IllegalArgumentException("cohortId is required");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        if (cohort.getProgram() == null || !programId.equals(cohort.getProgram().getId())) {
            throw new IllegalArgumentException("Cohort does not belong to the selected program");
        }

        List<CourseProgram> rawRows = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(programId, cohortId);

        // General rows are loaded first; a cohort-specific row replaces the general row.
        // Same-scope duplicates are resolved deterministically by the lowest CourseProgram id.
        List<CourseProgram> ordered = rawRows.stream()
                .sorted(Comparator
                        .comparing((CourseProgram cp) -> cp.getCohort() != null)
                        .thenComparing(cp -> cp.getId() == null ? Integer.MAX_VALUE : cp.getId()))
                .toList();

        Map<Integer, CourseProgram> uniqueByCourse = new LinkedHashMap<>();
        for (CourseProgram candidate : ordered) {
            if (candidate.getCourse() == null || candidate.getCourse().getId() == null) {
                continue;
            }
            Integer courseId = candidate.getCourse().getId();
            CourseProgram current = uniqueByCourse.get(courseId);
            if (current == null || shouldReplace(current, candidate)) {
                uniqueByCourse.put(courseId, candidate);
            }
        }

        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        int totalCredits = 0;
        for (CourseProgram cp : uniqueByCourse.values()) {
            Course course = cp.getCourse();
            int credits = nonNegative(course.getCreditTheory()) + nonNegative(course.getCreditLab());
            totalCredits += credits;

            GroupIdentity identity = resolveGroup(cp);
            MutableGroup group = groups.computeIfAbsent(identity.key(), ignored -> new MutableGroup(identity));
            group.credits += credits;
            group.courseCount += 1;
        }

        final int calculatedTotal = totalCredits;
        List<CreditDistributionResponse.CreditGroup> resultGroups = groups.values().stream()
                .sorted(Comparator.comparing(group -> group.identity.code()))
                .map(group -> CreditDistributionResponse.CreditGroup.builder()
                        .courseTypeId(group.identity.courseTypeId())
                        .code(group.identity.code())
                        .name(group.identity.name())
                        .nameVn(group.identity.nameVn())
                        .credits(group.credits)
                        .courseCount(group.courseCount)
                        .percentage(calculatedTotal == 0 ? 0D : round2(group.credits * 100D / calculatedTotal))
                        .build())
                .toList();

        Integer declared = program.getTotalCredits();
        int difference = declared == null ? 0 : calculatedTotal - declared;
        List<CreditDistributionResponse.DataWarning> warnings = new ArrayList<>();
        if (declared != null && difference != 0) {
            warnings.add(CreditDistributionResponse.DataWarning.builder()
                    .code("PROGRAM_CREDIT_MISMATCH")
                    .message("Tổng tín chỉ tính từ CTĐT là " + calculatedTotal
                            + ", khác tổng tín chỉ khai báo " + declared + ".")
                    .build());
        }
        if (groups.containsKey(UNCLASSIFIED_CODE)) {
            warnings.add(CreditDistributionResponse.DataWarning.builder()
                    .code("UNCLASSIFIED_COURSES")
                    .message("Có môn học chưa được gán nhóm môn; hệ thống đưa vào nhóm Chưa phân loại.")
                    .build());
        }

        return CreditDistributionResponse.builder()
                .programId(program.getId())
                .programCode(program.getCode())
                .programName(program.getName())
                .programNameVn(program.getNameVn())
                .cohortId(cohort.getId())
                .cohortName(cohort.getName())
                .cohortEntryYear(cohort.getEntryYear())
                .declaredProgramCredits(declared)
                .calculatedTotalCredits(calculatedTotal)
                .creditDifference(difference)
                .matchesDeclaredTotal(declared == null || difference == 0)
                .uniqueCourseCount(uniqueByCourse.size())
                .duplicateRowsRemoved(Math.max(0, rawRows.size() - uniqueByCourse.size()))
                .dataSource("CourseProgram scoped by program + cohort; credits are theory + lab; duplicate courses removed")
                .groups(resultGroups)
                .warnings(warnings)
                .build();
    }

    private boolean shouldReplace(CourseProgram current, CourseProgram candidate) {
        boolean currentSpecific = current.getCohort() != null;
        boolean candidateSpecific = candidate.getCohort() != null;
        if (candidateSpecific != currentSpecific) {
            return candidateSpecific;
        }
        int currentId = current.getId() == null ? Integer.MAX_VALUE : current.getId();
        int candidateId = candidate.getId() == null ? Integer.MAX_VALUE : candidate.getId();
        return candidateId < currentId;
    }

    private GroupIdentity resolveGroup(CourseProgram cp) {
        CourseType type = cp.getCourseType();
        if (type != null) {
            return new GroupIdentity(
                    "TYPE:" + type.getId(), type.getId(), type.getCode(), type.getName(), type.getNameVn());
        }
        if (Boolean.TRUE.equals(cp.getRequired())) {
            return new GroupIdentity("REQUIRED_FALLBACK", null,
                    "COMPULSORY", "Compulsory", "Môn bắt buộc");
        }
        if (Boolean.FALSE.equals(cp.getRequired())) {
            return new GroupIdentity("ELECTIVE_FALLBACK", null,
                    "ELECTIVE", "Elective", "Môn tự chọn");
        }
        return new GroupIdentity(UNCLASSIFIED_CODE, null,
                UNCLASSIFIED_CODE, "Unclassified", "Chưa phân loại");
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record GroupIdentity(String key, Integer courseTypeId, String code, String name, String nameVn) {}

    private static class MutableGroup {
        private final GroupIdentity identity;
        private int credits;
        private int courseCount;

        private MutableGroup(GroupIdentity identity) {
            this.identity = identity;
        }
    }
}
