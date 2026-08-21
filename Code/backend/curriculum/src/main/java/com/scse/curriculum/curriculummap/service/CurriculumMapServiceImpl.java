package com.scse.curriculum.curriculummap.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.courserelationship.entity.CourseRelationship;
import com.scse.curriculum.courserelationship.entity.RelationType;
import com.scse.curriculum.courserelationship.repository.CourseRelationshipRepository;
import com.scse.curriculum.curriculummap.dto.CurriculumMapResponse;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumMapServiceImpl
        implements CurriculumMapService {

    private static final List<String> SEMESTER_ORDER = List.of(
            "Semester 1",
            "Semester 2",
            "Semester 3",
            "Semester 4",
            "Semester 5",
            "Semester 6",
            "Summer",
            "Semester 7",
            "Semester 8",
            "Elective",
            "Unassigned"
    );

    private final CourseProgramRepository courseProgramRepository;

    private final CourseRelationshipRepository courseRelationshipRepository;

    private final ProgramRepository programRepository;

    private final CohortRepository cohortRepository;

    @Override
    @Transactional(readOnly = true)
    public CurriculumMapResponse generate(
            Integer programId,
            Integer cohortId
    ) {

        // =========================
        // 1. KIỂM TRA THAM SỐ
        // =========================
        if (programId == null) {
            throw new IllegalArgumentException(
                    "programId không được để trống"
            );
        }

        // =========================
        // 2. LẤY CTĐT VÀ KHÓA
        // =========================
        Program program = programRepository
                .findById(programId)
                .orElseThrow(()
                        -> new ResourceNotFoundException(
                        "Không tìm thấy chương trình đào tạo "
                        + "có ID = "
                        + programId
                )
                );

        Cohort cohort = null;

        if (cohortId != null) {

            cohort = cohortRepository
                    .findById(cohortId)
                    .orElseThrow(()
                            -> new ResourceNotFoundException(
                            "Không tìm thấy khóa có ID = "
                            + cohortId
                    )
                    );

            if (cohort.getProgram() == null
                    || !Objects.equals(
                            cohort.getProgram().getId(),
                            programId
                    )) {

                throw new IllegalArgumentException(
                        "Khóa được chọn không thuộc chương trình đào tạo này"
                );
            }
        }

        // =========================
        // 3. LẤY DANH SÁCH MÔN
        // =========================
        List<CourseProgram> rawCoursePrograms;

        if (cohortId == null) {

            /*
     * Không chọn cohort:
     * lấy cấu hình chung của Program.
             */
            rawCoursePrograms
                    = courseProgramRepository
                            .findGeneralByProgramIdWithRelations(
                                    programId
                            );

        } else {

            /*
     * Có chọn cohort:
     * lấy cấu hình chung và cấu hình override riêng.
             */
            rawCoursePrograms
                    = courseProgramRepository
                            .findEffectiveByProgramIdAndCohortIdWithRelations(
                                    programId,
                                    cohortId
                            );
        }

        /*
         * Có thể tồn tại:
         * - Một CourseProgram áp dụng chung: cohort = null
         * - Một CourseProgram riêng cho khóa: cohort = cohortId
         *
         * Nếu cùng một môn xuất hiện hai lần,
         * ưu tiên bản riêng của khóa.
         */
        Map<Integer, CourseProgram> courseProgramByCourseId
                = new LinkedHashMap<>();

        for (CourseProgram courseProgram : rawCoursePrograms) {

            if (courseProgram.getCourse() == null
                    || courseProgram.getCourse().getId() == null) {
                continue;
            }

            Integer courseId
                    = courseProgram.getCourse().getId();

            CourseProgram existing
                    = courseProgramByCourseId.get(courseId);

            if (existing == null) {
                courseProgramByCourseId.put(
                        courseId,
                        courseProgram
                );

                continue;
            }

            boolean existingIsGeneral
                    = existing.getCohort() == null;

            boolean candidateIsSpecific
                    = courseProgram.getCohort() != null;

            if (existingIsGeneral && candidateIsSpecific) {
                courseProgramByCourseId.put(
                        courseId,
                        courseProgram
                );
            }
        }

        List<CourseProgram> coursePrograms
                = new ArrayList<>(
                        courseProgramByCourseId.values()
                );

        // =========================
        // 4. KHỞI TẠO CÁC HỌC KỲ
        // =========================
        Map<String, List<CurriculumMapResponse.CourseNode>> semesterMap = new LinkedHashMap<>();

        for (String semester : SEMESTER_ORDER) {
            semesterMap.put(
                    semester,
                    new ArrayList<>()
            );
        }

        Set<Integer> selectedCourseIds
                = new HashSet<>();

        // =========================
        // 5. TẠO NODE TỪ COURSE_PROGRAM
        // =========================
        for (CourseProgram courseProgram : coursePrograms) {

            Course course = courseProgram.getCourse();

            if (course == null || course.getId() == null) {
                continue;
            }

            selectedCourseIds.add(course.getId());

            String semester;

            if (courseProgram.getSemesterSuggest() == null
                    || courseProgram.getSemesterSuggest() <= 0) {

                semester = "Unassigned";

            } else {

                semester = normalizeSemesterKey(
                        "Semester "
                        + courseProgram.getSemesterSuggest()
                );
            }

            semesterMap.putIfAbsent(
                    semester,
                    new ArrayList<>()
            );

            String majorCode = null;

            if (program.getMajor() != null) {
                majorCode = program.getMajor().getCode();
            }

            String courseTypeName = null;

            if (courseProgram.getCourseType() != null) {
                courseTypeName
                        = courseProgram
                                .getCourseType()
                                .getName();
            }

            String syllabusVersion = null;

            if (courseProgram.getSyllabus() != null) {
                syllabusVersion
                        = courseProgram
                                .getSyllabus()
                                .getVersionLabel();
            }

            CurriculumMapResponse.CourseNode node =
        CurriculumMapResponse.CourseNode
                .builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getName())
                .courseNameVn(course.getNameVn())
                .creditTheory(
                        course.getCreditTheory()
                )
                .creditLab(
                        course.getCreditLab()
                )
                .semester(semester)
                .academicYear(
                        cohort != null
                                ? cohort.getName()
                                : program.getCode()
                )
                .major(majorCode)
                .courseTypes(courseTypeName)
                .syllabusVersion(syllabusVersion)
                .build();

            semesterMap
                    .get(semester)
                    .add(node);
        }

        // =========================
        // 6. TẠO NHÓM HỌC KỲ
        // =========================
        List<CurriculumMapResponse.SemesterGroup> semesterGroups
                = semesterMap
                        .entrySet()
                        .stream()
                        .filter(entry
                                -> !entry.getValue().isEmpty()
                        )
                        .map(entry
                                -> CurriculumMapResponse.SemesterGroup
                                .builder()
                                .semester(entry.getKey())
                                .courses(
                                        entry.getValue()
                                                .stream()
                                                .sorted(
                                                        Comparator
                                                                .comparing(
                                                                        CurriculumMapResponse.CourseNode::getCourseCode,
                                                                        Comparator
                                                                                .nullsLast(
                                                                                        String::compareToIgnoreCase
                                                                                )
                                                                )
                                                )
                                                .toList()
                                )
                                .build()
                        )
                        .toList();

        // =========================
        // 7. LẤY QUAN HỆ MÔN HỌC
        // =========================
        List<CourseRelationship> relationships
                = courseRelationshipRepository
                        .findAllWithCourses();

        List<CurriculumMapResponse.RelationEdge> relationEdges
                = relationships
                        .stream()
                        .filter(relationship
                                -> relationship.getCourse() != null
                        && relationship
                                .getRelatedCourse() != null
                        && selectedCourseIds.contains(
                                relationship
                                        .getCourse()
                                        .getId()
                        )
                        && selectedCourseIds.contains(
                                relationship
                                        .getRelatedCourse()
                                        .getId()
                        )
                        && isVisibleOnMap(
                                relationship
                                        .getRelationType()
                        )
                        )
                        .map(relationship
                                -> CurriculumMapResponse.RelationEdge
                                .builder()
                                /*
                                         * relatedCourse là môn đi trước.
                                         * course là môn phụ thuộc.
                                         *
                                         * Ví dụ:
                                         * Programming 1
                                         *       ->
                                         * Programming 2
                                 */
                                .fromCourseId(
                                        relationship
                                                .getRelatedCourse()
                                                .getId()
                                )
                                .fromCourseCode(
                                        relationship
                                                .getRelatedCourse()
                                                .getCourseCode()
                                )
                                .fromCourseName(
                                        relationship
                                                .getRelatedCourse()
                                                .getName()
                                )
                                .toCourseId(
                                        relationship
                                                .getCourse()
                                                .getId()
                                )
                                .toCourseCode(
                                        relationship
                                                .getCourse()
                                                .getCourseCode()
                                )
                                .toCourseName(
                                        relationship
                                                .getCourse()
                                                .getName()
                                )
                                .relationType(
                                        relationship
                                                .getRelationType()
                                )
                                .build()
                        )
                        .toList();

        // =========================
        // 8. TRẢ KẾT QUẢ
        // =========================
        Integer majorId =
        program.getMajor() != null
                ? program.getMajor().getId()
                : null;

String majorCode =
        program.getMajor() != null
                ? program.getMajor().getCode()
                : null;

return CurriculumMapResponse
        .builder()

        .programId(program.getId())
        .programCode(program.getCode())

        .majorId(majorId)
        .majorCode(majorCode)

        .cohortId(
                cohort != null
                        ? cohort.getId()
                        : null
        )
        .cohortName(
                cohort != null
                        ? cohort.getName()
                        : null
        )

        /*
         * Giữ field cũ để frontend hiện tại tiếp tục chạy.
         */
        .academicYear(
                cohort != null
                        ? cohort.getName()
                        : program.getCode()
        )
        .major(majorCode)

        .semesters(semesterGroups)
        .relations(relationEdges)
        .build();
    }

    private String normalizeSemesterKey(String value) {

        if (value == null || value.isBlank()) {
            return "Unassigned";
        }

        String text = value.trim();
        String lower = text.toLowerCase();

        if (lower.contains("summer")
                || lower.contains("hè")) {
            return "Summer";
        }

        if (lower.contains("elective")
                || lower.contains("tự chọn")) {
            return "Elective";
        }

        java.util.regex.Matcher matcher
                = java.util.regex.Pattern
                        .compile(
                                "(?:semester|hk|học kỳ|hoc ky)?\\s*(\\d+)",
                                java.util.regex.Pattern.CASE_INSENSITIVE
                        )
                        .matcher(text);

        if (matcher.find()) {
            return "Semester "
                    + Integer.parseInt(
                            matcher.group(1)
                    );
        }

        return text;
    }

    private boolean isVisibleOnMap(
            RelationType relationType
    ) {

        return relationType
                == RelationType.PREREQUISITE
                || relationType
                == RelationType.RECOMMENDED
                || relationType
                == RelationType.COREQUISITE;
    }
}
