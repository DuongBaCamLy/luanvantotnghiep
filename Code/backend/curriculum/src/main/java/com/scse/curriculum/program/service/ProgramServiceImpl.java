package com.scse.curriculum.program.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.department.entity.Department;
import com.scse.curriculum.department.repository.DepartmentRepository;
import com.scse.curriculum.major.entity.Major;
import com.scse.curriculum.major.repository.MajorRepository;
import com.scse.curriculum.program.dto.CloneProgramRequest;
import com.scse.curriculum.program.dto.CloneProgramResponse;
import com.scse.curriculum.program.dto.CreateProgramRequest;
import com.scse.curriculum.program.dto.CurriculumTimelineResponse;
import com.scse.curriculum.program.dto.ProgramCreditValidationResponse;
import com.scse.curriculum.program.dto.ProgramDiffResponse;
import com.scse.curriculum.program.dto.ProgramResponse;
import com.scse.curriculum.program.dto.UpdateProgramRequest;
import com.scse.curriculum.program.dto.ProgramArchiveValidationResponse;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.programtype.entity.ProgramType;
import com.scse.curriculum.programtype.repository.ProgramTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository repository;
    private final MajorRepository majorRepository;
    private final ProgramTypeRepository programTypeRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseProgramRepository courseProgramRepository;
    private final CohortRepository cohortRepository;

    @Override
    public ProgramResponse create(CreateProgramRequest request) {

        if (repository.existsByCode(request.getCode())) {
            throw new RuntimeException("Program code already exists");
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new ResourceNotFoundException("Major not found"));

        ProgramType programType = programTypeRepository.findById(request.getProgramTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Program Type not found"));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Program program = Program.builder()
                .code(request.getCode())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .major(major)
                .programType(programType)
                .department(department)
                .accreditationBody(request.getAccreditationBody())
                .totalCredits(request.getTotalCredits())
                .durationYears(request.getDurationYears())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return map(repository.save(program));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponse getById(Integer id) {
        Program program = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        return map(program);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponse getByCode(String code) {
        Program program = repository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        return map(program);
    }

    @Override
    public ProgramResponse update(Integer id, UpdateProgramRequest request) {
        Program program = findProgram(id);
        if (request.getValidTo() != null && request.getValidTo().isBefore(request.getValidFrom())) {
            throw new IllegalArgumentException("Valid-to date must be on or after the valid-from date");
        }

        program.setName(request.getName().trim());
        program.setNameVn(trimToNull(request.getNameVn()));
        program.setMajor(majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new ResourceNotFoundException("Major not found")));
        program.setProgramType(programTypeRepository.findById(request.getProgramTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Program Type not found")));
        program.setDepartment(departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found")));
        program.setAccreditationBody(trimToNull(request.getAccreditationBody()));
        program.setTotalCredits(request.getTotalCredits());
        program.setDurationYears(request.getDurationYears());
        program.setValidFrom(request.getValidFrom());
        program.setValidTo(request.getValidTo());
        return map(repository.save(program));
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramArchiveValidationResponse validateArchive(Integer id) {
        Program program = findProgram(id);
        return archiveValidation(program);
    }

    @Override
    public ProgramResponse archive(Integer id) {
        Program program = findProgram(id);
        ProgramArchiveValidationResponse validation = archiveValidation(program);
        if (!validation.isCanArchive()) {
            throw new IllegalStateException("Curriculum cannot be archived: " + String.join(" ", validation.getViolations()));
        }
        // Soft archive only: no cohort, course-program, enrollment, or syllabus data is changed or deleted.
        program.setIsActive(false);
        return map(repository.save(program));
    }

    @Override
    public ProgramResponse reactivate(Integer id) {
        Program program = findProgram(id);
        if (Boolean.TRUE.equals(program.getIsActive())) {
            throw new IllegalStateException("Curriculum is already active");
        }
        program.setIsActive(true);
        return map(repository.save(program));
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramDiffResponse getDiff(Integer programId, Integer oldCohortId, Integer newCohortId) {
        Program program = repository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        Cohort oldCohort = cohortRepository.findById(oldCohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Old Cohort not found"));
        Cohort newCohort = cohortRepository.findById(newCohortId)
                .orElseThrow(() -> new ResourceNotFoundException("New Cohort not found"));

        List<CourseProgram> oldCourses = courseProgramRepository.findByProgramIdAndCohortIdWithRelations(programId, oldCohortId);
        List<CourseProgram> newCourses = courseProgramRepository.findByProgramIdAndCohortIdWithRelations(programId, newCohortId);

        Map<Integer, CourseProgram> oldCourseMap = oldCourses.stream()
                .collect(Collectors.toMap(cp -> cp.getCourse().getId(), cp -> cp));
        Map<Integer, CourseProgram> newCourseMap = newCourses.stream()
                .collect(Collectors.toMap(cp -> cp.getCourse().getId(), cp -> cp));

        List<ProgramDiffResponse.CourseProgramDiff> added = new ArrayList<>();
        List<ProgramDiffResponse.CourseProgramDiff> removed = new ArrayList<>();
        List<ProgramDiffResponse.CourseProgramDiff> modified = new ArrayList<>();

        for (CourseProgram newItem : newCourses) {
            Integer courseId = newItem.getCourse().getId();
            if (!oldCourseMap.containsKey(courseId)) {
                added.add(toDiffItem(newItem, null));
                continue;
            }

            CourseProgram oldItem = oldCourseMap.get(courseId);
            Map<String, ProgramDiffResponse.FieldDiff> changes = new HashMap<>();

            if (!Objects.equals(courseTypeId(oldItem), courseTypeId(newItem))) {
                changes.put("courseType", ProgramDiffResponse.FieldDiff.builder()
                        .oldValue(courseTypeName(oldItem))
                        .newValue(courseTypeName(newItem))
                        .build());
            }
            if (!Objects.equals(oldItem.getSemesterSuggest(), newItem.getSemesterSuggest())) {
                changes.put("semesterSuggest", ProgramDiffResponse.FieldDiff.builder()
                        .oldValue(String.valueOf(oldItem.getSemesterSuggest()))
                        .newValue(String.valueOf(newItem.getSemesterSuggest()))
                        .build());
            }
            if (!Objects.equals(oldItem.getYearSuggest(), newItem.getYearSuggest())) {
                changes.put("yearSuggest", ProgramDiffResponse.FieldDiff.builder()
                        .oldValue(String.valueOf(oldItem.getYearSuggest()))
                        .newValue(String.valueOf(newItem.getYearSuggest()))
                        .build());
            }
            if (!Objects.equals(oldItem.getRequired(), newItem.getRequired())) {
                changes.put("isRequired", ProgramDiffResponse.FieldDiff.builder()
                        .oldValue(String.valueOf(oldItem.getRequired()))
                        .newValue(String.valueOf(newItem.getRequired()))
                        .build());
            }

            if (!changes.isEmpty()) {
                modified.add(toDiffItem(newItem, changes));
            }
        }

        for (CourseProgram oldItem : oldCourses) {
            if (!newCourseMap.containsKey(oldItem.getCourse().getId())) {
                removed.add(toDiffItem(oldItem, null));
            }
        }

        return ProgramDiffResponse.builder()
                .programId(programId)
                .programCode(program.getCode())
                .oldCohortId(oldCohortId)
                .newCohortId(newCohortId)
                .oldCohortYear(oldCohort.getName())
                .newCohortYear(newCohort.getName())
                .courseDiff(ProgramDiffResponse.ListDiff.<ProgramDiffResponse.CourseProgramDiff>builder()
                        .added(added)
                        .removed(removed)
                        .modified(modified)
                        .build())
                .build();
    }
@Override
@Transactional(readOnly = true)
public List<CurriculumTimelineResponse> getCurriculumTimeline(
        Integer programId) {

    Program program = repository.findById(programId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Program not found"));

    List<Cohort> cohorts = cohortRepository
            .findByProgram_IdOrderByEntryYearDesc(programId)
            .stream()
            .sorted(
                    Comparator
                            .comparing(Cohort::getEntryYear)
                            .thenComparing(Cohort::getId))
            .toList();

    List<CurriculumTimelineResponse> timeline =
            new ArrayList<>();

    Cohort previousCohort = null;

    Map<Integer, CourseProgram> previousCourses =
            Map.of();

    for (Cohort cohort : cohorts) {

        Map<Integer, CourseProgram> currentCourses =
                getEffectiveCourseMap(
                        programId,
                        cohort.getId());

        List<CurriculumTimelineResponse.CourseSummaryResponse>
                courseSnapshots =
                currentCourses
                        .values()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        item ->
                                                item.getCourse()
                                                        .getCourseCode(),
                                        String.CASE_INSENSITIVE_ORDER))
                        .map(this::toTimelineCourseSummary)
                        .toList();

        boolean baseline =
                previousCohort == null;

        List<CurriculumTimelineResponse.CourseChangeResponse>
                addedCourses =
                baseline
                        ? List.of()
                        : getAddedTimelineCourses(
                                previousCourses,
                                currentCourses);

        List<CurriculumTimelineResponse.CourseChangeResponse>
                removedCourses =
                baseline
                        ? List.of()
                        : getRemovedTimelineCourses(
                                previousCourses,
                                currentCourses);

        List<CurriculumTimelineResponse.CourseChangeResponse>
                changedCourses =
                baseline
                        ? List.of()
                        : getChangedTimelineCourses(
                                previousCourses,
                                currentCourses);

        int changeCount =
                addedCourses.size()
                        + removedCourses.size()
                        + changedCourses.size();

        timeline.add(
                CurriculumTimelineResponse.builder()
                        .programId(program.getId())
                        .programCode(program.getCode())
                        .programName(
                                program.getNameVn() != null
                                        && !program
                                                .getNameVn()
                                                .isBlank()
                                        ? program.getNameVn()
                                        : program.getName())
                        .cohortId(cohort.getId())
                        .cohortName(cohort.getName())
                        .entryYear(cohort.getEntryYear())
                        .previousCohortId(
                                previousCohort != null
                                        ? previousCohort.getId()
                                        : null)
                        .previousCohortName(
                                previousCohort != null
                                        ? previousCohort.getName()
                                        : null)
                        .previousEntryYear(
                                previousCohort != null
                                        ? previousCohort.getEntryYear()
                                        : null)
                        .baseline(baseline)
                        .totalCourses(currentCourses.size())
                        .changeCount(changeCount)
                        .currentCourses(courseSnapshots)
                        .addedCourses(addedCourses)
                        .removedCourses(removedCourses)
                        .changedCourses(changedCourses)
                        .build());

        previousCohort = cohort;
        previousCourses = currentCourses;
    }

    return timeline;
}
    @Override
    @Transactional(readOnly = true)
    public ProgramCreditValidationResponse validateCredits(Integer programId, Integer cohortId) {
        Program program = repository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        ensureCohortBelongsToProgram(programId, cohort, "Cohort does not belong to this program");

        List<CourseProgram> coursePrograms = courseProgramRepository
                .findEffectiveByProgramIdAndCohortIdWithRelations(programId, cohortId);

        List<ProgramCreditValidationResponse.CourseCreditItem> courseItems = coursePrograms.stream()
                .map(this::toCreditItem)
                .toList();

        int actualCredits = courseItems.stream()
                .mapToInt(item -> safe(item.getTotalCredits()))
                .sum();
        int requiredCredits = courseItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()))
                .mapToInt(item -> safe(item.getTotalCredits()))
                .sum();
        int electiveCredits = actualCredits - requiredCredits;
        int expectedCredits = safe(program.getTotalCredits());
        int difference = actualCredits - expectedCredits;
        boolean valid = expectedCredits > 0 && difference == 0;

        String message;
        if (expectedCredits <= 0) {
            message = "CTĐT chưa cấu hình tổng số tín chỉ yêu cầu.";
        } else if (valid) {
            message = "Tổng tín chỉ hợp lệ.";
        } else if (difference < 0) {
            message = "Tổng tín chỉ hiện tại đang thiếu " + Math.abs(difference) + " tín chỉ so với CTĐT.";
        } else {
            message = "Tổng tín chỉ hiện tại đang dư " + difference + " tín chỉ so với CTĐT.";
        }

        return ProgramCreditValidationResponse.builder()
                .programId(program.getId())
                .programCode(program.getCode())
                .cohortId(cohort.getId())
                .cohortName(cohort.getName())
                .expectedTotalCredits(program.getTotalCredits())
                .actualTotalCredits(actualCredits)
                .requiredCredits(requiredCredits)
                .electiveCredits(electiveCredits)
                .difference(difference)
                .valid(valid)
                .message(message)
                .courses(courseItems)
                .build();
    }

    @Override
    public CloneProgramResponse cloneToCohort(Integer programId, CloneProgramRequest request) {
        return cloneFromCohort(
                programId,
                request.getSourceCohortId(),
                programId,
                request.getTargetCohortId(),
                Boolean.TRUE.equals(request.getOverwriteExisting()));
    }

    @Override
    public CloneProgramResponse cloneFromCohort(
            Integer sourceProgramId,
            Integer sourceCohortId,
            Integer targetProgramId,
            Integer targetCohortId) {
        return cloneFromCohort(sourceProgramId, sourceCohortId, targetProgramId, targetCohortId, false);
    }

    private CloneProgramResponse cloneFromCohort(
            Integer sourceProgramId,
            Integer sourceCohortId,
            Integer targetProgramId,
            Integer targetCohortId,
            boolean overwriteExisting) {
        Program sourceProgram = repository.findById(sourceProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("Source program not found"));
        Program targetProgram = repository.findById(targetProgramId)
                .orElseThrow(() -> new ResourceNotFoundException("Target program not found"));
        Cohort sourceCohort = cohortRepository.findById(sourceCohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Source cohort not found"));
        Cohort targetCohort = cohortRepository.findById(targetCohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Target cohort not found"));

        ensureCohortBelongsToProgram(sourceProgram.getId(), sourceCohort, "Source cohort does not belong to the source program");
        ensureCohortBelongsToProgram(targetProgram.getId(), targetCohort, "Target cohort does not belong to the target program");

        if (Objects.equals(sourceCohort.getId(), targetCohort.getId())) {
            throw new IllegalArgumentException("Source cohort and target cohort must be different");
        }

        List<CourseProgram> sourceItems = courseProgramRepository
                .findByProgramIdAndCohortIdWithRelations(sourceProgram.getId(), sourceCohort.getId());
        List<CourseProgram> targetItems = courseProgramRepository
                .findByProgramIdAndCohortIdWithRelations(targetProgram.getId(), targetCohort.getId());
        int overwrittenCount = 0;

        if (overwriteExisting && !targetItems.isEmpty()) {
            overwrittenCount = targetItems.size();
            courseProgramRepository.deleteAll(targetItems);
            courseProgramRepository.flush();
            targetItems = List.of();
        }

        Map<Integer, CourseProgram> targetByCourseId = targetItems.stream()
                .collect(Collectors.toMap(cp -> cp.getCourse().getId(), cp -> cp));

        List<Integer> createdIds = new ArrayList<>();
        int skippedCount = 0;

        for (CourseProgram sourceItem : sourceItems) {
            Integer courseId = sourceItem.getCourse().getId();
            if (!overwriteExisting && targetByCourseId.containsKey(courseId)) {
                skippedCount++;
                continue;
            }

            CourseProgram clone = CourseProgram.builder()
                    .course(sourceItem.getCourse())
                    .program(targetProgram)
                    .cohort(targetCohort)
                    .courseType(sourceItem.getCourseType())
                    .semesterSuggest(sourceItem.getSemesterSuggest())
                    .yearSuggest(sourceItem.getYearSuggest())
                    .required(sourceItem.getRequired())
                    .build();

            CourseProgram saved = courseProgramRepository.save(clone);
            createdIds.add(saved.getId());
        }

        return CloneProgramResponse.builder()
                .programId(targetProgram.getId())
                .programCode(targetProgram.getCode())
                .sourceCohortId(sourceCohort.getId())
                .sourceCohortName(sourceCohort.getName())
                .targetCohortId(targetCohort.getId())
                .targetCohortName(targetCohort.getName())
                .copiedCount(createdIds.size())
                .skippedCount(skippedCount)
                .overwrittenCount(overwrittenCount)
                .createdCourseProgramIds(createdIds)
                .message("Đã clone " + createdIds.size() + " môn học từ "
                        + sourceCohort.getName() + " sang " + targetCohort.getName()
                        + (skippedCount > 0 ? ". Bỏ qua " + skippedCount + " môn đã tồn tại." : "."))
                .build();
    }

    private ProgramResponse map(Program program) {
        return ProgramResponse.builder()
                .id(program.getId())
                .code(program.getCode())
                .name(program.getName())
                .nameVn(program.getNameVn())
                .majorId(program.getMajor().getId())
                .majorCode(program.getMajor().getCode())
                .programTypeId(program.getProgramType().getId())
                .programTypeCode(program.getProgramType().getCode())
                .departmentId(program.getDepartment().getId())
                .departmentCode(program.getDepartment().getCode())
                .accreditationBody(program.getAccreditationBody())
                .totalCredits(program.getTotalCredits())
                .durationYears(program.getDurationYears())
                .validFrom(program.getValidFrom())
                .validTo(program.getValidTo())
                .isActive(program.getIsActive())
                .createdAt(program.getCreatedAt())
                .build();
    }

    private Map<Integer, CourseProgram> getEffectiveCourseMap(
        Integer programId,
        Integer cohortId) {

    Map<Integer, CourseProgram> effectiveCourses =
            new HashMap<>();

    List<CourseProgram> coursePrograms =
            courseProgramRepository
                    .findEffectiveByProgramIdAndCohortIdWithRelations(
                            programId,
                            cohortId);

    for (CourseProgram item : coursePrograms) {

        Integer courseId =
                item.getCourse().getId();

        CourseProgram current =
                effectiveCourses.get(courseId);

        if (current == null
                || shouldReplaceTimelineCourse(
                        current,
                        item)) {

            effectiveCourses.put(
                    courseId,
                    item);
        }
    }

    return effectiveCourses;
}

private boolean shouldReplaceTimelineCourse(
        CourseProgram current,
        CourseProgram candidate) {

    boolean currentSpecific =
            current.getCohort() != null;

    boolean candidateSpecific =
            candidate.getCohort() != null;

    /*
     * Nếu cùng một môn tồn tại ở:
     * - cấu hình chung cohort = null;
     * - cấu hình riêng theo cohort;
     *
     * thì cấu hình riêng theo cohort được ưu tiên.
     */
    if (currentSpecific != candidateSpecific) {
        return candidateSpecific;
    }

    /*
     * Trường hợp dữ liệu bị trùng cùng mức,
     * ưu tiên bản ghi mới hơn.
     */
    return safeId(candidate.getId())
            > safeId(current.getId());
}

private List<CurriculumTimelineResponse.CourseChangeResponse>
        getAddedTimelineCourses(
                Map<Integer, CourseProgram> previousCourses,
                Map<Integer, CourseProgram> currentCourses) {

    return currentCourses
            .entrySet()
            .stream()
            .filter(entry ->
                    !previousCourses.containsKey(
                            entry.getKey()))
            .map(Map.Entry::getValue)
            .sorted(
                    Comparator.comparing(
                            item ->
                                    item.getCourse()
                                            .getCourseCode(),
                            String.CASE_INSENSITIVE_ORDER))
            .map(item ->
                    toTimelineCourseChange(
                            item,
                            "ADDED",
                            List.of()))
            .toList();
}

private List<CurriculumTimelineResponse.CourseChangeResponse>
        getRemovedTimelineCourses(
                Map<Integer, CourseProgram> previousCourses,
                Map<Integer, CourseProgram> currentCourses) {

    return previousCourses
            .entrySet()
            .stream()
            .filter(entry ->
                    !currentCourses.containsKey(
                            entry.getKey()))
            .map(Map.Entry::getValue)
            .sorted(
                    Comparator.comparing(
                            item ->
                                    item.getCourse()
                                            .getCourseCode(),
                            String.CASE_INSENSITIVE_ORDER))
            .map(item ->
                    toTimelineCourseChange(
                            item,
                            "REMOVED",
                            List.of()))
            .toList();
}

private List<CurriculumTimelineResponse.CourseChangeResponse>
        getChangedTimelineCourses(
                Map<Integer, CourseProgram> previousCourses,
                Map<Integer, CourseProgram> currentCourses) {

    List<CurriculumTimelineResponse.CourseChangeResponse>
            changedCourses =
            new ArrayList<>();

    currentCourses
            .values()
            .stream()
            .sorted(
                    Comparator.comparing(
                            item ->
                                    item.getCourse()
                                            .getCourseCode(),
                            String.CASE_INSENSITIVE_ORDER))
            .forEach(currentItem -> {

                Integer courseId =
                        currentItem
                                .getCourse()
                                .getId();

                CourseProgram previousItem =
                        previousCourses.get(courseId);

                if (previousItem == null) {
                    return;
                }

                List<CurriculumTimelineResponse.FieldChangeResponse>
                        changes =
                        getTimelineFieldChanges(
                                previousItem,
                                currentItem);

                if (!changes.isEmpty()) {

                    changedCourses.add(
                            toTimelineCourseChange(
                                    currentItem,
                                    "MODIFIED",
                                    changes));
                }
            });

    return changedCourses;
}

private List<CurriculumTimelineResponse.FieldChangeResponse>
        getTimelineFieldChanges(
                CourseProgram previousItem,
                CourseProgram currentItem) {

    List<CurriculumTimelineResponse.FieldChangeResponse>
            changes =
            new ArrayList<>();

    addTimelineFieldChange(
            changes,
            "courseType",
            "Nhóm môn học",
            courseTypeId(previousItem),
            courseTypeId(currentItem),
            courseTypeName(previousItem),
            courseTypeName(currentItem));

    addTimelineFieldChange(
            changes,
            "required",
            "Tính chất",
            previousItem.getRequired(),
            currentItem.getRequired(),
            requiredLabel(
                    previousItem.getRequired()),
            requiredLabel(
                    currentItem.getRequired()));

    addTimelineFieldChange(
            changes,
            "yearSuggest",
            "Năm học đề xuất",
            previousItem.getYearSuggest(),
            currentItem.getYearSuggest(),
            displayValue(
                    previousItem.getYearSuggest()),
            displayValue(
                    currentItem.getYearSuggest()));

    addTimelineFieldChange(
            changes,
            "semesterSuggest",
            "Học kỳ đề xuất",
            previousItem.getSemesterSuggest(),
            currentItem.getSemesterSuggest(),
            displayValue(
                    previousItem.getSemesterSuggest()),
            displayValue(
                    currentItem.getSemesterSuggest()));

    addTimelineFieldChange(
            changes,
            "termCode",
            "Mã học kỳ",
            previousItem.getTermCode(),
            currentItem.getTermCode(),
            enumValue(
                    previousItem.getTermCode()),
            enumValue(
                    currentItem.getTermCode()));

    addTimelineFieldChange(
            changes,
            "syllabus",
            "Đề cương liên kết",
            syllabusId(previousItem),
            syllabusId(currentItem),
            syllabusLabel(previousItem),
            syllabusLabel(currentItem));

    return changes;
}

private void addTimelineFieldChange(
        List<CurriculumTimelineResponse.FieldChangeResponse>
                changes,
        String field,
        String label,
        Object oldComparableValue,
        Object newComparableValue,
        String oldDisplayValue,
        String newDisplayValue) {

    if (Objects.equals(
            oldComparableValue,
            newComparableValue)) {

        return;
    }

    changes.add(
            CurriculumTimelineResponse
                    .FieldChangeResponse
                    .builder()
                    .field(field)
                    .label(label)
                    .oldValue(oldDisplayValue)
                    .newValue(newDisplayValue)
                    .build());
}

private CurriculumTimelineResponse.CourseSummaryResponse
        toTimelineCourseSummary(
                CourseProgram item) {

    Course course =
            item.getCourse();

    String courseName =
            course.getNameVn() != null
                    && !course.getNameVn().isBlank()
                    ? course.getNameVn()
                    : course.getName();

    return CurriculumTimelineResponse
            .CourseSummaryResponse
            .builder()
            .courseId(course.getId())
            .courseCode(course.getCourseCode())
            .courseName(courseName)
            .courseType(courseTypeName(item))
            .required(item.getRequired())
            .yearSuggest(item.getYearSuggest())
            .semesterSuggest(
                    item.getSemesterSuggest())
            .termCode(
                    item.getTermCode() != null
                            ? item.getTermCode().name()
                            : null)
            .syllabusId(syllabusId(item))
            .syllabusVersionLabel(
                    item.getSyllabus() != null
                            ? item.getSyllabus()
                                    .getVersionLabel()
                            : null)
            .build();
}

private CurriculumTimelineResponse.CourseChangeResponse
        toTimelineCourseChange(
                CourseProgram item,
                String changeType,
                List<CurriculumTimelineResponse.FieldChangeResponse>
                        changes) {

    Course course =
            item.getCourse();

    String courseName =
            course.getNameVn() != null
                    && !course.getNameVn().isBlank()
                    ? course.getNameVn()
                    : course.getName();

    return CurriculumTimelineResponse
            .CourseChangeResponse
            .builder()
            .courseId(course.getId())
            .courseCode(course.getCourseCode())
            .courseName(courseName)
            .changeType(changeType)
            .changes(changes)
            .build();
}

private String requiredLabel(
        Boolean required) {

    if (required == null) {
        return "Chưa xác định";
    }

    return Boolean.TRUE.equals(required)
            ? "Bắt buộc"
            : "Tự chọn";
}

private String displayValue(
        Object value) {

    return value == null
            ? "Chưa xác định"
            : String.valueOf(value);
}

private String enumValue(
        Enum<?> value) {

    return value == null
            ? "Chưa xác định"
            : value.name();
}

private Integer syllabusId(
        CourseProgram item) {

    return item.getSyllabus() != null
            ? item.getSyllabus().getId()
            : null;
}

private String syllabusLabel(
        CourseProgram item) {

    if (item.getSyllabus() == null) {
        return "Chưa liên kết";
    }

    String versionLabel =
            item.getSyllabus()
                    .getVersionLabel();

    return versionLabel == null
            || versionLabel.isBlank()
            ? "Syllabus #"
                    + item.getSyllabus().getId()
            : versionLabel;
}

private int safeId(
        Integer value) {

    return value == null
            ? 0
            : value;
}
    private Program findProgram(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
    }

    /**
     * A curriculum may be archived only when it is active, has complete metadata,
     * and every existing cohort has a course list whose credits match the stated
     * total. These checks prevent a partially configured curriculum from being
     * hidden as historical data. Archiving itself is always a soft state change.
     */
    private ProgramArchiveValidationResponse archiveValidation(Program program) {
        List<String> violations = new ArrayList<>();
        if (!Boolean.TRUE.equals(program.getIsActive())) {
            violations.add("Curriculum is already archived.");
        }
        if (program.getMajor() == null || program.getTotalCredits() == null || program.getTotalCredits() <= 0
                || program.getValidFrom() == null) {
            violations.add("Major, total credits, and valid-from date must be configured.");
        }
        if (program.getValidTo() != null && program.getValidTo().isBefore(program.getValidFrom())) {
            violations.add("Valid-to date must be on or after the valid-from date.");
        }

        List<Cohort> cohorts = cohortRepository.findByProgram_IdOrderByEntryYearDesc(program.getId());
        if (cohorts.isEmpty()) {
            violations.add("At least one cohort is required before archiving a curriculum.");
        }
        for (Cohort cohort : cohorts) {
            List<CourseProgram> items = courseProgramRepository
                    .findEffectiveByProgramIdAndCohortIdWithRelations(program.getId(), cohort.getId());
            int actualCredits = items.stream()
                    .map(CourseProgram::getCourse)
                    .mapToInt(course -> safe(course.getCreditTheory()) + safe(course.getCreditLab()))
                    .sum();
            if (actualCredits != safe(program.getTotalCredits())) {
                violations.add("Cohort " + cohort.getName() + " has " + actualCredits
                        + " credits; expected " + program.getTotalCredits() + ".");
            }
        }
        return ProgramArchiveValidationResponse.builder()
                .programId(program.getId())
                .canArchive(violations.isEmpty())
                .violations(violations)
                .build();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProgramDiffResponse.CourseProgramDiff toDiffItem(
            CourseProgram item,
            Map<String, ProgramDiffResponse.FieldDiff> changes) {
        return ProgramDiffResponse.CourseProgramDiff.builder()
                .courseId(item.getCourse().getId())
                .courseCode(item.getCourse().getCourseCode())
                .courseName(item.getCourse().getName())
                .changes(changes)
                .build();
    }

    private ProgramCreditValidationResponse.CourseCreditItem toCreditItem(CourseProgram item) {
        Course course = item.getCourse();
        int theory = safe(course.getCreditTheory());
        int lab = safe(course.getCreditLab());

        return ProgramCreditValidationResponse.CourseCreditItem.builder()
                .courseProgramId(item.getId())
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getName())
                .creditTheory(course.getCreditTheory())
                .creditLab(course.getCreditLab())
                .totalCredits(theory + lab)
                .courseTypeName(item.getCourseType() != null ? item.getCourseType().getName() : null)
                .semesterSuggest(item.getSemesterSuggest())
                .yearSuggest(item.getYearSuggest())
                .required(item.getRequired())
                .build();
    }

    private void ensureCohortBelongsToProgram(Integer programId, Cohort cohort, String message) {
        if (cohort.getProgram() == null || !Objects.equals(cohort.getProgram().getId(), programId)) {
            throw new IllegalArgumentException(message);
        }
    }

    private Integer courseTypeId(CourseProgram item) {
        return item.getCourseType() != null ? item.getCourseType().getId() : null;
    }

    private String courseTypeName(CourseProgram item) {
        return item.getCourseType() != null ? item.getCourseType().getName() : null;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
