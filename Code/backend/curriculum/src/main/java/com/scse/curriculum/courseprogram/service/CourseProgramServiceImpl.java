package com.scse.curriculum.courseprogram.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.cohort.entity.Cohort;
import com.scse.curriculum.cohort.repository.CohortRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.course.entity.Course;
import com.scse.curriculum.course.repository.CourseRepository;
import com.scse.curriculum.courseprogram.dto.CourseProgramResponse;
import com.scse.curriculum.courseprogram.dto.CreateCourseProgramRequest;
import com.scse.curriculum.courseprogram.dto.UpdateCourseProgramRequest;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.coursetype.entity.CourseType;
import com.scse.curriculum.coursetype.repository.CourseTypeRepository;
import com.scse.curriculum.program.entity.Program;
import com.scse.curriculum.program.repository.ProgramRepository;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseProgramServiceImpl
        implements CourseProgramService {
private final SyllabusRepository syllabusRepository;
    private final CourseProgramRepository repository;

    private final CourseRepository courseRepository;

    private final ProgramRepository programRepository;

    private final CohortRepository cohortRepository;

    private final CourseTypeRepository courseTypeRepository;

    @Override
    public CourseProgramResponse create(
            CreateCourseProgramRequest request) {

        Course course
                = courseRepository.findById(
                        request.getCourseId())
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Course not found"));

        Program program
                = programRepository.findById(
                        request.getProgramId())
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Program not found"));

        Cohort cohort = null;

if (request.getCohortId() != null) {
    cohort = cohortRepository
            .findById(request.getCohortId())
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Cohort not found"
                    )
            );

    if (cohort.getProgram() == null
            || !Objects.equals(
                    cohort.getProgram().getId(),
                    program.getId()
            )) {

        throw new IllegalArgumentException(
                "Cohort không thuộc chương trình đào tạo đã chọn"
        );
    }
}

        CourseType courseType
                = courseTypeRepository.findById(
                        request.getCourseTypeId())
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "Course type not found"));

        Integer semesterSuggest =
        request.getSemesterSuggest();

if (request.getTermCode() != null
        && request.getTermCode()
                .isRegularSemester()) {

    semesterSuggest =
            request.getTermCode()
                    .getSemesterNumber();
}

CourseProgram entity
        = CourseProgram.builder()
                .course(course)
                .program(program)
                .cohort(cohort)
                .courseType(courseType)
                .termCode(
                        request.getTermCode())
                .semesterSuggest(
                        semesterSuggest)
                .yearSuggest(
                        request.getYearSuggest())
                .required(
                        Boolean.TRUE.equals(
                                request.getRequired()))
                .build();

        return map(
                repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgramResponse getById(
            Integer id) {

        return map(
                repository.findById(id)
                        .orElseThrow(()
                                -> new ResourceNotFoundException(
                                "CourseProgram not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgramResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgramResponse> getByProgram(
            Integer programId) {

        return repository.findByProgram_Id(programId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgramResponse> getByCohort(
            Integer cohortId) {

        return repository.findByCohort_Id(cohortId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseProgramResponse> getByProgramAndCohort(
            Integer programId,
            Integer cohortId) {

        return repository
        .findEffectiveByProgramIdAndCohortIdWithRelations(
                programId,
                cohortId)
        .stream()
        .map(this::map)
        .toList();
    }

    @Override
    public void delete(
            Integer id) {

        repository.deleteById(id);
    }

    @Override
    public CourseProgramResponse update(
            Integer id,
            UpdateCourseProgramRequest request) {

        CourseProgram entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CourseProgram not found"));

                if (request.getTermCode() != null) {
    entity.setTermCode(
            request.getTermCode()
    );

    entity.setSemesterSuggest(
            request.getTermCode()
                    .getSemesterNumber()
    );
}
        if (request.getSemesterSuggest() != null) {
            entity.setSemesterSuggest(request.getSemesterSuggest());
        }
        if (request.getCourseTypeId() != null) {
            CourseType courseType = courseTypeRepository.findById(request.getCourseTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course type not found"));
            entity.setCourseType(courseType);
        }
        if (request.getIsRequired() != null) {
            entity.setRequired(request.getIsRequired());
        }

        return map(repository.save(entity));
    }

    private CourseProgramResponse map(CourseProgram entity) {

    Course course = entity.getCourse();
    Program program = entity.getProgram();
    Cohort cohort = entity.getCohort();
    CourseType courseType = entity.getCourseType();
    Syllabus syllabus = entity.getSyllabus();

    Integer creditTheory =
            course != null
                    ? course.getCreditTheory()
                    : null;

    Integer creditLab =
            course != null
                    ? course.getCreditLab()
                    : null;

    int totalCredits =
            (creditTheory != null ? creditTheory : 0)
            + (creditLab != null ? creditLab : 0);

    return CourseProgramResponse.builder()
            .id(entity.getId())

            .courseId(
                    course != null
                            ? course.getId()
                            : null
            )
            .courseCode(
                    course != null
                            ? course.getCourseCode()
                            : null
            )
            .courseName(
                    course != null
                            ? course.getName()
                            : null
            )

            .programId(
                    program != null
                            ? program.getId()
                            : null
            )
            .programCode(
                    program != null
                            ? program.getCode()
                            : null
            )
            .programName(
                    program != null
                            ? program.getName()
                            : null
            )

            .majorId(
                    program != null
                            && program.getMajor() != null
                            ? program.getMajor().getId()
                            : null
            )
            .majorCode(
                    program != null
                            && program.getMajor() != null
                            ? program.getMajor().getCode()
                            : null
            )
            .majorName(
                    program != null
                            && program.getMajor() != null
                            ? program.getMajor().getName()
                            : null
            )

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

            .courseTypeId(
                    courseType != null
                            ? courseType.getId()
                            : null
            )
            .courseTypeName(
                    courseType != null
                            ? courseType.getName()
                            : null
            )

            .creditTheory(creditTheory)
            .creditLab(creditLab)
            .totalCredits(totalCredits)

            .syllabusId(
                    syllabus != null
                            ? syllabus.getId()
                            : null
            )
            .syllabusVersionNumber(
                    syllabus != null
                            ? syllabus.getVersionNumber()
                            : null
            )
            .syllabusVersionLabel(
                    syllabus != null
                            ? syllabus.getVersionLabel()
                            : null
            )
            .syllabusStatus(
                    syllabus != null
                            && syllabus.getStatus() != null
                            ? syllabus.getStatus().name()
                            : null
            )
            .syllabusCurrent(
                    syllabus != null
                            ? syllabus.getIsCurrent()
                            : null
            )
            .hasSyllabus(syllabus != null)

            .semesterSuggest(entity.getSemesterSuggest())
            .yearSuggest(entity.getYearSuggest())
            .required(entity.getRequired())

            .build();
}
    @Override
@Transactional
public CourseProgramResponse assignSyllabus(
        Integer courseProgramId,
        Integer syllabusId) {

    CourseProgram courseProgram = repository.findById(courseProgramId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("CourseProgram not found"));

    Syllabus syllabus = syllabusRepository.findById(syllabusId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Syllabus not found"));

    if (courseProgram.getCourse() == null
            || syllabus.getCourse() == null
            || !Objects.equals(
                    courseProgram.getCourse().getId(),
                    syllabus.getCourse().getId())) {

        throw new IllegalArgumentException(
                "Không thể gán đề cương của môn khác vào CTĐT");
    }

    courseProgram.setSyllabus(syllabus);

    return map(repository.save(courseProgram));
}
}
