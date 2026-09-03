package com.scse.curriculum.curriculummap.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.courserelationship.entity.CourseRelationship;
import com.scse.curriculum.courserelationship.entity.RelationType;
import com.scse.curriculum.courserelationship.repository.CourseRelationshipRepository;
import com.scse.curriculum.curriculummap.dto.CurriculumMapResponse;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.entity.SyllabusStatus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurriculumMapServiceImpl implements CurriculumMapService {
    private final SyllabusRepository syllabusRepository;
    private final SyllabusAccessService syllabusAccessService;
    private final CourseProgramRepository courseProgramRepository;
    private final CourseRelationshipRepository courseRelationshipRepository;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;

    @Override
    @Transactional(readOnly = true)
    public CurriculumMapResponse generate(
            Integer programId,
            Integer cohortId,
            String semester,
            String status) {
        if (programId == null) {
            throw new IllegalArgumentException("programId không được để trống");
        }

        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy chương trình đào tạo có ID = " + programId));
        Cohort cohort = loadAndValidateCohort(programId, cohortId);

        String semesterFilter = normalizeOptional(semester);
        SyllabusStatus statusFilter = parseStatus(status);

        // The node set starts exclusively from the same real syllabus links used
        // by Catalog. It never starts from the static curriculum course list.
        List<Syllabus> filteredSyllabuses = syllabusRepository
                .findCatalogScope(programId, cohortId)
                .stream()
                .filter(syllabusAccessService::canView)
                .filter(syllabus -> semesterFilter == null
                        || normalizeSemester(syllabus.getSemester()).equals(semesterFilter))
                .filter(syllabus -> statusFilter == null || syllabus.getStatus() == statusFilter)
                .toList();

        // Match Catalog's visible-row rule: one current/latest version per course
        // within the selected program/cohort/status/semester dataset.
        Map<Integer, Syllabus> syllabusByCourseId = new LinkedHashMap<>();
        for (Syllabus candidate : filteredSyllabuses) {
            if (candidate.getCourse() == null || candidate.getCourse().getId() == null) continue;
            syllabusByCourseId.merge(candidate.getCourse().getId(), candidate, this::newerCatalogVersion);
        }

        Map<Integer, CourseProgram> mappingByCourseId = courseProgramRepository
                .findByProgramIdAndCohortIdWithRelations(programId, cohortId)
                .stream()
                .filter(cp -> cp.getSyllabus() != null && syllabusByCourseId.containsKey(cp.getCourse().getId()))
                .collect(Collectors.toMap(
                        cp -> cp.getCourse().getId(),
                        cp -> cp,
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<CurriculumMapResponse.CourseNode> nodes = syllabusByCourseId.values().stream()
                .map(syllabus -> toNode(syllabus, mappingByCourseId.get(syllabus.getCourse().getId()), program, cohort))
                .sorted(Comparator.comparing(CurriculumMapResponse.CourseNode::getCourseCode,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<CurriculumMapResponse.SemesterGroup> semesterGroups = nodes.stream()
                .collect(Collectors.groupingBy(
                        CurriculumMapResponse.CourseNode::getSemester,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> semesterOrder(entry.getKey())))
                .map(entry -> CurriculumMapResponse.SemesterGroup.builder()
                        .semester(entry.getKey()).courses(entry.getValue()).build())
                .toList();

        Set<Integer> selectedCourseIds = syllabusByCourseId.keySet();
        List<CurriculumMapResponse.RelationEdge> edges = courseRelationshipRepository.findAllWithCourses().stream()
                .filter(this::isValidVisibleRelationship)
                .filter(relation -> selectedCourseIds.contains(relation.getCourse().getId())
                        && selectedCourseIds.contains(relation.getRelatedCourse().getId()))
                .map(this::toEdge)
                .toList();

        String majorCode = program.getMajor() == null ? null : program.getMajor().getCode();
        return CurriculumMapResponse.builder()
                .programId(program.getId()).programCode(program.getCode())
                .majorId(program.getMajor() == null ? null : program.getMajor().getId())
                .majorCode(majorCode)
                .cohortId(cohort == null ? null : cohort.getId())
                .cohortName(cohort == null ? null : cohort.getName())
                .academicYear(cohort == null ? program.getCode() : cohort.getName())
                .major(majorCode)
                .semesters(semesterGroups)
                .relations(edges)
                .build();
    }

    private Cohort loadAndValidateCohort(Integer programId, Integer cohortId) {
        if (cohortId == null) return null;
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa có ID = " + cohortId));
        if (cohort.getProgram() == null || !Objects.equals(cohort.getProgram().getId(), programId)) {
            throw new IllegalArgumentException("Khóa được chọn không thuộc chương trình đào tạo này");
        }
        return cohort;
    }

    private Syllabus newerCatalogVersion(Syllabus left, Syllabus right) {
        if (Boolean.TRUE.equals(right.getIsCurrent()) != Boolean.TRUE.equals(left.getIsCurrent())) {
            return Boolean.TRUE.equals(right.getIsCurrent()) ? right : left;
        }
        int leftVersion = left.getVersionNumber() == null ? 0 : left.getVersionNumber();
        int rightVersion = right.getVersionNumber() == null ? 0 : right.getVersionNumber();
        return rightVersion > leftVersion ? right : left;
    }

    private CurriculumMapResponse.CourseNode toNode(
            Syllabus syllabus, CourseProgram mapping, Program program, Cohort cohort) {
        var course = syllabus.getCourse();
        String courseType = mapping == null || mapping.getCourseType() == null
                ? syllabus.getCourseTypes() : mapping.getCourseType().getName();
        return CurriculumMapResponse.CourseNode.builder()
                .courseId(course.getId()).courseCode(course.getCourseCode())
                .courseName(course.getName()).courseNameVn(course.getNameVn())
                .creditTheory(course.getCreditTheory()).creditLab(course.getCreditLab())
                .semester(displaySemester(syllabus.getSemester()))
                .academicYear(syllabus.getAcademicYear())
                .major(program.getMajor() == null ? syllabus.getMajor() : program.getMajor().getCode())
                .courseTypes(courseType).syllabusVersion(syllabus.getVersionLabel()).build();
    }

    private boolean isValidVisibleRelationship(CourseRelationship relationship) {
        RelationType type = relationship.getRelationType();
        return relationship.getCourse() != null && relationship.getRelatedCourse() != null
                && (type == RelationType.PREREQUISITE
                    || type == RelationType.RECOMMENDED
                    || type == RelationType.COREQUISITE);
    }

    private CurriculumMapResponse.RelationEdge toEdge(CourseRelationship relationship) {
        var from = relationship.getRelatedCourse();
        var to = relationship.getCourse();
        return CurriculumMapResponse.RelationEdge.builder()
                .fromCourseId(from.getId()).fromCourseCode(from.getCourseCode()).fromCourseName(from.getName())
                .toCourseId(to.getId()).toCourseCode(to.getCourseCode()).toCourseName(to.getName())
                .relationType(relationship.getRelationType()).build();
    }

    private SyllabusStatus parseStatus(String raw) {
        String value = normalizeOptional(raw);
        if (value == null) return null;
        try {
            return SyllabusStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Trạng thái syllabus không hợp lệ: " + raw);
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) return null;
        return value.trim();
    }

    private String normalizeSemester(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) return "";
        var matcher = java.util.regex.Pattern.compile(
                "(?i)^(?:(?:semester|hk)\\s*)?([1-8])$").matcher(normalized);
        if (matcher.matches()) return matcher.group(1);
        if (normalized.equalsIgnoreCase("summer") || normalized.equalsIgnoreCase("summer semester")) return "summer";
        return normalized.toLowerCase();
    }

    private String displaySemester(String value) {
        String normalized = normalizeSemester(value);
        if (normalized.matches("[1-8]")) return "Semester " + normalized;
        if (normalized.equals("summer")) return "Summer";
        return value == null || value.isBlank() ? "Unassigned" : value.trim();
    }

    private int semesterOrder(String value) {
        String normalized = normalizeSemester(value);
        if (normalized.matches("[1-8]")) return Integer.parseInt(normalized);
        if (normalized.equals("summer")) return 9;
        return 10;
    }
}
