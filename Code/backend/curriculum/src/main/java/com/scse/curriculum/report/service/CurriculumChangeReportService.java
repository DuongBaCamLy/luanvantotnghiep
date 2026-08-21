package com.scse.curriculum.report.service;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.program.dto.ProgramDiffResponse;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.program.service.ProgramService;
import com.scse.curriculum.report.dto.CurriculumChangeReportData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurriculumChangeReportService {

    private final ProgramService programService;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final CourseProgramRepository courseProgramRepository;

    @Transactional(readOnly = true)
    public CurriculumChangeReportData build(
            Integer programId,
            Integer oldCohortId,
            Integer newCohortId) {

        requirePositive(programId, "programId");
        requirePositive(oldCohortId, "oldCohortId");
        requirePositive(newCohortId, "newCohortId");
        if (Objects.equals(oldCohortId, newCohortId)) {
            throw new IllegalArgumentException("Hai cohort so sánh phải khác nhau.");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình đào tạo."));
        Cohort oldCohort = cohortRepository.findById(oldCohortId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cohort cũ."));
        Cohort newCohort = cohortRepository.findById(newCohortId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cohort mới."));
        ensureBelongs(programId, oldCohort, "Cohort cũ không thuộc chương trình đã chọn.");
        ensureBelongs(programId, newCohort, "Cohort mới không thuộc chương trình đã chọn.");

        // Reuse the canonical Program diff already used by the UI.
        ProgramDiffResponse canonicalDiff = programService.getDiff(programId, oldCohortId, newCohortId);

        Map<Integer, CourseProgram> oldByCourse = deduplicate(
                courseProgramRepository.findByProgramIdAndCohortIdWithRelations(programId, oldCohortId));
        Map<Integer, CourseProgram> newByCourse = deduplicate(
                courseProgramRepository.findByProgramIdAndCohortIdWithRelations(programId, newCohortId));

        Set<Integer> addedIds = ids(canonicalDiff.getCourseDiff() == null ? null : canonicalDiff.getCourseDiff().getAdded());
        Set<Integer> removedIds = ids(canonicalDiff.getCourseDiff() == null ? null : canonicalDiff.getCourseDiff().getRemoved());
        Set<Integer> modifiedIds = ids(canonicalDiff.getCourseDiff() == null ? null : canonicalDiff.getCourseDiff().getModified());

        // Defensive reconciliation: report remains complete even if the UI diff service evolves.
        newByCourse.keySet().stream().filter(id -> !oldByCourse.containsKey(id)).forEach(addedIds::add);
        oldByCourse.keySet().stream().filter(id -> !newByCourse.containsKey(id)).forEach(removedIds::add);
        oldByCourse.keySet().stream().filter(newByCourse::containsKey)
                .filter(id -> !fieldChanges(oldByCourse.get(id), newByCourse.get(id)).isEmpty())
                .forEach(modifiedIds::add);

        List<CurriculumChangeReportData.CourseChange> courseChanges = new ArrayList<>();
        addedIds.forEach(id -> courseChanges.add(change("ADDED", null, newByCourse.get(id))));
        removedIds.forEach(id -> courseChanges.add(change("REMOVED", oldByCourse.get(id), null)));
        modifiedIds.stream()
                .filter(id -> oldByCourse.containsKey(id) && newByCourse.containsKey(id))
                .forEach(id -> courseChanges.add(change("MODIFIED", oldByCourse.get(id), newByCourse.get(id))));
        courseChanges.sort(Comparator
                .comparing(CurriculumChangeReportData.CourseChange::getChangeType)
                .thenComparing(item -> safe(item.getCourseCode()), String.CASE_INSENSITIVE_ORDER));

        List<CurriculumChangeReportData.MetadataChange> metadata = cohortMetadata(oldCohort, newCohort);
        int oldCredits = totalCredits(oldByCourse.values());
        int newCredits = totalCredits(newByCourse.values());
        int unchanged = (int) oldByCourse.keySet().stream()
                .filter(newByCourse::containsKey)
                .filter(id -> !modifiedIds.contains(id))
                .count();

        List<String> warnings = new ArrayList<>();
        warnings.add("Tín chỉ hiện được lưu ở Course dùng chung. Báo cáo chỉ nhận diện thay đổi tín chỉ khi dữ liệu nguồn của hai cohort thực sự khác nhau.");
        if (courseChanges.isEmpty() && metadata.isEmpty()) {
            warnings.add("Không phát hiện thay đổi giữa hai cohort.");
        }

        return CurriculumChangeReportData.builder()
                .formatVersion("FR-07.6-v1")
                .generatedAt(OffsetDateTime.now())
                .scopeKey("program=" + programId + "|oldCohort=" + oldCohortId + "|newCohort=" + newCohortId)
                .programId(programId)
                .programCode(program.getCode())
                .programName(program.getName())
                .programNameVn(program.getNameVn())
                .oldCohort(cohortSnapshot(oldCohort, oldByCourse.size(), oldCredits))
                .newCohort(cohortSnapshot(newCohort, newByCourse.size(), newCredits))
                .summary(CurriculumChangeReportData.Summary.builder()
                        .oldCourseCount(oldByCourse.size())
                        .newCourseCount(newByCourse.size())
                        .addedCourses(addedIds.size())
                        .removedCourses(removedIds.size())
                        .modifiedCourses(modifiedIds.size())
                        .unchangedCourses(unchanged)
                        .oldTotalCredits(oldCredits)
                        .newTotalCredits(newCredits)
                        .creditDifference(newCredits - oldCredits)
                        .metadataChangeCount(metadata.size())
                        .build())
                .metadataChanges(metadata)
                .courseChanges(courseChanges)
                .warnings(warnings)
                .build();
    }

    private static Map<Integer, CourseProgram> deduplicate(List<CourseProgram> rows) {
        Map<Integer, CourseProgram> result = new LinkedHashMap<>();
        if (rows == null) return result;
        rows.stream()
                .filter(Objects::nonNull)
                .filter(cp -> cp.getCourse() != null && cp.getCourse().getId() != null)
                .sorted(Comparator.comparing(CourseProgram::getId, Comparator.nullsLast(Integer::compareTo)))
                .forEach(cp -> result.putIfAbsent(cp.getCourse().getId(), cp));
        return result;
    }

    private static Set<Integer> ids(List<ProgramDiffResponse.CourseProgramDiff> items) {
        if (items == null) return new java.util.LinkedHashSet<>();
        return items.stream().map(ProgramDiffResponse.CourseProgramDiff::getCourseId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static CurriculumChangeReportData.CourseChange change(
            String type,
            CourseProgram oldItem,
            CourseProgram newItem) {
        CourseProgram source = newItem != null ? newItem : oldItem;
        Course course = source == null ? null : source.getCourse();
        return CurriculumChangeReportData.CourseChange.builder()
                .changeType(type)
                .courseId(course == null ? null : course.getId())
                .courseCode(course == null ? null : course.getCourseCode())
                .courseName(course == null ? null : course.getName())
                .courseNameVn(course == null ? null : course.getNameVn())
                .oldValue(snapshot(oldItem))
                .newValue(snapshot(newItem))
                .changes(oldItem != null && newItem != null ? fieldChanges(oldItem, newItem) : List.of())
                .build();
    }

    private static CurriculumChangeReportData.CourseSnapshot snapshot(CourseProgram item) {
        if (item == null) return null;
        Course course = item.getCourse();
        int theory = course == null ? 0 : nonNegative(course.getCreditTheory());
        int lab = course == null ? 0 : nonNegative(course.getCreditLab());
        return CurriculumChangeReportData.CourseSnapshot.builder()
                .courseProgramId(item.getId())
                .creditTheory(theory)
                .creditLab(lab)
                .totalCredits(theory + lab)
                .semesterSuggest(item.getSemesterSuggest())
                .yearSuggest(item.getYearSuggest())
                .termCode(item.getTermCode() == null ? null : item.getTermCode().name())
                .required(item.getRequired())
                .courseTypeId(item.getCourseType() == null ? null : item.getCourseType().getId())
                .courseTypeCode(item.getCourseType() == null ? null : item.getCourseType().getCode())
                .courseTypeName(item.getCourseType() == null ? null : item.getCourseType().getName())
                .courseTypeNameVn(item.getCourseType() == null ? null : item.getCourseType().getNameVn())
                .build();
    }

    private static List<CurriculumChangeReportData.FieldChange> fieldChanges(CourseProgram oldItem, CourseProgram newItem) {
        Map<String, CurriculumChangeReportData.FieldChange> changes = new LinkedHashMap<>();
        CurriculumChangeReportData.CourseSnapshot oldValue = snapshot(oldItem);
        CurriculumChangeReportData.CourseSnapshot newValue = snapshot(newItem);
        add(changes, "totalCredits", "Tổng tín chỉ", oldValue.getTotalCredits(), newValue.getTotalCredits());
        add(changes, "creditTheory", "Tín chỉ lý thuyết", oldValue.getCreditTheory(), newValue.getCreditTheory());
        add(changes, "creditLab", "Tín chỉ thực hành", oldValue.getCreditLab(), newValue.getCreditLab());
        add(changes, "semesterSuggest", "Học kỳ đề xuất", oldValue.getSemesterSuggest(), newValue.getSemesterSuggest());
        add(changes, "yearSuggest", "Năm đề xuất", oldValue.getYearSuggest(), newValue.getYearSuggest());
        add(changes, "termCode", "Mã học kỳ", oldValue.getTermCode(), newValue.getTermCode());
        add(changes, "required", "Bắt buộc", oldValue.getRequired(), newValue.getRequired());
        add(changes, "courseType", "Nhóm môn", preferred(oldValue.getCourseTypeNameVn(), oldValue.getCourseTypeName()),
                preferred(newValue.getCourseTypeNameVn(), newValue.getCourseTypeName()));
        return new ArrayList<>(changes.values());
    }

    private static void add(Map<String, CurriculumChangeReportData.FieldChange> target,
                            String field, String label, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            target.put(field, CurriculumChangeReportData.FieldChange.builder()
                    .field(field).label(label).oldValue(value(oldValue)).newValue(value(newValue)).build());
        }
    }

    private static List<CurriculumChangeReportData.MetadataChange> cohortMetadata(Cohort oldCohort, Cohort newCohort) {
        List<CurriculumChangeReportData.MetadataChange> result = new ArrayList<>();
        metadata(result, "name", "Tên cohort", oldCohort.getName(), newCohort.getName());
        metadata(result, "entryYear", "Năm tuyển sinh", oldCohort.getEntryYear(), newCohort.getEntryYear());
        metadata(result, "description", "Mô tả cohort", oldCohort.getDescription(), newCohort.getDescription());
        metadata(result, "active", "Trạng thái hoạt động", oldCohort.getIsActive(), newCohort.getIsActive());
        return result;
    }

    private static void metadata(List<CurriculumChangeReportData.MetadataChange> result,
                                 String field, String label, Object oldValue, Object newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            result.add(CurriculumChangeReportData.MetadataChange.builder()
                    .field(field).label(label).oldValue(value(oldValue)).newValue(value(newValue)).build());
        }
    }

    private static CurriculumChangeReportData.CohortSnapshot cohortSnapshot(Cohort cohort, int courses, int credits) {
        return CurriculumChangeReportData.CohortSnapshot.builder()
                .id(cohort.getId()).name(cohort.getName()).entryYear(cohort.getEntryYear())
                .description(cohort.getDescription()).active(cohort.getIsActive())
                .courseCount(courses).totalCredits(credits).build();
    }

    private static int totalCredits(java.util.Collection<CourseProgram> rows) {
        return rows.stream().map(CourseProgram::getCourse).filter(Objects::nonNull)
                .mapToInt(course -> nonNegative(course.getCreditTheory()) + nonNegative(course.getCreditLab())).sum();
    }

    private static int nonNegative(Integer value) { return value == null ? 0 : Math.max(0, value); }
    private static String preferred(String first, String second) { return first != null && !first.isBlank() ? first : second; }
    private static String value(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static void requirePositive(Integer value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " phải là số nguyên dương.");
    }
    private static void ensureBelongs(Integer programId, Cohort cohort, String message) {
        if (cohort.getProgram() == null || !Objects.equals(programId, cohort.getProgram().getId())) {
            throw new IllegalArgumentException(message);
        }
    }
}
